package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.ProductDAO;
import com.edj.developer.apploans.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAOImpl implements ProductDAO {
    private static final Logger log = LoggerFactory.getLogger(ProductDAOImpl.class);

    @Override
    public boolean insert(Product p) {
        String sql = "INSERT INTO products (name, description, stock, base_price) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setInt(3, p.getStock());
            ps.setDouble(4, p.getBasePrice());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al insertar producto", e);
            return false;
        }
    }

    @Override
    public boolean update(Product p) {
        String sql = "UPDATE products SET name = ?, description = ?, stock = ?, base_price = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setInt(3, p.getStock());
            ps.setDouble(4, p.getBasePrice());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al actualizar producto", e);
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al borrar producto", e);
            return false;
        }
    }

    @Override
    public Product findById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Error al buscar producto por ID", e);
        }
        return null;
    }

    @Override
    public List<Product> findAllPaged(String search, String stockFilter, int limit, int offset) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");

        buildFilters(sql, search, stockFilter);
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = bindFilters(ps, search, stockFilter);
            ps.setInt(paramIndex++, limit);
            ps.setInt(paramIndex, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Error en findAllPaged de productos", e);
        }
        return list;
    }

    @Override
    public int countProducts(String search, String stockFilter) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products WHERE 1=1");
        buildFilters(sql, search, stockFilter);

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindFilters(ps, search, stockFilter);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error al contar productos", e);
        }
        return 0;
    }

    // --- MÉTODOS DE ESTADÍSTICAS RÁPIDAS ---
    @Override public int getTotalCount() { return getSingleCount("SELECT COUNT(*) FROM products"); }
    @Override public int getLowStockCount() { return getSingleCount("SELECT COUNT(*) FROM products WHERE stock > 0 AND stock < 5"); }
    @Override public int getNoStockCount() { return getSingleCount("SELECT COUNT(*) FROM products WHERE stock = 0"); }

    private int getSingleCount(String sql) {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { log.error("Error en conteo estadístico", e); }
        return 0;
    }

    // --- HELPERS AUXILIARES ---
    private void buildFilters(StringBuilder sql, String search, String stockFilter) {
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (name LIKE ? OR description LIKE ?)");
        }
        if (stockFilter != null) {
            switch (stockFilter) {
                case "CON STOCK" -> sql.append(" AND stock > 0");
                case "STOCK BAJO" -> sql.append(" AND stock > 0 AND stock < 5");
                case "SIN STOCK" -> sql.append(" AND stock = 0");
            }
        }
    }

    private int bindFilters(PreparedStatement ps, String search, String stockFilter) throws SQLException {
        int idx = 1;
        if (search != null && !search.trim().isEmpty()) {
            String value = "%" + search.trim() + "%";
            ps.setString(idx++, value);
            ps.setString(idx++, value);
        }
        return idx;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("stock"),
                rs.getDouble("base_price"),
                rs.getString("created_at")
        );
    }
}