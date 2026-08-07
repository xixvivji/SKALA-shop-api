package com.skala.shopping.order.internal.web.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UpdateFulfillmentRequest {

    @Pattern(regexp="PAID|PREPARING|SHIPPED|DELIVERED")
    private String status;

    @Size(max = 80)
    private String trackingCarrier;

    @Size(max = 100)
    private String trackingNumber;

    @Size(max = 500)
    private String trackingUrl;

    public UpdateFulfillmentRequest() { }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTrackingCarrier() { return trackingCarrier; }
    public void setTrackingCarrier(String trackingCarrier) { this.trackingCarrier = trackingCarrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getTrackingUrl() { return trackingUrl; }
    public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }

    @AssertTrue(message = "status 또는 추적 정보 중 하나는 필수입니다.")
    public boolean hasAction() {
        return hasText(status)
                || hasText(trackingCarrier)
                || hasText(trackingNumber)
                || hasText(trackingUrl);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
