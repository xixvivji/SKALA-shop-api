package com.skala.shopping.storefront.internal;

import com.skala.shopping.order.PurchasedProductView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CustomerDetailView {

    private final UUID memberId;
    private final String customerId;
    private final String name;
    private final BigDecimal customerPoint;
    private final List<PurchasedProductView> products;

    public CustomerDetailView(
            UUID memberId,
            String customerId,
            String name,
            BigDecimal customerPoint,
            List<PurchasedProductView> products
    ) {
        this.memberId = memberId;
        this.customerId = customerId;
        this.name = name;
        this.customerPoint = customerPoint;
        this.products = products;
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

    public List<PurchasedProductView> getProducts() {
        return products;
    }
}
