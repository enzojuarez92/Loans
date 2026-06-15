package com.edj.developer.apploans.model;

public class SalePayment {
    private int id;
    private int saleId;
    private int installmentNumber;
    private double amount;
    private double paidAmount;
    private String dueDate;
    private String status;

    public SalePayment() {}

    public SalePayment(int id, int saleId, int installmentNumber, double amount, double paidAmount, String dueDate, String status) {
        this.id = id;
        this.saleId = saleId;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.paidAmount = paidAmount;
        this.dueDate = dueDate;
        this.status = status;
    }

    // Getters y Setters que te pide el Controller a gritos
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public int getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(int installmentNumber) { this.installmentNumber = installmentNumber; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}