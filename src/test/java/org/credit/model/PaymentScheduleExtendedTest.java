package org.credit.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentScheduleExtendedTest {

    @Test
    void testCalculateInterest30360() {
        // 12000 * 12% * (30 / 360) = 120.00
        BigDecimal interest = PaymentSchedule.calculateInterest30360(
                BigDecimal.valueOf(12000), BigDecimal.valueOf(12), 30);
        assertEquals(0, BigDecimal.valueOf(120.00).compareTo(interest));
    }

    @Test
    void testLumpSumMonthlyInterest() {
        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setLoanAmount(12000.0);
        schedule.setTermMonths(6);
        schedule.setInterestRate(12.0);
        schedule.generate(PaymentSchedule.METHOD_LUMP_SUM_MONTHLY_INTEREST, new Date());

        List<Payment> payments = schedule.getPayments();
        assertEquals(6, payments.size());

        // Первые 5 месяцев: ОД = 0, проценты = 120.00
        for (int i = 0; i < 5; i++) {
            assertEquals(0.0, payments.get(i).getPrincipalAmount(), 0.001);
            assertEquals(120.0, payments.get(i).getInterestAmount(), 0.01);
            assertEquals(12000.0, payments.get(i).getRemainingBalance(), 0.01);
        }
        // Последний месяц: ОД = 12000.00 + проценты = 120.00
        Payment last = payments.get(5);
        assertEquals(12000.0, last.getPrincipalAmount(), 0.01);
        assertEquals(120.0, last.getInterestAmount(), 0.01);
        assertEquals(12120.0, last.getTotalAmount(), 0.01);
        assertEquals(0.0, last.getRemainingBalance(), 0.001);
    }

    @Test
    void testLumpSumEndInterest() {
        PaymentSchedule schedule = new PaymentSchedule();
        schedule.setLoanAmount(12000.0);
        schedule.setTermMonths(6);
        schedule.setInterestRate(12.0);
        schedule.generate(PaymentSchedule.METHOD_LUMP_SUM_END_INTEREST, new Date());

        List<Payment> payments = schedule.getPayments();
        assertEquals(6, payments.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(0.0, payments.get(i).getTotalAmount(), 0.001);
        }
        Payment last = payments.get(5);
        assertEquals(12000.0, last.getPrincipalAmount(), 0.01);
        assertEquals(720.0, last.getInterestAmount(), 0.01);
        assertEquals(0.0, last.getRemainingBalance(), 0.001);
    }
}
