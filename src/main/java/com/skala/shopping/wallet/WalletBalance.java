package com.skala.shopping.wallet;

import java.math.BigDecimal;
import java.util.UUID;

public final class WalletBalance {

    private final UUID memberId;
    private final BigDecimal balance;

    public WalletBalance(UUID memberId, BigDecimal balance) {
        this.memberId = memberId;
        this.balance = balance;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
