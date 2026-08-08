package com.skala.shopping.payment.internal;

import com.skala.shopping.payment.internal.domain.Payment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByMemberIdAndPrepareCommandId(UUID memberId, UUID commandId);
    Optional<Payment> findByMemberIdAndOrderId(UUID memberId, UUID orderId);
    Page<Payment> findAllByMemberId(UUID memberId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
}
