package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.LoanDAO;
import com.edj.developer.apploans.dao.impl.LoanDAOImpl;
import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanPayment;
import com.edj.developer.apploans.model.LoanReceipt; // Tu nuevo modelo
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.Optional;

public class LoanDetailController {

    @FXML private Button btnCancelLoan, btnRegisterPayment;
    @FXML private Label lblCustomerName, lblLoanId, lblTotalAmount, lblPaidAmount, lblPendingAmount;

    // Tabla 1: Cuotas
    @FXML private TableView<LoanPayment> tablePayments;
    @FXML private TableColumn<LoanPayment, Integer> colNumber;
    @FXML private TableColumn<LoanPayment, String> colDueDate, colPaymentDate, colStatus;
    @FXML private TableColumn<LoanPayment, Double> colAmount, colPaidAmount, colBalance;

    // 💡 Tabla 2: Historial de Entregas reales
    @FXML private TableView<LoanReceipt> tableHistory;
    @FXML private TableColumn<LoanReceipt, Integer> colHistId;
    @FXML private TableColumn<LoanReceipt, String> colHistDate, colHistNotes;
    @FXML private TableColumn<LoanReceipt, Double> colHistAmount;
    @FXML private TableColumn<LoanReceipt, Void> colHistActions;

    private final LoanDAO loanDAO = new LoanDAOImpl();
    private Loan currentLoan;

    @FXML
    public void initialize() {
        setupColumns();
        setupHistoryColumns();
        setupHistoryActionsColumn();
    }

    public void setLoanData(Loan loan) {
        this.currentLoan = loan;
        refreshData();
    }

    private void setupColumns() {
        colNumber.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getInstallmentNumber()).asObject());
        colDueDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDueDate()));
        colPaymentDate.setCellValueFactory(cellData -> {
            String pDate = cellData.getValue().getPaymentDate();
            return new javafx.beans.property.SimpleStringProperty((pDate == null || pDate.isEmpty()) ? "-" : pDate);
        });

        colAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        colPaidAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getPaidAmount()).asObject());
        colBalance.setCellValueFactory(cellData -> {
            double balance = cellData.getValue().getAmount() - cellData.getValue().getPaidAmount();
            return new javafx.beans.property.SimpleDoubleProperty(balance).asObject();
        });

        // Factorías de renderizado de moneda
        var currencyFactory = new javafx.util.Callback<TableColumn<LoanPayment, Double>, TableCell<LoanPayment, Double>>() {
            @Override public TableCell<LoanPayment, Double> call(TableColumn<LoanPayment, Double> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(Double item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : String.format("$ %,.2f", item));
                    }
                };
            }
        };
        colAmount.setCellFactory(currencyFactory);
        colPaidAmount.setCellFactory(currencyFactory);
        colBalance.setCellFactory(currencyFactory);

        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatusDisplayName()));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String displayStatus, boolean empty) {
                super.updateItem(displayStatus, empty);
                if (empty || displayStatus == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(displayStatus);
                    switch (displayStatus.toUpperCase()) {
                        case "PAGADA", "PAGADO" -> setTextFill(Color.web("#198754"));
                        case "PARCIAL", "PAGO PARCIAL" -> setTextFill(Color.web("#fd7e14"));
                        case "VENCIDA", "VENCIDO" -> setTextFill(Color.web("#b30000"));
                        default -> setTextFill(Color.web("#0d6efd"));
                    }
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });
    }

    // 💡 CONFIGURACIÓN DE LAS COLUMNAS DE LA TABLA DE HISTORIAL
    private void setupHistoryColumns() {
        colHistId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getId()).asObject());
        colHistDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPaymentDate()));
        colHistNotes.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getNotes()));
        colHistAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());

        colHistAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$ %,.2f", item));
            }
        });
    }

    // 💡 NUEVO ACCIONADOR GENERAL: Abre el modal pasándole todo el saldo pendiente total
    @FXML
    private void handleOpenPaymentModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PaymentModalView.fxml"));
            Parent root = loader.load();

            PaymentModalController modal = loader.getController();

            // Calculamos el saldo pendiente de TODO el préstamo
            double total = currentLoan.getTotalAmount();
            double paid = currentLoan.getPayments().stream().mapToDouble(LoanPayment::getPaidAmount).sum();
            double saldoTotalRestante = total - paid;

            if (saldoTotalRestante <= 0) {
                AlertHelper.showInfo("Préstamo Saldado", "Sin saldos", "Este préstamo ya se encuentra completamente pagado.");
                return;
            }

            modal.setInitialData(saldoTotalRestante);

            Stage stage = new Stage();
            stage.setTitle("Registrar Entrega de Efectivo");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Double montoEntregado = modal.getAmountResult();
            if (montoEntregado != null && montoEntregado > 0) {
                // 🚀 GATILLAMOS LA CASCADA MÁGICA QUE CODEAMOS EN EL DAO
                boolean ok = loanDAO.processCascadePayment(currentLoan.getId(), montoEntregado, "Entrega parcial en efectivo");
                if (ok) {
                    refreshData();
                    AlertHelper.showInfo("Éxito", "Cobro registrado", "El pago se distribuyó correctamente en las cuotas.");
                } else {
                    AlertHelper.showError("Error", "Error de guardado", "No se pudo procesar la cascada de cobros.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshData() {
        this.currentLoan = loanDAO.findFullLoanById(currentLoan.getId());

        if (currentLoan != null) {
            lblCustomerName.setText(currentLoan.getCustomerName());
            lblLoanId.setText("Préstamo #" + currentLoan.getId());

            double total = currentLoan.getTotalAmount();
            double paid = currentLoan.getPayments().stream().mapToDouble(LoanPayment::getPaidAmount).sum();

            lblTotalAmount.setText(String.format("$ %.2f", total));
            lblPaidAmount.setText(String.format("$ %.2f", paid));
            lblPendingAmount.setText(String.format("$ %.2f", total - paid));

            tablePayments.setItems(FXCollections.observableArrayList(currentLoan.getPayments()));

            // 💡 Cargamos el nuevo historial de puchos abajo
            if (currentLoan.getReceipts() != null) {
                tableHistory.setItems(FXCollections.observableArrayList(currentLoan.getReceipts()));
            }

            boolean tienePagosRealizados = currentLoan.getPayments().stream()
                    .anyMatch(p -> "PAID".equalsIgnoreCase(p.getStatus()) || "PARTIAL".equalsIgnoreCase(p.getStatus()));
            boolean yaEstaAnulado = "CANCELED".equalsIgnoreCase(currentLoan.getStatus());

            btnCancelLoan.setDisable(tienePagosRealizados || yaEstaAnulado);

            // Si el préstamo ya está completamente cobrado o anulado, bloqueamos registrar más cobros
            btnRegisterPayment.setDisable((total - paid) <= 0 || yaEstaAnulado);
        }
    }

    @FXML
    private void handleBack() {
        try {
            Scene scene = lblCustomerName.getScene();
            if (scene != null) {
                StackPane contentArea = (StackPane) scene.lookup("#contentArea");

                // 1. Limpiamos la vista actual con tu reflexión en el MainController
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

                // 2. Disparamos la vuelta a la pantalla de préstamos
                var botonPrestamos = scene.lookup("#btnPrestamos");
                if (botonPrestamos instanceof Button btn) {
                    btn.fire();
                }

                // ─── 💡 LA SOLUCIÓN DEFINITIVA CON ASINCRONÍA VISUAL ───
                // Platform.runLater espera a que JavaFX termine de meter la nueva lista en el contentArea.
                javafx.application.Platform.runLater(() -> {
                    try {
                        if (contentArea != null) {
                            // Buscamos todos los botones dentro de la NUEVA pantalla que ya se renderizó
                            var todosLosBotones = contentArea.lookupAll(".btn");
                            for (javafx.scene.Node nodo : todosLosBotones) {
                                if (nodo instanceof Button b && b.getOnAction() != null) {
                                    // Buscamos específicamente el método handleRefresh que me mostraste en tu FXML
                                    if (b.getOnAction().toString().contains("handleRefresh")) {
                                        b.fire(); // ¡Gatillamos el refresco real!
                                        System.out.println("🚀 ¡Refresco automático ejecutado con éxito!");
                                        break;
                                    }
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

    @FXML
    private void handleCancelLoan() {
        if (currentLoan == null) return;
        var confirmacion = AlertHelper.showConfirm("Anular Préstamo", "¿Está seguro?", "Se pasará todo a 'ANULADO'.");
        if (confirmacion.isPresent() && confirmacion.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            if (loanDAO.cancelLoan(currentLoan.getId())) {
                refreshData();
                AlertHelper.showInfo("Éxito", "Préstamo Anulado", "Correctamente.");
            } else {
                AlertHelper.showError("Error", "Error", "No se pudo anular.");
            }
        }
    }

    @FXML private void handleGenerateReport() { showPrintPreview(); }

    private void showPrintPreview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReportView.fxml"));
            Parent root = loader.load();
            ReportController controller = loader.getController();
            controller.setData(currentLoan);
            Stage stage = new Stage();
            stage.setTitle("Resumen de Cuenta - " + currentLoan.getCustomerName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ─── 🗑️ CONFIGURACIÓN DEL BOTÓN DE ANULACIÓN DE PAGO ───
    private void setupHistoryActionsColumn() {
        colHistActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();
            private final FontIcon icon = new FontIcon("fas-trash-alt");

            {
                btnDelete.setGraphic(icon);
                // Estilo compacto, elegante y rojo sutil para el peligro
                btnDelete.setStyle("-fx-cursor: hand; -fx-background-color: #fce8e6; -fx-text-fill: #a51d24; -fx-padding: 4 8; -fx-background-radius: 4;");

                btnDelete.setOnAction(event -> {
                    LoanReceipt receipt = getTableView().getItems().get(getIndex());

                    Optional<ButtonType> result = AlertHelper.showConfirm(
                            "Anular Recibo de Pago",
                            "¿Estás seguro de que querés eliminar este pago?",
                            "El monto de $ " + String.format("%,.2f", receipt.getAmount()) + " será revocado de las cuotas."
                    );

                    if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {

                        // Buscamos la última cuota que tiene saldo pagado para indicarle al DAO por dónde empezar a restar
                        int targetInstallmentId = 0;
                        if (currentLoan != null && currentLoan.getPayments() != null) {
                            targetInstallmentId = currentLoan.getPayments().stream()
                                    .filter(p -> "PAID".equalsIgnoreCase(p.getStatus()) || "PARTIAL".equalsIgnoreCase(p.getStatus()))
                                    .mapToInt(LoanPayment::getId)
                                    .max() // Agarramos el ID más alto de cuota tocada
                                    .orElse(0);
                        }

                        // Ejecutamos tu nuevo método del DAO en inglés
                        boolean success = loanDAO.revertLastPayment(
                                receipt.getId(),
                                currentLoan.getId(),
                                receipt.getAmount(),
                                0
                        );

                        if (success) {
                            // Refrescamos en caliente usando tu método nativo del controlador
                            refreshData();
                            AlertHelper.showInfo("Pago Revertido", "Éxito", "El recibo fue eliminado y las cuotas se recalcularon correctamente.");
                        } else {
                            AlertHelper.showError("Error", "Error de base de datos", "No se pudo procesar la anulación del pago.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.collections.ObservableList<LoanReceipt> historyList = getTableView().getItems();
                    LoanReceipt currentReceipt = historyList.get(getIndex());

                    // Como ordenás el historial por "id DESC" en la consulta SQL del DAO,
                    // la posición 0 siempre va a ser el último pago ingresado al sistema.
                    LoanReceipt lastReceiptMade = historyList.get(0);

                    // Condición de seguridad extrema: el botón solo aparece en la última entrega realizada
                    if (currentReceipt.getId() == lastReceiptMade.getId() && !"CANCELED".equalsIgnoreCase(currentLoan.getStatus())) {
                        setGraphic(btnDelete);
                    } else {
                        setGraphic(null); // Oculto para blindar los cobros más viejos
                    }
                }
            }
        });
    }
}