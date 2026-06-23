package com.edj.developer.apploans.dao;

import java.util.Map;

public interface DashboardDAO {
    double getTotalOutstandingCapital();
    double getTotalRecoveredCapital();
    int getActiveLoansCount();
    int getDelinquentCustomersCount();

    int getTotalLoansCount();
    int getLoansCountByStatus(String status);
    int getTotalUniqueCustomersCount();

    int getWeeklyPendingInstallmentsCount();
    double getWeeklyPendingAmountTotal();
    Map<String, Double> getWeeklyCollectionsByDay();

    Map<String, Double> getMonthlyLoanCollections();

    // Sales Methods
    int getTotalSalesCount();
    int getSalesCountByStatus(String status);
    Map<String, Double> getMonthlySalesRevenue();
}