package org.credit.model;

import java.util.List;
import java.util.Map;

/**
 * Пользователь с ролью «Кредитный специалист (аналитик)».
 */
public class CreditSpecialist extends User {
    public static final String ROLE = "CreditSpecialist";

    public CreditSpecialist(String login, String password, String role) {
        super(login, password, role != null ? role : ROLE);
    }

    public CreditSpecialist(long id, String login, String password, String fullName) {
        super(id, login, password, ROLE, fullName);
    }

    public List<ValidationError> inputData(Borrower borrower, Form2501 form, LoanApplication application) {
        List<ValidationError> errors = new java.util.ArrayList<>();
        if (borrower != null) errors.addAll(borrower.validateDetailed());
        if (form != null) errors.addAll(form.validateDetailed());
        if (application != null) errors.addAll(application.validateDetailed());
        return errors;
    }

    public FinalReport runCalculations(Borrower borrower, Form2501 history,
                                       LoanApplication application, ScoringModel scoringModel,
                                       List<ActiveLoan> existingLoans, double pdnThreshold) {
        return application.runAllCalculations(borrower, history, scoringModel, existingLoans, pdnThreshold);
    }

    public ActiveLoan approveLoan(FinalReport report, Borrower borrower, String clientLogin) {
        if (report == null) {
            throw new IllegalArgumentException("Отчёт не сформирован");
        }
        return ActiveLoan.fromReport(report, borrower, clientLogin, getLogin());
    }

    public FinalReport modifyLoanParams(ActiveLoan loan, Map<String, Object> params,
                                        Double monthlyIncome, Form2501 history,
                                        ScoringModel scoringModel, double pdnThreshold) {
        if (loan == null) {
            throw new IllegalArgumentException("Кредит не выбран");
        }
        loan.changeParameters(params);
        return loan.buildUpdatedReport(monthlyIncome, history, scoringModel, pdnThreshold);
    }

    public void inputData() {}
    public void runCalculations() {}
    public void approveLoan() {}
    public void modifyLoanParams() {}
}
