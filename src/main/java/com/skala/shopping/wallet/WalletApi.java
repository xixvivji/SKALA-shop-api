package com.skala.shopping.wallet;

import java.math.BigDecimal;
import java.util.UUID;
import com.skala.shopping.common.PageResponse;

public interface WalletApi {

    WalletBalance openAccount(
            UUID memberId,
            BigDecimal initialBalance,
            UUID referenceId,
            UUID commandId
    );

    WalletBalance getBalance(UUID memberId);

    WalletBalance debit(UUID memberId, BigDecimal amount, UUID referenceId, UUID commandId);

    WalletBalance credit(UUID memberId, BigDecimal amount, UUID referenceId, UUID commandId);

    PageResponse<PointTransactionView> getTransactions(UUID memberId, int page, int size);
}
