package com.skala.shopping.storefront.internal.web.dto.response;

import com.skala.shopping.storefront.internal.RegistrationView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "RegistrationResponse", description = "회원가입 결과")
public final class RegistrationResponse {

    @Schema(description = "회원 식별자")
    private final UUID memberId;

    @Schema(description = "로그인에 사용하는 고객 ID", example = "skala01")
    private final String customerId;

    @Schema(description = "고객 이름", example = "김스칼라")
    private final String name;

    @Schema(description = "가입 시 지급된 포인트", example = "1000000")
    private final BigDecimal customerPoint;

    public RegistrationResponse(
            UUID memberId,
            String customerId,
            String name,
            BigDecimal customerPoint
    ) {
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

    public static RegistrationResponse from(RegistrationView registration) {
        return new RegistrationResponse(
                registration.getMemberId(),
                registration.getCustomerId(),
                registration.getName(),
                registration.getCustomerPoint()
        );
    }
}
