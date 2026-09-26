package com.stocksense.dashboard;

import java.math.BigDecimal;

public class DashboardKpiResponse {

    private long totalProducts;
    private long totalProductsInStock;
    private BigDecimal totalStockUnits;
    private long lowStockProducts;
    private long outOfStockProducts;
    private long pendingReceipts;
    private long pendingDeliveries;
    private long scheduledTransfers;

    public DashboardKpiResponse() {
    }

    public DashboardKpiResponse(long totalProducts, long totalProductsInStock, BigDecimal totalStockUnits,
                                long lowStockProducts, long outOfStockProducts,
                                long pendingReceipts, long pendingDeliveries, long scheduledTransfers) {
        this.totalProducts = totalProducts;
        this.totalProductsInStock = totalProductsInStock;
        this.totalStockUnits = totalStockUnits != null ? totalStockUnits : BigDecimal.ZERO;
        this.lowStockProducts = lowStockProducts;
        this.outOfStockProducts = outOfStockProducts;
        this.pendingReceipts = pendingReceipts;
        this.pendingDeliveries = pendingDeliveries;
        this.scheduledTransfers = scheduledTransfers;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalProductsInStock() {
        return totalProductsInStock;
    }

    public void setTotalProductsInStock(long totalProductsInStock) {
        this.totalProductsInStock = totalProductsInStock;
    }

    public BigDecimal getTotalStockUnits() {
        return totalStockUnits;
    }

    public void setTotalStockUnits(BigDecimal totalStockUnits) {
        this.totalStockUnits = totalStockUnits;
    }

    public long getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(long lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public long getOutOfStockProducts() {
        return outOfStockProducts;
    }

    public void setOutOfStockProducts(long outOfStockProducts) {
        this.outOfStockProducts = outOfStockProducts;
    }

    public long getPendingReceipts() {
        return pendingReceipts;
    }

    public void setPendingReceipts(long pendingReceipts) {
        this.pendingReceipts = pendingReceipts;
    }

    public long getPendingDeliveries() {
        return pendingDeliveries;
    }

    public void setPendingDeliveries(long pendingDeliveries) {
        this.pendingDeliveries = pendingDeliveries;
    }

    public long getScheduledTransfers() {
        return scheduledTransfers;
    }

    public void setScheduledTransfers(long scheduledTransfers) {
        this.scheduledTransfers = scheduledTransfers;
    }
}
