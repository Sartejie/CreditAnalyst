package org.credit.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Действующий утверждённый кредит и его пост-кредитное сопровождение.
 */
public class ActiveLoan {
    public static final String STATUS_ACTIVE = "Действующий";
    public static final String STATUS_OVERDUE = "Просроченный";
    public static final String STATUS_EARLY_REPAID = "Досрочно погашенный";
    public static final String STATUS_CLOSED = "Закрытый";

    private long id;
    private long applicationId;
    private long borrowerId;
    private long formId;
    private String clientLogin;
    private String specialistLogin;
    private String borrowerName;
    private String contractNumber;
    private Double monthlyIncome;

    private String status = STATUS_ACTIVE;
    private Double originalAmount;
    private Double outstandingPrincipal;
    private Double accruedInterest = 0.0;
    private Double overdueAmount = 0.0;
    private Integer overdueDays = 0;
    private Integer paidPeriods = 0;

    private Double rate;
    private Integer term;
    private String repaymentMethod;
    private Date issueDate = new Date();
    private Date lastAccrualDate = new Date();
    private Date nextPaymentDate;

    private PaymentSchedule schedule;
    private List<Payment> payments = new ArrayList<>();

    public ActiveLoan() {}

    public ActiveLoan(long id, long applicationId, String status,
                      Double outstandingPrincipal, Double accruedInterest,
                      Double overdueAmount, Double rate, Integer term,
                      String repaymentMethod) {
        this.id = id;
        this.applicationId = applicationId;
        this.status = status;
        this.originalAmount = outstandingPrincipal;
        this.outstandingPrincipal = outstandingPrincipal;
        this.accruedInterest = accruedInterest != null ? accruedInterest : 0.0;
        this.overdueAmount = overdueAmount != null ? overdueAmount : 0.0;
        this.rate = rate;
        this.term = term;
        this.repaymentMethod = repaymentMethod;
    }

    public static ActiveLoan fromReport(FinalReport report, Borrower borrower,
                                        String clientLogin, String specialistLogin) {
        ActiveLoan loan = new ActiveLoan();
        PaymentSchedule sch = report.getSchedule();
        loan.setApplicationId(report.getApplicationId());
        if (borrower != null) {
            loan.setBorrowerId(borrower.getId());
            loan.setBorrowerName(borrower.getFullName());
            loan.setMonthlyIncome(borrower.getIncome());
            if (clientLogin == null || clientLogin.trim().isEmpty()) {
                clientLogin = borrower.getClientLogin();
            }
        }
        loan.setClientLogin(clientLogin != null && !clientLogin.trim().isEmpty() ? clientLogin : "client");
        loan.setSpecialistLogin(specialistLogin);
        loan.setStatus(STATUS_ACTIVE);

        if (sch != null) {
            loan.setOriginalAmount(sch.getLoanAmount());
            loan.setOutstandingPrincipal(sch.getLoanAmount());
            loan.setRate(sch.getInterestRate());
            loan.setTerm(sch.getTermMonths());
            loan.setRepaymentMethod(sch.getMethod());
            Date start = sch.getStartDate() != null ? sch.getStartDate() : new Date();
            loan.setIssueDate(start);
            loan.setLastAccrualDate(start);
            loan.setSchedule(sch);
            if (sch.getPayments() != null && !sch.getPayments().isEmpty()) {
                loan.setNextPaymentDate(sch.getPayments().get(0).getDate());
            }
        }
        loan.setAccruedInterest(0.0);
        loan.setOverdueAmount(0.0);
        loan.setOverdueDays(0);
        loan.setPaidPeriods(0);
        return loan;
    }

    public boolean isOpen() {
        return STATUS_ACTIVE.equals(status) || STATUS_OVERDUE.equals(status);
    }

    public double getMonthlyPaymentEstimate() {
        if (schedule != null && schedule.getMonthlyPayment() != null) {
            return schedule.getMonthlyPayment();
        }
        if (outstandingPrincipal != null && term != null && term > 0) {
            return BigDecimal.valueOf(outstandingPrincipal)
                    .divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return 0.0;
    }

    /**
     * Проверяет просрочку со следующего дня после установленной даты платежа.
     */
    public void checkAndUpdateOverdue(Date currentDate) {
        if (!isOpen() || nextPaymentDate == null || currentDate == null) {
            return;
        }
        if (outstandingPrincipal == null || outstandingPrincipal <= 0) {
            return;
        }
        Date curDay = truncateToDay(currentDate);
        Date dueDay = truncateToDay(nextPaymentDate);

        if (curDay.after(dueDay)) {
            long diffMs = curDay.getTime() - dueDay.getTime();
            int days = (int) Math.max(1, diffMs / (1000L * 60 * 60 * 24));
            this.overdueDays = days;
            this.status = STATUS_OVERDUE;

            // Начисляем проценты за период и фиксируем сумму просроченной задолженности
            BigDecimal periodInterest = calculateAccruedInterestUntil(currentDate);
            if (periodInterest.compareTo(BigDecimal.valueOf(accruedInterest != null ? accruedInterest : 0.0)) > 0) {
                this.accruedInterest = periodInterest.doubleValue();
            }

            BigDecimal scheduledDue = getNextScheduledPaymentAmount();
            BigDecimal currentOverdue = BigDecimal.valueOf(overdueAmount != null ? overdueAmount : 0.0);
            if (currentOverdue.compareTo(BigDecimal.ZERO) <= 0) {
                BigDecimal maxDebt = BigDecimal.valueOf(getBalance());
                BigDecimal computed = scheduledDue.compareTo(BigDecimal.ZERO) > 0 ? scheduledDue : maxDebt;
                this.overdueAmount = computed.min(maxDebt).setScale(2, RoundingMode.HALF_UP).doubleValue();
            }
        }
    }

    /**
     * Результат предварительной проверки платежа (для определения досрочного погашения).
     */
    public static class PaymentAnalysis {
        public final BigDecimal accruedInterest;
        public final BigDecimal scheduledDue;
        public final BigDecimal totalDebt;
        public final boolean exceedsScheduledDue;
        public final boolean exceedsTotalDebt;

        public PaymentAnalysis(BigDecimal accruedInterest, BigDecimal scheduledDue,
                               BigDecimal totalDebt, boolean exceedsScheduledDue, boolean exceedsTotalDebt) {
            this.accruedInterest = accruedInterest;
            this.scheduledDue = scheduledDue;
            this.totalDebt = totalDebt;
            this.exceedsScheduledDue = exceedsScheduledDue;
            this.exceedsTotalDebt = exceedsTotalDebt;
        }
    }

    public PaymentAnalysis analyzePayment(double amount, Date paymentDate) {
        Date effectiveDate = paymentDate != null ? paymentDate : new Date();
        checkAndUpdateOverdue(effectiveDate);

        BigDecimal interest = getEffectiveAccruedInterest(effectiveDate);
        BigDecimal principal = bd(outstandingPrincipal);
        BigDecimal overdue = bd(overdueAmount);
        BigDecimal totalDebt = principal.add(interest).add(overdue).setScale(2, RoundingMode.HALF_UP);

        BigDecimal nextScheduled = getNextScheduledPaymentAmount();
        BigDecimal scheduledDue = overdue.compareTo(BigDecimal.ZERO) > 0
                ? overdue.max(nextScheduled)
                : (nextScheduled.compareTo(BigDecimal.ZERO) > 0 ? nextScheduled : totalDebt);
        scheduledDue = scheduledDue.min(totalDebt).setScale(2, RoundingMode.HALF_UP);

        BigDecimal amt = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        boolean exceedsTotal = amt.compareTo(totalDebt) > 0;
        boolean exceedsDue = amt.compareTo(scheduledDue) > 0;
        return new PaymentAnalysis(interest, scheduledDue, totalDebt, exceedsDue, exceedsTotal);
    }

    public void registerPayment(Payment payment) {
        registerPayment(payment, false);
    }

    /**
     * Регистрирует платёж, пересчитывает остаток основного долга, проценты, просрочку и статус.
     */
    public void registerPayment(Payment payment, boolean confirmEarlyRepayment) {
        if (payment == null || payment.getTotalAmount() == null || payment.getTotalAmount() <= 0) {
            throw new IllegalArgumentException("Сумма платежа должна быть больше 0");
        }
        if (!isOpen()) {
            throw new IllegalStateException("Кредит уже закрыт (" + status + ")");
        }

        Date payDate = payment.getDate() != null ? payment.getDate() : new Date();
        PaymentAnalysis analysis = analyzePayment(payment.getTotalAmount(), payDate);

        BigDecimal totalPaid = BigDecimal.valueOf(payment.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
        // Не списываем больше полной задолженности по кредиту
        if (totalPaid.compareTo(analysis.totalDebt) > 0) {
            totalPaid = analysis.totalDebt;
        }

        BigDecimal remainingToAllocate = totalPaid;
        BigDecimal currentOverdue = bd(overdueAmount);
        BigDecimal currentInterest = analysis.accruedInterest;
        BigDecimal currentPrincipal = bd(outstandingPrincipal);

        // 1. Погашение просрочки
        BigDecimal paidOverdue = remainingToAllocate.min(currentOverdue);
        currentOverdue = currentOverdue.subtract(paidOverdue).setScale(2, RoundingMode.HALF_UP);
        remainingToAllocate = remainingToAllocate.subtract(paidOverdue);

        // 2. Погашение начисленных процентов
        BigDecimal paidInterest = remainingToAllocate.min(currentInterest);
        currentInterest = currentInterest.subtract(paidInterest).setScale(2, RoundingMode.HALF_UP);
        remainingToAllocate = remainingToAllocate.subtract(paidInterest);

        // 3. Погашение основного долга
        BigDecimal paidPrincipal = remainingToAllocate.min(currentPrincipal);
        currentPrincipal = currentPrincipal.subtract(paidPrincipal).setScale(2, RoundingMode.HALF_UP);

        this.overdueAmount = currentOverdue.doubleValue();
        this.accruedInterest = currentInterest.doubleValue();
        this.outstandingPrincipal = currentPrincipal.doubleValue();
        this.lastAccrualDate = payDate;

        boolean isFullEarly = currentPrincipal.compareTo(BigDecimal.ZERO) == 0
                && currentInterest.compareTo(BigDecimal.ZERO) == 0
                && currentOverdue.compareTo(BigDecimal.ZERO) == 0
                && (confirmEarlyRepayment || (term != null && paidPeriods + 1 < term));

        boolean isFullStandard = currentPrincipal.compareTo(BigDecimal.ZERO) == 0
                && currentInterest.compareTo(BigDecimal.ZERO) == 0
                && currentOverdue.compareTo(BigDecimal.ZERO) == 0
                && !isFullEarly;

        if (isFullEarly) {
            this.status = STATUS_EARLY_REPAID;
            this.overdueDays = 0;
            this.nextPaymentDate = null;
            payment.setType("ДОСРОЧНОЕ ПОГАШЕНИЕ");
        } else if (isFullStandard) {
            this.status = STATUS_CLOSED;
            this.overdueDays = 0;
            this.nextPaymentDate = null;
            payment.setType("ПОГАШЕНИЕ (ЗАКРЫТИЕ)");
        } else if (confirmEarlyRepayment && analysis.exceedsScheduledDue) {
            this.status = currentOverdue.compareTo(BigDecimal.ZERO) > 0 ? STATUS_OVERDUE : STATUS_ACTIVE;
            if (currentOverdue.compareTo(BigDecimal.ZERO) == 0) {
                this.overdueDays = 0;
            }
            this.paidPeriods = (paidPeriods != null ? paidPeriods : 0) + 1;
            payment.setType("ЧАСТИЧНОЕ ДОСРОЧНОЕ ПОГАШЕНИЕ");
            recalculate();
        } else {
            this.status = currentOverdue.compareTo(BigDecimal.ZERO) > 0 ? STATUS_OVERDUE : STATUS_ACTIVE;
            if (currentOverdue.compareTo(BigDecimal.ZERO) == 0) {
                this.overdueDays = 0;
            }
            this.paidPeriods = (paidPeriods != null ? paidPeriods : 0) + 1;
            advanceNextPaymentDate();
            payment.setType("СТАНДАРТНЫЙ");
        }

        payment.setActiveLoanId(this.id);
        payment.setDate(payDate);
        payment.setTotalAmount(totalPaid.doubleValue());
        payment.setPrincipalAmount(paidPrincipal.doubleValue());
        payment.setInterestAmount(paidInterest.add(paidOverdue).doubleValue());
        payment.setRemainingBalance(currentPrincipal.doubleValue());
        this.payments.add(payment);
    }

    /**
     * Пересчёт остаточного графика платежей при изменении параметров или частичном досрочном погашении.
     */
    public void recalculate() {
        if (outstandingPrincipal == null || outstandingPrincipal <= 0) {
            if (schedule != null) {
                schedule.getPayments().clear();
                schedule.setMonthlyPayment(0.0);
                schedule.setTotalAmount(0.0);
            }
            return;
        }
        int totalTerm = term != null && term > 0 ? term : 12;
        int done = paidPeriods != null ? paidPeriods : 0;
        int remainingTerm = Math.max(1, totalTerm - done);

        PaymentSchedule newSchedule = new PaymentSchedule();
        newSchedule.setLoanAmount(outstandingPrincipal);
        newSchedule.setTermMonths(remainingTerm);
        newSchedule.setInterestRate(rate != null ? rate : 0.0);
        Date base = lastAccrualDate != null ? lastAccrualDate : (issueDate != null ? issueDate : new Date());
        newSchedule.generate(repaymentMethod != null ? repaymentMethod : PaymentSchedule.METHOD_ANNUITY, base);
        this.schedule = newSchedule;

        if (!newSchedule.getPayments().isEmpty()) {
            this.nextPaymentDate = newSchedule.getPayments().get(0).getDate();
        }
    }

    public void changeParameters(Map<String, Object> params) {
        if (params == null) {
            return;
        }
        if (params.containsKey("rate") && params.get("rate") != null) {
            this.rate = ((Number) params.get("rate")).doubleValue();
        }
        if (params.containsKey("term") && params.get("term") != null) {
            this.term = ((Number) params.get("term")).intValue();
        }
        if (params.containsKey("repaymentMethod") && params.get("repaymentMethod") != null) {
            this.repaymentMethod = String.valueOf(params.get("repaymentMethod"));
        }
        if (params.containsKey("earlyRepaymentAmount") && params.get("earlyRepaymentAmount") != null) {
            double earlyAmt = ((Number) params.get("earlyRepaymentAmount")).doubleValue();
            if (earlyAmt > 0) {
                Date earlyDate = params.get("paymentDate") instanceof Date
                        ? (Date) params.get("paymentDate")
                        : new Date();
                Payment earlyPay = new Payment(earlyDate, earlyAmt, 0.0, 0.0, 0.0);
                registerPayment(earlyPay, true);
                return;
            }
        }
        recalculate();
    }

    /**
     * Формирует обновлённый итоговый отчёт после изменения параметров действующего кредита.
     */
    public FinalReport buildUpdatedReport(Double incomeOverride, Form2501 history,
                                          ScoringModel scoringModel, double pdnThreshold) {
        if (this.schedule == null) {
            recalculate();
        }
        Double income = incomeOverride != null ? incomeOverride : this.monthlyIncome;

        DebtBurden pdn = new DebtBurden();
        pdn.calculate(income, this.schedule != null ? this.schedule.getPayments() : null, history, null, pdnThreshold);

        LoanApplication appProxy = new LoanApplication();
        appProxy.setParameters(
                outstandingPrincipal != null ? outstandingPrincipal : 0.0,
                schedule != null && schedule.getTermMonths() != null ? schedule.getTermMonths() : term,
                rate,
                repaymentMethod,
                issueDate != null ? issueDate : new Date()
        );

        ScoringModel model = scoringModel != null ? scoringModel : new ScoringModel();
        ScoringResult scoring = model.evaluate(history, appProxy, pdn, income);

        CreditStateReport stateReport = new CreditStateReport();
        stateReport.update(this);

        Borrower borrowerProxy = new Borrower();
        borrowerProxy.setFullName(borrowerName);
        borrowerProxy.setMonthlyIncome(income);

        FinalReport report = new FinalReport();
        report.setApplicationId(this.applicationId);
        report.setBorrower(borrowerProxy);
        report.setApplication(appProxy);
        report.setForm2501(history);
        report.aggregate(this.schedule, pdn, stateReport, scoring);
        return report;
    }

    private BigDecimal calculateAccruedInterestUntil(Date targetDate) {
        Date from = lastAccrualDate != null ? lastAccrualDate : issueDate;
        int days = PaymentSchedule.daysBetween30360(from, targetDate);
        if (days <= 0) {
            days = 30;
        }
        return PaymentSchedule.calculateInterest30360(bd(outstandingPrincipal), bd(rate), days);
    }

    private BigDecimal getEffectiveAccruedInterest(Date targetDate) {
        BigDecimal stored = bd(accruedInterest);
        if (stored.compareTo(BigDecimal.ZERO) > 0) {
            return stored;
        }
        return calculateAccruedInterestUntil(targetDate);
    }

    private BigDecimal getNextScheduledPaymentAmount() {
        if (schedule != null && schedule.getPayments() != null && !schedule.getPayments().isEmpty()) {
            int idx = Math.min(paidPeriods != null ? paidPeriods : 0, schedule.getPayments().size() - 1);
            return schedule.getPayments().get(idx).getTotalAmountBD();
        }
        return BigDecimal.valueOf(getMonthlyPaymentEstimate()).setScale(2, RoundingMode.HALF_UP);
    }

    private void advanceNextPaymentDate() {
        if (schedule != null && schedule.getPayments() != null && paidPeriods < schedule.getPayments().size()) {
            this.nextPaymentDate = schedule.getPayments().get(paidPeriods).getDate();
        } else if (nextPaymentDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(nextPaymentDate);
            cal.add(Calendar.MONTH, 1);
            this.nextPaymentDate = cal.getTime();
        }
    }

    private static Date truncateToDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static BigDecimal bd(Double value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    public Double getBalance() {
        return bd(outstandingPrincipal)
                .add(bd(accruedInterest))
                .add(bd(overdueAmount))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(long borrowerId) { this.borrowerId = borrowerId; }

    public long getFormId() { return formId; }
    public void setFormId(long formId) { this.formId = formId; }

    public String getClientLogin() { return clientLogin; }
    public void setClientLogin(String clientLogin) { this.clientLogin = clientLogin; }

    public String getSpecialistLogin() { return specialistLogin; }
    public void setSpecialistLogin(String specialistLogin) { this.specialistLogin = specialistLogin; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public Double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Double originalAmount) { this.originalAmount = originalAmount; }

    public Double getOutstandingPrincipal() { return outstandingPrincipal; }
    public void setOutstandingPrincipal(Double value) { this.outstandingPrincipal = value; }

    public Double getAccruedInterest() { return accruedInterest; }
    public void setAccruedInterest(Double value) { this.accruedInterest = value; }

    public Double getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(Double value) { this.overdueAmount = value; }

    public Integer getOverdueDays() { return overdueDays; }
    public void setOverdueDays(Integer overdueDays) { this.overdueDays = overdueDays; }

    public Integer getPaidPeriods() { return paidPeriods; }
    public void setPaidPeriods(Integer paidPeriods) { this.paidPeriods = paidPeriods; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public Integer getTerm() { return term; }
    public void setTerm(Integer term) { this.term = term; }

    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String method) { this.repaymentMethod = method; }

    public Date getIssueDate() { return issueDate; }
    public void setIssueDate(Date issueDate) { this.issueDate = issueDate; }

    public Date getLastAccrualDate() { return lastAccrualDate; }
    public void setLastAccrualDate(Date lastAccrualDate) { this.lastAccrualDate = lastAccrualDate; }

    public Date getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(Date nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }

    public PaymentSchedule getSchedule() { return schedule; }
    public void setSchedule(PaymentSchedule schedule) { this.schedule = schedule; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
    }
}
