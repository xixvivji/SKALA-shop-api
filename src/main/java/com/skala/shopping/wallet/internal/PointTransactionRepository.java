package com.skala.shopping.wallet.internal;

import com.skala.shopping.wallet.internal.domain.PointTransaction;
import com.skala.shopping.wallet.internal.domain.PointTransactionType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    Page<PointTransaction> findAllByMemberId(UUID memberId, Pageable pageable);

    Optional<PointTransaction> findByMemberIdAndCommandIdAndTransactionType(
            UUID memberId,
            UUID commandId,
            PointTransactionType transactionType
    );
}
