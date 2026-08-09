package com.skala.shopping.search.internal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영 Swagger UI가 독립 Search Service의 OpenAPI 문서를 함께 표시하도록 전달합니다. */
@RestController
@ConditionalOnProperty(name = "shopping.search.enabled", havingValue = "true")
class SearchDocumentationController {

    private final ProductSearchApplicationService service;

    SearchDocumentationController(ProductSearchApplicationService service) {
        this.service = service;
    }

    @GetMapping(value = "/v3/api-docs/search", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> openApi() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.openApi());
    }
}
