package com.edj.developer.apploans.model;

public class LoanFrequency {
    private int id;
    private String name;
    private int daysInterval;

    public LoanFrequency() {}

    public LoanFrequency(String name, int daysInterval) {
        this.name = name;
        this.daysInterval = daysInterval;
    }

    public LoanFrequency(int id, String name, int daysInterval) {
        this.id = id;
        this.name = name;
        this.daysInterval = daysInterval;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDaysInterval() { return daysInterval; }
    public void setDaysInterval(int daysInterval) { this.daysInterval = daysInterval; }

    @Override
    public String toString() { return name; }
}