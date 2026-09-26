package com.stocksense.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public class CreateDeliveryOrderRequest {

    @NotBlank(message = "Reference cannot be blank")
    private String reference;

    @NotBlank(message = "Customer name cannot be blank")
    private String customerName;

    private String customerContact;

    private String deliveryAddress;

    private Long sourceLocationId;

    private String notes;

    @NotEmpty(message = "Delivery order must have at least one line")
    @Valid
    private List<CreateLineRequest> lines;

    public static class CreateLineRequest {
        @NotNull(message = "Product ID cannot be null")
        private Long productId;

        @NotNull(message = "Ordered quantity cannot be null")
        @Positive(message = "Ordered quantity must be strictly positive")
        private BigDecimal orderedQuantity;

        private BigDecimal deliveredQuantity;

        private Boolean picked;

        private Boolean packed;

        public CreateLineRequest() {
        }

        public CreateLineRequest(Long productId, BigDecimal orderedQuantity, BigDecimal deliveredQuantity) {
            this.productId = productId;
            this.orderedQuantity = orderedQuantity;
            this.deliveredQuantity = deliveredQuantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
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

    public CreateDeliveryOrderRequest() {
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
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
