package com.skala.shopping.storefront.internal;

import java.math.BigDecimal;
import java.util.UUID;

public final class RegistrationView {

    private final UUID memberId;
    private final String customerId;
    private final String name;
    private final BigDecimal customerPoint;

    public RegistrationView(UUID memberId, String customerId, String name, BigDecimal customerPoint) {
        this.memberId = memberId;
        this.customerId = customerId;
        this.name = name;
        this.customerPoint = customerPoint;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCustomerPoint() {
        return customerPoint;
    }
}
