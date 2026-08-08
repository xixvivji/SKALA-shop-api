package com.skala.shopping.order.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.order.OrderLineCommand;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTests {

    @Mock private ShopOrderRepository orderRepository;
    @Mock private OrderItemRepository itemRepository;
    @Mock private OrderCancellationRepository cancellationRepository;
    @Mock private OrderShippingAddressRepository shippingAddressRepository;
    @Mock private OrderStatusHistoryRepository statusHistoryRepository;
    @Mock private ProductReader productReader;
    @Mock private PointManager pointManager;
    @Mock private StockManager stockManager;
    @Mock private CouponManager couponManager;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OrderRequestLock requestLock;

    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OrderApplicationService(
                orderRepository,
                itemRepository,
                cancellationRepository,
                shippingAddressRepository,
                statusHistoryRepository,
                productReader,
                pointManager,
                stockManager,
                couponManager,
                eventPublisher,
                requestLock
        );
    }

    @Test
    void rejectsMissingShippingAddressBeforeAnyCollaboratorIsCalled() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.placeOrder(
                        UUID.randomUUID(),
                        List.of(new OrderLineCommand(UUID.randomUUID(), 1)),
                        null,
                        UUID.randomUUID()
                )
        );

        assertEquals(ErrorCode.INVALID_PARAMETER, exception.errorCode());
        assertEquals("배송지를 입력해야 합니다.", exception.getMessage());
        verifyNoInteractions(
                orderRepository,
                itemRepository,
                cancellationRepository,
                shippingAddressRepository,
                statusHistoryRepository,
                productReader,
                pointManager,
                stockManager,
                couponManager,
                eventPublisher,
                requestLock
        );
    }
}
