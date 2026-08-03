package com.skala.shopping.storefront.internal.web.dto.response;

import com.skala.shopping.storefront.internal.CustomerDetailView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(name = "CustomerResponse", description = "현재 고객의 프로필, 포인트와 구매 상품")
public final class CustomerResponse {

    @Schema(description = "회원 식별자")
    private final UUID memberId;

    @Schema(description = "로그인에 사용하는 고객 ID", example = "skala01")
    private final String customerId;

    @Schema(description = "고객 이름", example = "김스칼라")
    private final String name;

    @Schema(description = "현재 포인트 잔액", example = "970000")
    private final BigDecimal customerPoint;

    @Schema(description = "현재 로그인 계정 역할", example = "CUSTOMER")
    private final String role;

    @Schema(description = "취소되지 않고 보유 중인 구매 상품")
    private final List<PurchasedProductResponse> products;

    public CustomerResponse(
            UUID memberId,
            String customerId,
            String name,
            BigDecimal customerPoint,
            String role,
            List<PurchasedProductResponse> products
    ) {
        this.memberId = memberId;
        this.customerId = customerId;
        this.name = name;
        this.customerPoint = customerPoint;
        this.role = role;
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

    public String getRole() {
        return role;
    }

    public List<PurchasedProductResponse> getProducts() {
        return products;
    }

    public static CustomerResponse from(CustomerDetailView customer, String role) {
        return new CustomerResponse(
                customer.getMemberId(),
                customer.getCustomerId(),
                customer.getName(),
                customer.getCustomerPoint(),
                role,
                customer.getProducts().stream().map(PurchasedProductResponse::from).toList()
        );
    }
}
