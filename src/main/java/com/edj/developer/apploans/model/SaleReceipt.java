package com.edj.developer.apploans.model;

import javafx.beans.property.*;

public class SaleReceipt {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty paymentDate = new SimpleStringProperty();
    private final DoubleProperty amount = new SimpleDoubleProperty();
    private final StringProperty notes = new SimpleStringProperty();

    // Constructor vacío estándar
    public SaleReceipt() {}

    // Constructor completo para instanciar rápido desde tu base de datos o DAOs
    public SaleReceipt(int id, String paymentDate, double amount, String notes) {
        setId(id);
        setPaymentDate(paymentDate);
        setAmount(amount);
        setNotes(notes);
    }

    // --- PROPIEDADES, GETTERS Y SETTERS ---

    // ID / Número de Recibo
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }
    public IntegerProperty idProperty() { return id; }

    // Fecha de Pago (Maneja el String con fecha u hora que viene de DB)
    public String getPaymentDate() { return paymentDate.get(); }
    public void setPaymentDate(String value) { paymentDate.set(value); }
    public StringProperty paymentDateProperty() { return paymentDate; }

    // Monto Entregado
    public double getAmount() { return amount.get(); }
    public void setAmount(double value) { amount.set(value); }
    public DoubleProperty amountProperty() { return amount; }

    // Notas o Detalles del Cobro
    public String getNotes() { return notes.get(); }
    public void setNotes(String value) { notes.set(value); }
    public StringProperty notesProperty() { return notes; }
}