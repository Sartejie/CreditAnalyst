package org.credit.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Расчёт показателя долговой нагрузки — ПДН (Постановление Правления НБ РБ № 100).
 * ПДН = ((SUM M_действ + M_нов) / D_ср) * 100%.
 */
public class DebtBurden {
    public static final double DEFAULT_THRESHOLD_PERCENT = 40.0;

    private Double value;
    private Double existingMonthlyPayments = 0.0;
    private Double newLoanMonthlyPayment = 0.0;
    private Double thresholdPercent = DEFAULT_THRESHOLD_PERCENT;
    private boolean thresholdExceeded;

    public void calculate(Double income, List<Payment> payments) {
        calculate(income, payments, null, null, this.thresholdPercent != null ? this.thresholdPercent : DEFAULT_THRESHOLD_PERCENT);
    }

    public void calculate(Double income, List<Payment> newPayments, Form2501 history,
                          List<ActiveLoan> activeLoans, double thresholdPercent) {
        this.thresholdPercent = thresholdPercent;

        BigDecimal newMonthly = BigDecimal.ZERO;
        if (newPayments != null && !newPayments.isEmpty()) {
            BigDecimal sumNew = BigDecimal.ZERO;
            for (Payment p : newPayments) {
                sumNew = sumNew.add(p.getTotalAmountBD());
            }
            newMonthly = sumNew.divide(BigDecimal.valueOf(newPayments.size()), 2, RoundingMode.HALF_UP);
        }
        this.newLoanMonthlyPayment = newMonthly.doubleValue();

        BigDecimal existingMonthly = BigDecimal.ZERO;
        if (history != null && history.getCreditDeals() != null) {
            for (CreditDeal deal : history.getCreditDeals()) {
                existingMonthly = existingMonthly.add(deal.getEstimatedMonthlyPayment());
            }
        }
        if (activeLoans != null) {
            for (ActiveLoan loan : activeLoans) {
                if (loan != null && loan.isOpen() && loan.getMonthlyPaymentEstimate() > 0) {
                    existingMonthly = existingMonthly.add(BigDecimal.valueOf(loan.getMonthlyPaymentEstimate()));
                }
            }
        }
        this.existingMonthlyPayments = existingMonthly.setScale(2, RoundingMode.HALF_UP).doubleValue();

        BigDecimal totalMonthly = existingMonthly.add(newMonthly);
        if (income == null || income <= 0) {
            this.value = 0.0;
            this.thresholdExceeded = totalMonthly.compareTo(BigDecimal.ZERO) > 0;
            return;
        }

        BigDecimal incomeBD = BigDecimal.valueOf(income);
        BigDecimal pdn = totalMonthly
                .divide(incomeBD, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        this.value = pdn.doubleValue();
        this.thresholdExceeded = this.value > thresholdPercent;
    }

    public String getWarningMessage() {
        if (!thresholdExceeded) {
            return null;
        }
        return String.format(
                "ВНИМАНИЕ: ПДН (%.2f%%) превышает нормативный порог НБ РБ (%.2f%%)!",
                value != null ? value : 0.0,
                thresholdPercent != null ? thresholdPercent : DEFAULT_THRESHOLD_PERCENT
        );
    }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public Double getExistingMonthlyPayments() { return existingMonthlyPayments; }
    public Double getNewLoanMonthlyPayment() { return newLoanMonthlyPayment; }

    public Double getThresholdPercent() { return thresholdPercent; }
    public void setThresholdPercent(Double thresholdPercent) { this.thresholdPercent = thresholdPercent; }

    public boolean isThresholdExceeded() { return thresholdExceeded; }
    public void setThresholdExceeded(boolean thresholdExceeded) { this.thresholdExceeded = thresholdExceeded; }
}
