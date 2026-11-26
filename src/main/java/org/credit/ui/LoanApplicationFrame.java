package org.credit.ui;

import org.credit.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LoanApplicationFrame extends JFrame {
    private final List<CreditDeal> creditDealsList = new ArrayList<>();
    private final DefaultTableModel tableModel;

    private JTabbedPane tabbedPane;

    // Поля Borrower
    private JTextField fullNameField, citizenshipField, idNumberField, documentField, addressField, incomeField;
    private JSpinner birthDateSpinner;

    // Поля LoanApplication
    private JTextField amountField, rateField;
    private JComboBox<String> methodComboBox;
    private JSpinner termSpinner, appDateSpinner;

    public LoanApplicationFrame() {

        setTitle("Создание кредитной заявки");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 650);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty("JTabbedPane.tabType", "card");

        // --- Вкладка 1: Заемщик ---
        JPanel borrowerPanel = new JPanel(new GridBagLayout());
        borrowerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = createGbc();

        fullNameField = new JTextField();
        citizenshipField = new JTextField("Республика Беларусь");
        idNumberField = new JTextField();
        birthDateSpinner = createDateSpinner();
        documentField = new JTextField("Паспорт РБ");
        addressField = new JTextField();
        incomeField = new JTextField();

        int row = 0;
        addField(borrowerPanel, gbc, row++, "ФИО:", fullNameField);
        addField(borrowerPanel, gbc, row++, "Гражданство:", citizenshipField);
        addField(borrowerPanel, gbc, row++, "Номер документа:", idNumberField);
        addField(borrowerPanel, gbc, row++, "Дата рождения:", birthDateSpinner);
        addField(borrowerPanel, gbc, row++, "Документ:", documentField);
        addField(borrowerPanel, gbc, row++, "Адрес:", addressField);
        addField(borrowerPanel, gbc, row++, "Ежемесячный доход:", incomeField);

        tabbedPane.addTab("1. Заемщик", borrowerPanel);

        // --- Вкладка 2: Сделки ---
        JPanel dealsPanel = new JPanel(new BorderLayout(10, 10));
        dealsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] columns = {"№ Договора", "Сумма", "Срок", "Тип кредита"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(25);
        dealsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel dealsControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addDealBtn = new JButton("Добавить сделку");
        JButton editDealBtn = new JButton("Редактировать");
        JButton removeDealBtn = new JButton("Удалить");

        addDealBtn.addActionListener(e -> {
            CreditDealDialog dialog = new CreditDealDialog(this, null);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                creditDealsList.add(dialog.getDeal());
                updateTable();
            }
        });

        editDealBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                CreditDeal dealToEdit = creditDealsList.get(selectedRow);
                CreditDealDialog dialog = new CreditDealDialog(this, dealToEdit);
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    updateTable();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Выберите сделку для редактирования.");
            }
        });

        removeDealBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                creditDealsList.remove(selectedRow);
                updateTable();
            } else {
                JOptionPane.showMessageDialog(this, "Выберите сделку для удаления.");
            }
        });

        dealsControlPanel.add(addDealBtn);
        dealsControlPanel.add(editDealBtn);
        dealsControlPanel.add(removeDealBtn);
        dealsPanel.add(dealsControlPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("2. Сделки (Форма 2501)", dealsPanel);

        // --- Вкладка 3: Параметры заявки ---
        JPanel loanPanel = new JPanel(new GridBagLayout());
        loanPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        amountField = new JTextField();
        termSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 360, 1));
        rateField = new JTextField();

        // Создаем выпадающий список с тремя способами возврата
        String[] repaymentMethods = {
                "Аннуитетные платежи",
                "Дифференцированные платежи",
                "Платежи равными долями"
        };
        methodComboBox = new JComboBox<>(repaymentMethods);

        appDateSpinner = createDateSpinner();

        row = 0;
        addField(loanPanel, gbc, row++, "Сумма кредита:", amountField);
        addField(loanPanel, gbc, row++, "Срок (мес):", termSpinner);
        addField(loanPanel, gbc, row++, "Ставка (%):", rateField);
        addField(loanPanel, gbc, row++, "Метод погашения:", methodComboBox); // Добавляем ComboBox
        addField(loanPanel, gbc, row++, "Дата заявки:", appDateSpinner);

        tabbedPane.addTab("3. Параметры заявки", loanPanel);

        // --- Нижняя панель ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton importXmlBtn = new JButton("Импорт XML");
        importXmlBtn.addActionListener(e -> importXmlData());

        JButton submitBtn = new JButton("Отправить заявку");
        submitBtn.putClientProperty("JButton.buttonType", "default"); // FlatLaf акцентная кнопка
        submitBtn.setFont(submitBtn.getFont().deriveFont(Font.BOLD));
        submitBtn.addActionListener(e -> submitApplication());

        bottomPanel.add(importXmlBtn);
        bottomPanel.add(submitBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    }

    private void updateTable() {
        tableModel.setRowCount(0);
        for (CreditDeal deal : creditDealsList) {
            tableModel.addRow(new Object[]{
                    deal.getContractNumber(),
                    deal.getAmount() != null ? deal.getAmount() + " " + deal.getCurrency() : "",
                    deal.getTerm() != null ? deal.getTerm() + " мес." : "",
                    deal.getCreditType()
            });
        }
    }

    private void importXmlData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите XML файл с данными");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                // Парсинг XML
                javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                org.w3c.dom.Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                // 1. Заполнение данных Заемщика
                org.w3c.dom.NodeList borrowerNodes = doc.getElementsByTagName("Borrower");
                if (borrowerNodes.getLength() > 0) {
                    org.w3c.dom.Element bEl = (org.w3c.dom.Element) borrowerNodes.item(0);
                    fullNameField.setText(getTagValue("fullName", bEl));
                    citizenshipField.setText(getTagValue("citizenship", bEl));
                    idNumberField.setText(getTagValue("idNumber", bEl));
                    documentField.setText(getTagValue("document", bEl));
                    addressField.setText(getTagValue("address", bEl));
                    incomeField.setText(getTagValue("monthlyIncome", bEl));

                    String birthDateStr = getTagValue("birthDate", bEl);
                    if (birthDateStr != null && !birthDateStr.isEmpty()) {
                        birthDateSpinner.setValue(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(birthDateStr));
                    }
                }

                // 2. Заполнение Сделок
                creditDealsList.clear();
                org.w3c.dom.NodeList dealNodes = doc.getElementsByTagName("CreditDeal");
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

                for (int i = 0; i < dealNodes.getLength(); i++) {
                    org.w3c.dom.Element dEl = (org.w3c.dom.Element) dealNodes.item(i);
                    CreditDeal deal = new CreditDeal();
                    deal.setContractNumber(getTagValue("contractNumber", dEl));
                    deal.setDealType(getTagValue("dealType", dEl));
                    deal.setCreditType(getTagValue("creditType", dEl));
                    deal.setCurrency(getTagValue("currency", dEl));
                    deal.setSecurity(getTagValue("security", dEl));
                    deal.setRiskGroup(getTagValue("riskGroup", dEl));

                    String amountStr = getTagValue("amount", dEl);
                    if (amountStr != null && !amountStr.isEmpty()) deal.setAmount(Double.parseDouble(amountStr));

                    String termStr = getTagValue("term", dEl);
                    if (termStr != null && !termStr.isEmpty()) deal.setTerm(Integer.parseInt(termStr));

                    String concDate = getTagValue("conclusionDate", dEl);
                    if (concDate != null && !concDate.isEmpty()) deal.setConclusionDate(sdf.parse(concDate));

                    String repDate = getTagValue("repaymentDate", dEl);
                    if (repDate != null && !repDate.isEmpty()) deal.setRepaymentDate(sdf.parse(repDate));

                    creditDealsList.add(deal);
                }

                updateTable();

                // 3. Переключение на 3-ю вкладку (индекс 2)
                tabbedPane.setSelectedIndex(2);
                JOptionPane.showMessageDialog(this, "Данные успешно импортированы!", "Успех", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка при чтении файла: " + ex.getMessage(), "Ошибка импорта", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // Вспомогательный метод для получения значения тега
    private String getTagValue(String tag, org.w3c.dom.Element element) {
        org.w3c.dom.NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }

    private void submitApplication() {
        try {
            // 1. Сборка Borrower
            Borrower borrower = new Borrower(
                    fullNameField.getText(),
                    citizenshipField.getText(),
                    idNumberField.getText(),
                    (Date) birthDateSpinner.getValue(),
                    documentField.getText(),
                    addressField.getText(),
                    Double.parseDouble(incomeField.getText().replace(",", "."))
            );

            if (!borrower.validate()) {
                JOptionPane.showMessageDialog(this, "Проверьте данные заемщика (вкладка 1).", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 2. Сборка Form2501
            Form2501 form = new Form2501();
            form.getCreditDeals().addAll(creditDealsList);
            if (!form.validate()) {
                JOptionPane.showMessageDialog(this, "Добавьте хотя бы одну корректную сделку (вкладка 2).", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 3. Сборка LoanApplication
            LoanApplication application = new LoanApplication();
            application.setParameters(
                    Double.parseDouble(amountField.getText().replace(",", ".")),
                    (Integer) termSpinner.getValue(),
                    Double.parseDouble(rateField.getText().replace(",", ".")),
                    (String) methodComboBox.getSelectedItem(),
                    (Date) appDateSpinner.getValue()
            );

            if (!application.validate()) {
                JOptionPane.showMessageDialog(this, "Проверьте параметры заявки (вкладка 3).", "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ScoringModel scoringModel = new ScoringModel();

            // 4. Выполнение расчетов (предполагается, что методы calculateSchedule и calculateScoring есть в LoanApplication)
            PaymentSchedule schedule = application.calculateSchedule();

            // Рассчитываем долговую нагрузку
            DebtBurden pdn = new DebtBurden();
            pdn.calculate(borrower.getIncome(), schedule.getPayments());

            ScoringResult scoring = scoringModel.evaluate(form, application, pdn, borrower.getIncome());

            // 5. Формирование FinalReport и вызов диалогового окна
            FinalReport report = new FinalReport();
            report.aggregate(schedule, pdn, scoring);

            FinalReportDialog reportDialog = new FinalReportDialog(this, report);
            reportDialog.setVisible(true);

            // 6. Обработка решения пользователя
            FinalReportDialog.Decision decision = reportDialog.getDecision();
            switch (decision) {
                case APPROVE:
                    JOptionPane.showMessageDialog(this, "Кредит успешно одобрен!", "Одобрено", JOptionPane.INFORMATION_MESSAGE);
                    // Здесь можно сохранить заявку в БД или закрыть окно
                    // dispose();
                    break;
                case REJECT:
                    JOptionPane.showMessageDialog(this, "В выдаче кредита отказано.", "Отказ", JOptionPane.WARNING_MESSAGE);
                    // dispose();
                    break;
                case EDIT:
                case CANCEL:
                default:
                    // Пользователь вернулся к редактированию или закрыл окно, ничего не делаем
                    break;
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Убедитесь, что числовые поля заполнены верно.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private void addField(JPanel p, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel(label), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(comp, gbc);
    }

    private JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd.MM.yyyy"));
        return spinner;
    }
}