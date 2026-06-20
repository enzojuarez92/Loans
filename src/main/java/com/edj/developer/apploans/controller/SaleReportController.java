package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.model.Sale; // Asegúrate de apuntar a tu modelo real de Venta
import com.edj.developer.apploans.model.SaleReceipt; // La clase que represente el historial de pagos de la venta
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

public class SaleReportController {
    @FXML private VBox reportContainer;
    @FXML private Label lblReportCustomer, lblReportProduct, lblReportDate, lblSaleId, lblTotalPaid, lblReportTotalPending;

    // Etiquetas de contacto del cliente vinculadas a la venta
    @FXML private Label lblCustomerPhone, lblCustomerAddress, lblCustomerEmail;

    @FXML private TableView<SaleReceipt> tableReport;
    @FXML private TableColumn<SaleReceipt, Integer> colNum;
    @FXML private TableColumn<SaleReceipt, String> colDate;
    @FXML private TableColumn<SaleReceipt, Double> colAmount;
    @FXML private TableColumn<SaleReceipt, String> colNotes;

    public void setData(Sale sale) {
        if (sale == null) return;

        // 1. Cabecera Principal y Detalles de Venta
        lblReportCustomer.setText(sale.getCustomerName());
        lblReportProduct.setText(sale.getProductName()); // Muestra el producto de la venta financiera
        lblSaleId.setText("Venta #" + sale.getId());
        lblReportDate.setText("Fecha de emisión: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // 2. Carga de Información de Contacto del Cliente
        String phone = sale.getCustomerPhone();
        String address = sale.getCustomerAddress();
        String email = sale.getCustomerEmail();

        lblCustomerPhone.setText((phone == null || phone.trim().isEmpty()) ? "Teléfono: --" : "Teléfono: " + phone);
        lblCustomerAddress.setText((address == null || address.trim().isEmpty()) ? "Dirección: --" : "Dirección: " + address);
        lblCustomerEmail.setText((email == null || email.trim().isEmpty()) ? "Email: --" : "Email: " + email);

        // 3. Configurar Columnas (Aplicando la limpieza de hora que querías)
        colNum.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getId()).asObject());
        colDate.setCellValueFactory(cd -> {
            String fullDate = cd.getValue().getPaymentDate(); // O getCreatedAt(), según mapees tu historial
            String dateOnly = (fullDate != null && fullDate.contains(" ")) ? fullDate.split(" ")[0] : fullDate;
            return new javafx.beans.property.SimpleStringProperty(dateOnly);
        });
        colAmount.setCellValueFactory(cd -> new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getAmount()).asObject());
        colNotes.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getNotes()));

        // Formateo estético para los montos monetarios en la tabla de cobros
        colAmount.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$ %,.2f", item));
            }
        });

        // 4. Cargar Historial de Pagos Recibidos de la Venta
        if (sale.getReceipts() != null) {
            tableReport.setItems(FXCollections.observableArrayList(sale.getReceipts()));
        }

        // 5. Totales Financieros de la Venta
        double paid = sale.getReceipts() != null
                ? sale.getReceipts().stream().mapToDouble(SaleReceipt::getAmount).sum()
                : 0.0;
        double pending = sale.getTotalAmount() - paid; // Total de la venta financiera menos lo entregado

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

        if (job.showPrintDialog(reportContainer.getScene().getWindow())) {
            javafx.print.Printer printer = job.getPrinter();

            // Configuración idéntica de página A4 con márgenes finos
            javafx.print.PageLayout pageLayout = printer.createPageLayout(
                    javafx.print.Paper.A4,
                    javafx.print.PageOrientation.PORTRAIT,
                    14.17, 14.17, 14.17, 14.17
            );

            job.getJobSettings().setPageLayout(pageLayout);

            double printableWidth = pageLayout.getPrintableWidth();
            double reportWidth = reportContainer.getBoundsInLocal().getWidth();

            // Ajuste y escalado dinámico en caso de desborde horizontal
            double scale = 1.0;
            if (reportWidth > printableWidth) {
                scale = printableWidth / reportWidth;
            }

            javafx.scene.transform.Scale scaleTransform = new javafx.scene.transform.Scale(scale, scale);
            reportContainer.getTransforms().add(scaleTransform);

            boolean success = job.printPage(reportContainer);
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