package com.stocksense.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public class CreateReceiptOrderRequest {

    @NotBlank(message = "Reference cannot be blank")
    private String reference;

    @NotBlank(message = "Supplier name cannot be blank")
    private String supplierName;

    private String supplierContact;

    private Long destinationLocationId;

    private String notes;

    @NotEmpty(message = "Receipt order must have at least one line")
    @Valid
    private List<CreateLineRequest> lines;

    public static class CreateLineRequest {
        @NotNull(message = "Product ID cannot be null")
        private Long productId;

        @NotNull(message = "Expected quantity cannot be null")
        @Positive(message = "Expected quantity must be strictly positive")
        private BigDecimal expectedQuantity;

        private BigDecimal receivedQuantity;

        public CreateLineRequest() {
        }

        public CreateLineRequest(Long productId, BigDecimal expectedQuantity, BigDecimal receivedQuantity) {
            this.productId = productId;
            this.expectedQuantity = expectedQuantity;
            this.receivedQuantity = receivedQuantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
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

    public CreateReceiptOrderRequest() {
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierContact() {
        return supplierContact;
    }

    public void setSupplierContact(String supplierContact) {
        this.supplierContact = supplierContact;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<CreateLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<CreateLineRequest> lines) {
        this.lines = lines;
    }
}
