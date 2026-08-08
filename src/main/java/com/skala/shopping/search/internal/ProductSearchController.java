package com.skala.shopping.search.internal;

import com.skala.shopping.common.PageResponse; import jakarta.validation.constraints.Max; import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController @ConditionalOnProperty(name="shopping.search.enabled",havingValue="true")
class ProductSearchController {
    private final ProductSearchApplicationService service; ProductSearchController(ProductSearchApplicationService service){this.service=service;}
    @GetMapping("/api/search/products") PageResponse<ProductSearchResponse> search(
            @RequestParam @NotBlank @Size(max=100) String query,@RequestParam(defaultValue="0") @Min(0) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.search(query.trim(),page,size);}
    @PostMapping("/api/admin/search/reindex") java.util.Map<String,Long> reindex(){return java.util.Map.of("indexed",service.reindex());}
}
