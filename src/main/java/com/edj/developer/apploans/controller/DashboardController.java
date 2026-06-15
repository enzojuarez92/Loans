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

    // Kpis de arriba y contadores de la Izquierda
    @FXML private Label lblTotalLoans;
    @FXML private Label lblActiveLoansCount, lblCompletedLoansCount, lblCanceledLoansCount;
    @FXML private PieChart chartStatusPie;

    // Sección Centro (Cobros de la semana)
    @FXML private Label lblCuotasBadge, lblWeeklyTotal;
    @FXML private BarChart<String, Number> chartWeeklyPayments;

    // Sección Derecha e indicadores superiores
    @FXML private Label lblTotalOut, lblTotalIn, lblMorosos, lblActiveLoans;
    @FXML private Label lblTotalCustomers;
    @FXML private Label lblLiveDateTime;

    private final DashboardDAO dashboardDAO = new DashboardDAOImpl();

    @FXML
    public void initialize() {
        startLiveClock();

        // ─── 💡 EL TRUCO DE REFRESCO AUTOMÁTICO EN CALIENTE ───
        // Escuchamos cuando el contenedor del Dashboard se acopla visualmente a la pantalla.
        // Cada vez que navegues al Dashboard desde el menú, JavaFX asocia la vista a la escena activa.
        // En ese preciso milisegundo, forzamos la recarga total de las consultas SQL.
        if (chartStatusPie != null) {
            chartStatusPie.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    loadRealStats();
                    setupWeeklyChart();
                    setupStatusPieChart();
                }
            });
        }

        // Primera carga por las dudas
        loadRealStats();
        setupWeeklyChart();
        setupStatusPieChart();
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
        // 1. Cargamos las tarjetas superiores requeridas
        double out = dashboardDAO.getTotalPrestado();
        double in = dashboardDAO.getTotalCobrado();
        int active = dashboardDAO.getPrestamosActivos();
        int morosos = dashboardDAO.getClientesMorosos();

        lblTotalOut.setText(String.format("$ %,.2f", out));
        lblTotalIn.setText(String.format("$ %,.2f", in));
        lblMorosos.setText(String.valueOf(morosos));
        if (lblActiveLoans != null) lblActiveLoans.setText(String.valueOf(active));

        // 2. Cargamos el desglose de la izquierda (Dona)
        int totalLoans = dashboardDAO.getPrestamosTotalesCount();
        int activos = dashboardDAO.getPrestamosPorEstadoCount("ACTIVE");
        int completados = dashboardDAO.getPrestamosPorEstadoCount("COMPLETED");
        int cancelados = dashboardDAO.getPrestamosPorEstadoCount("CANCELED");

        lblTotalLoans.setText(String.valueOf(totalLoans));
        lblActiveLoansCount.setText(String.valueOf(activos));
        lblCompletedLoansCount.setText(String.valueOf(completados));
        lblCanceledLoansCount.setText(String.valueOf(cancelados));

        // 3. Cargamos la sección derecha (Clientes Únicos)
        lblTotalCustomers.setText(String.valueOf(dashboardDAO.getTotalClientesUnicosCount()));

        // 4. Cargamos la cabecera de la semana del centro
        int cuotasSemana = dashboardDAO.getCuotasPendientesSemanaCount();
        double montoSemana = dashboardDAO.getMontoPendienteSemanaTotal();

        lblCuotasBadge.setText(cuotasSemana + " Cuotas");
        lblWeeklyTotal.setText(String.format("$ %,.2f", montoSemana));
    }

    private void setupWeeklyChart() {
        chartWeeklyPayments.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Cobros Efectivos de la Semana");

        Map<String, Double> datosSemanales = dashboardDAO.getCobrosSemanalesPorDia();

        // Si hay importes reales mayores a cero los dibuja, si está todo en cero rellena con valores planos bajos para mantener la estética limpia
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
            datosSemanales.forEach((dia, total) -> {
                series.getData().add(new XYChart.Data<>(dia, total));
            });
        }

        chartWeeklyPayments.getData().add(series);
    }

    private void setupStatusPieChart() {
        int activos = Integer.parseInt(lblActiveLoansCount.getText());
        int completados = Integer.parseInt(lblCompletedLoansCount.getText());
        int cancelados = Integer.parseInt(lblCanceledLoansCount.getText());

        if (activos == 0 && completados == 0 && cancelados == 0) {
            // Valores fallback estéticos si la base de datos es virgen
            activos = 1; completados = 1; cancelados = 0;
        }

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Activos", activos),
                new PieChart.Data("Completados", completados),
                new PieChart.Data("Cancelados", cancelados)
        );

        chartStatusPie.setData(pieChartData);

        if (!pieChartData.isEmpty() && pieChartData.get(0).getNode() != null) {
            pieChartData.get(0).getNode().setStyle("-fx-pie-color: #28a745;"); // Verde badge-success
            pieChartData.get(1).getNode().setStyle("-fx-pie-color: #007bff;"); // Azul badge-primary
            pieChartData.get(2).getNode().setStyle("-fx-pie-color: #dc3545;"); // Rojo badge-danger
        }
    }
}