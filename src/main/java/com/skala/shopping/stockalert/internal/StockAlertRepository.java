package com.skala.shopping.stockalert.internal;

import com.skala.shopping.stockalert.internal.domain.StockAlertSubscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockAlertRepository extends JpaRepository<StockAlertSubscription, UUID> {

    Optional<StockAlertSubscription> findByMemberIdAndProductId(UUID memberId, UUID productId);

    Page<StockAlertSubscription> findByMemberIdOrderByCreatedAtDescIdDesc(UUID memberId, Pageable pageable);
}
