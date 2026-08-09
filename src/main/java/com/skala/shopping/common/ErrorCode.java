package com.skala.shopping.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "요청 값이 올바르지 않습니다."),
    DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "DATA_NOT_FOUND", "요청한 데이터를 찾을 수 없습니다."),
    DATA_DUPLICATED(HttpStatus.CONFLICT, "DATA_DUPLICATED", "이미 존재하는 데이터입니다."),
    CONCURRENT_MODIFICATION(
            HttpStatus.CONFLICT,
            "CONCURRENT_MODIFICATION",
            "다른 요청이 먼저 데이터를 변경했습니다. 최신 상태를 확인한 뒤 다시 시도해 주세요."
    ),
    IDEMPOTENCY_CONFLICT(
            HttpStatus.CONFLICT,
            "IDEMPOTENCY_CONFLICT",
            "동일한 멱등성 키가 다른 요청에 이미 사용되었습니다."
    ),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),
    TOO_MANY_REQUESTS(
            HttpStatus.TOO_MANY_REQUESTS,
            "TOO_MANY_REQUESTS",
            "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."
    ),
    UPSTREAM_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "UPSTREAM_UNAVAILABLE",
            "연결된 서비스가 일시적으로 응답하지 않습니다."
    ),
    INSUFFICIENT_FUNDS(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "보유 포인트가 부족합니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "주문 가능한 재고가 부족합니다."),
    INSUFFICIENT_QUANTITY(HttpStatus.CONFLICT, "INSUFFICIENT_QUANTITY", "취소할 수량이 부족합니다."),
    PRODUCT_NOT_SALEABLE(HttpStatus.CONFLICT, "PRODUCT_NOT_SALEABLE", "현재 판매할 수 없는 상품입니다."),
    PAYMENT_DECLINED(HttpStatus.CONFLICT, "PAYMENT_DECLINED", "모의 결제가 승인되지 않았습니다."),
    PAYMENT_NOT_READY(HttpStatus.CONFLICT, "PAYMENT_NOT_READY", "현재 결제를 처리할 수 없는 상태입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
