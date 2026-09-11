package com.edj.developer.apploans.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.edj.developer.apploans.util.PasswordHasher;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

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
        migrateExistingDatabaseSafely();
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
            stmt.execute(DatabaseTables.CREATE_PAYMENT_ALLOCATIONS_TABLE);

            log.info("Tables verified/created successfully.");
        } catch (SQLException e) {
            log.error("Error creating tables", e);
            throw new RuntimeException("Could not initialize database schema.", e);
        }
    }

    /**
     * Migraciones compatibles con instalaciones ya productivas.
     *
     * No se modifican importes, cuotas, estados ni recibos existentes. Si una
     * base anterior usaba sales_payments.payment_date, se agrega paid_at y se
     * copia únicamente esa fecha para que los reportes puedan leerla.
     */
    private static void migrateExistingDatabaseSafely() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            boolean hasPaidAt = hasColumn(conn, "sales_payments", "paid_at");
            boolean hasPaymentDate = hasColumn(conn, "sales_payments", "payment_date");

            if (!hasPaidAt) {
                stmt.execute("ALTER TABLE sales_payments ADD COLUMN paid_at TEXT");
                log.info("Migración: agregada columna sales_payments.paid_at.");
            }
            if (hasPaymentDate) {
                stmt.executeUpdate("""
                    UPDATE sales_payments
                    SET paid_at = payment_date
                    WHERE paid_at IS NULL AND payment_date IS NOT NULL
                    """);
                log.info("Migración: se conservaron fechas históricas de pagos de ventas.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo verificar la compatibilidad de la base de datos.", e);
        }
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
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
            ps.setString(2, PasswordHasher.hash(ADMIN_PASSWORD));
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
