package com.stocksense.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.inventory.StockBalance;
import com.stocksense.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockBalanceResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal reorderLevel;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public StockBalanceResponse() {
    }

    public static StockBalanceResponse fromEntity(StockBalance balance) {
        StockBalanceResponse dto = new StockBalanceResponse();
        dto.setId(balance.getId());
        Product p = balance.getProduct();
        if (p != null) {
            dto.setProductId(p.getId());
            dto.setProductName(p.getName());
            dto.setSku(p.getSku());
            dto.setUnit(p.getUnit());
            dto.setReorderLevel(p.getReorderLevel());
        }
        dto.setQuantity(balance.getQuantity());
        dto.setUpdatedAt(balance.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
