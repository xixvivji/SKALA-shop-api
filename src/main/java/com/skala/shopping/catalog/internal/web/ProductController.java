package com.skala.shopping.catalog.internal.web;

import com.skala.shopping.catalog.internal.ProductApplicationService;
import com.skala.shopping.catalog.internal.web.dto.request.CreateProductRequest;
import com.skala.shopping.catalog.internal.web.dto.request.UpdateProductRequest;
import com.skala.shopping.catalog.internal.web.dto.request.CreateProductVariantRequest;
import com.skala.shopping.catalog.internal.web.dto.response.ProductResponse;
import com.skala.shopping.catalog.internal.web.dto.response.ProductVariantResponse;
import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "상품", description = "상품 조회와 관리")
class ProductController {

    private final ProductApplicationService service;

    ProductController(ProductApplicationService service) {
        this.service = service;
    }

    @GetMapping({"", "/list"})
    @Operation(summary = "상품 목록 조회")
    PageResponse<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return ProductResponse.pageFrom(service.searchProducts(
                query, categoryId, minPrice, maxPrice, page, size));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 단건 조회")
    ProductResponse getProduct(@PathVariable UUID productId) {
        return ProductResponse.from(service.getProduct(productId));
    }

    @GetMapping("/{productId}/variants")
    @Operation(summary = "상품 옵션 목록 조회")
    List<ProductVariantResponse> getVariants(@PathVariable UUID productId) {
        return service.getVariants(productId).stream().map(ProductVariantResponse::new).toList();
    }

    @PostMapping("/{productId}/variants")
    @Operation(summary = "상품 옵션 등록", security = {@SecurityRequirement(name = "cookieAuth")})
    ResponseEntity<ProductVariantResponse> createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request
    ) {
        ProductVariantResponse response = new ProductVariantResponse(service.createVariant(
                productId, request.getSku(), request.getOptionName(), request.getOptionValue(),
                request.getAdditionalPrice(), request.getInitialQuantity()));
        return ResponseEntity.created(URI.create("/api/products/" + productId + "/variants/" + response.getId()))
                .body(response);
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @Operation(summary = "상품 옵션 삭제", security = {@SecurityRequirement(name = "cookieAuth")})
    ResponseEntity<Void> deleteVariant(@PathVariable UUID productId, @PathVariable UUID variantId) {
        service.deleteVariant(productId, variantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(
            summary = "상품 등록",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "등록된 상품",
                            content = @Content(schema = @Schema(implementation = ProductResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "상품 입력값 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "관리자 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "상품명 중복",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse product = ProductResponse.from(service.createProduct(
                request.getProductName(),
                request.getProductPrice(),
                request.getInitialQuantity(), request.getCategoryId(),
                request.getDescription(), request.getImageUrl()
        ));
        return ResponseEntity.created(URI.create("/api/products/" + product.getId())).body(product);
    }

    @PutMapping("/{productId}")
    @Operation(
            summary = "상품 수정",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "수정된 상품",
                            content = @Content(schema = @Schema(implementation = ProductResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "상품 ID 또는 입력값 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "관리자 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "상품을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "상품명 중복",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ProductResponse updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ProductResponse.from(service.updateProduct(
                productId,
                request.getProductName(),
                request.getProductPrice(), request.getCategoryId(),
                request.getDescription(), request.getImageUrl()
        ));
    }

    @DeleteMapping("/{productId}")
    @Operation(
            summary = "상품 삭제",
            security = {@SecurityRequirement(name = "cookieAuth")},
            responses = {
                    @ApiResponse(responseCode = "204", description = "상품 삭제 완료"),
                    @ApiResponse(
                            responseCode = "400",
                            description = "상품 ID 오류",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "관리자 권한 또는 CSRF 토큰 필요",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "상품을 찾을 수 없음",
                            content = @Content(schema = @Schema(implementation = ApiError.class))
                    )
            }
    )
    ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        service.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
