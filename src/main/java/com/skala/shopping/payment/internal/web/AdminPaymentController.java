package com.skala.shopping.payment.internal.web;

import com.skala.shopping.common.PageResponse;
import com.skala.shopping.payment.PaymentApi;
import com.skala.shopping.payment.PaymentView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.UUID;
import com.skala.shopping.payment.internal.web.dto.request.FakeWebhookRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/payments")
class AdminPaymentController {
    private final PaymentApi paymentApi;
    AdminPaymentController(PaymentApi paymentApi) { this.paymentApi = paymentApi; }
    @GetMapping
    PageResponse<PaymentView> all(@RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return paymentApi.getAll(page, size);
    }

    @PostMapping("/{paymentId}/reconcile")
    PaymentView reconcile(@PathVariable UUID paymentId) {
        return paymentApi.reconcile(paymentId);
    }

    @PostMapping("/fake-webhooks")
    PaymentView webhook(@Valid @RequestBody FakeWebhookRequest request) {
        return paymentApi.processWebhook(
                request.getEventId(), request.getPaymentId(), request.getEventType());
    }
}
