package org.credit.ui;

import com.formdev.flatlaf.FlatClientProperties;
import org.credit.model.FinalReport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

public class FinalReportDialog extends JDialog {
    public enum Decision { APPROVE, REJECT, EDIT, CANCEL }

    private Decision decision = Decision.CANCEL;

    public FinalReportDialog(Frame owner, FinalReport report) {
        super(owner, "Итоговый отчет по кредитной заявке", true);
        initUI(report);
        setSize(580, 540);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initUI(FinalReport report) {
        setLayout(new BorderLayout());

        // ===== Заголовок (стиль-класс FlatLaf "h1") =====
        JLabel title = new JLabel("Итоговый отчет");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h1");
        title.setBorder(new EmptyBorder(16, 20, 8, 20));
        add(title, BorderLayout.NORTH);

        // ===== Центральная часть с текстом отчета =====
        JTextArea textArea = new JTextArea(buildReportText(report));
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setCaretPosition(0);
        textArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        // Подхватываем цвет фона панели, чтобы поле «сливалось» с диалогом
        textArea.setBackground(UIManager.getColor("Panel.background"));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(
                UIManager.getColor("Component.borderColor"), 1));

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(new EmptyBorder(0, 16, 0, 16));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // ===== Кнопки =====
        JButton editBtn = new JButton("Вернуться к редактированию");
        editBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        editBtn.addActionListener(e -> {
            decision = Decision.EDIT;
            dispose();
        });

        JButton rejectBtn = createAccentButton("Отказать",
                "#B22222", "#D32F2F", "#8B1A1A");
        rejectBtn.addActionListener(e -> {
            decision = Decision.REJECT;
            dispose();
        });

        JButton approveBtn = createAccentButton("Одобрить кредит",
                "#2E8B57", "#3AA76D", "#26704A");
        approveBtn.addActionListener(e -> {
            decision = Decision.APPROVE;
            dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBorder(new EmptyBorder(0, 16, 8, 16));
        buttonPanel.add(editBtn);
        buttonPanel.add(rejectBtn);
        buttonPanel.add(approveBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // ===== Горячие клавиши =====
        getRootPane().setDefaultButton(approveBtn);
        getRootPane().registerKeyboardAction(
                e -> { decision = Decision.CANCEL; dispose(); },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    /** Цветная кнопка на inline-стилях FlatLaf. */
    private static JButton createAccentButton(String text, String bg, String hover, String pressed) {
        JButton btn = new JButton(text);
        btn.putClientProperty(FlatClientProperties.STYLE, String.format(
                "background: %s; foreground: #FFFFFF; " +
                        "hoverBackground: %s; pressedBackground: %s; " +
                        "borderWidth: 0; focusWidth: 0; arc: 8",
                bg, hover, pressed));
        return btn;
    }

    private static String buildReportText(FinalReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ИТОГОВЫЙ ОТЧЕТ ===\n\n");

        if (report.getSchedule() != null) {
            sb.append(String.format("Запрашиваемая сумма: %.2f\n",
                    report.getSchedule().getLoanAmount()));
            sb.append(String.format("Срок:               %d мес.\n",
                    report.getSchedule().getTermMonths()));
            sb.append(String.format("Ежемесячный платеж: %.2f\n",
                    report.getSchedule().getMonthlyPayment()));
            sb.append(String.format("Общая сумма выплат: %.2f\n\n",
                    report.getSchedule().getTotalAmount()));
        }

        if (report.getPdn() != null && report.getPdn().getValue() != null) {
            sb.append(String.format("ПДН (долговая нагрузка): %.2f %%\n",
                    report.getPdn().getValue()));
        }

        if (report.getScoring() != null) {
            sb.append(String.format("Скоринг: %d баллов\n",
                    report.getScoring().getScore()));
            sb.append(String.format("Категория риска: %s\n\n",
                    report.getScoring().getRiskCategory()));
        }

        return sb.toString();
    }

    public Decision getDecision() {
        return decision;
    }
}