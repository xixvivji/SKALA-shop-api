package com.skala.shopping.order.internal.web.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UpdateFulfillmentRequest {

    @Pattern(regexp="PAID|PREPARING|SHIPPED|DELIVERED")
    private String status;

    @Size(max = 80)
    private String trackingCarrier;

    @Size(max = 100)
    private String trackingNumber;

    @Size(max = 500)
    @Pattern(regexp = "^$|https://.+", message = "배송 조회 URL은 https:// 형식이어야 합니다.")
    private String trackingUrl;

    @FutureOrPresent(message = "예상 배송일은 현재 이후여야 합니다.")
    private Instant estimatedDeliveryAt;

    public UpdateFulfillmentRequest() { }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTrackingCarrier() { return trackingCarrier; }
    public void setTrackingCarrier(String trackingCarrier) { this.trackingCarrier = trackingCarrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }
    public Instant getEstimatedDeliveryAt() { return estimatedDeliveryAt; }
    public void setEstimatedDeliveryAt(Instant estimatedDeliveryAt) {
        this.estimatedDeliveryAt = estimatedDeliveryAt;
    }

    @AssertTrue(message = "status 또는 추적 정보 중 하나는 필수입니다.")
    public boolean hasAction() {
        return hasText(status)
                || trackingCarrier != null
                || trackingNumber != null
                || trackingUrl != null
                || estimatedDeliveryAt != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
