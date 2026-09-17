package com.rembyte.service;
public class OrderStatistics {
    private int totalOrders;
    private long completedOrders;
    private double totalRevenue;
    private double totalPaid;
    private double averageOrderPrice;
    private double totalMaterialCost;
    private double netProfit;
    public OrderStatistics() {}
    public OrderStatistics(int totalOrders, long completedOrders, double totalRevenue, double totalPaid,
                            double averageOrderPrice, double totalMaterialCost, double netProfit) {
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.totalRevenue = totalRevenue;
        this.totalPaid = totalPaid;
        this.averageOrderPrice = averageOrderPrice;
        this.totalMaterialCost = totalMaterialCost;
        this.netProfit = netProfit;
    }
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    public long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(double totalPaid) { this.totalPaid = totalPaid; }
    public double getAverageOrderPrice() { return averageOrderPrice; }
    public void setAverageOrderPrice(double averageOrderPrice) { this.averageOrderPrice = averageOrderPrice; }
    public double getTotalMaterialCost() { return totalMaterialCost; }
    public void setTotalMaterialCost(double totalMaterialCost) { this.totalMaterialCost = totalMaterialCost; }
    public double getNetProfit() { return netProfit; }
    public void setNetProfit(double netProfit) { this.netProfit = netProfit; }
}
