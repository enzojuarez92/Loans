package com.edj.developer.apploans.dao;

import java.util.Map;

public interface DashboardDAO {
    double getTotalPrestado();
    double getTotalCobrado();
    int getPrestamosActivos();
    int getClientesMorosos();
    Map<String, Double> getCobrosMensuales(); // Para el gráfico polenta
}