package com.edj.developer.apploans.dao;

import com.edj.developer.apploans.model.Sale;
import java.util.List;

public interface SaleDAO {

    boolean saveWithTransaction(Sale sale, int daysInterval);

    List<Sale> findAllPaged(String search, String statusFilter, int limit, int offset);

    int countSales(String search, String statusFilter);

    Sale findFullSaleById(int id);

    boolean updateSalePaymentStatus(int paymentId, String status, double paidAmount);

    boolean cancelSaleWithStockRestoration(int saleId, int productId);
}