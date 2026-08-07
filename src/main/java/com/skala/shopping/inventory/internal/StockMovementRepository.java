package com.skala.shopping.inventory.internal;

import com.skala.shopping.inventory.internal.domain.StockMovement;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findAllByProductId(UUID productId, Pageable pageable);

    Optional<StockMovement> findByOperationIdAndProductId(
            UUID operationId,
            UUID productId
    );
}
