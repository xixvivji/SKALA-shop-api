package com.skala.shopping.review.internal;

import com.skala.shopping.review.internal.domain.ProductReview;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    Optional<ProductReview> findByMemberIdAndProductId(UUID memberId, UUID productId);

    Page<ProductReview> findByProductIdOrderByCreatedAtDescIdDesc(UUID productId, Pageable pageable);

    Page<ProductReview> findByMemberIdOrderByCreatedAtDescIdDesc(UUID memberId, Pageable pageable);
}
