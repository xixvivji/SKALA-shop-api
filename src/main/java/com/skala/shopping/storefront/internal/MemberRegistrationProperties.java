package com.skala.shopping.storefront.internal;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("shopping.member")
class MemberRegistrationProperties {

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
