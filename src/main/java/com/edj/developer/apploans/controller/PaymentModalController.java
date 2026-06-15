package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.util.AlertHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class PaymentModalController {
    @FXML private Label lblPending;
    @FXML private TextField txtAmountToPay;

    private double pendingAmount;
    private Double amountResult = null;

    private DecimalFormat moneyFormatter;

    @FXML
    public void initialize() {
        setupMoneyFormatter();
        setupLiveMaskFormatter();

        txtAmountToPay.setOnAction(event -> handleConfirm());
    }

    private void setupMoneyFormatter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "AR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        moneyFormatter = new DecimalFormat("#,##0.00", symbols);
    }

    public void setInitialData(double pending) {
        this.pendingAmount = pending;
        lblPending.setText(moneyFormatter.format(pending));

        // Se carga con el formato inicial completo
        txtAmountToPay.setText(moneyFormatter.format(pending));

        txtAmountToPay.requestFocus();
        Platform.runLater(() -> txtAmountToPay.selectAll());
    }

    /**
     * Aplica la máscara en tiempo real controlando los cambios del texto
     */
    private void setupLiveMaskFormatter() {
        txtAmountToPay.textProperty().addListener((observable, oldValue, newValue) -> {
            // Evitamos bucles infinitos cuando el formateador cambie el texto
            if (newValue == null || newValue.isEmpty() || newValue.equals(oldValue)) return;

            // 1. Limpiamos el string quedándonos solo con los números puros
            String digits = newValue.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                Platform.runLater(() -> txtAmountToPay.setText(""));
                return;
            }

            // 2. Convertimos los dígitos a centavos (Ej: "60000" pasa a ser 600,00)
            double value = Double.parseDouble(digits) / 100.0;

            // 3. Formateamos usando la máscara comercial de pesos
            String formatted = moneyFormatter.format(value);

            // 4. Actualizamos el campo conservando el cursor al final de forma segura
            Platform.runLater(() -> {
                txtAmountToPay.setText(formatted);
                txtAmountToPay.positionCaret(formatted.length());
            });
        });
    }

    @FXML
    private void handleConfirm() {
        String rawText = txtAmountToPay.getText().trim();
        if (rawText.isEmpty()) {
            AlertHelper.showWarning("Campo vacío", "Falta monto", "Por favor, ingrese un monto a pagar.");
            return;
        }

        double input;
        try {
            // Como el texto siempre está formateado, usamos el moneyFormatter de forma segura
            input = moneyFormatter.parse(rawText).doubleValue();
        } catch (Exception e) {
            AlertHelper.showError("Error de formato", "Número inválido", "El monto ingresado no tiene un formato numérico válido.");
            return;
        }

        if (input <= 0) {
            AlertHelper.showWarning("Monto inválido", "Valor incorrecto", "El monto a pagar debe ser mayor a $ 0.00");
            return;
        }

        if (input > (pendingAmount + 0.01)) {
            AlertHelper.showWarning("Monto excedido", "Supera el saldo",
                    String.format("El monto no puede superar el saldo pendiente ($ %,.2f).", pendingAmount));
            return;
        }

        amountResult = input;
        ((Stage) txtAmountToPay.getScene().getWindow()).close();
    }

    @FXML
    private void handleCancel() {
        amountResult = null;
        ((Stage) txtAmountToPay.getScene().getWindow()).close();
    }

    public Double getAmountResult() { return amountResult; }
}