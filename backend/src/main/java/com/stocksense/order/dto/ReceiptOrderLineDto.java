package com.stocksense.order.dto;

import java.math.BigDecimal;

public class ReceiptOrderLineDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private BigDecimal expectedQuantity;
    private BigDecimal receivedQuantity;

    public ReceiptOrderLineDto() {
    }

    public ReceiptOrderLineDto(Long id, Long productId, String productSku, String productName,
                               BigDecimal expectedQuantity, BigDecimal receivedQuantity) {
        this.id = id;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.expectedQuantity = expectedQuantity;
        this.receivedQuantity = receivedQuantity;
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

    public BigDecimal getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(BigDecimal expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public BigDecimal getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }
}
