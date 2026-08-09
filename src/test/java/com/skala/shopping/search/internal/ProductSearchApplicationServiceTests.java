package com.skala.shopping.search.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductSearchApplicationServiceTests {

    @Test
    void returnsIndependentSearchServiceResult() {
        SearchServiceClient client = mock(SearchServiceClient.class);
        CatalogApi catalog = mock(CatalogApi.class);
        SearchServiceClient.SearchProduct product = searchProduct("검색 키보드");
        SearchServiceClient.SearchPage page = new SearchServiceClient.SearchPage();
        page.setContent(List.of(product));
        page.setPage(0);
        page.setSize(20);
        page.setTotalElements(1);
        page.setTotalPages(1);
        when(client.search("키보드", 0, 20)).thenReturn(page);
        ProductSearchApplicationService service = new ProductSearchApplicationService(client, catalog);

        PageResponse<ProductSearchResponse> result = service.search("키보드", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("검색 키보드", result.getContent().getFirst().getName());
    }

    @Test
    void searchServiceFailureFallsBackToCatalog() {
        SearchServiceClient client = mock(SearchServiceClient.class);
        CatalogApi catalog = mock(CatalogApi.class);
        when(client.search("키보드", 0, 20))
                .thenThrow(new IllegalStateException("search unavailable"));
        ProductSnapshot product = new ProductSnapshot(
                UUID.randomUUID(), "폴백 키보드", new BigDecimal("25000"), "ACTIVE");
        when(catalog.searchProducts("키보드", null, null, null, 0, 20))
                .thenReturn(new PageResponse<>(List.of(product), 0, 20, 1, 1));
        ProductSearchApplicationService service = new ProductSearchApplicationService(client, catalog);

        PageResponse<ProductSearchResponse> result = service.search("키보드", 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("폴백 키보드", result.getContent().getFirst().getName());
    }

    @Test
    void reindexFailureIsExposedAsServiceUnavailable() {
        SearchServiceClient client = mock(SearchServiceClient.class);
        when(client.reindex()).thenThrow(new IllegalStateException("search unavailable"));
        ProductSearchApplicationService service = new ProductSearchApplicationService(
                client, mock(CatalogApi.class));

        BusinessException exception = assertThrows(BusinessException.class, service::reindex);

        assertEquals(ErrorCode.UPSTREAM_UNAVAILABLE, exception.errorCode());
    }

    @Test
    void delegatesReindexToSearchService() {
        SearchServiceClient client = mock(SearchServiceClient.class);
        when(client.reindex()).thenReturn(18L);
        ProductSearchApplicationService service = new ProductSearchApplicationService(
                client, mock(CatalogApi.class));

        assertEquals(18L, service.reindex());
        verify(client).reindex();
    }

    private SearchServiceClient.SearchProduct searchProduct(String name) {
        SearchServiceClient.SearchProduct product = new SearchServiceClient.SearchProduct();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setPrice(new BigDecimal("12000"));
        product.setStatus("ACTIVE");
        return product;
    }
}
