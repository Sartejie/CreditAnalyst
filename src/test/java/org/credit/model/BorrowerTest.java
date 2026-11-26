package org.credit.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class BorrowerTest {
    private Borrower borrower;

    @BeforeEach
    void setUp() {
        borrower = new Borrower(
                "Иванов Иван Иванович",
                "РФ",
                "1234 567890",
                new Date(),
                "Паспорт",
                "г. Москва, ул. Пушкина, д. 1",
                55000.0
        );
    }

    @Test
    void testValidateSuccess() {
        assertTrue(borrower.validate(), "Валидный заемщик должен проходить проверку");
    }

    @Test
    void testValidateNullFullName() {
        borrower.setFullName(null);
        assertFalse(borrower.validate(), "Заемщик без ФИО не должен проходить проверку");
    }

    @Test
    void testValidateNegativeIncome() {
        borrower.setMonthlyIncome(-100.0);
        assertFalse(borrower.validate(), "Отрицательный доход недопустим");
    }

    @Test
    void testGetIncome() {
        assertEquals(55000.0, borrower.getIncome(), "Метод getIncome должен возвращать месячный доход");
    }
}