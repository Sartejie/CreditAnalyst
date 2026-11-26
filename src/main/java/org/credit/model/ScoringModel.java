package org.credit.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Настраиваемая скоринговая модель оценки вероятности дефолта на горизонте 12 месяцев.
 */
public class ScoringModel {
    public static final String RISK_LOW = "Низкий риск (Low Risk)";
    public static final String RISK_MEDIUM = "Средний риск (Medium Risk)";
    public static final String RISK_HIGH = "Высокий риск (High Risk)";

    private Map<String, Double> rules;

    public ScoringModel() {
        rules = new LinkedHashMap<>();
        rules.put("base_score", 850.0);
        rules.put("low_risk_threshold", 700.0);
        rules.put("medium_risk_threshold", 500.0);
        rules.put("pdn_warning_threshold", 40.0);
        rules.put("max_pdn_threshold", 50.0);
        rules.put("critical_pdn_threshold", 70.0);
        rules.put("max_loan_amount", 5000000.0);
        rules.put("max_rate_threshold", 25.0);
        rules.put("max_term_threshold", 60.0);
        rules.put("max_active_deals_threshold", 3.0);

        // Веса штрафов
        rules.put("pdn_high_penalty", 150.0);
        rules.put("pdn_critical_penalty", 300.0);
        rules.put("invalid_history_penalty", 100.0);
        rules.put("bad_risk_group_penalty", 150.0);
        rules.put("overdue_history_penalty", 150.0);
        rules.put("many_deals_penalty", 50.0);
        rules.put("debt_exceeds_income_penalty", 100.0);
        rules.put("max_loan_amount_penalty", 150.0);
        rules.put("high_dti_penalty", 100.0);
        rules.put("high_rate_penalty", 50.0);
        rules.put("long_term_penalty", 50.0);
    }

    public Map<String, Double> getRules() {
        return rules;
    }

    public void setRules(Map<String, Double> newRules) {
        if (newRules != null) {
            this.rules.putAll(newRules);
        }
    }

    public void setRule(String key, Double value) {
        if (key != null && value != null) {
            this.rules.put(key, value);
        }
    }

    public ScoringResult evaluate(Form2501 history, LoanApplication loan, DebtBurden pdn, Double income) {
        double currentScore = rules.getOrDefault("base_score", 850.0);
        List<String> factors = new ArrayList<>();
        boolean incompleteData = false;

        // Проверка полноты данных для расчёта скоринга
        if (income == null || income <= 0) {
            incompleteData = true;
            factors.add("Отсутствуют корректные сведения о среднемесячном доходе");
        }
        if (loan == null || loan.getAmount() == null) {
            incompleteData = true;
            factors.add("Отсутствуют параметры запрашиваемого кредита");
        }
        if (pdn == null || pdn.getValue() == null) {
            incompleteData = true;
            factors.add("Не рассчитан показатель долговой нагрузки (ПДН)");
        }

        // 1. Оценка долговой нагрузки (ПДН)
        if (pdn != null && pdn.getValue() != null) {
            double pdnValue = pdn.getValue();
            if (pdnValue > rules.getOrDefault("critical_pdn_threshold", 70.0)) {
                double pen = rules.getOrDefault("pdn_critical_penalty", 300.0);
                currentScore -= pen;
                factors.add(String.format("Критический ПДН (%.2f%%): -%.0f", pdnValue, pen));
            } else if (pdnValue > rules.getOrDefault("max_pdn_threshold", 50.0)) {
                double pen = rules.getOrDefault("pdn_high_penalty", 150.0);
                currentScore -= pen;
                factors.add(String.format("Повышенный ПДН (%.2f%%): -%.0f", pdnValue, pen));
            }
        }

        // 2. Оценка кредитной истории (Форма 2501)
        if (history == null || !history.validate()) {
            incompleteData = true;
            double pen = rules.getOrDefault("invalid_history_penalty", 100.0);
            currentScore -= pen;
            factors.add(String.format("Отсутствует или неполна кредитная история (Форма 2501): -%.0f", pen));
        } else {
            double totalExistingDebt = 0.0;
            List<CreditDeal> deals = history.getCreditDeals();

            if (deals.size() > rules.getOrDefault("max_active_deals_threshold", 3.0)) {
                double pen = rules.getOrDefault("many_deals_penalty", 50.0);
                currentScore -= pen;
                factors.add(String.format("Большое количество сделок (%d): -%.0f", deals.size(), pen));
            }

            for (CreditDeal deal : deals) {
                if (deal.getAmount() != null) {
                    totalExistingDebt += deal.getAmount();
                }

                String riskGroup = deal.getRiskGroup();
                if (riskGroup != null) {
                    String lowerRisk = riskGroup.toLowerCase();
                    if (lowerRisk.contains("high") || lowerRisk.contains("высок")
                            || riskGroup.equals("4") || riskGroup.equals("5")) {
                        double pen = rules.getOrDefault("bad_risk_group_penalty", 150.0);
                        currentScore -= pen;
                        factors.add(String.format("Негативная группа риска по сделке %s (%s): -%.0f",
                                deal.getContractNumber(), riskGroup, pen));
                    }
                }

                boolean hasOverdue = (deal.getOverdueDays() != null && deal.getOverdueDays() > 0)
                        || (deal.getOverdueAmount() != null && deal.getOverdueAmount() > 0);
                if (hasOverdue) {
                    double pen = rules.getOrDefault("overdue_history_penalty", 150.0);
                    currentScore -= pen;
                    factors.add(String.format("Наличие просрочки по сделке %s: -%.0f",
                            deal.getContractNumber(), pen));
                }
            }

            if (income != null && income > 0 && totalExistingDebt > (income * 12)) {
                double pen = rules.getOrDefault("debt_exceeds_income_penalty", 100.0);
                currentScore -= pen;
                factors.add(String.format("Текущие обязательства превышают годовой доход: -%.0f", pen));
            }
        }

        // 3. Оценка параметров запрашиваемого кредита
        if (loan != null && loan.getAmount() != null) {
            if (loan.getAmount() > rules.getOrDefault("max_loan_amount", 5000000.0)) {
                double pen = rules.getOrDefault("max_loan_amount_penalty", 150.0);
                currentScore -= pen;
                factors.add(String.format("Сумма кредита превышает лимит модели: -%.0f", pen));
            }

            if (income != null && income > 0) {
                double debtToIncomeRatio = loan.getAmount() / income;
                if (debtToIncomeRatio > 10) {
                    double pen = rules.getOrDefault("high_dti_penalty", 100.0);
                    currentScore -= pen;
                    factors.add(String.format("Высокое соотношение суммы кредита к доходу (DTI=%.1f): -%.0f",
                            debtToIncomeRatio, pen));
                }
            }

            if (loan.getRate() != null && loan.getRate() > rules.getOrDefault("max_rate_threshold", 25.0)) {
                double pen = rules.getOrDefault("high_rate_penalty", 50.0);
                currentScore -= pen;
                factors.add(String.format("Высокая процентная ставка (%.2f%%): -%.0f", loan.getRate(), pen));
            }

            if (loan.getTerm() != null && loan.getTerm() > rules.getOrDefault("max_term_threshold", 60.0)) {
                double pen = rules.getOrDefault("long_term_penalty", 50.0);
                currentScore -= pen;
                factors.add(String.format("Длительный срок кредитования (%d мес.): -%.0f", loan.getTerm(), pen));
            }
        }

        int finalScore = (int) Math.max(300, Math.round(currentScore));

        String riskCategory;
        if (incompleteData) {
            // Требование п. 4.4 ТЗ: консервативная оценка «Высокий риск» при неполных данных
            riskCategory = RISK_HIGH;
            factors.add("Присвоена консервативная категория «Высокий риск» из-за неполных данных");
        } else if (finalScore >= rules.getOrDefault("low_risk_threshold", 700.0)) {
            riskCategory = RISK_LOW;
        } else if (finalScore >= rules.getOrDefault("medium_risk_threshold", 500.0)) {
            riskCategory = RISK_MEDIUM;
        } else {
            riskCategory = RISK_HIGH;
        }

        return new ScoringResult(finalScore, riskCategory, incompleteData, factors);
    }
}
