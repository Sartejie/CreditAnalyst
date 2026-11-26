package org.credit.ui;

import org.credit.model.CreditSpecialist;
import org.credit.model.ScoringModel;

import javax.swing.*;
import java.awt.*;

/**
 * Автономное окно подачи кредитной заявки (обёртка вокруг LoanApplicationPanel).
 */
public class LoanApplicationFrame extends JFrame {
    public LoanApplicationFrame() {
        setTitle("Создание кредитной заявки — Кредит-Аналитик");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(860, 680);
        setLocationRelativeTo(null);

        CreditSpecialist defaultSpecialist = new CreditSpecialist("analyst", "", CreditSpecialist.ROLE);
        LoanApplicationPanel panel = new LoanApplicationPanel(defaultSpecialist, new ScoringModel(), null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(panel, BorderLayout.CENTER);
    }
}
