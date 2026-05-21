package com.edj.developer.apploans.model;

public class DailyReportItem {
    private final int loanId;
    private final String customerName;
    private final int installmentNumber;
    private final double amount;
    private final String dueDate;

    public DailyReportItem(int loanId, String customerName, int installmentNumber, double amount, String dueDate) {
        this.loanId = loanId;
        this.customerName = customerName;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    public int getLoanId() { return loanId; }
    public String getCustomerName() { return customerName; }
    public int getInstallmentNumber() { return installmentNumber; }
    public double getAmount() { return amount; }
    public String getDueDate() { return dueDate; }
}