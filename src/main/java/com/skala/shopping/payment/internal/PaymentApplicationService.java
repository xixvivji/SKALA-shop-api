package com.skala.shopping.payment.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.PaymentOrderView;
import com.skala.shopping.payment.PaymentApi;
import com.skala.shopping.payment.PaymentView;
import com.skala.shopping.payment.internal.domain.Payment;
import com.skala.shopping.payment.internal.domain.PaymentRefund;
import com.skala.shopping.payment.internal.domain.PaymentWebhookEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentApplicationService implements PaymentApi {
    private final PaymentRepository repository;
    private final OrderApi orderApi;
    private final FakeGateway gateway;
    private final MeterRegistry meterRegistry;
    private final PaymentRefundRepository refundRepository;
    private final PaymentWebhookEventRepository webhookRepository;
    private final Clock clock = Clock.systemUTC();

    PaymentApplicationService(PaymentRepository repository, OrderApi orderApi,
                              FakeGateway gateway, MeterRegistry meterRegistry,
                              PaymentRefundRepository refundRepository,
                              PaymentWebhookEventRepository webhookRepository) {
        this.repository = repository; this.orderApi = orderApi;
        this.gateway = gateway; this.meterRegistry = meterRegistry;
        this.refundRepository = refundRepository; this.webhookRepository = webhookRepository;
    }

    @Override
    @Transactional
    public PaymentView prepare(UUID memberId, UUID orderId, String method, UUID commandId) {
        requireIds(memberId, orderId, commandId);
        String normalizedMethod = normalizeMethod(method);
        String fingerprint = orderId + "|" + normalizedMethod;
        Payment replay = repository.findByMemberIdAndPrepareCommandId(memberId, commandId).orElse(null);
        if (replay != null) {
            if (!replay.hasPrepareFingerprint(fingerprint)) throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            return replay.toView();
        }
        PaymentOrderView order = orderApi.getPaymentOrder(memberId, orderId);
        if (!"PAYMENT_PENDING".equals(order.getStatus()) || order.getPaymentAmount().signum() <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_READY, "외부 결제가 필요한 주문이 아닙니다.");
        }
        Payment existing = repository.findByMemberIdAndOrderId(memberId, orderId).orElse(null);
        if (existing != null) return existing.toView();
        return repository.save(new Payment(UUID.randomUUID(), orderId, memberId, commandId,
                fingerprint, normalizedMethod, order.getPaymentAmount(), clock.instant())).toView();
    }

    @Override
    @Transactional
    public PaymentView approve(UUID memberId, UUID paymentId, String cardNumber, UUID commandId) {
        requireIds(memberId, paymentId, commandId);
        Payment payment = lockedOwnedPayment(memberId, paymentId);
        String normalizedCard = normalizeCardNumber(cardNumber);
        String approvalFingerprint = approvalFingerprint(normalizedCard);
        if (payment.isApprovalReplay(commandId, approvalFingerprint)) {
            return payment.approvalReplayView();
        }
        String masked = mask(normalizedCard);
        payment.beginApproval(commandId, approvalFingerprint, masked, clock.instant());
        FakeGatewayResult result = gateway.approve(
                payment.id(), payment.toView().getRequestedAmount(), normalizedCard);
        if (!result.approved()) {
            payment.fail(result.failureCode(), result.failureMessage(), clock.instant());
            orderApi.failExternalPayment(memberId, payment.orderId(), payment.id());
            payment.captureApprovalResult();
            meterRegistry.counter("shopping.payment.results", "result", "failed",
                    "code", result.failureCode()).increment();
            return payment.toView();
        }
        payment.approve(result.transactionId(), clock.instant());
        try {
            orderApi.confirmExternalPayment(memberId, payment.orderId(), payment.id());
            meterRegistry.counter("shopping.payment.results", "result", "approved").increment();
        } catch (RuntimeException exception) {
            gateway.cancel(result.transactionId(), payment.toView().getApprovedAmount());
            payment.compensate(clock.instant());
            orderApi.failExternalPayment(memberId, payment.orderId(), payment.id());
            meterRegistry.counter("shopping.payment.results", "result", "compensated").increment();
        }
        payment.captureApprovalResult();
        return payment.toView();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentView get(UUID memberId, UUID paymentId) {
        return ownedPayment(memberId, paymentId).toView();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentView getByOrder(UUID memberId, UUID orderId) {
        return repository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다."))
                .toView();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentView> getMine(UUID memberId, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(repository.findAllByMemberId(memberId, pageable).map(Payment::toView));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentView> getAll(int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(repository.findAll(pageable).map(Payment::toView));
    }

    @Override
    @Transactional
    public PaymentView refund(UUID paymentId, BigDecimal amount, UUID commandId) {
        requireIds(paymentId, commandId);
        PaymentRefund replay = refundRepository.findByCommandId(commandId).orElse(null);
        if (replay != null) {
            if (!replay.matches(paymentId, amount)) throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            return repository.findById(paymentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND)).toView();
        }
        Payment payment = repository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다."));
        gateway.cancel(payment.providerTransactionId(), amount);
        payment.refund(amount, clock.instant());
        refundRepository.save(new PaymentRefund(paymentId, commandId, amount, clock.instant()));
        meterRegistry.counter("shopping.payment.refunds", "result", "success").increment();
        return payment.toView();
    }

    @Override
    @Transactional
    public PaymentView refundByOrder(UUID memberId, UUID orderId, BigDecimal amount, UUID commandId) {
        Payment payment = repository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다."));
        return refund(payment.id(), amount, commandId);
    }

    @Override
    @Transactional
    public PaymentView reconcile(UUID paymentId) {
        Payment payment = repository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다."));
        PaymentView view = payment.toView();
        if ("PAID".equals(view.getStatus())) {
            orderApi.confirmExternalPayment(payment.memberId(), payment.orderId(), paymentId);
        }
        meterRegistry.counter("shopping.payment.reconciliations", "status", view.getStatus()).increment();
        return payment.toView();
    }

    @Override
    @Transactional
    public PaymentView processWebhook(UUID eventId, UUID paymentId, String eventType) {
        requireIds(eventId, paymentId);
        String normalizedType = eventType == null ? "" : eventType.trim().toUpperCase(Locale.ROOT);
        PaymentWebhookEvent existing = webhookRepository.findById(eventId).orElse(null);
        if (existing != null) {
            if (!existing.matches(paymentId, normalizedType)) throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            return repository.findById(paymentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND)).toView();
        }
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다."));
        webhookRepository.save(new PaymentWebhookEvent(eventId, paymentId, normalizedType, clock.instant()));
        meterRegistry.counter("shopping.payment.webhooks", "type",
                normalizedType.isBlank() ? "UNKNOWN" : normalizedType).increment();
        return payment.toView();
    }

    private Payment lockedOwnedPayment(UUID memberId, UUID paymentId) {
        Payment payment = repository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다."));
        if (!payment.belongsTo(memberId)) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다.");
        return payment;
    }

    private Payment ownedPayment(UUID memberId, UUID paymentId) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다."));
        if (!payment.belongsTo(memberId)) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "결제를 찾을 수 없습니다.");
        return payment;
    }

    private String normalizeMethod(String method) {
        String normalized = method == null ? "CARD" : method.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("CARD") && !normalized.equals("BANK_TRANSFER")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "결제 수단은 CARD 또는 BANK_TRANSFER여야 합니다.");
        }
        return normalized;
    }

    private String mask(String cardNumber) {
        return cardNumber.substring(0, 4) + "-****-****-" + cardNumber.substring(12);
    }

    private String normalizeCardNumber(String cardNumber) {
        String normalized = cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");
        if (normalized.length() != 16) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "16자리 테스트 카드 번호를 입력해야 합니다."
            );
        }
        return normalized;
    }

    private String approvalFingerprint(String normalizedCard) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedCard.getBytes(StandardCharsets.UTF_8));
            return "v1:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private void requireIds(UUID... ids) {
        for (UUID id : ids) if (id == null) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
}
