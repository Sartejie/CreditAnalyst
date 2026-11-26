package org.credit.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditDealTest {
    private CreditDeal deal;

    @BeforeEach
    void setUp() {
        deal = new CreditDeal();
        deal.setContractNumber("CR-2023-001");
        deal.setDealType("Кредит");
        deal.setCreditType("Потребительский");
        deal.setAmount(150000.0);
        deal.setCurrency("RUB");
        deal.setTerm(24);
        deal.setSecurity("Залог авто");
    }

    @Test
    void testGetDetails() {
        String details = deal.getDetails();

        assertTrue(details.contains("CR-2023-001"));
        assertTrue(details.contains("Потребительский"));
        assertTrue(details.contains("150000,00") || details.contains("150000.00")); // Зависит от локали
        assertTrue(details.contains("24 мес."));
    }
}