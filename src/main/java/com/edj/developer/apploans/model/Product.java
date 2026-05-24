package com.edj.developer.apploans.model;

public class Product {
    private int id;
    private String name;
    private String description;
    private int stock;
    private double basePrice;
    private String createdAt;

    public Product() {}

    public Product(int id, String name, String description, int stock, double basePrice, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.stock = stock;
        this.basePrice = basePrice;
        this.createdAt = createdAt;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}