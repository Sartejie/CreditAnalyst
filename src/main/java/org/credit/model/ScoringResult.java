package org.credit.model;

public class ScoringResult {
    private Integer score;
    private String riskCategory;

    public ScoringResult() {}

    public ScoringResult(Integer score, String riskCategory) {
        this.score = score;
        this.riskCategory = riskCategory;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }
}