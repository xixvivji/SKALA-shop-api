package com.skala.shopping.search.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any; import static org.mockito.Mockito.*;
import com.skala.shopping.catalog.CatalogApi; import com.skala.shopping.catalog.ProductSearchChanged;
import com.skala.shopping.catalog.ProductSnapshot; import java.math.BigDecimal; import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

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
}
