package com.skala.shopping.payment.internal.domain;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.payment.PaymentView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_order", columnNames = "order_id"),
        @UniqueConstraint(name = "uk_payments_member_prepare", columnNames = {"member_id", "prepare_command_id"})
})
public class Payment {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(name = "prepare_command_id", nullable = false) private UUID prepareCommandId;
    @Column(name = "prepare_fingerprint", nullable = false, length = 300) private String prepareFingerprint;
    @Column(name = "approve_command_id") private UUID approveCommandId;
    @Column(nullable = false, length = 30) private String provider;
    @Column(name = "provider_transaction_id", length = 100) private String providerTransactionId;
    @Column(nullable = false, length = 30) private String method;
    @Column(name = "masked_number", length = 30) private String maskedNumber;
    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2) private BigDecimal requestedAmount;
    @Column(name = "approved_amount", nullable = false, precision = 19, scale = 2) private BigDecimal approvedAmount;
    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2) private BigDecimal refundedAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private PaymentStatus status;
    @Column(name = "failure_code", length = 50) private String failureCode;
    @Column(name = "failure_message", length = 200) private String failureMessage;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected Payment() { }

    public Payment(UUID id, UUID orderId, UUID memberId, UUID prepareCommandId,
                   String prepareFingerprint, String method, BigDecimal amount, Instant now) {
        this.id = id; this.orderId = orderId; this.memberId = memberId;
        this.prepareCommandId = prepareCommandId; this.prepareFingerprint = prepareFingerprint;
        this.provider = "FAKE"; this.method = method; this.requestedAmount = amount;
        this.approvedAmount = BigDecimal.ZERO.setScale(2);
        this.refundedAmount = BigDecimal.ZERO.setScale(2);
        this.status = PaymentStatus.READY; this.createdAt = now; this.updatedAt = now;
    }

    public UUID id() { return id; }
    public UUID orderId() { return orderId; }
    public UUID memberId() { return memberId; }
    public String providerTransactionId() { return providerTransactionId; }
    public boolean belongsTo(UUID candidate) { return memberId.equals(candidate); }
    public boolean hasPrepareFingerprint(String value) { return prepareFingerprint.equals(value); }
    public boolean wasApprovedBy(UUID commandId) {
        return approveCommandId != null && approveCommandId.equals(commandId)
                && status == PaymentStatus.PAID;
    }

    public void beginApproval(UUID commandId, String masked, Instant now) {
        if (status != PaymentStatus.READY) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_READY);
        }
        approveCommandId = commandId; maskedNumber = masked;
        status = PaymentStatus.PAYMENT_PENDING; failureCode = null; failureMessage = null;
        updatedAt = now;
    }

    public void approve(String transactionId, Instant now) {
        if (status != PaymentStatus.PAYMENT_PENDING) throw new BusinessException(ErrorCode.PAYMENT_NOT_READY);
        providerTransactionId = transactionId; approvedAmount = requestedAmount;
        status = PaymentStatus.PAID; approvedAt = now; updatedAt = now;
    }

    public void fail(String code, String message, Instant now) {
        status = PaymentStatus.PAYMENT_FAILED;
        failureCode = code; failureMessage = message; updatedAt = now;
    }

    public void compensate(Instant now) {
        refundedAmount = approvedAmount;
        status = PaymentStatus.REFUNDED;
        failureCode = "ORDER_CONFIRMATION_FAILED";
        failureMessage = "주문 확정 실패로 승인 결제가 자동 취소되었습니다.";
        updatedAt = now;
    }

    public void refund(BigDecimal amount, Instant now) {
        if (status != PaymentStatus.PAID && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_READY, "승인된 결제만 환불할 수 있습니다.");
        }
        if (amount == null || amount.signum() <= 0
                || refundedAmount.add(amount).compareTo(approvedAmount) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "환불 가능 금액을 초과했습니다.");
        }
        refundedAmount = refundedAmount.add(amount);
        status = refundedAmount.compareTo(approvedAmount) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        updatedAt = now;
    }

    public PaymentView toView() {
        return new PaymentView(id, orderId, provider, providerTransactionId, method,
                maskedNumber, requestedAmount, approvedAmount, refundedAmount,
                status.name(), failureCode, failureMessage, approvedAt, createdAt);
    }
}
