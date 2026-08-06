package com.skala.shopping.order;

import java.time.Instant;
import java.util.UUID;

public final class OrderStatusHistoryView {
    private final UUID id; private final String fromStatus,toStatus; private final UUID changedBy; private final Instant changedAt;
    public OrderStatusHistoryView(UUID id,String fromStatus,String toStatus,UUID changedBy,Instant changedAt){
        this.id=id;this.fromStatus=fromStatus;this.toStatus=toStatus;this.changedBy=changedBy;this.changedAt=changedAt;}
    public UUID getId(){return id;} public String getFromStatus(){return fromStatus;} public String getToStatus(){return toStatus;}
    public UUID getChangedBy(){return changedBy;} public Instant getChangedAt(){return changedAt;}
}
