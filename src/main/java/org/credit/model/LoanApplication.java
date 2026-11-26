package org.credit.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Заявка на получение кредита. Оркестрирует валидацию и параллельный расчёт графика, ПДН и скоринга.
 */
public class LoanApplication {
    private long id;
    private long borrowerId;
    private long formId;
    private String status;
    private String createdBy;

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
        return validateDetailed().isEmpty();
    }

    public List<ValidationError> validateDetailed() {
        List<ValidationError> errors = new ArrayList<>();
        String section = "Параметры заявки";

        if (amount == null || amount <= 0) {
            errors.add(new ValidationError(section, "Сумма кредита", "должна быть больше 0"));
        }
        if (term == null || term <= 0) {
            errors.add(new ValidationError(section, "Срок кредита (мес.)", "должен быть больше 0"));
        }
        if (rate == null || rate < 0) {
            errors.add(new ValidationError(section, "Процентная ставка (%)", "не может быть отрицательной (>= 0)"));
        }
        if (repaymentMethod == null || repaymentMethod.trim().isEmpty()) {
            errors.add(new ValidationError(section, "Способ погашения", "должен быть выбран"));
        }
        if (applicationDate == null) {
            errors.add(new ValidationError(section, "Дата заявки", "обязательна для заполнения"));
        }
        return errors;
    }

    public PaymentSchedule calculateSchedule() {
        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setLoanAmount(amount);
        schedule.setTermMonths(term);
        schedule.setInterestRate(rate);
        schedule.setStartDate(applicationDate != null ? applicationDate : new Date());
        schedule.generate(repaymentMethod, schedule.getStartDate());
        return schedule;
    }

    public DebtBurden calculatePDN(Borrower borrower, Form2501 history,
                                    PaymentSchedule schedule, List<ActiveLoan> activeLoans,
                                    double pdnThreshold) {
        DebtBurden pdn = new DebtBurden();
        Double income = borrower != null ? borrower.getIncome() : null;
        List<Payment> payments = schedule != null ? schedule.getPayments() : null;
        pdn.calculate(income, payments, history, activeLoans, pdnThreshold);
        return pdn;
    }

    public DebtBurden calculatePDN() {
        return calculatePDN(null, null, calculateSchedule(), null, DebtBurden.DEFAULT_THRESHOLD_PERCENT);
    }

    public ScoringResult calculateScoring(Form2501 history, DebtBurden pdn, Double income, ScoringModel model) {
        ScoringModel effectiveModel = model != null ? model : new ScoringModel();
        return effectiveModel.evaluate(history, this, pdn, income);
    }

    public ScoringResult calculateScoring() {
        return new ScoringModel().evaluate(null, this, calculatePDN(), null);
    }

    /**
     * Выполняет расчёты параллельно (Fork -> ветви графика, ПДН, скоринга, состояния -> Join -> FinalReport).
     */
    public FinalReport runAllCalculations(Borrower borrower, Form2501 history,
                                          ScoringModel scoringModel, List<ActiveLoan> existingLoans,
                                          double pdnThreshold) {
        Double income = borrower != null ? borrower.getIncome() : null;

        CompletableFuture<PaymentSchedule> scheduleFuture =
                CompletableFuture.supplyAsync(this::calculateSchedule);

        CompletableFuture<DebtBurden> pdnFuture = scheduleFuture.thenApplyAsync(
                schedule -> calculatePDN(borrower, history, schedule, existingLoans, pdnThreshold));

        CompletableFuture<ScoringResult> scoringFuture = pdnFuture.thenApplyAsync(
                pdn -> calculateScoring(history, pdn, income, scoringModel));

        CompletableFuture<CreditStateReport> stateFuture = CompletableFuture.supplyAsync(() -> {
            CreditStateReport state = new CreditStateReport();
            state.setOutstandingPrincipal(amount != null ? amount : 0.0);
            state.setAccruedInterest(0.0);
            state.setOverdueAmount(0.0);
            state.setStatus("На рассмотрении");
            return state;
        });

        CompletableFuture.allOf(scheduleFuture, pdnFuture, scoringFuture, stateFuture).join();

        FinalReport report = new FinalReport();
        report.setApplicationId(this.id);
        report.setBorrower(borrower);
        report.setApplication(this);
        report.setForm2501(history);
        report.aggregate(scheduleFuture.join(), pdnFuture.join(), stateFuture.join(), scoringFuture.join());
        return report;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(long borrowerId) { this.borrowerId = borrowerId; }

    public long getFormId() { return formId; }
    public void setFormId(long formId) { this.formId = formId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Integer getTerm() { return term; }
    public void setTerm(Integer term) { this.term = term; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public String getRepaymentMethod() { return repaymentMethod; }
    public void setRepaymentMethod(String repaymentMethod) { this.repaymentMethod = repaymentMethod; }

    public Date getApplicationDate() { return applicationDate; }
    public void setApplicationDate(Date applicationDate) { this.applicationDate = applicationDate; }
}
