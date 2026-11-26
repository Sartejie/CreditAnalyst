package org.credit.model;

/**
 * Абстрактный базовый пользователь системы «Кредит-Аналитик».
 */
public abstract class User {
    private long id;
    private String login;
    private String password;
    private String role;
    private String fullName;
    private boolean authenticated;

    public User(String login, String password, String role) {
        this(0, login, password, role, login);
    }

    public User(long id, String login, String password, String role, String fullName) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
    }

    /**
     * Базовая проверка заполненности учётных данных.
     */
    public Boolean login() {
        if (login == null || login.trim().isEmpty() || password == null || password.isEmpty()) {
            this.authenticated = false;
            return false;
        }
        this.authenticated = true;
        return true;
    }

    public void logout() {
        this.authenticated = false;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
