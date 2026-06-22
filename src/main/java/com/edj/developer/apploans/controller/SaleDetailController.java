package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.SaleDAO;
import com.edj.developer.apploans.dao.impl.SaleDAOImpl;
import com.edj.developer.apploans.model.Sale;
import com.edj.developer.apploans.model.SalePayment;
import com.edj.developer.apploans.model.SaleReceipt;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
    @FXML private Button btnCancelSale, btnRegisterPayment;

    // Tabla 1: Plan de cuotas
    @FXML private TableView<SalePayment> tablePayments;
    @FXML private TableColumn<SalePayment, Integer> colNumber;
    @FXML private TableColumn<SalePayment, String> colDueDate, colStatus;
    @FXML private TableColumn<SalePayment, Double> colAmount, colPaidAmount, colBalance;

    // Tabla 2: Historial de Entregas reales
    @FXML private TableView<SaleReceipt> tableHistory;
    @FXML private TableColumn<SaleReceipt, Integer> colHistId;
    @FXML private TableColumn<SaleReceipt, String> colHistDate, colHistNotes;
    @FXML private TableColumn<SaleReceipt, Double> colHistAmount;
    @FXML private TableColumn<SaleReceipt, Void> colHistActions;

    private final SaleDAO saleDAO = new SaleDAOImpl();
    private Sale currentSale;
    private final DecimalFormat moneyFormatter = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));

    @FXML
    public void initialize() {
        setupColumns();
        setupHistoryColumns();
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
                        case "CANCELED", "ANULADA" -> { setText("ANULADA"); setTextFill(Color.web("#dc3545")); }
                        default -> { setText("PENDIENTE"); setTextFill(Color.web("#0d6efd")); }
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupHistoryColumns() {
        colHistId.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        colHistDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("paymentDate"));
        colHistNotes.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("notes"));

        colHistAmount.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
        colHistAmount.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double v, boolean e) {
                super.updateItem(v, e); setText(e || v == null ? null : moneyFormatter.format(v));
            }
        });

        colHistActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();
            {
                FontIcon icon = new FontIcon("fas-trash-alt");
                icon.setIconSize(13); icon.setIconColor(Color.valueOf("#dc3545"));
                btnDelete.setGraphic(icon);
                btnDelete.getStyleClass().addAll("btn", "btn-sm", "btn-light");
                btnDelete.setTooltip(new Tooltip("Eliminar este cobro y devolver saldos"));

                btnDelete.setOnAction(event -> {
                    SaleReceipt receipt = getTableView().getItems().get(getIndex());
                    handleDeleteReceipt(receipt);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || "CANCELED".equalsIgnoreCase(currentSale.getStatus())) {
                    setGraphic(null);
                } else {
                    javafx.collections.ObservableList<SaleReceipt> historyList = getTableView().getItems();
                    if (historyList != null && !historyList.isEmpty()) {
                        SaleReceipt currentReceipt = historyList.get(getIndex());
                        SaleReceipt lastReceiptMade = historyList.get(0);

                        if (currentReceipt.getId() == lastReceiptMade.getId()) {
                            setGraphic(btnDelete);
                        } else {
                            setGraphic(null);
                        }
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    @FXML
    private void handleOpenPaymentModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PaymentModalView.fxml"));
            Parent root = loader.load();

            PaymentModalController modal = loader.getController();

            double total = currentSale.getTotalAmount();
            double paid = currentSale.getPayments().stream().mapToDouble(SalePayment::getPaidAmount).sum();
            double totalPendingBalance = total - paid;

            if (totalPendingBalance <= 0) {
                AlertHelper.showInfo("Venta Saldada", "Sin saldos pendientes", "Esta operación comercial ya está totalmente liquidada.");
                return;
            }

            modal.setInitialData(totalPendingBalance);

            Stage stage = new Stage();
            stage.setTitle("Registrar Entrega de Efectivo - Artículo");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Double amountEntered = modal.getAmountResult();
            if (amountEntered != null && amountEntered > 0) {
                boolean ok = saleDAO.processSaleCascadePayment(currentSale.getId(), amountEntered, "Entrega de efectivo general");
                if (ok) {
                    refreshData();
                    AlertHelper.showInfo("Éxito", "Cobro registrado", "La entrega se distribuyó correctamente en las cuotas del artículo.");
                } else {
                    AlertHelper.showError("Error", "Error operativo", "No se pudo impactar la cascada de saldos.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 💡 NUEVO Y ASOCIADO AL CARD-HEADER
    @FXML
    private void handleGenerateReport() {
        try {
            // 1. Cargamos el FXML correcto de reportes de venta
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SaleReportView.fxml"));
            Parent root = loader.load();

            // 2. 💡 CORREGIDO: Usamos el controlador real asignado a esta vista
            com.edj.developer.apploans.controller.SaleReportController controller = loader.getController();
            controller.setData(currentSale); // Inyectamos la venta actual

            // 3. Abrimos el modal
            Stage stage = new Stage();
            stage.setTitle("Resumen de Cuenta de Venta - " + currentSale.getCustomerName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Error de Carga", "No se pudo abrir el resumen.");
        }
    }

    private void handleDeleteReceipt(SaleReceipt receipt) {
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Anular Cobro Realizado",
                "¿Está seguro de eliminar el recibo N° " + receipt.getId() + " por " + moneyFormatter.format(receipt.getAmount()) + "?",
                "El sistema aplicará una reversión en cascada restando el dinero de las cuotas afectadas automáticamente."
        );

        if (result.isPresent() && !result.get().getButtonData().isCancelButton()) {
            boolean ok = saleDAO.revertLastSalePayment(receipt.getId(), currentSale.getId(), receipt.getAmount());
            if (ok) {
                AlertHelper.showInfo("Éxito", "Cobro eliminado", "Las cuotas volvieron a sus saldos anteriores.");
                refreshData();
            } else {
                AlertHelper.showError("Error", "Fallo operacional", "No se pudo revertir el pago.");
            }
        }
    }

    @FXML
    private void handleCancelSale() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Anular Operación Comercial");
        alert.setHeaderText("¿Está seguro de que desea eliminar la venta #" + currentSale.getId() + "?");

        Label lblMsg = new Label("Se cancelarán de forma definitiva todas las cuotas de la venta del producto:\n👉 '" + currentSale.getProductName() + "'");
        lblMsg.setStyle("-fx-font-weight: bold;");

        CheckBox chkRestock = new CheckBox("Reingresar 1 unidad del producto al stock actual");
        chkRestock.setSelected(true);
        chkRestock.setStyle("-fx-text-fill: #0d6efd; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        VBox content = new VBox(10, lblMsg, chkRestock);
        alert.getDialogPane().setContent(content);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean restoreStock = chkRestock.isSelected();
            boolean ok = saleDAO.cancelSaleWithOption(currentSale.getId(), currentSale.getProductId(), restoreStock);

            if (ok) {
                String stockMsg = restoreStock ? "El stock fue devuelto al inventario." : "El stock NO fue modificado.";
                AlertHelper.showInfo("Éxito", "Operación Anulada", "La venta se canceló correctamente. " + stockMsg);
                handleBack();
            } else {
                AlertHelper.showError("Error", "Fallo al Procesar", "No se pudo anular la venta en la base de datos.");
            }
        }
    }

    private void refreshData() {
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
            tableHistory.setItems(FXCollections.observableArrayList(currentSale.getReceipts()));

            boolean isAlreadyCanceled = "CANCELED".equalsIgnoreCase(currentSale.getStatus());
            btnCancelSale.setDisable(isAlreadyCanceled);
            btnRegisterPayment.setDisable((total - paid) <= 0 || isAlreadyCanceled);
        }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.scene.Scene scene = lblCustomerName.getScene();
            if (scene != null) {
                StackPane contentArea = (StackPane) scene.lookup("#contentArea");

                // 1. Limpiamos la caché de la vista actual en el MainController
                if (contentArea != null && contentArea.getParent() instanceof javafx.scene.layout.BorderPane borderPane) {
                    if (borderPane.getUserData() instanceof MainController mainController) {
                        try {
                            java.lang.reflect.Field field = MainController.class.getDeclaredField("currentView");
                            field.setAccessible(true);
                            field.set(mainController, null);
                        } catch (Exception re) {
                            System.out.println("Error reflexión: " + re.getMessage());
                        }
                    }
                }

                // 2. Simulamos el click en el botón del menú lateral de Ventas
                javafx.scene.Node navBtn = scene.lookup("#btnVentas");
                if (navBtn instanceof Button btn) {
                    btn.fire(); // Esto carga la lista general de ventas
                }

                // 3. 🚀 ASINCRONÍA VISUAL BLINDADA: Buscamos el botón de refrescar por fx:id o texto
                javafx.application.Platform.runLater(() -> {
                    try {
                        if (contentArea != null) {
                            // Intentamos buscarlo primero directamente por su fx:id
                            javafx.scene.Node refreshNode = contentArea.lookup("#btnRefresh");

                            if (refreshNode instanceof Button btnRefresh) {
                                btnRefresh.fire();
                                System.out.println("🚀 ¡Refresco automático ejecutado por fx:id!");
                            } else {
                                // Si por alguna razón el lookup por id no lo toma, barremos los botones por texto
                                var todosLosBotones = contentArea.lookupAll(".btn");
                                boolean encontrado = false;

                                for (javafx.scene.Node nodo : todosLosBotones) {
                                    if (nodo instanceof Button b) {
                                        String texto = b.getText() != null ? b.getText().toLowerCase() : "";
                                        // Si el botón dice "actualizar", "refrescar" o tiene el id correcto
                                        if (texto.contains("actualizar") || texto.contains("refrescar") || "btnRefresh".equals(b.getId())) {
                                            b.fire();
                                            System.out.println("🚀 ¡Refresco automático ejecutado por texto del botón!");
                                            encontrado = true;
                                            break;
                                        }
                                    }
                                }

                                if (!encontrado) {
                                    System.out.println("⚠️ No se pudo disparar el click automático. Recordá que en SaleView.fxml tu botón de refrescar debe tener fx:id=\"btnRefresh\" o el texto 'Actualizar'.");
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("Error en el refresco asincrónico: " + ex.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}