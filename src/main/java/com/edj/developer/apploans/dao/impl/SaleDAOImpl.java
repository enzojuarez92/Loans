package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.SaleDAO;
import com.edj.developer.apploans.model.Sale;
import com.edj.developer.apploans.model.SalePayment;
import com.edj.developer.apploans.model.SaleReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SaleDAOImpl implements SaleDAO {
    private static final Logger log = LoggerFactory.getLogger(SaleDAOImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public boolean saveWithTransaction(Sale sale, int daysInterval) {
        String insertSaleSql = """
            INSERT INTO sales (customer_id, product_id, selling_price, interest_rate, total_amount, installments, frequency, start_date, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
            """;

        String insertPaymentSql = """
            INSERT INTO sales_payments (sale_id, installment_no, amount, due_date, status)
            VALUES (?, ?, ?, ?, 'PENDING')
            """;

        String checkStockSql = "SELECT stock, name FROM products WHERE id = ?";
        String updateStockSql = "UPDATE products SET stock = stock - 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psCheck = conn.prepareStatement(checkStockSql)) {
                psCheck.setInt(1, sale.getProductId());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        int currentStock = rs.getInt("stock");
                        String prodName = rs.getString("name");
                        if (currentStock <= 0) {
                            throw new SQLException("Sin unidades disponibles en inventario para: " + prodName);
                        }
                    } else {
                        throw new SQLException("El producto seleccionado no existe.");
                    }
                }
            }

            try (PreparedStatement psUpdate = conn.prepareStatement(updateStockSql)) {
                psUpdate.setInt(1, sale.getProductId());
                psUpdate.executeUpdate();
            }

            int saleId = 0;
            try (PreparedStatement psSale = conn.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS)) {
                psSale.setInt(1, sale.getCustomerId());
                psSale.setInt(2, sale.getProductId());
                psSale.setDouble(3, sale.getSellingPrice());
                psSale.setDouble(4, sale.getInterestRate());
                psSale.setDouble(5, sale.getTotalAmount());
                psSale.setInt(6, sale.getInstallments());
                psSale.setString(7, sale.getFrequency());
                psSale.setString(8, sale.getStartDate());
                psSale.executeUpdate();

                try (ResultSet rs = psSale.getGeneratedKeys()) {
                    if (rs.next()) saleId = rs.getInt(1);
                }
            }

            if (saleId == 0) throw new SQLException("Error al generar el ID de la venta.");

            double installmentAmount = sale.getTotalAmount() / sale.getInstallments();
            LocalDate dueDate = LocalDate.parse(sale.getStartDate());

            try (PreparedStatement psPay = conn.prepareStatement(insertPaymentSql)) {
                for (int i = 1; i <= sale.getInstallments(); i++) {
                    dueDate = dueDate.plusDays(daysInterval);

                    psPay.setInt(1, saleId);
                    psPay.setInt(2, i);
                    psPay.setDouble(3, installmentAmount);
                    psPay.setString(4, dueDate.format(DATE_FORMATTER));
                    psPay.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { log.error("Error rollback", ex); } }
            throw new RuntimeException(e.getMessage());
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { log.error(e.getMessage()); } }
        }
    }

    @Override
    public List<Sale> findAllPaged(String search, String statusFilter, int limit, int offset) {
        List<Sale> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
        SELECT s.*, (c.first_name || ' ' || c.last_name) AS customer_name, p.name AS product_name 
        FROM sales s
        JOIN customers c ON s.customer_id = c.id
        JOIN products p ON s.product_id = p.id
        WHERE 1=1
        """);

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND ((c.first_name || ' ' || c.last_name) LIKE ? OR p.name LIKE ?)");
        }
        if (statusFilter != null && !statusFilter.isEmpty() && !"TODOS".equals(statusFilter)) {
            sql.append(" AND s.status = ?");
        }

        sql.append(" ORDER BY s.id DESC LIMIT ? OFFSET ?");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (search != null && !search.trim().isEmpty()) {
                String filter = "%" + search.trim() + "%";
                ps.setString(idx++, filter);
                ps.setString(idx++, filter);
            }
            if (statusFilter != null && !statusFilter.isEmpty() && !"TODOS".equals(statusFilter)) {
                ps.setString(idx++, statusFilter);
            }

            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setId(rs.getInt("id"));
                    s.setCustomerId(rs.getInt("customer_id"));
                    s.setCustomerName(rs.getString("customer_name"));
                    s.setProductId(rs.getInt("product_id"));
                    s.setProductName(rs.getString("product_name"));
                    s.setSellingPrice(rs.getDouble("selling_price"));
                    s.setInterestRate(rs.getDouble("interest_rate"));
                    s.setTotalAmount(rs.getDouble("total_amount"));
                    s.setInstallments(rs.getInt("installments"));
                    s.setFrequency(rs.getString("frequency"));
                    s.setStartDate(rs.getString("start_date"));
                    s.setStatus(rs.getString("status"));
                    s.setCreatedAt(rs.getString("created_at"));
                    list.add(s);
                }
            }
        } catch (SQLException e) { log.error("Error listando ventas", e); }
        return list;
    }

    @Override
    public int countSales(String search, String statusFilter) {
        StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*) FROM sales s 
        JOIN customers c ON s.customer_id = c.id 
        JOIN products p ON s.product_id = p.id
        WHERE 1=1
        """);

        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND ((c.first_name || ' ' || c.last_name) LIKE ? OR p.name LIKE ?)");
        }
        if (statusFilter != null && !statusFilter.isEmpty() && !"TODOS".equals(statusFilter)) {
            sql.append(" AND s.status = ?");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (search != null && !search.trim().isEmpty()) {
                String filter = "%" + search.trim() + "%";
                ps.setString(idx++, filter);
                ps.setString(idx++, filter);
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
    public Sale findFullSaleById(int id) {
        Sale s = null;
        // 💡 MODIFICADO: Agregados c.phone, c.address y c.email a la consulta SQL
        String saleSql = """
        SELECT s.*, 
               (c.first_name || ' ' || c.last_name) AS customer_name, 
               c.phone AS customer_phone, 
               c.address AS customer_address, 
               c.email AS customer_email, 
               p.name AS product_name 
        FROM sales s
        JOIN customers c ON s.customer_id = c.id
        JOIN products p ON s.product_id = p.id
        WHERE s.id = ?
        """;

        String paymentsSql = "SELECT * FROM sales_payments WHERE sale_id = ? ORDER BY installment_no ASC";
        String historySql = "SELECT id, amount, payment_date, notes FROM payment_history WHERE sale_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psSale = conn.prepareStatement(saleSql)) {

            psSale.setInt(1, id);
            try (ResultSet rs = psSale.executeQuery()) {
                if (rs.next()) {
                    s = new Sale();
                    s.setId(rs.getInt("id"));
                    s.setCustomerId(rs.getInt("customer_id"));
                    s.setCustomerName(rs.getString("customer_name"));
                    s.setProductId(rs.getInt("product_id"));
                    s.setProductName(rs.getString("product_name"));
                    s.setSellingPrice(rs.getDouble("selling_price"));
                    s.setInterestRate(rs.getDouble("interest_rate"));
                    s.setTotalAmount(rs.getDouble("total_amount"));
                    s.setInstallments(rs.getInt("installments"));
                    s.setFrequency(rs.getString("frequency"));
                    s.setStartDate(rs.getString("start_date"));
                    s.setStatus(rs.getString("status"));
                    s.setCreatedAt(rs.getString("created_at"));

                    // 💡 AGREGADO: Mapeo de la información de contacto hacia el objeto Sale
                    s.setCustomerPhone(rs.getString("customer_phone"));
                    s.setCustomerAddress(rs.getString("customer_address"));
                    s.setCustomerEmail(rs.getString("customer_email"));
                }
            }

            if (s != null) {
                // 1. Cargar plan de cuotas
                List<SalePayment> pList = new ArrayList<>();
                try (PreparedStatement psPay = conn.prepareStatement(paymentsSql)) {
                    psPay.setInt(1, id);
                    try (ResultSet rsPay = psPay.executeQuery()) {
                        while (rsPay.next()) {
                            SalePayment sp = new SalePayment(
                                    rsPay.getInt("id"),
                                    rsPay.getInt("sale_id"),
                                    rsPay.getInt("installment_no"),
                                    rsPay.getDouble("amount"),
                                    rsPay.getDouble("paid_amount"),
                                    rsPay.getString("due_date"),
                                    rsPay.getString("status")
                            );
                            pList.add(sp);
                        }
                    }
                }
                s.setPayments(pList);

                // 2. Cargar historial usando SaleReceipt
                List<SaleReceipt> receipts = new ArrayList<>();
                try (PreparedStatement psHist = conn.prepareStatement(historySql)) {
                    psHist.setInt(1, id);
                    try (ResultSet rsH = psHist.executeQuery()) {
                        while (rsH.next()) {
                            SaleReceipt receipt = new SaleReceipt(
                                    rsH.getInt("id"),
                                    rsH.getString("payment_date"),
                                    rsH.getDouble("amount"),
                                    rsH.getString("notes")
                            );
                            receipts.add(receipt);
                        }
                    }
                }
                s.setReceipts(receipts);
            }
        } catch (SQLException e) { log.error("Error al buscar detalle de venta", e); }
        return s;
    }

    @Override
    public boolean updateSalePaymentStatus(int paymentId, String status, double paidAmount) {
        String sql = "UPDATE sales_payments SET status = ?, paid_amount = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setDouble(2, paidAmount);
            ps.setInt(3, paymentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { log.error(e.getMessage()); return false; }
    }

    @Override
    public boolean processSaleCascadePayment(int saleId, double totalAmount, String notes) {
        String sqlInsertHistory = "INSERT INTO payment_history (sale_id, amount, payment_date, notes) VALUES (?, ?, datetime('now', 'localtime'), ?)";
        String sqlSelectPayments = "SELECT id, amount, paid_amount FROM sales_payments WHERE sale_id = ? AND status != 'PAID' ORDER BY installment_no ASC";
        String sqlUpdatePayment = "UPDATE sales_payments SET paid_amount = ?, status = ?, paid_at = datetime('now', 'localtime') WHERE id = ?";
        String sqlCountPending = "SELECT COUNT(*) FROM sales_payments WHERE sale_id = ? AND status != 'PAID'";
        String sqlUpdateSaleStatus = "UPDATE sales SET status = 'COMPLETED' WHERE id = ?";

        class TempCuotaSale {
            int id; double amount; double paidAmount;
            TempCuotaSale(int id, double amount, double paidAmount) { this.id = id; this.amount = amount; this.paidAmount = paidAmount; }
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlInsertHistory)) {
                ps.setInt(1, saleId);
                ps.setDouble(2, totalAmount);
                ps.setString(3, (notes == null || notes.trim().isEmpty()) ? "Entrega de cuota comercial" : notes.trim());
                ps.executeUpdate();
            }

            List<TempCuotaSale> list = new ArrayList<>();
            try (PreparedStatement psSel = conn.prepareStatement(sqlSelectPayments)) {
                psSel.setInt(1, saleId);
                try (ResultSet rs = psSel.executeQuery()) {
                    while (rs.next()) {
                        list.add(new TempCuotaSale(rs.getInt("id"), rs.getDouble("amount"), rs.getDouble("paid_amount")));
                    }
                }
            }

            double remaining = totalAmount;
            try (PreparedStatement psUp = conn.prepareStatement(sqlUpdatePayment)) {
                for (TempCuotaSale c : list) {
                    if (remaining <= 0) break;
                    double debt = c.amount - c.paidAmount;
                    if (remaining >= debt) {
                        remaining -= debt;
                        psUp.setDouble(1, c.amount);
                        psUp.setString(2, "PAID");
                        psUp.setInt(3, c.id);
                    } else {
                        psUp.setDouble(1, c.paidAmount + remaining);
                        psUp.setString(2, "PARTIAL");
                        psUp.setInt(3, c.id);
                        remaining = 0;
                    }
                    psUp.addBatch();
                }
                if (!list.isEmpty()) psUp.executeBatch();
            }

            int pending = 0;
            try (PreparedStatement psCount = conn.prepareStatement(sqlCountPending)) {
                psCount.setInt(1, saleId);
                try (ResultSet rs = psCount.executeQuery()) { if (rs.next()) pending = rs.getInt(1); }
            }
            if (pending == 0) {
                try (PreparedStatement psUpSale = conn.prepareStatement(sqlUpdateSaleStatus)) {
                    psUpSale.setInt(1, saleId); psUpSale.executeUpdate();
                }
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @Override
    public boolean revertLastSalePayment(int receiptId, int saleId, double amount) {
        String sqlDeleteReceipt = "DELETE FROM payment_history WHERE id = ?";
        String sqlUpdateSale = "UPDATE sales SET status = 'ACTIVE' WHERE id = ?";
        String sqlSelectPaymentsToRevert = "SELECT id, amount, paid_amount FROM sales_payments WHERE sale_id = ? AND paid_amount > 0 ORDER BY installment_no DESC";
        String sqlUpdatePayment = "UPDATE sales_payments SET paid_amount = ?, status = ?, paid_at = CASE WHEN ? > 0 THEN paid_at ELSE NULL END WHERE id = ?";

        class TempCuotaSaleRevert {
            int id; double amount; double paidAmount;
            TempCuotaSaleRevert(int id, double amount, double paidAmount) { this.id = id; this.amount = amount; this.paidAmount = paidAmount; }
        }

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlDeleteReceipt)) {
                ps.setInt(1, receiptId); ps.executeUpdate();
            }

            List<TempCuotaSaleRevert> list = new ArrayList<>();
            try (PreparedStatement psSel = conn.prepareStatement(sqlSelectPaymentsToRevert)) {
                psSel.setInt(1, saleId);
                try (ResultSet rs = psSel.executeQuery()) {
                    while (rs.next()) {
                        list.add(new TempCuotaSaleRevert(rs.getInt("id"), rs.getDouble("amount"), rs.getDouble("paid_amount")));
                    }
                }
            }

            double revertAmount = amount;
            try (PreparedStatement psUp = conn.prepareStatement(sqlUpdatePayment)) {
                for (TempCuotaSaleRevert c : list) {
                    if (revertAmount <= 0) break;
                    if (revertAmount >= c.paidAmount) {
                        revertAmount -= c.paidAmount;
                        psUp.setDouble(1, 0.0);
                        psUp.setString(2, "PENDING");
                        psUp.setDouble(3, 0.0);
                    } else {
                        double newPaid = c.paidAmount - revertAmount;
                        revertAmount = 0;
                        psUp.setDouble(1, newPaid);
                        psUp.setString(2, "PARTIAL");
                        psUp.setDouble(3, newPaid);
                    }
                    psUp.setInt(4, c.id);
                    psUp.addBatch();
                }
                if (!list.isEmpty()) psUp.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlUpdateSale)) {
                ps.setInt(1, saleId); ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    @Override
    public boolean cancelSaleWithOption(int saleId, int productId, boolean restoreStock) {
        String updateSale = "UPDATE sales SET status = 'CANCELED' WHERE id = ?";
        String updatePayments = "UPDATE sales_payments SET status = 'CANCELED' WHERE sale_id = ?";
        String restoreStockSql = "UPDATE products SET stock = stock + 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement ps1 = conn.prepareStatement(updateSale)) {
                ps1.setInt(1, saleId); ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(updatePayments)) {
                ps2.setInt(1, saleId); ps2.executeUpdate();
            }

            if (restoreStock) {
                try (PreparedStatement ps3 = conn.prepareStatement(restoreStockSql)) {
                    ps3.setInt(1, productId); ps3.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}