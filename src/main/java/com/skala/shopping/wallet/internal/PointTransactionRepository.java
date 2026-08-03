package com.skala.shopping.wallet.internal;

import com.skala.shopping.wallet.internal.domain.PointTransaction;
import com.skala.shopping.wallet.internal.domain.PointTransactionType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    Optional<PointTransaction> findByMemberIdAndCommandIdAndTransactionType(
            UUID memberId,
            UUID commandId,
            PointTransactionType transactionType
    );
}
