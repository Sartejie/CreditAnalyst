package org.credit.model;

public class Client extends User {

    // Конструктор, вызывающий конструктор родителя
    public Client(String login, String password, String role) {
        super(login, password, role);
    }

    public void viewSchedule() {
        // TODO: Логика просмотра графика платежей
    }

    public void makePayment() {
        // TODO: Логика внесения платежа
    }

    public void viewPaymentHistory() {
        // TODO: Логика просмотра истории платежей
    }
}