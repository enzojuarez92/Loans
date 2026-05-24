package com.edj.developer.apploans.dao;

import com.edj.developer.apploans.model.Product;
import java.util.List;

public interface ProductDAO {
    boolean insert(Product product);
    boolean update(Product product);
    boolean delete(int id);
    Product findById(int id);
    List<Product> findAllPaged(String search, String stockFilter, int limit, int offset);
    int countProducts(String search, String stockFilter);

    // Métodos rápidos para las tarjetas de estadísticas
    int getTotalCount();
    int getLowStockCount();
    int getNoStockCount();
}