package com.stocksense.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_category", columnList = "category")
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reorder_level", nullable = false, precision = 18, scale = 4)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Product() {
    }

    public Product(String name, String sku, String category, String unit,
                   BigDecimal reorderLevel, String description, BigDecimal price) {
        this.name = name != null ? name.trim() : null;
        this.sku = sku != null ? sku.trim() : null;
        this.category = category != null ? category.trim() : null;
        this.unit = unit != null ? unit.trim() : null;
        this.reorderLevel = reorderLevel != null ? reorderLevel : BigDecimal.ZERO;
        this.description = description != null ? description.trim() : null;
        this.price = price;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.reorderLevel == null) {
            this.reorderLevel = BigDecimal.ZERO;
        }
        if (this.name != null) this.name = this.name.trim();
        if (this.sku != null) this.sku = this.sku.trim();
        if (this.category != null) this.category = this.category.trim();
        if (this.unit != null) this.unit = this.unit.trim();
        if (this.description != null) this.description = this.description.trim();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.reorderLevel == null) {
            this.reorderLevel = BigDecimal.ZERO;
        }
        if (this.name != null) this.name = this.name.trim();
        if (this.sku != null) this.sku = this.sku.trim();
        if (this.category != null) this.category = this.category.trim();
        if (this.unit != null) this.unit = this.unit.trim();
        if (this.description != null) this.description = this.description.trim();
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

    // Compatibility getter
    public String getUnitOfMeasure() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel != null ? reorderLevel : BigDecimal.ZERO;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
