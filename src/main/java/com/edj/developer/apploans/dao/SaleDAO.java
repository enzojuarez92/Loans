package com.edj.developer.apploans.dao;

import com.edj.developer.apploans.model.Sale;

public interface SaleDAO {

    boolean saveWithTransaction(Sale sale, int daysInterval);

    java.util.List<Sale> findAllPaged(String search, String statusFilter, int limit, int offset);

    int countSales(String search, String statusFilter);

    Sale findFullSaleById(int id);

    boolean updateSalePaymentStatus(int paymentId, String status, double paidAmount);

    boolean processSaleCascadePayment(int saleId, double totalAmount, String notes);

    boolean revertLastSalePayment(int receiptId, int saleId, double amount);

    boolean cancelSaleWithOption(int saleId, int productId, boolean restoreStock);
}