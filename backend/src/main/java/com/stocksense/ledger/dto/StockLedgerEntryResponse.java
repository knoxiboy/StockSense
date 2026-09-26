package com.stocksense.ledger.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.ledger.StockLedgerEntry;
import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.product.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockLedgerEntryResponse {

    private Long id;
    private Long operationId;
    private Long productId;
    private String productName;
    private String sku;
    private String unit;
    private OperationType operationType;
    private BigDecimal quantityChange;
    private BigDecimal previousQuantity;
    private BigDecimal resultingQuantity;
    private String reference;
    private String notes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public StockLedgerEntryResponse() {
    }

    public static StockLedgerEntryResponse fromEntity(StockLedgerEntry entry) {
        StockLedgerEntryResponse dto = new StockLedgerEntryResponse();
        dto.setId(entry.getId());
        dto.setOperationType(entry.getOperationType());
        dto.setQuantityChange(entry.getQuantityChange());
        dto.setPreviousQuantity(entry.getPreviousQuantity());
        dto.setResultingQuantity(entry.getResultingQuantity());
        dto.setCreatedAt(entry.getCreatedAt());

        Product product = entry.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setSku(product.getSku());
            dto.setUnit(product.getUnit());
        }

        StockOperation operation = entry.getOperation();
        if (operation != null) {
            dto.setOperationId(operation.getId());
            dto.setReference(operation.getReference());
            dto.setNotes(operation.getNotes());
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
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

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public BigDecimal getPreviousQuantity() {
        return previousQuantity;
    }

    public void setPreviousQuantity(BigDecimal previousQuantity) {
        this.previousQuantity = previousQuantity;
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
