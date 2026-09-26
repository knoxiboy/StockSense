package com.stocksense.warehouse.dto;

import com.stocksense.warehouse.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateLocationRequest {

    @NotBlank(message = "Location name is required")
    @Size(min = 1, max = 150, message = "Location name must be between 1 and 150 characters")
    private String name;

    @NotBlank(message = "Location code is required")
    @Size(min = 1, max = 50, message = "Location code must be between 1 and 50 characters")
    private String code;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    private LocationType locationType = LocationType.INTERNAL;

    public CreateLocationRequest() {
    }

    public CreateLocationRequest(String name, String code, Long warehouseId, LocationType locationType) {
        this.name = name;
        this.code = code;
        this.warehouseId = warehouseId;
        this.locationType = locationType != null ? locationType : LocationType.INTERNAL;
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

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }
}
