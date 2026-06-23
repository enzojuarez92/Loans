package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.model.Sale;
import com.edj.developer.apploans.model.SaleReceipt;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
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
        lblReportProduct.setText(sale.getProductName());
        lblSaleId.setText("Venta #" + sale.getId());
        lblReportDate.setText("Fecha de emisión: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // 2. Carga de Información de Contacto del Cliente
        String phone = sale.getCustomerPhone();
        String address = sale.getCustomerAddress();
        String email = sale.getCustomerEmail();

        lblCustomerPhone.setText((phone == null || phone.trim().isEmpty()) ? "Teléfono: --" : "Teléfono: " + phone);
        lblCustomerAddress.setText((address == null || address.trim().isEmpty()) ? "Dirección: --" : "Dirección: " + address);
        lblCustomerEmail.setText((email == null || email.trim().isEmpty()) ? "Email: --" : "Email: " + email);

        // 3. Configurar Columnas
        colNum.setCellValueFactory(cd -> new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getId()).asObject());
        colDate.setCellValueFactory(cd -> {
            String fullDate = cd.getValue().getPaymentDate();
            String dateOnly = (fullDate != null && fullDate.contains(" ")) ? fullDate.split(" ")[0] : fullDate;
            return new javafx.beans.property.SimpleStringProperty(dateOnly);
        });
        colAmount.setCellValueFactory(cd -> new javafx.beans.property.SimpleDoubleProperty(cd.getValue().getAmount()).asObject());
        colNotes.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getNotes()));

        // Formateo estético para los montos monetarios
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
        double pending = sale.getTotalAmount() - paid;

        lblTotalPaid.setText(String.format("$ %,.2f", paid));
        lblReportTotalPending.setText(String.format("$ %,.2f", pending));
    }

    @FXML
    private void handlePrint() {
        // 💡 REUTILIZACIÓN COMPLETA: Creamos la ventana de previsualización unificada
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);

        String title = "Resumen de Cuenta - " + lblSaleId.getText();
        modalStage.setTitle(title);

        // Pasamos nuestro reportContainer de FXML (que actúa como la grilla/hoja interna)
        PrintModalController printController = new PrintModalController(reportContainer, title);

        Scene scene = new Scene(printController.getRootView(), 850, 650);
        if (getClass().getResource("/css/style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        }

        modalStage.setScene(scene);
        modalStage.showAndWait();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) reportContainer.getScene().getWindow();
        stage.close();
    }
}