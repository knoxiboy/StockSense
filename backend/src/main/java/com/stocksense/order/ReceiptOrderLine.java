package com.stocksense.order;

import com.stocksense.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "receipt_order_lines", indexes = {
        @Index(name = "idx_receipt_line_order", columnList = "receipt_order_id"),
        @Index(name = "idx_receipt_line_product", columnList = "product_id")
})
public class ReceiptOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_order_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private ReceiptOrder receiptOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Product product;

    @Column(name = "expected_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal expectedQuantity;

    @Column(name = "received_quantity", precision = 18, scale = 4)
    private BigDecimal receivedQuantity;

    public ReceiptOrderLine() {
    }

    public ReceiptOrderLine(ReceiptOrder receiptOrder, Product product, BigDecimal expectedQuantity, BigDecimal receivedQuantity) {
        this.receiptOrder = receiptOrder;
        this.product = product;
        this.expectedQuantity = expectedQuantity;
        this.receivedQuantity = receivedQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReceiptOrder getReceiptOrder() {
        return receiptOrder;
    }

    public void setReceiptOrder(ReceiptOrder receiptOrder) {
        this.receiptOrder = receiptOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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
