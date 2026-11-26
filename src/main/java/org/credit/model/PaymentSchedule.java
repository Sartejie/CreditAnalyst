package org.credit.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * График погашения кредита по базе 30/360 (Инструкция НБ РБ № 488).
 * Поддерживает 3 способа возврата: Аннуитетный, Дифференцированный, Единовременный.
 */
public class PaymentSchedule {
    public static final String METHOD_ANNUITY = "Аннуитетные платежи";
    public static final String METHOD_DIFFERENTIATED = "Дифференцированные платежи";
    public static final String METHOD_LUMP_SUM_MONTHLY_INTEREST = "Единовременный (проценты ежемесячно)";
    public static final String METHOD_LUMP_SUM_END_INTEREST = "Единовременный (проценты в конце срока)";

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DAYS_IN_YEAR_360 = BigDecimal.valueOf(360);
    private static final BigDecimal DAYS_IN_MONTH_30 = BigDecimal.valueOf(30);
    private static final BigDecimal MONTHS_12 = BigDecimal.valueOf(12);

    private Double loanAmount;
    private Integer termMonths;
    private Double interestRate; // Годовая ставка (%)
    private String method;
    private Date startDate;
    private Double monthlyPayment;
    private Double totalAmount;
    private List<Payment> payments = new ArrayList<>();

    public PaymentSchedule() {}

    public void generate(String method) {
        generate(method, this.startDate != null ? this.startDate : new Date());
    }

    public void generate(String method, Date baseDate) {
        this.method = method;
        this.startDate = baseDate != null ? baseDate : new Date();
        this.payments.clear();

        if (loanAmount == null || termMonths == null || interestRate == null
                || termMonths <= 0 || loanAmount <= 0 || interestRate < 0 || method == null) {
            this.monthlyPayment = 0.0;
            this.totalAmount = 0.0;
            return;
        }

        BigDecimal principalTotal = BigDecimal.valueOf(loanAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal annualRate = BigDecimal.valueOf(interestRate);
        // i = r / (12 * 100), эквивалентно (r / 100) * (30 / 360) по базе 30/360
        BigDecimal monthlyRate = annualRate.divide(MONTHS_12.multiply(HUNDRED), MC);

        String normalized = method.toLowerCase();
        boolean isAnnuity = normalized.contains("аннуитет") || normalized.contains("annuit");
        boolean isLumpSum = normalized.contains("единовремен") || normalized.contains("lump");
        boolean lumpEndInterest = isLumpSum && normalized.contains("конце срока");

        BigDecimal balance = principalTotal;
        BigDecimal totalSum = BigDecimal.ZERO;

        BigDecimal fixedAnnuity = BigDecimal.ZERO;
        if (isAnnuity) {
            if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
                fixedAnnuity = principalTotal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
            } else {
                BigDecimal onePlusI = BigDecimal.ONE.add(monthlyRate, MC);
                BigDecimal pow = onePlusI.pow(termMonths, MC);
                BigDecimal factor = monthlyRate.multiply(pow, MC)
                        .divide(pow.subtract(BigDecimal.ONE, MC), MC);
                fixedAnnuity = principalTotal.multiply(factor, MC).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal diffPrincipal = principalTotal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        BigDecimal accumulatedLumpInterest = BigDecimal.ZERO;

        Calendar cal = Calendar.getInstance();
        cal.setTime(this.startDate);

        for (int k = 1; k <= termMonths; k++) {
            cal.add(Calendar.MONTH, 1);
            Date paymentDate = cal.getTime();

            // I_k = O_{k-1} * (r / 100) * (30 / 360)
            BigDecimal periodInterest = calculateInterest30360(balance, annualRate, 30);
            BigDecimal principalPart;
            BigDecimal interestPart;

            if (isAnnuity) {
                interestPart = periodInterest;
                if (k == termMonths) {
                    principalPart = balance;
                } else {
                    principalPart = fixedAnnuity.subtract(interestPart).setScale(2, RoundingMode.HALF_UP);
                    if (principalPart.compareTo(balance) > 0) {
                        principalPart = balance;
                    }
                    if (principalPart.compareTo(BigDecimal.ZERO) < 0) {
                        principalPart = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }
                }
            } else if (isLumpSum) {
                if (lumpEndInterest) {
                    accumulatedLumpInterest = accumulatedLumpInterest.add(periodInterest);
                    if (k == termMonths) {
                        principalPart = balance;
                        interestPart = accumulatedLumpInterest.setScale(2, RoundingMode.HALF_UP);
                    } else {
                        principalPart = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                        interestPart = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }
                } else {
                    interestPart = periodInterest;
                    principalPart = (k == termMonths) ? balance : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }
            } else {
                // Дифференцированный способ (равные части основного долга)
                interestPart = periodInterest;
                principalPart = (k == termMonths) ? balance : diffPrincipal;
            }

            BigDecimal paymentSum = principalPart.add(interestPart).setScale(2, RoundingMode.HALF_UP);
            balance = balance.subtract(principalPart).setScale(2, RoundingMode.HALF_UP);
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            totalSum = totalSum.add(paymentSum);

            Payment p = new Payment(paymentDate, paymentSum, principalPart, interestPart, balance);
            p.setPeriodNumber(k);
            p.setType("ПЛАНОВЫЙ");
            payments.add(p);
        }

        if (isAnnuity) {
            this.monthlyPayment = fixedAnnuity.doubleValue();
        } else if (!payments.isEmpty()) {
            this.monthlyPayment = isLumpSum && lumpEndInterest
                    ? totalSum.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP).doubleValue()
                    : payments.get(0).getTotalAmount();
        } else {
            this.monthlyPayment = 0.0;
        }

        this.totalAmount = totalSum.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Начисление процентов по базе 30/360: I = O * (r / 100) * (d / 360).
     */
    public static BigDecimal calculateInterest30360(BigDecimal balance, BigDecimal annualRatePercent, int days) {
        if (balance == null || annualRatePercent == null || balance.compareTo(BigDecimal.ZERO) <= 0
                || annualRatePercent.compareTo(BigDecimal.ZERO) <= 0 || days <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return balance
                .multiply(annualRatePercent, MC)
                .divide(HUNDRED, MC)
                .multiply(BigDecimal.valueOf(days), MC)
                .divide(DAYS_IN_YEAR_360, 2, RoundingMode.HALF_UP);
    }

    /**
     * Количество дней между двумя датами по методу 30/360 (Европейский/банковский стандарт 30E/360).
     */
    public static int daysBetween30360(Date start, Date end) {
        if (start == null || end == null || !end.after(start)) {
            return 0;
        }
        Calendar c1 = Calendar.getInstance();
        c1.setTime(start);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(end);

        int d1 = Math.min(c1.get(Calendar.DAY_OF_MONTH), 30);
        int d2 = Math.min(c2.get(Calendar.DAY_OF_MONTH), 30);
        int m1 = c1.get(Calendar.MONTH) + 1;
        int m2 = c2.get(Calendar.MONTH) + 1;
        int y1 = c1.get(Calendar.YEAR);
        int y2 = c2.get(Calendar.YEAR);

        return Math.max(0, (y2 - y1) * 360 + (m2 - m1) * 30 + (d2 - d1));
    }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Double getLoanAmount() { return loanAmount; }
    public void setLoanAmount(Double loanAmount) { this.loanAmount = loanAmount; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public Double getInterestRate() { return interestRate; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }

    public Double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(Double monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
}
