package com.skala.shopping.wallet.internal.domain;

import com.skala.shopping.wallet.WalletBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "point_transactions",
        schema = "wallet",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_point_transactions_member_command_type",
                columnNames = {"member_id", "command_id", "transaction_type"}
        )
)
public class PointTransaction {

    @Id
    private UUID id;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private PointTransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "command_id", nullable = false)
    private UUID commandId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PointTransaction() {
    }

    public PointTransaction(
            UUID memberId,
            PointTransactionType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            UUID referenceId,
            UUID commandId,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.memberId = memberId;
        this.transactionType = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.commandId = commandId;
        this.createdAt = now;
    }

    public boolean hasAmount(BigDecimal expectedAmount) {
        return amount.compareTo(expectedAmount) == 0;
    }

    public WalletBalance toBalance() {
        return new WalletBalance(memberId, balanceAfter);
    }
}
