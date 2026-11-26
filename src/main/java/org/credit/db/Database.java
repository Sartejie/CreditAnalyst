package org.credit.db;

import org.credit.model.*;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Фасад локальной СУБД SQLite для хранения пользователей, заявок, Формы 2501, отчётов, кредитов и платежей.
 */
public class Database implements AutoCloseable {
    private static final String DEFAULT_URL = "jdbc:sqlite:credit.db";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final Connection connection;

    public Database() {
        this(DEFAULT_URL);
    }

    public Database(String url) {
        try {
            connection = DriverManager.getConnection(url);
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
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    login TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL,
                    full_name TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS borrowers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    last_name TEXT,
                    first_name TEXT,
                    middle_name TEXT,
                    full_name TEXT,
                    citizenship TEXT,
                    id_number TEXT,
                    birth_date TEXT,
                    document_type TEXT,
                    document_series_number TEXT,
                    document_issue_date TEXT,
                    document TEXT,
                    country TEXT,
                    region TEXT,
                    district TEXT,
                    locality TEXT,
                    address TEXT,
                    monthly_income REAL,
                    client_login TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS forms_2501 (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    borrower_id INTEGER NOT NULL,
                    created_at TEXT,
                    FOREIGN KEY (borrower_id) REFERENCES borrowers(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS credit_deals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    form_id INTEGER NOT NULL,
                    deal_type TEXT,
                    credit_type TEXT,
                    contract_number TEXT,
                    conclusion_date TEXT,
                    amount REAL,
                    currency TEXT,
                    term INTEGER,
                    renewal_term INTEGER,
                    repayment_date TEXT,
                    security TEXT,
                    risk_group TEXT,
                    relevance_date TEXT,
                    overdue_days INTEGER,
                    overdue_amount REAL,
                    monthly_payment REAL,
                    FOREIGN KEY (form_id) REFERENCES forms_2501(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS overdrafts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    form_id INTEGER NOT NULL,
                    contract_date TEXT,
                    limit_amount REAL,
                    limit_set_date TEXT,
                    FOREIGN KEY (form_id) REFERENCES forms_2501(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS loan_applications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    borrower_id INTEGER,
                    form_id INTEGER,
                    amount REAL,
                    term INTEGER,
                    rate REAL,
                    repayment_method TEXT,
                    application_date TEXT,
                    status TEXT,
                    created_by TEXT,
                    FOREIGN KEY (borrower_id) REFERENCES borrowers(id),
                    FOREIGN KEY (form_id) REFERENCES forms_2501(id)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS active_loans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    application_id INTEGER NOT NULL,
                    borrower_id INTEGER,
                    form_id INTEGER,
                    client_login TEXT,
                    specialist_login TEXT,
                    borrower_name TEXT,
                    contract_number TEXT,
                    monthly_income REAL,
                    status TEXT,
                    original_amount REAL,
                    outstanding_principal REAL,
                    accrued_interest REAL,
                    overdue_amount REAL,
                    overdue_days INTEGER,
                    paid_periods INTEGER,
                    rate REAL,
                    term INTEGER,
                    repayment_method TEXT,
                    issue_date TEXT,
                    last_accrual_date TEXT,
                    next_payment_date TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS active_loan_schedule (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    active_loan_id INTEGER NOT NULL,
                    period_number INTEGER,
                    payment_date TEXT,
                    principal_amount REAL,
                    interest_amount REAL,
                    total_amount REAL,
                    remaining_balance REAL,
                    FOREIGN KEY (active_loan_id) REFERENCES active_loans(id) ON DELETE CASCADE
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
                    FOREIGN KEY(active_loan_id) REFERENCES active_loans(id) ON DELETE CASCADE
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
                    borrower_name TEXT,
                    created_at TEXT,
                    created_by TEXT,
                    FOREIGN KEY (application_id) REFERENCES loan_applications(id)
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
                    FOREIGN KEY (report_id) REFERENCES final_reports(id) ON DELETE CASCADE
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS scoring_rules (
                    rule_key TEXT PRIMARY KEY,
                    rule_value REAL NOT NULL
                )
            """);
        }

        migrateExistingTables();
        seedDefaultUsers();
    }

    private void migrateExistingTables() throws SQLException {
        ensureColumn("active_loans", "borrower_id", "INTEGER");
        ensureColumn("active_loans", "form_id", "INTEGER");
        ensureColumn("active_loans", "client_login", "TEXT");
        ensureColumn("active_loans", "specialist_login", "TEXT");
        ensureColumn("active_loans", "borrower_name", "TEXT");
        ensureColumn("active_loans", "contract_number", "TEXT");
        ensureColumn("active_loans", "monthly_income", "REAL");
        ensureColumn("active_loans", "original_amount", "REAL");
        ensureColumn("active_loans", "overdue_days", "INTEGER");
        ensureColumn("active_loans", "paid_periods", "INTEGER");
        ensureColumn("active_loans", "issue_date", "TEXT");
        ensureColumn("active_loans", "last_accrual_date", "TEXT");
        ensureColumn("active_loans", "next_payment_date", "TEXT");

        ensureColumn("final_reports", "borrower_name", "TEXT");
        ensureColumn("final_reports", "created_at", "TEXT");
        ensureColumn("final_reports", "created_by", "TEXT");
    }

    private void ensureColumn(String table, String column, String type) throws SQLException {
        Set<String> existing = new HashSet<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                existing.add(rs.getString("name").toLowerCase());
            }
        }
        if (!existing.contains(column.toLowerCase())) {
            try (Statement st = connection.createStatement()) {
                st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            }
        }
    }

    private void seedDefaultUsers() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) == 0) {
                insertUser("analyst", "analyst123", CreditSpecialist.ROLE, "Иванов Алексей Александрович");
                insertUser("client", "client123", Client.ROLE, "Петров Пётр Петрович");
                insertUser("client2", "client123", Client.ROLE, "Сидорова Анна Сергеевна");
            }
        }
    }

    public void insertUser(String login, String password, String role, String fullName) throws SQLException {
        String sql = "INSERT OR IGNORE INTO users (login, password, role, full_name) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, fullName);
            ps.executeUpdate();
        }
    }

    public User authenticate(String login, String password) {
        if (login == null || password == null) return null;
        String sql = "SELECT id, login, password, role, full_name FROM users WHERE login = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                long id = rs.getLong("id");
                String role = rs.getString("role");
                String fullName = rs.getString("full_name");
                User user = CreditSpecialist.ROLE.equalsIgnoreCase(role)
                        ? new CreditSpecialist(id, rs.getString("login"), rs.getString("password"), fullName)
                        : new Client(id, rs.getString("login"), rs.getString("password"), fullName);
                user.setAuthenticated(true);
                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка аутентификации", e);
        }
    }

    public List<String> getClientLogins() {
        List<String> logins = new ArrayList<>();
        String sql = "SELECT login FROM users WHERE role = ? ORDER BY login";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, Client.ROLE);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logins.add(rs.getString("login"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось получить список клиентов", e);
        }
        return logins;
    }

    public long saveBorrower(Borrower b) {
        String sql = """
            INSERT INTO borrowers (
                last_name, first_name, middle_name, full_name, citizenship, id_number, birth_date,
                document_type, document_series_number, document_issue_date, document,
                country, region, district, locality, address, monthly_income, client_login
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getLastName());
            ps.setString(2, b.getFirstName());
            ps.setString(3, b.getMiddleName());
            ps.setString(4, b.getFullName());
            ps.setString(5, b.getCitizenship());
            ps.setString(6, b.getIdNumber());
            ps.setString(7, formatDate(b.getBirthDate()));
            ps.setString(8, b.getDocumentType());
            ps.setString(9, b.getDocumentSeriesNumber());
            ps.setString(10, formatDate(b.getDocumentIssueDate()));
            ps.setString(11, b.getDocument());
            ps.setString(12, b.getCountry());
            ps.setString(13, b.getRegion());
            ps.setString(14, b.getDistrict());
            ps.setString(15, b.getLocality());
            ps.setString(16, b.getAddress());
            setDouble(ps, 17, b.getMonthlyIncome());
            ps.setString(18, b.getClientLogin());
            ps.executeUpdate();
            long id = getGeneratedId(ps);
            b.setId(id);
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить заёмщика", e);
        }
    }

    public long saveForm2501(long borrowerId, Form2501 form) {
        String formSql = "INSERT INTO forms_2501 (borrower_id, created_at) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(formSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, borrowerId);
            ps.setString(2, formatDate(new Date()));
            ps.executeUpdate();
            long formId = getGeneratedId(ps);

            if (form.getCreditDeals() != null) {
                String dealSql = """
                    INSERT INTO credit_deals (
                        form_id, deal_type, credit_type, contract_number, conclusion_date,
                        amount, currency, term, renewal_term, repayment_date, security,
                        risk_group, relevance_date, overdue_days, overdue_amount, monthly_payment
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
                try (PreparedStatement dps = connection.prepareStatement(dealSql)) {
                    for (CreditDeal d : form.getCreditDeals()) {
                        dps.setLong(1, formId);
                        dps.setString(2, d.getDealType());
                        dps.setString(3, d.getCreditType());
                        dps.setString(4, d.getContractNumber());
                        dps.setString(5, formatDate(d.getConclusionDate()));
                        setDouble(dps, 6, d.getAmount());
                        dps.setString(7, d.getCurrency());
                        setInt(dps, 8, d.getTerm());
                        setInt(dps, 9, d.getRenewalTerm());
                        dps.setString(10, formatDate(d.getRepaymentDate()));
                        dps.setString(11, d.getSecurity());
                        dps.setString(12, d.getRiskGroup());
                        dps.setString(13, formatDate(d.getRelevanceDate()));
                        setInt(dps, 14, d.getOverdueDays());
                        setDouble(dps, 15, d.getOverdueAmount());
                        setDouble(dps, 16, d.getMonthlyPayment());
                        dps.addBatch();
                    }
                    dps.executeBatch();
                }
            }

            if (form.getOverdrafts() != null && !form.getOverdrafts().isEmpty()) {
                String ovSql = "INSERT INTO overdrafts (form_id, contract_date, limit_amount, limit_set_date) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ops = connection.prepareStatement(ovSql)) {
                    for (OverdraftInfo ov : form.getOverdrafts()) {
                        ops.setLong(1, formId);
                        ops.setString(2, formatDate(ov.getContractDate()));
                        setDouble(ops, 3, ov.getLimitAmount());
                        ops.setString(4, formatDate(ov.getLimitSetDate()));
                        ops.addBatch();
                    }
                    ops.executeBatch();
                }
            }
            return formId;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить Форму 2501", e);
        }
    }

    public Form2501 getForm2501ById(long formId) {
        if (formId <= 0) return null;
        Form2501 form = new Form2501();
        String dealSql = "SELECT * FROM credit_deals WHERE form_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(dealSql)) {
            ps.setLong(1, formId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CreditDeal d = new CreditDeal();
                    d.setDealType(rs.getString("deal_type"));
                    d.setCreditType(rs.getString("credit_type"));
                    d.setContractNumber(rs.getString("contract_number"));
                    d.setConclusionDate(parseDate(rs.getString("conclusion_date")));
                    d.setAmount(getNullableDouble(rs, "amount"));
                    d.setCurrency(rs.getString("currency"));
                    d.setTerm(getNullableInt(rs, "term"));
                    d.setRenewalTerm(getNullableInt(rs, "renewal_term"));
                    d.setRepaymentDate(parseDate(rs.getString("repayment_date")));
                    d.setSecurity(rs.getString("security"));
                    d.setRiskGroup(rs.getString("risk_group"));
                    d.setRelevanceDate(parseDate(rs.getString("relevance_date")));
                    d.setOverdueDays(getNullableInt(rs, "overdue_days"));
                    d.setOverdueAmount(getNullableDouble(rs, "overdue_amount"));
                    d.setMonthlyPayment(getNullableDouble(rs, "monthly_payment"));
                    form.getCreditDeals().add(d);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить Форму 2501", e);
        }
        return form;
    }

    public long saveApplication(LoanApplication app) {
        String sql = """
            INSERT INTO loan_applications (
                borrower_id, form_id, amount, term, rate, repayment_method, application_date, status, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (app.getBorrowerId() > 0) ps.setLong(1, app.getBorrowerId()); else ps.setNull(1, Types.INTEGER);
            if (app.getFormId() > 0) ps.setLong(2, app.getFormId()); else ps.setNull(2, Types.INTEGER);
            setDouble(ps, 3, app.getAmount());
            setInt(ps, 4, app.getTerm());
            setDouble(ps, 5, app.getRate());
            ps.setString(6, app.getRepaymentMethod());
            ps.setString(7, formatDate(app.getApplicationDate()));
            ps.setString(8, app.getStatus() != null ? app.getStatus() : "На рассмотрении");
            ps.setString(9, app.getCreatedBy());
            ps.executeUpdate();
            long id = getGeneratedId(ps);
            app.setId(id);
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить заявку", e);
        }
    }

    private void ensureApplicationExists(FinalReport report) throws SQLException {
        if (report.getApplicationId() > 0) {
            try (PreparedStatement check = connection.prepareStatement("SELECT 1 FROM loan_applications WHERE id = ?")) {
                check.setLong(1, report.getApplicationId());
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) return;
                }
            }
        }
        long borrowerId = 0;
        long formId = 0;
        if (report.getBorrower() != null) {
            borrowerId = report.getBorrower().getId() > 0
                    ? report.getBorrower().getId()
                    : saveBorrower(report.getBorrower());
        }
        if (report.getForm2501() != null && borrowerId > 0) {
            formId = saveForm2501(borrowerId, report.getForm2501());
        }
        LoanApplication app = report.getApplication() != null ? report.getApplication() : new LoanApplication();
        app.setBorrowerId(borrowerId);
        app.setFormId(formId);
        if (app.getAmount() == null && report.getSchedule() != null) {
            app.setParameters(
                    report.getSchedule().getLoanAmount(),
                    report.getSchedule().getTermMonths(),
                    report.getSchedule().getInterestRate(),
                    report.getSchedule().getMethod(),
                    new Date()
            );
        }
        long appId = saveApplication(app);
        report.setApplicationId(appId);
    }

    public void SaveReport(FinalReport report) {
        String reportSql = """
            INSERT INTO final_reports (
                application_id, pdn_value, scoring_score, risk_category, report_data,
                loan_amount, term_months, interest_rate, repayment_method,
                monthly_payment, total_amount, borrower_name, created_at, created_by
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        String paymentSql = """
            INSERT INTO payment_schedule_details (
                report_id, payment_date, principal_amount, interest_amount, total_amount, remaining_balance
            ) VALUES (?, ?, ?, ?, ?, ?)
        """;

        try {
            connection.setAutoCommit(false);
            try {
                PaymentSchedule schedule = report.getSchedule();
                DebtBurden pdn = report.getPdn();
                ScoringResult scoring = report.getScoring();

                if (schedule == null || pdn == null || scoring == null) {
                    throw new IllegalArgumentException("Отчёт должен содержать график, ПДН и скоринг");
                }

                ensureApplicationExists(report);

                long reportId;
                try (PreparedStatement ps = connection.prepareStatement(reportSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, report.getApplicationId());
                    setDouble(ps, 2, pdn.getValue());
                    if (scoring.getScore() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, scoring.getScore());
                    ps.setString(4, scoring.getRiskCategory());
                    ps.setString(5, scoring.getFactors() != null ? String.join("; ", scoring.getFactors()) : null);
                    setDouble(ps, 6, schedule.getLoanAmount());
                    setInt(ps, 7, schedule.getTermMonths());
                    setDouble(ps, 8, schedule.getInterestRate());
                    ps.setString(9, schedule.getMethod());
                    setDouble(ps, 10, schedule.getMonthlyPayment());
                    setDouble(ps, 11, schedule.getTotalAmount());
                    ps.setString(12, report.getBorrower() != null ? report.getBorrower().getFullName() : null);
                    ps.setString(13, formatDate(report.getCreatedAt() != null ? report.getCreatedAt() : new Date()));
                    ps.setString(14, report.getCreatedBy());
                    ps.executeUpdate();
                    reportId = getGeneratedId(ps);
                    report.setId(reportId);
                }

                try (PreparedStatement ps = connection.prepareStatement(paymentSql)) {
                    for (Payment payment : schedule.getPayments()) {
                        ps.setLong(1, reportId);
                        ps.setString(2, payment.getDate() != null ? formatDate(payment.getDate()) : formatDate(new Date()));
                        setDouble(ps, 3, payment.getPrincipalAmount());
                        setDouble(ps, 4, payment.getInterestAmount());
                        setDouble(ps, 5, payment.getTotalAmount());
                        setDouble(ps, 6, payment.getRemainingBalance());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw new RuntimeException("Failed to save final report. Transaction rolled back.", e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database transaction error", e);
        }
    }

    public int countReports() {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM final_reports")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось подсчитать отчёты", e);
        }
    }

    public void SaveLoan(ActiveLoan loan) {
        boolean hasExplicitId = loan.getId() > 0;
        String sql = hasExplicitId
                ? """
                    INSERT INTO active_loans (
                        id, application_id, borrower_id, form_id, client_login, specialist_login,
                        borrower_name, contract_number, monthly_income, status, original_amount,
                        outstanding_principal, accrued_interest, overdue_amount, overdue_days,
                        paid_periods, rate, term, repayment_method, issue_date, last_accrual_date, next_payment_date
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
                : """
                    INSERT INTO active_loans (
                        application_id, borrower_id, form_id, client_login, specialist_login,
                        borrower_name, contract_number, monthly_income, status, original_amount,
                        outstanding_principal, accrued_interest, overdue_amount, overdue_days,
                        paid_periods, rate, term, repayment_method, issue_date, last_accrual_date, next_payment_date
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int idx = 1;
            if (hasExplicitId) ps.setLong(idx++, loan.getId());
            ps.setLong(idx++, loan.getApplicationId());
            ps.setLong(idx++, loan.getBorrowerId());
            ps.setLong(idx++, loan.getFormId());
            ps.setString(idx++, loan.getClientLogin());
            ps.setString(idx++, loan.getSpecialistLogin());
            ps.setString(idx++, loan.getBorrowerName());
            ps.setString(idx++, loan.getContractNumber());
            setDouble(ps, idx++, loan.getMonthlyIncome());
            ps.setString(idx++, loan.getStatus());
            setDouble(ps, idx++, loan.getOriginalAmount());
            setDouble(ps, idx++, loan.getOutstandingPrincipal());
            setDouble(ps, idx++, loan.getAccruedInterest());
            setDouble(ps, idx++, loan.getOverdueAmount());
            setInt(ps, idx++, loan.getOverdueDays());
            setInt(ps, idx++, loan.getPaidPeriods());
            setDouble(ps, idx++, loan.getRate());
            setInt(ps, idx++, loan.getTerm());
            ps.setString(idx++, loan.getRepaymentMethod());
            ps.setString(idx++, formatDate(loan.getIssueDate()));
            ps.setString(idx++, formatDate(loan.getLastAccrualDate()));
            ps.setString(idx, formatDate(loan.getNextPaymentDate()));
            ps.executeUpdate();

            if (!hasExplicitId) {
                loan.setId(getGeneratedId(ps));
            }
            if (loan.getContractNumber() == null || loan.getContractNumber().isEmpty()) {
                loan.setContractNumber(String.format("КР-%04d", loan.getId()));
                try (PreparedStatement ups = connection.prepareStatement("UPDATE active_loans SET contract_number = ? WHERE id = ?")) {
                    ups.setString(1, loan.getContractNumber());
                    ups.setLong(2, loan.getId());
                    ups.executeUpdate();
                }
            }
            if (loan.getSchedule() != null) {
                saveLoanSchedule(loan.getId(), loan.getSchedule());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save loan", e);
        }
    }

    public void UpdateLoan(ActiveLoan loan) {
        String sql = """
            UPDATE active_loans SET
                application_id = ?, borrower_id = ?, form_id = ?, client_login = ?, specialist_login = ?,
                borrower_name = ?, contract_number = ?, monthly_income = ?, status = ?, original_amount = ?,
                outstanding_principal = ?, accrued_interest = ?, overdue_amount = ?, overdue_days = ?,
                paid_periods = ?, rate = ?, term = ?, repayment_method = ?,
                issue_date = ?, last_accrual_date = ?, next_payment_date = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            ps.setLong(idx++, loan.getApplicationId());
            ps.setLong(idx++, loan.getBorrowerId());
            ps.setLong(idx++, loan.getFormId());
            ps.setString(idx++, loan.getClientLogin());
            ps.setString(idx++, loan.getSpecialistLogin());
            ps.setString(idx++, loan.getBorrowerName());
            ps.setString(idx++, loan.getContractNumber());
            setDouble(ps, idx++, loan.getMonthlyIncome());
            ps.setString(idx++, loan.getStatus());
            setDouble(ps, idx++, loan.getOriginalAmount());
            setDouble(ps, idx++, loan.getOutstandingPrincipal());
            setDouble(ps, idx++, loan.getAccruedInterest());
            setDouble(ps, idx++, loan.getOverdueAmount());
            setInt(ps, idx++, loan.getOverdueDays());
            setInt(ps, idx++, loan.getPaidPeriods());
            setDouble(ps, idx++, loan.getRate());
            setInt(ps, idx++, loan.getTerm());
            ps.setString(idx++, loan.getRepaymentMethod());
            ps.setString(idx++, formatDate(loan.getIssueDate()));
            ps.setString(idx++, formatDate(loan.getLastAccrualDate()));
            ps.setString(idx++, formatDate(loan.getNextPaymentDate()));
            ps.setLong(idx, loan.getId());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Loan not found: " + loan.getId());
            }

            if (loan.getSchedule() != null) {
                saveLoanSchedule(loan.getId(), loan.getSchedule());
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
                if (!rs.next()) return null;
                ActiveLoan loan = mapLoan(rs);
                loan.setSchedule(getLoanSchedule(loan));
                loan.setPayments(getPaymentsByLoan(loan.getId()));
                return loan;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get loan", e);
        }
    }

    public List<ActiveLoan> getAllLoans() {
        return queryLoans("SELECT * FROM active_loans ORDER BY id DESC", null);
    }

    public List<ActiveLoan> getLoansByClient(String clientLogin) {
        return queryLoans("SELECT * FROM active_loans WHERE client_login = ? ORDER BY id DESC", clientLogin);
    }

    private List<ActiveLoan> queryLoans(String sql, String param) {
        List<ActiveLoan> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (param != null) ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActiveLoan loan = mapLoan(rs);
                    loan.setSchedule(getLoanSchedule(loan));
                    loan.setPayments(getPaymentsByLoan(loan.getId()));
                    loan.checkAndUpdateOverdue(new Date());
                    list.add(loan);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить список кредитов", e);
        }
        return list;
    }

    private ActiveLoan mapLoan(ResultSet rs) throws SQLException {
        ActiveLoan loan = new ActiveLoan();
        loan.setId(rs.getLong("id"));
        loan.setApplicationId(rs.getLong("application_id"));
        loan.setBorrowerId(rs.getLong("borrower_id"));
        loan.setFormId(rs.getLong("form_id"));
        loan.setClientLogin(rs.getString("client_login"));
        loan.setSpecialistLogin(rs.getString("specialist_login"));
        loan.setBorrowerName(rs.getString("borrower_name"));
        loan.setContractNumber(rs.getString("contract_number"));
        loan.setMonthlyIncome(getNullableDouble(rs, "monthly_income"));
        loan.setStatus(rs.getString("status"));
        loan.setOriginalAmount(getNullableDouble(rs, "original_amount"));
        loan.setOutstandingPrincipal(getNullableDouble(rs, "outstanding_principal"));
        loan.setAccruedInterest(getNullableDouble(rs, "accrued_interest"));
        loan.setOverdueAmount(getNullableDouble(rs, "overdue_amount"));
        loan.setOverdueDays(getNullableInt(rs, "overdue_days"));
        loan.setPaidPeriods(getNullableInt(rs, "paid_periods"));
        loan.setRate(getNullableDouble(rs, "rate"));
        loan.setTerm(getNullableInt(rs, "term"));
        loan.setRepaymentMethod(rs.getString("repayment_method"));
        loan.setIssueDate(parseDate(rs.getString("issue_date")));
        loan.setLastAccrualDate(parseDate(rs.getString("last_accrual_date")));
        loan.setNextPaymentDate(parseDate(rs.getString("next_payment_date")));
        return loan;
    }

    public void saveLoanSchedule(long activeLoanId, PaymentSchedule schedule) {
        try {
            try (PreparedStatement del = connection.prepareStatement("DELETE FROM active_loan_schedule WHERE active_loan_id = ?")) {
                del.setLong(1, activeLoanId);
                del.executeUpdate();
            }
            if (schedule == null || schedule.getPayments() == null) return;

            String sql = """
                INSERT INTO active_loan_schedule (
                    active_loan_id, period_number, payment_date,
                    principal_amount, interest_amount, total_amount, remaining_balance
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                int idx = 1;
                for (Payment p : schedule.getPayments()) {
                    ps.setLong(1, activeLoanId);
                    ps.setInt(2, p.getPeriodNumber() > 0 ? p.getPeriodNumber() : idx);
                    ps.setString(3, formatDate(p.getDate()));
                    setDouble(ps, 4, p.getPrincipalAmount());
                    setDouble(ps, 5, p.getInterestAmount());
                    setDouble(ps, 6, p.getTotalAmount());
                    setDouble(ps, 7, p.getRemainingBalance());
                    ps.addBatch();
                    idx++;
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить график кредита", e);
        }
    }

    public PaymentSchedule getLoanSchedule(ActiveLoan loan) {
        PaymentSchedule sch = new PaymentSchedule();
        sch.setLoanAmount(loan.getOutstandingPrincipal() != null ? loan.getOutstandingPrincipal() : loan.getOriginalAmount());
        sch.setTermMonths(loan.getTerm());
        sch.setInterestRate(loan.getRate());
        sch.setMethod(loan.getRepaymentMethod());
        sch.setStartDate(loan.getIssueDate());

        String sql = "SELECT * FROM active_loan_schedule WHERE active_loan_id = ? ORDER BY period_number ASC, id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loan.getId());
            try (ResultSet rs = ps.executeQuery()) {
                double total = 0.0;
                while (rs.next()) {
                    Payment p = new Payment(
                            parseDate(rs.getString("payment_date")),
                            getNullableDouble(rs, "total_amount"),
                            getNullableDouble(rs, "principal_amount"),
                            getNullableDouble(rs, "interest_amount"),
                            getNullableDouble(rs, "remaining_balance")
                    );
                    p.setPeriodNumber(rs.getInt("period_number"));
                    p.setActiveLoanId(loan.getId());
                    p.setType("ПЛАНОВЫЙ");
                    sch.getPayments().add(p);
                    total += p.getTotalAmount();
                }
                if (!sch.getPayments().isEmpty()) {
                    sch.setMonthlyPayment(sch.getPayments().get(0).getTotalAmount());
                    sch.setTotalAmount(Math.round(total * 100.0) / 100.0);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить график кредита", e);
        }
        return sch;
    }

    public void SavePayment(Payment payment) {
        String sql = """
            INSERT INTO payments (
                active_loan_id, payment_date, amount, principal_part, interest_part, remaining_balance, type
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, payment.getActiveLoanId());
            ps.setString(2, formatDate(payment.getDate()));
            setDouble(ps, 3, payment.getTotalAmount());
            setDouble(ps, 4, payment.getPrincipalAmount());
            setDouble(ps, 5, payment.getInterestAmount());
            setDouble(ps, 6, payment.getRemainingBalance());
            ps.setString(7, payment.getType());
            ps.executeUpdate();
            payment.setId(getGeneratedId(ps));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment", e);
        }
    }

    public List<Payment> getPaymentsByLoan(long activeLoanId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT * FROM payments WHERE active_loan_id = ? ORDER BY payment_date ASC, id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, activeLoanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment(
                            parseDate(rs.getString("payment_date")),
                            getNullableDouble(rs, "amount"),
                            getNullableDouble(rs, "principal_part"),
                            getNullableDouble(rs, "interest_part"),
                            getNullableDouble(rs, "remaining_balance")
                    );
                    p.setId(rs.getLong("id"));
                    p.setActiveLoanId(rs.getLong("active_loan_id"));
                    p.setType(rs.getString("type"));
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить историю платежей", e);
        }
        return list;
    }

    public void saveScoringRules(Map<String, Double> rules) {
        if (rules == null) return;
        String sql = "INSERT OR REPLACE INTO scoring_rules (rule_key, rule_value) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Double> entry : rules.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setDouble(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось сохранить правила скоринга", e);
        }
    }

    public Map<String, Double> loadScoringRules() {
        Map<String, Double> rules = new LinkedHashMap<>();
        String sql = "SELECT rule_key, rule_value FROM scoring_rules";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rules.put(rs.getString("rule_key"), rs.getDouble("rule_value"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось загрузить правила скоринга", e);
        }
        return rules;
    }

    private static long getGeneratedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) return keys.getLong(1);
        }
        throw new SQLException("Не удалось получить сгенерированный ID");
    }

    private static String formatDate(Date date) {
        return date == null ? null : new SimpleDateFormat(DATE_PATTERN).format(date);
    }

    private static Date parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(text.trim());
        } catch (ParseException e) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(text.trim());
            } catch (ParseException ignored) {
                return null;
            }
        }
    }

    private static void setDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) ps.setNull(index, Types.REAL); else ps.setDouble(index, value);
    }

    private static void setInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER); else ps.setInt(index, value);
    }

    private static Double getNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    @Override
    public void close() {
        try {
            if (!connection.isClosed()) connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close database", e);
        }
    }
}
