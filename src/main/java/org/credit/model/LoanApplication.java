package org.credit.model;

import java.util.Date;

public class LoanApplication {
    private Double amount;
    private Integer term;
    private Double rate;
    private String repaymentMethod;
    private Date applicationDate;

    public LoanApplication() {}

    public void setParameters(Double amount, Integer term, Double rate, String repaymentMethod, Date applicationDate) {
        this.amount = amount;
        this.term = term;
        this.rate = rate;
        this.repaymentMethod = repaymentMethod;
        this.applicationDate = applicationDate;
    }

    public Boolean validate() {
        if (amount == null || amount <= 0) return false;
        if (term == null || term <= 0) return false;
        if (rate == null || rate < 0) return false;
        if (repaymentMethod == null || repaymentMethod.trim().isEmpty()) return false;
        if (applicationDate == null) return false;
        return true;
    }

    public PaymentSchedule calculateSchedule() {
        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setLoanAmount(amount);
        schedule.setTermMonths(term);
        schedule.setInterestRate(rate);
        schedule.generate(repaymentMethod);
        return schedule;
    }

    // Геттеры для тестов
    public Double getAmount() { return amount; }
    public Integer getTerm() { return term; }
    public Double getRate() { return rate; }
    public String getRepaymentMethod() { return repaymentMethod; }
    public Date getApplicationDate() { return applicationDate; }
}