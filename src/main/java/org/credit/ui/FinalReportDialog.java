package org.credit.ui;

import com.formdev.flatlaf.FlatClientProperties;
import org.credit.db.Database;
import org.credit.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Экран 4: Просмотр итогового отчёта (график, ПДН, скоринг, состояние) + сохранение в БД, экспорт PDF/XML и утверждение кредита.
 */
public class FinalReportDialog extends JDialog {
    public enum Decision { APPROVE, REJECT, EDIT, CANCEL }

    private Decision decision = Decision.CANCEL;
    private final FinalReport report;
    private final boolean allowApproval;
    private boolean savedToDb = false;

    public FinalReportDialog(Window owner, FinalReport report) {
        this(owner, report, true);
    }

    public FinalReportDialog(Window owner, FinalReport report, boolean allowApproval) {
        super(owner, "Итоговый аналитический отчёт", ModalityType.APPLICATION_MODAL);
        this.report = report;
        this.allowApproval = allowApproval;
        initUI();
        setSize(860, 640);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 8));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(12, 16, 4, 16));
        JLabel title = new JLabel("Итоговый аналитический отчёт по кредиту");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        headerPanel.add(title, BorderLayout.WEST);

        // Предупреждение о превышении порога ПДН
        if (report.getPdn() != null && report.getPdn().isThresholdExceeded()) {
            JLabel warnLabel = new JLabel(report.getPdn().getWarningMessage());
            warnLabel.setOpaque(true);
            warnLabel.setBackground(new Color(255, 232, 230));
            warnLabel.setForeground(new Color(175, 25, 25));
            warnLabel.setFont(warnLabel.getFont().deriveFont(Font.BOLD));
            warnLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 100, 100)),
                    new EmptyBorder(8, 12, 8, 12)));
            headerPanel.add(warnLabel, BorderLayout.SOUTH);
        }
        add(headerPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBorder(new EmptyBorder(0, 16, 0, 16));

        // Вкладка 1: Сводка, ПДН, Скоринг, Состояние
        JTextArea summaryArea = new JTextArea(buildSummaryText());
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        summaryArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        summaryArea.setCaretPosition(0);
        tabs.addTab("Сводка, ПДН, Скоринг и Состояние", new JScrollPane(summaryArea));

        // Вкладка 2: Таблица графика платежей
        tabs.addTab("График платежей (30/360)", createSchedulePanel());

        add(tabs, BorderLayout.CENTER);

        // Нижняя панель кнопок
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(4, 16, 12, 16));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton saveDbBtn = new JButton("Сохранить в БД");
        saveDbBtn.addActionListener(e -> onSaveToDb(saveDbBtn));
        JButton exportPdfBtn = new JButton("Экспорт PDF");
        exportPdfBtn.addActionListener(e -> onExportPdf());
        JButton exportXmlBtn = new JButton("Экспорт XML");
        exportXmlBtn.addActionListener(e -> onExportXml());

        leftActions.add(saveDbBtn);
        leftActions.add(exportPdfBtn);
        leftActions.add(exportXmlBtn);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton editBtn = new JButton(allowApproval ? "Вернуться" : "Закрыть");
        editBtn.addActionListener(e -> {
            decision = Decision.EDIT;
            dispose();
        });
        rightActions.add(editBtn);

        if (allowApproval) {
            JButton rejectBtn = createAccentButton("Отказать", "#B22222", "#D32F2F", "#8B1A1A");
            rejectBtn.addActionListener(e -> {
                decision = Decision.REJECT;
                dispose();
            });

            JButton approveBtn = createAccentButton("Утвердить кредит", "#2E8B57", "#3AA76D", "#26704A");
            approveBtn.addActionListener(e -> {
                decision = Decision.APPROVE;
                dispose();
            });

            rightActions.add(rejectBtn);
            rightActions.add(approveBtn);
            getRootPane().setDefaultButton(approveBtn);
        }

        bottomPanel.add(leftActions, BorderLayout.WEST);
        bottomPanel.add(rightActions, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        getRootPane().registerKeyboardAction(
                e -> { decision = Decision.CANCEL; dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private JPanel createSchedulePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"№", "Дата платежа", "Основной долг", "Проценты", "Итого платёж", "Остаток долга"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        if (report.getSchedule() != null && report.getSchedule().getPayments() != null) {
            List<Payment> list = report.getSchedule().getPayments();
            for (int i = 0; i < list.size(); i++) {
                Payment p = list.get(i);
                model.addRow(new Object[]{
                        p.getPeriodNumber() > 0 ? p.getPeriodNumber() : (i + 1),
                        p.getDate() != null ? df.format(p.getDate()) : "",
                        String.format("%.2f", p.getPrincipalAmount()),
                        String.format("%.2f", p.getInterestAmount()),
                        String.format("%.2f", p.getTotalAmount()),
                        String.format("%.2f", p.getRemainingBalance())
                });
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void onSaveToDb(JButton btn) {
        try (Database db = new Database()) {
            db.SaveReport(report);
            savedToDb = true;
            btn.setEnabled(false);
            btn.setText("Сохранено в БД");
            JOptionPane.showMessageDialog(this, "Отчёт успешно сохранён в базу данных.",
                    "Сохранение", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка при сохранении в БД: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onExportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Экспорт итогового отчёта в PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF файлы (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File("credit_report.pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = ensureExtension(chooser.getSelectedFile(), ".pdf");
            try {
                report.exportPDF(file);
                JOptionPane.showMessageDialog(this, "PDF-отчёт сохранён:\n" + file.getAbsolutePath(),
                        "Экспорт PDF", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка экспорта PDF: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onExportXml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Экспорт итогового отчёта в XML");
        chooser.setFileFilter(new FileNameExtensionFilter("XML файлы (*.xml)", "xml"));
        chooser.setSelectedFile(new File("credit_report.xml"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = ensureExtension(chooser.getSelectedFile(), ".xml");
            try {
                report.exportXML(file);
                JOptionPane.showMessageDialog(this, "XML-отчёт сохранён:\n" + file.getAbsolutePath(),
                        "Экспорт XML", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка экспорта XML: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static File ensureExtension(File f, String ext) {
        if (!f.getName().toLowerCase().endsWith(ext)) {
            return new File(f.getParentFile(), f.getName() + ext);
        }
        return f;
    }

    private String buildSummaryText() {
        StringBuilder sb = new StringBuilder();
        if (report.getBorrower() != null) {
            Borrower b = report.getBorrower();
            sb.append("=== 1. СВЕДЕНИЯ О ЗАЁМЩИКЕ ===\n");
            sb.append(String.format("ФИО:                    %s\n", safe(b.getFullName())));
            sb.append(String.format("Идентификационный №:    %s\n", safe(b.getIdNumber())));
            sb.append(String.format("Гражданство:            %s\n", safe(b.getCitizenship())));
            sb.append(String.format("Документ:               %s\n", safe(b.getDocument())));
            sb.append(String.format("Адрес:                  %s\n", safe(b.getAddress())));
            sb.append(String.format("Среднемесячный доход:   %.2f\n\n", b.getIncome() != null ? b.getIncome() : 0.0));
        }

        if (report.getSchedule() != null) {
            PaymentSchedule s = report.getSchedule();
            sb.append("=== 2. ПАРАМЕТРЫ И ГРАФИК КРЕДИТА ===\n");
            sb.append(String.format("Сумма кредита:          %.2f\n", val(s.getLoanAmount())));
            sb.append(String.format("Срок / Ставка:          %d мес. / %.2f%%\n",
                    s.getTermMonths() != null ? s.getTermMonths() : 0, val(s.getInterestRate())));
            sb.append(String.format("Способ возврата:        %s\n", safe(s.getMethod())));
            sb.append(String.format("Ежемесячный платёж:     %.2f\n", val(s.getMonthlyPayment())));
            sb.append(String.format("Общая сумма выплат:     %.2f\n\n", val(s.getTotalAmount())));
        }

        if (report.getPdn() != null) {
            DebtBurden p = report.getPdn();
            sb.append("=== 3. ПОКАЗАТЕЛЬ ДОЛГОВОЙ НАГРУЗКИ (ПДН) ===\n");
            sb.append(String.format("ПДН:                    %.2f %%\n", val(p.getValue())));
            sb.append(String.format("По действующим кредитам: %.2f / мес.\n", val(p.getExistingMonthlyPayments())));
            sb.append(String.format("По новому кредиту:      %.2f / мес.\n", val(p.getNewLoanMonthlyPayment())));
            sb.append(String.format("Нормативный порог:      %.2f %%\n", val(p.getThresholdPercent())));
            if (p.isThresholdExceeded()) {
                sb.append(">>> ").append(p.getWarningMessage()).append("\n");
            }
            sb.append("\n");
        }

        if (report.getScoring() != null) {
            ScoringResult sc = report.getScoring();
            sb.append("=== 4. ОЦЕНКА ВЕРОЯТНОСТИ ДЕФОЛТА (СКОРИНГ 12 МЕС.) ===\n");
            sb.append(String.format("Скоринговый балл:       %d\n", sc.getScore() != null ? sc.getScore() : 0));
            sb.append(String.format("Категория риска:        %s\n", safe(sc.getRiskCategory())));
            if (sc.getFactors() != null && !sc.getFactors().isEmpty()) {
                sb.append("Факторы расчёта:\n");
                for (String f : sc.getFactors()) {
                    sb.append("  • ").append(f).append("\n");
                }
            }
            sb.append("\n");
        }

        if (report.getState() != null) {
            CreditStateReport st = report.getState();
            sb.append("=== 5. СОСТОЯНИЕ КРЕДИТА ===\n");
            sb.append(String.format("Статус:                 %s\n", safe(st.getStatus())));
            sb.append(String.format("Остаток основного долга: %.2f\n", val(st.getOutstandingPrincipal())));
            sb.append(String.format("Начисленные проценты:   %.2f\n", val(st.getAccruedInterest())));
            sb.append(String.format("Просроченная задолж.:   %.2f (%d дн.)\n",
                    val(st.getOverdue()), st.getOverdueDays() != null ? st.getOverdueDays() : 0));
        }
        return sb.toString();
    }

    private static JButton createAccentButton(String text, String bg, String hover, String pressed) {
        JButton btn = new JButton(text);
        btn.putClientProperty(FlatClientProperties.STYLE, String.format(
                "background: %s; foreground: #FFFFFF; hoverBackground: %s; pressedBackground: %s; " +
                        "borderWidth: 0; focusWidth: 0; arc: 8", bg, hover, pressed));
        return btn;
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static double val(Double d) { return d == null ? 0.0 : d; }

    public Decision getDecision() { return decision; }
    public boolean isSavedToDb() { return savedToDb; }
}
