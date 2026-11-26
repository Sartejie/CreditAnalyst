package org.credit.model;

import java.util.HashMap;
import java.util.Map;

public class ScoringModel {
    private Map<String, Double> rules;

    public ScoringModel() {
        rules = new HashMap<>();
        // Инициализация правил (пороговых значений)
        rules.put("max_pdn_threshold", 50.0);       // Максимальный ПДН (в %)
        rules.put("critical_pdn_threshold", 70.0);  // Критический ПДН (в %)
        rules.put("max_loan_amount", 5000000.0);    // Максимальная сумма кредита
        rules.put("base_score", 850.0);             // Максимальный базовый балл
    }

    public ScoringResult evaluate(Form2501 history, LoanApplication loan, DebtBurden pdn, Double income) {
        double currentScore = rules.getOrDefault("base_score", 850.0);

        // 1. Оценка долговой нагрузки (ПДН)
        if (pdn != null && pdn.getValue() != null) {
            double pdnValue = pdn.getValue();
            if (pdnValue > rules.get("critical_pdn_threshold")) {
                currentScore -= 300; // Критическая нагрузка
            } else if (pdnValue > rules.get("max_pdn_threshold")) {
                currentScore -= 150; // Высокая нагрузка
            }
        }

        // 2. Оценка кредитной истории (Form2501)
        if (history == null || !history.validate()) {
            // Нет кредитной истории или она невалидна
            currentScore -= 100;
        } else {
            double totalExistingDebt = 0.0;

            for (CreditDeal deal : history.getCreditDeals()) {
                if (deal.getAmount() != null) {
                    totalExistingDebt += deal.getAmount();
                }

                // Штраф за плохую группу риска (например, если группа "High", "4", "5" и т.д.)
                String riskGroup = deal.getRiskGroup();
                if (riskGroup != null) {
                    String lowerRisk = riskGroup.toLowerCase();
                    if (lowerRisk.contains("high") || lowerRisk.contains("высок") || riskGroup.equals("4") || riskGroup.equals("5")) {
                        currentScore -= 150;
                    }
                }
            }

            // Штраф, если общий долг по справке превышает годовой доход заемщика
            if (income != null && income > 0 && totalExistingDebt > (income * 12)) {
                currentScore -= 100;
            }
        }

        // 3. Оценка суммы текущего запрашиваемого кредита
        if (loan != null && loan.getAmount() != null) {
            if (loan.getAmount() > rules.get("max_loan_amount")) {
                currentScore -= 150;
            }

            // Проверка соотношения суммы кредита к доходу (DTI)
            if (income != null && income > 0) {
                double debtToIncomeRatio = loan.getAmount() / income;
                if (debtToIncomeRatio > 10) { // Сумма кредита > 10 годовых доходов
                    currentScore -= 100;
                }
            }
        }

        // Ограничиваем минимальный балл (например, 300)
        int finalScore = (int) Math.max(300, currentScore);

        // Определение категории риска
        String riskCategory;
        if (finalScore >= 700) {
            riskCategory = "Низкий риск (Low Risk)";
        } else if (finalScore >= 500) {
            riskCategory = "Средний риск (Medium Risk)";
        } else {
            riskCategory = "Высокий риск (High Risk)";
        }

        return new ScoringResult(finalScore, riskCategory);
    }
}