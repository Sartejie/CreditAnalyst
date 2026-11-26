package org.credit.model;

public class User {
    private String login;
    private String password;
    private String role;

    // Конструктор (добавлен для удобства, в UML не указан)
    public User(String login, String password, String role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }

    public Boolean login() {
        // TODO: Логика авторизации (например, проверка логина и пароля)
        return false;
    }

    public void logout() {
        // TODO: Логика выхода из системы
    }

    // Геттеры и сеттеры (добавлены для доступа к private полям)
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}