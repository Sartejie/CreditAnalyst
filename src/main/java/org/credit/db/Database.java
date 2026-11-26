
package org.credit.db;

import org.credit.model.*;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class Database implements AutoCloseable {

    private static final String URL = "jdbc:sqlite:credit.db";

    private final Connection connection;

    public Database() {
        try {
            connection = DriverManager.getConnection(URL);

            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            initializeDatabase();

        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }

    private void initializeDatabase() throws SQLException {

        try (Statement st = connection.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS active_loans (
                    id INTEGER PRIMARY KEY,
                    application_id INTEGER NOT NULL,
                    status TEXT,
                    outstanding_principal REAL,
                    accrued_interest REAL,
                    overdue_amount REAL,
                    rate REAL,
                    term INTEGER,
                    repayment_method TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    active_loan_id INTEGER NOT NULL,
                    payment_date TEXT,
                    amount REAL,
                    principal_part REAL,
                    interest_part REAL,
                    remaining_balance REAL,
                    type TEXT,
                    FOREIGN KEY(active_loan_id)
                    REFERENCES active_loans(id)
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS final_reports (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            application_id INTEGER NOT NULL,
                            pdn_value REAL,
                            scoring_score INTEGER,
                            risk_category TEXT,
                            report_data TEXT,
                            loan_amount REAL,
                            term_months INTEGER,
                            interest_rate REAL,
                            repayment_method TEXT,
                            monthly_payment REAL,
                            total_amount REAL,
                            FOREIGN KEY (application_id)
                                REFERENCES loan_applications(id)
                        )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS payment_schedule_details (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    report_id INTEGER NOT NULL,
                    payment_date TEXT NOT NULL,
                    principal_amount REAL,
                    interest_amount REAL,
                    total_amount REAL,
                    remaining_balance REAL,
                    FOREIGN KEY (report_id)
                        REFERENCES final_reports(id)
                        ON DELETE CASCADE
                )
            """);
        }
    }


    public void SaveReport(FinalReport report) {

        String reportSql = """
        INSERT INTO final_reports (
            application_id,
            pdn_value,
            scoring_score,
            risk_category,
            report_data,
            loan_amount,
            term_months,
            interest_rate,
            repayment_method,
            monthly_payment,
            total_amount
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        String paymentSql = """
        INSERT INTO payment_schedule_details (
            report_id,
            payment_date,
            principal_amount,
            interest_amount,
            total_amount,
            remaining_balance
        ) VALUES (?, ?, ?, ?, ?, ?)
    """;

        try {
            connection.setAutoCommit(false);

            try {
                PaymentSchedule schedule = report.getSchedule();
                DebtBurden pdn = report.getPdn();
                ScoringResult scoring = report.getScoring();

                if (schedule == null || pdn == null || scoring == null) {
                    throw new IllegalArgumentException(
                            "Report must contain schedule, PDN and scoring"
                    );
                }

                long reportId;

                // 1. Сохраняем основной отчёт
                try (PreparedStatement ps =
                             connection.prepareStatement(reportSql)) {

                    ps.setLong(1, report.getApplicationId());

                    setDouble(ps, 2, pdn.getValue());

                    if (scoring.getScore() == null)
                        ps.setNull(3, Types.INTEGER);
                    else
                        ps.setInt(3, scoring.getScore());

                    ps.setString(4, scoring.getRiskCategory());

                    // Дополнительная информация
                    ps.setString(5, null);

                    setDouble(ps, 6, schedule.getLoanAmount());

                    if (schedule.getTermMonths() == null)
                        ps.setNull(7, Types.INTEGER);
                    else
                        ps.setInt(7, schedule.getTermMonths());

                    setDouble(ps, 8, schedule.getInterestRate());

                    ps.setString(9, schedule.getMethod());

                    setDouble(ps, 10, schedule.getMonthlyPayment());
                    setDouble(ps, 11, schedule.getTotalAmount());

                    ps.executeUpdate();
                }

                // 2. Получаем ID только что созданного отчёта
                try (Statement st = connection.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT last_insert_rowid()")) {

                    if (!rs.next()) {
                        throw new SQLException("Cannot obtain report ID");
                    }

                    reportId = rs.getLong(1);
                }

                // 3. Сохраняем график платежей
                List<Payment> payments = schedule.getPayments();

                try (PreparedStatement ps =
                             connection.prepareStatement(paymentSql)) {

                    SimpleDateFormat dateFormat =
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    for (Payment payment : payments) {

                        ps.setLong(1, reportId);

                        if (payment.getDate() == null) {
                            ps.setNull(2, Types.VARCHAR);
                        } else {
                            ps.setString(
                                    2,
                                    dateFormat.format(payment.getDate())
                            );
                        }

                        setDouble(ps, 3, payment.getPrincipalAmount());
                        setDouble(ps, 4, payment.getInterestAmount());
                        setDouble(ps, 5, payment.getTotalAmount());
                        setDouble(ps, 6, payment.getRemainingBalance());

                        ps.addBatch();
                    }

                    ps.executeBatch();
                }

                // 4. Подтверждаем транзакцию
                connection.commit();

            } catch (Exception e) {

                connection.rollback();

                throw new RuntimeException(
                        "Failed to save final report. Transaction rolled back.",
                        e
                );

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database transaction error", e);
        }
    }


    public void SaveLoan(ActiveLoan loan) {

        String sql = """
            INSERT INTO active_loans (
                id, application_id, status,
                outstanding_principal, accrued_interest,
                overdue_amount, rate, term, repayment_method
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loan.getId());
            ps.setLong(2, loan.getApplicationId());
            ps.setString(3, loan.getStatus());
            setDouble(ps, 4, loan.getOutstandingPrincipal());
            setDouble(ps, 5, loan.getAccruedInterest());
            setDouble(ps, 6, loan.getOverdueAmount());
            setDouble(ps, 7, loan.getRate());

            if (loan.getTerm() == null)
                ps.setNull(8, Types.INTEGER);
            else
                ps.setInt(8, loan.getTerm());

            ps.setString(9, loan.getRepaymentMethod());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save loan", e);
        }
    }

    public void UpdateLoan(ActiveLoan loan) {

        String sql = """
            UPDATE active_loans SET
                application_id = ?,
                status = ?,
                outstanding_principal = ?,
                accrued_interest = ?,
                overdue_amount = ?,
                rate = ?,
                term = ?,
                repayment_method = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loan.getApplicationId());
            ps.setString(2, loan.getStatus());
            setDouble(ps, 3, loan.getOutstandingPrincipal());
            setDouble(ps, 4, loan.getAccruedInterest());
            setDouble(ps, 5, loan.getOverdueAmount());
            setDouble(ps, 6, loan.getRate());

            if (loan.getTerm() == null)
                ps.setNull(7, Types.INTEGER);
            else
                ps.setInt(7, loan.getTerm());

            ps.setString(8, loan.getRepaymentMethod());
            ps.setLong(9, loan.getId());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException(
                        "Loan not found: " + loan.getId()
                );
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update loan", e);
        }
    }

    public ActiveLoan GetLoan(long id) {

        String sql = "SELECT * FROM active_loans WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next())
                    return null;

                ActiveLoan loan = new ActiveLoan();

                loan.setId(rs.getLong("id"));
                loan.setApplicationId(rs.getLong("application_id"));
                loan.setStatus(rs.getString("status"));

                loan.setOutstandingPrincipal(
                        getNullableDouble(rs, "outstanding_principal")
                );

                loan.setAccruedInterest(
                        getNullableDouble(rs, "accrued_interest")
                );

                loan.setOverdueAmount(
                        getNullableDouble(rs, "overdue_amount")
                );

                loan.setRate(getNullableDouble(rs, "rate"));

                int term = rs.getInt("term");
                loan.setTerm(rs.wasNull() ? null : term);

                loan.setRepaymentMethod(
                        rs.getString("repayment_method")
                );

                return loan;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get loan", e);
        }
    }

    public void SavePayment(Payment payment) {

        String sql = """
            INSERT INTO payments (
                active_loan_id,
                payment_date,
                amount,
                principal_part,
                interest_part,
                remaining_balance,
                type
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, payment.getActiveLoanId());

            if (payment.getDate() == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(
                        2,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(payment.getDate())
                );
            }

            setDouble(ps, 3, payment.getTotalAmount());
            setDouble(ps, 4, payment.getPrincipalAmount());
            setDouble(ps, 5, payment.getInterestAmount());
            setDouble(ps, 6, payment.getRemainingBalance());
            ps.setString(7, payment.getType());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment", e);
        }
    }

    private static void setDouble(
            PreparedStatement ps, int index, Double value
    ) throws SQLException {

        if (value == null)
            ps.setNull(index, Types.REAL);
        else
            ps.setDouble(index, value);
    }

    private static Double getNullableDouble(
            ResultSet rs, String column
    ) throws SQLException {

        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    @Override
    public void close() {
        try {
            if (!connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close database", e);
        }
    }
}
