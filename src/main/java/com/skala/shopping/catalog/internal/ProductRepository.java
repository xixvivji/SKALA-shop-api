package com.skala.shopping.catalog.internal;

import com.skala.shopping.catalog.internal.domain.Product;
import com.skala.shopping.catalog.internal.domain.ProductStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndStatusNot(UUID id, ProductStatus status);

    boolean existsByNameIgnoreCaseAndStatusNot(String name, ProductStatus status);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);
}
