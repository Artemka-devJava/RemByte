package com.rembyte.service;

public class PartsStatistics {
    private int purchasedCount;
    private double purchasedSum;
    private int soldCount;
    private double revenueSum;
    private double profitSum;
    private double currentBalance;
    private double inventoryValue;

    public PartsStatistics() {}

    public int getPurchasedCount() { return purchasedCount; }
    public void setPurchasedCount(int purchasedCount) { this.purchasedCount = purchasedCount; }

    public double getPurchasedSum() { return purchasedSum; }
    public void setPurchasedSum(double purchasedSum) { this.purchasedSum = purchasedSum; }

    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }

    public double getRevenueSum() { return revenueSum; }
    public void setRevenueSum(double revenueSum) { this.revenueSum = revenueSum; }

    public double getProfitSum() { return profitSum; }
    public void setProfitSum(double profitSum) { this.profitSum = profitSum; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }

    public double getInventoryValue() { return inventoryValue; }
    public void setInventoryValue(double inventoryValue) { this.inventoryValue = inventoryValue; }
}
