package com.edj.developer.apploans.model;

public class GeneralReportItem {
    private final String customerName;
    private final int loanId;
    private final double outstandingBalance;

    public GeneralReportItem(String customerName, int loanId, double outstandingBalance) {
        this.customerName = customerName;
        this.loanId = loanId;
        this.outstandingBalance = outstandingBalance;
    }

    public String getCustomerName() { return customerName; }
    public int getLoanId() { return loanId; }
    public double getOutstandingBalance() { return outstandingBalance; }

    /** @deprecated El informe general ahora comunica saldo pendiente, no cuota. */
    @Deprecated
    public double getInstallmentAmount() { return outstandingBalance; }
}
