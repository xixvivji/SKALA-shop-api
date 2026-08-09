package com.skala.shopping.search.internal;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 공개 검색 API와 독립 Search Service 사이의 gateway입니다.
 *
 * <p>검색 서비스가 일시적으로 중단되어도 상품 탐색이 완전히 막히지 않도록 읽기 요청은
 * PostgreSQL 카탈로그 조회로 폴백합니다. 색인 재생성처럼 Search Service가 실제로 수행해야
 * 하는 관리 작업은 실패를 숨기지 않고 503으로 반환합니다.</p>
 */
@Service
@ConditionalOnProperty(name = "shopping.search.enabled", havingValue = "true")
class ProductSearchApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchApplicationService.class);

    private final SearchServiceClient client;
    private final CatalogApi catalog;

    ProductSearchApplicationService(SearchServiceClient client, CatalogApi catalog) {
        this.client = client;
        this.catalog = catalog;
    }

    PageResponse<ProductSearchResponse> search(String query, int page, int size) {
        try {
            SearchServiceClient.SearchPage result = client.search(query, page, size);
            return new PageResponse<>(
                    result.getContent().stream().map(ProductSearchResponse::new).toList(),
                    result.getPage(),
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages()
            );
        } catch (RuntimeException exception) {
            log.warn("product_search_service_fallback_to_catalog query={} page={} size={}",
                    query, page, size, exception);
            var fallback = catalog.searchProducts(query, null, null, null, page, size);
            return new PageResponse<>(
                    fallback.getContent().stream().map(ProductSearchResponse::new).toList(),
                    fallback.getPage(),
                    fallback.getSize(),
                    fallback.getTotalElements(),
                    fallback.getTotalPages()
            );
        }
    }

    long reindex() {
        try {
            return client.reindex();
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    ErrorCode.UPSTREAM_UNAVAILABLE,
                    "검색 서비스가 응답하지 않아 색인을 재생성하지 못했습니다."
            );
        }
    }

    String openApi() {
        try {
            return client.openApi();
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    ErrorCode.UPSTREAM_UNAVAILABLE,
                    "검색 서비스 API 명세를 불러오지 못했습니다."
            );
        }
    }
}
