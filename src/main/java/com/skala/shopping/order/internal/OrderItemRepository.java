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

    List<OrderItem> findAllByOrderIdOrderByIdAsc(UUID orderId);

    List<OrderItem> findAllByOrderIdOrderByLineNumberAsc(UUID orderId);

    List<OrderItem> findAllByOrderIdInOrderByOrderIdAscIdAsc(Collection<UUID> orderIds);

    List<OrderItem> findAllByOrderIdInOrderByOrderIdAscLineNumberAsc(Collection<UUID> orderIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from OrderItem item where item.id = :itemId and item.orderId = :orderId")
    java.util.Optional<OrderItem> findByIdAndOrderIdForUpdate(
            @Param("itemId") UUID itemId, @Param("orderId") UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from OrderItem item
            join ShopOrder shopOrder on shopOrder.id = item.orderId
            where shopOrder.memberId = :memberId
              and shopOrder.status in (com.skala.shopping.order.internal.domain.OrderStatus.PAID,
                                       com.skala.shopping.order.internal.domain.OrderStatus.PARTIALLY_CANCELED)
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
              and shopOrder.status in (com.skala.shopping.order.internal.domain.OrderStatus.PAID,
                                       com.skala.shopping.order.internal.domain.OrderStatus.PARTIALLY_CANCELED)
              and item.canceledQuantity < item.orderedQuantity
            order by shopOrder.orderedAt desc, shopOrder.id desc, item.id asc
            """)
    List<OrderItem> findPurchasedItems(@Param("memberId") UUID memberId);

    @Query("""
            select (count(item) > 0)
            from OrderItem item
            join ShopOrder shopOrder on shopOrder.id = item.orderId
            where shopOrder.memberId = :memberId
              and shopOrder.status in (com.skala.shopping.order.internal.domain.OrderStatus.PAID,
                                       com.skala.shopping.order.internal.domain.OrderStatus.PARTIALLY_CANCELED)
              and item.productId = :productId
              and item.canceledQuantity < item.orderedQuantity
            """)
    boolean hasPurchasedProduct(
            @Param("memberId") UUID memberId,
            @Param("productId") UUID productId
    );
}
