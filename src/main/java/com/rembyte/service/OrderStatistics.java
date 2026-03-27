package com.rembyte.service;
public class OrderStatistics {
    private int totalOrders;
    private long completedOrders;
    private double totalRevenue;
    private double totalPaid;
    private double averageOrderPrice;
    public OrderStatistics() {}
    public OrderStatistics(int totalOrders, long completedOrders, double totalRevenue, double totalPaid, double averageOrderPrice) {
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.totalRevenue = totalRevenue;
        this.totalPaid = totalPaid;
        this.averageOrderPrice = averageOrderPrice;
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
}
