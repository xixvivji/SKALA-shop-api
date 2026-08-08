package com.skala.shopping.catalog.internal;

import com.skala.shopping.catalog.internal.domain.ProductStatus;
import com.skala.shopping.catalog.internal.domain.ProductVariant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    Optional<ProductVariant> findByIdAndStatusNot(UUID id, ProductStatus status);
    List<ProductVariant> findAllByProductIdAndStatusNotOrderByCreatedAtAscIdAsc(UUID productId, ProductStatus status);
    boolean existsBySkuIgnoreCase(String sku);
}
