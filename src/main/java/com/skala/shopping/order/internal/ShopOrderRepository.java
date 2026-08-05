package com.skala.shopping.order.internal;

import com.skala.shopping.order.internal.domain.ShopOrder;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ShopOrderRepository extends JpaRepository<ShopOrder, UUID> {

    Optional<ShopOrder> findByMemberIdAndRequestId(UUID memberId, UUID requestId);

    Page<ShopOrder> findAllByMemberId(UUID memberId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shopOrder from ShopOrder shopOrder where shopOrder.id = :orderId")
    Optional<ShopOrder> findByIdForUpdate(@Param("orderId") UUID orderId);
}
