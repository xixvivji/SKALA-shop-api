package com.skala.shopping.search.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductSearchChanged;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.common.PageResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

class ProductSearchApplicationServiceTests {
    @Test void indexesChangedProductAndDeletesRemovedProduct(){
        ProductSearchDocumentRepository repository=mock(ProductSearchDocumentRepository.class);
        ProductSearchApplicationService service=new ProductSearchApplicationService(repository,mock(CatalogApi.class),mock(ElasticsearchOperations.class));
        UUID id=UUID.randomUUID(); ProductSnapshot product=new ProductSnapshot(id,"검색 상품",new BigDecimal("1000"),"ACTIVE");
        service.update(new ProductSearchChanged(product,false));
        verify(repository).save(any(ProductSearchDocument.class));
        service.update(new ProductSearchChanged(product,true));
        verify(repository).deleteById(id.toString());
    }

    @Test
    void searchIndexFailureDoesNotPreventApplicationStartup() {
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        when(operations.indexOps(ProductSearchDocument.class))
                .thenThrow(new IllegalStateException("search unavailable"));
        ProductSearchApplicationService service = new ProductSearchApplicationService(
                mock(ProductSearchDocumentRepository.class), mock(CatalogApi.class), operations);

        assertDoesNotThrow(service::initializeIndex);
    }

    @Test
    void emptyElasticsearchResultFallsBackToPostgres() {
        ProductSearchDocumentRepository repository = mock(ProductSearchDocumentRepository.class);
        CatalogApi catalog = mock(CatalogApi.class);
        when(repository.findByNameContainingOrDescriptionContaining(
                "키보드", "키보드", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        ProductSnapshot product = new ProductSnapshot(
                UUID.randomUUID(), "폴백 키보드", new BigDecimal("25000"), "ACTIVE");
        when(catalog.searchProducts("키보드", null, null, null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(product), 0, 20, 1, 1));
        ProductSearchApplicationService service = new ProductSearchApplicationService(
                repository, catalog, mock(ElasticsearchOperations.class));

        PageResponse<ProductSearchResponse> result = service.search("키보드", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("폴백 키보드", result.getContent().getFirst().getName());
    }

    @Test
    void startupBackfillsEmptyIndexFromCatalog() {
        ProductSearchDocumentRepository repository = mock(ProductSearchDocumentRepository.class);
        CatalogApi catalog = mock(CatalogApi.class);
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        IndexOperations index = mock(IndexOperations.class);
        when(operations.indexOps(ProductSearchDocument.class)).thenReturn(index);
        when(index.exists()).thenReturn(false);
        when(repository.count()).thenReturn(0L);
        ProductSnapshot product = new ProductSnapshot(
                UUID.randomUUID(), "초기 색인 상품", new BigDecimal("1000"), "ACTIVE");
        when(catalog.getProducts(0, 100))
                .thenReturn(new PageResponse<>(List.of(product), 0, 100, 1, 1));
        when(repository.findAll()).thenReturn(List.of());
        ProductSearchApplicationService service = new ProductSearchApplicationService(
                repository, catalog, operations);

        service.initializeIndex();

        verify(index).createWithMapping();
        verify(repository).saveAll(any());
        verify(repository, never()).deleteAll();
    }

    @Test
    void reindexWritesNewDocumentsBeforeRemovingStaleOnes() {
        ProductSearchDocumentRepository repository = mock(ProductSearchDocumentRepository.class);
        CatalogApi catalog = mock(CatalogApi.class);
        ProductSnapshot active = new ProductSnapshot(
                UUID.randomUUID(), "현재 상품", new BigDecimal("1000"), "ACTIVE");
        ProductSnapshot stale = new ProductSnapshot(
                UUID.randomUUID(), "삭제 상품", new BigDecimal("2000"), "ACTIVE");
        ProductSearchDocument staleDocument = new ProductSearchDocument(new ProductSearchChanged(stale, false));
        when(catalog.getProducts(0, 100))
                .thenReturn(new PageResponse<>(List.of(active), 0, 100, 1, 1));
        when(repository.findAll()).thenReturn(List.of(staleDocument));
        ProductSearchApplicationService service = new ProductSearchApplicationService(
                repository, catalog, mock(ElasticsearchOperations.class));

        service.reindex();

        var order = inOrder(repository);
        order.verify(repository).saveAll(any());
        order.verify(repository).findAll();
        order.verify(repository).deleteAllById(List.of(stale.getId().toString()));
    }
}
