package org.credit.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PaymentSchedule {
    private Double loanAmount;
    private Integer termMonths;
    private Double interestRate; // Ставка в % годовых
    private String method;
    private Double monthlyPayment; // Для аннуитета - фиксированный, для дифф. - первый платеж
    private Double totalAmount;

    // Согласно UML-диаграмме
    private List<Payment> payments = new ArrayList<>();

    public PaymentSchedule() {}

    // Метод из UML
    public void generate(String method) {
        this.method = method;
        this.payments.clear();

        if (loanAmount == null || termMonths == null || interestRate == null || termMonths <= 0) {
            return;
        }

        double monthlyRate = interestRate / 100.0 / 12.0;
        double balance = loanAmount;
        double totalSum = 0;

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // Начало графика - сегодня (или можно передать дату заявки)

        double annPayment = 0;
        if (monthlyRate == 0) {
            annPayment = loanAmount / termMonths;
        } else {
            annPayment = loanAmount * monthlyRate / (1 - Math.pow(1 + monthlyRate, -termMonths));
        }
        this.monthlyPayment = Math.round(annPayment * 100.0) / 100.0;

        for (int i = 1; i <= termMonths; i++) {
            double principal;
            double interest;

            if (method.equals("Аннуитетные платежи")) {
                // Аннуитет: равные общие платежи, доля процентов уменьшается
                interest = balance * monthlyRate;
                principal = annPayment - interest;
                if (i == termMonths) { // Корректировка последнего платежа
                    principal = balance;
                }
            } else {
                // Дифференцированные и "Равными долями":
                // Основной долг гасится равными частями, проценты начисляются на остаток
                principal = loanAmount / termMonths;
                if (i == termMonths) { // Корректировка последнего платежа
                    principal = balance;
                }
                interest = balance * monthlyRate;
            }

            double paymentSum = principal + interest;
            balance -= principal;
            totalSum += paymentSum;

            // Округление до копеек
            paymentSum = Math.round(paymentSum * 100.0) / 100.0;
            principal = Math.round(principal * 100.0) / 100.0;
            interest = Math.round(interest * 100.0) / 100.0;
            double roundedBalance = Math.round(balance * 100.0) / 100.0;

            cal.add(Calendar.MONTH, 1);
            Date paymentDate = cal.getTime();

            payments.add(new Payment(paymentDate, paymentSum, principal, interest, roundedBalance));
        }

        this.totalAmount = Math.round(totalSum * 100.0) / 100.0;
    }

    // Метод из UML
    public List<Payment> getPayments() {
        return payments;
    }
    public String getMethod() {
        return method;
    }
    // Сеттеры и геттеры, необходимые для интеграции с LoanApplication
    public void setLoanAmount(Double loanAmount) { this.loanAmount = loanAmount; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    public void setMethod(String method) { this.method = method; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }
    public void setMonthlyPayment(Double monthlyPayment) { this.monthlyPayment = monthlyPayment; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getMonthlyPayment() { return monthlyPayment; }
    public Double getTotalAmount() { return totalAmount; }
    public Double getLoanAmount() { return loanAmount; }
    public Integer getTermMonths() { return termMonths; }
    public Double getInterestRate() { return interestRate; }
}