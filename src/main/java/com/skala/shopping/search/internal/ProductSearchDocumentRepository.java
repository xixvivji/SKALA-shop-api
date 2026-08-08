package com.skala.shopping.search.internal;

import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

interface ProductSearchDocumentRepository extends ElasticsearchRepository<ProductSearchDocument,String> {
    Page<ProductSearchDocument> findByNameContainingOrDescriptionContaining(
            String name,String description,Pageable pageable);
}
