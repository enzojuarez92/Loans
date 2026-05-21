package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.LoanDAO;
import com.edj.developer.apploans.dao.impl.LoanDAOImpl;
import com.edj.developer.apploans.model.Customer;
import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanSummary;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CustomerDetailController
 *
 * Controlador para mostrar la ficha integral del cliente y su historial de préstamos.
 */
public class CustomerDetailController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── Capa de Datos ───────────────────────────────────────────────────
    private final LoanDAO loanDAO = new LoanDAOImpl();

    // ─── FXML – Header ────────────────────────────────────────────────────
    @FXML private Label lblCustomerName;
    @FXML private Label lblCustomerDoc;
    @FXML private Label lblStatusBadge;

    // ─── FXML – Estadísticas ──────────────────────────────────────────────
    @FXML private Label lblTotalLoans;
    @FXML private Label lblPendingBalance;

    // ─── FXML – Datos Personales ──────────────────────────────────────────
    @FXML private Label lblPhone;
    @FXML private Label lblEmail;
    @FXML private Label lblAddress;
    @FXML private Label lblCity;
    @FXML private Label lblBirthDate;
    @FXML private Label lblNotes;

    // ─── FXML – Tabla Historial de Préstamos ─────────────────────────────
    @FXML private TableView<Loan> tableCustomerLoans;
    @FXML private TableColumn<Loan, Integer> colLoanId;
    @FXML private TableColumn<Loan, Double> colLoanAmount;
    @FXML private TableColumn<Loan, Double> colLoanRemaining;
    @FXML private TableColumn<Loan, String> colLoanStatus;
    @FXML private TableColumn<Loan, String> colLoanDate;

    /**
     * JavaFX ejecuta automáticamente este método al inflar el FXML.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
    }

    private void setupTableColumns() {
        colLoanId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colLoanId.setStyle("-fx-alignment: CENTER;");

        // Formateo de montos a dos decimales
        colLoanAmount.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getAmount()));
        colLoanAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setText(empty || amount == null ? null : String.format("$%,.2f", amount));
            }
        });

        // Mostramos el saldo restante del préstamo (guardado temporalmente en interestRate en tu query)
        colLoanRemaining.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getInterestRate()));
        colLoanRemaining.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double remaining, boolean empty) {
                super.updateItem(remaining, empty);
                if (empty || remaining == null) {
                    setText(null);
                } else {
                    setText(String.format("$%,.2f", remaining));
                    setStyle(remaining > 0 ? "-fx-text-fill: #b58105; -fx-font-weight: bold;" : "-fx-text-fill: #198754;");
                }
            }
        });

        // Fecha de inicio formateada
        colLoanDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStartDate()));
        colLoanDate.setStyle("-fx-alignment: CENTER;");

        // CORRECCIÓN: Estado del préstamo mapeado automáticamente a Español mediante el Enum de Loan
        colLoanStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatusDisplayName()));
        colLoanStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String displayStatus, boolean empty) {
                super.updateItem(displayStatus, empty);
                if (empty || displayStatus == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(displayStatus);
                badge.getStyleClass().add("badge");

                // Mapeamos los colores basados en la traducción que viene del Enum
                badge.getStyleClass().add(switch (displayStatus) {
                    case "ACTIVO" -> "badge-success";
                    case "COMPLETADO", "FINALIZADO" -> "badge-secondary";
                    default -> "badge-info";
                });

                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap);
                setText(null);
            }
        });

        tableCustomerLoans.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * Contrato público llamado desde el controlador principal para rellenar la ficha.
     */
    public void setCustomer(Customer customer) {
        if (customer == null) return;

        // 1. Rellenar Cabecera & Estado traduciendo dinámicamente si viene en inglés
        lblCustomerName.setText(customer.getFullName());
        lblCustomerDoc.setText("Doc: " + nullSafe(customer.getDocNumber()));

        String rawStatus = customer.getStatus() != null ? customer.getStatus().toUpperCase() : "ACTIVO";

        // Normalizamos visualmente el estado del cliente a Español para la UI
        String statusDisplay = switch (rawStatus) {
            case "ACTIVE", "ACTIVO" -> "ACTIVO";
            case "INACTIVE", "INACTIVO" -> "INACTIVO";
            default -> rawStatus;
        };

        lblStatusBadge.setText(statusDisplay);

        // Estilos usando tus paletas de colores basadas en el estado en español
        lblStatusBadge.setStyle(switch (statusDisplay) {
            case "ACTIVO" -> "-fx-background-color: #d1e7dd; -fx-text-fill: #0f5132; -fx-padding: 5 10; -fx-background-radius: 4; -fx-font-weight: bold;";
            case "INACTIVE", "INACTIVO" -> "-fx-background-color: #f8d7da; -fx-text-fill: #842029; -fx-padding: 5 10; -fx-background-radius: 4; -fx-font-weight: bold;";
            default -> "-fx-background-color: #e2e3e5; -fx-text-fill: #41464b; -fx-padding: 5 10; -fx-background-radius: 4; -fx-font-weight: bold;";
        });

        // 2. Rellenar Datos Personales
        lblPhone.setText(nullSafe(customer.getPhone()));
        lblEmail.setText(nullSafe(customer.getEmail()));
        lblAddress.setText(nullSafe(customer.getAddress()));
        lblCity.setText(nullSafe(customer.getCity()));
        lblBirthDate.setText(customer.getBirthDate() != null ? customer.getBirthDate().format(DATE_FMT) : "—");

        String notes = customer.getNotes();
        lblNotes.setText(notes != null && !notes.isBlank() ? notes : "Sin observaciones.");

        // 3. Cargar Métricas y Tabla del Historial
        loadLoanHistory(customer.getId());
    }

    private void loadLoanHistory(int customerId) {
        try {
            // A. Consultar resumen analítico de base de datos
            LoanSummary summary = loanDAO.getSummaryByCustomerId(customerId);

            // Poblar las tarjetas superiores con el desglose exacto
            lblTotalLoans.setText("%d  (%d Activos / %d Finalizados)".formatted(
                    summary.getTotalLoans(),
                    summary.getActiveLoans(),
                    summary.getCompletedLoans()
            ));

            lblPendingBalance.setText(String.format("$%,.2f", summary.getPendingBalance()));

            // B. Consultar listado de préstamos e inyectarlo a la tabla
            List<Loan> history = loanDAO.findLoansByCustomerId(customerId);
            tableCustomerLoans.setItems(FXCollections.observableArrayList(history));

        } catch (Exception e) {
            e.printStackTrace();
            lblTotalLoans.setText("Error");
            lblPendingBalance.setText("$0.00");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblCustomerName.getScene().getWindow();
        stage.close();
    }

    private String nullSafe(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}