package com.skala.shopping.wallet.internal.domain;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.wallet.WalletBalance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_accounts", schema = "wallet")
public class PointAccount {

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PointAccount() {
    }

    public PointAccount(UUID memberId, BigDecimal balance, Instant now) {
        this.memberId = memberId;
        this.balance = balance;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void debit(BigDecimal amount, Instant now) {
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        balance = balance.subtract(amount);
        updatedAt = now;
    }

    public void credit(BigDecimal amount, Instant now) {
        balance = balance.add(amount);
        updatedAt = now;
    }

    public WalletBalance toBalance() {
        return new WalletBalance(memberId, balance);
    }
}
