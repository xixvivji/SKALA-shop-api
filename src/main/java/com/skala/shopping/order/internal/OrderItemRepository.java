package com.skala.shopping.order.internal;

import com.skala.shopping.order.internal.domain.OrderItem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrderId(UUID orderId);

    List<OrderItem> findAllByOrderIdIn(Collection<UUID> orderIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from OrderItem item
            join ShopOrder shopOrder on shopOrder.id = item.orderId
            where shopOrder.memberId = :memberId
              and item.productId = :productId
              and item.canceledQuantity < item.orderedQuantity
            order by shopOrder.orderedAt desc, shopOrder.id desc, item.id asc
            """)
    List<OrderItem> findCancelableItems(
            @Param("memberId") UUID memberId,
            @Param("productId") UUID productId
    );

    @Query("""
            select item
            from OrderItem item
            join ShopOrder shopOrder on shopOrder.id = item.orderId
            where shopOrder.memberId = :memberId
              and item.canceledQuantity < item.orderedQuantity
            order by shopOrder.orderedAt desc
            """)
    List<OrderItem> findPurchasedItems(@Param("memberId") UUID memberId);
}
