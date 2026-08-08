package com.skala.shopping.returns.internal.domain;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.returns.ReturnView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="return_requests", schema="returns")
public class ReturnRequest {
    @Id private UUID id;
    @Column(name="command_id", nullable=false, unique=true) private UUID commandId;
    @Column(name="member_id", nullable=false) private UUID memberId;
    @Column(name="order_id", nullable=false) private UUID orderId;
    @Column(name="order_item_id", nullable=false, unique=true) private UUID orderItemId;
    @Column(name="product_id", nullable=false) private UUID productId;
    @Column(name="product_name", nullable=false, length=200) private String productName;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false, length=50) private String reason;
    @Column(name="evidence_image_url", length=1000) private String evidenceImageUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private ReturnStatus status;
    @Column(name="gross_refund_amount", nullable=false, precision=19, scale=2) private BigDecimal grossRefundAmount;
    @Column(name="shipping_fee", nullable=false, precision=19, scale=2) private BigDecimal shippingFee;
    @Column(name="refund_amount", nullable=false, precision=19, scale=2) private BigDecimal refundAmount;
    @Column(name="point_refund_amount", nullable=false, precision=19, scale=2) private BigDecimal pointRefundAmount;
    @Column(name="payment_refund_amount", nullable=false, precision=19, scale=2) private BigDecimal paymentRefundAmount;
    @Column(name="balance_after", precision=19, scale=2) private BigDecimal balanceAfter;
    @Column(name="admin_note", length=500) private String adminNote;
    @Column(name="requested_at", nullable=false) private Instant requestedAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Column(name="processed_by") private UUID processedBy;
    @Version private long version;
    protected ReturnRequest() { }

    public ReturnRequest(UUID commandId, UUID memberId, UUID orderId, UUID orderItemId,
                         UUID productId, String productName, int quantity, String reason,
                         String evidenceImageUrl, BigDecimal grossRefundAmount,
                         BigDecimal shippingFee, BigDecimal refundAmount,
                         BigDecimal pointRefundAmount, BigDecimal paymentRefundAmount,
                         Instant now) {
        id=UUID.randomUUID(); this.commandId=commandId; this.memberId=memberId;
        this.orderId=orderId; this.orderItemId=orderItemId; this.productId=productId;
        this.productName=productName; this.quantity=quantity; this.reason=reason;
        this.evidenceImageUrl=evidenceImageUrl; status=ReturnStatus.REQUESTED;
        this.grossRefundAmount=grossRefundAmount; this.shippingFee=shippingFee;
        this.refundAmount=refundAmount; this.pointRefundAmount=pointRefundAmount;
        this.paymentRefundAmount=paymentRefundAmount; requestedAt=now; updatedAt=now;
    }
    public UUID id(){return id;} public UUID memberId(){return memberId;}
    public UUID orderId(){return orderId;} public UUID orderItemId(){return orderItemId;}
    public int quantity(){return quantity;} public BigDecimal refundAmount(){return refundAmount;}
    public BigDecimal pointRefundAmount(){return pointRefundAmount;}
    public BigDecimal paymentRefundAmount(){return paymentRefundAmount;}
    public ReturnStatus status(){return status;}
    public boolean matches(UUID expectedMember, UUID expectedOrder, UUID expectedItem,
                           int expectedQuantity, String expectedReason) {
        return memberId.equals(expectedMember) && orderId.equals(expectedOrder)
                && orderItemId.equals(expectedItem) && quantity==expectedQuantity
                && reason.equals(expectedReason);
    }
    public void transition(ReturnStatus next, UUID adminId, String note, Instant now) {
        if (!status.canTransitionTo(next))
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "허용되지 않는 반품 상태 변경입니다.");
        status=next; processedBy=adminId; adminNote=normalize(note); updatedAt=now;
    }
    public void complete(BigDecimal balance, UUID adminId, String note, Instant now) {
        transition(ReturnStatus.REFUNDED, adminId, note, now); balanceAfter=balance;
    }
    public ReturnView toView(){return new ReturnView(id,orderId,orderItemId,productId,productName,
            quantity,reason,evidenceImageUrl,status.name(),grossRefundAmount,shippingFee,
            refundAmount,pointRefundAmount,paymentRefundAmount,balanceAfter,adminNote,
            requestedAt,updatedAt);}
    private String normalize(String value){return value==null||value.isBlank()?null:value.trim();}
}
