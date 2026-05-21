package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.CustomerDAO;
import com.edj.developer.apploans.dao.impl.CustomerDAOImpl;
import com.edj.developer.apploans.model.Customer;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CustomerController
 *
 * Gestión de la vista principal de clientes con filtrado y paginación en español.
 */
public class CustomerController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    // ─── Configuración ────────────────────────────────────────────────────
    private static final int     DEFAULT_PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── DAO ──────────────────────────────────────────────────────────────
    private final CustomerDAO customerDAO = new CustomerDAOImpl();

    // ─── FXML – Estadísticas ──────────────────────────────────────────────
    @FXML private Label lblTotalCustomers;
    @FXML private Label lblActiveCustomers;
    @FXML private Label lblInactiveCustomers;
    @FXML private Label lblWithLoans;

    // ─── FXML – Filtros ───────────────────────────────────────────────────
    @FXML private TextField      txtSearch;
    @FXML private ComboBox<String>   cmbStatusFilter;
    @FXML private Button              btnNewCustomer;
    @FXML private Button              btnRefresh;

    // ─── FXML – Tabla ─────────────────────────────────────────────────────
    @FXML private TableView<Customer>           tableCustomers;
    @FXML private TableColumn<Customer, Integer> colId;
    @FXML private TableColumn<Customer, String>  colFullName;
    @FXML private TableColumn<Customer, String>  colDocNumber;
    @FXML private TableColumn<Customer, String>  colPhone;
    @FXML private TableColumn<Customer, String>  colEmail;
    @FXML private TableColumn<Customer, String>  colCity;
    @FXML private TableColumn<Customer, String>  colStatus;
    @FXML private TableColumn<Customer, String>  colCreatedAt;
    @FXML private TableColumn<Customer, Void>    colActions;

    // ─── FXML – Paginación ────────────────────────────────────────────────
    @FXML private Pagination            pagination;
    @FXML private ComboBox<Integer>    cmbPageSize;
    @FXML private Label                lblPageInfo;

    // ─── Estado interno ───────────────────────────────────────────────────
    private ObservableList<Customer> masterList;
    private FilteredList<Customer>   filteredList;
    private ObservableList<Customer> currentPageList;

    private int currentPage  = 0;
    private int pageSize     = DEFAULT_PAGE_SIZE;

    /* ═══════════════════════════════════════════════════════════════════
       INICIALIZACIÓN
       ═══════════════════════════════════════════════════════════════════ */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        log.info("Inicializando Controlador de Clientes");
        initLists();
        setupFilters();
        setupColumns();
        setupPagination();
        loadData();
        refreshStats();
        tableCustomers.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void initLists() {
        masterList      = FXCollections.observableArrayList();
        filteredList    = new FilteredList<>(masterList, c -> true);
        currentPageList = FXCollections.observableArrayList();
        tableCustomers.setItems(currentPageList);
    }

    /* ═══════════════════════════════════════════════════════════════════
       FILTROS
       ═══════════════════════════════════════════════════════════════════ */

    private void setupFilters() {
        cmbStatusFilter.setItems(FXCollections.observableArrayList("Todos", "ACTIVO", "INACTIVO"));
        cmbStatusFilter.setValue("Todos");

        txtSearch.textProperty().addListener((obs, o, n) -> {
            currentPage = 0;
            applyFilter();
        });

        cmbStatusFilter.valueProperty().addListener((obs, o, n) -> {
            currentPage = 0;
            applyFilter();
        });
    }

    private void applyFilter() {
        String query  = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
        String status = cmbStatusFilter.getValue();

        filteredList.setPredicate(c -> {
            boolean matchesText   = query.isEmpty() || c.getSearchableText().contains(query);
            boolean matchesStatus = "Todos".equals(status) || status == null || status.equalsIgnoreCase(c.getStatus());
            return matchesText && matchesStatus;
        });

        refreshPagination();
    }

    /* ═══════════════════════════════════════════════════════════════════
       COLUMNAS DE LA TABLA
       ═══════════════════════════════════════════════════════════════════ */

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setStyle("-fx-alignment: CENTER;");

        colFullName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFullName()));

        colDocNumber.setCellValueFactory(new PropertyValueFactory<>("docNumber"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));

        // Status con Badges en español
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }

                Label badge = new Label(status.toUpperCase());
                badge.getStyleClass().add("badge");
                badge.getStyleClass().add(switch (status.toUpperCase()) {
                    case "ACTIVE", "ACTIVO" -> "badge-success";
                    case "INACTIVE", "INACTIVO" -> "badge-secondary";
                    default -> "badge-info";
                });

                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap);
                setText(null);
            }
        });

        colCreatedAt.setCellValueFactory(cell -> {
            var dt = cell.getValue().getCreatedAt();
            return new SimpleStringProperty(dt == null ? "—" : dt.format(DATE_FMT));
        });
        colCreatedAt.setStyle("-fx-alignment: CENTER;");

        // CONFIGURACIÓN DINÁMICA DE ACCIONES (CORREGIDA)
        // CONFIGURACIÓN DE ACCIONES (Guiado bajo la lógica limpia de Préstamos)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button();
            private final Button btnEdit = new Button();
            private final HBox box = new HBox(8);

            {
                box.setAlignment(Pos.CENTER);
                btnView.setMnemonicParsing(false);
                btnEdit.setMnemonicParsing(false);
                btnView.setTooltip(new Tooltip("Ver ficha del cliente"));
                btnEdit.setTooltip(new Tooltip("Editar cliente"));

                // Estilos limpios heredados de tus botones (como en tu controlador de préstamos)
                btnView.getStyleClass().addAll("btn-action", "btn-action-view");
                btnEdit.getStyleClass().addAll("btn-action", "btn-action-edit");

                // Configuramos el ícono del OJO usando la API nativa de color (Solución de Préstamos)
                FontIcon iconView = new FontIcon("fas-eye");
                iconView.setIconSize(13);
                iconView.setIconColor(javafx.scene.paint.Color.valueOf("#25467E"));
                btnView.setGraphic(iconView);

                // Configuramos el ícono del LÁPIZ usando la API nativa de color
                FontIcon iconEdit = new FontIcon("fas-pen");
                iconEdit.setIconSize(13);
                iconEdit.setIconColor(javafx.scene.paint.Color.valueOf("#198754"));
                btnEdit.setGraphic(iconEdit);

                // Agregamos ambos botones al contenedor
                box.getChildren().addAll(btnView, btnEdit);

                // Acciones de eventos limpias
                btnView.setOnAction(e -> {
                    Customer customer = getTableRow().getItem();
                    if (customer != null) handleViewCustomer(customer);
                });

                btnEdit.setOnAction(e -> {
                    Customer customer = getTableRow().getItem();
                    if (customer != null) handleEditCustomer(customer);
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

    /* ═══════════════════════════════════════════════════════════════════
       PAGINACIÓN
       ═══════════════════════════════════════════════════════════════════ */

    private void setupPagination() {
        cmbPageSize.setItems(FXCollections.observableArrayList(10, 15, 25, 50, 100));
        cmbPageSize.setValue(DEFAULT_PAGE_SIZE);

        cmbPageSize.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                pageSize    = n;
                currentPage = 0;
                refreshPagination();
            }
        });

        pagination.currentPageIndexProperty().addListener((obs, o, n) -> {
            currentPage = n.intValue();
            renderCurrentPage();
        });
    }

    private void refreshPagination() {
        int total = filteredList.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        pagination.setPageCount(pages);
        if (currentPage >= pages) currentPage = pages - 1;
        if (currentPage < 0) currentPage = 0;
        pagination.setCurrentPageIndex(currentPage);
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        int total = filteredList.size();
        int from  = currentPage * pageSize;
        int to    = Math.min(from + pageSize, total);

        currentPageList.setAll(filteredList.subList(from, to));

        lblPageInfo.setText(total == 0
                ? "Sin resultados"
                : "Mostrando %d – %d de %d registros".formatted(from + 1, to, total)
        );
    }

    /* ═══════════════════════════════════════════════════════════════════
       CARGA DE DATOS
       ═══════════════════════════════════════════════════════════════════ */

    private void loadData() {
        try {
            List<Customer> customers = customerDAO.findAll();
            masterList.setAll(customers);
            applyFilter();
            log.info("Clientes cargados: {}", customers.size());
        } catch (Exception e) {
            log.error("Error al cargar clientes", e);
            AlertHelper.showError("Error de Datos", "No se pudieron cargar los clientes", e.getMessage());
        }
    }

    private void refreshStats() {
        try {
            lblTotalCustomers.setText(String.valueOf(customerDAO.countAll()));
            lblActiveCustomers.setText(String.valueOf(customerDAO.countByStatus("ACTIVE")));
            lblInactiveCustomers.setText(String.valueOf(customerDAO.countByStatus("INACTIVE")));
            lblWithLoans.setText(String.valueOf(customerDAO.countWithActiveLoans()));
        } catch (Exception e) {
            log.warn("No se pudieron actualizar las estadísticas: {}", e.getMessage());
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       MANEJADORES DE EVENTOS
       ═══════════════════════════════════════════════════════════════════ */

    @FXML
    private void handleNewCustomer() {
        openCustomerModal(null);
    }

    @FXML
    private void handleRefresh() {
        txtSearch.clear();
        cmbStatusFilter.setValue("Todos");
        loadData();
        refreshStats();
    }

    private void handleEditCustomer(Customer customer) {
        openCustomerModal(customer);
    }

    private void handleViewCustomer(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerDetailView.fxml"));
            Parent root = loader.load();

            CustomerDetailController detailCtrl = loader.getController();
            detailCtrl.setCustomer(customer);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(tableCustomers.getScene().getWindow());
            modal.setTitle("Ficha de Cliente — " + customer.getFullName());
            modal.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            modal.setScene(scene);
            modal.showAndWait();

        } catch (Exception e) {
            log.error("Error al abrir la ficha de detalle del cliente", e);
            AlertHelper.showError("Error de UI", "No se pudo abrir el detalle del cliente", e.getMessage());
        }
    }

    private void openCustomerModal(Customer customer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CustomerForm.fxml"));
            Parent root = loader.load();

            CustomerFormController formCtrl = loader.getController();

            Runnable onSavedCallback = () -> {
                loadData();
                refreshStats();
                AlertHelper.showToast(
                        tableCustomers.getScene(),
                        customer == null ? "¡Cliente creado con éxito!" : "¡Cliente actualizado con éxito!",
                        AlertHelper.ToastType.SUCCESS
                );
            };

            formCtrl.setOnSavedCallback(onSavedCallback);
            if (customer != null) formCtrl.setCustomer(customer);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initOwner(tableCustomers.getScene().getWindow());
            modal.setTitle(customer == null ? "Nuevo Cliente" : "Editar Cliente — " + customer.getFullName());
            modal.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            modal.setScene(scene);
            modal.showAndWait();

        } catch (Exception e) {
            log.error("Error al abrir el modal de cliente", e);
            AlertHelper.showError("Error de UI", "No se pudo abrir el formulario", e.getMessage());
        }
    }
}