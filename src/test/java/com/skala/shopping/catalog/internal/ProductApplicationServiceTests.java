package com.skala.shopping.catalog.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.skala.shopping.catalog.ProductCreated;
import com.skala.shopping.catalog.ProductSnapshot;
import com.skala.shopping.catalog.internal.domain.Product;
import com.skala.shopping.catalog.internal.domain.ProductStatus;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ProductApplicationServiceTests {

    private static final BigDecimal MAX_PRODUCT_PRICE = new BigDecimal("30000000.00");

    @Mock
    private ProductRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProductApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProductApplicationService(repository, eventPublisher);
    }

    @Test
    void acceptsMaximumSafePriceWhenCreatedWithoutController() {
        when(repository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductSnapshot product = service.createProduct("안전 가격 상품", MAX_PRODUCT_PRICE, 1);

        assertEquals(0, MAX_PRODUCT_PRICE.compareTo(product.getPrice()));
        verify(eventPublisher).publishEvent(any(ProductCreated.class));
    }

    @ParameterizedTest
    @MethodSource("invalidProductPrices")
    void rejectsUnsafePriceWhenCreatedWithoutController(BigDecimal price) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProduct("잘못된 가격 상품", price, 1)
        );

        assertEquals(ErrorCode.INVALID_PARAMETER, exception.errorCode());
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void rejectsNullPriceWhenCreatedWithoutController() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createProduct("가격 없는 상품", null, 1)
        );

        assertEquals(ErrorCode.INVALID_PARAMETER, exception.errorCode());
        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void acceptsMaximumSafePriceWhenUpdatedWithoutController() {
        UUID productId = UUID.randomUUID();
        Product product = new Product("기존 상품", BigDecimal.ONE, Instant.EPOCH);
        when(repository.findByIdAndStatusNot(productId, ProductStatus.DELETED))
                .thenReturn(Optional.of(product));

        ProductSnapshot updated = service.updateProduct(
                productId,
                "변경 상품",
                MAX_PRODUCT_PRICE
        );

        assertEquals(0, MAX_PRODUCT_PRICE.compareTo(updated.getPrice()));
    }

    @Test
    void rejectsUnsafePriceWhenUpdatedWithoutController() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateProduct(
                        UUID.randomUUID(),
                        "잘못된 가격 상품",
                        new BigDecimal("30000000.01")
                )
        );

        assertEquals(ErrorCode.INVALID_PARAMETER, exception.errorCode());
        verifyNoInteractions(repository, eventPublisher);
    }

    private static Stream<BigDecimal> invalidProductPrices() {
        return Stream.of(
                new BigDecimal("-0.01"),
                BigDecimal.ZERO,
                new BigDecimal("0.001"),
                new BigDecimal("1.001"),
                new BigDecimal("30000000.01")
        );
    }
}
