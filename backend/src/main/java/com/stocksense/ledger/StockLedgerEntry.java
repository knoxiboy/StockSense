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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
