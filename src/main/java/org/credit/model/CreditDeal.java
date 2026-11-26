package org.credit.model;

import java.util.Date;

public class CreditDeal {
    private String dealType;
    private String creditType;
    private String contractNumber;
    private Date conclusionDate;
    private Double amount;
    private String currency;
    private Integer term;
    private Date repaymentDate;
    private String security;
    private String riskGroup;

    public CreditDeal() {}

    // Геттеры и сеттеры
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

    public Date getRepaymentDate() { return repaymentDate; }
    public void setRepaymentDate(Date repaymentDate) { this.repaymentDate = repaymentDate; }

    public String getSecurity() { return security; }
    public void setSecurity(String security) { this.security = security; }

    public String getRiskGroup() { return riskGroup; }
    public void setRiskGroup(String riskGroup) { this.riskGroup = riskGroup; }

    public String getDetails() {
        return String.format("Договор №%s | Тип: %s/%s | Сумма: %.2f %s | Срок: %d мес. | Обеспечение: %s",
                contractNumber != null ? contractNumber : "б/н",
                dealType, creditType,
                amount != null ? amount : 0.0, currency != null ? currency : "",
                term != null ? term : 0,
                security != null ? security : "нет");
    }
}