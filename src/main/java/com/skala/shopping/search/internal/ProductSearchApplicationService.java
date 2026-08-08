package com.skala.shopping.search.internal;

import com.skala.shopping.catalog.CatalogApi; import com.skala.shopping.catalog.ProductSearchChanged;
import com.skala.shopping.common.PageResponse; import java.util.ArrayList; import java.util.List;
import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest; import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase; import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations; import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Service @ConditionalOnProperty(name="shopping.search.enabled",havingValue="true")
class ProductSearchApplicationService {
    private static final Logger log=LoggerFactory.getLogger(ProductSearchApplicationService.class);
    private final ProductSearchDocumentRepository repository; private final CatalogApi catalog; private final ElasticsearchOperations operations;
    ProductSearchApplicationService(ProductSearchDocumentRepository repository,CatalogApi catalog,ElasticsearchOperations operations){this.repository=repository;this.catalog=catalog;this.operations=operations;}
    @EventListener(ApplicationReadyEvent.class) void initializeIndex(){var index=operations.indexOps(ProductSearchDocument.class);
        if(!index.exists())index.createWithMapping();}
    PageResponse<ProductSearchResponse> search(String query,int page,int size){try{var result=repository
            .findByNameContainingOrDescriptionContaining(query,query,PageRequest.of(page,size));
        return new PageResponse<>(result.getContent().stream().map(ProductSearchResponse::new).toList(),
                result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());}
        catch(RuntimeException exception){log.warn("product_search_fallback_to_postgres query={}",query,exception);
            var fallback=catalog.searchProducts(query,null,null,null,page,size);
            return new PageResponse<>(fallback.getContent().stream().map(ProductSearchResponse::new).toList(),fallback.getPage(),
                    fallback.getSize(),fallback.getTotalElements(),fallback.getTotalPages());}}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    void update(ProductSearchChanged event){try{if(event.isDeleted())repository.deleteById(event.getId().toString());
        else repository.save(new ProductSearchDocument(event));}catch(RuntimeException exception){log.error("product_search_index_failed productId={}",event.getId(),exception);}}
    long reindex(){List<ProductSearchDocument> documents=new ArrayList<>();int page=0;
        while(true){var products=catalog.getProducts(page++,100);products.getContent().forEach(product -> documents.add(
                new ProductSearchDocument(new ProductSearchChanged(product,false))));if(page>=products.getTotalPages())break;}
        repository.deleteAll();repository.saveAll(documents);return documents.size();}
}
