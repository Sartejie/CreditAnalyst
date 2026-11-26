package org.credit.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebtBurdenTest {

    @Test
    void testPdnIncludesExistingDealsAndChecksThreshold() {
        Form2501 history = new Form2501();
        CreditDeal existing = new CreditDeal();
        existing.setContractNumber("EX-1");
        existing.setAmount(12000.0);
        existing.setTerm(12);
        existing.setMonthlyPayment(500.0);
        history.getCreditDeals().add(existing);

        Payment newPay = new Payment(new java.util.Date(), 400.0, 300.0, 100.0, 0.0);
        DebtBurden pdn = new DebtBurden();
        // Доход 2000, платежи = 500 + 400 = 900 -> ПДН = 45% > 40%
        pdn.calculate(2000.0, List.of(newPay), history, null, 40.0);

        assertEquals(45.0, pdn.getValue(), 0.01);
        assertEquals(500.0, pdn.getExistingMonthlyPayments(), 0.01);
        assertEquals(400.0, pdn.getNewLoanMonthlyPayment(), 0.01);
        assertTrue(pdn.isThresholdExceeded());
        assertNotNull(pdn.getWarningMessage());
    }

    @Test
    void testScoringAssignsHighRiskOnIncompleteData() {
        ScoringModel model = new ScoringModel();
        LoanApplication app = new LoanApplication();
        app.setParameters(1000.0, 12, 10.0, PaymentSchedule.METHOD_ANNUITY, new java.util.Date());
        DebtBurden pdn = new DebtBurden();
        pdn.setValue(20.0);

        ScoringResult result = model.evaluate(new Form2501(), app, pdn, 3000.0);
        assertTrue(result.isIncompleteData());
        assertEquals(ScoringModel.RISK_HIGH, result.getRiskCategory());
    }
}
