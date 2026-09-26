package com.stocksense.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateWarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    @Size(min = 1, max = 150, message = "Warehouse name must be between 1 and 150 characters")
    private String name;

    @NotBlank(message = "Warehouse code is required")
    @Size(min = 1, max = 50, message = "Warehouse code must be between 1 and 50 characters")
    private String code;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    public CreateWarehouseRequest() {
    }

    public CreateWarehouseRequest(String name, String code, String address) {
        this.name = name;
        this.code = code;
        this.address = address;
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
}
