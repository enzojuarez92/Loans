package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.dao.IConfigDAO;
import com.edj.developer.apploans.dao.impl.ConfigDAOImpl;
import com.edj.developer.apploans.model.LoanAmount;
import com.edj.developer.apploans.model.LoanFrequency;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingController implements Initializable {

    // --- Componentes Montos ---
    @FXML private TextField txtNewAmount;
    @FXML private TableView<LoanAmount> tblAmounts;
    @FXML private TableColumn<LoanAmount, Double> colAmountValue;
    @FXML private TableColumn<LoanAmount, Void> colAmountAction;

    // --- Componentes Frecuencias ---
    @FXML private TextField txtFreqName;
    @FXML private TextField txtFreqDays;
    @FXML private TableView<LoanFrequency> tblFrequencies;
    @FXML private TableColumn<LoanFrequency, String> colFreqName;
    @FXML private TableColumn<LoanFrequency, Integer> colFreqDays;
    @FXML private TableColumn<LoanFrequency, Void> colFreqAction;

    // --- Componentes General ---
    @FXML private TextField txtBusinessName;
    @FXML private TextField txtUserFullName;
    @FXML private PasswordField txtNewPassword;

    private final IConfigDAO configDAO = new ConfigDAOImpl();
    private final ObservableList<LoanAmount> amountList = FXCollections.observableArrayList();
    private final ObservableList<LoanFrequency> frequencyList = FXCollections.observableArrayList();

    private DecimalFormat currencyFormatter;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initCurrencyFormatter();
        setupCurrencyMask();
        setupTables();
        setupActionsTable();
        checkDefaultData();
        loadData();
        loadGeneralSettings();
    }

    private void initCurrencyFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        currencyFormatter = new DecimalFormat("#,##0.00", symbols);
    }

    /**
     * Máscara en tiempo real: Formatea a medida que el usuario escribe (estilo cajero).
     */
    private void setupCurrencyMask() {
        txtNewAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            String cleanString = newValue.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) {
                txtNewAmount.setText("");
                return;
            }

            try {
                double parsed = Double.parseDouble(cleanString) / 100.0;
                String formatted = currencyFormatter.format(parsed);

                Platform.runLater(() -> {
                    txtNewAmount.setText(formatted);
                    txtNewAmount.positionCaret(formatted.length());
                });
            } catch (Exception e) {
                // Fail-safe silencioso
            }
        });
    }

    private void setupTables() {
        // Montos
        colAmountValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        tblAmounts.setItems(amountList);
        colAmountValue.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText("$ " + currencyFormatter.format(item));
            }
        });

        // Frecuencias
        colFreqName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFreqDays.setCellValueFactory(new PropertyValueFactory<>("daysInterval"));
        tblFrequencies.setItems(frequencyList);
    }

    private void setupActionsTable() {
        // Botones Borrar Montos
        colAmountAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();
            {
                btnDelete.getStyleClass().add("btn-danger-sm");
                FontIcon icon = new FontIcon("fas-trash-alt");
                icon.setIconColor(javafx.scene.paint.Color.WHITE);
                btnDelete.setGraphic(icon);
                btnDelete.setOnAction(event -> handleDeleteAmount(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });

        // Botones Borrar Frecuencias
        colFreqAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnDelete = new Button();
            {
                btnDelete.setDisable(true);
                btnDelete.getStyleClass().add("btn-danger-sm");
                FontIcon icon = new FontIcon("fas-trash-alt");
                icon.setIconColor(javafx.scene.paint.Color.WHITE);
                btnDelete.setGraphic(icon);
                btnDelete.setOnAction(event -> handleDeleteFrequency(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void loadData() {
        amountList.setAll(configDAO.findAllAmounts());
        frequencyList.setAll(configDAO.findAllFrequencies());
    }

    private void checkDefaultData() {
        // Montos limpia desde cero según requerimiento.
        if (configDAO.findAllFrequencies().isEmpty()) {
            configDAO.saveFrequency(new LoanFrequency("DIARIO", 1));
            configDAO.saveFrequency(new LoanFrequency("SEMANAL", 7));
            configDAO.saveFrequency(new LoanFrequency("QUINCENAL", 15));
            configDAO.saveFrequency(new LoanFrequency("MENSUAL", 30));
        }
    }

    /**
     * Carga inicial unificada: levanta marca de 'config' y operador de 'users'
     */
    private void loadGeneralSettings() {
        String configSql = "SELECT business_name FROM config LIMIT 1";
        String userSql = "SELECT full_name FROM users WHERE id = 1";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            // Traer Negocio
            try (ResultSet rs = stmt.executeQuery(configSql)) {
                if (rs.next() && rs.getString("business_name") != null) {
                    txtBusinessName.setText(rs.getString("business_name"));
                } else {
                    txtBusinessName.setText("AppLoans");
                }
            }

            // Traer Operador
            try (ResultSet rs = stmt.executeQuery(userSql)) {
                if (rs.next()) {
                    txtUserFullName.setText(rs.getString("full_name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddAmount() {
        String input = txtNewAmount.getText().trim();
        if (input.isEmpty()) return;

        try {
            Number number = currencyFormatter.parse(input);
            double val = number.doubleValue();

            if (val <= 0) {
                AlertHelper.showWarning("Validación", "Monto Incorrecto", "El monto debe ser mayor a cero.");
                return;
            }

            if (configDAO.saveAmount(new LoanAmount(val))) {
                loadData();
                txtNewAmount.clear();
                AlertHelper.showToast(tblAmounts.getScene(), "Monto agregado con éxito!", AlertHelper.ToastType.SUCCESS);
            }
        } catch (ParseException e) {
            AlertHelper.showError("Error", "Monto inválido", "Asegúrese de ingresar números válidos.");
        }
    }

    @FXML
    private void handleAddFrequency() {
        try {
            String name = txtFreqName.getText().trim();
            String daysStr = txtFreqDays.getText().trim();

            if (name.isEmpty() || daysStr.isEmpty()) {
                AlertHelper.showWarning("Validación", "Campos vacíos", "Por favor complete todos los datos.");
                return;
            }

            int days = Integer.parseInt(daysStr);
            if (configDAO.saveFrequency(new LoanFrequency(name.toUpperCase(), days))) {
                loadData();
                txtFreqName.clear();
                txtFreqDays.clear();
                AlertHelper.showToast(tblFrequencies.getScene(), "Frecuencia creada con éxito!", AlertHelper.ToastType.SUCCESS);
            }
        } catch (NumberFormatException e) {
            AlertHelper.showError("Error", "Días inválidos", "El intervalo de días debe ser un número entero.");
        }
    }

    private void handleDeleteAmount(LoanAmount amount) {
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Borrar",
                "¿Eliminar monto?",
                "El monto $" + currencyFormatter.format(amount.getValue()) + " dejará de estar disponible."
        );

        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            if (configDAO.deleteAmount(amount.getId())) {
                loadData();
                AlertHelper.showToast(tblAmounts.getScene(), "Monto eliminado con éxito!", AlertHelper.ToastType.SUCCESS);
            }
        }
    }

    private void handleDeleteFrequency(LoanFrequency freq) {
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Borrar",
                "¿Eliminar frecuencia?",
                "La frecuencia " + freq.getName() + " se eliminará del sistema."
        );

        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            if (configDAO.deleteFrequency(freq.getId())) {
                loadData();
                AlertHelper.showToast(tblFrequencies.getScene(), "Frecuencia eliminada con éxito!", AlertHelper.ToastType.SUCCESS);
            }
        }
    }

    @FXML
    private void handleUpdateGeneral() {
        String businessName = txtBusinessName.getText().trim();
        String fullName = txtUserFullName.getText().trim();
        String newPass = txtNewPassword.getText().trim();

        if (fullName.isEmpty()) {
            AlertHelper.showWarning("Validación", "Nombre Requerido", "El nombre completo del operador no puede estar vacío.");
            return;
        }
        if (businessName.isEmpty()) businessName = "AppLoans";

        String updateConfigSql = "UPDATE config SET business_name = ?";
        StringBuilder updateUserSql = new StringBuilder("UPDATE users SET full_name = ?");
        boolean updatePass = !newPass.isEmpty();

        if (updatePass) {
            updateUserSql.append(", password = ?");
        }
        updateUserSql.append(" WHERE id = 1");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psConfig = conn.prepareStatement(updateConfigSql);
             PreparedStatement psUser = conn.prepareStatement(updateUserSql.toString())) {

            conn.setAutoCommit(false); // Transacción segura

            try {
                // 1. Guardar en tabla config
                psConfig.setString(1, businessName);
                psConfig.executeUpdate();

                // 2. Guardar en tabla users
                psUser.setString(1, fullName);
                if (updatePass) {
                    psUser.setString(2, newPass);
                }
                psUser.executeUpdate();

                conn.commit();
                txtNewPassword.clear();

                // ─── COMUNICACIÓN EN VIVO DE CONTROLADORES ───
                if (txtBusinessName.getScene() != null && txtBusinessName.getScene().getRoot() != null) {
                    Object controller = txtBusinessName.getScene().getRoot().getUserData();
                    if (controller instanceof MainController) {
                        ((MainController) controller).refreshBusinessAndUserInfo();
                    }
                }

                AlertHelper.showInfo("Éxito", "Configuración actualizada", "Los datos generales se guardaron correctamente.");

            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "No se pudo actualizar", "Ocurrió un problema guardando en la base de datos.");
        }
    }

    // Asegurate de importar estas clases al principio del controlador:
// import java.io.File;
// import java.nio.file.Files;
// import java.nio.file.StandardCopyOption;
// import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
// import com.edj.developer.apploans.util.AlertHelper;

    @FXML
    private void handleCreateBackup() {
        try {
            // 🚀 CORRECCIÓN: Apuntar siempre a la carpeta segura del usuario
            String userHome = System.getProperty("user.home");
            File appDir = new File(userHome, ".apploans");
            File currentDb = new File(appDir, "apploans.db");

            if (!currentDb.exists()) {
                AlertHelper.showError("Error de Backup", "Archivo no encontrado",
                        "No se pudo encontrar la base de datos en: " + currentDb.getAbsolutePath());
                return;
            }

            // 🚀 CORRECCIÓN: Crear la carpeta de backups en el directorio del usuario para evitar bloqueos de Windows
            File backupDir = new File(appDir, "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs(); // mkdirs crea el árbol completo si falta
            }

            // Generar nombre único con fecha
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(formatter);
            String backupFileName = "backup_" + timestamp + ".db";
            File destFile = new File(backupDir, backupFileName);

            // Copiar el archivo físico
            Files.copy(currentDb.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            AlertHelper.showInfo("Respaldo Exitoso", "Copia de Seguridad Creada",
                    "El backup se guardó correctamente en:\n" + destFile.getAbsolutePath());

        } catch (Exception e) {
            AlertHelper.showError("Error de Sistema", "No se pudo crear el Backup",
                    "Ocurrió un problema al intentar duplicar el archivo: " + e.getMessage());
        }
    }

    @FXML
    private void handleRestoreBackup() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Copia de Seguridad (.db)");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Base de datos SQLite (*.db)", "*.db")
            );

            // 🚀 MEJORA: Abrir el explorador directo en la carpeta de backups del usuario para facilitarle la vida
            String userHome = System.getProperty("user.home");
            File appDir = new File(userHome, ".apploans");
            File backupDir = new File(appDir, "backups");
            if (backupDir.exists()) {
                fileChooser.setInitialDirectory(backupDir);
            }

            File selectedBackup = fileChooser.showOpenDialog(txtBusinessName.getScene().getWindow());
            if (selectedBackup == null) return;

            Optional<ButtonType> confirm = AlertHelper.showConfirm(
                    "⚠️ ADVERTENCIA CRÍTICA",
                    "¿Está completamente seguro de restaurar esta base de datos?",
                    "Esta acción reemplazará TODOS los datos actuales por los del backup.\n" +
                            "Se cerrará la aplicación automáticamente al finalizar para aplicar los cambios."
            );

            if (confirm.isPresent() && !confirm.get().getButtonData().isCancelButton()) {
                File targetDb = new File(appDir, "apploans.db");

                // Copiar el backup pisando el archivo real
                Files.copy(selectedBackup.toPath(), targetDb.toPath(), StandardCopyOption.REPLACE_EXISTING);

                AlertHelper.showInfo(
                        "Restauración Completada",
                        "Datos Restaurados con Éxito",
                        "El sistema se cerrará ahora. Volvé a iniciarlo para ver los datos cargados."
                );

                Platform.exit();
                System.exit(0);
            }

        } catch (Exception e) {
            AlertHelper.showError(
                    "Error de Restauración",
                    "No se pudo restaurar el archivo",
                    "Ocurrió un fallo al intentar sobreescribir la base de datos: " + e.getMessage()
            );
        }
    }
}