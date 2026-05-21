package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.LoanDAO;
import com.edj.developer.apploans.dao.impl.LoanDAOImpl;
import com.edj.developer.apploans.model.Loan;
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
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LoanListController {

    @FXML private TableView<Loan> tableLoans;
    @FXML private TableColumn<Loan, Integer> colId;
    @FXML private TableColumn<Loan, String> colCustomer;
    @FXML private TableColumn<Loan, Double> colAmount, colTotal;
    @FXML private TableColumn<Loan, String> colInstallments;
    @FXML private TableColumn<Loan, String> colFrequency, colDate;
    @FXML private TableColumn<Loan, Void> colActions;

    @FXML private Label lblTotalLoans, lblActiveAmount;
    @FXML private TextField txtSearch;

    // FXML Componentes de Paginación
    @FXML private Pagination pagination;
    @FXML private ComboBox<Integer> cmbPageSize;
    @FXML private Label lblPageInfo;

    private final LoanDAO loanDAO = new LoanDAOImpl();

    // Listas para control de memoria y filtrado
    private List<Loan> masterList = new ArrayList<>();
    private List<Loan> filteredList = new ArrayList<>();
    private final ObservableList<Loan> currentPageItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupActionsColumn();
        setupPaginationControls();
        handleRefresh();

        txtSearch.textProperty().addListener((obs, oldText, newText) -> {
            applyFilter(newText);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colFrequency.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));

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
                FontIcon icon = new FontIcon("fas-eye");
                icon.setIconSize(14);
                icon.setIconColor(javafx.scene.paint.Color.valueOf("#0d6efd"));
                btnDetails.setGraphic(icon);
                btnDetails.getStyleClass().addAll("btn", "btn-sm", "btn-light");
                btnDetails.setTooltip(new Tooltip("Ver ficha detallada"));

                btnDetails.setOnAction(event -> {
                    Loan selectedLoan = getTableView().getItems().get(getIndex());
                    showLoanDetails(selectedLoan);
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
        // Configuramos opciones de tamaño por página
        cmbPageSize.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        cmbPageSize.setValue(10); // Valor inicial predeterminado

        // Escuchar cambios de tamaño de página
        cmbPageSize.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                calculatePaginationStructure();
            }
        });

        // Escuchar cambios de página activa en el componente de JavaFX
        pagination.currentPageIndexProperty().addListener((obs, oldPage, newPage) -> {
            updateTableContentPage(newPage.intValue());
        });
    }

    @FXML
    public void handleRefresh() {
        loanDAO.checkAndUpdateOverdueInstallments();
        masterList = loanDAO.findAll(); // Carga maestra de DB

        // Al refrescar mantenemos sincronizadas estadísticas y filtros
        updateStats();
        applyFilter(txtSearch.getText());
    }

    private void applyFilter(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            filteredList = new ArrayList<>(masterList);
        } else {
            String lower = keyword.toLowerCase().trim();
            filteredList = masterList.stream()
                    .filter(l -> l.getCustomerName() != null && l.getCustomerName().toLowerCase().contains(lower))
                    .collect(Collectors.toList());
        }
        calculatePaginationStructure();
    }

    private void calculatePaginationStructure() {
        int totalItems = filteredList.size();
        int pageSize = cmbPageSize.getValue();

        // Calculamos cuántas páginas reales se necesitan
        int pageCount = (int) Math.ceil((double) totalItems / pageSize);
        if (pageCount == 0) pageCount = 1;

        pagination.setPageCount(pageCount);

        // Forzamos actualización de la página inicial (0)
        if (pagination.getCurrentPageIndex() == 0) {
            updateTableContentPage(0);
        } else {
            pagination.setCurrentPageIndex(0);
        }
    }

    private void updateTableContentPage(int pageIdx) {
        int totalItems = filteredList.size();
        int pageSize = cmbPageSize.getValue();

        int fromIndex = pageIdx * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);

        // Subconjunto usando Stream
        List<Loan> pageData = filteredList.stream()
                .skip(fromIndex)
                .limit(pageSize)
                .collect(Collectors.toList());

        currentPageItems.setAll(pageData);

        // Actualizar el texto informativo del footer
        if (totalItems == 0) {
            lblPageInfo.setText("Mostrando 0 – 0 de 0 registros");
        } else {
            lblPageInfo.setText(String.format("Mostrando %d – %d de %d registros", (fromIndex + 1), toIndex, totalItems));
        }
    }

    private void updateStats() {
        lblTotalLoans.setText(String.valueOf(masterList.size()));
        double total = masterList.stream().mapToDouble(Loan::getAmount).sum();
        lblActiveAmount.setText(String.format("$ %,.2f", total));
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