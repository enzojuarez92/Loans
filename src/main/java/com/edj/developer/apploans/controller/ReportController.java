package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanPayment;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportController {
    @FXML private VBox reportContainer;
    @FXML private Label lblReportCustomer, lblReportDate, lblLoanId, lblTotalPaid, lblReportTotalPending;
    @FXML private TableView<LoanPayment> tableReport;
    @FXML private TableColumn<LoanPayment, Integer> colNum;
    @FXML private TableColumn<LoanPayment, String> colDate, colStatus;
    @FXML private TableColumn<LoanPayment, Double> colAmount, colPaid, colBalance;

    public void setData(Loan loan) {
        // 1. Cabecera
        lblReportCustomer.setText("Cliente: " + loan.getCustomerName());
        lblLoanId.setText("Préstamo #" + loan.getId());
        lblReportDate.setText("Fecha de emisión: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // 2. Configurar Columnas
        colNum.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getInstallmentNumber()).asObject());
        colDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDueDate()));
        colAmount.setCellValueFactory(cd -> new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getAmount()).asObject());
        colPaid.setCellValueFactory(cd -> new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getPaidAmount()).asObject());
        colBalance.setCellValueFactory(cd -> {
            double b = cd.getValue().getAmount() - cd.getValue().getPaidAmount();
            return new javafx.beans.property.SimpleDoubleProperty(b).asObject();
        });
        colStatus.setCellValueFactory(cd -> {
            String s = cd.getValue().getStatus();
            if("PAID".equalsIgnoreCase(s)) return new javafx.beans.property.SimpleStringProperty("PAGADO");
            if("PARTIAL".equalsIgnoreCase(s)) return new javafx.beans.property.SimpleStringProperty("PARCIAL");
            return new javafx.beans.property.SimpleStringProperty("PENDIENTE");
        });

        // 3. Cargar Datos
        tableReport.setItems(FXCollections.observableArrayList(loan.getPayments()));

        // 4. Totales
        double paid = loan.getPayments().stream().mapToDouble(LoanPayment::getPaidAmount).sum();
        double pending = loan.getTotalAmount() - paid;

        lblTotalPaid.setText(String.format("$ %.2f", paid));
        lblReportTotalPending.setText(String.format("$ %.2f", pending));
    }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(reportContainer.getScene().getWindow())) {
            // Configuramos para que el contenido se ajuste a la página
            boolean success = job.printPage(reportContainer);
            if (success) {
                job.endJob();
            }
        }
    }

    @FXML
    private void handleClose() {
        // Obtenemos la ventana actual y la cerramos
        Stage stage = (Stage) reportContainer.getScene().getWindow();
        stage.close();
    }
}