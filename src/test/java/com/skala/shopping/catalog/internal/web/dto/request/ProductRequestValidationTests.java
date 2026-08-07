package com.skala.shopping.catalog.internal.web.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductRequestValidationTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsMaximumJavaScriptSafeProductPrice() {
        BigDecimal maximum = new BigDecimal("30000000.00");

        assertThat(validator.validateValue(
                CreateProductRequest.class,
                "productPrice",
                maximum
        )).isEmpty();
        assertThat(validator.validateValue(
                UpdateProductRequest.class,
                "productPrice",
                maximum
        )).isEmpty();
        assertThat(maximum.multiply(BigDecimal.valueOf(1_000_000)))
                .isEqualByComparingTo("30000000000000.00");
    }

    @Test
    void rejectsPriceAboveJavaScriptSafeProductPrice() {
        BigDecimal unsafe = new BigDecimal("30000000.01");

        assertThat(validator.validateValue(
                CreateProductRequest.class,
                "productPrice",
                unsafe
        )).isNotEmpty();
        assertThat(validator.validateValue(
                UpdateProductRequest.class,
                "productPrice",
                unsafe
        )).isNotEmpty();
    }

    @Test
    void rejectsPriceWithMoreThanTwoFractionDigits() {
        BigDecimal invalidScale = new BigDecimal("1.001");

        assertThat(validator.validateValue(
                CreateProductRequest.class,
                "productPrice",
                invalidScale
        )).isNotEmpty();
        assertThat(validator.validateValue(
                UpdateProductRequest.class,
                "productPrice",
                invalidScale
        )).isNotEmpty();
    }

    @Test
    void documentsSafeMaximumInSwaggerSchema() throws NoSuchFieldException {
        Schema createSchema = CreateProductRequest.class
                .getDeclaredField("productPrice")
                .getAnnotation(Schema.class);
        Schema updateSchema = UpdateProductRequest.class
                .getDeclaredField("productPrice")
                .getAnnotation(Schema.class);

        assertThat(createSchema.maximum()).isEqualTo("30000000.00");
        assertThat(createSchema.description()).contains("센트 단위");
        assertThat(updateSchema.maximum()).isEqualTo("30000000.00");
        assertThat(updateSchema.description()).contains("센트 단위");
    }
}
