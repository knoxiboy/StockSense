package com.stocksense.warehouse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.warehouse.Location;
import com.stocksense.warehouse.LocationType;
import com.stocksense.warehouse.Warehouse;
import java.time.LocalDateTime;

public class LocationResponse {

    private Long id;
    private String name;
    private String code;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private LocationType locationType;
    private boolean active;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public LocationResponse() {
    }

    public static LocationResponse fromEntity(Location location) {
        LocationResponse dto = new LocationResponse();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setCode(location.getCode());
        Warehouse wh = location.getWarehouse();
        if (wh != null) {
            dto.setWarehouseId(wh.getId());
            dto.setWarehouseName(wh.getName());
            dto.setWarehouseCode(wh.getCode());
        }
        dto.setLocationType(location.getLocationType());
        dto.setActive(location.isActive());
        dto.setCreatedAt(location.getCreatedAt());
        dto.setUpdatedAt(location.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
