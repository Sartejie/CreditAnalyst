package org.credit.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Результат оценки вероятности дефолта заёмщика на горизонте 12 месяцев.
 */
public class ScoringResult {
    private Integer score;
    private String riskCategory;
    private boolean incompleteData;
    private List<String> factors = new ArrayList<>();

    public ScoringResult() {}

    public ScoringResult(Integer score, String riskCategory) {
        this.score = score;
        this.riskCategory = riskCategory;
    }

    public ScoringResult(Integer score, String riskCategory, boolean incompleteData, List<String> factors) {
        this.score = score;
        this.riskCategory = riskCategory;
        this.incompleteData = incompleteData;
        if (factors != null) {
            this.factors.addAll(factors);
        }
    }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public boolean isIncompleteData() { return incompleteData; }
    public void setIncompleteData(boolean incompleteData) { this.incompleteData = incompleteData; }

    public List<String> getFactors() { return factors; }
    public void setFactors(List<String> factors) {
        this.factors = factors != null ? factors : new ArrayList<>();
    }
}
