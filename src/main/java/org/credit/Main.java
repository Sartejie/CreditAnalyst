package org.credit;

import com.formdev.flatlaf.FlatLightLaf;
import org.credit.db.Database;
import org.credit.model.ActiveLoan;
import org.credit.model.FinalReport;
import org.credit.ui.FinalReportDialog;
import org.credit.ui.LoanApplicationFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try (Database database = new Database()) {

            ActiveLoan loan = database.GetLoan(1);

            if (loan != null) {
                loan.setOutstandingPrincipal(150000.);
                database.UpdateLoan(loan);
            }
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel( new FlatLightLaf() );
            } catch( Exception ex ) {
                System.err.println( "Failed to initialize LaF" );
            }
            // Установка глобального скругления углов для FlatLaf (опционально)
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("TextComponent.arc", 8);

            LoanApplicationFrame frame = new LoanApplicationFrame();
            frame.setVisible(true);
        });
    }
}