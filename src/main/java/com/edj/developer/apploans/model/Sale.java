package com.edj.developer.apploans.model;

import java.util.ArrayList;
import java.util.List;

public class Sale {
    private int id;
    private int customerId;
    private String customerName; // Auxiliar para la grilla
    private int productId;
    private String productName;   // Auxiliar para la grilla
    private double sellingPrice;
    private double interestRate;
    private double totalAmount;
    private int installments;
    private String frequency;
    private String startDate;
    private String status;
    private String createdAt;

    // 💡 AGREGADOS: Campos de contacto del cliente para que los consuma el reporte
    private String customerPhone;
    private String customerAddress;
    private String customerEmail;

    private List<SalePayment> payments = new ArrayList<>();
    // 💡 CORREGIDO: Ahora usa tu nueva clase SaleReceipt en lugar de LoanReceipt
    private List<SaleReceipt> receipts = new ArrayList<>();

    public Sale() {}

    // --- GETTERS Y SETTERS DE LAS LISTAS ---
    public List<SalePayment> getPayments() { return payments; }
    public void setPayments(List<SalePayment> payments) { this.payments = payments; }

    public List<SaleReceipt> getReceipts() { return receipts; }
    public void setReceipts(List<SaleReceipt> receipts) { this.receipts = receipts; }

    // --- GETTERS Y SETTERS DE DATOS DE CONTACTO ---
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    // --- GETTERS Y SETTERS TRADICIONALES ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public int getInstallments() { return installments; }
    public void setInstallments(int installments) { this.installments = installments; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}