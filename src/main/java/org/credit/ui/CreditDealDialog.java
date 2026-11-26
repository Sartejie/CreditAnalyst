package org.credit.ui;

import org.credit.model.CreditDeal;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CreditDealDialog extends JDialog {
    private CreditDeal deal;
    private boolean saved = false;

    private JTextField contractNumberField;
    private JTextField dealTypeField;
    private JTextField creditTypeField;
    private JSpinner conclusionDateSpinner;
    private JTextField amountField;
    private JTextField currencyField;
    private JSpinner termSpinner;
    private JSpinner repaymentDateSpinner;
    private JTextField securityField;
    private JTextField riskGroupField;

    public CreditDealDialog(Frame owner, CreditDeal existingDeal) {
        super(owner, existingDeal == null ? "Добавление сделки" : "Редактирование сделки", true);
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
        gbc.insets = new Insets(5, 5, 5, 5);
        int row = 0;

        contractNumberField = new JTextField(20);
        dealTypeField = new JTextField(20);
        creditTypeField = new JTextField(20);
        conclusionDateSpinner = createDateSpinner();
        amountField = new JTextField(20);
        currencyField = new JTextField(20);
        termSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 360, 1));
        repaymentDateSpinner = createDateSpinner();
        securityField = new JTextField(20);
        riskGroupField = new JTextField(20);

        addField(panel, gbc, row++, "№ Договора:", contractNumberField);
        addField(panel, gbc, row++, "Тип сделки:", dealTypeField);
        addField(panel, gbc, row++, "Тип кредита:", creditTypeField);
        addField(panel, gbc, row++, "Дата заключения:", conclusionDateSpinner);
        addField(panel, gbc, row++, "Сумма:", amountField);
        addField(panel, gbc, row++, "Валюта:", currencyField);
        addField(panel, gbc, row++, "Срок (мес):", termSpinner);
        addField(panel, gbc, row++, "Дата погашения:", repaymentDateSpinner);
        addField(panel, gbc, row++, "Обеспечение:", securityField);
        addField(panel, gbc, row++, "Группа риска:", riskGroupField);

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
        contractNumberField.setText(deal.getContractNumber());
        dealTypeField.setText(deal.getDealType());
        creditTypeField.setText(deal.getCreditType());
        if (deal.getConclusionDate() != null) conclusionDateSpinner.setValue(deal.getConclusionDate());
        if (deal.getAmount() != null) amountField.setText(String.valueOf(deal.getAmount()));
        currencyField.setText(deal.getCurrency());
        if (deal.getTerm() != null) termSpinner.setValue(deal.getTerm());
        if (deal.getRepaymentDate() != null) repaymentDateSpinner.setValue(deal.getRepaymentDate());
        securityField.setText(deal.getSecurity());
        riskGroupField.setText(deal.getRiskGroup());
    }

    private void onSave() {
        try {
            deal.setContractNumber(contractNumberField.getText());
            deal.setDealType(dealTypeField.getText());
            deal.setCreditType(creditTypeField.getText());
            deal.setConclusionDate((Date) conclusionDateSpinner.getValue());
            deal.setAmount(Double.parseDouble(amountField.getText().replace(",", ".")));
            deal.setCurrency(currencyField.getText());
            deal.setTerm((Integer) termSpinner.getValue());
            deal.setRepaymentDate((Date) repaymentDateSpinner.getValue());
            deal.setSecurity(securityField.getText());
            deal.setRiskGroup(riskGroupField.getText());

            saved = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Проверьте правильность ввода суммы.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
    public CreditDeal getDeal() { return deal; }
}