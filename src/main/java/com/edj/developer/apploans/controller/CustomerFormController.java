package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.CustomerDAO;
import com.edj.developer.apploans.dao.impl.CustomerDAOImpl;
import com.edj.developer.apploans.model.Customer;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * CustomerFormController
 *
 * Controller for managing customer creation and editing, refactored to standard English naming.
 */
public class CustomerFormController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(CustomerFormController.class);

    // ─── Validation Patterns ─────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[\\d\\s\\-().+]{7,20}$");

    private static final Pattern DOC_PATTERN =
            Pattern.compile("^[A-Za-z0-9]{4,20}$");

    private static final int MAX_NOTES_LENGTH = 500;

    // ─── DAO ──────────────────────────────────────────────────────────────
    private final CustomerDAO customerDAO = new CustomerDAOImpl();

    // ─── Internal State ───────────────────────────────────────────────────
    private Customer editingCustomer = null;
    private Runnable onSavedCallback;
    private boolean isEditMode = false;

    // ─── FXML – Header ────────────────────────────────────────────────────
    @FXML private FontIcon headerIcon;
    @FXML private Label    lblModalTitle;
    @FXML private Label    lblModalSubtitle;
    @FXML private Label    lblModeBadge;

    // ─── FXML – Identity Section ─────────────────────────────────────────
    @FXML private TextField  txtDocNumber;
    @FXML private TextField  txtFirstName;
    @FXML private TextField  txtLastName;
    @FXML private DatePicker dpBirthDate;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private Label errDocNumber;
    @FXML private Label errFirstName;
    @FXML private Label errLastName;
    @FXML private Label errBirthDate;
    @FXML private Label errStatus;

    // ─── FXML – Contact Section ──────────────────────────────────────────
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private TextField txtAddress;
    @FXML private TextField txtCity;

    @FXML private Label errPhone;
    @FXML private Label errEmail;
    @FXML private Label errAddress;

    // ─── FXML – Notes & Global Errors ────────────────────────────────────
    @FXML private TextArea txtNotes;
    @FXML private HBox      pnlGlobalError;
    @FXML private Label      lblGlobalError;

    // ─── FXML – Footer ────────────────────────────────────────────────────
    @FXML private Button    btnSave;
    @FXML private Button    btnCancel;
    @FXML private FontIcon btnSaveIcon;

    /* ═══════════════════════════════════════════════════════════════════
       PUBLIC API / CONTRACT
       ═══════════════════════════════════════════════════════════════════ */

    public void setOnSavedCallback(Runnable callback) {
        this.onSavedCallback = callback;
    }

    public void setCustomer(Customer customer) {
        if (customer == null) return;

        this.editingCustomer = customer;
        this.isEditMode      = true;

        // UI update for Edit Mode
        lblModalTitle.setText("Editar Cliente");
        lblModalSubtitle.setText("Modifique los campos y guarde para aplicar los cambios");
        lblModeBadge.setText("EDICIÓN");
        lblModeBadge.getStyleClass().removeAll("badge-primary");
        lblModeBadge.getStyleClass().add("badge-warning");
        headerIcon.setIconLiteral("fas-user-edit");

        populateFields(customer);
    }

    /* ═══════════════════════════════════════════════════════════════════
       INITIALIZATION
       ═══════════════════════════════════════════════════════════════════ */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupStatusCombo();
        setupDatePicker();
        setupDateMask();
        setupRealTimeValidation();
        setupNotesCounter();
    }

    private void setupStatusCombo() {
        cmbStatus.getItems().addAll("ACTIVO", "INACTIVO");
        cmbStatus.setValue("ACTIVO");
    }

    private void setupDatePicker() {
        dpBirthDate.setPromptText("dd/mm/aaaa");
        dpBirthDate.setConverter(new javafx.util.StringConverter<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : fmt.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) return null;
                try { return LocalDate.parse(text, fmt); }
                catch (Exception e) { return null; }
            }
        });

        // Disable future dates
        dpBirthDate.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });
    }

    private void setupDateMask() {
        TextField dateEditor = dpBirthDate.getEditor();
        dateEditor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() < oldValue.length()) {
                return;
            }

            String digitsOnly = newValue.replaceAll("[^\\d]", "");
            if (digitsOnly.length() > 8) {
                digitsOnly = digitsOnly.substring(0, 8);
            }

            StringBuilder maskedText = new StringBuilder();
            for (int i = 0; i < digitsOnly.length(); i++) {
                if (i == 2 || i == 4) {
                    maskedText.append("/");
                }
                maskedText.append(digitsOnly.charAt(i));
            }

            if (!newValue.equals(maskedText.toString())) {
                dateEditor.setText(maskedText.toString());
                dateEditor.positionCaret(maskedText.length());
            }
        });
    }

    private void setupRealTimeValidation() {
        txtDocNumber.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateDocNumber(true);
        });
        txtDocNumber.textProperty().addListener((obs, o, n) -> clearError(errDocNumber, txtDocNumber));

        txtFirstName.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateRequired(txtFirstName, errFirstName, "El nombre es obligatorio");
        });
        txtFirstName.textProperty().addListener((obs, o, n) -> clearError(errFirstName, txtFirstName));

        txtLastName.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateRequired(txtLastName, errLastName, "El apellido es obligatorio");
        });
        txtLastName.textProperty().addListener((obs, o, n) -> clearError(errLastName, txtLastName));

        txtPhone.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validatePhone();
        });
        txtPhone.textProperty().addListener((obs, o, n) -> clearError(errPhone, txtPhone));

        txtEmail.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateEmail();
        });
        txtEmail.textProperty().addListener((obs, o, n) -> clearError(errEmail, txtEmail));

        txtAddress.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) validateAddress();
        });
        txtAddress.textProperty().addListener((obs, o, n) -> clearError(errAddress, txtAddress));

        cmbStatus.valueProperty().addListener((obs, o, n) -> clearError(errStatus, cmbStatus));
    }

    private void setupNotesCounter() {
        txtNotes.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.length() > MAX_NOTES_LENGTH) {
                txtNotes.setText(n.substring(0, MAX_NOTES_LENGTH));
            }
        });
    }

    /* ═══════════════════════════════════════════════════════════════════
       FIELD POPULATION (EDIT MODE)
       ═══════════════════════════════════════════════════════════════════ */

    private void populateFields(Customer c) {
        txtDocNumber.setText(nullSafe(c.getDocNumber()));
        txtFirstName.setText(nullSafe(c.getFirstName()));
        txtLastName.setText(nullSafe(c.getLastName()));
        txtPhone.setText(nullSafe(c.getPhone()));
        txtEmail.setText(nullSafe(c.getEmail()));
        txtAddress.setText(nullSafe(c.getAddress()));
        txtCity.setText(nullSafe(c.getCity()));
        txtNotes.setText(nullSafe(c.getNotes()));

        cmbStatus.setValue(c.getStatus() != null ? c.getStatus() : "ACTIVO");

        if (c.getBirthDate() != null) {
            dpBirthDate.setValue(c.getBirthDate());
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       EVENT HANDLERS
       ═══════════════════════════════════════════════════════════════════ */

    @FXML
    private void handleSave() {
        hideGlobalError();

        List<String> errors = runFullValidation();

        if (!errors.isEmpty()) {
            showGlobalError("Por favor, corrija los %d error(es) antes de guardar.".formatted(errors.size()));
            return;
        }

        Customer customer = buildCustomerFromForm();

        try {
            setBusyState(true);

            if (isEditMode) {
                customer.setId(editingCustomer.getId());
                boolean ok = customerDAO.update(customer);
                if (!ok) throw new RuntimeException("No se pudo actualizar. El cliente podría haber sido eliminado.");
                log.info("Customer updated: {}", customer);
            } else {
                customerDAO.save(customer);
                log.info("Customer created: {}", customer);
            }

            if (onSavedCallback != null) {
                onSavedCallback.run();
            }
            closeModal();

        } catch (Exception e) {
            log.error("Error saving customer", e);
            showGlobalError("Error de base de datos: " + e.getMessage());
        } finally {
            setBusyState(false);
        }
    }

    @FXML
    private void handleCancel() {
        if (hasUnsavedChanges()) {
            Optional<ButtonType> result = AlertHelper.showConfirm(
                    "Descartar cambios",
                    "Tiene cambios sin guardar",
                    "¿Está seguro de que desea cerrar sin guardar?"
            );

            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                closeModal();
            }
        } else {
            closeModal();
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       VALIDATIONS
       ═══════════════════════════════════════════════════════════════════ */

    private List<String> runFullValidation() {
        List<String> errors = new ArrayList<>();

        if (!validateDocNumber(true)) errors.add("El documento es obligatorio.");
        if (!validateRequired(txtFirstName, errFirstName, "El nombre es obligatorio")) errors.add("El nombre es obligatorio.");
        if (!validateRequired(txtLastName, errLastName, "El apellido es obligatorio")) errors.add("El apellido es obligatorio.");
        if (!validateBirthDate()) errors.add("La fecha de nacimiento es obligatoria.");
        if (!validatePhone()) errors.add("El teléfono es obligatorio.");
        if (!validateAddress()) errors.add("La dirección es obligatoria.");
        if (!validateEmail()) errors.add("El correo electrónico es inválido.");

        return errors;
    }

    private boolean validateDocNumber(boolean checkUniqueness) {
        String doc = txtDocNumber.getText().trim();

        if (doc.isEmpty()) {
            return showFieldError(errDocNumber, txtDocNumber, "El documento es obligatorio");
        }

        if (!DOC_PATTERN.matcher(doc).matches()) {
            return showFieldError(errDocNumber, txtDocNumber, "Use solo letras y números (4-20 caracteres)");
        }

        if (checkUniqueness) {
            try {
                var existing = customerDAO.findByDocNumber(doc);
                if (existing.isPresent()) {
                    boolean isSameRecord = isEditMode &&
                            existing.get().getId() == editingCustomer.getId();

                    if (!isSameRecord) {
                        return showFieldError(errDocNumber, txtDocNumber,
                                "El documento «%s» ya está registrado".formatted(doc));
                    }
                }
            } catch (Exception e) {
                log.warn("Could not verify document uniqueness: {}", e.getMessage());
            }
        }

        clearError(errDocNumber, txtDocNumber);
        return true;
    }

    private boolean validateRequired(TextField field, Label errorLabel, String message) {
        if (field.getText() == null || field.getText().isBlank()) {
            return showFieldError(errorLabel, field, message);
        }
        clearError(errorLabel, field);
        return true;
    }

    private boolean validatePhone() {
        String phone = txtPhone.getText().trim();

        if (phone.isBlank()) {
            return showFieldError(errPhone, txtPhone, "El teléfono es obligatorio");
        }

        String digitsOnly = phone.replaceAll("[^\\d]", "");
        if (digitsOnly.length() < 7) {
            return showFieldError(errPhone, txtPhone, "El teléfono debe tener al menos 7 dígitos");
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return showFieldError(errPhone, txtPhone, "Formato inválido. Use ej: 381 444-5566");
        }

        clearError(errPhone, txtPhone);
        return true;
    }

    private boolean validateEmail() {
        String email = txtEmail.getText().trim();

        // Email remains optional
        if (email.isBlank()) {
            clearError(errEmail, txtEmail);
            return true;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return showFieldError(errEmail, txtEmail, "Ingrese un correo válido: usuario@dominio.com");
        }

        clearError(errEmail, txtEmail);
        return true;
    }

    private boolean validateAddress() {
        String address = txtAddress.getText().trim();

        if (address.isBlank()) {
            return showFieldError(errAddress, txtAddress, "La dirección es obligatoria");
        }

        clearError(errAddress, txtAddress);
        return true;
    }

    private boolean validateBirthDate() {
        LocalDate date = dpBirthDate.getValue();

        if (date == null) {
            return showFieldError(errBirthDate, dpBirthDate, "La fecha de nacimiento es obligatoria");
        }

        if (date.isAfter(LocalDate.now())) {
            return showFieldError(errBirthDate, dpBirthDate, "La fecha no puede ser futura");
        }

        if (date.isBefore(LocalDate.now().minusYears(120))) {
            return showFieldError(errBirthDate, dpBirthDate, "La fecha es demasiado antigua");
        }

        clearError(errBirthDate, dpBirthDate);
        return true;
    }

    /* ═══════════════════════════════════════════════════════════════════
       OBJECT BUILDING
       ═══════════════════════════════════════════════════════════════════ */

    private Customer buildCustomerFromForm() {
        Customer c = new Customer();

        c.setDocNumber(txtDocNumber.getText().trim().toUpperCase());
        c.setFirstName(capitalize(txtFirstName.getText().trim()));
        c.setLastName(capitalize(txtLastName.getText().trim()));
        c.setPhone(txtPhone.getText().trim());
        c.setEmail(txtEmail.getText().trim().toLowerCase());
        c.setAddress(txtAddress.getText().trim());
        c.setCity(capitalize(txtCity.getText().trim()));
        c.setStatus(cmbStatus.getValue());
        c.setNotes(txtNotes.getText().trim());
        c.setBirthDate(dpBirthDate.getValue());

        return c;
    }

    /* ═══════════════════════════════════════════════════════════════════
       UI HELPERS
       ═══════════════════════════════════════════════════════════════════ */

    private boolean showFieldError(Label errorLabel, javafx.scene.Node field, String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        field.getStyleClass().removeAll("form-control-error");
        field.getStyleClass().add("form-control-error");

        return false;
    }

    private void clearError(Label errorLabel, javafx.scene.Node field) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        field.getStyleClass().remove("form-control-error");
        hideGlobalError();
    }

    private void showGlobalError(String message) {
        lblGlobalError.setText(message);
        pnlGlobalError.setVisible(true);
        pnlGlobalError.setManaged(true);
    }

    private void hideGlobalError() {
        pnlGlobalError.setVisible(false);
        pnlGlobalError.setManaged(false);
    }

    private void setBusyState(boolean busy) {
        btnSave.setDisable(busy);
        btnCancel.setDisable(busy);
        btnSaveIcon.setIconLiteral(busy ? "fas-spinner" : "fas-save");
        btnSave.setText(busy ? "Guardando..." : "Guardar Cliente");
    }

    private boolean hasUnsavedChanges() {
        if (!isEditMode) {
            return !txtDocNumber.getText().isBlank()
                    || !txtFirstName.getText().isBlank()
                    || !txtLastName.getText().isBlank()
                    || !txtPhone.getText().isBlank()
                    || !txtEmail.getText().isBlank();
        }

        return !txtDocNumber.getText().trim().equalsIgnoreCase(nullSafe(editingCustomer.getDocNumber()))
                || !txtFirstName.getText().trim().equalsIgnoreCase(nullSafe(editingCustomer.getFirstName()))
                || !txtLastName.getText().trim().equalsIgnoreCase(nullSafe(editingCustomer.getLastName()))
                || !txtPhone.getText().trim().equals(nullSafe(editingCustomer.getPhone()))
                || !txtEmail.getText().trim().equals(nullSafe(editingCustomer.getEmail()));
    }

    private void closeModal() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        String[] words = text.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String nullSafe(String s) { return s == null ? "" : s; }
}