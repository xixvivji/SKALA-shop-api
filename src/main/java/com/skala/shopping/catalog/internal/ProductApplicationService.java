package com.skala.shopping.catalog.internal;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.catalog.internal.domain.Product;
import com.skala.shopping.catalog.internal.domain.ProductStatus;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductApplicationService implements CatalogApi {

    private final ProductRepository repository;
    private final Clock clock = Clock.systemUTC();

    public ProductApplicationService(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSnapshot getSaleableProduct(UUID productId) {
        Product product = findProduct(productId);
        if (!product.isSaleable()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_SALEABLE);
        }
        return product.toSnapshot();
    }

    @Transactional(readOnly = true)
    public ProductSnapshot getProduct(UUID productId) {
        return findProduct(productId).toSnapshot();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSnapshot> getProducts(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                repository.findAllByStatus(ProductStatus.ACTIVE, pageable)
                        .map(Product::toSnapshot)
        );
    }

    @Transactional
    public ProductSnapshot createProduct(String name, BigDecimal price) {
        String normalizedName = normalizeName(name);
        validateUniqueName(normalizedName);
        return repository.save(new Product(normalizedName, price, clock.instant())).toSnapshot();
    }

    @Transactional
    public ProductSnapshot updateProduct(UUID productId, String name, BigDecimal price) {
        Product product = findProduct(productId);
        String normalizedName = normalizeName(name);
        if (!product.toSnapshot().getName().equalsIgnoreCase(normalizedName)) {
            validateUniqueName(normalizedName);
        }
        product.update(normalizedName, price, clock.instant());
        return product.toSnapshot();
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        findProduct(productId).delete(clock.instant());
    }

    private Product findProduct(UUID productId) {
        return repository.findByIdAndStatusNot(productId, ProductStatus.DELETED)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    private void validateUniqueName(String name) {
        if (repository.existsByNameIgnoreCaseAndStatusNot(name, ProductStatus.DELETED)) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATED, "동일한 상품명이 이미 존재합니다.");
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }
}
