package com.skala.shopping.review.internal;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.review.ReviewApi;
import com.skala.shopping.review.ReviewResponse;
import com.skala.shopping.review.internal.domain.ProductReview;
import com.skala.shopping.order.OrderApi;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewApplicationService implements ReviewApi {

    private final ProductReviewRepository repository;
    private final CatalogApi catalogApi;
    private final OrderApi orderApi;
    private final Clock clock = Clock.systemUTC();

    ReviewApplicationService(
            ProductReviewRepository repository,
            CatalogApi catalogApi,
            OrderApi orderApi
    ) {
        this.repository = repository;
        this.catalogApi = catalogApi;
        this.orderApi = orderApi;
    }

    @Override
    @Transactional
    public ReviewResponse writeReview(UUID memberId, UUID productId, int rating, String comment) {
        catalogApi.getSaleableProduct(productId);
        if (!orderApi.hasPurchasedProduct(memberId, productId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "구매한 상품만 리뷰를 작성할 수 있습니다.");
        }
        var existing = repository.findByMemberIdAndProductId(memberId, productId);
        if (existing.isEmpty()) {
            ProductReview created = repository.save(new ProductReview(
                    memberId,
                    productId,
                    rating,
                    normalizeComment(comment),
                    clock.instant()
            ));
            return created.toResponse();
        }
        ProductReview review = existing.get();
        review.update(rating, normalizeComment(comment), clock.instant());
        return repository.save(review).toResponse();
    }

    @Override
    @Transactional
    public void deleteReview(UUID memberId, UUID productId) {
        var review = repository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "리뷰를 찾을 수 없습니다."));
        repository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(UUID memberId, UUID productId) {
        return repository.findByMemberIdAndProductId(memberId, productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "리뷰를 찾을 수 없습니다."))
                .toResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getProductReviews(UUID productId, int page, int size) {
        catalogApi.getSaleableProduct(productId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(
                repository.findByProductIdOrderByCreatedAtDescIdDesc(productId, pageable)
                        .map(ProductReview::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(UUID memberId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(
                repository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId, pageable)
                        .map(ProductReview::toResponse)
        );
    }

    private String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }
}
