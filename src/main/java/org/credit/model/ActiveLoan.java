
package org.credit.model;

import java.util.Map;

public class ActiveLoan {

    private long id;
    private long applicationId;

    private String status;
    private Double outstandingPrincipal;
    private Double accruedInterest;
    private Double overdueAmount;
    private Double rate;
    private Integer term;
    private String repaymentMethod;

    public ActiveLoan() {}

    public ActiveLoan(long id, long applicationId, String status,
                      Double outstandingPrincipal, Double accruedInterest,
                      Double overdueAmount, Double rate, Integer term,
                      String repaymentMethod) {
        this.id = id;
        this.applicationId = applicationId;
        this.status = status;
        this.outstandingPrincipal = outstandingPrincipal;
        this.accruedInterest = accruedInterest;
        this.overdueAmount = overdueAmount;
        this.rate = rate;
        this.term = term;
        this.repaymentMethod = repaymentMethod;
    }

    public void registerPayment(Payment payment) {
        // Логика обработки платежа
    }

    public void recalculate() {
        // Логика перерасчета
    }

    public void changeParameters(Map<String, Object> params) {
        if (params.containsKey("rate"))
            this.rate = (Double) params.get("rate");

        if (params.containsKey("term"))
            this.term = (Integer) params.get("term");

        if (params.containsKey("repaymentMethod"))
            this.repaymentMethod = (String) params.get("repaymentMethod");

        recalculate();
    }

    public Double getBalance() {
        return (outstandingPrincipal == null ? 0 : outstandingPrincipal)
                + (accruedInterest == null ? 0 : accruedInterest)
                + (overdueAmount == null ? 0 : overdueAmount);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getOutstandingPrincipal() { return outstandingPrincipal; }
    public void setOutstandingPrincipal(Double value) { this.outstandingPrincipal = value; }

    public Double getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(Double value) { this.accruedInterest = value; }

    public Double getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(Double value) { this.overdueAmount = value; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Integer getTerm() { return term; }
    public void setTerm(Integer term) { this.term = term; }

    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String method) { this.repaymentMethod = method; }
}
