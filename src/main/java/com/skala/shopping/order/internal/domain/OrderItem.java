package com.skala.shopping.order.internal.domain;

import com.skala.shopping.order.OrderItemView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "orders")
public class OrderItem {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "ordered_quantity", nullable = false)
    private int orderedQuantity;

    @Column(name = "canceled_quantity", nullable = false)
    private int canceledQuantity;

    protected OrderItem() {
    }

    public OrderItem(
            UUID orderId,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int orderedQuantity
    ) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.orderedQuantity = orderedQuantity;
        this.canceledQuantity = 0;
    }

    public UUID orderId() {
        return orderId;
    }

    public UUID productId() {
        return productId;
    }

    public String productName() {
        return productName;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public int availableQuantity() {
        return orderedQuantity - canceledQuantity;
    }

    public BigDecimal cancel(int quantity) {
        if (quantity <= 0 || quantity > availableQuantity()) {
            throw new IllegalArgumentException("Invalid cancellation quantity");
        }
        canceledQuantity += quantity;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public OrderItemView toView() {
        return new OrderItemView(
                id,
                productId,
                productName,
                unitPrice,
                orderedQuantity,
                canceledQuantity
        );
    }

    public OrderItemView toCreationView() {
        return new OrderItemView(
                id,
                productId,
                productName,
                unitPrice,
                orderedQuantity,
                0
        );
    }
}
