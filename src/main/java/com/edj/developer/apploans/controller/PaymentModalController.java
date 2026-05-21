package com.edj.developer.apploans.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PaymentModalController {
    @FXML private Label lblPending;
    @FXML private TextField txtAmountToPay;

    private double pendingAmount;
    private Double amountResult = null;

    public void setInitialData(double pending) {
        this.pendingAmount = pending;
        lblPending.setText(String.format("$ %.2f", pending));
        txtAmountToPay.setText(String.valueOf(pending));
    }

    @FXML
    private void handleConfirm() {
        try {
            double input = Double.parseDouble(txtAmountToPay.getText());
            if (input <= 0) throw new Exception();
            if (input > pendingAmount) {
                // Si quieres ser estricto y no dejar que paguen de más
                new Alert(Alert.AlertType.WARNING, "El monto no puede superar el saldo pendiente.").show();
                return;
            }
            amountResult = input;
            ((Stage) txtAmountToPay.getScene().getWindow()).close();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Monto inválido").show();
        }
    }

    @FXML private void handleCancel() {
        ((Stage) txtAmountToPay.getScene().getWindow()).close();
    }

    public Double getAmountResult() { return amountResult; }
}