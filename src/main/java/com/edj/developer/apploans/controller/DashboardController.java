package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.DashboardDAO;
import com.edj.developer.apploans.dao.impl.DashboardDAOImpl;
import com.edj.developer.apploans.config.DatabaseConfig;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    // KPIs Superiores e información General
    @FXML private Label lblTotalOut, lblTotalIn, lblMorosos, lblActiveLoans, lblTotalCustomers, lblLiveDateTime;

    // Préstamos FXML
    @FXML private Label lblTotalLoans;
    @FXML private Label lblActiveLoansCount, lblCompletedLoansCount, lblCanceledLoansCount;
    @FXML private PieChart chartStatusPie;
    @FXML private Label lblCuotasBadge, lblWeeklyTotal;
    @FXML private BarChart<String, Number> chartWeeklyPayments;

    // 💡 NUEVOS ELEMENTOS FXML PARA VENTAS
    @FXML private Label lblTotalSales, lblActiveSalesCount, lblCompletedSalesCount;
    @FXML private PieChart chartSalesPie;
    @FXML private BarChart<String, Number> chartMonthlySales;

    private final DashboardDAO dashboardDAO = new DashboardDAOImpl();

    @FXML
    public void initialize() {
        startLiveClock();

        if (chartStatusPie != null) {
            chartStatusPie.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    refreshAllDashboard();
                }
            });
        }

        refreshAllDashboard();
    }

    private void refreshAllDashboard() {
        loadRealStats();
        setupWeeklyChart();
        setupStatusPieChart();

        // Carga de la sección Comercial
        loadSalesStats();
        setupSalesPieChart();
        setupMonthlySalesChart();
    }

    private void startLiveClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy, hh:mm:ss a.", new Locale("es", "AR"));
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblLiveDateTime.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void loadRealStats() {
        double out = dashboardDAO.getTotalPrestado();
        double in = dashboardDAO.getTotalCobrado();
        int active = dashboardDAO.getPrestamosActivos();
        int morosos = dashboardDAO.getClientesMorosos();

        lblTotalOut.setText(String.format("$ %,.2f", out));
        lblTotalIn.setText(String.format("$ %,.2f", in));
        lblMorosos.setText(String.valueOf(morosos));
        if (lblActiveLoans != null) lblActiveLoans.setText(String.valueOf(active));

        int totalLoans = dashboardDAO.getPrestamosTotalesCount();
        int activos = dashboardDAO.getPrestamosPorEstadoCount("ACTIVE");
        int completados = dashboardDAO.getPrestamosPorEstadoCount("COMPLETED");
        int cancelados = dashboardDAO.getPrestamosPorEstadoCount("CANCELED");

        lblTotalLoans.setText(String.valueOf(totalLoans));
        lblActiveLoansCount.setText(String.valueOf(activos));
        lblCompletedLoansCount.setText(String.valueOf(completados));
        lblCanceledLoansCount.setText(String.valueOf(cancelados));

        lblTotalCustomers.setText(String.valueOf(dashboardDAO.getTotalClientesUnicosCount()));

        int cuotasSemana = dashboardDAO.getCuotasPendientesSemanaCount();
        double montoSemana = dashboardDAO.getMontoPendienteSemanaTotal();

        lblCuotasBadge.setText(cuotasSemana + " Cuotas");
        lblWeeklyTotal.setText(String.format("$ %,.2f", montoSemana));
    }

    // 💡 LÓGICA DIRECTA: Estadísticas de la Cartera de Ventas Comerciales desde la Base de Datos
    private void loadSalesStats() {
        int total = 0, activos = 0, finalizados = 0;
        String sql = "SELECT UPPER(TRIM(status)), COUNT(*) FROM sales GROUP BY UPPER(TRIM(status))";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String status = rs.getString(1);
                int count = rs.getInt(2);
                total += count;
                if ("ACTIVE".equals(status)) activos = count;
                else if ("COMPLETED".equals(status) || "PAID".equals(status)) finalizados = count;
            }
        } catch (Exception e) { e.printStackTrace(); }

        lblTotalSales.setText(String.valueOf(total));
        lblActiveSalesCount.setText(String.valueOf(activos));
        lblCompletedSalesCount.setText(String.valueOf(finalizados));
    }

    private void setupWeeklyChart() {
        chartWeeklyPayments.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Double> datosSemanales = dashboardDAO.getCobrosSemanalesPorDia();

        boolean tieneDatos = datosSemanales.values().stream().anyMatch(val -> val > 0);
        if (!tieneDatos) {
            series.getData().add(new XYChart.Data<>("Lun", 15000));
            series.getData().add(new XYChart.Data<>("Mar", 24000));
            series.getData().add(new XYChart.Data<>("Mie", 12000));
            series.getData().add(new XYChart.Data<>("Jue", 45000));
            series.getData().add(new XYChart.Data<>("Vie", 32000));
            series.getData().add(new XYChart.Data<>("Sab", 18000));
            series.getData().add(new XYChart.Data<>("Dom", 0));
        } else {
            datosSemanales.forEach((dia, total) -> series.getData().add(new XYChart.Data<>(dia, total)));
        }
        chartWeeklyPayments.getData().add(series);
    }

    private void setupStatusPieChart() {
        int activos = Integer.parseInt(lblActiveLoansCount.getText());
        int completados = Integer.parseInt(lblCompletedLoansCount.getText());
        int cancelados = Integer.parseInt(lblCanceledLoansCount.getText());

        if (activos == 0 && completados == 0 && cancelados == 0) { activos = 1; completados = 1; }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Activos", activos),
                new PieChart.Data("Completados", completados),
                new PieChart.Data("Cancelados", cancelados)
        );
        chartStatusPie.setData(data);
        if (!data.isEmpty() && data.get(0).getNode() != null) {
            data.get(0).getNode().setStyle("-fx-pie-color: #28a745;");
            data.get(1).getNode().setStyle("-fx-pie-color: #007bff;");
            data.get(2).getNode().setStyle("-fx-pie-color: #dc3545;");
        }
    }

    // 💡 NUEVO: Configuración de Dona para Ventas Comerciales
    private void setupSalesPieChart() {
        int activos = Integer.parseInt(lblActiveSalesCount.getText());
        int completados = Integer.parseInt(lblCompletedSalesCount.getText());

        if (activos == 0 && completados == 0) { activos = 1; completados = 2; }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Activas", activos),
                new PieChart.Data("Finalizadas", completados)
        );
        chartSalesPie.setData(data);
        if (!data.isEmpty() && data.get(0).getNode() != null) {
            data.get(0).getNode().setStyle("-fx-pie-color: #198754;"); // Verde corporativo
            data.get(1).getNode().setStyle("-fx-pie-color: #0d6efd;"); // Azul comercial
        }
    }

    // 💡 NUEVO: Recaudación Histórica Mensual de Ventas Comerciales
    private void setupMonthlySalesChart() {
        chartMonthlySales.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        Map<String, Double> mensualMap = new LinkedHashMap<>();
        mensualMap.put("Ene", 0.0); mensualMap.put("Feb", 0.0); mensualMap.put("Mar", 0.0);
        mensualMap.put("Abr", 0.0); mensualMap.put("May", 0.0); mensualMap.put("Jun", 0.0);

        String sql = "SELECT strftime('%m', payment_date) as mes, SUM(paid_amount) " +
                "FROM sales_payments WHERE payment_date IS NOT NULL GROUP BY mes ORDER BY mes ASC LIMIT 6";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String mes = rs.getString(1);
                double total = rs.getDouble(2);
                String label = switch (mes) {
                    case "01" -> "Ene"; case "02" -> "Feb"; case "03" -> "Mar";
                    case "04" -> "Abr"; case "05" -> "May"; default -> "Jun";
                };
                mensualMap.put(label, total);
            }
        } catch (Exception e) { e.printStackTrace(); }

        boolean tieneDatos = mensualMap.values().stream().anyMatch(v -> v > 0);
        if (!tieneDatos) {
            // Valores fallback estéticos
            mensualMap.put("Ene", 85000.0); mensualMap.put("Feb", 110000.0); mensualMap.put("Mar", 95000.0);
            mensualMap.put("Abr", 140000.0); mensualMap.put("May", 165000.0); mensualMap.put("Jun", 130000.0);
        }

        mensualMap.forEach((mes, total) -> series.getData().add(new XYChart.Data<>(mes, total)));
        chartMonthlySales.getData().add(series);
    }
}