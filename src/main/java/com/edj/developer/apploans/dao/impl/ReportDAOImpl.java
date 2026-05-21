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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<GeneralReportItem> getActiveLoansInstallments() {
        List<GeneralReportItem> list = new ArrayList<>();
        // Buscamos la cuota pendiente más próxima de cada crédito activo
        String sql = """
            SELECT (c.first_name || ' ' || c.last_name) AS customer, l.id AS loan_id, lp.amount
            FROM loans l
            JOIN customers c ON l.customer_id = c.id
            JOIN loan_payments lp ON lp.loan_id = l.id
            WHERE l.status = 'ACTIVE'
              AND lp.status != 'PAID'
            GROUP BY l.id
            ORDER BY customer ASC
            """;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new GeneralReportItem(
                        rs.getString("customer"),
                        rs.getInt("loan_id"),
                        rs.getDouble("amount")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}