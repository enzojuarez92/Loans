package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.dao.DashboardDAO;
import com.edj.developer.apploans.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardDAOImpl implements DashboardDAO {

    @Override
    public double getTotalOutstandingCapital() {
        String sql = "SELECT SUM(amount) FROM loans WHERE UPPER(TRIM(status)) != 'CANCELED'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    @Override
    public double getTotalRecoveredCapital() {
        String sql = "SELECT SUM(paid_amount) FROM loan_payments";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    @Override
    public int getActiveLoansCount() {
        String sql = "SELECT COUNT(*) FROM loans WHERE UPPER(TRIM(status)) = 'ACTIVE'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getDelinquentCustomersCount() {
        String sql = "SELECT COUNT(DISTINCT l.customer_id) " +
                "FROM loan_payments lp " +
                "JOIN loans l ON lp.loan_id = l.id " +
                "WHERE lp.status != 'PAID' AND date(lp.due_date) < date('now','localtime') AND UPPER(TRIM(l.status)) = 'ACTIVE'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getTotalLoansCount() {
        String sql = "SELECT COUNT(*) FROM loans";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getLoansCountByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM loans WHERE UPPER(TRIM(status)) = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getTotalUniqueCustomersCount() {
        String sql = "SELECT COUNT(DISTINCT customer_id) FROM loans";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getWeeklyPendingInstallmentsCount() {
        String sql = "SELECT COUNT(*) FROM loan_payments WHERE status != 'PAID' AND date(due_date) BETWEEN date('now','localtime') AND date('now', '+7 days')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public double getWeeklyPendingAmountTotal() {
        String sql = "SELECT SUM(amount - paid_amount) FROM loan_payments WHERE status != 'PAID' AND date(due_date) BETWEEN date('now','localtime') AND date('now', '+7 days')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }

    @Override
    public Map<String, Double> getWeeklyCollectionsByDay() {
        Map<String, Double> data = new LinkedHashMap<>();
        data.put("Lun", 0.0); data.put("Mar", 0.0); data.put("Mie", 0.0);
        data.put("Jue", 0.0); data.put("Vie", 0.0); data.put("Sab", 0.0); data.put("Dom", 0.0);

        String sql = "SELECT strftime('%w', payment_date) as dia_num, SUM(paid_amount) " +
                "FROM loan_payments " +
                "WHERE payment_date IS NOT NULL AND date(payment_date) >= date('now', '-7 days') " +
                "GROUP BY dia_num";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String diaNum = rs.getString(1);
                double total = rs.getDouble(2);
                String diaLabel = switch (diaNum) {
                    case "1" -> "Lun"; case "2" -> "Mar"; case "3" -> "Mie";
                    case "4" -> "Jue"; case "5" -> "Vie"; case "6" -> "Sab";
                    default -> "Dom";
                };
                data.put(diaLabel, total);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    @Override
    public Map<String, Double> getMonthlyLoanCollections() {
        Map<String, Double> data = new LinkedHashMap<>();
        String sql = "SELECT strftime('%m', payment_date) as mes, SUM(paid_amount) " +
                "FROM loan_payments WHERE payment_date IS NOT NULL " +
                "GROUP BY mes ORDER BY mes ASC LIMIT 6";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String mes = rs.getString(1);
                double total = rs.getDouble(2);
                String mesNombre = switch (mes) {
                    case "01" -> "Ene"; case "02" -> "Feb"; case "03" -> "Mar";
                    case "04" -> "Abr"; case "05" -> "May"; case "06" -> "Jun";
                    case "07" -> "Jul"; case "08" -> "Ago"; case "09" -> "Sep";
                    case "10" -> "Oct"; case "11" -> "Nov"; default -> "Dic";
                };
                data.put(mesNombre, total);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    @Override
    public int getTotalSalesCount() {
        String sql = "SELECT COUNT(*) FROM sales";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public int getSalesCountByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM sales WHERE UPPER(TRIM(status)) = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public Map<String, Double> getMonthlySalesRevenue() {
        Map<String, Double> data = new LinkedHashMap<>();
        data.put("Ene", 0.0); data.put("Feb", 0.0); data.put("Mar", 0.0);
        data.put("Abr", 0.0); data.put("May", 0.0); data.put("Jun", 0.0);

        String sql = "SELECT strftime('%m', payment_date) as mes, SUM(paid_amount) " +
                "FROM sales_payments WHERE payment_date IS NOT NULL GROUP BY mes ORDER BY mes ASC LIMIT 6";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String mes = rs.getString(1);
                double total = rs.getDouble(2);
                String label = switch (mes) {
                    case "01" -> "Ene"; case "02" -> "Feb"; case "03" -> "Mar";
                    case "04" -> "Abr"; case "05" -> "May"; default -> "Jun";
                };
                data.put(label, total);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }
}