package com.skala.shopping.inventory.internal;

import com.skala.shopping.inventory.internal.domain.StockMovement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Optional<StockMovement> findByOperationIdAndProductId(
            UUID operationId,
            UUID productId
    );
}
