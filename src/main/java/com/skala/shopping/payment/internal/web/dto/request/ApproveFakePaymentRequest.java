package com.skala.shopping.payment.internal.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class ApproveFakePaymentRequest {
    @NotBlank
    @Pattern(regexp = "[0-9 -]{16,19}", message = "테스트 카드 번호 형식이 올바르지 않습니다.")
    private String testCardNumber;
    public ApproveFakePaymentRequest() { }
    public String getTestCardNumber() { return testCardNumber; }
    public void setTestCardNumber(String testCardNumber) { this.testCardNumber = testCardNumber; }
}
