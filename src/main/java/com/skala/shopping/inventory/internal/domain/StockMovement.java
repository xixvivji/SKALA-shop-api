package com.skala.shopping.inventory.internal.domain;

import com.skala.shopping.inventory.StockBalance;
import com.skala.shopping.inventory.StockMovementView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stock_movements",
        schema = "inventory",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_movements_operation_product",
                columnNames = {"operation_id", "product_id"}
        )
)
public class StockMovement {

    @Id
    private UUID id;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "available_after", nullable = false)
    private int availableAfter;

    @Column(name = "active_after", nullable = false)
    private boolean activeAfter;

    @Column(name = "request_fingerprint", nullable = false, length = 512)
    private String requestFingerprint;

    @Column(length = 200)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(
            UUID operationId,
            UUID productId,
            StockMovementType movementType,
            int quantity,
            int availableAfter,
            boolean activeAfter,
            String requestFingerprint,
            String reason,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.operationId = operationId;
        this.productId = productId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.availableAfter = availableAfter;
        this.activeAfter = activeAfter;
        this.requestFingerprint = requestFingerprint;
        this.reason = reason;
        this.createdAt = now;
    }

    public boolean hasFingerprint(String fingerprint) {
        return requestFingerprint.equals(fingerprint);
    }

    public StockBalance toBalance() {
        return new StockBalance(productId, availableAfter, activeAfter);
    }

    public StockMovementView toView(){return new StockMovementView(id,operationId,productId,movementType.name(),
            quantity,availableAfter,activeAfter,reason,createdAt);}
}
