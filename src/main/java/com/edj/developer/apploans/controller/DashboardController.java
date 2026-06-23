package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.DashboardDAO;
import com.edj.developer.apploans.dao.impl.DashboardDAOImpl;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class DashboardController {

    @FXML private Label lblTotalOut, lblTotalIn, lblMorosos, lblActiveLoans, lblTotalCustomers, lblLiveDateTime;
    @FXML private Label lblTotalLoans, lblActiveLoansCount, lblCompletedLoansCount, lblCanceledLoansCount;
    @FXML private PieChart chartStatusPie;
    @FXML private Label lblCuotasBadge, lblWeeklyTotal;
    @FXML private BarChart<String, Number> chartWeeklyPayments;

    // FXML Elements for Sales (Including Canceled)
    @FXML private Label lblTotalSales, lblActiveSalesCount, lblCompletedSalesCount, lblCanceledSalesCount;
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
        loadLoanStats();
        setupWeeklyChart();
        setupStatusPieChart();

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

    private void loadLoanStats() {
        double out = dashboardDAO.getTotalOutstandingCapital();
        double in = dashboardDAO.getTotalRecoveredCapital();
        int active = dashboardDAO.getActiveLoansCount();
        int delinquent = dashboardDAO.getDelinquentCustomersCount();

        lblTotalOut.setText(String.format("$ %,.2f", out));
        lblTotalIn.setText(String.format("$ %,.2f", in));
        lblMorosos.setText(String.valueOf(delinquent));
        if (lblActiveLoans != null) lblActiveLoans.setText(String.valueOf(active));

        int totalLoans = dashboardDAO.getTotalLoansCount();
        int activeCount = dashboardDAO.getLoansCountByStatus("ACTIVE");
        int completedCount = dashboardDAO.getLoansCountByStatus("COMPLETED");
        int canceledCount = dashboardDAO.getLoansCountByStatus("CANCELED");

        lblTotalLoans.setText(String.valueOf(totalLoans));
        lblActiveLoansCount.setText(String.valueOf(activeCount));
        lblCompletedLoansCount.setText(String.valueOf(completedCount));
        lblCanceledLoansCount.setText(String.valueOf(canceledCount));

        lblTotalCustomers.setText(String.valueOf(dashboardDAO.getTotalUniqueCustomersCount()));

        int weeklyCount = dashboardDAO.getWeeklyPendingInstallmentsCount();
        double weeklyAmount = dashboardDAO.getWeeklyPendingAmountTotal();

        lblCuotasBadge.setText(weeklyCount + " Cuotas");
        lblWeeklyTotal.setText(String.format("$ %,.2f", weeklyAmount));
    }

    private void loadSalesStats() {
        // 🚀 Delegación total al DAO en inglés
        int total = dashboardDAO.getTotalSalesCount();
        int active = dashboardDAO.getSalesCountByStatus("ACTIVE");
        int completed = dashboardDAO.getSalesCountByStatus("COMPLETED");
        int canceled = dashboardDAO.getSalesCountByStatus("CANCELED");

        lblTotalSales.setText(String.valueOf(total));
        lblActiveSalesCount.setText(String.valueOf(active));
        lblCompletedSalesCount.setText(String.valueOf(completed));
        if (lblCanceledSalesCount != null) {
            lblCanceledSalesCount.setText(String.valueOf(canceled));
        }
    }

    private void setupWeeklyChart() {
        chartWeeklyPayments.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Double> weeklyData = dashboardDAO.getWeeklyCollectionsByDay();

        boolean hasData = weeklyData.values().stream().anyMatch(val -> val > 0);
        if (!hasData) {
            weeklyData.put("Lun", 15000.0); weeklyData.put("Mar", 24000.0); weeklyData.put("Mie", 12000.0);
            weeklyData.put("Jue", 45000.0); weeklyData.put("Vie", 32000.0); weeklyData.put("Sab", 18000.0);
            weeklyData.put("Dom", 0.0);
        }

        weeklyData.forEach((day, total) -> series.getData().add(new XYChart.Data<>(day, total)));
        chartWeeklyPayments.getData().add(series);
    }

    private void setupStatusPieChart() {
        int active = Integer.parseInt(lblActiveLoansCount.getText());
        int completed = Integer.parseInt(lblCompletedLoansCount.getText());
        int canceled = Integer.parseInt(lblCanceledLoansCount.getText());

        if (active == 0 && completed == 0 && canceled == 0) { active = 1; completed = 1; }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Activos", active),
                new PieChart.Data("Completados", completed),
                new PieChart.Data("Cancelados", canceled)
        );
        chartStatusPie.setData(data);
        if (!data.isEmpty() && data.get(0).getNode() != null) {
            data.get(0).getNode().setStyle("-fx-pie-color: #28a745;");
            data.get(1).getNode().setStyle("-fx-pie-color: #007bff;");
            data.get(2).getNode().setStyle("-fx-pie-color: #dc3545;");
        }
    }

    private void setupSalesPieChart() {
        int active = Integer.parseInt(lblActiveSalesCount.getText());
        int completed = Integer.parseInt(lblCompletedSalesCount.getText());
        int canceled = lblCanceledSalesCount != null ? Integer.parseInt(lblCanceledSalesCount.getText()) : 0;

        if (active == 0 && completed == 0 && canceled == 0) { active = 1; completed = 2; }

        // 🚀 Ahora se renderiza el tercer estado en la torta de Ventas
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Activas", active),
                new PieChart.Data("Finalizadas", completed),
                new PieChart.Data("Anuladas", canceled)
        );
        chartSalesPie.setData(data);
        if (!data.isEmpty() && data.get(0).getNode() != null) {
            data.get(0).getNode().setStyle("-fx-pie-color: #198754;");
            data.get(1).getNode().setStyle("-fx-pie-color: #0d6efd;");
            if (data.size() > 2 && data.get(2).getNode() != null) {
                data.get(2).getNode().setStyle("-fx-pie-color: #dc3545;"); // Rojo para canceladas
            }
        }
    }

    private void setupMonthlySalesChart() {
        chartMonthlySales.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // 🚀 Consumimos el mapa directamente desde el DAO
        Map<String, Double> monthlyMap = dashboardDAO.getMonthlySalesRevenue();

        boolean hasData = monthlyMap.values().stream().anyMatch(v -> v > 0);
        if (!hasData) {
            monthlyMap.put("Ene", 85000.0); monthlyMap.put("Feb", 110000.0); monthlyMap.put("Mar", 95000.0);
            monthlyMap.put("Abr", 140000.0); monthlyMap.put("May", 165000.0); monthlyMap.put("Jun", 130000.0);
        }

        monthlyMap.forEach((month, total) -> series.getData().add(new XYChart.Data<>(month, total)));
        chartMonthlySales.getData().add(series);
    }
}