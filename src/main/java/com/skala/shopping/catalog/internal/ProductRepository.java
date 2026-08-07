package com.skala.shopping.catalog.internal;

import com.skala.shopping.catalog.internal.domain.Product;
import com.skala.shopping.catalog.internal.domain.ProductStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndStatusNot(UUID id, ProductStatus status);

    boolean existsByNameIgnoreCaseAndStatusNot(String name, ProductStatus status);

    Page<Product> findAllByStatus(ProductStatus status, Pageable pageable);

    @Query("""
            select product from Product product
            where product.status = :status
              and (cast(:query as string) is null
                   or lower(product.name) like concat('%', lower(cast(:query as string)), '%')
                   or lower(coalesce(product.description, '')) like concat('%', lower(cast(:query as string)), '%'))
              and (:categoryId is null or product.categoryId = :categoryId)
              and (:minPrice is null or product.price >= :minPrice)
              and (:maxPrice is null or product.price <= :maxPrice)
            """)
    Page<Product> search(@Param("status") ProductStatus status, @Param("query") String query,
                         @Param("categoryId") UUID categoryId, @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);
}
