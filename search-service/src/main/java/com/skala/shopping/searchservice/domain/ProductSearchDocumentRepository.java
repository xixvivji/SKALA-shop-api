package com.skala.shopping.searchservice.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchDocumentRepository
        extends ElasticsearchRepository<ProductSearchDocument, String> {

    Page<ProductSearchDocument> findByNameContainingOrDescriptionContaining(
            String name,
            String description,
            Pageable pageable
    );
}
