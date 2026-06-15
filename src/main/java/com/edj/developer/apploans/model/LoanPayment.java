package com.edj.developer.apploans.model;

public class LoanPayment {
    private int id;
    private int loanId;
    private int installmentNumber;
    private double amount;
    private double paidAmount;
    private String dueDate;
    private String status;
    private String paymentDate;

    public LoanPayment(int installmentNumber, double amount, String dueDate) {
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
        this.paidAmount = 0.0;
        this.status = "PENDING";
    }

    // Getters y Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(int installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getStatusDisplayName() {
        try {
            return Loan.InstallmentStatus.valueOf(this.status.toUpperCase()).getDisplayName();
        } catch (Exception e) {
            return this.status; // Por si viene nulo o vacío
        }
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }
}