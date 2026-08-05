package com.skala.shopping.inventory;

import java.time.Instant;
import java.util.UUID;

public final class StockMovementView {
    private final UUID id,operationId,productId; private final String type; private final int quantity,availableAfter;
    private final boolean activeAfter; private final String reason; private final Instant createdAt;
    public StockMovementView(UUID id,UUID operationId,UUID productId,String type,int quantity,int availableAfter,
                             boolean activeAfter,String reason,Instant createdAt){this.id=id;this.operationId=operationId;
        this.productId=productId;this.type=type;this.quantity=quantity;this.availableAfter=availableAfter;
        this.activeAfter=activeAfter;this.reason=reason;this.createdAt=createdAt;}
    public UUID getId(){return id;} public UUID getOperationId(){return operationId;} public UUID getProductId(){return productId;}
    public String getType(){return type;} public int getQuantity(){return quantity;} public int getAvailableAfter(){return availableAfter;}
    public boolean isActiveAfter(){return activeAfter;} public String getReason(){return reason;} public Instant getCreatedAt(){return createdAt;}
}
