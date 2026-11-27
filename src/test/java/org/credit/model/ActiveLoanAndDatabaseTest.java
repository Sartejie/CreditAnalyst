package org.credit.model;

import org.credit.db.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActiveLoanAndDatabaseTest {

    @Test
    void testActiveLoanPaymentEarlyRepaymentAndOverdue() {
        LoanApplication app = new LoanApplication();
        app.setParameters(1200.0, 12, 12.0, PaymentSchedule.METHOD_ANNUITY, new Date());
        Borrower b = new Borrower("Тест Тест", "РБ", "ID1", new Date(), "Паспорт", "Минск", 2000.0);
        FinalReport report = app.runAllCalculations(b, new Form2501(), new ScoringModel(), null, 40.0);

        ActiveLoan loan = ActiveLoan.fromReport(report, b, "client", "analyst");
        assertEquals(ActiveLoan.STATUS_ACTIVE, loan.getStatus());

        // Просрочка на следующий день после даты очередного платежа
        Calendar cal = Calendar.getInstance();
        cal.setTime(loan.getNextPaymentDate());
        cal.add(Calendar.DAY_OF_MONTH, 2);
        loan.checkAndUpdateOverdue(cal.getTime());
        assertEquals(ActiveLoan.STATUS_OVERDUE, loan.getStatus());
        assertTrue(loan.getOverdueDays() >= 2);
        assertTrue(loan.getOverdueAmount() > 0);

        // Полное досрочное погашение
        ActiveLoan.PaymentAnalysis analysis = loan.analyzePayment(5000.0, cal.getTime());
        assertTrue(analysis.exceedsScheduledDue);
        assertTrue(analysis.exceedsTotalDebt);

        Payment fullPay = new Payment(cal.getTime(), analysis.totalDebt.doubleValue(), 0.0, 0.0, 0.0);
        loan.registerPayment(fullPay, true);
        assertEquals(ActiveLoan.STATUS_EARLY_REPAID, loan.getStatus());
        assertEquals(0.0, loan.getOutstandingPrincipal(), 0.01);
        assertEquals("ДОСРОЧНОЕ ПОГАШЕНИЕ", fullPay.getType());
    }

    @Test
    void testModifyParametersAndSaveToDatabase(@TempDir Path tempDir) throws Exception {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("test_credit.db").toAbsolutePath();

        LoanApplication app = new LoanApplication();
        app.setParameters(6000.0, 12, 12.0, PaymentSchedule.METHOD_ANNUITY, new Date());
        Borrower b = new Borrower("Петров Пётр", "РБ", "3150585A001PB1", new Date(), "Паспорт", "Минск", 3000.0);
        Form2501 form = new Form2501();
        CreditDeal deal = new CreditDeal();
        deal.setContractNumber("D-1");
        deal.setAmount(1000.0);
        deal.setTerm(10);
        form.getCreditDeals().add(deal);

        FinalReport report = app.runAllCalculations(b, form, new ScoringModel(), null, 40.0);

        try (Database db = new Database(dbUrl)) {
            assertNotNull(db.authenticate("analyst", "analyst123"));
            assertNotNull(db.authenticate("client", "client123"));

            long bId = db.saveBorrower(b);
            long fId = db.saveForm2501(bId, form);
            app.setBorrowerId(bId);
            app.setFormId(fId);
            long appId = db.saveApplication(app);
            report.setApplicationId(appId);
            db.SaveReport(report);
            assertEquals(1, db.countReports());

            ActiveLoan loan = ActiveLoan.fromReport(report, b, "client", "analyst");
            loan.setBorrowerId(bId);
            loan.setFormId(fId);
            loan.setApplicationId(appId);
            db.SaveLoan(loan);

            loan.changeParameters(Map.of("rate", 10.0, "term", 24, "repaymentMethod", PaymentSchedule.METHOD_DIFFERENTIATED));
            db.UpdateLoan(loan);

            ActiveLoan loaded = db.GetLoan(loan.getId());
            assertNotNull(loaded);
            assertEquals(10.0, loaded.getRate());
            assertEquals(24, loaded.getTerm());
            assertEquals(24, loaded.getSchedule().getPayments().size());
        }

        // Проверка экспорта PDF и XML
        File pdf = report.exportPDF(tempDir.resolve("report.pdf").toFile());
        //File xml = report.exportXML(tempDir.resolve("report.xml").toFile());
        assertTrue(pdf.exists() && pdf.length() > 500);
        //assertTrue(xml.exists() && xml.length() > 200);
    }
}
