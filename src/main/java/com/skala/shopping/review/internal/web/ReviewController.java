package com.skala.shopping.review.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.review.ReviewApi;
import com.skala.shopping.review.ReviewResponse;
import com.skala.shopping.review.internal.web.dto.request.ReviewCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "상품 리뷰", description = "상품 리뷰 작성/조회")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "고객 권한 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class ReviewController {

    private final ReviewApi reviewApi;

    ReviewController(ReviewApi reviewApi) {
        this.reviewApi = reviewApi;
    }

    @PostMapping("/api/products/{productId}/reviews")
    @Operation(summary = "상품 리뷰 등록/수정", description = "구매한 상품의 평점과 내용을 등록하거나 기존 리뷰를 수정합니다.")
    ReviewResponse write(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return reviewApi.writeReview(memberId(jwt), productId, request.getRating(), request.getComment());
    }

    @GetMapping("/api/products/{productId}/reviews/me")
    @Operation(summary = "내 리뷰 조회", description = "로그인한 고객이 해당 상품에 작성한 리뷰를 조회합니다.")
    ReviewResponse getMine(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        return reviewApi.getMyReview(memberId(jwt), productId);
    }

    @GetMapping("/api/products/{productId}/reviews")
    @Operation(summary = "상품 리뷰 목록", description = "상품에 등록된 리뷰를 최신순으로 페이지 조회합니다.")
    PageResponse<ReviewResponse> listByProduct(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return reviewApi.getProductReviews(productId, page, size);
    }

    @DeleteMapping("/api/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "내 리뷰 삭제", description = "로그인한 고객이 해당 상품에 작성한 리뷰를 삭제합니다.")
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID productId) {
        reviewApi.deleteReview(memberId(jwt), productId);
    }

    @GetMapping("/api/reviews/me")
    @Operation(summary = "내 리뷰 목록", description = "로그인한 고객이 작성한 모든 리뷰를 페이지 조회합니다.")
    PageResponse<ReviewResponse> listMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return reviewApi.getMyReviews(memberId(jwt), page, size);
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
