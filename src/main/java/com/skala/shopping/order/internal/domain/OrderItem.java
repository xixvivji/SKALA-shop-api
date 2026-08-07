package com.skala.shopping.order.internal.domain;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.order.OrderItemView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount;

    @Column(name = "ordered_quantity", nullable = false)
    private int orderedQuantity;

    @Column(name = "canceled_quantity", nullable = false)
    private int canceledQuantity;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    protected OrderItem() {
    }

    public OrderItem(
            UUID orderId,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int orderedQuantity
    ) {
        this(orderId, productId, productName, unitPrice, orderedQuantity,
                unitPrice.multiply(BigDecimal.valueOf(orderedQuantity)), 0);
    }

    public OrderItem(
            UUID orderId,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int orderedQuantity,
            int lineNumber
    ) {
        this(orderId, productId, productName, unitPrice, orderedQuantity,
                unitPrice.multiply(BigDecimal.valueOf(orderedQuantity)), lineNumber);
    }

    public OrderItem(
            UUID orderId,
            UUID productId,
            String productName,
            BigDecimal unitPrice,
            int orderedQuantity,
            BigDecimal paidAmount,
            int lineNumber
    ) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.paidAmount = paidAmount.setScale(2, RoundingMode.UNNECESSARY);
        this.refundedAmount = BigDecimal.ZERO.setScale(2);
        this.orderedQuantity = orderedQuantity;
        this.canceledQuantity = 0;
        this.lineNumber = lineNumber;
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
            throw new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY);
        }
        int quantityAfterCancellation = canceledQuantity + quantity;
        BigDecimal refund = quantityAfterCancellation == orderedQuantity
                ? paidAmount.subtract(refundedAmount)
                : paidAmount.multiply(BigDecimal.valueOf(quantity))
                        .divide(BigDecimal.valueOf(orderedQuantity), 2, RoundingMode.DOWN);
        canceledQuantity = quantityAfterCancellation;
        refundedAmount = refundedAmount.add(refund);
        return refund;
    }

    public OrderItemView toView() {
        return new OrderItemView(
                id,
                productId,
                productName,
                unitPrice,
                paidAmount,
                refundedAmount,
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
                paidAmount,
                BigDecimal.ZERO.setScale(2),
                orderedQuantity,
                0
        );
    }
}
