package com.edj.developer.apploans.dao;

import java.util.Map;

public interface DashboardDAO {
    double getTotalPrestado();
    double getTotalCobrado();
    int getPrestamosActivos();
    int getClientesMorosos();

    // NÚMEROS REALES PARA LA DONA Y CONTADORES
    int getPrestamosTotalesCount();
    int getPrestamosPorEstadoCount(String status);
    int getTotalClientesUnicosCount();

    // DATOS REALES PARA EL GRÁFICO DE BARRAS SEMANAL
    int getCuotasPendientesSemanaCount();
    double getMontoPendienteSemanaTotal();
    Map<String, Double> getCobrosSemanalesPorDia(); // Agrupado de Lunes a Domingo

    Map<String, Double> getCobrosMensuales();
}