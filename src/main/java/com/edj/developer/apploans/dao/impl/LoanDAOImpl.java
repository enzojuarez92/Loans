package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.LoanDAO;
import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanPayment;
import com.edj.developer.apploans.model.LoanReceipt;
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
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
        return frequencies;
    }

    @Override
    public boolean saveFullLoan(Loan loan) {
        String sqlLoan = "INSERT INTO loans (customer_id, amount, interest_rate, installments, frequency, start_date, total_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
        String sqlPayment = "INSERT INTO loan_payments (loan_id, installment_number, amount, due_date, status) VALUES (?, ?, ?, ?, 'PENDING')";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            int loanId;
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
        String sql = "SELECT l.*, c.first_name, c.last_name FROM loans l INNER JOIN customers c ON l.customer_id = c.id ORDER BY l.id DESC";
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
                loan.setStatus(rs.getString("status"));
                loan.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));
                loans.add(loan);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return loans;
    }

    @Override
    public List<Loan> findAllPaged(String search, String statusFilter, int limit, int offset) {
        List<Loan> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT l.*, c.first_name, c.last_name 
            FROM loans l 
            INNER JOIN customers c ON l.customer_id = c.id 
            WHERE 1=1
            """);

        // Buscador Inteligente: Nombre, N° de préstamo (ID), Monto o Monto Total
        if (search != null && !search.trim().isEmpty()) {
            sql.append("""
                 AND (
                     (c.first_name || ' ' || c.last_name) LIKE ? 
                     OR CAST(l.id AS TEXT) LIKE ? 
                     OR CAST(l.amount AS TEXT) LIKE ? 
                     OR CAST(l.total_amount AS TEXT) LIKE ?
                 )
                """);
        }
        if (statusFilter != null && !statusFilter.isEmpty() && !"TODOS".equals(statusFilter)) {
            sql.append(" AND l.status = ?");
        }

        sql.append(" ORDER BY l.id DESC LIMIT ? OFFSET ?");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (search != null && !search.trim().isEmpty()) {
                String filter = "%" + search.trim() + "%";
                ps.setString(idx++, filter); // Nombre del cliente
                ps.setString(idx++, filter); // ID del préstamo
                ps.setString(idx++, filter); // Monto original
                ps.setString(idx++, filter); // Monto total con intereses
            }
            if (statusFilter != null && !statusFilter.isEmpty() && !"TODOS".equals(statusFilter)) {
                ps.setString(idx++, statusFilter);
            }

            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);

            try (ResultSet rs = ps.executeQuery()) {
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
                    loan.setStatus(rs.getString("status"));
                    loan.setCustomerName(rs.getString("first_name") + " " + rs.getString("last_name"));
                    list.add(loan);
                }
            }
        } catch (SQLException e) { log.error("Error paginando préstamos", e); }
        return list;
    }

    @Override
    public int countLoans(String search, String statusFilter) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*) FROM loans l 
            INNER JOIN customers c ON l.customer_id = c.id 
            WHERE 1=1
            """);

        // Misma lógica exacta de condiciones para el conteo
        if (search != null && !search.trim().isEmpty()) {
            sql.append("""
                 AND (
                     (c.first_name || ' ' || c.last_name) LIKE ? 
                     OR CAST(l.id AS TEXT) LIKE ? 
                     OR CAST(l.amount AS TEXT) LIKE ? 
                     OR CAST(l.total_amount AS TEXT) LIKE ?
                 )
                """);
        }
        if (statusFilter != null && !statusFilter.isEmpty() && !"TODOS".equals(statusFilter)) {
            sql.append(" AND l.status = ?");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (search != null && !search.trim().isEmpty()) {
                String filter = "%" + search.trim() + "%";
                ps.setString(idx++, filter); // Nombre del cliente
                ps.setString(idx++, filter); // ID del préstamo
                ps.setString(idx++, filter); // Monto original
                ps.setString(idx++, filter); // Monto total con intereses
            }
            if (statusFilter != null && !statusFilter.isEmpty() && !"TODOS".equals(statusFilter)) {
                ps.setString(idx++, statusFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { log.error(e.getMessage()); }
        return 0;
    }

    @Override
    public boolean updatePaymentStatus(int paymentId, String newStatus) {
        String sql = "UPDATE loan_payments SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus.toUpperCase());
            pstmt.setInt(2, paymentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public Loan findFullLoanById(int loanId) {
        String sqlLoan = "SELECT l.*, c.first_name, c.last_name FROM loans l INNER JOIN customers c ON l.customer_id = c.id WHERE l.id = ?";
        String sqlPayments = "SELECT * FROM loan_payments WHERE loan_id = ? ORDER BY installment_number ASC";
        // 💡 NUEVA CONSULTA: Traemos el historial de entregas de este préstamo
        String sqlHistory = "SELECT id, amount, payment_date, notes FROM payment_history WHERE loan_id = ? ORDER BY id DESC";

        Loan loan = null;

        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. Mapeamos los datos generales del Préstamo
            try (PreparedStatement pstmt = conn.prepareStatement(sqlLoan)) {
                pstmt.setInt(1, loanId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        loan = new Loan();
                        loan.setId(rs.getInt("id"));
                        loan.setCustomerId(rs.getInt("customer_id"));
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
                // 2. Mapeamos el Plan de Cuotas
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
                            p.setPaymentDate(rs.getString("payment_date"));
                            payments.add(p);
                        }
                        loan.setPayments(payments);
                    }
                }

                // 3. 💡 MAPEAMOS LAS ENTREGAS REALES (HISTORIAL)
                List<LoanReceipt> receipts = new ArrayList<>();
                try (PreparedStatement pstmt = conn.prepareStatement(sqlHistory)) {
                    pstmt.setInt(1, loanId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            LoanReceipt receipt = new LoanReceipt(
                                    rs.getInt("id"),
                                    rs.getDouble("amount"),
                                    rs.getString("payment_date"),
                                    rs.getString("notes")
                            );
                            receipts.add(receipt);
                        }
                        loan.setReceipts(receipts); // Se lo inyectamos al préstamo fresco
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
        // MODIFICADO: Agregamos payment_date con el datetime actual de SQLite
        String sqlUpdatePayment = """
        UPDATE loan_payments 
        SET status = ?, 
            paid_amount = ?, 
            payment_date = datetime('now', 'localtime') 
        WHERE id = ?
        """;
        String sqlCheckLoan = "SELECT loan_id FROM loan_payments WHERE id = ?";
        String sqlCountPending = "SELECT COUNT(*) FROM loan_payments WHERE loan_id = ? AND status = 'PENDING'";
        String sqlUpdateLoanStatus = "UPDATE loans SET status = 'COMPLETED' WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            int loanId = -1;

            try (PreparedStatement psCheck = conn.prepareStatement(sqlCheckLoan)) {
                psCheck.setInt(1, paymentId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) loanId = rs.getInt("loan_id");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdatePayment)) {
                pstmt.setString(1, newStatus.toUpperCase());
                pstmt.setDouble(2, paidAmount);
                pstmt.setInt(3, paymentId); // Sigue siendo el parámetro 3 porque el datetime() no usa "?"
                pstmt.executeUpdate();
            }

            if (loanId != -1) {
                int pendingCount = 0;
                try (PreparedStatement psCount = conn.prepareStatement(sqlCountPending)) {
                    psCount.setInt(1, loanId);
                    try (ResultSet rs = psCount.executeQuery()) {
                        if (rs.next()) pendingCount = rs.getInt(1);
                    }
                }

                if (pendingCount == 0) {
                    try (PreparedStatement psUpLoan = conn.prepareStatement(sqlUpdateLoanStatus)) {
                        psUpLoan.setInt(1, loanId);
                        psUpLoan.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    @Override
    public LoanSummary getSummaryByCustomerId(int customerId) {
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
        } catch (Exception e) { log.error("Error al obtener resumen de préstamos", e); }
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
                    loan.setInterestRate(rs.getDouble("restante"));
                    loans.add(loan);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
        return frequencies;
    }

    @Override
    public void checkAndUpdateOverdueInstallments() {
        String sql = """
        UPDATE loan_payments 
        SET status = 'OVERDUE' 
        WHERE (status = 'PENDING' OR status = 'PARTIAL' OR status IS NULL) 
          AND date(due_date) < date('now', 'localtime')
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                log.info("Se actualizaron " + rowsAffected + " cuotas a estado VENCIDA (OVERDUE).");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public boolean cancelLoan(int loanId) {
        String sqlUpdateLoan = "UPDATE loans SET status = 'CANCELED' WHERE id = ?";
        String sqlUpdatePayments = "UPDATE loan_payments SET status = 'CANCELED' WHERE loan_id = ? AND status = 'PENDING'";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Iniciamos transacción por seguridad

            try (PreparedStatement psLoan = conn.prepareStatement(sqlUpdateLoan)) {
                psLoan.setInt(1, loanId);
                psLoan.executeUpdate();
            }

            try (PreparedStatement psPayments = conn.prepareStatement(sqlUpdatePayments)) {
                psPayments.setInt(1, loanId);
                psPayments.executeUpdate();
            }

            conn.commit(); // Confirmamos los cambios si todo salió joya
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean processCascadePayment(int loanId, double totalAmount, String notes) {
        String sqlInsertHistory = """
        INSERT INTO payment_history (loan_id, amount, payment_date, notes) 
        VALUES (?, ?, datetime('now', 'localtime'), ?)
        """;

        String sqlSelectPayments = """
        SELECT id, amount, paid_amount 
        FROM loan_payments 
        WHERE loan_id = ? AND status != 'PAID' AND status != 'CANCELED'
        ORDER BY installment_number ASC
        """;

        String sqlUpdatePayment = """
        UPDATE loan_payments 
        SET paid_amount = ?, 
            status = ?, 
            payment_date = datetime('now', 'localtime') 
        WHERE id = ?
        """;

        String sqlCountPending = "SELECT COUNT(*) FROM loan_payments WHERE loan_id = ? AND status != 'PAID' AND status != 'CANCELED'";
        String sqlUpdateLoanStatus = "UPDATE loans SET status = 'COMPLETED' WHERE id = ?";

        // 💡 Estructura auxiliar para guardar las cuotas temporales en memoria
        class TempCuota {
            int id;
            double amount;
            double paidAmount;
            TempCuota(int id, double amount, double paidAmount) {
                this.id = id; this.amount = amount; this.paidAmount = paidAmount;
            }
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // 🔒 Transacción segura

            // 1. Insertar el recibo en el historial de pagos
            try (PreparedStatement psHist = conn.prepareStatement(sqlInsertHistory)) {
                psHist.setInt(1, loanId);
                psHist.setDouble(2, totalAmount);
                psHist.setString(3, (notes == null || notes.trim().isEmpty()) ? "Cobro en cascada" : notes.trim());
                psHist.executeUpdate();
            }

            // 2. LEER LAS CUOTAS EN MEMORIA (Para liberar el driver de SQLite de inmediato)
            List<TempCuota> cuotasPendientes = new ArrayList<>();
            try (PreparedStatement psSel = conn.prepareStatement(sqlSelectPayments)) {
                psSel.setInt(1, loanId);
                try (ResultSet rs = psSel.executeQuery()) {
                    while (rs.next()) {
                        cuotasPendientes.add(new TempCuota(
                                rs.getInt("id"),
                                rs.getDouble("amount"),
                                rs.getDouble("paid_amount")
                        ));
                    }
                } // El ResultSet y el PreparedStatement se cierran ACÁ automáticamente
            }

            // 3. PROCESAR EL DERRAME EN CASCADA USANDO LA LISTA EN MEMORIA
            double dineroRestante = totalAmount;

            try (PreparedStatement psUpPay = conn.prepareStatement(sqlUpdatePayment)) {
                for (TempCuota cuota : cuotasPendientes) {
                    if (dineroRestante <= 0) break;

                    double deudoCuota = cuota.amount - cuota.paidAmount;

                    if (dineroRestante >= deudoCuota) {
                        dineroRestante -= deudoCuota;

                        psUpPay.setDouble(1, cuota.amount);
                        psUpPay.setString(2, "PAID");
                        psUpPay.setInt(3, cuota.id);
                        psUpPay.addBatch();
                    } else {
                        double nuevoPaidAmount = cuota.paidAmount + dineroRestante;
                        dineroRestante = 0;

                        psUpPay.setDouble(1, nuevoPaidAmount);
                        psUpPay.setString(2, "PARTIAL");
                        psUpPay.setInt(3, cuota.id);
                        psUpPay.addBatch();
                    }
                }

                // Si la lista no estaba vacía y se agregaron comandos, ejecutamos el lote
                if (!cuotasPendientes.isEmpty()) {
                    psUpPay.executeBatch();
                }
            }

            // 4. Chequear si cerramos el préstamo completo
            int pendingCount = 0;
            try (PreparedStatement psCount = conn.prepareStatement(sqlCountPending)) {
                psCount.setInt(1, loanId);
                try (ResultSet rs = psCount.executeQuery()) {
                    if (rs.next()) pendingCount = rs.getInt(1);
                }
            }

            if (pendingCount == 0) {
                try (PreparedStatement psUpLoan = conn.prepareStatement(sqlUpdateLoanStatus)) {
                    psUpLoan.setInt(1, loanId);
                    psUpLoan.executeUpdate();
                }
            }

            conn.commit(); // Éxito total y persistencia atómica
            return true;

        } catch (SQLException e) {
            log.error("Cascading payment failed. Rollback executed.", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
        }
    }

    @Override
    public boolean revertLastPayment(int receiptId, int loanId, double amount, int targetInstallmentId) {
        String sqlDeleteReceipt = "DELETE FROM payment_history WHERE id = ?";
        String sqlUpdateLoan = "UPDATE loans SET status = 'ACTIVE' WHERE id = ?";

        // Traemos todas las cuotas del préstamo que tengan algún pago, ordenadas de la ÚLTIMA a la PRIMERA
        String sqlSelectPaymentsToRevert = """
        SELECT id, amount, paid_amount 
        FROM loan_payments 
        WHERE loan_id = ? AND paid_amount > 0
        ORDER BY installment_number DESC
    """;

        String sqlUpdateInstallment = """
        UPDATE loan_payments 
        SET paid_amount = ?, 
            status = ?, 
            payment_date = CASE WHEN ? > 0 THEN payment_date ELSE NULL END 
        WHERE id = ?
    """;

        // Clase auxiliar temporal para la reversión en memoria
        class TempCuotaRevert {
            int id; double amount; double paidAmount;
            TempCuotaRevert(int id, double amount, double paidAmount) {
                this.id = id; this.amount = amount; this.paidAmount = paidAmount;
            }
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false); // 🔒 Transacción atómica

            // 1. Borramos el recibo físico del historial
            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteReceipt)) {
                ps.setInt(1, receiptId);
                ps.executeUpdate();
            }

            // 2. Cargamos las cuotas afectadas en memoria para no bloquear SQLite
            List<TempCuotaRevert> cuotasAFijar = new ArrayList<>();
            try (PreparedStatement psSel = conn.prepareStatement(sqlSelectPaymentsToRevert)) {
                psSel.setInt(1, loanId);
                try (ResultSet rs = psSel.executeQuery()) {
                    while (rs.next()) {
                        cuotasAFijar.add(new TempCuotaRevert(
                                rs.getInt("id"),
                                rs.getDouble("amount"),
                                rs.getDouble("paid_amount")
                        ));
                    }
                }
            }

            // 3. Procesamos la CASCADA INVERSA (quitando la plata desde la última cuota tocada hacia atrás)
            double dineroPorDevolver = amount;

            try (PreparedStatement psUpPay = conn.prepareStatement(sqlUpdateInstallment)) {
                for (TempCuotaRevert cuota : cuotasAFijar) {
                    if (dineroPorDevolver <= 0) break;

                    // Si lo que hay que devolver es mayor o igual a lo que se pagó en esta cuota
                    if (dineroPorDevolver >= cuota.paidAmount) {
                        dineroPorDevolver -= cuota.paidAmount;

                        // La cuota vuelve a cero absoluto (PENDING)
                        psUpPay.setDouble(1, 0.0);
                        psUpPay.setString(2, "PENDING");
                        psUpPay.setDouble(3, 0.0);
                        psUpPay.setInt(4, cuota.id);
                        psUpPay.addBatch();
                    } else {
                        // Si el dinero por devolver es menor, solo le restamos un cacho a esta cuota
                        double nuevoPaidAmount = cuota.paidAmount - dineroPorDevolver;
                        dineroPorDevolver = 0; // Satisfecho

                        psUpPay.setDouble(1, nuevoPaidAmount);
                        psUpPay.setString(2, "PARTIAL");
                        psUpPay.setDouble(3, nuevoPaidAmount);
                        psUpPay.setInt(4, cuota.id);
                        psUpPay.addBatch();
                    }
                }

                if (!cuotasAFijar.isEmpty()) {
                    psUpPay.executeBatch();
                }
            }

            // 4. Forzamos el estado ACTIVE en el préstamo general por si estaba en COMPLETED
            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateLoan)) {
                ps.setInt(1, loanId);
                ps.executeUpdate();
            }

            conn.commit(); // 🚀 Guardamos todo junto si no hubo fallos
            log.info("🔄 Reversión en cascada completada para el préstamo ID: {}", loanId);
            return true;
        } catch (SQLException e) {
            log.error("Fallo la reversión en cascada. Aplicando Rollback.", e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}