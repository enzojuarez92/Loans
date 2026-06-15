package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.CustomerDAO;
import com.edj.developer.apploans.dao.ProductDAO;
import com.edj.developer.apploans.dao.SaleDAO;
import com.edj.developer.apploans.dao.impl.CustomerDAOImpl;
import com.edj.developer.apploans.dao.impl.ProductDAOImpl;
import com.edj.developer.apploans.dao.impl.SaleDAOImpl;
import com.edj.developer.apploans.model.Customer;
import com.edj.developer.apploans.model.Product;
import com.edj.developer.apploans.model.Sale;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SaleFormController {

    private static final Logger log = LoggerFactory.getLogger(SaleFormController.class);

    // ─── Componentes FXML exactos de tu SaleFormView ───────────────────────
    @FXML private ComboBox<Customer> cmbCustomer;
    @FXML private DatePicker dpStartDate;
    @FXML private ComboBox<Product> cmbProduct;
    @FXML private ComboBox<String> cmbFrequency;
    @FXML private ComboBox<Integer> cmbInstallments;
    @FXML private ComboBox<Double> cmbInterest;

    @FXML private Label lblTotalAmount;         // El grande de "TOTAL LIQUIDACIÓN"
    @FXML private Label lblProductPrice;        // Precio del Artículo
    @FXML private Label lblInstallmentSummary;   // Distribución (cuotas)
    @FXML private Label lblInterestEarned;      // Recargo Cargado
    @FXML private Button btnCreateSale;         // Botón Confirmar

    private final SaleDAO saleDAO = new SaleDAOImpl();
    private final CustomerDAO customerDAO = new CustomerDAOImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();

    @FXML
    public void initialize() {
        dpStartDate.setValue(LocalDate.now());
        initConverters();
        loadData();
        setupListeners();
        resetLabels();
    }

    /**
     * Sincroniza cómo se ven los objetos complejos en los ComboBox para que no queden vacíos
     */
    private void initConverters() {
        cmbCustomer.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) { return (c == null) ? "" : c.getFullName() + " (" + c.getDocNumber() + ")"; }
            @Override public Customer fromString(String s) {
                return cmbCustomer.getItems().stream().filter(c -> toString(c).equals(s)).findFirst().orElse(null);
            }
        });

        cmbProduct.setConverter(new StringConverter<>() {
            @Override public String toString(Product p) { return (p == null) ? "" : p.getName() + " [Stock: " + p.getStock() + "]"; }
            @Override public Product fromString(String s) {
                return cmbProduct.getItems().stream().filter(p -> toString(p).equals(s)).findFirst().orElse(null);
            }
        });

        // Convertidores para los ComboBox editables (igual que hacés en préstamos)
        StringConverter<Double> doubleConv = new StringConverter<>() {
            @Override public String toString(Double n) { return n == null ? "" : String.valueOf(n); }
            @Override public Double fromString(String s) {
                if (s == null || s.isEmpty()) return 0.0;
                try { return Double.parseDouble(s.replaceAll("[^\\d.]", "")); } catch (Exception e) { return 0.0; }
            }
        };

        StringConverter<Integer> intConv = new StringConverter<>() {
            @Override public String toString(Integer n) { return n == null ? "" : String.valueOf(n); }
            @Override public Integer fromString(String s) {
                if (s == null || s.isEmpty()) return 0;
                try { return Integer.parseInt(s.replaceAll("[^\\d]", "")); } catch (Exception e) { return 0; }
            }
        };

        cmbInterest.setConverter(doubleConv);
        cmbInstallments.setConverter(intConv);
    }

    private void loadData() {
        // 1. Clientes activos
        List<Customer> actives = customerDAO.findAll().stream()
                .filter(c -> "ACTIVO".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());
        cmbCustomer.setItems(FXCollections.observableArrayList(actives));

        // 2. SOLUCIÓN PRODUCTOS: Usamos tu findAllPaged trayendo un límite alto para el combo de los que tengan stock
        List<Product> products = productDAO.findAllPaged("", "CON STOCK", 500, 0);
        cmbProduct.setItems(FXCollections.observableArrayList(products));

        // 3. Frecuencias
        cmbFrequency.setItems(FXCollections.observableArrayList("DIARIO", "SEMANAL", "QUINCENAL", "MENSUAL"));
        cmbFrequency.setValue("DIARIO");

        // 4. Llenar opciones de los combos de interés y cuotas
        ObservableList<Double> interests = FXCollections.observableArrayList();
        ObservableList<Integer> installments = FXCollections.observableArrayList();
        for (double i = 0; i <= 100; i++) interests.add(i);
        for (int i = 1; i <= 100; i++) installments.add(i);
        cmbInterest.setItems(interests);
        cmbInstallments.setItems(installments);
    }

    private void setupListeners() {
        Runnable updateAction = this::calculatePreview;
        cmbProduct.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbFrequency.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbCustomer.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        dpStartDate.valueProperty().addListener((o, ov, nv) -> updateAction.run());

        // Al ser editables, escuchamos los cambios en el editor de texto directo
        cmbInstallments.getEditor().textProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbInterest.getEditor().textProperty().addListener((o, ov, nv) -> updateAction.run());
    }

    private void calculatePreview() {
        try {
            Product prod = cmbProduct.getValue();
            if (prod == null) { resetLabels(); return; }

            double price = prod.getBasePrice(); // Cambiar por prod.getBasePrice() si financiás al costo básico
            double interestRate = cmbInterest.getConverter().fromString(cmbInterest.getEditor().getText());
            int installments = cmbInstallments.getConverter().fromString(cmbInstallments.getEditor().getText());

            if (price <= 0 || installments <= 0) { resetLabels(); return; }

            double interestGain = price * (interestRate / 100.0);
            double total = price + interestGain;

            BigDecimal totalBD = BigDecimal.valueOf(total);
            BigDecimal cuotaBD = totalBD.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);

            lblProductPrice.setText(String.format("$ %,.2f", price));
            lblInterestEarned.setText(String.format("$ %,.2f", interestGain));
            lblTotalAmount.setText(String.format("$ %,.2f", total));
            lblInstallmentSummary.setText(String.format("%d cuotas de $ %,.2f", installments, cuotaBD.doubleValue()));
        } catch (Exception e) {
            resetLabels();
        }
    }

    @FXML
    private void handleCreateSale() {
        if (cmbCustomer.getValue() == null) {
            AlertHelper.showWarning("Validación", "Falta Cliente", "Seleccione un cliente válido.");
            return;
        }
        if (cmbProduct.getValue() == null) {
            AlertHelper.showWarning("Validación", "Falta Producto", "Seleccione un artículo para realizar la venta.");
            return;
        }
        if (dpStartDate.getValue() == null) {
            AlertHelper.showWarning("Validación", "Falta Fecha", "Indique la fecha de inicio.");
            return;
        }

        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Confirmar Registro",
                "¿Desea registrar esta venta financiada?",
                "Cliente: " + cmbCustomer.getValue().getFullName() + "\nProducto: " + cmbProduct.getValue().getName()
        );

        if (result.isPresent() && !result.get().getButtonData().isCancelButton()) {
            if (saveSale()) {
                AlertHelper.showInfo("Éxito", "Venta Creada", "La venta y el plan de cuotas se guardaron con éxito.");
                // Cierra el modal flotante de forma limpia
                ((Stage) btnCreateSale.getScene().getWindow()).close();
            }
        }
    }

    private boolean saveSale() {
        try {
            Sale sale = new Sale();
            Product selectedProd = cmbProduct.getValue();

            sale.setCustomerId(cmbCustomer.getValue().getId());
            sale.setProductId(selectedProd.getId());
            sale.setStartDate(dpStartDate.getValue().toString());
            sale.setFrequency(cmbFrequency.getValue());

            double price = selectedProd.getBasePrice();
            double interestRate = Double.parseDouble(cmbInterest.getEditor().getText().replaceAll("[^\\d.]", ""));
            int installments = Integer.parseInt(cmbInstallments.getEditor().getText().replaceAll("[^\\d.]", ""));

            sale.setSellingPrice(price);
            sale.setInterestRate(interestRate);
            sale.setInstallments(installments);

            double totalAmount = price + (price * (interestRate / 100.0));
            sale.setTotalAmount(totalAmount);
            sale.setStatus("ACTIVE");

            // Mapeo de días según frecuencia para el plan de cuotas del SaleDAOImpl
            int intervalDays = switch (cmbFrequency.getValue().toUpperCase()) {
                case "DIARIO" -> 1;
                case "SEMANAL" -> 7;
                case "QUINCENAL" -> 15;
                default -> 30; // MENSUAL
            };

            return saleDAO.saveWithTransaction(sale, intervalDays);
        } catch (Exception e) {
            log.error("Error al procesar el guardado de la venta", e);
            AlertHelper.showError("Error", "Datos Inválidos", "Asegúrese de que los campos numéricos estén correctos.");
            return false;
        }
    }

    @FXML
    private void handleCancel() {
        // Cierra la ventana modal usando cualquier nodo seguro que esté en el FXML
        Stage stage = (Stage) cmbCustomer.getScene().getWindow();
        stage.close();
    }

    private void resetLabels() {
        lblTotalAmount.setText("0,00");
        lblProductPrice.setText("0,00");
        lblInterestEarned.setText("0,00");
        lblInstallmentSummary.setText("0 cuotas de $ 0,00");
    }
}