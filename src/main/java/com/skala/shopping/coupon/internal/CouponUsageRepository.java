package com.skala.shopping.coupon.internal;

import com.skala.shopping.coupon.internal.domain.CouponUsage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    boolean existsByMemberIdAndCouponCode(UUID memberId, String couponCode);
}
