package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.LoanDAO;
import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanPayment;
import com.edj.developer.apploans.model.LoanSummary;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoanDAOImpl implements LoanDAO {
    private static final Logger log = LoggerFactory.getLogger(LoanDAOImpl.class);

    @Override
    public List<Double> findAllSuggestedAmounts() {
        List<Double> amounts = new ArrayList<>();
        String sql = "SELECT value FROM config_amounts;";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                amounts.add(rs.getDouble("value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return amounts;
    }

    @Override
    public List<String> findAllFrequencies() {
        List<String> frequencies = new ArrayList<>();
        String sql = "SELECT name FROM config_frequencies;";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                frequencies.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return frequencies;
    }

    @Override
    public boolean saveFullLoan(Loan loan) {
        // Agregamos la columna 'status' explícitamente como 'ACTIVE' al crear el préstamo
        String sqlLoan = "INSERT INTO loans (customer_id, amount, interest_rate, installments, frequency, start_date, total_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
        String sqlPayment = "INSERT INTO loan_payments (loan_id, installment_number, amount, due_date, status) VALUES (?, ?, ?, ?, 'PENDING')";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            int loanId;
            // 1. Insertar Préstamo
            try (PreparedStatement stmtLoan = conn.prepareStatement(sqlLoan, Statement.RETURN_GENERATED_KEYS)) {
                stmtLoan.setInt(1, loan.getCustomerId());
                stmtLoan.setDouble(2, loan.getAmount());
                stmtLoan.setDouble(3, loan.getInterestRate());
                stmtLoan.setInt(4, loan.getInstallments());
                stmtLoan.setString(5, loan.getFrequency());
                stmtLoan.setString(6, loan.getStartDate());
                stmtLoan.setDouble(7, loan.getTotalAmount());

                int affectedRows = stmtLoan.executeUpdate();
                if (affectedRows == 0) throw new SQLException("Fallo al insertar el préstamo.");

                try (ResultSet generatedKeys = stmtLoan.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        loanId = generatedKeys.getInt(1);
                        loan.setId(loanId);
                    } else {
                        throw new SQLException("No se obtuvo el ID del préstamo.");
                    }
                }
            }

            // 2. Insertar Cuotas (Payments)
            try (PreparedStatement stmtPay = conn.prepareStatement(sqlPayment)) {
                for (LoanPayment p : loan.getPayments()) {
                    stmtPay.setInt(1, loanId);
                    stmtPay.setInt(2, p.getInstallmentNumber());
                    stmtPay.setDouble(3, p.getAmount());

                    String dueDate = (p.getDueDate() != null) ? p.getDueDate() : LocalDate.now().toString();
                    stmtPay.setString(4, dueDate);

                    stmtPay.addBatch();
                }
                stmtPay.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @Override
    public List<Loan> findAll() {
        List<Loan> loans = new ArrayList<>();
        String sql = "SELECT l.*, c.first_name, c.last_name FROM loans l " +
                "INNER JOIN customers c ON l.customer_id = c.id ORDER BY l.id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Loan loan = new Loan();
                loan.setId(rs.getInt("id"));
                loan.setCustomerId(rs.getInt("customer_id"));
                loan.setAmount(rs.getDouble("amount"));
                loan.setInterestRate(rs.getDouble("interest_rate"));
                loan.setTotalAmount(rs.getDouble("total_amount"));
                loan.setInstallments(rs.getInt("installments"));
                loan.setFrequency(rs.getString("frequency"));
                loan.setStartDate(rs.getString("start_date"));
                loan.setStatus(rs.getString("status")); // Mapeamos el estado correctamente
                loan.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));
                loans.add(loan);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loans;
    }

    @Override
    public boolean updatePaymentStatus(int paymentId, String newStatus) {
        String sql = "UPDATE loan_payments SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, paymentId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar estado del pago: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Loan findFullLoanById(int loanId) {
        String sqlLoan = "SELECT l.*, c.first_name, c.last_name FROM loans l " +
                "INNER JOIN customers c ON l.customer_id = c.id WHERE l.id = ?";
        String sqlPayments = "SELECT * FROM loan_payments WHERE loan_id = ? ORDER BY installment_number ASC";

        Loan loan = null;

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sqlLoan)) {
                pstmt.setInt(1, loanId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        loan = new Loan();
                        loan.setId(rs.getInt("id"));
                        loan.setAmount(rs.getDouble("amount"));
                        loan.setTotalAmount(rs.getDouble("total_amount"));
                        loan.setInstallments(rs.getInt("installments"));
                        loan.setFrequency(rs.getString("frequency"));
                        loan.setStartDate(rs.getString("start_date"));
                        loan.setStatus(rs.getString("status"));
                        loan.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));
                    }
                }
            }

            if (loan != null) {
                List<LoanPayment> payments = new ArrayList<>();
                try (PreparedStatement pstmt = conn.prepareStatement(sqlPayments)) {
                    pstmt.setInt(1, loanId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            LoanPayment p = new LoanPayment(
                                    rs.getInt("installment_number"),
                                    rs.getDouble("amount"),
                                    rs.getString("due_date")
                            );
                            p.setId(rs.getInt("id"));
                            p.setLoanId(rs.getInt("loan_id"));
                            p.setStatus(rs.getString("status"));
                            p.setPaidAmount(rs.getDouble("paid_amount"));
                            payments.add(p);
                        }
                        loan.setPayments(payments);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loan;
    }

    @Override
    public boolean updatePaymentStatusWithAmount(int paymentId, String newStatus, double paidAmount) {
        String sqlUpdatePayment = "UPDATE loan_payments SET status = ?, paid_amount = ? WHERE id = ?";
        String sqlCheckLoan = "SELECT loan_id FROM loan_payments WHERE id = ?";
        String sqlCountPending = "SELECT COUNT(*) FROM loan_payments WHERE loan_id = ? AND status = 'PENDING'";
        String sqlUpdateLoanStatus = "UPDATE loans SET status = 'COMPLETED' WHERE id = ?"; // Corregido a COMPLETED

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            int loanId = -1;

            // 1. Obtener el ID del préstamo
            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheckLoan)) {
                psCheck.setInt(1, paymentId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) loanId = rs.getInt("loan_id");
                }
            }

            // 2. Actualizar la cuota actual
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdatePayment)) {
                pstmt.setString(1, newStatus.toUpperCase());
                pstmt.setDouble(2, paidAmount);
                pstmt.setInt(3, paymentId);
                pstmt.executeUpdate();
            }

            // 3. Verificar si quedan cuotas pendientes para cerrar el préstamo
            if (loanId != -1) {
                int pendingCount = 0;
                try (PreparedStatement psCount = conn.prepareStatement(sqlCountPending)) {
                    psCount.setInt(1, loanId);
                    try (ResultSet rs = psCount.executeQuery()) {
                        if (rs.next()) pendingCount = rs.getInt(1);
                    }
                }

                // 4. Si da 0, el préstamo pasa a COMPLETED de forma automática
                if (pendingCount == 0) {
                    try (PreparedStatement psUpLoan = conn.prepareStatement(sqlUpdateLoanStatus)) {
                        psUpLoan.setInt(1, loanId);
                        psUpLoan.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public LoanSummary getSummaryByCustomerId(int customerId) {
        // Query corregida: Eliminada duplicación de COMPLETED y unificado a mayúsculas estándar
        String sql = """
        SELECT 
            COUNT(l.id) as total,
            SUM(CASE WHEN l.status = 'ACTIVE' THEN 1 ELSE 0 END) as activos,
            SUM(CASE WHEN l.status = 'COMPLETED' THEN 1 ELSE 0 END) as completados,
            COALESCE(
                (SELECT SUM(p.amount - p.paid_amount) 
                 FROM loan_payments p 
                 JOIN loans l2 ON p.loan_id = l2.id 
                 WHERE l2.customer_id = ? AND l2.status = 'ACTIVE'
                ), 0.0
            ) as saldo_pendiente
        FROM loans l
        WHERE l.customer_id = ?
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, customerId);
            pstmt.setInt(2, customerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new LoanSummary(
                            rs.getInt("total"),
                            rs.getInt("activos"),
                            rs.getInt("completados"),
                            rs.getDouble("saldo_pendiente")
                    );
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).error("Error al obtener resumen de préstamos", e);
        }
        return new LoanSummary(0, 0, 0, 0.0);
    }

    @Override
    public List<Loan> findLoansByCustomerId(int customerId) {
        List<Loan> loans = new ArrayList<>();
        String sql = """
        SELECT l.*, 
               COALESCE((SELECT SUM(p.amount - p.paid_amount) FROM loan_payments p WHERE p.loan_id = l.id), 0.0) as restante
        FROM loans l 
        WHERE l.customer_id = ? 
        ORDER BY l.id DESC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Loan loan = new Loan();
                    loan.setId(rs.getInt("id"));
                    loan.setAmount(rs.getDouble("amount"));
                    loan.setTotalAmount(rs.getDouble("total_amount"));
                    loan.setStatus(rs.getString("status"));
                    loan.setStartDate(rs.getString("start_date"));
                    loan.setInterestRate(rs.getDouble("restante")); // Mapeo temporal del saldo restante
                    loans.add(loan);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loans;
    }

    @Override
    public Map<String, Integer> findAllFrequenciesWithIntervals() {
        Map<String, Integer> frequencies = new HashMap<>();
        String sql = "SELECT name, days_interval FROM config_frequencies;";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                frequencies.put(rs.getString("name").toUpperCase(), rs.getInt("days_interval"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return frequencies;
    }

    @Override
    public void checkAndUpdateOverdueInstallments() {
        // Usamos date(due_date) para obligar a SQLite a interpretarlo de manera correcta
        String sql = """
        UPDATE loan_payments 
        SET status = 'OVERDUE' 
        WHERE (status = 'PENDING' OR status = 'PARTIAL' OR status IS NULL) 
          AND date(due_date) < date('now', 'localtime')
        """;

        try (java.sql.Connection conn = com.edj.developer.apploans.config.DatabaseConfig.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Se actualizaron " + rowsAffected + " cuotas a estado VENCIDA (OVERDUE).");
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }
}