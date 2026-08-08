package com.skala.shopping.returns.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.ReturnSettlementView;
import com.skala.shopping.order.ReturnableOrderItemView;
import com.skala.shopping.payment.PaymentApi;
import com.skala.shopping.returns.ReturnApi;
import com.skala.shopping.returns.ReturnView;
import com.skala.shopping.returns.internal.domain.ReturnRequest;
import com.skala.shopping.returns.internal.domain.ReturnStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReturnApplicationService implements ReturnApi {
    private static final BigDecimal CUSTOMER_RETURN_FEE = new BigDecimal("3000.00");
    private static final Set<String> SELLER_RESPONSIBLE = Set.of("DAMAGED", "DEFECTIVE", "WRONG_ITEM");
    private static final Set<String> REASONS = Set.of(
            "CHANGE_OF_MIND", "SIZE_MISMATCH", "DAMAGED", "DEFECTIVE", "WRONG_ITEM", "OTHER");
    private final ReturnRequestRepository repository;
    private final OrderApi orderApi;
    private final PaymentApi paymentApi;
    private final Clock clock = Clock.systemUTC();

    ReturnApplicationService(ReturnRequestRepository repository, OrderApi orderApi, PaymentApi paymentApi) {
        this.repository=repository; this.orderApi=orderApi; this.paymentApi=paymentApi;
    }

    @Override
    @Transactional
    public ReturnView request(UUID memberId, UUID orderId, UUID orderItemId, int quantity,
                              String reason, String evidenceImageUrl, UUID commandId) {
        String normalizedReason=normalizeReason(reason);
        ReturnRequest replay=repository.findByMemberIdAndCommandId(memberId,commandId).orElse(null);
        if(replay!=null){
            if(!replay.matches(memberId,orderId,orderItemId,quantity,normalizedReason))
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            return replay.toView();
        }
        ReturnableOrderItemView item=orderApi.getReturnableItem(memberId,orderId,orderItemId);
        if(quantity<=0||quantity>item.getReturnableQuantity())
            throw new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY,"반품 가능한 수량을 초과했습니다.");
        BigDecimal gross=quantity==item.getReturnableQuantity()?item.getRefundableAmount()
                :item.getRefundableAmount().multiply(BigDecimal.valueOf(quantity))
                .divide(BigDecimal.valueOf(item.getReturnableQuantity()),2,RoundingMode.DOWN);
        BigDecimal fee=SELLER_RESPONSIBLE.contains(normalizedReason)?BigDecimal.ZERO.setScale(2)
                :CUSTOMER_RETURN_FEE.min(gross);
        BigDecimal refund=gross.subtract(fee);
        BigDecimal pointRefund=refund.multiply(item.getPointRatio()).setScale(2,RoundingMode.DOWN);
        BigDecimal paymentRefund=refund.subtract(pointRefund);
        return repository.save(new ReturnRequest(commandId,memberId,orderId,orderItemId,
                item.getProductId(),item.getProductName(),quantity,normalizedReason,
                normalizeEvidenceUrl(evidenceImageUrl),gross,fee,refund,pointRefund,
                paymentRefund,clock.instant())).toView();
    }

    @Override @Transactional(readOnly=true)
    public PageResponse<ReturnView> getMine(UUID memberId,int page,int size){
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Order.desc("requestedAt"),Sort.Order.desc("id")));
        return PageResponse.from(repository.findAllByMemberId(memberId,pageable).map(ReturnRequest::toView));
    }
    @Override @Transactional(readOnly=true)
    public PageResponse<ReturnView> getAll(int page,int size){
        var pageable=PageRequest.of(page,size,Sort.by(Sort.Order.desc("requestedAt"),Sort.Order.desc("id")));
        return PageResponse.from(repository.findAll(pageable).map(ReturnRequest::toView));
    }

    @Override
    @Transactional
    public ReturnView changeStatus(UUID adminId,UUID returnId,String status,String adminNote,UUID commandId){
        ReturnRequest request=repository.findByIdForUpdate(returnId)
                .orElseThrow(()->new BusinessException(ErrorCode.DATA_NOT_FOUND,"반품 요청을 찾을 수 없습니다."));
        ReturnStatus next;
        try{next=ReturnStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));}
        catch(RuntimeException exception){throw new BusinessException(ErrorCode.INVALID_PARAMETER,"반품 상태가 올바르지 않습니다.");}
        if(next==ReturnStatus.REFUNDED){
            if(request.status()!=ReturnStatus.APPROVED)
                throw new BusinessException(ErrorCode.INVALID_PARAMETER,"승인된 반품만 환불할 수 있습니다.");
            if(request.paymentRefundAmount().signum()>0)
                paymentApi.refundByOrder(request.memberId(),request.orderId(),
                        request.paymentRefundAmount(),commandId);
            ReturnSettlementView settlement=orderApi.settleReturn(request.memberId(),request.orderId(),
                    request.orderItemId(),request.quantity(),request.refundAmount(),
                    request.pointRefundAmount(),commandId);
            request.complete(settlement.getBalanceAfter(),adminId,adminNote,clock.instant());
        }else{
            request.transition(next,adminId,adminNote,clock.instant());
        }
        return request.toView();
    }

    private String normalizeReason(String reason){
        String normalized=reason==null?"":reason.trim().toUpperCase(Locale.ROOT);
        if(!REASONS.contains(normalized))throw new BusinessException(ErrorCode.INVALID_PARAMETER,"반품 사유가 올바르지 않습니다.");
        return normalized;
    }
    private String normalizeEvidenceUrl(String url){
        if(url==null||url.isBlank())return null;
        String value=url.trim();
        if(value.length()>1000||!value.startsWith("https://"))
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,"증빙 이미지는 HTTPS URL이어야 합니다.");
        return value;
    }
}
