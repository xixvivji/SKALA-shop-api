package com.skala.shopping.catalog.internal.web;

import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.catalog.internal.ProductApplicationService;
import com.skala.shopping.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
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
    PageResponse<ProductSnapshot> getProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return service.getProducts(page, size);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 단건 조회")
    ProductSnapshot getProduct(@PathVariable UUID productId) {
        return service.getProduct(productId);
    }

    @PostMapping
    @Operation(
            summary = "상품 등록",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    ResponseEntity<ProductSnapshot> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductSnapshot product = service.createProduct(
                request.getProductName(),
                request.getProductPrice()
        );
        return ResponseEntity.created(URI.create("/api/products/" + product.getId())).body(product);
    }

    @PutMapping("/{productId}")
    @Operation(
            summary = "상품 수정",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    ProductSnapshot updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request
    ) {
        return service.updateProduct(
                productId,
                request.getProductName(),
                request.getProductPrice()
        );
    }

    @DeleteMapping("/{productId}")
    @Operation(
            summary = "상품 삭제",
            security = {@SecurityRequirement(name = "cookieAuth")}
    )
    ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        service.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }
}
