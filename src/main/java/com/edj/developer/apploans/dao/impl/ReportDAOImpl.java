package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.ReportDAO;
import com.edj.developer.apploans.model.DailyReportItem;
import com.edj.developer.apploans.model.GeneralReportItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportDAOImpl implements ReportDAO {

    @Override
    public List<DailyReportItem> getDailyDueInstallments() {
        List<DailyReportItem> list = new ArrayList<>();
        String sql = """
        SELECT lp.loan_id, (c.first_name || ' ' || c.last_name) AS customer, 
               lp.installment_number, lp.amount, lp.due_date
        FROM loan_payments lp
        JOIN loans l ON lp.loan_id = l.id
        JOIN customers c ON l.customer_id = c.id
        WHERE date(lp.due_date) = date('now', 'localtime')
          AND lp.status != 'PAID'
          AND UPPER(TRIM(lp.status)) NOT IN ('CANCELED', 'CANCELADO')
          AND UPPER(TRIM(l.status)) IN ('ACTIVE', 'ACTIVO') -- 🚀 Filtra SOLO préstamos vigentes/activos
        ORDER BY customer ASC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DailyReportItem(
                        rs.getInt("loan_id"),
                        rs.getString("customer"),
                        rs.getInt("installment_number"),
                        rs.getDouble("amount"),
                        rs.getString("due_date")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<GeneralReportItem> getActiveLoansInstallments() {
        List<GeneralReportItem> list = new ArrayList<>();
        String sql = """
        SELECT 
            (c.first_name || ' ' || c.last_name) AS customer, 
            l.id AS loan_id, 
            COALESCE(SUM(MAX(lp.amount - lp.paid_amount, 0)), 0) AS saldo_pendiente
        FROM loans l
        JOIN customers c ON l.customer_id = c.id
        JOIN loan_payments lp ON lp.loan_id = l.id
        WHERE UPPER(TRIM(l.status)) IN ('ACTIVE', 'ACTIVO')
          AND UPPER(TRIM(lp.status)) NOT IN ('PAID', 'CANCELED')
        GROUP BY l.id, c.first_name, c.last_name
        ORDER BY customer ASC
        """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new GeneralReportItem(
                        rs.getString("customer"),
                        rs.getInt("loan_id"),
                        rs.getDouble("saldo_pendiente")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // 💡 CORREGIDO: Vencimientos de Cuotas de Ventas del Día de Hoy (Excluyendo Anuladas)
    @Override
    public List<DailyReportItem> getDailyDueSalesInstallments() {
        List<DailyReportItem> list = new ArrayList<>();
        String sql = """
            SELECT sp.sale_id, (c.first_name || ' ' || c.last_name) AS customer, 
                   sp.installment_no AS installment_number, sp.amount, sp.due_date
            FROM sales_payments sp
            JOIN sales s ON sp.sale_id = s.id
            JOIN customers c ON s.customer_id = c.id
            WHERE date(sp.due_date) = date('now', 'localtime')
              AND UPPER(TRIM(sp.status)) NOT IN ('PAID', 'CANCELED', 'CANCELADO')
              AND UPPER(TRIM(s.status)) NOT IN ('CANCELED', 'CANCELADO')
            ORDER BY customer ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DailyReportItem(
                        rs.getInt("sale_id"),
                        rs.getString("customer"),
                        rs.getInt("installment_number"),
                        rs.getDouble("amount"),
                        rs.getString("due_date")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<GeneralReportItem> getActiveSalesInstallments() {
        List<GeneralReportItem> list = new ArrayList<>();
        String sql = """
            SELECT (c.first_name || ' ' || c.last_name) AS customer, 
            s.id AS sale_id, 
            COALESCE(SUM(MAX(sp.amount - sp.paid_amount, 0)), 0) AS saldo_pendiente
            FROM sales s
            JOIN customers c ON s.customer_id = c.id
            JOIN sales_payments sp ON sp.sale_id = s.id
            WHERE UPPER(TRIM(s.status)) IN ('ACTIVE', 'ACTIVO')
              AND UPPER(TRIM(sp.status)) NOT IN ('PAID', 'CANCELED')
            GROUP BY s.id
            ORDER BY customer ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new GeneralReportItem(
                        rs.getString("customer"),
                        rs.getInt("sale_id"),
                        rs.getDouble("saldo_pendiente")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
