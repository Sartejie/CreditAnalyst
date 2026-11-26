package org.credit.ui;

import org.credit.db.Database;
import org.credit.model.ScoringModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Экран 5: Настройка весовых коэффициентов и пороговых значений скоринговой модели (для аналитика).
 */
public class ScoringConfigDialog extends JDialog {
    private final ScoringModel scoringModel;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private boolean saved = false;

    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry("base_score", "Базовый максимальный балл"),
            Map.entry("low_risk_threshold", "Порог категории «Низкий риск» (>=") ,
            Map.entry("medium_risk_threshold", "Порог категории «Средний риск» (>=)"),
            Map.entry("pdn_warning_threshold", "Порог предупреждения ПДН (%)"),
            Map.entry("max_pdn_threshold", "Порог повышенного ПДН (%)"),
            Map.entry("critical_pdn_threshold", "Порог критического ПДН (%)"),
            Map.entry("max_loan_amount", "Лимит суммы кредита без штрафа"),
            Map.entry("max_rate_threshold", "Порог процентной ставки (%)"),
            Map.entry("max_term_threshold", "Порог срока кредита (мес.)"),
            Map.entry("max_active_deals_threshold", "Макс. число сделок без штрафа"),
            Map.entry("pdn_high_penalty", "Штраф за повышенный ПДН"),
            Map.entry("pdn_critical_penalty", "Штраф за критический ПДН"),
            Map.entry("invalid_history_penalty", "Штраф за отсутствие/невалидность истории"),
            Map.entry("bad_risk_group_penalty", "Штраф за негативную группу риска"),
            Map.entry("overdue_history_penalty", "Штраф за просрочку в истории"),
            Map.entry("many_deals_penalty", "Штраф за большое число сделок"),
            Map.entry("debt_exceeds_income_penalty", "Штраф если долг > годового дохода"),
            Map.entry("max_loan_amount_penalty", "Штраф за превышение лимита суммы"),
            Map.entry("high_dti_penalty", "Штраф за высокий DTI (>10 доходов)"),
            Map.entry("high_rate_penalty", "Штраф за высокую ставку"),
            Map.entry("long_term_penalty", "Штраф за длительный срок")
    );

    public ScoringConfigDialog(Window owner, ScoringModel scoringModel) {
        super(owner, "Настройка скоринговой модели", ModalityType.APPLICATION_MODAL);
        this.scoringModel = scoringModel;

        String[] cols = {"Параметр (ключ)", "Описание", "Значение"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        loadRows();

        initUI();
        setSize(720, 520);
        setLocationRelativeTo(owner);
    }

    private void loadRows() {
        tableModel.setRowCount(0);
        for (Map.Entry<String, Double> entry : scoringModel.getRules().entrySet()) {
            tableModel.addRow(new Object[]{
                    entry.getKey(),
                    DESCRIPTIONS.getOrDefault(entry.getKey(), entry.getKey()),
                    entry.getValue()
            });
        }
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        JLabel hint = new JLabel("Отредактируйте значения в колонке «Значение» и нажмите «Сохранить»:");
        hint.setBorder(new EmptyBorder(12, 16, 0, 16));
        add(hint, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new EmptyBorder(0, 16, 0, 16));
        add(sp, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBorder(new EmptyBorder(0, 16, 12, 16));

        JButton resetBtn = new JButton("Сбросить по умолчанию");
        resetBtn.addActionListener(e -> {
            ScoringModel def = new ScoringModel();
            scoringModel.setRules(def.getRules());
            loadRows();
        });

        JButton saveBtn = new JButton("Сохранить");
        saveBtn.addActionListener(e -> onSave());

        JButton cancelBtn = new JButton("Отмена");
        cancelBtn.addActionListener(e -> dispose());

        buttons.add(resetBtn);
        buttons.add(saveBtn);
        buttons.add(cancelBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    private void onSave() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        Map<String, Double> updated = new LinkedHashMap<>();
        try {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String key = String.valueOf(tableModel.getValueAt(i, 0));
                String rawVal = String.valueOf(tableModel.getValueAt(i, 2)).trim().replace(",", ".");
                double val = Double.parseDouble(rawVal);
                if (val < 0) {
                    throw new IllegalArgumentException("Значение параметра «" + key + "» не может быть отрицательным.");
                }
                updated.put(key, val);
            }
            scoringModel.setRules(updated);
            try (Database db = new Database()) {
                db.saveScoringRules(scoringModel.getRules());
            }
            saved = true;
            JOptionPane.showMessageDialog(this, "Настройки скоринговой модели сохранены.",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Все значения должны быть корректными числами.",
                    "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
}
