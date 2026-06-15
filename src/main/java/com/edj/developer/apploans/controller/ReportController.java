package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanReceipt; // 💡 Importamos el nuevo modelo de recibos
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

    // 💡 CAMBIO: Ahora la tabla maneja objetos de tipo 'LoanReceipt'
    @FXML private TableView<LoanReceipt> tableReport;
    @FXML private TableColumn<LoanReceipt, Integer> colNum;
    @FXML private TableColumn<LoanReceipt, String> colDate;
    @FXML private TableColumn<LoanReceipt, Double> colAmount;
    @FXML private TableColumn<LoanReceipt, String> colNotes; // Nueva columna para el detalle del pago

    public void setData(Loan loan) {
        if (loan == null) return;

        // 1. Cabecera
        lblReportCustomer.setText("Cliente: " + loan.getCustomerName());
        lblLoanId.setText("Préstamo #" + loan.getId());
        lblReportDate.setText("Fecha de emisión: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // 2. Configurar Columnas para el Historial de Entregas
        colNum.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getId()).asObject());
        colDate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getPaymentDate()));
        colAmount.setCellValueFactory(cd -> new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getAmount()).asObject());
        colNotes.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getNotes()));

        // Formateo estético para los montos monetarios en la tabla
        colAmount.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$ %,.2f", item));
            }
        });

        // 3. Cargar Datos usando la lista de recibos que agregamos al Loan
        if (loan.getReceipts() != null) {
            tableReport.setItems(FXCollections.observableArrayList(loan.getReceipts()));
        }

        // 4. Totales Financieros reales del cliente
        double totalPrestamo = loan.getTotalAmount();
        double paid = loan.getReceipts().stream().mapToDouble(LoanReceipt::getAmount).sum();
        double pending = totalPrestamo - paid;

        lblTotalPaid.setText(String.format("$ %,.2f", paid));
        lblReportTotalPending.setText(String.format("$ %,.2f", pending));
    }

    @FXML
    private void handlePrint() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            System.err.println("No se pudo crear el trabajo de impresión (¿Hay impresoras instaladas?)");
            return;
        }

        // 1. Mostrar el cuadro de diálogo para que el usuario elija la impresora
        if (job.showPrintDialog(reportContainer.getScene().getWindow())) {

            // 2. Obtener la impresora seleccionada por el usuario
            javafx.print.Printer printer = job.getPrinter();

            // 3. Crear la configuración específica para A4 con orientación vertical y márgenes mínimos (0.5 cm = 14.17 puntos)
            javafx.print.PageLayout pageLayout = printer.createPageLayout(
                    javafx.print.Paper.A4,
                    javafx.print.PageOrientation.PORTRAIT,
                    14.17, 14.17, 14.17, 14.17 // Márgenes: Izquierdo, Derecho, Superior, Inferior
            );

            // 4. Aplicar el diseño de página al trabajo de impresión
            job.getJobSettings().setPageLayout(pageLayout);

            // 💡 TRUCO DE ORO: Escalar el contenedor visual para que quepa perfecto en el ancho imprimible de la hoja A4
            double printableWidth = pageLayout.getPrintableWidth();
            double reportWidth = reportContainer.getBoundsInLocal().getWidth();

            // Calculamos la proporción de escala (si el reporte es más ancho que la hoja, lo achica proporcionalmente)
            double scale = 1.0;
            if (reportWidth > printableWidth) {
                scale = printableWidth / reportWidth;
            }

            // Aplicamos la transformación de escala al VBox antes de imprimir
            javafx.scene.transform.Scale scaleTransform = new javafx.scene.transform.Scale(scale, scale);
            reportContainer.getTransforms().add(scaleTransform);

            // 5. Imprimir la página
            boolean success = job.printPage(reportContainer);

            // 💡 IMPORTANTE: Limpiamos la transformación después de imprimir para que en la pantalla de la app no se vea alterado o achicado
            reportContainer.getTransforms().remove(scaleTransform);

            if (success) {
                job.endJob();
            }
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) reportContainer.getScene().getWindow();
        stage.close();
    }
}