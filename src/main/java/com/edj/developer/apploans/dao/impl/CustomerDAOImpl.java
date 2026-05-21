package com.edj.developer.apploans.dao.impl;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.CustomerDAO;
import com.edj.developer.apploans.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CustomerDAOImpl — Implementación SQLite para la gestión de clientes.
 *
 * Principios aplicados:
 * - PreparedStatement (previene Inyección SQL)
 * - try-with-resources (cierre automático de conexión)
 * - Borrado lógico (status='INACTIVE')
 * - mapRow() centralizado para mapeo RS → Objeto
 * - Registro de eventos con SLF4J
 */
public class CustomerDAOImpl implements CustomerDAO {

    private static final Logger log = LoggerFactory.getLogger(CustomerDAOImpl.class);

    // ─── Constantes SQL ──────────────────────────────────────────────────

    private static final String SQL_INSERT = """
        INSERT INTO customers
            (doc_number, first_name, last_name, phone, email, address,
             city, status, birth_date, notes, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """;

    private static final String SQL_UPDATE = """
        UPDATE customers
        SET doc_number=?, first_name=?, last_name=?, phone=?, email=?,
            address=?, city=?, status=?, birth_date=?, notes=?,
            updated_at=CURRENT_TIMESTAMP
        WHERE id=?
        """;

    private static final String SQL_SOFT_DELETE = """
        UPDATE customers
        SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP
        WHERE id=?
        """;

    private static final String SQL_HARD_DELETE =
            "DELETE FROM customers WHERE id=?";

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM customers WHERE id=?";

    private static final String SQL_FIND_BY_DOC =
            "SELECT * FROM customers WHERE doc_number=?";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM customers ORDER BY last_name, first_name";

    private static final String SQL_FIND_BY_STATUS =
            "SELECT * FROM customers WHERE status=? ORDER BY last_name, first_name";

    private static final String SQL_COUNT_ALL =
            "SELECT COUNT(*) FROM customers";

    private static final String SQL_COUNT_BY_STATUS =
            "SELECT COUNT(*) FROM customers WHERE status=?";

    private static final String SQL_COUNT_WITH_LOANS =
            "SELECT COUNT(DISTINCT customer_id) FROM loans WHERE status='ACTIVE'";

    // ─── Operaciones CRUD ────────────────────────────────────────────────

    @Override
    public Customer save(Customer customer) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            bindParams(ps, customer, false);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setId(keys.getInt(1));
                    log.info("Cliente guardado con ID={}", customer.getId());
                }
            }

        } catch (SQLException e) {
            log.error("Error al guardar cliente: {}", customer, e);
            throw new RuntimeException("No se pudo guardar el cliente en la base de datos.", e);
        }
        return customer;
    }

    @Override
    public boolean update(Customer customer) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            bindParams(ps, customer, true);
            int rows = ps.executeUpdate();
            log.info("Cliente ID={} actualizado. Filas afectadas={}", customer.getId(), rows);
            return rows > 0;

        } catch (SQLException e) {
            log.error("Error al actualizar cliente ID={}", customer.getId(), e);
            throw new RuntimeException("No se pudo actualizar la información del cliente.", e);
        }
    }

    @Override
    public boolean delete(int id) {
        // Borrado lógico para no romper historial de préstamos
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SOFT_DELETE)) {

            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            log.info("Cliente ID={} desactivado (borrado lógico). Filas={}", id, rows);
            return rows > 0;

        } catch (SQLException e) {
            log.error("Error al desactivar cliente ID={}", id, e);
            throw new RuntimeException("No se pudo eliminar (desactivar) al cliente.", e);
        }
    }

    @Override
    public boolean hardDelete(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_HARD_DELETE)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("Error al eliminar físicamente el cliente ID={}", id, e);
            throw new RuntimeException("Error crítico al eliminar el registro del cliente.", e);
        }
    }

    // ─── Consultas ───────────────────────────────────────────────────────

    @Override
    public Optional<Customer> findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar cliente por ID={}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByDocNumber(String docNumber) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DOC)) {

            ps.setString(1, docNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al buscar cliente por documento={}", docNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapRow(rs));

        } catch (SQLException e) {
            log.error("Error al listar todos los clientes", e);
        }
        return list;
    }

    @Override
    public List<Customer> findByStatus(String status) {
        List<Customer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STATUS)) {

            ps.setString(1, status.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("Error al listar clientes por estado={}", status, e);
        }
        return list;
    }

    @Override
    public int countAll() { return runCount(SQL_COUNT_ALL, null); }

    @Override
    public int countByStatus(String status) { return runCount(SQL_COUNT_BY_STATUS, status); }

    @Override
    public int countWithActiveLoans() { return runCount(SQL_COUNT_WITH_LOANS, null); }

    // ─── Ayudantes Privados ──────────────────────────────────────────────

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setDocNumber(rs.getString("doc_number"));
        c.setFirstName(rs.getString("first_name"));
        c.setLastName(rs.getString("last_name"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setAddress(rs.getString("address"));
        c.setCity(rs.getString("city"));
        c.setStatus(rs.getString("status"));
        c.setNotes(rs.getString("notes"));

        String bd = rs.getString("birth_date");
        if (bd != null && !bd.isBlank()) c.setBirthDate(LocalDate.parse(bd));

        String ca = rs.getString("created_at");
        if (ca != null && !ca.isBlank())
            c.setCreatedAt(LocalDateTime.parse(ca.replace(" ", "T")));

        return c;
    }

    private void bindParams(PreparedStatement ps, Customer c, boolean forUpdate) throws SQLException {
        ps.setString(1, c.getDocNumber());
        ps.setString(2, c.getFirstName());
        ps.setString(3, c.getLastName());
        ps.setString(4, c.getPhone());
        ps.setString(5, c.getEmail());
        ps.setString(6, c.getAddress());
        ps.setString(7, c.getCity());
        ps.setString(8, c.getStatus());
        ps.setString(9, c.getBirthDate() != null ? c.getBirthDate().toString() : null);
        ps.setString(10, c.getNotes());
        if (forUpdate) ps.setInt(11, c.getId());
    }

    private int runCount(String sql, String param) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (param != null) ps.setString(1, param.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error al ejecutar consulta de conteo: {}", sql, e);
        }
        return 0;
    }
}