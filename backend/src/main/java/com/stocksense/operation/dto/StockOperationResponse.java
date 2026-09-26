package com.stocksense.operation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.product.Product;
import com.stocksense.warehouse.Location;

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

    private Long locationId;
    private String locationName;
    private String locationCode;

    private Long sourceLocationId;
    private String sourceLocationName;
    private String sourceLocationCode;

    private Long destinationLocationId;
    private String destinationLocationName;
    private String destinationLocationCode;

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

        Location loc = op.getLocation();
        if (loc != null) {
            response.setLocationId(loc.getId());
            response.setLocationName(loc.getName());
            response.setLocationCode(loc.getCode());
        }

        Location src = op.getSourceLocation();
        if (src != null) {
            response.setSourceLocationId(src.getId());
            response.setSourceLocationName(src.getName());
            response.setSourceLocationCode(src.getCode());
        }

        Location dest = op.getDestinationLocation();
        if (dest != null) {
            response.setDestinationLocationId(dest.getId());
            response.setDestinationLocationName(dest.getName());
            response.setDestinationLocationCode(dest.getCode());
        }

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

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
