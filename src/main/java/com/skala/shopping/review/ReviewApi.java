package com.skala.shopping.review;

import com.skala.shopping.common.PageResponse;
import java.util.UUID;

public interface ReviewApi {

    ReviewResponse writeReview(UUID memberId, UUID productId, int rating, String comment);

    void deleteReview(UUID memberId, UUID productId);

    ReviewResponse getMyReview(UUID memberId, UUID productId);

    PageResponse<ReviewResponse> getProductReviews(UUID productId, int page, int size);

    PageResponse<ReviewResponse> getMyReviews(UUID memberId, int page, int size);
}
