package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.SaleDAO;
import com.edj.developer.apploans.dao.impl.SaleDAOImpl;
import com.edj.developer.apploans.model.Sale;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class SaleController {

    @FXML private Button btnNewSale;
    @FXML private Label lblTotalSales;
    @FXML private Label lblTotalInvoiced;
    @FXML private Label lblActiveSales;
    @FXML private TextField txtSearch;
    @FXML private Button btnRefresh;

    @FXML private TableView<Sale> tableSales;
    @FXML private TableColumn<Sale, Integer> colId;
    @FXML private TableColumn<Sale, String> colCustomer;
    @FXML private TableColumn<Sale, String> colProduct;
    @FXML private TableColumn<Sale, Double> colPrice;
    @FXML private TableColumn<Sale, Double> colInterests;
    @FXML private TableColumn<Sale, Double> colTotal;
    @FXML private TableColumn<Sale, Integer> colInstallments;
    @FXML private TableColumn<Sale, String> colStatus;
    @FXML private TableColumn<Sale, Void> colActions;

    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Label lblPageInfo;
    @FXML private Pagination pagination;

    private final SaleDAO saleDAO = new SaleDAOImpl();
    private final DecimalFormat moneyFormatter = new DecimalFormat("$#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));

    private final int rowsPerPage = 10;

    @FXML
    public void initialize() {
        setupTableColumns();

        // Listener de búsqueda en tiempo real (re-escribe la paginación al tipear)
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePagination();
        });

        // Configuración inicial de la paginación vinculada a la tabla
        pagination.currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            loadTableData(newValue.intValue());
        });

        cmbStatusFilter.setItems(FXCollections.observableArrayList("TODOS", "ACTIVA", "FINALIZADA", "ANULADA"));
        cmbStatusFilter.setValue("TODOS");
        cmbStatusFilter.valueProperty().addListener((o, ov, nv) -> updatePagination());

        handleRefresh();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCustomer.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colInstallments.setCellValueFactory(new PropertyValueFactory<>("installments"));

        // Traducción automática del Estado con Badges de colores en base a tu CSS
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                // Cambiamos los nombres que vienen de la BD por el texto comercial en español
                String displayTxt = switch (status.toUpperCase()) {
                    case "ACTIVE" -> "ACTIVA";
                    case "COMPLETED" -> "FINALIZADA";
                    case "CANCELED" -> "ANULADA";
                    default -> status.toUpperCase();
                };

                Label badge = new Label(displayTxt);
                badge.getStyleClass().add("badge");

                // Le acoplamos la clase de color según el estado
                badge.getStyleClass().add(switch (status.toUpperCase()) {
                    case "ACTIVE" -> "badge-success";
                    case "COMPLETED" -> "badge-primary";
                    case "CANCELED" -> "badge-danger";
                    default -> "badge-info";
                });

                HBox wrap = new HBox(badge);
                wrap.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(wrap);
                setText(null);
            }
        });

        // Formateo de Precio de Venta
        colPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colPrice.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : moneyFormatter.format(value));
            }
        });

        // Formateo de Interés
        colInterests.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        colInterests.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value + "%");
            }
        });

        // Formateo de Total a Cobrar
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colTotal.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : moneyFormatter.format(value));
            }
        });

        // Botón de Ver Ficha que salta directo al StackPane del detalle
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button();
            private final HBox box = new HBox(8);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnView.setMnemonicParsing(false);
                btnView.setTooltip(new Tooltip("Ver plan de pagos de la venta"));
                btnView.getStyleClass().addAll("btn-action", "btn-action-view");

                FontIcon iconView = new FontIcon("fas-eye");
                iconView.setIconSize(13);
                iconView.setIconColor(javafx.scene.paint.Color.valueOf("#25467E"));
                btnView.setGraphic(iconView);

                box.getChildren().add(btnView);

                btnView.setOnAction(e -> {
                    Sale sale = getTableRow().getItem();
                    if (sale != null) {
                        handleViewSaleDetails(sale);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || getTableRow() == null || getTableRow().getItem() == null ? null : box);
            }
        });
    }

    private String getSelectedStatusDb() {
        String sel = cmbStatusFilter.getValue();
        if (sel == null) return "TODOS";
        return switch (sel) {
            case "ACTIVA" -> "ACTIVE";
            case "FINALIZADA" -> "COMPLETED";
            case "ANULADA" -> "CANCELED";
            default -> "TODOS";
        };
    }

    private void updatePagination() {
        int totalRecords = saleDAO.countSales(txtSearch.getText(), getSelectedStatusDb());
        int pageCount = (int) Math.ceil((double) totalRecords / rowsPerPage);
        pagination.setPageCount(pageCount <= 0 ? 1 : pageCount);
        loadTableData(pagination.getCurrentPageIndex());
    }

    private void loadTableData(int pageIndex) {
        String search = txtSearch.getText();
        String statusFilter = getSelectedStatusDb();
        int offset = pageIndex * rowsPerPage;

        List<Sale> sales = saleDAO.findAllPaged(search, statusFilter, rowsPerPage, offset);
        tableSales.setItems(FXCollections.observableArrayList(sales));

        int totalRecords = saleDAO.countSales(search, statusFilter);
        int from = totalRecords == 0 ? 0 : offset + 1;
        int to = Math.min(offset + rowsPerPage, totalRecords);
        lblPageInfo.setText("Mostrando " + from + " – " + to + " de " + totalRecords + " registros");
    }

    private void loadStats() {
        String totalSql = "SELECT COUNT(*), SUM(total_amount) FROM sales";
        String activeSql = "SELECT COUNT(*) FROM sales WHERE status = 'ACTIVE'";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(totalSql);
            if (rs.next()) {
                lblTotalSales.setText(String.valueOf(rs.getInt(1)));
                lblTotalInvoiced.setText(moneyFormatter.format(rs.getDouble(2)));
            }

            rs = stmt.executeQuery(activeSql);
            if (rs.next()) {
                lblActiveSales.setText(String.valueOf(rs.getInt(1)));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNewSale() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SaleFormView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Registrar Nueva Venta Financiera");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            handleRefresh();
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "No se pudo abrir el formulario", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadStats();
        updatePagination();
    }

    // Método de navegación fluida e incrustación sobre el contentArea principal
    private void handleViewSaleDetails(Sale sale) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SaleDetailView.fxml"));
            Parent detailView = loader.load();

            SaleDetailController controller = loader.getController();
            controller.setSaleData(sale);

            StackPane contentArea = (StackPane) tableSales.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().add(detailView);
                contentArea.getChildren().forEach(node -> {
                    node.setVisible(node == detailView);
                    node.setManaged(node == detailView);
                });
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "No se pudo abrir el detalle de la venta", e.getMessage());
        }
    }
}