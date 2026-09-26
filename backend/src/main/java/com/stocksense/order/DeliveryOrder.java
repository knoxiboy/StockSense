package com.stocksense.order;

import com.stocksense.warehouse.Location;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "delivery_orders", indexes = {
        @Index(name = "idx_delivery_reference", columnList = "reference", unique = true),
        @Index(name = "idx_delivery_status", columnList = "status"),
        @Index(name = "idx_delivery_created_at", columnList = "created_at")
})
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String reference;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_contact", length = 200)
    private String customerContact;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    private Location sourceLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(length = 500)
    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "deliveryOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DeliveryOrderLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DeliveryOrder() {
    }

    public DeliveryOrder(String reference, String customerName, String customerContact, String deliveryAddress,
                         Location sourceLocation, OrderStatus status, String notes) {
        this.reference = reference != null ? reference.trim() : null;
        this.customerName = customerName != null ? customerName.trim() : null;
        this.customerContact = customerContact != null ? customerContact.trim() : null;
        this.deliveryAddress = deliveryAddress != null ? deliveryAddress.trim() : null;
        this.sourceLocation = sourceLocation;
        this.status = status != null ? status : OrderStatus.DRAFT;
        this.notes = notes != null ? notes.trim() : null;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.reference != null) this.reference = this.reference.trim();
        if (this.customerName != null) this.customerName = this.customerName.trim();
        if (this.customerContact != null) this.customerContact = this.customerContact.trim();
        if (this.deliveryAddress != null) this.deliveryAddress = this.deliveryAddress.trim();
        if (this.notes != null) this.notes = this.notes.trim();
        if (this.status == null) this.status = OrderStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.reference != null) this.reference = this.reference.trim();
        if (this.customerName != null) this.customerName = this.customerName.trim();
        if (this.customerContact != null) this.customerContact = this.customerContact.trim();
        if (this.deliveryAddress != null) this.deliveryAddress = this.deliveryAddress.trim();
        if (this.notes != null) this.notes = this.notes.trim();
    }

    public void addLine(DeliveryOrderLine line) {
        lines.add(line);
        line.setDeliveryOrder(this);
    }

    public void removeLine(DeliveryOrderLine line) {
        lines.remove(line);
        line.setDeliveryOrder(null);
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

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(Location sourceLocation) {
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

    public List<DeliveryOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<DeliveryOrderLine> lines) {
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
