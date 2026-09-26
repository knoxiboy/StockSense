package com.stocksense.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateWarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    @Size(min = 1, max = 150, message = "Warehouse name must be between 1 and 150 characters")
    private String name;

    @NotBlank(message = "Warehouse code is required")
    @Size(min = 1, max = 50, message = "Warehouse code must be between 1 and 50 characters")
    private String code;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    private Boolean active;

    public UpdateWarehouseRequest() {
    }

    public UpdateWarehouseRequest(String name, String code, String address, Boolean active) {
        this.name = name;
        this.code = code;
        this.address = address;
        this.active = active;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
