package com.edj.developer.apploans.model;

import java.util.ArrayList;
import java.util.List;

public class Loan {
    private int id;
    private int customerId;
    private String customerName; // Para mostrar en tablas
    private double amount;
    private double interestRate;
    private double totalAmount;
    private int installments;
    private String frequency;
    private String startDate;
    private String status;
    private List<LoanPayment> payments = new ArrayList<>();

    // Constructores, Getters y Setters
    public Loan() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getInstallments() {
        return installments;
    }

    public void setInstallments(int installments) {
        this.installments = installments;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<LoanPayment> getPayments() {
        return payments;
    }

    public void setPayments(List<LoanPayment> payments) {
        this.payments = payments;
    }

    public enum LoanStatus {
        ACTIVE("ACTIVO"),
        CANCELED("CANCELADO"),
        COMPLETED("COMPLETADO");

        private final String displayName;

        LoanStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum InstallmentStatus {
        PENDING("PENDIENTE"),
        OVERDUE("VENCIDA"),
        PARTIAL("PARCIAL"),
        PAID("PAGADO");

        private final String displayName;

        InstallmentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public String getStatusDisplayName() {
        try {
            return LoanStatus.valueOf(this.status.toUpperCase()).getDisplayName();
        } catch (Exception e) {
            return this.status; // Por si hay algún valor viejo o nulo
        }
    }
}