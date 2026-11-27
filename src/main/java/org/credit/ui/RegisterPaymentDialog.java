package org.credit.ui;

import org.credit.db.Database;
import org.credit.model.ActiveLoan;
import org.credit.model.CreditSpecialist;
import org.credit.model.Payment;
import org.credit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Date;

/**
 * Экран 8: Регистрация фактического платежа и оформление досрочного погашения (UC-02, UC-03).
 */
public class RegisterPaymentDialog extends JDialog {
    private User currentUser;
    private final ActiveLoan loan;
    private JTextField amountField;
    private JSpinner dateSpinner;
    private JLabel infoLabel;
    private boolean paymentRegistered = false;

    public RegisterPaymentDialog(Window owner, ActiveLoan loan, User currentUser) {
        super(owner, "Регистрация платежа по кредиту " +
                (loan.getContractNumber() != null ? loan.getContractNumber() : "#" + loan.getId()),
                ModalityType.APPLICATION_MODAL);
        this.loan = loan;
        this.currentUser = currentUser;
        initUI();
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(16, 16, 12, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        dateSpinner = new JSpinner(new SpinnerDateModel());
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy"));
        dateSpinner.addChangeListener(e -> refreshAnalysisHint());

        ActiveLoan.PaymentAnalysis analysis = loan.analyzePayment(0.0, (Date) dateSpinner.getValue());
        infoLabel = new JLabel();
        refreshAnalysisHint();

        amountField = new JTextField(String.format(java.util.Locale.US, "%.2f", analysis.scheduledDue.doubleValue()), 18);

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        panel.add(infoLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Дата платежа:"), gbc);
        gbc.gridx = 1;
        panel.add(dateSpinner, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Сумма платежа:"), gbc);
        gbc.gridx = 1;
        panel.add(amountField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(new EmptyBorder(0, 16, 12, 16));
        JButton payBtn = new JButton("Внести платёж");
        JButton cancelBtn = new JButton("Отмена");
        payBtn.addActionListener(e -> onRegister());
        cancelBtn.addActionListener(e -> dispose());
        buttons.add(payBtn);
        buttons.add(cancelBtn);

        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void refreshAnalysisHint() {
        Date dt = (Date) dateSpinner.getValue();
        ActiveLoan.PaymentAnalysis a = loan.analyzePayment(0.0, dt);
        infoLabel.setText(String.format(
                "<html><b>Кредит:</b> %s (%s)<br>" +
                        "<b>Плановый/текущий платёж:</b> %.2f | <b>Начисл. проценты:</b> %.2f<br>" +
                        "<b>Просрочка:</b> %.2f | <b>Полная задолженность:</b> %.2f</html>",
                loan.getContractNumber() != null ? loan.getContractNumber() : "#" + loan.getId(),
                loan.getStatus(),
                a.scheduledDue.doubleValue(),
                a.accruedInterest.doubleValue(),
                loan.getOverdueAmount() != null ? loan.getOverdueAmount() : 0.0,
                a.totalDebt.doubleValue()
        ));
    }

    private void onRegister() {
        try {
            double amount = Double.parseDouble(amountField.getText().trim().replace(",", "."));
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Сумма платежа должна быть больше 0.",
                        "Ошибка ввода", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Date payDate = (Date) dateSpinner.getValue();
            ActiveLoan.PaymentAnalysis analysis = loan.analyzePayment(amount, payDate);

            boolean confirmEarly = false;
            boolean isSpecialist = currentUser instanceof CreditSpecialist;
            if(analysis.exceedsScheduledDue && !isSpecialist)
            {
                JOptionPane.showMessageDialog(this,
                        String.format("Введённая сумма (%.2f) превышает полную задолженность (%.2f).\n" +
                                        "Досрочное погашение возможно только кредитным аналитиком",
                                amount, analysis.totalDebt.doubleValue()),
                        "Ошибка", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (analysis.exceedsScheduledDue) {
                String msg = analysis.exceedsTotalDebt
                        ? String.format("Введённая сумма (%.2f) превышает полную задолженность (%.2f).\n" +
                                "Оформить полное досрочное погашение кредита?",
                                amount, analysis.totalDebt.doubleValue())
                        : String.format("Введённая сумма (%.2f) превышает текущую задолженность к оплате (%.2f).\n" +
                                "Оформить досрочное погашение с пересчётом графика?",
                                amount, analysis.scheduledDue.doubleValue());

                int choice = JOptionPane.showConfirmDialog(this, msg,
                        "Предложение досрочного погашения", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
                confirmEarly = true;
            }

            Payment payment = new Payment(payDate, amount, 0.0, 0.0, 0.0);
            loan.registerPayment(payment, confirmEarly);

            try (Database db = new Database()) {
                db.SavePayment(payment);
                db.UpdateLoan(loan);
            }

            paymentRegistered = true;
            JOptionPane.showMessageDialog(this,
                    String.format("Платёж зарегистрирован (%s).\nОсновной долг: %.2f | Проценты/просрочка: %.2f\nОстаток долга: %.2f | Статус: %s",
                            payment.getType(), payment.getPrincipalAmount(), payment.getInterestAmount(),
                            loan.getOutstandingPrincipal(), loan.getStatus()),
                    "Платёж проведён", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Введите корректную числовую сумму платежа.",
                    "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при регистрации платежа: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isPaymentRegistered() { return paymentRegistered; }
}
