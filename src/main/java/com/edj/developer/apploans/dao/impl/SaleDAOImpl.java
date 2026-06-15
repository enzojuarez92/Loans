package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.SaleDAO;
import com.edj.developer.apploans.model.Sale;
import com.edj.developer.apploans.model.SalePayment;
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

            // 1. Validar Stock del Producto
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

            // Descontar una unidad del stock
            try (PreparedStatement psUpdate = conn.prepareStatement(updateStockSql)) {
                psUpdate.setInt(1, sale.getProductId());
                psUpdate.executeUpdate();
            }

            // 2. Insertar Cabecera de la Venta
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

            // 3. Generar Plan de Cuotas
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
            log.info("Venta Financiera ID {} e historial de cuotas creados con éxito.", saleId);
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.warn("Rollback ejecutado en Ventas debido a: {}", e.getMessage());
                } catch (SQLException ex) {
                    log.error("Error crítico en Rollback", ex);
                }
            }
            throw new RuntimeException(e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { log.error(e.getMessage()); }
            }
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

        // Filtros dinámicos
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
        } catch (SQLException e) { log.error("Error listando ventas filtradas", e); }
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
        String saleSql = """
        SELECT s.*, (c.first_name || ' ' || c.last_name) AS customer_name, p.name AS product_name 
        FROM sales s
        JOIN customers c ON s.customer_id = c.id
        JOIN products p ON s.product_id = p.id
        WHERE s.id = ?
        """;

        String paymentsSql = "SELECT * FROM sales_payments WHERE sale_id = ? ORDER BY installment_no ASC";

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
                }
            }

            if (s != null) {
                List<SalePayment> pList = new ArrayList<>();
                try (PreparedStatement psPay = conn.prepareStatement(paymentsSql)) {
                    psPay.setInt(1, id);
                    try (ResultSet rsPay = psPay.executeQuery()) {
                        while (rsPay.next()) {
                            // Mapeamos de acuerdo a las columnas de tu BD:
                            // rsPay.getInt("installment_no") va hacia installmentNumber
                            // rsPay.getDouble("paid_amount") ahora sí leerá la nueva columna
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
            }
        } catch (SQLException e) {
            log.error("Error al buscar detalle completo de la venta", e);
        }
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
        } catch (SQLException e) {
            log.error("Error al actualizar el pago de la cuota", e);
            return false;
        }
    }

    @Override
    public boolean cancelSaleWithStockRestoration(int saleId, int productId) {
        String updateSale = "UPDATE sales SET status = 'CANCELED' WHERE id = ?";
        String updatePayments = "UPDATE sales_payments SET status = 'ANULADA' WHERE sale_id = ?";
        String restoreStock = "UPDATE products SET stock = stock + 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            // 1. Cancelar Cabecera de la Venta
            try (PreparedStatement ps1 = conn.prepareStatement(updateSale)) {
                ps1.setInt(1, saleId);
                ps1.executeUpdate();
            }

            // 2. Anular todas sus Cuotas
            try (PreparedStatement ps2 = conn.prepareStatement(updatePayments)) {
                ps2.setInt(1, saleId);
                ps2.executeUpdate();
            }

            // 3. Devolver +1 al Stock del Producto
            try (PreparedStatement ps3 = conn.prepareStatement(restoreStock)) {
                ps3.setInt(1, productId);
                ps3.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { log.error("Error en rollback", ex); }
            }
            log.error("Error al anular la venta comercial", e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { log.error(e.getMessage()); }
            }
        }
    }
}