package com.edj.developer.apploans.model;

public class LoanReceipt {
    private int id;
    private double amount;
    private String paymentDate;
    private String notes;

    public LoanReceipt(int id, double amount, String paymentDate, String notes) {
        this.id = id;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.notes = notes;
    }

    public int getId() { return id; }
    public double getAmount() { return amount; }
    public String getPaymentDate() { return paymentDate; }
    public String getNotes() { return notes; }
}