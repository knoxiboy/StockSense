package com.stocksense.operation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Counted quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Counted quantity must be greater than or equal to zero")
    private BigDecimal countedQuantity;

    @Size(max = 100, message = "Reference must not exceed 100 characters")
    private String reference;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public CreateAdjustmentRequest() {
    }

    public CreateAdjustmentRequest(Long productId, BigDecimal countedQuantity, String reference, String notes) {
        this.productId = productId;
        this.countedQuantity = countedQuantity;
        this.reference = reference;
        this.notes = notes;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getCountedQuantity() {
        return countedQuantity;
    }

    public void setCountedQuantity(BigDecimal countedQuantity) {
        this.countedQuantity = countedQuantity;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
