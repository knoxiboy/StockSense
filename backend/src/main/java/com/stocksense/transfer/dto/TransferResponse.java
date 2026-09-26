package com.stocksense.transfer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.product.Product;
import com.stocksense.transfer.InternalTransfer;
import com.stocksense.transfer.TransferStatus;
import com.stocksense.warehouse.Location;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransferResponse {

    private Long id;
    private String reference;
    private Long productId;
    private String productName;
    private String sku;
    private String unit;

    private Long sourceLocationId;
    private String sourceLocationName;
    private String sourceLocationCode;
    private String sourceWarehouseName;

    private Long destinationLocationId;
    private String destinationLocationName;
    private String destinationLocationCode;
    private String destinationWarehouseName;

    private BigDecimal quantity;
    private TransferStatus status;
    private String notes;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public TransferResponse() {
    }

    public static TransferResponse fromEntity(InternalTransfer transfer) {
        TransferResponse dto = new TransferResponse();
        dto.setId(transfer.getId());
        dto.setReference(transfer.getReference());

        Product product = transfer.getProduct();
        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductName(product.getName());
            dto.setSku(product.getSku());
            dto.setUnit(product.getUnit());
        }

        Location src = transfer.getSourceLocation();
        if (src != null) {
            dto.setSourceLocationId(src.getId());
            dto.setSourceLocationName(src.getName());
            dto.setSourceLocationCode(src.getCode());
            if (src.getWarehouse() != null) {
                dto.setSourceWarehouseName(src.getWarehouse().getName());
            }
        }

        Location dest = transfer.getDestinationLocation();
        if (dest != null) {
            dto.setDestinationLocationId(dest.getId());
            dto.setDestinationLocationName(dest.getName());
            dto.setDestinationLocationCode(dest.getCode());
            if (dest.getWarehouse() != null) {
                dto.setDestinationWarehouseName(dest.getWarehouse().getName());
            }
        }

        dto.setQuantity(transfer.getQuantity());
        dto.setStatus(transfer.getStatus());
        dto.setNotes(transfer.getNotes());
        dto.setCompletedAt(transfer.getCompletedAt());
        dto.setCreatedAt(transfer.getCreatedAt());
        dto.setUpdatedAt(transfer.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
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

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public String getSourceLocationName() {
        return sourceLocationName;
    }

    public void setSourceLocationName(String sourceLocationName) {
        this.sourceLocationName = sourceLocationName;
    }

    public String getSourceLocationCode() {
        return sourceLocationCode;
    }

    public void setSourceLocationCode(String sourceLocationCode) {
        this.sourceLocationCode = sourceLocationCode;
    }

    public String getSourceWarehouseName() {
        return sourceWarehouseName;
    }

    public void setSourceWarehouseName(String sourceWarehouseName) {
        this.sourceWarehouseName = sourceWarehouseName;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public String getDestinationLocationName() {
        return destinationLocationName;
    }

    public void setDestinationLocationName(String destinationLocationName) {
        this.destinationLocationName = destinationLocationName;
    }

    public String getDestinationLocationCode() {
        return destinationLocationCode;
    }

    public void setDestinationLocationCode(String destinationLocationCode) {
        this.destinationLocationCode = destinationLocationCode;
    }

    public String getDestinationWarehouseName() {
        return destinationWarehouseName;
    }

    public void setDestinationWarehouseName(String destinationWarehouseName) {
        this.destinationWarehouseName = destinationWarehouseName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
