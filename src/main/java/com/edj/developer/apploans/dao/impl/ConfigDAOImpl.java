package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.IConfigDAO;
import com.edj.developer.apploans.model.LoanAmount;
import com.edj.developer.apploans.model.LoanFrequency;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigDAOImpl implements IConfigDAO {

    @Override
    public List<LoanAmount> findAllAmounts() {
        List<LoanAmount> list = new ArrayList<>();
        String sql = "SELECT * FROM config_amounts ORDER BY value ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new LoanAmount(rs.getInt("id"), rs.getDouble("value")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean saveAmount(LoanAmount amount) {
        String sql = "INSERT INTO config_amounts (value) VALUES (?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount.getValue());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false; // Probablemente valor duplicado
        }
    }

    @Override
    public boolean deleteAmount(int id) {
        String sql = "DELETE FROM config_amounts WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<LoanFrequency> findAllFrequencies() {
        List<LoanFrequency> list = new ArrayList<>();
        String sql = "SELECT * FROM config_frequencies ORDER BY days_interval ASC";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new LoanFrequency(rs.getInt("id"), rs.getString("name"), rs.getInt("days_interval")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean saveFrequency(LoanFrequency frequency) {
        String sql = "INSERT INTO config_frequencies (name, days_interval) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, frequency.getName());
            pstmt.setInt(2, frequency.getDaysInterval());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean deleteFrequency(int id) {
        String sql = "DELETE FROM config_frequencies WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}