package org.credit.ui;

import org.credit.db.Database;
import org.credit.model.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Экран 6: Список действующих кредитов (UC-06, UC-07) с фильтрацией и поиском.
 */
public class ActiveLoansPanel extends JPanel {
    private final User currentUser;
    private final ScoringModel scoringModel;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField;
    private final JComboBox<String> statusFilterBox;
    private List<ActiveLoan> displayedLoans = new ArrayList<>();

    public ActiveLoansPanel(User currentUser, ScoringModel scoringModel) {
        this.currentUser = currentUser;
        this.scoringModel = scoringModel != null ? scoringModel : new ScoringModel();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Панель фильтрации
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchField = new JTextField(20);
        statusFilterBox = new JComboBox<>(new String[]{
                "Все статусы",
                ActiveLoan.STATUS_ACTIVE,
                ActiveLoan.STATUS_OVERDUE,
                ActiveLoan.STATUS_EARLY_REPAID,
                ActiveLoan.STATUS_CLOSED
        });
        JButton searchBtn = new JButton("Найти / Обновить");
        searchBtn.addActionListener(e -> reloadLoans());
        statusFilterBox.addActionListener(e -> reloadLoans());

        filterPanel.add(new JLabel("Поиск (договор / ФИО):"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Статус:"));
        filterPanel.add(statusFilterBox);
        filterPanel.add(searchBtn);
        add(filterPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Договор", "Заёмщик", "Статус", "Остаток ОД", "Просрочка", "Ставка %", "Срок", "След. платёж"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton openBtn = new JButton("Открыть сопровождение (UC-07)");
        openBtn.addActionListener(e -> openSelectedServicing());
        JButton payBtn = new JButton("Зарегистрировать платёж (UC-02)");
        payBtn.addActionListener(e -> paySelectedLoan());
        actionsPanel.add(openBtn);
        actionsPanel.add(payBtn);

        if (currentUser instanceof CreditSpecialist) {
            JButton modifyBtn = new JButton("Изменить параметры (UC-05)");
            modifyBtn.addActionListener(e -> modifySelectedLoan());
            actionsPanel.add(modifyBtn);
        }

        add(actionsPanel, BorderLayout.SOUTH);
        reloadLoans();
    }

    public void reloadLoans() {
        tableModel.setRowCount(0);
        displayedLoans = new ArrayList<>();
        String query = searchField.getText().trim().toLowerCase();
        String statusFilter = String.valueOf(statusFilterBox.getSelectedItem());
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");

        try (Database db = new Database()) {
            List<ActiveLoan> source = (currentUser instanceof Client)
                    ? db.getLoansByClient(currentUser.getLogin())
                    : db.getAllLoans();

            for (ActiveLoan loan : source) {
                if (!"Все статусы".equals(statusFilter) && !statusFilter.equals(loan.getStatus())) {
                    continue;
                }
                String contract = loan.getContractNumber() != null ? loan.getContractNumber().toLowerCase() : "";
                String name = loan.getBorrowerName() != null ? loan.getBorrowerName().toLowerCase() : "";
                if (!query.isEmpty() && !contract.contains(query) && !name.contains(query)) {
                    continue;
                }
                displayedLoans.add(loan);
                tableModel.addRow(new Object[]{
                        loan.getId(),
                        loan.getContractNumber(),
                        loan.getBorrowerName() != null ? loan.getBorrowerName() : loan.getClientLogin(),
                        loan.getStatus(),
                        String.format("%.2f", loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : 0.0),
                        String.format("%.2f", loan.getOverdueAmount() != null ? loan.getOverdueAmount() : 0.0),
                        String.format("%.2f", loan.getRate() != null ? loan.getRate() : 0.0),
                        loan.getTerm() != null ? loan.getTerm() + " мес." : "",
                        loan.getNextPaymentDate() != null ? df.format(loan.getNextPaymentDate()) : "—"
                });
            }
        }
    }

    private ActiveLoan getSelectedLoan() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayedLoans.size()) {
            JOptionPane.showMessageDialog(this, "Выберите кредит из таблицы.", "Выбор кредита", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return displayedLoans.get(row);
    }

    private void openSelectedServicing() {
        ActiveLoan loan = getSelectedLoan();
        if (loan == null) return;
        LoanServicingDialog dlg = new LoanServicingDialog(SwingUtilities.getWindowAncestor(this), loan, currentUser, scoringModel);
        dlg.setVisible(true);
        if (dlg.isUpdated()) reloadLoans();
    }

    private void paySelectedLoan() {
        ActiveLoan loan = getSelectedLoan();
        if (loan == null) return;
        if (!loan.isOpen()) {
            JOptionPane.showMessageDialog(this, "Кредит уже закрыт (" + loan.getStatus() + ").",
                    "Операция недоступна", JOptionPane.WARNING_MESSAGE);
            return;
        }
        RegisterPaymentDialog dlg = new RegisterPaymentDialog(SwingUtilities.getWindowAncestor(this), loan);
        dlg.setVisible(true);
        if (dlg.isPaymentRegistered()) reloadLoans();
    }

    private void modifySelectedLoan() {
        ActiveLoan loan = getSelectedLoan();
        if (loan == null) return;
        if (!loan.isOpen()) {
            JOptionPane.showMessageDialog(this, "Нельзя изменить параметры закрытого кредита.",
                    "Операция недоступна", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ModifyLoanParamsDialog dlg = new ModifyLoanParamsDialog(SwingUtilities.getWindowAncestor(this), loan, scoringModel);
        dlg.setVisible(true);
        if (dlg.isApplied()) reloadLoans();
    }
}
