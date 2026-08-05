package com.skala.shopping.storefront.internal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("shopping.member")
class MemberRegistrationProperties {

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("30000000000000.00")
    @Digits(integer = 14, fraction = 2)
    private BigDecimal initialPoints;

    public MemberRegistrationProperties() {
    }

    public BigDecimal getInitialPoints() {
        return initialPoints;
    }

    public void setInitialPoints(BigDecimal initialPoints) {
        this.initialPoints = initialPoints;
    }
}
