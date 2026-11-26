package org.credit.model;

import java.util.Collections;
import java.util.List;

/**
 * Пользователь с ролью «Клиент (заёмщик)».
 */
public class Client extends User {
    public static final String ROLE = "Client";

    public Client(String login, String password, String role) {
        super(login, password, role != null ? role : ROLE);
    }

    public Client(long id, String login, String password, String fullName) {
        super(id, login, password, ROLE, fullName);
    }

    public List<Payment> viewSchedule(ActiveLoan loan) {
        if (loan == null || loan.getSchedule() == null) {
            return Collections.emptyList();
        }
        return loan.getSchedule().getPayments();
    }

    public boolean makePayment(ActiveLoan loan, Payment payment, boolean confirmEarlyRepayment) {
        if (loan == null || payment == null) {
            return false;
        }
        loan.registerPayment(payment, confirmEarlyRepayment);
        return true;
    }

    public List<Payment> viewPaymentHistory(ActiveLoan loan) {
        if (loan == null || loan.getPayments() == null) {
            return Collections.emptyList();
        }
        return loan.getPayments();
    }

    public void viewSchedule() {}
    public void makePayment() {}
    public void viewPaymentHistory() {}
}
