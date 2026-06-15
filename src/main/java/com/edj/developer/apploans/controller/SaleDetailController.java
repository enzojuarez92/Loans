package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.SaleDAO;
import com.edj.developer.apploans.dao.impl.SaleDAOImpl;
import com.edj.developer.apploans.model.Sale;
import com.edj.developer.apploans.model.SalePayment; // Asegurate que represente tus cuotas de productos
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public class SaleDetailController {

    @FXML private Label lblCustomerName, lblSaleId, lblProductName, lblTotalAmount, lblPaidAmount, lblPendingAmount;
    @FXML private Button btnCancelSale;

    @FXML private TableView<SalePayment> tablePayments;
    @FXML private TableColumn<SalePayment, Integer> colNumber;
    @FXML private TableColumn<SalePayment, String> colDueDate, colStatus;
    @FXML private TableColumn<SalePayment, Double> colAmount, colPaidAmount, colBalance;
    @FXML private TableColumn<SalePayment, Void> colAction;

    private final SaleDAO saleDAO = new SaleDAOImpl();
    private Sale currentSale;
    private final DecimalFormat moneyFormatter = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));

    @FXML
    public void initialize() {
        setupColumns();
    }

    public void setSaleData(Sale sale) {
        this.currentSale = sale;
        refreshData();
    }

    private void setupColumns() {
        colNumber.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getInstallmentNumber()).asObject());
        colDueDate.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getDueDate()));

        colAmount.setCellValueFactory(cell -> new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getAmount()).asObject());
        colAmount.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e); setText(e || v == null ? null : moneyFormatter.format(v));
            }
        });

        colPaidAmount.setCellValueFactory(cell -> new javafx.beans.property.SimpleDoubleProperty(cell.getValue().getPaidAmount()).asObject());
        colPaidAmount.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e); setText(e || v == null ? null : moneyFormatter.format(v));
            }
        });

        colBalance.setCellValueFactory(cell -> {
            double balance = cell.getValue().getAmount() - cell.getValue().getPaidAmount();
            return new javafx.beans.property.SimpleDoubleProperty(balance).asObject();
        });
        colBalance.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e); setText(e || v == null ? null : moneyFormatter.format(v));
            }
        });

        // Configuración visual del Estado de la cuota en Español
        colStatus.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getStatus()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setStyle("");
                } else {
                    switch (status.toUpperCase()) {
                        case "PAID", "PAGADA" -> { setText("PAGADA"); setTextFill(Color.web("#198754")); }
                        case "PARTIAL", "PARCIAL" -> { setText("PAGO PARCIAL"); setTextFill(Color.web("#fd7e14")); }
                        case "OVERDUE", "VENCIDA" -> { setText("VENCIDA"); setTextFill(Color.web("#b30000")); }
                        default -> { setText("PENDIENTE"); setTextFill(Color.web("#0d6efd")); }
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        setupActionColumn();
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnPay = new Button();
            {
                FontIcon icon = new FontIcon("fas-check-circle");
                icon.setIconSize(14);
                icon.setIconColor(Color.valueOf("#198754"));

                btnPay.setGraphic(icon);
                btnPay.getStyleClass().addAll("btn", "btn-sm", "btn-light");
                btnPay.setTooltip(new Tooltip("Procesar cobro de cuota"));

                btnPay.setOnAction(event -> {
                    SalePayment p = getTableView().getItems().get(getIndex());
                    handlePayInstallment(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || "CANCELED".equalsIgnoreCase(currentSale.getStatus())) {
                    setGraphic(null);
                } else {
                    SalePayment p = getTableView().getItems().get(getIndex());
                    // El botón se oculta si la cuota ya está totalmente liquidada
                    setGraphic(!"PAID".equalsIgnoreCase(p.getStatus()) ? btnPay : null);
                }
            }
        });
    }

    private void handlePayInstallment(SalePayment payment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PaymentModalView.fxml"));
            Parent root = loader.load();

            PaymentModalController modal = loader.getController();
            double remainingBalance = payment.getAmount() - payment.getPaidAmount();
            modal.setInitialData(remainingBalance);

            Stage stage = new Stage();
            stage.setTitle("Registrar Cobro de Artículo");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Double amountEntered = modal.getAmountResult();
            if (amountEntered != null) {
                double newPaidAccumulated = payment.getPaidAmount() + amountEntered;
                String newStatus = (newPaidAccumulated >= payment.getAmount()) ? "PAID" : "PARTIAL";

                // Inyección al DAO
                boolean ok = saleDAO.updateSalePaymentStatus(payment.getId(), newStatus, newPaidAccumulated);
                if (ok) refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelSale() {
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Anular Operación Comercial",
                "¿Está seguro de que desea eliminar la venta #" + currentSale.getId() + "?",
                "Esta acción restaurará 1 unidad de '" + currentSale.getProductName() + "' al inventario."
        );

        if (result.isPresent() && !result.get().getButtonData().isCancelButton()) {
            boolean ok = saleDAO.cancelSaleWithStockRestoration(currentSale.getId(), currentSale.getProductId());
            if (ok) {
                AlertHelper.showInfo("Éxito", "Operación Anulada", "La venta se canceló y el stock fue devuelto.");
                handleBack();
            } else {
                AlertHelper.showError("Error", "Fallo al Procesar", "No se pudo anular la venta en el sistema.");
            }
        }
    }

    private void refreshData() {
        // Obtenemos la cabecera e historial actualizado desde la base de datos
        this.currentSale = saleDAO.findFullSaleById(currentSale.getId());

        if (currentSale != null) {
            lblCustomerName.setText(currentSale.getCustomerName());
            lblSaleId.setText("Venta #" + currentSale.getId());
            lblProductName.setText(currentSale.getProductName());

            double total = currentSale.getTotalAmount();
            double paid = currentSale.getPayments().stream().mapToDouble(SalePayment::getPaidAmount).sum();

            lblTotalAmount.setText(moneyFormatter.format(total));
            lblPaidAmount.setText(moneyFormatter.format(paid));
            lblPendingAmount.setText(moneyFormatter.format(total - paid));

            tablePayments.setItems(FXCollections.observableArrayList(currentSale.getPayments()));

            // SEGURIDAD: Solo se habilita el botón de anulación si no se cobró un solo centavo de ninguna cuota
            // y además la venta no está previamente anulada.
            boolean hasPayments = paid > 0;
            boolean isAlreadyCanceled = "CANCELED".equalsIgnoreCase(currentSale.getStatus());
            btnCancelSale.setDisable(hasPayments || isAlreadyCanceled);
        }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.scene.Scene scene = lblCustomerName.getScene();
            if (scene != null) {
                StackPane contentArea = (StackPane) scene.lookup("#contentArea");
                if (contentArea != null && contentArea.getParent() instanceof javafx.scene.layout.BorderPane borderPane) {
                    if (borderPane.getUserData() instanceof MainController mainController) {
                        try {
                            java.lang.reflect.Field field = MainController.class.getDeclaredField("currentView");
                            field.setAccessible(true);
                            field.set(mainController, null); // Engaña al MainController para forzar la recarga
                        } catch (Exception re) {
                            System.out.println("Error de reflexión: " + re.getMessage());
                        }
                    }
                }
                // Dispara el evento del botón de navegación de ventas para volver
                javafx.scene.Node navBtn = scene.lookup("#btnVentas"); // Asegurá este ID en tu barra lateral
                if (navBtn instanceof Button btn) {
                    btn.fire();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}