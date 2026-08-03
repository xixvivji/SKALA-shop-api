package com.skala.shopping.order.internal;

import com.skala.shopping.wallet.WalletApi;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LocalPointManager implements PointManager {

    private final WalletApi walletApi;

    LocalPointManager(WalletApi walletApi) {
        this.walletApi = walletApi;
    }

    @Override
    public BigDecimal debit(
            UUID memberId,
            BigDecimal amount,
            UUID referenceId,
            UUID commandId
    ) {
        return walletApi.debit(memberId, amount, referenceId, commandId).getBalance();
    }

    @Override
    public BigDecimal credit(
            UUID memberId,
            BigDecimal amount,
            UUID referenceId,
            UUID commandId
    ) {
        return walletApi.credit(memberId, amount, referenceId, commandId).getBalance();
    }
}
