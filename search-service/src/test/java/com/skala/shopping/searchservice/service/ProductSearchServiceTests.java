package com.skala.shopping.searchservice.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shopping.searchservice.catalog.CatalogPage;
import com.skala.shopping.searchservice.catalog.CatalogProduct;
import com.skala.shopping.searchservice.catalog.CatalogSnapshotClient;
import com.skala.shopping.searchservice.domain.ProductSearchDocument;
import com.skala.shopping.searchservice.domain.ProductSearchDocumentRepository;
import com.skala.shopping.searchservice.messaging.ProductSearchChangedMessage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

class ProductSearchServiceTests {

    @Test
    void kafkaEventUpsertsAndDeletesByStableProductId() {
        ProductSearchDocumentRepository repository = mock(ProductSearchDocumentRepository.class);
        ProductSearchService service = service(repository, mock(CatalogSnapshotClient.class),
                mock(ElasticsearchOperations.class));
        ProductSearchChangedMessage event = event(false);

        service.apply(event);
        verify(repository).save(any(ProductSearchDocument.class));

        event.setDeleted(true);
        service.apply(event);
        verify(repository).deleteById(event.getId().toString());
    }

    @Test
    void searchReturnsElasticsearchPage() {
        ProductSearchDocumentRepository repository = mock(ProductSearchDocumentRepository.class);
        ProductSearchDocument document = new ProductSearchDocument(
                UUID.randomUUID(), "검색 키보드", new BigDecimal("25000"), null, "설명", null);
        when(repository.findByNameContainingOrDescriptionContaining(
                "키보드", "키보드", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(document), PageRequest.of(0, 20), 1));
        ProductSearchService service = service(repository, mock(CatalogSnapshotClient.class),
                mock(ElasticsearchOperations.class));

        var result = service.search("키보드", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("검색 키보드", result.getContent().getFirst().getName());
    }

    @Test
    void reindexWritesSnapshotBeforeRemovingStaleDocuments() {
        ProductSearchDocumentRepository repository = mock(ProductSearchDocumentRepository.class);
        CatalogSnapshotClient catalog = mock(CatalogSnapshotClient.class);
        CatalogProduct active = catalogProduct("현재 상품");
        CatalogPage page = new CatalogPage();
        page.setContent(List.of(active));
        page.setTotalPages(1);
        when(catalog.getProducts(0, 100)).thenReturn(page);
        ProductSearchDocument stale = new ProductSearchDocument(
                UUID.randomUUID(), "삭제 상품", new BigDecimal("1000"), null, null, null);
        when(repository.findAll()).thenReturn(List.of(stale));
        ProductSearchService service = service(repository, catalog, mock(ElasticsearchOperations.class));

        assertEquals(1L, service.reindex());

        var order = inOrder(repository);
        order.verify(repository).saveAll(any());
        order.verify(repository).findAll();
        order.verify(repository).deleteAllById(List.of(stale.getId()));
    }

    @Test
    void startupFailureDoesNotStopSearchProcess() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        when(operations.indexOps(ProductSearchDocument.class))
                .thenThrow(new IllegalStateException("elasticsearch unavailable"));
        ProductSearchService service = service(
                mock(ProductSearchDocumentRepository.class),
                mock(CatalogSnapshotClient.class),
                operations
        );

        assertDoesNotThrow(service::initializeIndex);
    }

    @Test
    void startupCreatesIndexAndBackfillsWhenEmpty() {
        ProductSearchDocumentRepository repository = mock(ProductSearchDocumentRepository.class);
        CatalogSnapshotClient catalog = mock(CatalogSnapshotClient.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations index = mock(IndexOperations.class);
        when(operations.indexOps(ProductSearchDocument.class)).thenReturn(index);
        when(index.exists()).thenReturn(false);
        when(repository.count()).thenReturn(0L);
        when(repository.findAll()).thenReturn(List.of());
        CatalogPage page = new CatalogPage();
        page.setContent(List.of(catalogProduct("초기 상품")));
        page.setTotalPages(1);
        when(catalog.getProducts(0, 100)).thenReturn(page);
        ProductSearchService service = service(repository, catalog, operations);

        service.initializeIndex();

        verify(index).createWithMapping();
        verify(repository).saveAll(any());
    }

    private ProductSearchService service(
            ProductSearchDocumentRepository repository,
            CatalogSnapshotClient catalog,
            ElasticsearchOperations operations
    ) {
        return new ProductSearchService(repository, catalog, operations, new SimpleMeterRegistry());
    }

    private ProductSearchChangedMessage event(boolean deleted) {
        ProductSearchChangedMessage event = new ProductSearchChangedMessage();
        event.setId(UUID.randomUUID());
        event.setName("이벤트 상품");
        event.setPrice(new BigDecimal("15000"));
        event.setDeleted(deleted);
        return event;
    }

    private CatalogProduct catalogProduct(String name) {
        CatalogProduct product = new CatalogProduct();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setPrice(new BigDecimal("10000"));
        product.setStatus("ACTIVE");
        return product;
    }
}
