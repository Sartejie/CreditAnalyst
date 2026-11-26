package org.credit.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Сведения о кредитной сделке из Формы 2501.
 */
public class CreditDeal {
    private String dealType;
    private String creditType;
    private String contractNumber;
    private Date conclusionDate;
    private Double amount;
    private String currency;
    private Integer term;
    private Integer renewalTerm; // Срок возобновляемости линии (мес.)
    private Date repaymentDate;
    private String security;
    private String riskGroup;
    private Date relevanceDate; // Дата актуальности сведений

    // Показатели для расчёта ПДН и скоринга
    private Integer overdueDays;
    private Double overdueAmount;
    private Double monthlyPayment;

    public CreditDeal() {}

    public String getDealType() { return dealType; }
    public void setDealType(String dealType) { this.dealType = dealType; }

    public String getCreditType() { return creditType; }
    public void setCreditType(String creditType) { this.creditType = creditType; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public Date getConclusionDate() { return conclusionDate; }
    public void setConclusionDate(Date conclusionDate) { this.conclusionDate = conclusionDate; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getTerm() { return term; }
    public void setTerm(Integer term) { this.term = term; }

    public Integer getRenewalTerm() { return renewalTerm; }
    public void setRenewalTerm(Integer renewalTerm) { this.renewalTerm = renewalTerm; }

    public Date getRepaymentDate() { return repaymentDate; }
    public void setRepaymentDate(Date repaymentDate) { this.repaymentDate = repaymentDate; }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public String getRiskGroup() { return riskGroup; }
    public void setRiskGroup(String riskGroup) { this.riskGroup = riskGroup; }

    public Date getRelevanceDate() { return relevanceDate; }
    public void setRelevanceDate(Date relevanceDate) { this.relevanceDate = relevanceDate; }

    public Integer getOverdueDays() { return overdueDays; }
    public void setOverdueDays(Integer overdueDays) { this.overdueDays = overdueDays; }

    public Double getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(Double overdueAmount) { this.overdueAmount = overdueAmount; }

    public Double getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(Double monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    /**
     * Оценивает ежемесячный платёж по действующей сделке для расчёта ПДН.
     */
    public BigDecimal getEstimatedMonthlyPayment() {
        if (monthlyPayment != null && monthlyPayment > 0) {
            return BigDecimal.valueOf(monthlyPayment).setScale(2, RoundingMode.HALF_UP);
        }
        if (amount != null && amount > 0 && term != null && term > 0) {
            return BigDecimal.valueOf(amount)
                    .divide(BigDecimal.valueOf(term), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public List<ValidationError> validateDetailed(int index) {
        List<ValidationError> errors = new ArrayList<>();
        String section = "Сделка №" + index;

        if (contractNumber == null || contractNumber.trim().isEmpty()) {
            errors.add(new ValidationError(section, "Номер договора", "обязателен для заполнения"));
        }
        if (amount == null || amount <= 0) {
            errors.add(new ValidationError(section, "Сумма кредита", "должна быть больше 0"));
        }
        if (term != null && term <= 0) {
            errors.add(new ValidationError(section, "Срок кредита", "должен быть больше 0"));
        }
        if (renewalTerm != null && renewalTerm < 0) {
            errors.add(new ValidationError(section, "Срок возобновляемости", "не может быть отрицательным"));
        }
        if (conclusionDate != null && repaymentDate != null && !repaymentDate.after(conclusionDate)) {
            errors.add(new ValidationError(section, "Срок возврата",
                    "дата возврата должна быть позже даты заключения"));
        }
        if (overdueDays != null && overdueDays < 0) {
            errors.add(new ValidationError(section, "Длительность просрочки", "не может быть отрицательной"));
        }
        if (overdueAmount != null && overdueAmount < 0) {
            errors.add(new ValidationError(section, "Сумма просрочки", "не может быть отрицательной"));
        }
        if (monthlyPayment != null && monthlyPayment < 0) {
            errors.add(new ValidationError(section, "Ежемесячный платёж", "не может быть отрицательным"));
        }
        return errors;
    }

    public String getDetails() {
        return String.format("Договор №%s | Тип: %s/%s | Сумма: %.2f %s | Срок: %d мес. | Обеспечение: %s",
                contractNumber != null ? contractNumber : "б/н",
                dealType, creditType,
                amount != null ? amount : 0.0, currency != null ? currency : "",
                term != null ? term : 0,
                security != null ? security : "нет");
    }
}
