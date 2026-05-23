package com.edj.developer.apploans.config;

/**
 * DatabaseTables
 * Centraliza las sentencias DDL de creación de tablas.
 */
public final class DatabaseTables {

    private DatabaseTables() {}

    public static final String CREATE_CONFIG_TABLE = """
            CREATE TABLE IF NOT EXISTS config (
                business_name   TEXT
            )
            """;

    public static final String CREATE_USERS_TABLE = """
        CREATE TABLE IF NOT EXISTS users (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            username    TEXT    NOT NULL UNIQUE,
            password    TEXT    NOT NULL,
            full_name   TEXT,
            role        TEXT    NOT NULL DEFAULT 'OPERATOR',
            active      INTEGER NOT NULL DEFAULT 1,
            created_at  TEXT    DEFAULT CURRENT_TIMESTAMP
        )
        """;

    public static final String CREATE_CUSTOMERS_TABLE = """
        CREATE TABLE IF NOT EXISTS customers (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            doc_number    TEXT    NOT NULL UNIQUE,
            first_name    TEXT    NOT NULL,
            last_name     TEXT    NOT NULL,
            phone         TEXT,
            email         TEXT,
            address       TEXT,
            city          TEXT,
            status        TEXT    NOT NULL DEFAULT 'ACTIVE',
            birth_date    TEXT,
            notes         TEXT,
            created_at    TEXT    DEFAULT CURRENT_TIMESTAMP,
            updated_at    TEXT    DEFAULT CURRENT_TIMESTAMP
        )
        """;

    public static final String CREATE_CONFIG_AMOUNTS_TABLE = """
        CREATE TABLE IF NOT EXISTS config_amounts (
            id     INTEGER PRIMARY KEY AUTOINCREMENT,
            value  REAL    NOT NULL UNIQUE
        )
        """;

    public static final String CREATE_CONFIG_FREQUENCIES_TABLE = """
        CREATE TABLE IF NOT EXISTS config_frequencies (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            name          TEXT    NOT NULL UNIQUE,
            days_interval INTEGER NOT NULL
        )
        """;

    public static final String CREATE_LOANS_TABLE = """
        CREATE TABLE IF NOT EXISTS loans (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id   INTEGER NOT NULL,
            amount        REAL    NOT NULL,
            interest_rate REAL    NOT NULL,
            total_amount  REAL    NOT NULL,
            installments  INTEGER NOT NULL,
            frequency     TEXT    NOT NULL,
            start_date    TEXT    NOT NULL,
            status        TEXT    DEFAULT 'ACTIVE',
            FOREIGN KEY (customer_id) REFERENCES customers(id)
        )
        """;

    public static final String CREATE_LOAN_PAYMENTS_TABLE = """
        CREATE TABLE IF NOT EXISTS loan_payments (
            id                 INTEGER PRIMARY KEY AUTOINCREMENT,
            loan_id            INTEGER NOT NULL,
            installment_number INTEGER NOT NULL,
            amount             REAL    NOT NULL,
            paid_amount        REAL    DEFAULT 0.0,
            due_date           TEXT    NOT NULL,
            status             TEXT    DEFAULT 'PENDING',
            payment_date       TEXT,
            FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
        )
        """;
}