package com.edj.developer.apploans.model;

public class GeneralReportItem {
    private final String customerName;
    private final int loanId;
    private final double installmentAmount;

    public GeneralReportItem(String customerName, int loanId, double installmentAmount) {
        this.customerName = customerName;
        this.loanId = loanId;
        this.installmentAmount = installmentAmount;
    }

    public String getCustomerName() { return customerName; }
    public int getLoanId() { return loanId; }
    public double getInstallmentAmount() { return installmentAmount; }
}