package org.credit.ui;

import org.credit.db.Database;
import org.credit.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Экран 7: Сопровождение действующего кредита (статус, остаток, проценты, просрочка, график, история платежей).
 */
public class LoanServicingDialog extends JDialog {
    private ActiveLoan loan;
    private final User currentUser;
    private final ScoringModel scoringModel;

    private JLabel statusValueLabel;
    private JLabel principalValueLabel;
    private JLabel interestValueLabel;
    private JLabel overdueValueLabel;
    private JLabel nextDateValueLabel;
    private JButton payBtn;
    private JButton modifyBtn;

    private DefaultTableModel scheduleTableModel;
    private DefaultTableModel historyTableModel;
    private boolean updated = false;

    public LoanServicingDialog(Window owner, ActiveLoan loan, User currentUser, ScoringModel scoringModel) {
        super(owner, "Сопровождение кредита " +
                (loan.getContractNumber() != null ? loan.getContractNumber() : "#" + loan.getId()),
                ModalityType.APPLICATION_MODAL);
        this.loan = loan;
        this.currentUser = currentUser;
        this.scoringModel = scoringModel != null ? scoringModel : new ScoringModel();

        // Автоматическая проверка просрочки на текущую дату
        this.loan.checkAndUpdateOverdue(new Date());
        try (Database db = new Database()) {
            db.UpdateLoan(this.loan);
        } catch (Exception ignored) {
        }

        initUI();
        refreshView();
        setSize(860, 600);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Карточка текущего состояния кредита
        JPanel statePanel = new JPanel(new GridLayout(2, 3, 12, 8));
        statePanel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(12, 16, 0, 16),
                BorderFactory.createTitledBorder("Состояние кредита")));

        statusValueLabel = new JLabel();
        principalValueLabel = new JLabel();
        interestValueLabel = new JLabel();
        overdueValueLabel = new JLabel();
        nextDateValueLabel = new JLabel();
        JLabel borrowerLabel = new JLabel("Заёмщик: " +
                (loan.getBorrowerName() != null ? loan.getBorrowerName() : "—"));

        statePanel.add(statusValueLabel);
        statePanel.add(principalValueLabel);
        statePanel.add(interestValueLabel);
        statePanel.add(overdueValueLabel);
        statePanel.add(nextDateValueLabel);
        statePanel.add(borrowerLabel);

        add(statePanel, BorderLayout.NORTH);

        // Вкладки: График и История платежей
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(new EmptyBorder(0, 16, 0, 16));

        String[] schCols = {"№", "Дата платежа", "Основной долг", "Проценты", "Итого платёж", "Остаток долга"};
        scheduleTableModel = new DefaultTableModel(schCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable schTable = new JTable(scheduleTableModel);
        schTable.setRowHeight(24);
        tabs.addTab("Актуальный график платежей (UC-01)", new JScrollPane(schTable));

        String[] histCols = {"Дата платежа", "Общая сумма", "Основной долг", "Проценты/просрочка", "Остаток долга", "Тип платежа"};
        historyTableModel = new DefaultTableModel(histCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable histTable = new JTable(historyTableModel);
        histTable.setRowHeight(24);
        tabs.addTab("История платежей (UC-04)", new JScrollPane(histTable));

        add(tabs, BorderLayout.CENTER);

        // Панель действий
        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setBorder(new EmptyBorder(0, 16, 12, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        payBtn = new JButton("Зарегистрировать платёж");
        payBtn.addActionListener(e -> onRegisterPayment());
        left.add(payBtn);

        if (currentUser instanceof CreditSpecialist) {
            modifyBtn = new JButton("Изменить параметры кредита");
            modifyBtn.addActionListener(e -> onModifyParameters());
            left.add(modifyBtn);

            JButton checkOverdueBtn = new JButton("Проверить просрочку на дату...");
            checkOverdueBtn.addActionListener(e -> onCheckOverdueAtDate());
            left.add(checkOverdueBtn);
        }

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton reportBtn = new JButton("Отчёт о состоянии");
        reportBtn.addActionListener(e -> onOpenReport());
        JButton closeBtn = new JButton("Закрыть");
        closeBtn.addActionListener(e -> dispose());
        right.add(reportBtn);
        right.add(closeBtn);

        buttons.add(left, BorderLayout.WEST);
        buttons.add(right, BorderLayout.EAST);
        add(buttons, BorderLayout.SOUTH);
    }

    private void refreshView() {
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        String status = loan.getStatus() != null ? loan.getStatus() : ActiveLoan.STATUS_ACTIVE;
        boolean overdue = ActiveLoan.STATUS_OVERDUE.equals(status);

        statusValueLabel.setText("<html><b>Статус:</b> <font color='" +
                (overdue ? "#B22222" : "#1F6E43") + "'>" + status + "</font></html>");
        principalValueLabel.setText(String.format("<html><b>Остаток ОД:</b> %.2f</html>",
                loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : 0.0));
        interestValueLabel.setText(String.format("<html><b>Начисленные %%:</b> %.2f</html>",
                loan.getAccruedInterest() != null ? loan.getAccruedInterest() : 0.0));
        overdueValueLabel.setText(String.format("<html><b>Просрочка:</b> %.2f (%d дн.)</html>",
                loan.getOverdueAmount() != null ? loan.getOverdueAmount() : 0.0,
                loan.getOverdueDays() != null ? loan.getOverdueDays() : 0));
        nextDateValueLabel.setText("<html><b>Следующий платёж:</b> " +
                (loan.getNextPaymentDate() != null ? df.format(loan.getNextPaymentDate()) : "—") + "</html>");

        boolean open = loan.isOpen();
        payBtn.setEnabled(open);
        if (modifyBtn != null) modifyBtn.setEnabled(open);

        // Заполнение графика
        scheduleTableModel.setRowCount(0);
        if (loan.getSchedule() != null && loan.getSchedule().getPayments() != null) {
            List<Payment> sch = loan.getSchedule().getPayments();
            for (int i = 0; i < sch.size(); i++) {
                Payment p = sch.get(i);
                scheduleTableModel.addRow(new Object[]{
                        p.getPeriodNumber() > 0 ? p.getPeriodNumber() : (i + 1),
                        p.getDate() != null ? df.format(p.getDate()) : "",
                        String.format("%.2f", p.getPrincipalAmount()),
                        String.format("%.2f", p.getInterestAmount()),
                        String.format("%.2f", p.getTotalAmount()),
                        String.format("%.2f", p.getRemainingBalance())
                });
            }
        }

        // Заполнение истории платежей
        historyTableModel.setRowCount(0);
        if (loan.getPayments() != null) {
            for (Payment p : loan.getPayments()) {
                historyTableModel.addRow(new Object[]{
                        p.getDate() != null ? df.format(p.getDate()) : "",
                        String.format("%.2f", p.getTotalAmount()),
                        String.format("%.2f", p.getPrincipalAmount()),
                        String.format("%.2f", p.getInterestAmount()),
                        String.format("%.2f", p.getRemainingBalance()),
                        p.getType() != null ? p.getType() : "СТАНДАРТНЫЙ"
                });
            }
        }
    }

    private void onRegisterPayment() {
        RegisterPaymentDialog dlg = new RegisterPaymentDialog(this, loan);
        dlg.setVisible(true);
        if (dlg.isPaymentRegistered()) {
            reloadLoanFromDb();
            updated = true;
            refreshView();
        }
    }

    private void onModifyParameters() {
        ModifyLoanParamsDialog dlg = new ModifyLoanParamsDialog(this, loan, scoringModel);
        dlg.setVisible(true);
        if (dlg.isApplied()) {
            reloadLoanFromDb();
            updated = true;
            refreshView();
            if (dlg.getUpdatedReport() != null) {
                FinalReportDialog repDlg = new FinalReportDialog(this, dlg.getUpdatedReport(), false);
                repDlg.setVisible(true);
            }
        }
    }

    private void onCheckOverdueAtDate() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd.MM.yyyy"));
        int res = JOptionPane.showConfirmDialog(this, spinner, "Выберите контрольную дату проверки просрочки",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            loan.checkAndUpdateOverdue((Date) spinner.getValue());
            try (Database db = new Database()) {
                db.UpdateLoan(loan);
            }
            updated = true;
            refreshView();
        }
    }

    private void onOpenReport() {
        Form2501 history = null;
        try (Database db = new Database()) {
            if (loan.getFormId() > 0) history = db.getForm2501ById(loan.getFormId());
        }
        double threshold = scoringModel.getRules().getOrDefault("pdn_warning_threshold", DebtBurden.DEFAULT_THRESHOLD_PERCENT);
        FinalReport report = loan.buildUpdatedReport(loan.getMonthlyIncome(), history, scoringModel, threshold);
        FinalReportDialog dlg = new FinalReportDialog(this, report, false);
        dlg.setVisible(true);
    }

    private void reloadLoanFromDb() {
        try (Database db = new Database()) {
            ActiveLoan fresh = db.GetLoan(loan.getId());
            if (fresh != null) {
                this.loan = fresh;
            }
        }
    }

    public boolean isUpdated() { return updated; }
}
