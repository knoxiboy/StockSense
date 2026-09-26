package com.stocksense.ledger;

import com.stocksense.operation.OperationType;
import com.stocksense.operation.StockOperation;
import com.stocksense.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_ledger_entries", indexes = {
        @Index(name = "idx_ledger_operation", columnList = "operation_id", unique = true),
        @Index(name = "idx_ledger_product", columnList = "product_id"),
        @Index(name = "idx_ledger_created_at", columnList = "created_at")
})
public class StockLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operation_id", nullable = false, unique = true)
    private StockOperation operation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private OperationType operationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private com.stocksense.warehouse.Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private com.stocksense.warehouse.Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_location_id")
    private com.stocksense.warehouse.Location sourceLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id")
    private com.stocksense.warehouse.Location destinationLocation;

    @Column(name = "quantity_change", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityChange;

    @Column(name = "previous_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal previousQuantity;

    @Column(name = "resulting_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal resultingQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StockLedgerEntry() {
    }

    public StockLedgerEntry(StockOperation operation, Product product, OperationType operationType,
                            BigDecimal quantityChange, BigDecimal previousQuantity, BigDecimal resultingQuantity) {
        this.operation = operation;
        this.product = product;
        this.operationType = operationType;
        this.quantityChange = quantityChange;
        this.previousQuantity = previousQuantity;
        this.resultingQuantity = resultingQuantity;
        if (operation != null) {
            this.location = operation.getLocation();
            this.sourceLocation = operation.getSourceLocation();
            this.destinationLocation = operation.getDestinationLocation();
            if (this.location != null) {
                this.warehouse = this.location.getWarehouse();
            }
        }
    }

    public StockLedgerEntry(StockOperation operation, Product product, OperationType operationType,
                            com.stocksense.warehouse.Location location,
                            BigDecimal quantityChange, BigDecimal previousQuantity, BigDecimal resultingQuantity) {
        this.operation = operation;
        this.product = product;
        this.operationType = operationType;
        this.location = location;
        if (location != null) {
            this.warehouse = location.getWarehouse();
        }
        this.quantityChange = quantityChange;
        this.previousQuantity = previousQuantity;
        this.resultingQuantity = resultingQuantity;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StockOperation getOperation() {
        return operation;
    }

    public void setOperation(StockOperation operation) {
        this.operation = operation;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public BigDecimal getPreviousQuantity() {
        return previousQuantity;
    }

    public void setPreviousQuantity(BigDecimal previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    public BigDecimal getResultingQuantity() {
        return resultingQuantity;
    }

    public void setResultingQuantity(BigDecimal resultingQuantity) {
        this.resultingQuantity = resultingQuantity;
    }

    public com.stocksense.warehouse.Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(com.stocksense.warehouse.Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public com.stocksense.warehouse.Location getLocation() {
        return location;
    }

    public void setLocation(com.stocksense.warehouse.Location location) {
        this.location = location;
    }

    public com.stocksense.warehouse.Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(com.stocksense.warehouse.Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public com.stocksense.warehouse.Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(com.stocksense.warehouse.Location destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
