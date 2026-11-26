package org.credit.model;

public class CreditSpecialist extends User {

    // Конструктор, вызывающий конструктор родителя
    public CreditSpecialist(String login, String password, String role) {
        super(login, password, role);
    }

    public void inputData() {
        // TODO: Логика ввода данных
    }

    public void runCalculations() {
        // TODO: Логика проведения расчетов
    }

    public void approveLoan() {
        // TODO: Логика одобрения кредита
    }

    public void modifyLoanParams() {
        // TODO: Логика изменения параметров кредита
    }
}