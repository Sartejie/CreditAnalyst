package org.credit.model;


import java.io.File;

public class FinalReport {
    private PaymentSchedule schedule;
    private DebtBurden pdn;
    private ScoringResult scoring;

    private long applicationId;

    public long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(long applicationId) {
        this.applicationId = applicationId;
    }

    public void aggregate(PaymentSchedule schedule, DebtBurden pdn, ScoringResult scoring) {
        this.schedule = schedule;
        this.pdn = pdn;
        this.scoring = scoring;
    }

    public File exportPDF() { return null; /* Логика генерации PDF */ }
    public File exportXML() { return null; /* Логика генерации XML */ }
    public void save() { /* Логика сохранения */ }

    // Геттеры для доступа из UI
    public PaymentSchedule getSchedule() { return schedule; }
    public DebtBurden getPdn() { return pdn; }
    public ScoringResult getScoring() { return scoring; }
}