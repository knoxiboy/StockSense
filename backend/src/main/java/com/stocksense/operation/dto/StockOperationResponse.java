package com.stocksense.operation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockOperationResponse {

    private Long id;
    private OperationType operationType;
    private Long productId;
    private String productName;
    private String sku;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal quantityChange;
    private BigDecimal resultingQuantity;
    private String reference;
    private String notes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public StockOperationResponse() {
    }

    public static StockOperationResponse fromEntity(StockOperation op, BigDecimal resultingQuantity) {
        StockOperationResponse response = new StockOperationResponse();
        response.setId(op.getId());
        response.setOperationType(op.getOperationType());
        Product p = op.getProduct();
        if (p != null) {
            response.setProductId(p.getId());
            response.setProductName(p.getName());
            response.setSku(p.getSku());
            response.setUnit(p.getUnit());
        }
        response.setQuantity(op.getQuantity());
        response.setQuantityChange(op.getQuantityChange());
        response.setResultingQuantity(resultingQuantity);
        response.setReference(op.getReference());
        response.setNotes(op.getNotes());
        response.setCreatedAt(op.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public BigDecimal getResultingQuantity() {
        return resultingQuantity;
    }

    public void setResultingQuantity(BigDecimal resultingQuantity) {
        this.resultingQuantity = resultingQuantity;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
