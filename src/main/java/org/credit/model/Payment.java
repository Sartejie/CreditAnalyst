package org.credit.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * Отдельный платёж (плановый в графике или фактический по действующему кредиту).
 */
public class Payment {
    private long id;
    private long activeLoanId;
    private int periodNumber;

    private Date date;
    private BigDecimal totalAmount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal remainingBalance;
    private String type;

    public Payment(Date date, Double totalAmount, Double principalAmount,
                   Double interestAmount, Double remainingBalance) {
        this(date, toBD(totalAmount), toBD(principalAmount), toBD(interestAmount), toBD(remainingBalance));
    }

    public Payment(Date date, BigDecimal totalAmount, BigDecimal principalAmount,
                   BigDecimal interestAmount, BigDecimal remainingBalance) {
        this.date = date;
        this.totalAmount = scale2(totalAmount);
        this.principalAmount = scale2(principalAmount);
        this.interestAmount = scale2(interestAmount);
        this.remainingBalance = scale2(remainingBalance);
    }

    private static BigDecimal toBD(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private static BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getActiveLoanId() { return activeLoanId; }
    public void setActiveLoanId(long id) { this.activeLoanId = id; }

    public int getPeriodNumber() { return periodNumber; }
    public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public Double getTotalAmount() { return totalAmount.doubleValue(); }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = scale2(toBD(totalAmount)); }

    public Double getPrincipalAmount() { return principalAmount.doubleValue(); }
    public void setPrincipalAmount(Double principalAmount) { this.principalAmount = scale2(toBD(principalAmount)); }

    public Double getInterestAmount() { return interestAmount.doubleValue(); }
    public void setInterestAmount(Double interestAmount) { this.interestAmount = scale2(toBD(interestAmount)); }

    public Double getRemainingBalance() { return remainingBalance.doubleValue(); }
    public void setRemainingBalance(Double remainingBalance) { this.remainingBalance = scale2(toBD(remainingBalance)); }

    public BigDecimal getTotalAmountBD() { return totalAmount; }
    public BigDecimal getPrincipalAmountBD() { return principalAmount; }
    public BigDecimal getInterestAmountBD() { return interestAmount; }
    public BigDecimal getRemainingBalanceBD() { return remainingBalance; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
