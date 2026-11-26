package org.credit.ui;

import org.credit.model.CreditDeal;
import org.credit.model.ValidationError;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.List;

/**
 * Диалог добавления и редактирования кредитной сделки Формы 2501.
 */
public class CreditDealDialog extends JDialog {
    private final CreditDeal deal;
    private boolean saved = false;

    private JTextField contractNumberField;
    private JTextField dealTypeField;
    private JTextField creditTypeField;
    private JSpinner conclusionDateSpinner;
    private JTextField amountField;
    private JTextField currencyField;
    private JSpinner termSpinner;
    private JSpinner renewalTermSpinner;
    private JSpinner repaymentDateSpinner;
    private JTextField securityField;
    private JTextField riskGroupField;
    private JSpinner relevanceDateSpinner;
    private JSpinner overdueDaysSpinner;
    private JTextField overdueAmountField;
    private JTextField monthlyPaymentField;

    public CreditDealDialog(Window owner, CreditDeal existingDeal) {
        super(owner, existingDeal == null ? "Добавление сделки (Форма 2501)" : "Редактирование сделки (Форма 2501)",
                ModalityType.APPLICATION_MODAL);
        this.deal = existingDeal != null ? existingDeal : new CreditDeal();

        initUI();
        if (existingDeal != null) {
            populateFields();
        }

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 5, 4, 5);
        int row = 0;

        contractNumberField = new JTextField(22);
        dealTypeField = new JTextField("Кредит", 22);
        creditTypeField = new JTextField("Потребительский", 22);
        conclusionDateSpinner = createDateSpinner();
        amountField = new JTextField(22);
        currencyField = new JTextField("BYN", 22);
        termSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 360, 1));
        renewalTermSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 360, 1));
        repaymentDateSpinner = createDateSpinner();
        securityField = new JTextField("Без обеспечения", 22);
        riskGroupField = new JTextField("1", 22);
        relevanceDateSpinner = createDateSpinner();
        overdueDaysSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3650, 1));
        overdueAmountField = new JTextField("0", 22);
        monthlyPaymentField = new JTextField(22);

        addField(panel, gbc, row++, "№ Договора*:", contractNumberField);
        addField(panel, gbc, row++, "Вид сделки:", dealTypeField);
        addField(panel, gbc, row++, "Вид кредита:", creditTypeField);
        addField(panel, gbc, row++, "Дата заключения:", conclusionDateSpinner);
        addField(panel, gbc, row++, "Сумма*:", amountField);
        addField(panel, gbc, row++, "Валюта:", currencyField);
        addField(panel, gbc, row++, "Срок возврата (мес.):", termSpinner);
        addField(panel, gbc, row++, "Срок возобновляемости (мес.):", renewalTermSpinner);
        addField(panel, gbc, row++, "Дата возврата:", repaymentDateSpinner);
        addField(panel, gbc, row++, "Обеспечение:", securityField);
        addField(panel, gbc, row++, "Группа риска:", riskGroupField);
        addField(panel, gbc, row++, "Дата актуальности сведений:", relevanceDateSpinner);
        addField(panel, gbc, row++, "Длительность просрочки (дн.):", overdueDaysSpinner);
        addField(panel, gbc, row++, "Сумма просрочки:", overdueAmountField);
        addField(panel, gbc, row++, "Ежемесячный платёж (если известен):", monthlyPaymentField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Сохранить");
        JButton cancelBtn = new JButton("Отмена");

        saveBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd.MM.yyyy"));
        return spinner;
    }

    private void addField(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(comp, gbc);
    }

    private void populateFields() {
        if (deal.getContractNumber() != null) contractNumberField.setText(deal.getContractNumber());
        if (deal.getDealType() != null) dealTypeField.setText(deal.getDealType());
        if (deal.getCreditType() != null) creditTypeField.setText(deal.getCreditType());
        if (deal.getConclusionDate() != null) conclusionDateSpinner.setValue(deal.getConclusionDate());
        if (deal.getAmount() != null) amountField.setText(String.valueOf(deal.getAmount()));
        if (deal.getCurrency() != null) currencyField.setText(deal.getCurrency());
        if (deal.getTerm() != null) termSpinner.setValue(deal.getTerm());
        if (deal.getRenewalTerm() != null) renewalTermSpinner.setValue(deal.getRenewalTerm());
        if (deal.getRepaymentDate() != null) repaymentDateSpinner.setValue(deal.getRepaymentDate());
        if (deal.getSecurity() != null) securityField.setText(deal.getSecurity());
        if (deal.getRiskGroup() != null) riskGroupField.setText(deal.getRiskGroup());
        if (deal.getRelevanceDate() != null) relevanceDateSpinner.setValue(deal.getRelevanceDate());
        if (deal.getOverdueDays() != null) overdueDaysSpinner.setValue(deal.getOverdueDays());
        if (deal.getOverdueAmount() != null) overdueAmountField.setText(String.valueOf(deal.getOverdueAmount()));
        if (deal.getMonthlyPayment() != null) monthlyPaymentField.setText(String.valueOf(deal.getMonthlyPayment()));
    }

    private void onSave() {
        try {
            CreditDeal candidate = new CreditDeal();
            candidate.setContractNumber(contractNumberField.getText().trim());
            candidate.setDealType(dealTypeField.getText().trim());
            candidate.setCreditType(creditTypeField.getText().trim());
            candidate.setConclusionDate((Date) conclusionDateSpinner.getValue());
            candidate.setAmount(Double.parseDouble(amountField.getText().trim().replace(",", ".")));
            candidate.setCurrency(currencyField.getText().trim());
            candidate.setTerm((Integer) termSpinner.getValue());
            candidate.setRenewalTerm((Integer) renewalTermSpinner.getValue());
            candidate.setRepaymentDate((Date) repaymentDateSpinner.getValue());
            candidate.setSecurity(securityField.getText().trim());
            candidate.setRiskGroup(riskGroupField.getText().trim());
            candidate.setRelevanceDate((Date) relevanceDateSpinner.getValue());
            candidate.setOverdueDays((Integer) overdueDaysSpinner.getValue());

            String ovAmt = overdueAmountField.getText().trim().replace(",", ".");
            candidate.setOverdueAmount(ovAmt.isEmpty() ? 0.0 : Double.parseDouble(ovAmt));

            String mPay = monthlyPaymentField.getText().trim().replace(",", ".");
            candidate.setMonthlyPayment(mPay.isEmpty() ? null : Double.parseDouble(mPay));

            List<ValidationError> errors = candidate.validateDetailed(1);
            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (ValidationError err : errors) {
                    sb.append("• ").append(err.getField()).append(": ").append(err.getMessage()).append("\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString(), "Ошибка валидации сделки", JOptionPane.WARNING_MESSAGE);
                return;
            }

            deal.setContractNumber(candidate.getContractNumber());
            deal.setDealType(candidate.getDealType());
            deal.setCreditType(candidate.getCreditType());
            deal.setConclusionDate(candidate.getConclusionDate());
            deal.setAmount(candidate.getAmount());
            deal.setCurrency(candidate.getCurrency());
            deal.setTerm(candidate.getTerm());
            deal.setRenewalTerm(candidate.getRenewalTerm());
            deal.setRepaymentDate(candidate.getRepaymentDate());
            deal.setSecurity(candidate.getSecurity());
            deal.setRiskGroup(candidate.getRiskGroup());
            deal.setRelevanceDate(candidate.getRelevanceDate());
            deal.setOverdueDays(candidate.getOverdueDays());
            deal.setOverdueAmount(candidate.getOverdueAmount());
            deal.setMonthlyPayment(candidate.getMonthlyPayment());

            saved = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Проверьте числовые поля (Сумма, Сумма просрочки, Ежемесячный платёж).",
                    "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
    public CreditDeal getDeal() { return deal; }
}
