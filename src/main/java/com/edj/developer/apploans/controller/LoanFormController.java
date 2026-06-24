package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.*;
import com.edj.developer.apploans.dao.impl.*;
import com.edj.developer.apploans.model.*;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class LoanFormController {

    @FXML private ComboBox<Customer> cmbCustomer;
    @FXML private DatePicker dpStartDate;
    @FXML private ComboBox<Double> cmbAmount;
    @FXML private ComboBox<String> cmbFrequency;
    @FXML private ComboBox<Integer> cmbInstallments;
    @FXML private TextField txtInterest;
    @FXML private Label lblTotalAmount, lblInstallmentSummary, lblInterestEarned;
    @FXML private Button btnCreateLoan;

    private final LoanDAO loanDAO = new LoanDAOImpl();
    private final CustomerDAO customerDAO = new CustomerDAOImpl();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Listas globales para el control de los filtros
    private final ObservableList<Customer> allActiveCustomers = FXCollections.observableArrayList();
    private final ObservableList<Double> allSuggestedAmounts = FXCollections.observableArrayList();

    private Map<String, Integer> frequencyMap = new HashMap<>();
    private DecimalFormat currencyFormatter;

    @FXML
    public void initialize() {
        dpStartDate.setValue(LocalDate.now());
        initCurrencyFormatter();
        initConverters();
        loadData();
        setupListeners();
        resetLabels();
    }

    private void initCurrencyFormatter() {
        // Configuramos formato local: Puntos para miles, coma para decimales
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        currencyFormatter = new DecimalFormat("$ #,##0.00", symbols);
    }

    private void initConverters() {
        // Converter de Clientes
        cmbCustomer.setConverter(new StringConverter<>() {
            @Override public String toString(Customer c) { return (c == null) ? "" : c.getFullName() + " (" + c.getDocNumber() + ")"; }
            @Override public Customer fromString(String s) {
                return allActiveCustomers.stream()
                        .filter(c -> toString(c).equals(s))
                        .findFirst().orElse(null);
            }
        });

        // 💡 NUEVO Converter de Montos con formato estético monetario ($ 100.000,00)
        cmbAmount.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double n) {
                return (n == null || n == 0.0) ? "" : currencyFormatter.format(n);
            }
            @Override
            public Double fromString(String s) {
                if (s == null || s.isEmpty()) return 0.0;
                try {
                    // Limpiamos el signo $, los puntos de miles y cambiamos la coma decimal por un punto de casteo
                    String clean = s.replaceAll("[^\\d,]", "").replace(",", ".");
                    return Double.parseDouble(clean);
                } catch (Exception e) {
                    return 0.0;
                }
            }
        });
    }

    private void loadData() {
        // 1. Clientes
        List<Customer> actives = customerDAO.findAll().stream()
                .filter(c -> "ACTIVO".equalsIgnoreCase(c.getStatus()))
                .collect(Collectors.toList());
        allActiveCustomers.setAll(actives);
        cmbCustomer.setItems(allActiveCustomers);
        setupCustomerFilter();

        // 2. Montos Sugeridos de la DB
        allSuggestedAmounts.setAll(loanDAO.findAllSuggestedAmounts());
        cmbAmount.setItems(allSuggestedAmounts);
        setupAmountFilter(); // 💡 Activamos el filtro en tiempo real para los montos

        // 3. Frecuencias
        frequencyMap = loanDAO.findAllFrequenciesWithIntervals();
        cmbFrequency.setItems(FXCollections.observableArrayList(frequencyMap.keySet()));

        // 4. Intereses y Cuotas
        ObservableList<Integer> installments = FXCollections.observableArrayList();
        for (int i = 1; i <= 100; i++) installments.add(i);
        cmbInstallments.setItems(installments);
    }

    private void setupCustomerFilter() {
        javafx.collections.transformation.FilteredList<Customer> filteredCustomers =
                new javafx.collections.transformation.FilteredList<>(allActiveCustomers, p -> true);

        cmbCustomer.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                Customer selected = cmbCustomer.getSelectionModel().getSelectedItem();
                if (selected != null && cmbCustomer.getConverter().toString(selected).equals(newValue)) return;

                if (newValue == null || newValue.trim().isEmpty()) {
                    filteredCustomers.setPredicate(p -> true);
                } else {
                    String filterText = newValue.toLowerCase().trim();
                    filteredCustomers.setPredicate(customer -> {
                        String fullName = (customer.getFullName() != null) ? customer.getFullName().toLowerCase() : "";
                        String docNumber = (customer.getDocNumber() != null) ? customer.getDocNumber().toLowerCase() : "";
                        return fullName.contains(filterText) || docNumber.contains(filterText);
                    });
                }
                cmbCustomer.setItems(filteredCustomers);
                if (!newValue.isEmpty() && !filteredCustomers.isEmpty()) cmbCustomer.show();
                else cmbCustomer.hide();
            });
        });

        cmbCustomer.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = cmbCustomer.getEditor().getText();
                Customer matchingCustomer = allActiveCustomers.stream()
                        .filter(c -> cmbCustomer.getConverter().toString(c).equals(text))
                        .findFirst().orElse(null);

                if (matchingCustomer != null) {
                    cmbCustomer.getSelectionModel().select(matchingCustomer);
                } else {
                    if (cmbCustomer.getSelectionModel().getSelectedItem() == null) cmbCustomer.getEditor().clear();
                }
                cmbCustomer.setItems(allActiveCustomers);
            }
        });
    }

    // 💡 NUEVO: Motor de filtrado y autopropuesta para los Montos del Capital
    private void setupAmountFilter() {
        javafx.collections.transformation.FilteredList<Double> filteredAmounts =
                new javafx.collections.transformation.FilteredList<>(allSuggestedAmounts, p -> true);

        cmbAmount.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                Double selected = cmbAmount.getSelectionModel().getSelectedItem();
                if (selected != null && cmbAmount.getConverter().toString(selected).equals(newValue)) return;

                if (newValue == null || newValue.trim().isEmpty()) {
                    filteredAmounts.setPredicate(p -> true);
                } else {
                    // Extraemos solo los dígitos que va metiendo el usuario para comparar limpiamente
                    String cleanDigits = newValue.replaceAll("[^\\d]", "");
                    filteredAmounts.setPredicate(amount -> {
                        String amtStr = String.valueOf(amount.intValue());
                        return amtStr.contains(cleanDigits);
                    });
                }
                cmbAmount.setItems(filteredAmounts);
                if (!newValue.isEmpty() && !filteredAmounts.isEmpty()) cmbAmount.show();
                else cmbAmount.hide();
            });
        });

        // 💡 VALIDACIÓN CRÍTICA AL PERDER EL ENFOQUE: No permite registrar si inventan un monto
        cmbAmount.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String text = cmbAmount.getEditor().getText();
                Double inputAmount = cmbAmount.getConverter().fromString(text);

                // Buscamos si el monto tipeado coincide exactamente con uno permitido en la lista
                Double matchingAmount = allSuggestedAmounts.stream()
                        .filter(a -> Math.abs(a - inputAmount) < 0.01)
                        .findFirst().orElse(null);

                if (matchingAmount != null) {
                    cmbAmount.getSelectionModel().select(matchingAmount);
                } else {
                    // 🛑 Si el monto NO está en tu lista permitida, se borra el campo para impedir el fraude
                    cmbAmount.getSelectionModel().clearSelection();
                    cmbAmount.getEditor().clear();
                    resetLabels();
                }
                cmbAmount.setItems(allSuggestedAmounts);
            }
        });
    }

    private void setupListeners() {
        Runnable updateAction = this::calculatePreview;
        cmbAmount.getEditor().textProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbInstallments.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbFrequency.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        cmbCustomer.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        dpStartDate.valueProperty().addListener((o, ov, nv) -> updateAction.run());
        txtInterest.textProperty().addListener((o, ov, nv) -> updateAction.run());
    }

    private void calculatePreview() {
        try {
            double amount = cmbAmount.getConverter().fromString(cmbAmount.getEditor().getText());

            if (cmbInstallments.getValue() == null) return;
            int installments = cmbInstallments.getValue();

            // 🚀 NUEVO: Lectura segura del TextField de interés
            String interestText = txtInterest.getText().trim().replace(",", ".");
            double interestRate = interestText.isEmpty() ? 0.0 : Double.parseDouble(interestText);

            if (amount <= 0 || installments <= 0) { resetLabels(); return; }

            double interestGain = amount * (interestRate / 100);
            double total = amount + interestGain;

            BigDecimal totalBD = BigDecimal.valueOf(total);
            BigDecimal cuotaBD = totalBD.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);

            lblTotalAmount.setText(currencyFormatter.format(total));
            lblInterestEarned.setText(currencyFormatter.format(interestGain));
            lblInstallmentSummary.setText(String.format("%d cuotas de %s", installments, currencyFormatter.format(cuotaBD.doubleValue())));
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
        if (cmbAmount.getSelectionModel().getSelectedItem() == null) {
            AlertHelper.showWarning("Validación", "Monto Inválido", "Seleccione un monto de capital autorizado de la lista.");
            return;
        }
        if (dpStartDate.getValue() == null) {
            AlertHelper.showWarning("Validación", "Falta Fecha", "Seleccione la fecha de inicio del crédito.");
            return;
        }
        if (cmbFrequency.getValue() == null) {
            AlertHelper.showWarning("Validación", "Falta Frecuencia", "Seleccione una frecuencia de pago.");
            return;
        }
        if (cmbInstallments.getValue() == null) {
            AlertHelper.showWarning("Validación", "Faltan Cuotas", "Seleccione la cantidad de cuotas.");
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

            double amount = cmbAmount.getValue();
            int inst = cmbInstallments.getValue();

            // 🚀 NUEVO: Lectura segura para la base de datos
            String interestText = txtInterest.getText().trim().replace(",", ".");
            double interest = interestText.isEmpty() ? 0.0 : Double.parseDouble(interestText);

            loan.setAmount(amount);
            loan.setInterestRate(interest);
            loan.setInstallments(inst);

            String selectedFreq = cmbFrequency.getValue();
            loan.setFrequency(selectedFreq);

            LocalDate fechaBase = dpStartDate.getValue();
            loan.setStartDate(fechaBase.format(formatter));

            double totalConInteres = amount + (amount * (interest / 100));
            loan.setTotalAmount(totalConInteres);

            List<LoanPayment> payments = new ArrayList<>();
            BigDecimal totalBD = BigDecimal.valueOf(totalConInteres);
            BigDecimal cuotaMonto = totalBD.divide(BigDecimal.valueOf(inst), 2, RoundingMode.HALF_UP);

            LocalDate fechaCuota = fechaBase;

            for (int i = 1; i <= inst; i++) {
                fechaCuota = calculateNextDate(fechaCuota, selectedFreq);

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
            AlertHelper.showError("Error", "Datos Inválidos", "Ocurrió un error al procesar el guardado.");
            return false;
        }
    }

    private LocalDate calculateNextDate(LocalDate cur, String freq) {
        if (freq == null) return cur.plusMonths(1);
        Integer daysInterval = frequencyMap.get(freq.toUpperCase());
        if (daysInterval != null) {
            return cur.plusDays(daysInterval);
        }
        return cur.plusMonths(1);
    }

    private void resetLabels() {
        lblTotalAmount.setText("$ 0,00");
        lblInterestEarned.setText("$ 0,00");
        lblInstallmentSummary.setText("0 cuotas de $ 0,00");
    }
}