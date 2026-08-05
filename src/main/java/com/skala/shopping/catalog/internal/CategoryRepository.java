package com.skala.shopping.catalog.internal;

import com.skala.shopping.catalog.internal.domain.Category;
import com.skala.shopping.catalog.internal.domain.ProductStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findAllByStatusOrderByNameAscIdAsc(ProductStatus status);
    Optional<Category> findByIdAndStatusNot(UUID id, ProductStatus status);
    boolean existsByNameIgnoreCaseAndStatusNot(String name, ProductStatus status);
}
