package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.dao.DashboardDAO;
import com.edj.developer.apploans.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardDAOImpl implements DashboardDAO {

    @Override
    public double getTotalPrestado() {
        String sql = "SELECT SUM(amount) FROM loans";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public double getTotalCobrado() {
        String sql = "SELECT SUM(paid_amount) FROM loan_payments";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public int getPrestamosActivos() {
        // Contamos los préstamos que no están pagados del todo ni cancelados
        String sql = "SELECT COUNT(*) FROM loans WHERE status NOT IN ('PAID', 'CANCELLED')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int getClientesMorosos() {
        String sql = "SELECT COUNT(DISTINCT l.customer_id) " +
                "FROM loan_payments lp " +
                "JOIN loans l ON lp.loan_id = l.id " +
                "WHERE lp.status != 'PAID' AND lp.due_date < CURRENT_DATE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Map<String, Double> getCobrosMensuales() {
        // LinkedHashMap para mantener el orden de los meses
        Map<String, Double> data = new LinkedHashMap<>();
        // Query para SQLite. Si usas MySQL cambia strftime por MONTHNAME o DATE_FORMAT
        String sql = "SELECT strftime('%m', payment_date) as mes, SUM(paid_amount) " +
                "FROM loan_payments WHERE status = 'PAID' AND payment_date IS NOT NULL " +
                "GROUP BY mes ORDER BY mes ASC LIMIT 6";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String mes = rs.getString(1);
                double total = rs.getDouble(2);
                data.put(mes, total);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
}