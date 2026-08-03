package com.skala.shopping.wallet.internal;

import com.skala.shopping.wallet.internal.domain.PointTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    boolean existsByCommandId(UUID commandId);
}
