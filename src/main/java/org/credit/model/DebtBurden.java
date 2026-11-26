package org.credit.model;

import java.util.List;

public class DebtBurden {
    private Double value;

    /**
     * Рассчитывает показатель долговой нагрузки (ПДН).
     * @param income Ежемесячный доход заемщика
     * @param payments Список платежей по кредиту
     */
    public void calculate(Double income, List<Payment> payments) {
        if (income == null || income <= 0 || payments == null || payments.isEmpty()) {
            this.value = 0.0;
            return;
        }

        // Вычисляем средний ежемесячный платеж по графику
        double totalPayments = payments.stream()
                .mapToDouble(Payment::getTotalAmount)
                .sum();
        double averageMonthlyPayment = totalPayments / payments.size();

        // ПДН = (Средний ежемесячный платеж / Ежемесячный доход) * 100%
        double pdn = (averageMonthlyPayment / income) * 100.0;

        // Округляем до 2 знаков после запятой
        this.value = Math.round(pdn * 100.0) / 100.0;
    }

    public Double getValue() {
        return value;
    }
}