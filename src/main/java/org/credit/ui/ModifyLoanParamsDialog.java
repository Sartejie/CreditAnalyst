package org.credit.ui;

import org.credit.db.Database;
import org.credit.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Экран 9: Изменение параметров действующего кредита аналитиком (UC-05) с предпросмотром пересчёта.
 */
public class ModifyLoanParamsDialog extends JDialog {
    private final ActiveLoan loan;
    private final ScoringModel scoringModel;

    private JTextField rateField;
    private JSpinner termSpinner;
    private JComboBox<String> methodBox;
    private JTextField earlyAmountField;
    private JTextArea previewArea;
    private FinalReport updatedReport;
    private boolean applied = false;

    public ModifyLoanParamsDialog(Window owner, ActiveLoan loan, ScoringModel scoringModel) {
        super(owner, "Изменение параметров кредита " +
                (loan.getContractNumber() != null ? loan.getContractNumber() : "#" + loan.getId()),
                ModalityType.APPLICATION_MODAL);
        this.loan = loan;
        this.scoringModel = scoringModel != null ? scoringModel : new ScoringModel();
        initUI();
        setSize(640, 500);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14, 16, 4, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        rateField = new JTextField(String.valueOf(loan.getRate() != null ? loan.getRate() : 12.0), 16);
        int currentRemaining = loan.getTerm() != null ? Math.max(1, loan.getTerm() - (loan.getPaidPeriods() != null ? loan.getPaidPeriods() : 0)) : 12;
        termSpinner = new JSpinner(new SpinnerNumberModel(currentRemaining, 1, 360, 1));
        methodBox = new JComboBox<>(new String[]{
                PaymentSchedule.METHOD_ANNUITY,
                PaymentSchedule.METHOD_DIFFERENTIATED,
                PaymentSchedule.METHOD_LUMP_SUM_MONTHLY_INTEREST,
                PaymentSchedule.METHOD_LUMP_SUM_END_INTEREST
        });
        if (loan.getRepaymentMethod() != null) {
            methodBox.setSelectedItem(loan.getRepaymentMethod());
        }
        earlyAmountField = new JTextField("0", 16);

        int row = 0;
        addRow(form, gbc, row++, "Текущий остаток основного долга:",
                new JLabel(String.format("%.2f", loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : 0.0)));
        addRow(form, gbc, row++, "Новая процентная ставка (%):", rateField);
        addRow(form, gbc, row++, "Новый оставшийся срок (мес.):", termSpinner);
        addRow(form, gbc, row++, "Способ погашения:", methodBox);
        addRow(form, gbc, row++, "Досрочное погашение (сумма, 0 — без):", earlyAmountField);

        add(form, BorderLayout.NORTH);

        previewArea = new JTextArea("Нажмите «Предпросмотр пересчёта» для оценки новых условий...");
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        previewArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane sp = new JScrollPane(previewArea);
        sp.setBorder(BorderFactory.createTitledBorder("Предпросмотр пересчёта (график, ПДН, скоринг)"));
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(0, 16, 0, 16));
        center.add(sp, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(new EmptyBorder(0, 16, 12, 16));
        JButton previewBtn = new JButton("Предпросмотр пересчёта");
        previewBtn.addActionListener(e -> buildPreview());
        JButton applyBtn = new JButton("Применить и сохранить в БД");
        applyBtn.addActionListener(e -> applyChanges());
        JButton cancelBtn = new JButton("Отмена");
        cancelBtn.addActionListener(e -> dispose());

        buttons.add(previewBtn);
        buttons.add(applyBtn);
        buttons.add(cancelBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    private void addRow(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(comp, gbc);
    }

    private Map<String, Object> readParams() {
        double rate = Double.parseDouble(rateField.getText().trim().replace(",", "."));
        if (rate < 0) throw new IllegalArgumentException("Ставка не может быть отрицательной.");
        int newRemainingTerm = (Integer) termSpinner.getValue();
        int done = loan.getPaidPeriods() != null ? loan.getPaidPeriods() : 0;
        double early = Double.parseDouble(earlyAmountField.getText().trim().replace(",", "."));
        if (early < 0) throw new IllegalArgumentException("Сумма досрочного погашения не может быть отрицательной.");

        Map<String, Object> params = new HashMap<>();
        params.put("rate", rate);
        params.put("term", done + newRemainingTerm);
        params.put("repaymentMethod", methodBox.getSelectedItem());
        if (early > 0) {
            params.put("earlyRepaymentAmount", early);
            params.put("paymentDate", new Date());
        }
        return params;
    }

    private ActiveLoan cloneLoanForPreview() {
        ActiveLoan copy = new ActiveLoan(
                loan.getId(), loan.getApplicationId(), loan.getStatus(),
                loan.getOutstandingPrincipal(), loan.getAccruedInterest(),
                loan.getOverdueAmount(), loan.getRate(), loan.getTerm(), loan.getRepaymentMethod()
        );
        copy.setBorrowerName(loan.getBorrowerName());
        copy.setMonthlyIncome(loan.getMonthlyIncome());
        copy.setPaidPeriods(loan.getPaidPeriods());
        copy.setIssueDate(loan.getIssueDate());
        copy.setLastAccrualDate(loan.getLastAccrualDate());
        copy.setNextPaymentDate(loan.getNextPaymentDate());
        copy.setOverdueDays(loan.getOverdueDays());
        copy.setSchedule(loan.getSchedule());
        return copy;
    }

    private void buildPreview() {
        try {
            Map<String, Object> params = readParams();
            ActiveLoan copy = cloneLoanForPreview();
            copy.changeParameters(params);

            Form2501 history = null;
            try (Database db = new Database()) {
                if (loan.getFormId() > 0) history = db.getForm2501ById(loan.getFormId());
            }
            double threshold = scoringModel.getRules().getOrDefault("pdn_warning_threshold", DebtBurden.DEFAULT_THRESHOLD_PERCENT);
            FinalReport report = copy.buildUpdatedReport(loan.getMonthlyIncome(), history, scoringModel, threshold);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Новый статус:          %s\n", copy.getStatus()));
            sb.append(String.format("Остаток основного долга: %.2f\n", copy.getOutstandingPrincipal()));
            if (report.getSchedule() != null) {
                sb.append(String.format("Новый ежемес. платёж:  %.2f\n", report.getSchedule().getMonthlyPayment()));
                sb.append(String.format("Общая сумма остатка:   %.2f\n", report.getSchedule().getTotalAmount()));
                sb.append(String.format("Платежей в графике:    %d\n", report.getSchedule().getPayments().size()));
            }
            if (report.getPdn() != null) {
                sb.append(String.format("Пересчитанный ПДН:     %.2f %%\n", report.getPdn().getValue()));
                if (report.getPdn().isThresholdExceeded()) {
                    sb.append("ПРЕДУПРЕЖДЕНИЕ: ").append(report.getPdn().getWarningMessage()).append("\n");
                }
            }
            if (report.getScoring() != null) {
                sb.append(String.format("Скоринг / Риск:        %d (%s)\n",
                        report.getScoring().getScore(), report.getScoring().getRiskCategory()));
            }
            previewArea.setText(sb.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Проверьте числовые поля.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void applyChanges() {
        try {
            Map<String, Object> params = readParams();
            int beforeCount = loan.getPayments().size();
            loan.changeParameters(params);

            Form2501 history = null;
            try (Database db = new Database()) {
                if (loan.getFormId() > 0) history = db.getForm2501ById(loan.getFormId());
                for (int i = beforeCount; i < loan.getPayments().size(); i++) {
                    db.SavePayment(loan.getPayments().get(i));
                }
                db.UpdateLoan(loan);
                double threshold = scoringModel.getRules().getOrDefault("pdn_warning_threshold", DebtBurden.DEFAULT_THRESHOLD_PERCENT);
                updatedReport = loan.buildUpdatedReport(loan.getMonthlyIncome(), history, scoringModel, threshold);
                db.SaveReport(updatedReport);
            }

            applied = true;
            JOptionPane.showMessageDialog(this, "Параметры кредита изменены, график и отчёт обновлены в БД.",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Проверьте числовые поля.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при сохранении изменений: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isApplied() { return applied; }
    public FinalReport getUpdatedReport() { return updatedReport; }
}
