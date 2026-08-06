package com.skala.shopping.order.internal;

import com.skala.shopping.order.internal.domain.OrderShippingAddress;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderShippingAddressRepository extends JpaRepository<OrderShippingAddress, UUID> {
    List<OrderShippingAddress> findAllByOrderIdIn(Collection<UUID> orderIds);
}
