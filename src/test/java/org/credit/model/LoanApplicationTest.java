package org.credit.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoanApplicationTest {
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        application = new LoanApplication();
        // Используем точное название метода, как в PaymentSchedule и UI
        application.setParameters(
                1000000.0,
                24,
                15.0,
                "Аннуитетные платежи",
                new Date()
        );
    }

    // ================== ТЕСТЫ ВАЛИДАЦИИ ==================

    @Test
    void testValidateSuccess() {
        assertTrue(application.validate(), "Валидная заявка должна проходить проверку");
    }

    @Test
    void testValidateNullAmount() {
        application.setParameters(null, 24, 15.0, "Аннуитетные платежи", new Date());
        assertFalse(application.validate(), "Заявка без суммы не должна проходить");
    }

    @Test
    void testValidateNegativeTerm() {
        application.setParameters(1000000.0, -5, 15.0, "Аннуитетные платежи", new Date());
        assertFalse(application.validate(), "Отрицательный срок недопустим");
    }

    @Test
    void testValidateNullRate() {
        application.setParameters(1000000.0, 24, null, "Аннуитетные платежи", new Date());
        assertFalse(application.validate(), "Отсутствие ставки недопустимо");
    }

    @Test
    void testValidateEmptyMethod() {
        application.setParameters(1000000.0, 24, 15.0, "  ", new Date());
        assertFalse(application.validate(), "Пустой метод погашения недопустим");
    }

    // ================== ТЕСТЫ ГРАФИКА ПЛАТЕЖЕЙ ==================

    @Test
    void testCalculateScheduleAnnuitet() {
        PaymentSchedule schedule = application.calculateSchedule();
        List<Payment> payments = schedule.getPayments();

        assertNotNull(schedule);
        assertEquals(1000000.0, schedule.getLoanAmount());
        assertEquals(24, schedule.getTermMonths());
        assertEquals(24, payments.size(), "Количество платежей должно совпадать со сроком");

        // При аннуитете все платежи (кроме последнего, из-за округления) одинаковые
        double firstPayment = payments.get(0).getTotalAmount();
        double lastPayment = payments.get(payments.size() - 1).getTotalAmount();
        assertEquals(firstPayment, lastPayment, 1.0, "Платежи при аннуитете должны быть равны");

        // Проверка, что остаток долга в конце равен 0
        assertEquals(0.0, payments.get(payments.size() - 1).getRemainingBalance(), 0.01, "Остаток долга должен быть 0");

        // Проверка, что общая сумма равна сумме всех платежей
        double sumOfPayments = payments.stream().mapToDouble(Payment::getTotalAmount).sum();
        assertEquals(schedule.getTotalAmount(), sumOfPayments, 1.0, "Общая сумма должна совпадать");

        assertTrue(schedule.getTotalAmount() > 1000000.0, "Переплата должна быть больше 0");
    }

    @Test
    void testCalculateScheduleDifferentiated() {
        application.setParameters(1000000.0, 24, 15.0, "Дифференцированные платежи", new Date());
        PaymentSchedule schedule = application.calculateSchedule();
        List<Payment> payments = schedule.getPayments();

        assertEquals(24, payments.size());

        // При дифференцированном платеже сумма первого платежа больше последнего
        double firstPayment = payments.get(0).getTotalAmount();
        double lastPayment = payments.get(payments.size() - 1).getTotalAmount();
        assertTrue(firstPayment > lastPayment, "При дифф. платеже сумма к концу срока должна уменьшаться");

        assertEquals(0.0, payments.get(payments.size() - 1).getRemainingBalance(), 0.01);
    }

    @Test
    void testCalculateScheduleZeroRate() {
        application.setParameters(1000000.0, 10, 0.0, "Аннуитетные платежи", new Date());
        PaymentSchedule schedule = application.calculateSchedule();

        // При нулевой ставке ежемесячный платеж = сумма / срок (1000000 / 10 = 100000)
        assertEquals(100000.0, schedule.getMonthlyPayment(), 0.01, "При нулевой ставке платеж должен быть равен сумме деленной на срок");
        // Общая сумма должна быть равна сумме кредита (без переплаты)
        assertEquals(1000000.0, schedule.getTotalAmount(), 0.01, "Переплата при нулевой ставке должна быть 0");
    }
}