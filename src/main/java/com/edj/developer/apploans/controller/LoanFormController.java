package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.*;
import com.edj.developer.apploans.dao.impl.*;
import com.edj.developer.apploans.model.*;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class LoanFormController {

    @FXML private ComboBox<Customer> cmbCustomer;
    @FXML private ComboBox<Double> cmbAmount;
    @FXML private ComboBox<String> cmbFrequency;
    @FXML private ComboBox<Integer> cmbInstallments;
    @FXML private ComboBox<Double> cmbInterest;
    @FXML private Label lblTotalAmount, lblInstallmentSummary, lblInterestEarned;
    @FXML private Button btnCreateLoan;

    private final LoanDAO loanDAO = new LoanDAOImpl();
    private final CustomerDAO customerDAO = new CustomerDAOImpl();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Almacén dinámico de frecuencias leídas desde la Base de Datos
    private Map<String, Integer> frequencyMap = new HashMap<>();

    @FXML
    public void initialize() {
        initConverters();
        loadData();
        setupListeners();
        resetLabels();
    }

    private void initConverters() {
        cmbCustomer.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) { return (c == null) ? "" : c.getFullName() + " (" + c.getDocNumber() + ")"; }
            @Override public Customer fromString(String s) {
                return cmbCustomer.getItems().stream()
                        .filter(c -> toString(c).equals(s))
                        .findFirst().orElse(null);
            }
        });

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

        cmbAmount.setConverter(doubleConv);
        cmbInterest.setConverter(doubleConv);
        cmbInstallments.setConverter(intConv);
    }

    private void loadData() {
        List<Customer> actives = customerDAO.findAll().stream()
                .filter(c -> "ACTIVO".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());
        cmbCustomer.setItems(FXCollections.observableArrayList(actives));

        cmbAmount.setItems(FXCollections.observableArrayList(loanDAO.findAllSuggestedAmounts()));

        // Cargamos frecuencias dinámicas con sus intervalos desde la base de datos
        frequencyMap = loanDAO.findAllFrequenciesWithIntervals();
        cmbFrequency.setItems(FXCollections.observableArrayList(frequencyMap.keySet()));

        ObservableList<Double> interests = FXCollections.observableArrayList();
        ObservableList<Integer> installments = FXCollections.observableArrayList();

        for (double i = 0; i <= 100; i++) interests.add(i);
        for (int i = 1; i <= 100; i++) installments.add(i); // Empezamos en 1 cuota mínimo

        cmbInterest.setItems(interests);
        cmbInstallments.setItems(installments);
    }

    private void setupListeners() {
        Runnable updateAction = this::calculatePreview;
        cmbAmount.getEditor().textProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbInterest.getEditor().textProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbInstallments.getEditor().textProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbFrequency.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbCustomer.valueProperty().addListener((o, ov, nv) -> updateAction.run());
    }

    private void calculatePreview() {
        try {
            double amount = cmbAmount.getConverter().fromString(cmbAmount.getEditor().getText());
            double interestRate = cmbInterest.getConverter().fromString(cmbInterest.getEditor().getText());
            int installments = cmbInstallments.getConverter().fromString(cmbInstallments.getEditor().getText());

            if (amount <= 0 || installments <= 0) { resetLabels(); return; }

            double interestGain = amount * (interestRate / 100);
            double total = amount + interestGain;

            // Redondeo de cuota para la etiqueta utilizando BigDecimal
            BigDecimal totalBD = BigDecimal.valueOf(total);
            BigDecimal cuotaBD = totalBD.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);

            lblTotalAmount.setText(String.format("$ %.2f", total));
            lblInterestEarned.setText(String.format("$ %.2f", interestGain));
            lblInstallmentSummary.setText(String.format("%d cuotas de $ %.2f", installments, cuotaBD.doubleValue()));
        } catch (Exception e) {
            resetLabels();
        }
    }

    @FXML
    private void handleCreateLoan() {
        if (cmbCustomer.getValue() == null) {
            AlertHelper.showWarning("Validación", "Falta Cliente", "Seleccione un cliente válido de la lista.");
            return;
        }
        if (cmbFrequency.getValue() == null) {
            AlertHelper.showWarning("Validación", "Falta Frecuencia", "Seleccione una frecuencia de pago.");
            return;
        }

        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Confirmar Registro",
                "¿Desea registrar este nuevo crédito?",
                "Cliente: " + cmbCustomer.getValue().getFullName()
        );

        if (result.isPresent() && !result.get().getButtonData().isCancelButton()) {
            if (saveLoan()) {
                AlertHelper.showInfo("Éxito", "Préstamo Creado", "El crédito se registró correctamente.");
                ((Stage) btnCreateLoan.getScene().getWindow()).close();
            }
        }
    }

    private boolean saveLoan() {
        try {
            Loan loan = new Loan();
            loan.setCustomerId(cmbCustomer.getValue().getId());

            double amount = Double.parseDouble(cmbAmount.getEditor().getText().replaceAll("[^\\d.]", ""));
            double interest = Double.parseDouble(cmbInterest.getEditor().getText().replaceAll("[^\\d.]", ""));
            int inst = Integer.parseInt(cmbInstallments.getEditor().getText().replaceAll("[^\\d.]", ""));

            loan.setAmount(amount);
            loan.setInterestRate(interest);
            loan.setInstallments(inst);

            String selectedFreq = cmbFrequency.getValue();
            loan.setFrequency(selectedFreq);

            LocalDate fechaBase = LocalDate.now();
            loan.setStartDate(fechaBase.format(formatter));

            double totalConInteres = amount + (amount * (interest / 100));
            loan.setTotalAmount(totalConInteres);

            // ─── ALGORITMO FINANCIERO CON REDONDEO CONTROLADO ───
            List<LoanPayment> payments = new ArrayList<>();

            BigDecimal totalBD = BigDecimal.valueOf(totalConInteres);
            // Dividimos con redondeo a 2 decimales hacia arriba (HALF_UP o CEILING)
            BigDecimal cuotaMonto = totalBD.divide(BigDecimal.valueOf(inst), 2, RoundingMode.HALF_UP);

            LocalDate fechaCuota = fechaBase;

            for (int i = 1; i <= inst; i++) {
                fechaCuota = calculateNextDate(fechaCuota, selectedFreq);

                // Si es la última cuota, por seguridad matemática ajustamos la diferencia por si sobraron centavos
                double montoFila = cuotaMonto.doubleValue();
                if (i == inst) {
                    BigDecimal acumuladoAnteriores = cuotaMonto.multiply(BigDecimal.valueOf(inst - 1));
                    montoFila = totalBD.subtract(acumuladoAnteriores).doubleValue();
                }

                LoanPayment p = new LoanPayment(i, montoFila, fechaCuota.format(formatter));
                payments.add(p);
            }

            loan.setPayments(payments);
            loan.setStatus("ACTIVE");
            return loanDAO.saveFullLoan(loan);

        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Error", "Datos Inválidos", "Asegúrate de rellenar los campos numéricos.");
            return false;
        }
    }

    /**
     * Calcula la fecha basándose de forma dinámica en los días guardados en DB.
     */
    private LocalDate calculateNextDate(LocalDate cur, String freq) {
        if (freq == null) return cur.plusMonths(1);

        Integer daysInterval = frequencyMap.get(freq.toUpperCase());
        if (daysInterval != null) {
            return cur.plusDays(daysInterval);
        }

        // Fallback por si acaso
        return cur.plusMonths(1);
    }

    private void resetLabels() {
        lblTotalAmount.setText("$ 0.00");
        lblInterestEarned.setText("$ 0.00");
        lblInstallmentSummary.setText("0 cuotas de $ 0.00");
    }
}