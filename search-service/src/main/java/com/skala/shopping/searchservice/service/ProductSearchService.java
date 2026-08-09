package com.skala.shopping.searchservice.service;

import com.skala.shopping.searchservice.api.PageResponse;
import com.skala.shopping.searchservice.api.ProductSearchResponse;
import com.skala.shopping.searchservice.catalog.CatalogProduct;
import com.skala.shopping.searchservice.catalog.CatalogSnapshotClient;
import com.skala.shopping.searchservice.domain.ProductSearchDocument;
import com.skala.shopping.searchservice.domain.ProductSearchDocumentRepository;
import com.skala.shopping.searchservice.messaging.ProductSearchChangedMessage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);
    private static final int REINDEX_PAGE_SIZE = 100;

    private final ProductSearchDocumentRepository repository;
    private final CatalogSnapshotClient catalog;
    private final ElasticsearchOperations operations;
    private final ReentrantReadWriteLock indexLock = new ReentrantReadWriteLock();
    private final Counter indexedCounter;
    private final Counter deletedCounter;
    private final Counter reindexCounter;

    public ProductSearchService(
            ProductSearchDocumentRepository repository,
            CatalogSnapshotClient catalog,
            ElasticsearchOperations operations,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.catalog = catalog;
        this.operations = operations;
        this.indexedCounter = Counter.builder("shopping.search.events")
                .tag("result", "indexed")
                .register(meterRegistry);
        this.deletedCounter = Counter.builder("shopping.search.events")
                .tag("result", "deleted")
                .register(meterRegistry);
        this.reindexCounter = Counter.builder("shopping.search.reindex")
                .register(meterRegistry);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        try {
            var index = operations.indexOps(ProductSearchDocument.class);
            if (!index.exists()) {
                index.createWithMapping();
            }
            if (repository.count() == 0) {
                long indexed = reindex();
                log.info("product_search_initial_backfill_completed indexed={}", indexed);
            }
        } catch (RuntimeException exception) {
            // Kafka는 독립적으로 재연결하므로 일시적인 Catalog/Elasticsearch 장애가 프로세스 시작을 막지 않습니다.
            log.error("product_search_index_initialization_failed", exception);
        }
    }

    public PageResponse<ProductSearchResponse> search(String query, int page, int size) {
        var result = repository.findByNameContainingOrDescriptionContaining(
                query,
                query,
                PageRequest.of(page, size)
        );
        return new PageResponse<>(
                result.getContent().stream().map(ProductSearchResponse::new).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public void apply(ProductSearchChangedMessage event) {
        if (event.getId() == null) {
            throw new IllegalArgumentException("상품 검색 이벤트에 id가 없습니다.");
        }
        indexLock.readLock().lock();
        try {
            if (event.isDeleted()) {
                repository.deleteById(event.getId().toString());
                deletedCounter.increment();
            } else {
                repository.save(toDocument(event));
                indexedCounter.increment();
            }
        } finally {
            indexLock.readLock().unlock();
        }
    }

    public long reindex() {
        indexLock.writeLock().lock();
        try {
            List<ProductSearchDocument> documents = loadCatalogDocuments();
            // 새 snapshot을 먼저 저장한 뒤 더 이상 판매하지 않는 문서만 제거해 빈 인덱스 시간을 만들지 않습니다.
            repository.saveAll(documents);

            Set<String> activeIds = documents.stream()
                    .map(ProductSearchDocument::getId)
                    .collect(java.util.stream.Collectors.toCollection(HashSet::new));
            List<String> staleIds = new ArrayList<>();
            repository.findAll().forEach(document -> {
                if (!activeIds.contains(document.getId())) {
                    staleIds.add(document.getId());
                }
            });
            if (!staleIds.isEmpty()) {
                repository.deleteAllById(staleIds);
            }
            reindexCounter.increment();
            return documents.size();
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    private List<ProductSearchDocument> loadCatalogDocuments() {
        List<ProductSearchDocument> documents = new ArrayList<>();
        int page = 0;
        while (true) {
            var products = catalog.getProducts(page++, REINDEX_PAGE_SIZE);
            products.getContent().stream()
                    .filter(product -> "ACTIVE".equals(product.getStatus()))
                    .map(this::toDocument)
                    .forEach(documents::add);
            if (page >= products.getTotalPages()) {
                return documents;
            }
        }
    }

    private ProductSearchDocument toDocument(ProductSearchChangedMessage event) {
        return new ProductSearchDocument(
                event.getId(),
                event.getName(),
                event.getPrice(),
                event.getCategoryId(),
                event.getDescription(),
                event.getImageUrl()
        );
    }

    private ProductSearchDocument toDocument(CatalogProduct product) {
        return new ProductSearchDocument(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategoryId(),
                product.getDescription(),
                product.getImageUrl()
        );
    }
}
