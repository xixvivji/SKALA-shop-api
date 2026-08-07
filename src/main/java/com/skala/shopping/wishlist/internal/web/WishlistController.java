package com.skala.shopping.wishlist.internal.web;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.wishlist.WishlistApi;
import com.skala.shopping.wishlist.WishlistItemView;
import com.skala.shopping.wishlist.internal.web.dto.request.WishlistItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "위시리스트", description = "상품 위시리스트")
@SecurityRequirement(name = "cookieAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "인증 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "고객 권한 필요",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
})
class WishlistController {

    private final WishlistApi wishlistApi;

    WishlistController(WishlistApi wishlistApi) {
        this.wishlistApi = wishlistApi;
    }

    @GetMapping
    @Operation(summary = "위시리스트 조회")
    List<WishlistItemView> get(@AuthenticationPrincipal Jwt jwt) {
        return wishlistApi.getWishlist(memberId(jwt));
    }

    @PostMapping
    @Operation(summary = "위시리스트에 상품 추가")
    WishlistItemView add(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WishlistItemRequest request
    ) {
        return wishlistApi.addItem(memberId(jwt), request.getProductId());
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "위시리스트에서 상품 삭제")
    void remove(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        wishlistApi.removeItem(memberId(jwt), productId);
    }

    private UUID memberId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.NOT_AUTHENTICATED);
        }
    }
}
