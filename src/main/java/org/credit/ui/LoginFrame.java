package org.credit.ui;

import com.formdev.flatlaf.FlatClientProperties;
import org.credit.db.Database;
import org.credit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Экран 1: Авторизация пользователей (CreditSpecialist / Client).
 */
public class LoginFrame extends JFrame {
    private final JTextField loginField;
    private final JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Вход в систему «Кредит-Аналитик»");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 290);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("ИС «Кредит-Аналитик»");
        title.putClientProperty(FlatClientProperties.STYLE_CLASS, "h2");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        loginField = new JTextField("analyst", 18);
        passwordField = new JPasswordField("analyst123", 18);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Логин:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(loginField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        form.add(new JLabel("Пароль:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        form.add(passwordField, gbc);

        JLabel hint = new JLabel("<html><small>Учётные записи по умолчанию:<br>" +
                "• Специалист: <b>analyst</b> / <b>analyst123</b><br>" +
                "• Клиент: <b>client</b> / <b>client123</b></small></html>");
        hint.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        form.add(hint, gbc);

        root.add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton loginBtn = new JButton("Войти");
        loginBtn.putClientProperty("JButton.buttonType", "default");
        loginBtn.addActionListener(e -> onLogin());
        buttons.add(loginBtn);
        root.add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(loginBtn);
        setContentPane(root);
    }

    private void onLogin() {
        String login = loginField.getText().trim();
        String password = new String(passwordField.getPassword());

        try (Database db = new Database()) {
            User user = db.authenticate(login, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Неверный логин или пароль.",
                        "Ошибка входа", JOptionPane.ERROR_MESSAGE);
                return;
            }
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка подключения к БД: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
