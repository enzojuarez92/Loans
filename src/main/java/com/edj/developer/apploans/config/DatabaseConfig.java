package com.edj.developer.apploans.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseConfig
 *
 * Responsabilidades:
 * 1. Proveer conexiones SQLite via getConnection()
 * 2. Inicializar el schema (CREATE TABLE IF NOT EXISTS)
 * 3. Sembrar el usuario admin por defecto (seed)
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    // 💡 CAMBIO CRUCIAL: Definimos la ruta dinámica en el HOME del usuario de Windows
    private static final String DB_URL;

    static {
        // Obtenemos la carpeta de usuario (ej: C:\Users\NombreUsuario)
        String userHome = System.getProperty("user.home");

        // Creamos una subcarpeta oculta para tu app (ej: C:\Users\NombreUsuario\.apploans)
        File appDir = new File(userHome, ".apploans");

        if (!appDir.exists()) {
            boolean creado = appDir.mkdirs(); // Crea la carpeta si no existe
            if (creado) {
                log.info("Carpetas de aplicación creadas en: {}", appDir.getAbsolutePath());
            }
        }

        // Archivo final: C:\Users\NombreUsuario\.apploans\apploans.db
        File dbFile = new File(appDir, "apploans.db");
        DB_URL = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    // Credenciales del admin por defecto (seed)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    private DatabaseConfig() {}

    /**
     * Retorna una nueva conexión SQLite.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Inicializa tablas y siembra datos base.
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

            // Habilitamos soporte de llaves foráneas en SQLite para esta conexión de inicialización
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Tablas Base del Sistema
            stmt.execute(DatabaseTables.CREATE_CONFIG_TABLE);
            stmt.execute(DatabaseTables.CREATE_USERS_TABLE);
            stmt.execute(DatabaseTables.CREATE_CUSTOMERS_TABLE);
            stmt.execute(DatabaseTables.CREATE_CONFIG_AMOUNTS_TABLE);
            stmt.execute(DatabaseTables.CREATE_CONFIG_FREQUENCIES_TABLE);

            // Módulo de Préstamos en Efectivo
            stmt.execute(DatabaseTables.CREATE_LOANS_TABLE);
            stmt.execute(DatabaseTables.CREATE_LOAN_PAYMENTS_TABLE);

            // Módulo de Productos y Ventas (¡Agregados acá!)
            stmt.execute(DatabaseTables.CREATE_PRODUCTS_TABLE);
            stmt.execute(DatabaseTables.CREATE_SALES_TABLE);
            stmt.execute(DatabaseTables.CREATE_SALES_PAYMENTS_TABLE);
            stmt.execute(DatabaseTables.CREATE_PAYMENTS_HISTORY_TABLE);

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

    /** Configuración por defecto de la aplicación */
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