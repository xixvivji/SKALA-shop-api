package com.skala.shopping.payment.internal;

import com.skala.shopping.payment.internal.domain.PaymentRefund;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    Optional<PaymentRefund> findByCommandId(UUID commandId);
}
