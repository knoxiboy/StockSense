package com.stocksense.inventory.dto;

import java.math.BigDecimal;

public class LocationBalanceItem {

    private Long locationId;
    private String locationName;
    private String locationCode;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private BigDecimal quantity;

    public LocationBalanceItem() {
    }

    public LocationBalanceItem(Long locationId, String locationName, String locationCode,
                               Long warehouseId, String warehouseName, String warehouseCode,
                               BigDecimal quantity) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.locationCode = locationCode;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.warehouseCode = warehouseCode;
        this.quantity = quantity;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
