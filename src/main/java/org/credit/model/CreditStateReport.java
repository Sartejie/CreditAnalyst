package org.credit.model;

public class CreditStateReport {

    // Приватные атрибуты (поля класса)
    // Используем класс-обертку Double, так как в UML указано с большой буквы (Double, а не double)
    private Double outstandingPrincipal;
    private Double accruedInterest;
    private Double overdueAmount;
    private String status;

    // Метод обновления отчета на основе данных по кредиту
    public void update(ActiveLoan loan) {
        // Логика обновления полей на основе переданного объекта ActiveLoan.
        // Например:
        // this.outstandingPrincipal = loan.getOutstandingPrincipal();
        // this.accruedInterest = loan.getAccruedInterest();
        // this.overdueAmount = loan.getOverdueAmount();
        // this.status = loan.getStatus();
    }

    // Публичные методы-геттеры
    public Double getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public Double getAccruedInterest() {
        return accruedInterest;
    }

    public Double getOverdue() {
        return overdueAmount;
    }

}