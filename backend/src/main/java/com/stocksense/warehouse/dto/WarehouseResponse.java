package com.stocksense.warehouse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stocksense.warehouse.Warehouse;
import java.time.LocalDateTime;

public class WarehouseResponse {

    private Long id;
    private String name;
    private String code;
    private String address;
    private boolean active;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public WarehouseResponse() {
    }

    public static WarehouseResponse fromEntity(Warehouse warehouse) {
        WarehouseResponse dto = new WarehouseResponse();
        dto.setId(warehouse.getId());
        dto.setName(warehouse.getName());
        dto.setCode(warehouse.getCode());
        dto.setAddress(warehouse.getAddress());
        dto.setActive(warehouse.isActive());
        dto.setCreatedAt(warehouse.getCreatedAt());
        dto.setUpdatedAt(warehouse.getUpdatedAt());
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
