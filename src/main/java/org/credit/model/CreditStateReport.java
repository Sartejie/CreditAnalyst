package org.credit.model;

/**
 * Отчёт о текущем состоянии кредита (остаток ОД, начисленные проценты, просрочка, статус).
 */
public class CreditStateReport {
    private Double outstandingPrincipal = 0.0;
    private Double accruedInterest = 0.0;
    private Double overdueAmount = 0.0;
    private Integer overdueDays = 0;
    private String status = "Новый";

    public void update(ActiveLoan loan) {
        if (loan == null) {
            return;
        }
        this.outstandingPrincipal = loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : 0.0;
        this.accruedInterest = loan.getAccruedInterest() != null ? loan.getAccruedInterest() : 0.0;
        this.overdueAmount = loan.getOverdueAmount() != null ? loan.getOverdueAmount() : 0.0;
        this.overdueDays = loan.getOverdueDays() != null ? loan.getOverdueDays() : 0;
        this.status = loan.getStatus() != null ? loan.getStatus() : "Неизвестно";
    }

    public Double getOutstandingPrincipal() { return outstandingPrincipal; }
    public void setOutstandingPrincipal(Double outstandingPrincipal) { this.outstandingPrincipal = outstandingPrincipal; }

    public Double getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(Double accruedInterest) { this.accruedInterest = accruedInterest; }

    public Double getOverdue() { return overdueAmount; }
    public Double getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(Double overdueAmount) { this.overdueAmount = overdueAmount; }

    public Integer getOverdueDays() { return overdueDays; }
    public void setOverdueDays(Integer overdueDays) { this.overdueDays = overdueDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
