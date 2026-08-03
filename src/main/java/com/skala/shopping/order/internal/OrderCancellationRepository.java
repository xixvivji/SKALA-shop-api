package com.skala.shopping.order.internal;

import com.skala.shopping.order.internal.domain.OrderCancellation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderCancellationRepository extends JpaRepository<OrderCancellation, UUID> {

    Optional<OrderCancellation> findByCommandId(UUID commandId);
}
