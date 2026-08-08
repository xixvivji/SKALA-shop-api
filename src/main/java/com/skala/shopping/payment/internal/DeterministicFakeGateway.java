package com.skala.shopping.payment.internal;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class DeterministicFakeGateway implements FakeGateway {
    static final String SUCCESS = "4242424242424242";
    static final String INSUFFICIENT = "4000000000000002";
    static final String DECLINED = "4000000000009995";
    static final String TIMEOUT = "4000000000000341";

    @Override
    public FakeGatewayResult approve(UUID paymentId, BigDecimal amount, String card) {
        String normalized = card == null ? "" : card.replaceAll("[^0-9]", "");
        return switch (normalized) {
            case SUCCESS -> new FakeGatewayResult(true, "fake_" + paymentId, null, null);
            case INSUFFICIENT -> new FakeGatewayResult(false, null, "INSUFFICIENT_BALANCE", "테스트 카드 잔액이 부족합니다.");
            case TIMEOUT -> new FakeGatewayResult(false, null, "GATEWAY_TIMEOUT", "모의 PG 응답 시간이 초과되었습니다.");
            case DECLINED -> new FakeGatewayResult(false, null, "CARD_DECLINED", "테스트 카드 승인이 거절되었습니다.");
            default -> new FakeGatewayResult(false, null, "INVALID_TEST_CARD", "등록되지 않은 테스트 카드 번호입니다.");
        };
    }

    @Override
    public void cancel(String providerTransactionId, BigDecimal amount) {
        // 외부 시스템이 없으므로 취소 호출 자체만 표현합니다. 결제 원장이 최종 결과를 보존합니다.
    }
}
