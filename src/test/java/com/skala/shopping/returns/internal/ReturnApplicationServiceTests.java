package com.skala.shopping.returns.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.ReturnSettlementView;
import com.skala.shopping.order.ReturnableOrderItemView;
import com.skala.shopping.payment.PaymentApi;
import com.skala.shopping.returns.ReturnView;
import com.skala.shopping.returns.internal.domain.ReturnRequest;
import com.skala.shopping.returns.internal.domain.ReturnStatus;
import com.skala.shopping.returns.internal.domain.ReturnStatusCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReturnApplicationServiceTests {

    @Mock
    ReturnRequestRepository repository;

    @Mock
    ReturnStatusCommandRepository statusCommandRepository;

    @Mock
    ReturnCommandLock commandLock;

    @Mock
    OrderApi orderApi;

    @Mock
    PaymentApi paymentApi;

    ReturnApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ReturnApplicationService(
                repository,
                statusCommandRepository,
                commandLock,
                orderApi,
                paymentApi
        );
    }

    @Test
    void treatsEvidenceUrlAsPartOfRequestIdempotencyFingerprint() {
        UUID memberId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        ReturnRequest existing = returnRequest(
                commandId,
                memberId,
                orderId,
                orderItemId,
                1,
                "DAMAGED",
                "https://example.com/first.jpg"
        );
        when(repository.findByCommandId(commandId)).thenReturn(Optional.of(existing));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.request(
                memberId,
                orderId,
                orderItemId,
                1,
                "damaged",
                "https://example.com/other.jpg",
                commandId
        ));

        assertEquals(ErrorCode.IDEMPOTENCY_CONFLICT, exception.errorCode());
        verify(orderApi, never()).getReturnableItem(any(), any(), any());
    }

    @Test
    void rejectsQuantityAlreadyReservedByAnActiveReturn() {
        UUID memberId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        when(repository.findByCommandId(commandId)).thenReturn(Optional.empty());
        when(orderApi.getReturnableItem(memberId, orderId, orderItemId))
                .thenReturn(returnableItem(memberId, orderId, orderItemId, 2, "20000.00"));
        when(repository.sumQuantityByOrderItemIdAndStatusIn(any(), any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.request(
                memberId,
                orderId,
                orderItemId,
                2,
                "CHANGE_OF_MIND",
                null,
                commandId
        ));

        assertEquals(ErrorCode.INSUFFICIENT_QUANTITY, exception.errorCode());
        verify(repository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void allocatesRemainingGrossAfterCompletedReturnWithoutChargingRetainedFeeAgain() {
        UUID memberId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        when(repository.findByCommandId(commandId)).thenReturn(Optional.empty());
        when(orderApi.getReturnableItem(memberId, orderId, orderItemId))
                .thenReturn(returnableItem(memberId, orderId, orderItemId, 2, "13000.00"));
        when(repository.sumQuantityByOrderItemIdAndStatusIn(any(), any())).thenReturn(0L);
        when(repository.sumGrossRefundAmountByOrderItemIdAndStatusIn(any(), any()))
                .thenReturn(BigDecimal.ZERO.setScale(2));
        when(repository.sumShippingFeeByOrderItemIdAndStatus(any(), any()))
                .thenReturn(new BigDecimal("3000.00"));
        when(repository.save(any(ReturnRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnView result = service.request(
                memberId,
                orderId,
                orderItemId,
                2,
                "DAMAGED",
                null,
                commandId
        );

        assertEquals(new BigDecimal("10000.00"), result.getGrossRefundAmount());
        assertEquals(2, result.getQuantity());
    }

    @Test
    void replaysOriginalStatusSnapshotAfterReturnHasAdvanced() {
        UUID adminId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        ReturnRequest request = returnRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "DAMAGED",
                null
        );
        request.transition(ReturnStatus.COLLECTING, adminId, "수거 시작", Instant.parse("2026-01-01T00:00:01Z"));
        ReturnStatusCommand command = new ReturnStatusCommand(
                commandId,
                request.id(),
                adminId,
                ReturnStatus.COLLECTING,
                "수거 시작",
                request.toView()
        );
        request.transition(ReturnStatus.INSPECTING, adminId, "검수 중", Instant.parse("2026-01-01T00:00:02Z"));
        when(statusCommandRepository.findById(commandId)).thenReturn(Optional.of(command));
        when(repository.findById(request.id())).thenReturn(Optional.of(request));

        ReturnView replay = service.changeStatus(
                adminId,
                request.id(),
                "collecting",
                " 수거 시작 ",
                commandId
        );

        assertEquals("COLLECTING", replay.getStatus());
        assertEquals("수거 시작", replay.getAdminNote());
        assertEquals(Instant.parse("2026-01-01T00:00:01Z"), replay.getUpdatedAt());
        verify(repository, never()).findByIdForUpdate(any());
    }

    @Test
    void refundStatusRetryDoesNotRepeatPaymentOrOrderSettlement() {
        UUID adminId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        ReturnRequest request = returnRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "DAMAGED",
                null
        );
        request.transition(ReturnStatus.COLLECTING, adminId, null, Instant.parse("2026-01-01T00:00:01Z"));
        request.transition(ReturnStatus.INSPECTING, adminId, null, Instant.parse("2026-01-01T00:00:02Z"));
        request.transition(ReturnStatus.APPROVED, adminId, null, Instant.parse("2026-01-01T00:00:03Z"));

        AtomicReference<ReturnStatusCommand> storedCommand = new AtomicReference<>();
        when(statusCommandRepository.findById(commandId))
                .thenAnswer(invocation -> Optional.ofNullable(storedCommand.get()));
        when(statusCommandRepository.save(any(ReturnStatusCommand.class))).thenAnswer(invocation -> {
            ReturnStatusCommand saved = invocation.getArgument(0);
            storedCommand.set(saved);
            return saved;
        });
        when(repository.findByIdForUpdate(request.id())).thenReturn(Optional.of(request));
        when(repository.findById(request.id())).thenReturn(Optional.of(request));
        when(orderApi.settleReturn(any(), any(), any(), any(Integer.class), any(), any(), any()))
                .thenReturn(new ReturnSettlementView(
                        new BigDecimal("10000.00"),
                        new BigDecimal("5000.00"),
                        new BigDecimal("1005000.00")
                ));

        ReturnView first = service.changeStatus(
                adminId,
                request.id(),
                "REFUNDED",
                "환불 완료",
                commandId
        );
        ReturnView replay = service.changeStatus(
                adminId,
                request.id(),
                "refunded",
                " 환불 완료 ",
                commandId
        );

        assertEquals("REFUNDED", first.getStatus());
        assertEquals(first.getUpdatedAt(), replay.getUpdatedAt());
        assertEquals(first.getBalanceAfter(), replay.getBalanceAfter());
        verify(paymentApi).refundByOrder(any(), any(), any(), any());
        verify(orderApi).settleReturn(any(), any(), any(), any(Integer.class), any(), any(), any());
    }

    @Test
    void normalizesReturnTimestampsToPostgresMicrosecondPrecision() {
        ReturnRequest request = new ReturnRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "테스트 상품",
                1,
                "DAMAGED",
                null,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("10000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00"),
                Instant.parse("2026-01-01T00:00:00.123456789Z")
        );

        assertEquals(
                Instant.parse("2026-01-01T00:00:00.123456Z"),
                request.toView().getRequestedAt()
        );
        request.transition(
                ReturnStatus.COLLECTING,
                UUID.randomUUID(),
                null,
                Instant.parse("2026-01-01T00:00:01.987654321Z")
        );
        assertEquals(
                Instant.parse("2026-01-01T00:00:01.987654Z"),
                request.toView().getUpdatedAt()
        );
    }

    private ReturnRequest returnRequest(
            UUID commandId,
            UUID memberId,
            UUID orderId,
            UUID orderItemId,
            int quantity,
            String reason,
            String evidenceUrl
    ) {
        return new ReturnRequest(
                commandId,
                memberId,
                orderId,
                orderItemId,
                UUID.randomUUID(),
                "테스트 상품",
                quantity,
                reason,
                evidenceUrl,
                new BigDecimal("10000.00"),
                BigDecimal.ZERO.setScale(2),
                new BigDecimal("10000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("5000.00"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
    }

    private ReturnableOrderItemView returnableItem(
            UUID memberId,
            UUID orderId,
            UUID orderItemId,
            int returnableQuantity,
            String refundableAmount
    ) {
        return new ReturnableOrderItemView(
                orderId,
                orderItemId,
                memberId,
                UUID.randomUUID(),
                "테스트 상품",
                returnableQuantity,
                new BigDecimal(refundableAmount),
                new BigDecimal("0.50000000")
        );
    }
}
