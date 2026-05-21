package com.edj.developer.apploans.model;

public class LoanSummary {
    private final int totalLoans;
    private final int activeLoans;
    private final int completedLoans;
    private final double pendingBalance;

    public LoanSummary(int totalLoans, int activeLoans, int completedLoans, double pendingBalance) {
        this.totalLoans = totalLoans;
        this.activeLoans = activeLoans;
        this.completedLoans = completedLoans;
        this.pendingBalance = pendingBalance;
    }

    public int getTotalLoans() { return totalLoans; }
    public int getActiveLoans() { return activeLoans; }
    public int getCompletedLoans() { return completedLoans; }
    public double getPendingBalance() { return pendingBalance; }
}