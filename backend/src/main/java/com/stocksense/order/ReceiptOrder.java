package com.stocksense.order;

import com.stocksense.warehouse.Location;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipt_orders", indexes = {
        @Index(name = "idx_receipt_reference", columnList = "reference", unique = true),
        @Index(name = "idx_receipt_status", columnList = "status"),
        @Index(name = "idx_receipt_created_at", columnList = "created_at")
})
public class ReceiptOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String reference;

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "supplier_contact", length = 200)
    private String supplierContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private Location destinationLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(length = 500)
    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "receiptOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ReceiptOrderLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ReceiptOrder() {
    }

    public ReceiptOrder(String reference, String supplierName, String supplierContact, Location destinationLocation, OrderStatus status, String notes) {
        this.reference = reference != null ? reference.trim() : null;
        this.supplierName = supplierName != null ? supplierName.trim() : null;
        this.supplierContact = supplierContact != null ? supplierContact.trim() : null;
        this.destinationLocation = destinationLocation;
        this.status = status != null ? status : OrderStatus.DRAFT;
        this.notes = notes != null ? notes.trim() : null;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.reference != null) this.reference = this.reference.trim();
        if (this.supplierName != null) this.supplierName = this.supplierName.trim();
        if (this.supplierContact != null) this.supplierContact = this.supplierContact.trim();
        if (this.notes != null) this.notes = this.notes.trim();
        if (this.status == null) this.status = OrderStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.reference != null) this.reference = this.reference.trim();
        if (this.supplierName != null) this.supplierName = this.supplierName.trim();
        if (this.supplierContact != null) this.supplierContact = this.supplierContact.trim();
        if (this.notes != null) this.notes = this.notes.trim();
    }

    public void addLine(ReceiptOrderLine line) {
        lines.add(line);
        line.setReceiptOrder(this);
    }

    public void removeLine(ReceiptOrderLine line) {
        lines.remove(line);
        line.setReceiptOrder(null);
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

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
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

    public List<ReceiptOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<ReceiptOrderLine> lines) {
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
