package com.skala.shopping.searchservice.api;

import com.skala.shopping.searchservice.common.ApiError;
import com.skala.shopping.searchservice.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "검색 서비스", description = "Kafka 이벤트 기반 Elasticsearch 상품 검색")
public class ProductSearchController {

    private final ProductSearchService service;

    public ProductSearchController(ProductSearchService service) {
        this.service = service;
    }

    @GetMapping("/internal/search/products")
    @Operation(
            summary = "상품 검색",
            description = "Elasticsearch 읽기 모델에서 상품명과 설명을 검색합니다."
    )
    @ApiResponse(responseCode = "200", description = "상품 검색 성공")
    @ApiResponse(
            responseCode = "400",
            description = "검색어 또는 페이지 요청값 오류",
            content = @Content(schema = @Schema(implementation = ApiError.class))
    )
    public PageResponse<ProductSearchResponse> search(
            @RequestParam @NotBlank @Size(max = 100) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return service.search(query.trim(), page, size);
    }

    @PostMapping("/internal/search/reindex")
    @Operation(
            summary = "상품 검색 색인 재생성",
            description = "Backend의 공개 Catalog API snapshot으로 Elasticsearch 색인을 복구합니다."
    )
    @ApiResponse(responseCode = "200", description = "색인 재생성 성공")
    public ReindexResponse reindex() {
        return new ReindexResponse(service.reindex());
    }
}
