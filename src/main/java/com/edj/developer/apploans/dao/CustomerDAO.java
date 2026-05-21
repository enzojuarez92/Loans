package com.edj.developer.apploans.dao;

import com.edj.developer.apploans.model.Customer;
import java.util.List;
import java.util.Optional;

/**
 * CustomerDAO — Contrato de persistencia para Customer.
 *
 * El uso de una interfaz desacopla la implementación concreta
 * (SQLite hoy, MySQL mañana) del resto de la aplicación.
 * Los controladores solo conocen esta interfaz.
 */
public interface CustomerDAO {

    /** Persiste un nuevo cliente. Retorna el objeto con el ID asignado. */
    Customer save(Customer customer);

    /** Actualiza un cliente existente. Retorna true si se afectó ≥1 fila. */
    boolean update(Customer customer);

    /**
     * Elimina (soft-delete) un cliente por ID.
     * Cambia status → 'INACTIVE' en lugar de borrar físicamente.
     * Retorna true si se afectó ≥1 fila.
     */
    boolean delete(int id);

    /** Elimina físicamente un registro (para tests o limpieza admin). */
    boolean hardDelete(int id);

    /** Busca un cliente por su ID primario. */
    Optional<Customer> findById(int id);

    /** Busca un cliente por número de documento. */
    Optional<Customer> findByDocNumber(String docNumber);

    /** Retorna todos los clientes ordenados por apellido. */
    List<Customer> findAll();

    /** Retorna clientes filtrados por status ("ACTIVE" | "INACTIVE"). */
    List<Customer> findByStatus(String status);

    /** Cuenta el total de registros en la tabla. */
    int countAll();

    /** Cuenta registros por status. */
    int countByStatus(String status);

    /** Cuenta cuántos clientes tienen al menos un préstamo activo. */
    int countWithActiveLoans();
}