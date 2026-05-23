package com.edj.developer.apploans.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConfig
 *
 * Responsabilidades:
 *  1. Proveer conexiones SQLite via getConnection()
 *  2. Inicializar el schema (CREATE TABLE IF NOT EXISTS)
 *  3. Sembrar el usuario admin por defecto (seed)
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    /** Ruta del archivo SQLite. Se crea automáticamente en el directorio de trabajo. */
    private static final String DB_URL = "jdbc:sqlite:apploans.db";

    // Credenciales del admin por defecto (seed)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123"; // En prod: usar hash BCrypt

    private DatabaseConfig() {}

    /**
     * Retorna una nueva conexión SQLite.
     * El caller es responsable de cerrarla (usar try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Punto de entrada principal: inicializa tablas y siembra datos base.
     * Llamar una sola vez al arrancar la aplicación desde Main.
     */
    public static void initDatabase() {
        log.info("Initializing database...");
        createTables();
        seedAdminUser();
        seedDefaultConfig();
        log.info("Database ready at: {}", DB_URL);
    }

    /** Ejecuta los DDL de creación de tablas definidos en DatabaseTables */
    private static void createTables() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(DatabaseTables.CREATE_CONFIG_TABLE);
            stmt.execute(DatabaseTables.CREATE_USERS_TABLE);
            stmt.execute(DatabaseTables.CREATE_CUSTOMERS_TABLE);
            stmt.execute(DatabaseTables.CREATE_CONFIG_AMOUNTS_TABLE);
            stmt.execute(DatabaseTables.CREATE_CONFIG_FREQUENCIES_TABLE);
            stmt.execute(DatabaseTables.CREATE_LOANS_TABLE);
            stmt.execute(DatabaseTables.CREATE_LOAN_PAYMENTS_TABLE);

            log.info("Tables verified/created successfully.");
        } catch (SQLException e) {
            log.error("Error creating tables", e);
            throw new RuntimeException("Could not initialize database schema.", e);
        }
    }

    /** Inserta el usuario admin si no existe (INSERT OR IGNORE) */
    private static void seedAdminUser() {
        String sql = """
            INSERT OR IGNORE INTO users (username, password, full_name, role, active)
            VALUES (?, ?, 'System Administrator', 'ADMIN', 1)
            """;

        try (Connection conn = getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, ADMIN_USERNAME);
            ps.setString(2, ADMIN_PASSWORD);
            int rows = ps.executeUpdate();

            if (rows > 0) log.info("Admin user seeded successfully.");
            else          log.info("Admin user already exists, skipping seed.");

        } catch (SQLException e) {
            log.error("Error seeding admin user", e);
        }
    }

    private static void seedDefaultConfig() {
        String checkSql = "SELECT COUNT(*) FROM config";
        String insertSql = "INSERT INTO config (business_name) VALUES ('AppLoans')";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(insertSql);
                log.info("Default system configuration seeded successfully.");
            }
        } catch (SQLException e) {
            log.error("Error seeding default config", e);
        }
    }
}