package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.LoanDAO;
import com.edj.developer.apploans.dao.impl.LoanDAOImpl;
import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.Sale;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class LoanListController {

    @FXML private TableView<Loan> tableLoans;
    @FXML private TableColumn<Loan, Integer> colId;
    @FXML private TableColumn<Loan, String> colCustomer;
    @FXML private TableColumn<Loan, Double> colAmount, colTotal;
    @FXML private TableColumn<Loan, String> colInstallments;
    @FXML private TableColumn<Loan, String> colFrequency, colDate;
    @FXML private TableColumn<Loan, String> colStatus;
    @FXML private TableColumn<Loan, Void> colActions;

    @FXML private Label lblTotalLoans, lblActiveAmount;
    @FXML private TextField txtSearch;

    // El nuevo ComboBox del filtro
    @FXML private ComboBox<String> cmbStatusFilter;

    // FXML Componentes de Paginación
    @FXML private Pagination pagination;
    @FXML private ComboBox<Integer> cmbPageSize;
    @FXML private Label lblPageInfo;

    private final LoanDAO loanDAO = new LoanDAOImpl();
    private final ObservableList<Loan> currentPageItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupActionsColumn();
        setupPaginationControls();

        // Inicializar opciones del filtro por estado
        cmbStatusFilter.setItems(FXCollections.observableArrayList("TODOS", "ACTIVO", "FINALIZADO", "ANULADO"));
        cmbStatusFilter.setValue("TODOS");

        // Listeners para recarga en tiempo real
        txtSearch.textProperty().addListener((obs, oldText, newText) -> updatePaginationStructure());
        cmbStatusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updatePaginationStructure());

        handleRefresh();
    }

    private void setupTable() {
        // 1. Mapeos estándar por propiedad (Asegurate de incluir las que faltaban)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colFrequency.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status")); // <-- Enlaza con getStatus() de tu modelo Loan

        // 2. Renderizado del Badge en la celda de Estado (Tu bloque switch actual que está perfecto)
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null); setText(null); return;
                }
                String displayTxt = switch (status.toUpperCase()) {
                    case "ACTIVE" -> "ACTIVO";
                    case "COMPLETED" -> "FINALIZADO";
                    case "CANCELED" -> "ANULADO";
                    default -> status.toUpperCase();
                };
                Label badge = new Label(displayTxt);
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(switch (status.toUpperCase()) {
                    case "ACTIVE" -> "badge-success";
                    case "COMPLETED" -> "badge-primary";
                    case "CANCELED" -> "badge-danger";
                    default -> "badge-info";
                });
                HBox wrap = new HBox(badge);
                wrap.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        // 3. Formateo de las columnas de montos y cuotas (Tus bloques actuales)
        colAmount.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getAmount()));
        colAmount.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$ %,.2f", item));
            }
        });

        colTotal.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getTotalAmount()));
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("$ %,.2f", item));
            }
        });

        colInstallments.setCellValueFactory(cell -> {
            Loan l = cell.getValue();
            if (l.getInstallments() <= 0) return new SimpleStringProperty("-");
            double valorCuota = l.getTotalAmount() / l.getInstallments();
            return new SimpleStringProperty(String.format("%d cuotas de $ %,.2f", l.getInstallments(), valorCuota));
        });

        tableLoans.setItems(currentPageItems);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetails = new Button();
            {
                btnDetails.getStyleClass().addAll("btn-action", "btn-action-view");
                FontIcon icon = new FontIcon("fas-eye");
                icon.setIconSize(14);
                icon.setIconColor(javafx.scene.paint.Color.valueOf("#25467E"));
                btnDetails.setGraphic(icon);
                btnDetails.setTooltip(new Tooltip("Ver ficha detallada"));

                btnDetails.setOnAction(event -> {
                    Loan selectedLoan = getTableView().getItems().get(getIndex());
                    if (selectedLoan != null) {
                        showLoanDetails(selectedLoan);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetails);
            }
        });
    }

    private void setupPaginationControls() {
        cmbPageSize.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        cmbPageSize.setValue(10);

        cmbPageSize.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updatePaginationStructure();
        });

        pagination.currentPageIndexProperty().addListener((obs, oldPage, newPage) -> {
            loadTableData(newPage.intValue());
        });
    }

    @FXML
    public void handleRefresh() {
        loanDAO.checkAndUpdateOverdueInstallments();
        loadStats();
        updatePaginationStructure();
    }

    private String getSelectedStatusDb() {
        String sel = cmbStatusFilter.getValue();
        if (sel == null) return "TODOS";
        return switch (sel) {
            case "ACTIVO" -> "ACTIVE";
            case "FINALIZADO" -> "COMPLETED";
            case "ANULADO" -> "CANCELED";
            default -> "TODOS";
        };
    }

    private void updatePaginationStructure() {
        String search = txtSearch.getText();
        String statusFilter = getSelectedStatusDb();

        int totalRecords = loanDAO.countLoans(search, statusFilter);
        int pageSize = cmbPageSize.getValue();

        int pageCount = (int) Math.ceil((double) totalRecords / pageSize);
        pagination.setPageCount(pageCount <= 0 ? 1 : pageCount);

        // Forzar recarga de la página actual
        loadTableData(pagination.getCurrentPageIndex());
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        String statusFilter = getSelectedStatusDb();
        int pageSize = cmbPageSize.getValue();
        int offset = pageIndex * pageSize;

        List<Loan> loans = loanDAO.findAllPaged(search, statusFilter, pageSize, offset);
        currentPageItems.setAll(loans);

        int totalRecords = loanDAO.countLoans(search, statusFilter);
        int from = totalRecords == 0 ? 0 : offset + 1;
        int to = Math.min(offset + pageSize, totalRecords);

        lblPageInfo.setText(String.format("Mostrando %d – %d de %d registros", from, to, totalRecords));
    }

    private void loadStats() {
        String totalSql = "SELECT COUNT(*), SUM(amount) FROM loans";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(totalSql)) {
            if (rs.next()) {
                lblTotalLoans.setText(String.valueOf(rs.getInt(1)));
                lblActiveAmount.setText(String.format("$ %,.2f", rs.getDouble(2)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLoanDetails(Loan loan) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoanDetailView.fxml"));
            Parent detailView = loader.load();

            LoanDetailController controller = loader.getController();
            controller.setLoanData(loan);

            StackPane contentArea = (StackPane) tableLoans.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().add(detailView);
                contentArea.getChildren().forEach(node -> {
                    node.setVisible(node == detailView);
                    node.setManaged(node == detailView);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewLoan() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoanFormView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Registrar Nuevo Préstamo");
            stage.setScene(new Scene(root));

            stage.showAndWait();
            handleRefresh();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}