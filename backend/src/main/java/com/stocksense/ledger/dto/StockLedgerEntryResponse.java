package com.stocksense.ledger.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.ledger.StockLedgerEntry;
import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.product.Product;
import com.stocksense.warehouse.Location;
import com.stocksense.warehouse.Warehouse;

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

    private Long warehouseId;
    private String warehouseName;
    private String warehouseCode;

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

        Location loc = entry.getLocation();
        if (loc != null) {
            dto.setLocationId(loc.getId());
            dto.setLocationName(loc.getName());
            dto.setLocationCode(loc.getCode());
            Warehouse wh = loc.getWarehouse();
            if (wh != null) {
                dto.setWarehouseId(wh.getId());
                dto.setWarehouseName(wh.getName());
                dto.setWarehouseCode(wh.getCode());
            }
        }

        Location srcLoc = entry.getSourceLocation();
        if (srcLoc != null) {
            dto.setSourceLocationId(srcLoc.getId());
            dto.setSourceLocationName(srcLoc.getName());
            dto.setSourceLocationCode(srcLoc.getCode());
        }

        Location destLoc = entry.getDestinationLocation();
        if (destLoc != null) {
            dto.setDestinationLocationId(destLoc.getId());
            dto.setDestinationLocationName(destLoc.getName());
            dto.setDestinationLocationCode(destLoc.getCode());
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
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
