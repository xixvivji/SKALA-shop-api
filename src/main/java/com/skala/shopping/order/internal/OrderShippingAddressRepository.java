package com.skala.shopping.order.internal;

import com.skala.shopping.order.internal.domain.OrderShippingAddress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderShippingAddressRepository extends JpaRepository<OrderShippingAddress, UUID> { }
