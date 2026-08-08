package com.skala.shopping.search.internal;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "shopping.search.enabled", havingValue = "true")
@Tag(name = "상품 검색", description = "Elasticsearch 상품 검색과 관리자 색인 관리")
class ProductSearchController {

    private final ProductSearchApplicationService service;

    ProductSearchController(ProductSearchApplicationService service) {
        this.service = service;
    }

    @GetMapping("/api/search/products")
    @Operation(
            summary = "상품 검색",
            description = "검색어와 페이지 조건으로 판매 중인 상품을 검색합니다."
    )
    @ApiResponse(responseCode = "400", description = "검색어 또는 페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<ProductSearchResponse> search(
            @RequestParam @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.search(query.trim(), page, size);
    }

    @PostMapping("/api/admin/search/reindex")
    @Operation(
            summary = "상품 검색 색인 재생성",
            description = "관리자가 PostgreSQL의 전체 상품을 Elasticsearch 색인에 다시 반영합니다.",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    @ApiResponse(responseCode = "403", description = "관리자 권한 또는 CSRF 토큰 필요",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    Map<String, Long> reindex() {
        return Map.of("indexed", service.reindex());
    }
}
