package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.ReportDAO;
import com.edj.developer.apploans.dao.impl.ReportDAOImpl;
import com.edj.developer.apploans.model.DailyReportItem;
import com.edj.developer.apploans.model.GeneralReportItem;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ReportHubController {

    private final ReportDAO reportDAO = new ReportDAOImpl();

    // ================= MÉTODOS DE PRÉSTAMOS =================
    @FXML
    private void generateDailyReport() {
        List<DailyReportItem> data = reportDAO.getDailyDueInstallments();
        openPrintModal("Reporte Diario (Vencimientos de Hoy)", buildDailyGrid(data, "PRÉST."));
    }

    @FXML
    private void generateGeneralReport() {
        List<GeneralReportItem> data = reportDAO.getActiveLoansInstallments();
        openPrintModal("Planilla General de Cobranza", buildGeneralGrid(data, "PRÉST."));
    }

    // ================= MÉTODOS DE VENTAS =================
    @FXML
    private void generateSaleDailyReport() {
        List<DailyReportItem> data = reportDAO.getDailyDueSalesInstallments();
        openPrintModal("Reporte Diario Ventas (Vencimientos de Hoy)", buildDailyGrid(data, "VENTA"));
    }

    @FXML
    private void generateSaleGeneralReport() {
        List<GeneralReportItem> data = reportDAO.getActiveSalesInstallments();
        openPrintModal("Planilla General de Cobranza - Ventas", buildGeneralGrid(data, "VENTA"));
    }

    // ================= MODAL DE IMPRESIÓN =================
    private void openPrintModal(String title, GridPane reportContent) {
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.setTitle(title);

        PrintModalController printController = new PrintModalController(reportContent, title);

        Scene scene = new Scene(printController.getRootView(), 850, 650);
        if (getClass().getResource("/css/style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        }

        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    // ── GRID DIARIO REUTILIZABLE ──
    private GridPane buildDailyGrid(List<DailyReportItem> data, String codeType) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(0);
        grid.setAlignment(Pos.TOP_CENTER);

        int itemsPerColumn = (int) Math.ceil((double) data.size() / 2);

        grid.add(createCustomTableHeader(codeType), 0, 0);
        grid.add(createCustomTableHeader(codeType), 1, 0);

        if (data.isEmpty()) {
            grid.add(createEmptyRowNotice(), 0, 1);
            return grid;
        }

        for (int i = 0; i < data.size(); i++) {
            DailyReportItem item = data.get(i);

            HBox row = new HBox(0);
            row.setStyle("-fx-border-color: #000000; -fx-border-width: 0 1 1 1; -fx-background-color: #ffffff;");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPrefHeight(28);
            row.setMinHeight(28);

            Label lblPrestamo = new Label("#" + item.getLoanId());
            lblPrestamo.setMinWidth(45);
            lblPrestamo.setPrefWidth(45);
            lblPrestamo.setAlignment(Pos.CENTER);
            lblPrestamo.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #212529; -fx-border-color: #000000; -fx-border-width: 0 1 0 0;");

            Label lblClient = new Label(" " + item.getCustomerName());
            lblClient.setMinWidth(155);
            lblClient.setPrefWidth(155);
            lblClient.setAlignment(Pos.CENTER_LEFT);
            lblClient.setStyle("-fx-font-size: 11px; -fx-text-fill: #212529; -fx-border-color: #000000; -fx-border-width: 0 1 0 0;");

            Label lblMontoCta = new Label(String.format("$ %,.2f ", item.getAmount()));
            lblMontoCta.setMinWidth(80);
            lblMontoCta.setPrefWidth(80);
            lblMontoCta.setAlignment(Pos.CENTER_RIGHT);
            lblMontoCta.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #212529; -fx-border-color: #000000; -fx-border-width: 0 1 0 0; -fx-padding: 0 3 0 0;");

            Label lblCobroVacio = new Label(" $                 ");
            lblCobroVacio.setMinWidth(90);
            lblCobroVacio.setPrefWidth(90);
            lblCobroVacio.setAlignment(Pos.CENTER_LEFT);
            lblCobroVacio.setStyle("-fx-font-size: 10px; -fx-text-fill: #495057;");

            row.getChildren().addAll(lblPrestamo, lblClient, lblMontoCta, lblCobroVacio);

            int targetColumn = (i < itemsPerColumn) ? 0 : 1;
            int targetRow = (i < itemsPerColumn) ? (i + 1) : (i - itemsPerColumn + 1);

            grid.add(row, targetColumn, targetRow);
        }
        return grid;
    }

    // ── GRID GENERAL REUTILIZABLE ──
    private GridPane buildGeneralGrid(List<GeneralReportItem> data, String codeType) {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(0);
        grid.setAlignment(Pos.TOP_CENTER);

        int itemsPerColumn = (int) Math.ceil((double) data.size() / 2);

        grid.add(createCustomTableHeader(codeType), 0, 0);
        grid.add(createCustomTableHeader(codeType), 1, 0);

        if (data.isEmpty()) {
            grid.add(createEmptyRowNotice(), 0, 1);
            return grid;
        }

        for (int i = 0; i < data.size(); i++) {
            GeneralReportItem item = data.get(i);

            HBox row = new HBox(0);
            row.setStyle("-fx-border-color: #000000; -fx-border-width: 0 1 1 1; -fx-background-color: #ffffff;");
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPrefHeight(28);
            row.setMinHeight(28);

            Label lblPrestamo = new Label("#" + item.getLoanId());
            lblPrestamo.setMinWidth(45);
            lblPrestamo.setPrefWidth(45);
            lblPrestamo.setAlignment(Pos.CENTER);
            lblPrestamo.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #212529; -fx-border-color: #000000; -fx-border-width: 0 1 0 0;");

            Label lblClient = new Label(" " + item.getCustomerName());
            lblClient.setMinWidth(155);
            lblClient.setPrefWidth(155);
            lblClient.setAlignment(Pos.CENTER_LEFT);
            lblClient.setStyle("-fx-font-size: 11px; -fx-text-fill: #212529; -fx-border-color: #000000; -fx-border-width: 0 1 0 0;");

            Label lblMontoCta = new Label(String.format("$ %,.2f ", item.getInstallmentAmount()));
            lblMontoCta.setMinWidth(80);
            lblMontoCta.setPrefWidth(80);
            lblMontoCta.setAlignment(Pos.CENTER_RIGHT);
            lblMontoCta.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #212529; -fx-border-color: #000000; -fx-border-width: 0 1 0 0; -fx-padding: 0 3 0 0;");

            Label lblCobroVacio = new Label(" $                ");
            lblCobroVacio.setMinWidth(90);
            lblCobroVacio.setPrefWidth(90);
            lblCobroVacio.setAlignment(Pos.CENTER_LEFT);
            lblCobroVacio.setStyle("-fx-font-size: 10px; -fx-text-fill: #495057;");

            row.getChildren().addAll(lblPrestamo, lblClient, lblMontoCta, lblCobroVacio);

            int targetColumn = (i < itemsPerColumn) ? 0 : 1;
            int targetRow = (i < itemsPerColumn) ? (i + 1) : (i - itemsPerColumn + 1);

            grid.add(row, targetColumn, targetRow);
        }
        return grid;
    }

    // ── HEADER DISEÑADO DINÁMICO (Cambia PRÉST. por VENTA según la sección del botón) ──
    private HBox createCustomTableHeader(String codeType) {
        HBox header = new HBox(0);
        header.setStyle("-fx-background-color: #007f5f; -fx-border-color: #000000; -fx-border-width: 1;");
        header.setPrefHeight(25);
        header.setMinHeight(25);
        header.setAlignment(Pos.CENTER_LEFT);

        Label hPrestamo = new Label(codeType);
        hPrestamo.setMinWidth(45);
        hPrestamo.setAlignment(Pos.CENTER);
        hPrestamo.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 9px; -fx-border-color: #ffffff; -fx-border-width: 0 1 0 0;");

        Label hCliente = new Label(" CLIENTE");
        hCliente.setMinWidth(155);
        hCliente.setAlignment(Pos.CENTER_LEFT);
        hCliente.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-border-color: #ffffff; -fx-border-width: 0 1 0 0;");

        Label hMonto = new Label("VALOR CTA");
        hMonto.setMinWidth(80);
        hMonto.setAlignment(Pos.CENTER);
        hMonto.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-border-color: #ffffff; -fx-border-width: 0 1 0 0;");

        Label hCobro = new Label("RECAUDADO");
        hCobro.setMinWidth(90);
        hCobro.setAlignment(Pos.CENTER);
        hCobro.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 9px;");

        header.getChildren().addAll(hPrestamo, hCliente, hMonto, hCobro);
        return header;
    }

    private HBox createEmptyRowNotice() {
        HBox emptyRow = new HBox();
        emptyRow.setStyle("-fx-border-color: #000000; -fx-border-width: 0 1 1 1; -fx-padding: 8;");
        emptyRow.setPrefWidth(370);
        Label lbl = new Label("No se encontraron registros activos.");
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        emptyRow.getChildren().add(lbl);
        return emptyRow;
    }
}