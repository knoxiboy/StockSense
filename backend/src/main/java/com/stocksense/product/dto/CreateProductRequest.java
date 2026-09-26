package com.stocksense.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 150, message = "Product name must be between 1 and 150 characters")
    private String name;

    @NotBlank(message = "Product SKU is required")
    @Size(min = 1, max = 80, message = "SKU must be between 1 and 80 characters")
    private String sku;

    @NotBlank(message = "Category is required")
    @Size(min = 1, max = 100, message = "Category must be between 1 and 100 characters")
    private String category;

    @NotBlank(message = "Unit is required")
    @Size(min = 1, max = 50, message = "Unit must be between 1 and 50 characters")
    @JsonAlias({"unitOfMeasure"})
    private String unit;

    @NotNull(message = "Reorder level is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level must be zero or positive")
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be zero or positive")
    private BigDecimal price;

    private String description;

    public CreateProductRequest() {
    }

    public CreateProductRequest(String name, String sku, String category, String unit,
                                BigDecimal reorderLevel, String description, BigDecimal price) {
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.unit = unit;
        this.reorderLevel = reorderLevel != null ? reorderLevel : BigDecimal.ZERO;
        this.description = description;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku != null ? sku.trim() : null;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category != null ? category.trim() : null;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit != null ? unit.trim() : null;
    }

    public String getUnitOfMeasure() {
        return unit;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unit = unitOfMeasure != null ? unitOfMeasure.trim() : null;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }
}
