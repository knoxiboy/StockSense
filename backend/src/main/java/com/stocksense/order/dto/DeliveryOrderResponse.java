package com.stocksense.order.dto;

import com.stocksense.order.OrderStatus;
import com.stocksense.warehouse.dto.LocationResponse;

import java.time.LocalDateTime;
import java.util.List;

public class DeliveryOrderResponse {
    private Long id;
    private String reference;
    private String customerName;
    private String customerContact;
    private String deliveryAddress;
    private LocationResponse sourceLocation;
    private OrderStatus status;
    private String notes;
    private LocalDateTime completedAt;
    private List<DeliveryOrderLineDto> lines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DeliveryOrderResponse() {
    }

    public DeliveryOrderResponse(Long id, String reference, String customerName, String customerContact,
                                 String deliveryAddress, LocationResponse sourceLocation, OrderStatus status,
                                 String notes, LocalDateTime completedAt, List<DeliveryOrderLineDto> lines,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reference = reference;
        this.customerName = customerName;
        this.customerContact = customerContact;
        this.deliveryAddress = deliveryAddress;
        this.sourceLocation = sourceLocation;
        this.status = status;
        this.notes = notes;
        this.completedAt = completedAt;
        this.lines = lines;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocationResponse getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(LocationResponse sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<DeliveryOrderLineDto> getLines() {
        return lines;
    }

    public void setLines(List<DeliveryOrderLineDto> lines) {
        this.lines = lines;
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
