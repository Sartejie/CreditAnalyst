
package org.credit.model;

import java.util.Date;

public class Payment {

    private long id;
    private long activeLoanId;

    private Date date;
    private Double totalAmount;
    private Double principalAmount;
    private Double interestAmount;
    private Double remainingBalance;
    private String type;

    public Payment(Date date, Double totalAmount,
                   Double principalAmount, Double interestAmount,
                   Double remainingBalance) {
        this.date = date;
        this.totalAmount = totalAmount;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.remainingBalance = remainingBalance;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getActiveLoanId() { return activeLoanId; }
    public void setActiveLoanId(long id) { this.activeLoanId = id; }

    public Date getDate() { return date; }
    public Double getTotalAmount() { return totalAmount; }
    public Double getPrincipalAmount() { return principalAmount; }
    public Double getInterestAmount() { return interestAmount; }
    public Double getRemainingBalance() { return remainingBalance; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
