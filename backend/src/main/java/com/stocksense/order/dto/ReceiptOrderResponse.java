package com.stocksense.order.dto;

import com.stocksense.order.OrderStatus;
import com.stocksense.warehouse.dto.LocationResponse;

import java.time.LocalDateTime;
import java.util.List;

public class ReceiptOrderResponse {
    private Long id;
    private String reference;
    private String supplierName;
    private String supplierContact;
    private LocationResponse destinationLocation;
    private OrderStatus status;
    private String notes;
    private LocalDateTime completedAt;
    private List<ReceiptOrderLineDto> lines;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReceiptOrderResponse() {
    }

    public ReceiptOrderResponse(Long id, String reference, String supplierName, String supplierContact,
                                LocationResponse destinationLocation, OrderStatus status, String notes,
                                LocalDateTime completedAt, List<ReceiptOrderLineDto> lines,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reference = reference;
        this.supplierName = supplierName;
        this.supplierContact = supplierContact;
        this.destinationLocation = destinationLocation;
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

    public LocationResponse getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(LocationResponse destinationLocation) {
        this.destinationLocation = destinationLocation;
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

    public List<ReceiptOrderLineDto> getLines() {
        return lines;
    }

    public void setLines(List<ReceiptOrderLineDto> lines) {
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
