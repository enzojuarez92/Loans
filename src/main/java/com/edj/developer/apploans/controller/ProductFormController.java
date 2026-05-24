package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.ProductDAO;
import com.edj.developer.apploans.dao.impl.ProductDAOImpl;
import com.edj.developer.apploans.model.Product;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ProductFormController {

    @FXML private Label lblTitle;
    @FXML private FontIcon iconHeader;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private Spinner<Integer> spnStock;
    @FXML private TextField txtBasePrice;
    @FXML private Button btnSave;

    private final ProductDAO productDAO = new ProductDAOImpl();
    private Product currentProduct;
    private boolean isEditMode = false;
    private boolean saveSuccess = false;

    @FXML
    public void initialize() {
        // Configuramos el Spinner de stock (Min: 0, Max: 9999, Inicial: 0)
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9999, 0);
        spnStock.setValueFactory(valueFactory);

        // Listener avanzado para formatear moneda en vivo sin bucles infinitos
        txtBasePrice.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            // Quitamos todo lo que no sea un número para analizar la base pura
            String cleanString = newValue.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) {
                txtBasePrice.setText("");
                return;
            }

            try {
                // Tomamos el string plano como si los últimos dos dígitos fueran los decimales (centavos)
                double parsed = Double.parseDouble(cleanString) / 100.0;

                // Formateador local para Argentina
                DecimalFormat formatter = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));
                String formatted = formatter.format(parsed);

                // Desactivamos momentáneamente el listener para setear el texto sin disparar recursividad
                Platform.runLater(() -> {
                    txtBasePrice.setText(formatted);
                    txtBasePrice.positionCaret(formatted.length()); // Deja el cursor siempre al final
                });

            } catch (NumberFormatException e) {
                txtBasePrice.setText(oldValue);
            }
        });
    }

    /**
     * Setea el producto a editar. Si viene nulo, el formulario se configura en "Modo Creación".
     */
    public void setProduct(Product product) {
        if (product != null) {
            this.currentProduct = product;
            this.isEditMode = true;

            lblTitle.setText("Editar Producto");
            iconHeader.setIconLiteral("fas-edit");
            btnSave.setText("Actualizar");

            txtName.setText(product.getName());
            txtDescription.setText(product.getDescription());
            spnStock.getValueFactory().setValue(product.getStock());

            // Pasamos el double de la BD a formato "centavos" para activar el listener correctamente en vivo
            DecimalFormat formatter = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("es", "AR")));
            txtBasePrice.setText(formatter.format(product.getBasePrice()));
        } else {
            this.currentProduct = new Product();
            this.isEditMode = false;
        }
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        currentProduct.setName(txtName.getText().trim());
        currentProduct.setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : "");
        currentProduct.setStock(spnStock.getValue());

        // Parseo seguro de la máscara regionalizada antes de guardar
        currentProduct.setBasePrice(parseCurrency(txtBasePrice.getText().trim()));

        boolean success;
        if (isEditMode) {
            success = productDAO.update(currentProduct);
        } else {
            success = productDAO.insert(currentProduct);
        }

        if (success) {
            saveSuccess = true;
            closeStage();
        } else {
            AlertHelper.showError("Error de Persistencia", "No se pudo guardar", "Ocurrió un problema técnico en la base de datos.");
        }
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private boolean validateForm() {
        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            AlertHelper.showWarning("Validación", "Campo Requerido", "Por favor introduzca el nombre del producto.");
            txtName.requestFocus();
            return false;
        }
        if (txtBasePrice.getText() == null || txtBasePrice.getText().trim().isEmpty()) {
            AlertHelper.showWarning("Validación", "Campo Requerido", "Debe asignarle un precio base inicial.");
            txtBasePrice.requestFocus();
            return false;
        }
        try {
            double price = parseCurrency(txtBasePrice.getText().trim());
            if (price < 0) {
                AlertHelper.showWarning("Validación", "Precio Inválido", "El precio base no puede ser negativo.");
                txtBasePrice.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            AlertHelper.showWarning("Validación", "Precio Inválido", "El formato de precio no es correcto.");
            txtBasePrice.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Convierte el texto con formato de moneda argentino ("1.500,50") a un double primitivo analizable por Java (1500.50).
     */
    private double parseCurrency(String formattedPrice) {
        String rawPrice = formattedPrice
                .replace(".", "")   // Quitamos los puntos separadores de miles
                .replace(",", "."); // Reemplazamos la coma decimal por el punto de Java
        return Double.parseDouble(rawPrice);
    }

    public boolean isSaveSuccess() { return saveSuccess; }

    private void closeStage() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }
}