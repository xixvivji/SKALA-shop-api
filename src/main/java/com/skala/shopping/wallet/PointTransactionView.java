package com.skala.shopping.wallet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PointTransactionView {
    private final UUID id; private final String type; private final BigDecimal amount;
    private final BigDecimal balanceAfter; private final UUID referenceId; private final Instant createdAt;
    public PointTransactionView(UUID id,String type,BigDecimal amount,BigDecimal balanceAfter,
                                UUID referenceId,Instant createdAt){this.id=id;this.type=type;this.amount=amount;
        this.balanceAfter=balanceAfter;this.referenceId=referenceId;this.createdAt=createdAt;}
    public UUID getId(){return id;} public String getType(){return type;} public BigDecimal getAmount(){return amount;}
    public BigDecimal getBalanceAfter(){return balanceAfter;} public UUID getReferenceId(){return referenceId;}
    public Instant getCreatedAt(){return createdAt;}
}
