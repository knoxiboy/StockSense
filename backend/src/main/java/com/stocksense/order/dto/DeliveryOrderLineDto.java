package com.stocksense.order.dto;

import java.math.BigDecimal;

public class DeliveryOrderLineDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal orderedQuantity;
    private BigDecimal deliveredQuantity;
    private Boolean picked;
    private Boolean packed;

    public DeliveryOrderLineDto() {
    }

    public DeliveryOrderLineDto(Long id, Long productId, String productSku, String productName,
                                BigDecimal orderedQuantity, BigDecimal deliveredQuantity,
                                Boolean picked, Boolean packed) {
        this.id = id;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.orderedQuantity = orderedQuantity;
        this.deliveredQuantity = deliveredQuantity;
        this.picked = picked;
        this.packed = packed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductSku() {
        return productSku;
    }

    public void setProductSku(String productSku) {
        this.productSku = productSku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getOrderedQuantity() {
        return orderedQuantity;
    }

    public void setOrderedQuantity(BigDecimal orderedQuantity) {
        this.orderedQuantity = orderedQuantity;
    }

    public BigDecimal getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(BigDecimal deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public Boolean getPicked() {
        return picked;
    }

    public void setPicked(Boolean picked) {
        this.picked = picked;
    }

    public Boolean getPacked() {
        return packed;
    }

    public void setPacked(Boolean packed) {
        this.packed = packed;
    }
}
