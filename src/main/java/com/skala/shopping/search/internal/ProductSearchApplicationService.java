package com.skala.shopping.search.internal;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductSearchChanged;
import com.skala.shopping.common.PageResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@ConditionalOnProperty(name = "shopping.search.enabled", havingValue = "true")
class ProductSearchApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchApplicationService.class);
    private static final int REINDEX_PAGE_SIZE = 100;

    private final ProductSearchDocumentRepository repository;
    private final CatalogApi catalog;
    private final ElasticsearchOperations operations;

    ProductSearchApplicationService(
            ProductSearchDocumentRepository repository,
            CatalogApi catalog,
            ElasticsearchOperations operations
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.operations = operations;
    }

    @EventListener(ApplicationReadyEvent.class)
    void initializeIndex() {
        try {
            var index = operations.indexOps(ProductSearchDocument.class);
            if (!index.exists()) {
                index.createWithMapping();
            }
            // 새 Elasticsearch 노드는 빈 인덱스만 만든 상태일 수 있으므로 PostgreSQL 원본으로 복구합니다.
            if (repository.count() == 0) {
                long indexed = reindex();
                log.info("product_search_initial_backfill_completed indexed={}", indexed);
            }
        } catch (RuntimeException exception) {
            // 검색 장애가 주문·회원 API의 시작까지 막지 않으며 조회 시 PostgreSQL로 폴백합니다.
            log.error("product_search_index_initialization_failed", exception);
        }
    }

    PageResponse<ProductSearchResponse> search(String query, int page, int size) {
        try {
            var result = repository.findByNameContainingOrDescriptionContaining(
                    query,
                    query,
                    PageRequest.of(page, size)
            );
            if (result.hasContent()) {
                return new PageResponse<>(
                        result.getContent().stream().map(ProductSearchResponse::new).toList(),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()
                );
            }
            // 정상 응답인 빈 인덱스도 장애 복구 직후나 색인 누락일 수 있어 원본 DB로 한 번 더 확인합니다.
            return searchPostgres(query, page, size);
        } catch (RuntimeException exception) {
            log.warn("product_search_fallback_to_postgres", exception);
            return searchPostgres(query, page, size);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void update(ProductSearchChanged event) {
        try {
            if (event.isDeleted()) {
                repository.deleteById(event.getId().toString());
            } else {
                repository.save(new ProductSearchDocument(event));
            }
        } catch (RuntimeException exception) {
            log.error("product_search_index_failed productId={}", event.getId(), exception);
        }
    }

    long reindex() {
        List<ProductSearchDocument> documents = loadCatalogDocuments();
        // 기존 인덱스를 먼저 지우면 save 실패 순간 전체 검색이 비므로 새 문서를 먼저 반영합니다.
        repository.saveAll(documents);

        Set<String> activeIds = documents.stream()
                .map(ProductSearchDocument::id)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<String> staleIds = new ArrayList<>();
        repository.findAll().forEach(document -> {
            if (!activeIds.contains(document.id())) {
                staleIds.add(document.id());
            }
        });
        if (!staleIds.isEmpty()) {
            repository.deleteAllById(staleIds);
        }
        return documents.size();
    }

    private List<ProductSearchDocument> loadCatalogDocuments() {
        List<ProductSearchDocument> documents = new ArrayList<>();
        int page = 0;
        while (true) {
            var products = catalog.getProducts(page++, REINDEX_PAGE_SIZE);
            products.getContent().forEach(product -> documents.add(
                    new ProductSearchDocument(new ProductSearchChanged(product, false))
            ));
            if (page >= products.getTotalPages()) {
                break;
            }
        }
        return documents;
    }

    private PageResponse<ProductSearchResponse> searchPostgres(String query, int page, int size) {
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
