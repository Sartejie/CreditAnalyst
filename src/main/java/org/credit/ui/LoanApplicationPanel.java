package org.credit.ui;

import org.credit.db.Database;
import org.credit.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * Экраны 2 и 3: Импорт (XML/JSON) и ручной ввод Формы 2501, ввод параметров заявки, валидация и расчёт.
 */
public class LoanApplicationPanel extends JPanel {
    private final User currentUser;
    private final ScoringModel scoringModel;
    private final Consumer<ActiveLoan> onLoanApproved;

    private final List<CreditDeal> creditDealsList = new ArrayList<>();
    private final List<OverdraftInfo> overdraftsList = new ArrayList<>();
    private final DefaultTableModel dealsTableModel;
    private final DefaultTableModel overdraftTableModel;

    private final JTabbedPane tabbedPane;

    // Поля Borrower (структура Формы 2501 РБ)
    private JTextField lastNameField, firstNameField, middleNameField;
    private JTextField citizenshipField, idNumberField;
    private JSpinner birthDateSpinner;
    private JTextField docTypeField, docSeriesField;
    private JSpinner docIssueDateSpinner;
    private JTextField countryField, regionField, districtField, localityField;
    private JTextField incomeField;
    private JComboBox<String> clientLoginBox;

    // Поля LoanApplication
    private JTextField amountField, rateField, pdnThresholdField;
    private JComboBox<String> methodComboBox;
    private JSpinner termSpinner, appDateSpinner;

    public LoanApplicationPanel(User currentUser, ScoringModel scoringModel, Consumer<ActiveLoan> onLoanApproved) {
        this.currentUser = currentUser;
        this.scoringModel = scoringModel != null ? scoringModel : new ScoringModel();
        this.onLoanApproved = onLoanApproved;

        setLayout(new BorderLayout(8, 8));
        tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty("JTabbedPane.tabType", "card");

        // --- Вкладка 1: Заёмщик ---
        JPanel borrowerPanel = new JPanel(new GridBagLayout());
        borrowerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints gbc = createGbc();

        lastNameField = new JTextField();
        firstNameField = new JTextField();
        middleNameField = new JTextField();
        citizenshipField = new JTextField("Республика Беларусь");
        idNumberField = new JTextField();
        birthDateSpinner = createDateSpinner();
        docTypeField = new JTextField("Паспорт гражданина РБ");
        docSeriesField = new JTextField();
        docIssueDateSpinner = createDateSpinner();
        countryField = new JTextField("Республика Беларусь");
        regionField = new JTextField();
        districtField = new JTextField();
        localityField = new JTextField();
        incomeField = new JTextField();
        clientLoginBox = new JComboBox<>();
        loadClientLogins();

        int row = 0;
        addField(borrowerPanel, gbc, row++, "Фамилия*:", lastNameField);
        addField(borrowerPanel, gbc, row++, "Собственное имя*:", firstNameField);
        addField(borrowerPanel, gbc, row++, "Отчество:", middleNameField);
        addField(borrowerPanel, gbc, row++, "Гражданство:", citizenshipField);
        addField(borrowerPanel, gbc, row++, "Идентификационный номер*:", idNumberField);
        addField(borrowerPanel, gbc, row++, "Дата рождения*:", birthDateSpinner);
        addField(borrowerPanel, gbc, row++, "Документ (вид):", docTypeField);
        addField(borrowerPanel, gbc, row++, "Серия и номер документа:", docSeriesField);
        addField(borrowerPanel, gbc, row++, "Дата выдачи документа:", docIssueDateSpinner);
        addField(borrowerPanel, gbc, row++, "Страна:", countryField);
        addField(borrowerPanel, gbc, row++, "Область / Район:", createTwoFieldRow(regionField, districtField));
        addField(borrowerPanel, gbc, row++, "Населённый пункт:", localityField);
        addField(borrowerPanel, gbc, row++, "Среднемесячный доход*:", incomeField);
        addField(borrowerPanel, gbc, row++, "Учётная запись клиента:", clientLoginBox);

        tabbedPane.addTab("1. Заёмщик", new JScrollPane(borrowerPanel));

        // --- Вкладка 2: Сделки и овердрафты (Форма 2501) ---
        JPanel formPanel = new JPanel(new BorderLayout(8, 8));
        formPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        String[] dealCols = {"№ Договора", "Сумма", "Срок", "Тип", "Группа риска", "Просрочка (дн.)"};
        dealsTableModel = new DefaultTableModel(dealCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable dealsTable = new JTable(dealsTableModel);
        dealsTable.setRowHeight(24);

        JPanel dealsSection = new JPanel(new BorderLayout(6, 6));
        dealsSection.setBorder(BorderFactory.createTitledBorder("Кредитные сделки (CreditDeal)"));
        dealsSection.add(new JScrollPane(dealsTable), BorderLayout.CENTER);

        JPanel dealsBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addDealBtn = new JButton("Добавить сделку");
        JButton editDealBtn = new JButton("Редактировать");
        JButton removeDealBtn = new JButton("Удалить");

        addDealBtn.addActionListener(e -> {
            CreditDealDialog dlg = new CreditDealDialog(SwingUtilities.getWindowAncestor(this), null);
            dlg.setVisible(true);
            if (dlg.isSaved()) {
                creditDealsList.add(dlg.getDeal());
                updateDealsTable();
            }
        });
        editDealBtn.addActionListener(e -> {
            int sel = dealsTable.getSelectedRow();
            if (sel >= 0) {
                CreditDealDialog dlg = new CreditDealDialog(SwingUtilities.getWindowAncestor(this), creditDealsList.get(sel));
                dlg.setVisible(true);
                if (dlg.isSaved()) updateDealsTable();
            } else {
                JOptionPane.showMessageDialog(this, "Выберите сделку для редактирования.");
            }
        });
        removeDealBtn.addActionListener(e -> {
            int sel = dealsTable.getSelectedRow();
            if (sel >= 0) {
                creditDealsList.remove(sel);
                updateDealsTable();
            }
        });
        dealsBtns.add(addDealBtn);
        dealsBtns.add(editDealBtn);
        dealsBtns.add(removeDealBtn);
        dealsSection.add(dealsBtns, BorderLayout.SOUTH);

        // Овердрафты
        String[] ovCols = {"Дата договора", "Лимит овердрафта", "Дата установления лимита"};
        overdraftTableModel = new DefaultTableModel(ovCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable ovTable = new JTable(overdraftTableModel);
        ovTable.setRowHeight(22);

        JPanel ovSection = new JPanel(new BorderLayout(6, 6));
        ovSection.setBorder(BorderFactory.createTitledBorder("Сведения об овердрафте (при наличии)"));
        ovSection.setPreferredSize(new Dimension(0, 150));
        ovSection.add(new JScrollPane(ovTable), BorderLayout.CENTER);

        JPanel ovBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addOvBtn = new JButton("Добавить овердрафт");
        JButton removeOvBtn = new JButton("Удалить овердрафт");
        addOvBtn.addActionListener(e -> onAddOverdraft());
        removeOvBtn.addActionListener(e -> {
            int sel = ovTable.getSelectedRow();
            if (sel >= 0) {
                overdraftsList.remove(sel);
                updateOverdraftTable();
            }
        });
        ovBtns.add(addOvBtn);
        ovBtns.add(removeOvBtn);
        ovSection.add(ovBtns, BorderLayout.SOUTH);

        formPanel.add(dealsSection, BorderLayout.CENTER);
        formPanel.add(ovSection, BorderLayout.SOUTH);
        tabbedPane.addTab("2. Кредитная история (Форма 2501)", formPanel);

        // --- Вкладка 3: Параметры заявки ---
        JPanel loanPanel = new JPanel(new GridBagLayout());
        loanPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        amountField = new JTextField();
        termSpinner = new JSpinner(new SpinnerNumberModel(12, 1, 360, 1));
        rateField = new JTextField("14.5");
        methodComboBox = new JComboBox<>(new String[]{
                PaymentSchedule.METHOD_ANNUITY,
                PaymentSchedule.METHOD_DIFFERENTIATED,
                PaymentSchedule.METHOD_LUMP_SUM_MONTHLY_INTEREST,
                PaymentSchedule.METHOD_LUMP_SUM_END_INTEREST
        });
        appDateSpinner = createDateSpinner();
        pdnThresholdField = new JTextField(String.valueOf(
                this.scoringModel.getRules().getOrDefault("pdn_warning_threshold", DebtBurden.DEFAULT_THRESHOLD_PERCENT)));

        row = 0;
        addField(loanPanel, gbc, row++, "Сумма кредита*:", amountField);
        addField(loanPanel, gbc, row++, "Срок (мес.)*:", termSpinner);
        addField(loanPanel, gbc, row++, "Процентная ставка (% годовых)*:", rateField);
        addField(loanPanel, gbc, row++, "Способ возврата (Инструкция № 488):", methodComboBox);
        addField(loanPanel, gbc, row++, "Дата заявки*:", appDateSpinner);
        addField(loanPanel, gbc, row++, "Нормативный порог ПДН (%):", pdnThresholdField);

        tabbedPane.addTab("3. Параметры кредита", loanPanel);
        add(tabbedPane, BorderLayout.CENTER);

        // --- Нижняя панель ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton importXmlBtn = new JButton("Импорт XML (Форма 2501)");
        importXmlBtn.addActionListener(e -> importFile(true));
        JButton importJsonBtn = new JButton("Импорт JSON (Форма 2501)");
        importJsonBtn.addActionListener(e -> importFile(false));
        JButton submitBtn = new JButton("Рассчитать и сформировать отчёт");
        submitBtn.putClientProperty("JButton.buttonType", "default");
        submitBtn.setFont(submitBtn.getFont().deriveFont(Font.BOLD));
        submitBtn.addActionListener(e -> submitApplication());

        bottomPanel.add(importXmlBtn);
        bottomPanel.add(importJsonBtn);
        bottomPanel.add(submitBtn);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadClientLogins() {
        clientLoginBox.removeAllItems();
        try (Database db = new Database()) {
            for (String login : db.getClientLogins()) {
                clientLoginBox.addItem(login);
            }
        } catch (Exception ignored) {
            clientLoginBox.addItem("client");
        }
    }

    private JPanel createTwoFieldRow(JTextField left, JTextField right) {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.add(left);
        p.add(right);
        return p;
    }

    private void onAddOverdraft() {
        JSpinner cDate = createDateSpinner();
        JTextField limitF = new JTextField("1000.00");
        JSpinner lDate = createDateSpinner();
        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        p.add(new JLabel("Дата заключения договора:")); p.add(cDate);
        p.add(new JLabel("Лимит овердрафта:")); p.add(limitF);
        p.add(new JLabel("Дата установления лимита:")); p.add(lDate);

        if (JOptionPane.showConfirmDialog(this, p, "Добавление овердрафта",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                double limit = Double.parseDouble(limitF.getText().trim().replace(",", "."));
                OverdraftInfo info = new OverdraftInfo((Date) cDate.getValue(), limit, (Date) lDate.getValue());
                List<ValidationError> errs = info.validateDetailed(overdraftsList.size() + 1);
                if (!errs.isEmpty()) {
                    JOptionPane.showMessageDialog(this, errs.get(0).toString(), "Ошибка валидации", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                overdraftsList.add(info);
                updateOverdraftTable();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Некорректный лимит овердрафта.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importFile(boolean xml) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(xml ? "Выберите XML файл Формы 2501" : "Выберите JSON файл Формы 2501");
        chooser.setFileFilter(new FileNameExtensionFilter(
                xml ? "XML файлы (*.xml)" : "JSON файлы (*.json)", xml ? "xml" : "json"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            Form2501 form = new Form2501();
            if (xml) form.importXML(file); else form.importJSON(file);

            if (form.getImportedBorrower() != null) {
                populateBorrowerFields(form.getImportedBorrower());
            }
            creditDealsList.clear();
            creditDealsList.addAll(form.getCreditDeals());
            overdraftsList.clear();
            overdraftsList.addAll(form.getOverdrafts());

            updateDealsTable();
            updateOverdraftTable();
            tabbedPane.setSelectedIndex(0);
            JOptionPane.showMessageDialog(this,
                    "Данные импортированы. Проверьте и при необходимости откорректируйте поля на вкладках 1 и 2.",
                    "Импорт выполнен", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при импорте файла: " + ex.getMessage(),
                    "Ошибка импорта", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateBorrowerFields(Borrower b) {
        lastNameField.setText(safe(b.getLastName()));
        firstNameField.setText(safe(b.getFirstName()));
        middleNameField.setText(safe(b.getMiddleName()));
        if (b.getCitizenship() != null) citizenshipField.setText(b.getCitizenship());
        idNumberField.setText(safe(b.getIdNumber()));
        if (b.getBirthDate() != null) birthDateSpinner.setValue(b.getBirthDate());

        docTypeField.setText(safe(b.getDocumentType() != null ? b.getDocumentType() : b.getDocument()));
        docSeriesField.setText(safe(b.getDocumentSeriesNumber()));
        if (b.getDocumentIssueDate() != null) docIssueDateSpinner.setValue(b.getDocumentIssueDate());

        countryField.setText(safe(b.getCountry() != null ? b.getCountry() : "Республика Беларусь"));
        regionField.setText(safe(b.getRegion()));
        districtField.setText(safe(b.getDistrict()));
        localityField.setText(safe(b.getLocality() != null ? b.getLocality() : b.getAddress()));
        if (b.getMonthlyIncome() != null) incomeField.setText(String.valueOf(b.getMonthlyIncome()));
    }

    private void updateDealsTable() {
        dealsTableModel.setRowCount(0);
        for (CreditDeal deal : creditDealsList) {
            dealsTableModel.addRow(new Object[]{
                    deal.getContractNumber(),
                    deal.getAmount() != null ? String.format("%.2f %s", deal.getAmount(), safe(deal.getCurrency())) : "",
                    deal.getTerm() != null ? deal.getTerm() + " мес." : "",
                    deal.getCreditType(),
                    deal.getRiskGroup(),
                    deal.getOverdueDays() != null ? deal.getOverdueDays() : 0
            });
        }
    }

    private void updateOverdraftTable() {
        overdraftTableModel.setRowCount(0);
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        for (OverdraftInfo ov : overdraftsList) {
            overdraftTableModel.addRow(new Object[]{
                    ov.getContractDate() != null ? df.format(ov.getContractDate()) : "",
                    ov.getLimitAmount() != null ? String.format("%.2f", ov.getLimitAmount()) : "",
                    ov.getLimitSetDate() != null ? df.format(ov.getLimitSetDate()) : ""
            });
        }
    }

    private void submitApplication() {
        List<ValidationError> errors = new ArrayList<>();

        Borrower borrower = new Borrower();
        borrower.setLastName(lastNameField.getText());
        borrower.setFirstName(firstNameField.getText());
        borrower.setMiddleName(middleNameField.getText());
        borrower.setCitizenship(citizenshipField.getText());
        borrower.setIdNumber(idNumberField.getText());
        borrower.setBirthDate((Date) birthDateSpinner.getValue());
        borrower.setDocumentType(docTypeField.getText());
        borrower.setDocumentSeriesNumber(docSeriesField.getText());
        borrower.setDocumentIssueDate((Date) docIssueDateSpinner.getValue());
        borrower.setCountry(countryField.getText());
        borrower.setRegion(regionField.getText());
        borrower.setDistrict(districtField.getText());
        borrower.setLocality(localityField.getText());
        borrower.setClientLogin(clientLoginBox.getSelectedItem() != null ? String.valueOf(clientLoginBox.getSelectedItem()) : "client");

        String rawIncome = incomeField.getText().trim().replace(",", ".");
        if (rawIncome.isEmpty()) {
            errors.add(new ValidationError("Заёмщик", "Среднемесячный доход", "обязателен для заполнения"));
        } else {
            try {
                borrower.setMonthlyIncome(Double.parseDouble(rawIncome));
            } catch (NumberFormatException ex) {
                errors.add(new ValidationError("Заёмщик", "Среднемесячный доход", "должен быть корректным числом"));
            }
        }
        errors.addAll(borrower.validateDetailed());

        Form2501 form = new Form2501();
        form.getCreditDeals().addAll(creditDealsList);
        form.getOverdrafts().addAll(overdraftsList);
        errors.addAll(form.validateDetailed());

        LoanApplication application = new LoanApplication();
        Double reqAmount = parseOptionalDouble(amountField.getText(), "Параметры заявки", "Сумма кредита", errors);
        Double reqRate = parseOptionalDouble(rateField.getText(), "Параметры заявки", "Процентная ставка", errors);
        Double pdnThreshold = parseOptionalDouble(pdnThresholdField.getText(), "Параметры заявки", "Порог ПДН", errors);
        application.setParameters(
                reqAmount,
                (Integer) termSpinner.getValue(),
                reqRate,
                (String) methodComboBox.getSelectedItem(),
                (Date) appDateSpinner.getValue()
        );
        if (currentUser != null) application.setCreatedBy(currentUser.getLogin());
        errors.addAll(application.validateDetailed());

        if (!errors.isEmpty()) {
            StringBuilder msg = new StringBuilder("Обнаружены ошибки заполнения данных (расчёты заблокированы):\n\n");
            for (ValidationError err : errors) {
                msg.append("• ").append(err.toString()).append("\n");
            }
            JOptionPane.showMessageDialog(this, msg.toString(), "Ошибка валидации (UC-13)", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (creditDealsList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Кредитные сделки в Форме 2501 отсутствуют. Согласно политике скоринга (п. 4.4 ТЗ), при неполных данных будет применена консервативная оценка «Высокий риск».",
                    "Предупреждение о неполных данных", JOptionPane.INFORMATION_MESSAGE);
        }

        double threshold = pdnThreshold != null && pdnThreshold > 0 ? pdnThreshold : DebtBurden.DEFAULT_THRESHOLD_PERCENT;
        List<ActiveLoan> existingDbLoans = new ArrayList<>();
        try (Database db = new Database()) {
            existingDbLoans = db.getLoansByClient(borrower.getClientLogin());
        } catch (Exception ignored) {
        }

        FinalReport report = application.runAllCalculations(borrower, form, scoringModel, existingDbLoans, threshold);
        if (currentUser != null) report.setCreatedBy(currentUser.getLogin());

        FinalReportDialog dialog = new FinalReportDialog(SwingUtilities.getWindowAncestor(this), report, true);
        dialog.setVisible(true);

        if (dialog.getDecision() == FinalReportDialog.Decision.APPROVE) {
            approveAndOpenServicing(borrower, form, application, report);
        } else if (dialog.getDecision() == FinalReportDialog.Decision.REJECT) {
            JOptionPane.showMessageDialog(this, "В выдаче кредита отказано.", "Отказ", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void approveAndOpenServicing(Borrower borrower, Form2501 form, LoanApplication application, FinalReport report) {
        try (Database db = new Database()) {
            long bId = db.saveBorrower(borrower);
            long fId = db.saveForm2501(bId, form);
            application.setBorrowerId(bId);
            application.setFormId(fId);
            application.setStatus(ActiveLoan.STATUS_ACTIVE);
            long appId = db.saveApplication(application);
            report.setApplicationId(appId);
            db.SaveReport(report);

            ActiveLoan activeLoan = ActiveLoan.fromReport(
                    report, borrower, borrower.getClientLogin(),
                    currentUser != null ? currentUser.getLogin() : "analyst");
            activeLoan.setBorrowerId(bId);
            activeLoan.setFormId(fId);
            activeLoan.setApplicationId(appId);
            db.SaveLoan(activeLoan);

            JOptionPane.showMessageDialog(this,
                    "Кредит утверждён! Создан договор " + activeLoan.getContractNumber(),
                    "Утверждение кредита (UC-15)", JOptionPane.INFORMATION_MESSAGE);

            if (onLoanApproved != null) {
                onLoanApproved.accept(activeLoan);
            }
            LoanServicingDialog servicingDialog = new LoanServicingDialog(
                    SwingUtilities.getWindowAncestor(this), activeLoan, currentUser, scoringModel);
            servicingDialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при утверждении кредита: " + ex.getMessage(),
                    "Ошибка БД", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Double parseOptionalDouble(String raw, String section, String field, List<ValidationError> errors) {
        String s = raw == null ? "" : raw.trim().replace(",", ".");
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(section, field, "должно быть корректным числом"));
            return null;
        }
    }

    private GridBagConstraints createGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
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

    private static String safe(String s) { return s == null ? "" : s; }
}
