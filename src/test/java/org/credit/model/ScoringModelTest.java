package org.credit.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringModelTest {

    private ScoringModel scoringModel;
    private final Double testIncome = 100_000.0;

    @BeforeEach
    void setUp() {
        scoringModel = new ScoringModel();
    }

    // Вспомогательный метод для создания валидной кредитной истории
    private Form2501 createValidHistory(CreditDeal... deals) {
        Form2501 history = new Form2501();
        ArrayList<CreditDeal> dealList = new ArrayList<>();
        if (deals != null) {
            Collections.addAll(dealList, deals);
        }
        // Если список пуст, validate() вернет false, поэтому добавляем безопасную сделку
        if (dealList.isEmpty()) {
            CreditDeal safeDeal = new CreditDeal();
            safeDeal.setContractNumber("12345");
            safeDeal.setAmount(10000.0);
            safeDeal.setRiskGroup("low");
            dealList.add(safeDeal);
        }
        history.setCreditDeals(dealList);
        return history;
    }

    // Вспомогательный метод для создания заявки
    private LoanApplication createLoan(Double amount) {
        LoanApplication loan = new LoanApplication();
        loan.setParameters(amount, 12, 15.0, "ANNUITY", new java.util.Date());
        return loan;
    }

    // Вспомогательный метод для создания объекта ПДН с заданным значением
    private DebtBurden createPdn(Double value) {
        return new DebtBurden() {
            @Override
            public Double getValue() {
                return value;
            }
        };
    }

    @Test
    @DisplayName("Идеальный заемщик получает максимальный балл")
    void testEvaluate_PerfectApplicant_ReturnsHighScoreLowRisk() {
        Form2501 history = createValidHistory();
        LoanApplication loan = createLoan(1_000_000.0);
        DebtBurden pdn = createPdn(20.0); // ПДН 20%

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        assertAll(
                () -> assertEquals(850, result.getScore(), "Балл должен быть максимальным"),
                () -> assertEquals("Низкий риск (Low Risk)", result.getRiskCategory())
        );
    }

    @Test
    @DisplayName("Высокий ПДН (выше 50%) снижает балл на 150")
    void testEvaluate_HighPdn_ReducesScore() {
        Form2501 history = createValidHistory();
        LoanApplication loan = createLoan(1_000_000.0);
        DebtBurden pdn = createPdn(60.0);

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        assertEquals(700, result.getScore()); // 850 - 150 = 700
    }

    @Test
    @DisplayName("Критический ПДН (выше 70%) снижает балл на 300")
    void testEvaluate_CriticalPdn_ReducesScoreAndChangesRisk() {
        Form2501 history = createValidHistory();
        LoanApplication loan = createLoan(1_000_000.0);
        DebtBurden pdn = createPdn(80.0);

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        assertAll(
                () -> assertEquals(550, result.getScore()), // 850 - 300 = 550
                () -> assertEquals("Средний риск (Medium Risk)", result.getRiskCategory())
        );
    }

    @Test
    @DisplayName("Отсутствие или невалидность истории снижает балл на 100")
    void testEvaluate_InvalidHistory_ReducesScore() {
        LoanApplication loan = createLoan(1_000_000.0);
        DebtBurden pdn = createPdn(20.0);
        Form2501 emptyHistory = new Form2501(); // Пустая история, validate() вернет false

        ScoringResult result = scoringModel.evaluate(emptyHistory, loan, pdn, testIncome);

        assertEquals(750, result.getScore()); // 850 - 100 = 750
    }

    @Test
    @DisplayName("Плохая группа риска в старом кредите снижает балл на 150")
    void testEvaluate_BadCreditHistoryRiskGroup_ReducesScore() {
        CreditDeal badDeal = new CreditDeal();
        badDeal.setContractNumber("BAD-001");
        badDeal.setAmount(50_000.0);
        badDeal.setRiskGroup("high"); // Плохая группа

        Form2501 history = createValidHistory(badDeal);
        LoanApplication loan = createLoan(1_000_000.0);
        DebtBurden pdn = createPdn(20.0);

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        assertEquals(700, result.getScore()); // 850 - 150 = 700
    }

    @Test
    @DisplayName("Превышение старого долга над годовым доходом снижает балл на 100")
    void testEvaluate_ExistingDebtExceedsAnnualIncome_ReducesScore() {
        CreditDeal bigDebt = new CreditDeal();
        bigDebt.setContractNumber("BIG-002");
        bigDebt.setAmount(1_500_000.0); // 1.5 млн > (100к * 12 мес)
        bigDebt.setRiskGroup("low");

        Form2501 history = createValidHistory(bigDebt);
        LoanApplication loan = createLoan(1_000_000.0);
        DebtBurden pdn = createPdn(20.0);

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        assertEquals(750, result.getScore()); // 850 - 100 = 750
    }

    @Test
    @DisplayName("Сумма кредита свыше 5 млн снижает балл на 150, а DTI — ещё на 100")
    void testEvaluate_LoanAmountExceedsLimit_ReducesScore() {
        // Arrange: 6 млн, доход 100к, ПДН 20%, история валидна.
        // Штрафы: -150 (сумма > 5 млн), -100 (DTI = 6млн/100к = 60 > 10).
        // Итог: 850 - 150 - 100 = 600.
        Form2501 history = createValidHistory();
        LoanApplication loan = createLoan(6_000_000.0);
        DebtBurden pdn = createPdn(20.0);

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        assertEquals(600, result.getScore(),
                "Балл снижен на 150 (сумма) и 100 (DTI)");
        assertEquals("Средний риск (Medium Risk)", result.getRiskCategory());
    }

    @Test
    @DisplayName("Соотношение кредита к доходу (DTI) > 10 снижает балл на 100")
    void testEvaluate_HighDebtToIncomeRatio_ReducesScore() {
        Form2501 history = createValidHistory();
        // Кредит 1.5 млн при доходе 100к. DTI = 15 (>10)
        LoanApplication loan = createLoan(1_500_000.0);
        DebtBurden pdn = createPdn(20.0);

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        assertEquals(750, result.getScore()); // 850 - 100 = 750
    }

    @Test
    @DisplayName("Худший сценарий: накопление штрафов упирается в минимум 300 баллов")
    void testEvaluate_WorstCaseScenario_ScoreFloorsAt300() {
        // Собираем все возможные штрафы
        CreditDeal badDeal = new CreditDeal();
        badDeal.setContractNumber("BAD-003");
        badDeal.setAmount(2_000_000.0); // Долг > годового дохода (-100)
        badDeal.setRiskGroup("5");      // Плохая группа (-150)

        Form2501 history = createValidHistory(badDeal);
        LoanApplication loan = createLoan(7_000_000.0); // > 5 млн (-150) и DTI > 10 (-100)
        DebtBurden pdn = createPdn(90.0); // Критический ПДН (-300)

        ScoringResult result = scoringModel.evaluate(history, loan, pdn, testIncome);

        // Сумма штрафов = 300 + 100 + 150 + 150 + 100 = 800.
        // 850 - 800 = 50. Но Math.max(300, 50) = 300.
        assertAll(
                () -> assertEquals(300, result.getScore(), "Балл не должен опускаться ниже 300"),
                () -> assertEquals("Высокий риск (High Risk)", result.getRiskCategory())
        );
    }

    @Test
    @DisplayName("Null вместо истории кредитов снижает балл на 100")
    void testEvaluate_NullHistory_ReducesScore() {
        LoanApplication loan = createLoan(1_000_000.0);
        DebtBurden pdn = createPdn(20.0);

        ScoringResult result = scoringModel.evaluate(null, loan, pdn, testIncome);

        assertEquals(750, result.getScore()); // 850 - 100 = 750
    }
}