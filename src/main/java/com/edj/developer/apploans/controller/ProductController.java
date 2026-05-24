package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.ProductDAO;
import com.edj.developer.apploans.dao.impl.ProductDAOImpl;
import com.edj.developer.apploans.model.Product;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;

public class ProductController {

    @FXML private Label lblTotalProducts, lblInStock, lblLowStock, lblNoStock, lblPageInfo;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbStockFilter, cmbPageSize;
    @FXML private TableView<Product> tableProducts;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName, colDescription, colCreatedAt;
    @FXML private TableColumn<Product, Double> colBasePrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Void> colActions;
    @FXML private Pagination pagination;

    private final ProductDAO productDAO = new ProductDAOImpl();
    private final ObservableList<Product> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFiltersAndPagination();
        loadStats();
        updateTableData();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colBasePrice.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Dar formato monetario regionalizado a la columna de precio base
        colBasePrice.setCellFactory(tc -> new TableCell<>() {
            private final java.text.DecimalFormat formatter = new java.text.DecimalFormat("$#,##0.00", new java.text.DecimalFormatSymbols(new java.util.Locale("es", "AR")));

            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(formatter.format(price));
                }
            }
        });

        // Configurar los botones de Editar y Eliminar en cada fila
        setupActionsColumn();
    }

    private void setupFiltersAndPagination() {
        cmbStockFilter.setItems(FXCollections.observableArrayList("Todos", "CON STOCK", "STOCK BAJO", "SIN STOCK"));
        cmbStockFilter.setValue("Todos");
        cmbStockFilter.setOnAction(e -> { pagination.setCurrentPageIndex(0); updateTableData(); });

        cmbPageSize.setItems(FXCollections.observableArrayList("10", "20", "50"));
        cmbPageSize.setValue("10");
        cmbPageSize.setOnAction(e -> { pagination.setCurrentPageIndex(0); updateTableData(); });

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            pagination.setCurrentPageIndex(0);
            updateTableData();
        });

        pagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> updateTableData());
    }

    private void loadStats() {
        int total = productDAO.getTotalCount();
        int low = productDAO.getLowStockCount();
        int none = productDAO.getNoStockCount();

        lblTotalProducts.setText(String.valueOf(total));
        lblLowStock.setText(String.valueOf(low));
        lblNoStock.setText(String.valueOf(none));
        lblInStock.setText(String.valueOf(total - none));
    }

    private void updateTableData() {
        String search = txtSearch.getText();
        String filter = cmbStockFilter.getValue().equals("Todos") ? null : cmbStockFilter.getValue();
        int limit = Integer.parseInt(cmbPageSize.getValue());
        int pageIndex = pagination.getCurrentPageIndex();
        int offset = pageIndex * limit;

        int totalRecords = productDAO.countProducts(search, filter);
        int pageCount = (int) Math.ceil((double) totalRecords / limit);
        pagination.setPageCount(pageCount == 0 ? 1 : pageCount);

        productList.setAll(productDAO.findAllPaged(search, filter, limit, offset));
        tableProducts.setItems(productList);

        int from = totalRecords == 0 ? 0 : offset + 1;
        int to = Math.min(offset + limit, totalRecords);
        lblPageInfo.setText(String.format("Mostrando %d – %d de %d registros", from, to, totalRecords));
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final HBox box = new HBox(8);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnEdit.setMnemonicParsing(false);
                btnEdit.setTooltip(new Tooltip("Editar producto"));

                // Estilos limpios idénticos a Clientes
                btnEdit.getStyleClass().addAll("btn-action", "btn-action-edit");

                // Configuración del ícono nativo del LÁPIZ verde
                FontIcon iconEdit = new FontIcon("fas-pen");
                iconEdit.setIconSize(13);
                iconEdit.setIconColor(javafx.scene.paint.Color.valueOf("#198754"));
                btnEdit.setGraphic(iconEdit);

                box.getChildren().addAll(btnEdit);

                // Acción del evento limpio
                btnEdit.setOnAction(e -> {
                    Product p = getTableRow().getItem(); // getTableRow() evita desajustes en el index al paginar
                    if (p != null) {
                        handleEditProduct(p);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void showProductForm(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductFormView.fxml"));
            Parent root = loader.load();

            ProductFormController controller = loader.getController();
            controller.setProduct(product);

            Stage stage = new Stage();
            stage.setTitle(product == null ? "Nuevo Producto" : "Editar Producto");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(tableProducts.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // Si el formulario se guardó con éxito, notificamos y recargamos
            if (controller.isSaveSuccess()) {
                if (product == null) {
                    // Mensaje para Nuevo Producto
                    AlertHelper.showInfo(
                            "Operación Exitosa",
                            "Producto Registrado",
                            "El nuevo producto se ha agregado correctamente al inventario."
                    );
                } else {
                    // Mensaje para Producto Editado
                    AlertHelper.showInfo(
                            "Operación Exitosa",
                            "Producto Actualizado",
                            "Los cambios del producto fueron guardados con éxito."
                    );
                }

                loadStats();
                updateTableData();
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ProductController.class).error("Error abriendo el formulario de producto", e);
            AlertHelper.showError("Error de Sistema", "No se pudo abrir la ventana", e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        txtSearch.clear();
        cmbStockFilter.setValue("Todos");
        loadStats();
        updateTableData();
    }

    @FXML
    private void handleNewProduct() {
        showProductForm(null); // Pasamos null -> Modo Creación
    }

    private void handleEditProduct(Product product) {
        showProductForm(product); // Pasamos el objeto -> Modo Edición
    }

    private void handleDeleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Producto");
        alert.setHeaderText("¿Seguro que desea eliminar este producto?");
        alert.setContentText(product.getName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (productDAO.delete(product.getId())) {
                loadStats();
                updateTableData();
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR, "No se pudo eliminar el producto porque está asociado a una venta.");
                error.show();
            }
        }
    }
}