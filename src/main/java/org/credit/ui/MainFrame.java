package org.credit.ui;

import org.credit.db.Database;
import org.credit.model.CreditSpecialist;
import org.credit.model.ScoringModel;
import org.credit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/**
 * Главное рабочее окно системы с ролевым разграничением функционала.
 */
public class MainFrame extends JFrame {
    private final User currentUser;
    private final ScoringModel scoringModel = new ScoringModel();
    private ActiveLoansPanel activeLoansPanel;

    public MainFrame(User user) {
        this.currentUser = user;
        loadScoringModelRules();

        boolean isSpecialist = user instanceof CreditSpecialist;
        setTitle("Кредит-Аналитик — " + (isSpecialist ? "Рабочее место специалиста" : "Личный кабинет клиента"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 720);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Верхняя панель пользователя
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(8, 14, 8, 14));
        topBar.setBackground(new Color(242, 245, 250));

        JLabel userLabel = new JLabel(String.format("Пользователь: %s (%s)",
                user.getFullName() != null ? user.getFullName() : user.getLogin(),
                isSpecialist ? "Кредитный специалист" : "Клиент (заёмщик)"));
        userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD));
        topBar.add(userLabel, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);
        if (isSpecialist) {
            JButton scoringConfigBtn = new JButton("Настройка скоринга");
            scoringConfigBtn.addActionListener(e -> {
                ScoringConfigDialog dlg = new ScoringConfigDialog(this, scoringModel);
                dlg.setVisible(true);
            });
            topActions.add(scoringConfigBtn);
        }
        JButton logoutBtn = new JButton("Выйти");
        logoutBtn.addActionListener(e -> {
            currentUser.logout();
            new LoginFrame().setVisible(true);
            dispose();
        });
        topActions.add(logoutBtn);
        topBar.add(topActions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Вкладки по роли
        JTabbedPane mainTabs = new JTabbedPane();
        activeLoansPanel = new ActiveLoansPanel(currentUser, scoringModel);

        if (isSpecialist) {
            LoanApplicationPanel appPanel = new LoanApplicationPanel(
                    currentUser, scoringModel, approvedLoan -> activeLoansPanel.reloadLoans());
            mainTabs.addTab("Оценка заявки и отчёт (Форма 2501)", appPanel);
            mainTabs.addTab("Действующие кредиты", activeLoansPanel);
        } else {
            mainTabs.addTab("Мои кредиты и платежи", activeLoansPanel);
        }

        mainTabs.addChangeListener(e -> {
            if (mainTabs.getSelectedComponent() == activeLoansPanel) {
                activeLoansPanel.reloadLoans();
            }
        });

        add(mainTabs, BorderLayout.CENTER);
    }

    private void loadScoringModelRules() {
        try (Database db = new Database()) {
            Map<String, Double> saved = db.loadScoringRules();
            if (!saved.isEmpty()) {
                scoringModel.setRules(saved);
            }
        } catch (Exception ignored) {
        }
    }
}
