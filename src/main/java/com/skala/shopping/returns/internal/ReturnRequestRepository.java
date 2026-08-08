package com.skala.shopping.returns.internal;

import com.skala.shopping.returns.internal.domain.ReturnRequest;
import com.skala.shopping.returns.internal.domain.ReturnStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
    Optional<ReturnRequest> findByCommandId(UUID commandId);
    Page<ReturnRequest> findAllByMemberId(UUID memberId, Pageable pageable);

    @Query("""
            select coalesce(sum(request.quantity), 0)
            from ReturnRequest request
            where request.orderItemId = :orderItemId and request.status in :statuses
            """)
    long sumQuantityByOrderItemIdAndStatusIn(
            @Param("orderItemId") UUID orderItemId,
            @Param("statuses") Collection<ReturnStatus> statuses
    );

    @Query("""
            select coalesce(sum(request.grossRefundAmount), 0)
            from ReturnRequest request
            where request.orderItemId = :orderItemId and request.status in :statuses
            """)
    BigDecimal sumGrossRefundAmountByOrderItemIdAndStatusIn(
            @Param("orderItemId") UUID orderItemId,
            @Param("statuses") Collection<ReturnStatus> statuses
    );

    @Query("""
            select coalesce(sum(request.shippingFee), 0)
            from ReturnRequest request
            where request.orderItemId = :orderItemId and request.status = :status
            """)
    BigDecimal sumShippingFeeByOrderItemIdAndStatus(
            @Param("orderItemId") UUID orderItemId,
            @Param("status") ReturnStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from ReturnRequest request where request.id=:id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") UUID id);
}
