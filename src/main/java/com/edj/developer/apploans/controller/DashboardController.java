package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.DashboardDAO;
import com.edj.developer.apploans.dao.impl.DashboardDAOImpl;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label; // IMPORTANTE: Que sea el de JavaFX

import java.util.Map;

public class DashboardController {

    @FXML private Label lblTotalOut, lblTotalIn, lblMorosos, lblActiveLoans;
    @FXML private AreaChart<String, Number> chartPerformance;

    private final DashboardDAO dashboardDAO = new DashboardDAOImpl();

    @FXML
    public void initialize() {
        loadStats();
        setupChart();
    }

    private void loadStats() {
        double out = dashboardDAO.getTotalPrestado();
        double in = dashboardDAO.getTotalCobrado();
        int active = dashboardDAO.getPrestamosActivos();
        int morosos = dashboardDAO.getClientesMorosos();

        lblTotalOut.setText(String.format("$ %.2f", out));
        lblTotalIn.setText(String.format("$ %.2f", in));
        lblMorosos.setText(String.valueOf(morosos));
        // Si no tienes el label lblActiveLoans en el FXML todavía, esta línea fallará.
        if (lblActiveLoans != null) lblActiveLoans.setText(String.valueOf(active));
    }

    private void setupChart() {
        chartPerformance.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Recaudación Mensual 2026");

        Map<String, Double> datosReales = dashboardDAO.getCobrosMensuales();

        if (datosReales.isEmpty()) {
            // Datos de prueba solo si la DB está vacía para que no se vea feo
            series.getData().add(new XYChart.Data<>("Sin Datos", 0));
        } else {
            datosReales.forEach((mes, total) -> {
                series.getData().add(new XYChart.Data<>(mes, total));
            });
        }

        chartPerformance.getData().add(series);
    }
}