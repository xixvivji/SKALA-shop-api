package com.skala.shopping.catalog.internal;

import com.skala.shopping.catalog.CatalogApi;
import com.skala.shopping.catalog.ProductCreated;
import com.skala.shopping.catalog.ProductDeleted;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.catalog.ProductVariantSnapshot;
import com.skala.shopping.catalog.ProductSearchChanged;
import com.skala.shopping.catalog.internal.domain.Product;
import com.skala.shopping.catalog.internal.domain.ProductStatus;
import com.skala.shopping.catalog.internal.domain.ProductVariant;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductApplicationService implements CatalogApi {

    private static final BigDecimal MIN_PRODUCT_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRODUCT_PRICE = new BigDecimal("30000000.00");

    private final ProductRepository repository;
    private final ProductVariantRepository variantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock = Clock.systemUTC();

    public ProductApplicationService(
            ProductRepository repository,
            ProductVariantRepository variantRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.variantRepository = variantRepository;
        this.eventPublisher = eventPublisher;
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

    @Override
    @Transactional(readOnly = true)
    public ProductVariantSnapshot getSaleableVariant(UUID productId, UUID variantId) {
        Product product = findProduct(productId);
        if (!product.isSaleable()) throw new BusinessException(ErrorCode.PRODUCT_NOT_SALEABLE);
        UUID resolvedVariantId = variantId == null ? productId : variantId;
        ProductVariant variant = variantRepository.findByIdAndStatusNot(resolvedVariantId, ProductStatus.DELETED)
                .filter(candidate -> candidate.productId().equals(productId) && candidate.isSaleable())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "상품 옵션을 찾을 수 없습니다."));
        ProductSnapshot snapshot = product.toSnapshot();
        return variant.toSnapshot(new ProductVariant.ProductSnapshotSource(
                snapshot.getId(), snapshot.getName(), snapshot.getPrice()));
    }

    @Transactional(readOnly = true)
    public List<ProductVariantSnapshot> getVariants(UUID productId) {
        Product product = findProduct(productId);
        ProductSnapshot snapshot = product.toSnapshot();
        ProductVariant.ProductSnapshotSource source = new ProductVariant.ProductSnapshotSource(
                snapshot.getId(), snapshot.getName(), snapshot.getPrice());
        return variantRepository.findAllByProductIdAndStatusNotOrderByCreatedAtAscIdAsc(
                productId, ProductStatus.DELETED).stream().map(variant -> variant.toSnapshot(source)).toList();
    }

    @Transactional
    public ProductVariantSnapshot createVariant(UUID productId, String sku, String optionName,
                                                String optionValue, BigDecimal additionalPrice,
                                                int initialQuantity) {
        Product product = findProduct(productId);
        String normalizedSku = sku.trim().toUpperCase();
        if (variantRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATED, "동일한 SKU가 이미 존재합니다.");
        }
        validateAdditionalPrice(additionalPrice);
        Instant now = clock.instant();
        ProductVariant variant = variantRepository.save(new ProductVariant(
                UUID.randomUUID(), productId, normalizedSku, optionName.trim(), optionValue.trim(),
                additionalPrice, now));
        eventPublisher.publishEvent(new ProductCreated(variant.id(), initialQuantity));
        ProductSnapshot snapshot = product.toSnapshot();
        return variant.toSnapshot(new ProductVariant.ProductSnapshotSource(
                snapshot.getId(), snapshot.getName(), snapshot.getPrice()));
    }

    @Transactional
    public void deleteVariant(UUID productId, UUID variantId) {
        if (productId.equals(variantId)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "기본 옵션은 삭제할 수 없습니다.");
        }
        ProductVariant variant = variantRepository.findByIdAndStatusNot(variantId, ProductStatus.DELETED)
                .filter(candidate -> candidate.productId().equals(productId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "상품 옵션을 찾을 수 없습니다."));
        variant.deactivate(clock.instant());
        eventPublisher.publishEvent(new ProductDeleted(variantId));
    }

    @Transactional(readOnly = true)
    public ProductSnapshot getProduct(UUID productId) {
        return findProduct(productId).toSnapshot();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSnapshot> getProducts(int page, int size) {
        return searchProducts(null, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSnapshot> searchProducts(String query, UUID categoryId,
                                                        BigDecimal minPrice, BigDecimal maxPrice,
                                                        int page, int size) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "최소 가격은 최대 가격보다 클 수 없습니다.");
        }
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return PageResponse.from(
                repository.search(ProductStatus.ACTIVE,
                                query == null || query.isBlank() ? null : query.trim(),
                                categoryId, minPrice, maxPrice, pageable)
                        .map(Product::toSnapshot)
        );
    }

    @Transactional
    public ProductSnapshot createProduct(String name, BigDecimal price, int initialQuantity) {
        return createProduct(name, price, initialQuantity, null, null, null);
    }

    @Transactional
    public ProductSnapshot createProduct(String name, BigDecimal price, int initialQuantity,
                                         UUID categoryId, String description, String imageUrl) {
        validatePrice(price);
        String normalizedName = normalizeName(name);
        validateUniqueName(normalizedName);
        Product entity = new Product(normalizedName, price, clock.instant());
        entity.updateDetails(categoryId, normalizeNullable(description), normalizeNullable(imageUrl), clock.instant());
        ProductSnapshot product = repository.save(entity).toSnapshot();
        variantRepository.save(new ProductVariant(
                product.getId(), product.getId(), "DEFAULT-" + product.getId(), null, null,
                BigDecimal.ZERO.setScale(2), clock.instant()));
        eventPublisher.publishEvent(new ProductCreated(product.getId(), initialQuantity));
        eventPublisher.publishEvent(new ProductSearchChanged(product, false));
        return product;
    }

    @Transactional
    public ProductSnapshot updateProduct(UUID productId, String name, BigDecimal price) {
        validatePrice(price);
        Product product = findProduct(productId);
        return updateProduct(productId, name, price, product.toSnapshot().getCategoryId(),
                product.toSnapshot().getDescription(), product.toSnapshot().getImageUrl());
    }

    @Transactional
    public ProductSnapshot updateProduct(UUID productId, String name, BigDecimal price,
                                         UUID categoryId, String description, String imageUrl) {
        validatePrice(price);
        Product product = findProduct(productId);
        String normalizedName = normalizeName(name);
        if (!product.toSnapshot().getName().equalsIgnoreCase(normalizedName)) {
            validateUniqueName(normalizedName);
        }
        product.update(normalizedName, price, clock.instant());
        product.updateDetails(categoryId, normalizeNullable(description), normalizeNullable(imageUrl), clock.instant());
        ProductSnapshot updated = product.toSnapshot();
        eventPublisher.publishEvent(new ProductSearchChanged(updated, false));
        return updated;
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product=findProduct(productId);
        ProductSnapshot deletedSnapshot=product.toSnapshot();
        product.delete(clock.instant());
        // 마이그레이션 전에 생성되어 기본 SKU 행이 없는 레거시 상품도 재고를 반드시 비활성화합니다.
        eventPublisher.publishEvent(new ProductDeleted(productId));
        variantRepository.findAllByProductIdAndStatusNotOrderByCreatedAtAscIdAsc(productId, ProductStatus.DELETED)
                .stream().filter(variant -> !variant.id().equals(productId))
                .forEach(variant -> {
                    variant.deactivate(clock.instant());
                    eventPublisher.publishEvent(new ProductDeleted(variant.id()));
                });
        eventPublisher.publishEvent(new ProductSearchChanged(deletedSnapshot, true));
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

    private void validatePrice(BigDecimal price) {
        if (price == null
                || price.compareTo(MIN_PRODUCT_PRICE) < 0
                || price.compareTo(MAX_PRODUCT_PRICE) > 0
                || price.scale() > 2) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "상품 가격은 0.01 이상 30,000,000.00 이하이며 소수점 둘째 자리까지 입력해야 합니다."
            );
        }
    }

    private void validateAdditionalPrice(BigDecimal price) {
        if (price == null || price.signum() < 0 || price.compareTo(MAX_PRODUCT_PRICE) > 0 || price.scale() > 2) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                    "옵션 추가 금액은 0 이상 30,000,000.00 이하이며 소수점 둘째 자리까지 입력해야 합니다.");
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeNullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
