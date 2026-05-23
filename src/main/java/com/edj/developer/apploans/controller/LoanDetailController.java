package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.LoanDAO;
import com.edj.developer.apploans.dao.impl.LoanDAOImpl;
import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanPayment;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.print.PrinterJob;
import javafx.print.Printer;
import java.io.IOException;

public class LoanDetailController {

    @FXML private Label lblCustomerName, lblLoanId, lblTotalAmount, lblPaidAmount, lblPendingAmount, lblStatusBadge;
    @FXML private TableView<LoanPayment> tablePayments;
    @FXML private TableColumn<LoanPayment, Integer> colNumber;
    @FXML private TableColumn<LoanPayment, String> colDueDate, colStatus;
    @FXML private TableColumn<LoanPayment, Double> colAmount;
    @FXML private TableColumn<LoanPayment, Void> colAction;
    @FXML private TableColumn<LoanPayment, Double> colPaidAmount;
    @FXML private TableColumn<LoanPayment, Double> colBalance;

    private final LoanDAO loanDAO = new LoanDAOImpl();
    private Loan currentLoan;

    @FXML
    public void initialize() {
        setupColumns();
    }

    // Este método lo llamarás desde el LoanListController al abrir la ventana
    public void setLoanData(Loan loan) {
        this.currentLoan = loan;
        refreshData();
    }

    private void setupColumns() {
        colNumber.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getInstallmentNumber()).asObject());
        colDueDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDueDate()));
        colAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
        colPaidAmount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getPaidAmount()).asObject());
        colBalance.setCellValueFactory(cellData -> {
            double balance = cellData.getValue().getAmount() - cellData.getValue().getPaidAmount();
            return new javafx.beans.property.SimpleDoubleProperty(balance).asObject();
        });

        // Modificado para usar la traducción automática del modelo
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatusDisplayName()));

        // Formateo de celda de estado con colores dinámicos basados en la traducción en español
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String displayStatus, boolean empty) {
                super.updateItem(displayStatus, empty);
                if (empty || displayStatus == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(displayStatus);
                    // Evaluamos según la respuesta en español de tu Enum
                    switch (displayStatus.toUpperCase()) {
                        case "PAGADA", "PAGADO" -> setTextFill(Color.web("#198754"));       // Verde
                        case "PARCIAL", "PAGO PARCIAL" -> setTextFill(Color.web("#fd7e14")); // Naranja
                        case "VENCIDA", "VENCIDO" -> setTextFill(Color.web("#b30000"));     // Rojo Oscuro / Alerta
                        default -> setTextFill(Color.web("#0d6efd"));                        // Azul para PENDIENTE estándar
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
                // Unificamos el diseño al estilo "Eye" (btn-light sutil con ícono coloreado)
                FontIcon icon = new FontIcon("fas-check-circle");
                icon.setIconSize(14);
                icon.setIconColor(Color.valueOf("#198754")); // Ícono verde de confirmación

                btnPay.setGraphic(icon);
                btnPay.getStyleClass().addAll("btn", "btn-sm", "btn-light"); // Fondo sutil
                btnPay.setTooltip(new Tooltip("Procesar cobro de cuota"));

                btnPay.setOnAction(event -> {
                    LoanPayment p = getTableView().getItems().get(getIndex());
                    handlePayInstallment(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    LoanPayment p = getTableView().getItems().get(getIndex());
                    // El botón aparece si la cuota NO está pagada completamente (aplica a PENDING, PARTIAL y OVERDUE)
                    boolean mostrarBoton = !"PAID".equalsIgnoreCase(p.getStatus());
                    setGraphic(mostrarBoton ? btnPay : null);
                }
            }
        });
    }

    // Reemplaza el método handlePayInstallment en tu LoanDetailController:

    private void handlePayInstallment(LoanPayment payment) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PaymentModalView.fxml"));
            Parent root = loader.load();

            PaymentModalController modal = loader.getController();
            // El saldo pendiente es: Monto total de la cuota - lo que ya pagó
            double saldoDeEstaCuota = payment.getAmount() - payment.getPaidAmount();
            modal.setInitialData(saldoDeEstaCuota);

            Stage stage = new Stage();
            stage.setTitle("Cobrar Cuota");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            Double montoEntregado = modal.getAmountResult();
            if (montoEntregado != null) {
                // Calculamos el nuevo acumulado
                double nuevoPagado = payment.getPaidAmount() + montoEntregado;
                String nuevoEstado = (nuevoPagado >= payment.getAmount()) ? "PAID" : "PARTIAL";

                // Necesitarás actualizar tu DAO para que acepte el monto pagado
                boolean ok = loanDAO.updatePaymentStatusWithAmount(payment.getId(), nuevoEstado, nuevoPagado);
                if (ok) refreshData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshData() {
        // Volvemos a traer el préstamo fresco de la DB
        this.currentLoan = loanDAO.findFullLoanById(currentLoan.getId());

        if (currentLoan != null) {
            lblCustomerName.setText(currentLoan.getCustomerName());
            lblLoanId.setText("Préstamo #" + currentLoan.getId());

            double total = currentLoan.getTotalAmount();
            double paid = currentLoan.getPayments().stream()
                    .mapToDouble(LoanPayment::getPaidAmount)
                    .sum();

            lblTotalAmount.setText(String.format("$ %.2f", total));
            lblPaidAmount.setText(String.format("$ %.2f", paid));
            lblPendingAmount.setText(String.format("$ %.2f", total - paid));

            tablePayments.setItems(FXCollections.observableArrayList(currentLoan.getPayments()));
        }
    }

    @FXML
    private void handleBack() {
        try {
            javafx.scene.Scene scene = lblCustomerName.getScene();
            if (scene != null) {

                // 1. Buscamos el contentArea para llegar al MainController
                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) scene.lookup("#contentArea");
                if (contentArea != null && contentArea.getParent() instanceof javafx.scene.layout.BorderPane borderPane) {

                    if (borderPane.getUserData() instanceof MainController mainController) {
                        // 2. ¡EL TRUCO MAESTRO!: Usamos reflexión de Java para poner 'currentView' en null.
                        // Esto engaña al if(viewKey.equals(currentView)) y lo obliga a recargar la lista.
                        try {
                            java.lang.reflect.Field field = MainController.class.getDeclaredField("currentView");
                            field.setAccessible(true);
                            field.set(mainController, null); // Forzamos que se olvide de la vista actual
                        } catch (Exception re) {
                            System.out.println("No se pudo resetear currentView por reflexión: " + re.getMessage());
                        }
                    }
                }

                // 3. Ahora que el MainController cree que venimos de la nada, el clic va a levantar la lista sí o sí
                javafx.scene.Node botonPrestamos = scene.lookup("#btnPrestamos");
                if (botonPrestamos instanceof javafx.scene.control.Button btn) {
                    btn.fire();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGenerateReport() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Generar Resumen");
        alert.setHeaderText("¿Desea generar el resumen de cuentas?");
        alert.setContentText("Se creará un documento con el estado actual de todos los pagos.");

        // Personalizar un poco el Alert para que use tu CSS
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");

        if (alert.showAndWait().get() == ButtonType.OK) {
            showPrintPreview();
        }
    }

    private void showPrintPreview() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReportView.fxml"));
            Parent root = loader.load();

            ReportController controller = loader.getController();
            controller.setData(currentLoan);

            Stage stage = new Stage();
            stage.setTitle("Resumen de Cuenta - " + currentLoan.getCustomerName());

            // Hacerlo modal para que no interactúen con lo de atrás mientras ven el reporte
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.setScene(new Scene(root));
            stage.show();

            // ¡Ya no ponemos el PrinterJob aquí!
            // Ahora el usuario decide cuándo imprimir desde la nueva ventana.

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}