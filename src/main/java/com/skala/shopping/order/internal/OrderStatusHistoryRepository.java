package com.skala.shopping.order.internal;

import com.skala.shopping.order.internal.domain.OrderStatusHistory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> { }
