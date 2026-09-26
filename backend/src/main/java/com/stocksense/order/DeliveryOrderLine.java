package com.stocksense.order;

import com.stocksense.product.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "delivery_order_lines", indexes = {
        @Index(name = "idx_delivery_line_order", columnList = "delivery_order_id"),
        @Index(name = "idx_delivery_line_product", columnList = "product_id")
})
public class DeliveryOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private DeliveryOrder deliveryOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Product product;

    @Column(name = "ordered_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal orderedQuantity;

    @Column(name = "delivered_quantity", precision = 18, scale = 4)
    private BigDecimal deliveredQuantity;

    @Column(nullable = false)
    private Boolean picked = false;

    @Column(nullable = false)
    private Boolean packed = false;

    public DeliveryOrderLine() {
    }

    public DeliveryOrderLine(DeliveryOrder deliveryOrder, Product product, BigDecimal orderedQuantity, BigDecimal deliveredQuantity) {
        this.deliveryOrder = deliveryOrder;
        this.product = product;
        this.orderedQuantity = orderedQuantity;
        this.deliveredQuantity = deliveredQuantity;
        this.picked = false;
        this.packed = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DeliveryOrder getDeliveryOrder() {
        return deliveryOrder;
    }

    public void setDeliveryOrder(DeliveryOrder deliveryOrder) {
        this.deliveryOrder = deliveryOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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
        this.picked = picked != null ? picked : false;
    }

    public Boolean getPacked() {
        return packed;
    }

    public void setPacked(Boolean packed) {
        this.packed = packed != null ? packed : false;
    }
}
