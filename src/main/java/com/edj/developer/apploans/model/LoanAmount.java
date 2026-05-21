package com.edj.developer.apploans.model;

public class LoanAmount {
    private int id;
    private double value;

    public LoanAmount() {}

    public LoanAmount(double value) {
        this.value = value;
    }

    public LoanAmount(int id, double value) {
        this.id = id;
        this.value = value;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    @Override
    public String toString() { return String.valueOf(value); }
}