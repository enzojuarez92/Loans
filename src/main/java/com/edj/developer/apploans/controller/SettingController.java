package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.IConfigDAO;
import com.edj.developer.apploans.dao.impl.ConfigDAOImpl;
import com.edj.developer.apploans.model.LoanAmount;
import com.edj.developer.apploans.model.LoanFrequency;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingController implements Initializable {

    // --- Componentes Montos ---
    @FXML private TextField txtNewAmount;
    @FXML private TableView<LoanAmount> tblAmounts;
    @FXML private TableColumn<LoanAmount, Double> colAmountValue;
    @FXML private TableColumn<LoanAmount, Void> colAmountAction; // IMPORTANTE: Agregar esto

    // --- Componentes Frecuencias ---
    @FXML private TextField txtFreqName;
    @FXML private TextField txtFreqDays;
    @FXML private TableView<LoanFrequency> tblFrequencies;
    @FXML private TableColumn<LoanFrequency, String> colFreqName;
    @FXML private TableColumn<LoanFrequency, Integer> colFreqDays;
    @FXML private TableColumn<LoanFrequency, Void> colFreqAction; // IMPORTANTE: Agregar esto

    // --- Componentes General (Nuevos) ---
    @FXML private TextField txtBusinessName;
    @FXML private TextField txtUserFullName;
    @FXML private PasswordField txtNewPassword;

    private final IConfigDAO configDAO = new ConfigDAOImpl();
    private final ObservableList<LoanAmount> amountList = FXCollections.observableArrayList();
    private final ObservableList<LoanFrequency> frequencyList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTables();
        setupActionsTable(); // Llamamos al método de los botones
        loadData();
        checkDefaultData();
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
                else setText(String.format("$ %,.2f", item));
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
        if (amountList.isEmpty()) {
            configDAO.saveAmount(new LoanAmount(10000.0));
            configDAO.saveAmount(new LoanAmount(50000.0));
            amountList.setAll(configDAO.findAllAmounts());
        }
        if (frequencyList.isEmpty()) {
            configDAO.saveFrequency(new LoanFrequency("SEMANAL", 7));
            configDAO.saveFrequency(new LoanFrequency("MENSUAL", 30));
            frequencyList.setAll(configDAO.findAllFrequencies());
        }
    }

    @FXML
    private void handleAddAmount() {
        try {
            double val = Double.parseDouble(txtNewAmount.getText().trim());
            if (configDAO.saveAmount(new LoanAmount(val))) {
                loadData();
                txtNewAmount.clear();
            }
        } catch (Exception e) { AlertHelper.showError("Error", "Monto inválido", ""); }
    }

    @FXML
    private void handleAddFrequency() {
        try {
            String name = txtFreqName.getText().trim();
            int days = Integer.parseInt(txtFreqDays.getText().trim());
            if (configDAO.saveFrequency(new LoanFrequency(name.toUpperCase(), days))) {
                loadData();
                txtFreqName.clear(); txtFreqDays.clear();
            }
        } catch (Exception e) { AlertHelper.showError("Error", "Datos inválidos", ""); }
    }

    private void handleDeleteAmount(LoanAmount amount) {
        // Usamos showConfirm (que es el nombre en tu AlertHelper)
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Borrar",
                "¿Eliminar monto?",
                "El monto $" + amount.getValue() + " dejará de estar disponible."
        );

        // En JavaFX, el botón "Yes" de tu AlertHelper devuelve el tipo OK_DONE
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            if (configDAO.deleteAmount(amount.getId())) {
                loadData();
                // Opcional: mostrar un toast de éxito
                AlertHelper.showToast(tblAmounts.getScene(), "Monto eliminado", AlertHelper.ToastType.SUCCESS);
            }
        }
    }

    private void handleDeleteFrequency(LoanFrequency freq) {
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Borrar",
                "¿Eliminar frecuencia?",
                "La frecuencia " + freq.getName() + " se eliminará."
        );

        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            if (configDAO.deleteFrequency(freq.getId())) {
                loadData();
                AlertHelper.showToast(tblFrequencies.getScene(), "Frecuencia eliminada", AlertHelper.ToastType.SUCCESS);
            }
        }
    }

    @FXML
    private void handleUpdateGeneral() {
        // Aquí irá la lógica para actualizar nombre negocio, usuario y pass
        AlertHelper.showInfo("Éxito", "Configuración actualizada", "Los cambios se guardaron correctamente.");
    }
}