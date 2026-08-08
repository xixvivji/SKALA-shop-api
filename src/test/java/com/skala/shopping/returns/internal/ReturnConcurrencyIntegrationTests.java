package com.skala.shopping.returns.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.order.internal.domain.FulfillmentStatus;
import com.skala.shopping.order.internal.domain.OrderItem;
import com.skala.shopping.order.internal.domain.ShopOrder;
import com.skala.shopping.returns.ReturnApi;
import com.skala.shopping.returns.ReturnView;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = "shopping.security.rate-limit.enabled=false")
class ReturnConcurrencyIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    ReturnApi returnApi;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void serializesDifferentCommandsSoActiveReturnsCannotOverReserveOneOrderItem() throws Exception {
        DeliveredOrder delivered = createDeliveredOrder(2);

        List<String> outcomes = concurrently(
                () -> requestOutcome(delivered, 2, UUID.randomUUID()),
                () -> requestOutcome(delivered, 2, UUID.randomUUID())
        );
        Collections.sort(outcomes);

        assertEquals(List.of("INSUFFICIENT_QUANTITY", "SUCCESS"), outcomes);
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from returns.return_requests where order_item_id = ?",
                Integer.class,
                delivered.orderItemId()
        ));
        assertEquals(2, jdbcTemplate.queryForObject(
                "select sum(quantity) from returns.return_requests where order_item_id = ?",
                Integer.class,
                delivered.orderItemId()
        ));
    }

    @Test
    void permitsRejectedFollowUpAndReplaysTheOriginalStatusCommandSnapshot() {
        DeliveredOrder delivered = createDeliveredOrder(2);
        UUID adminId = UUID.randomUUID();
        ReturnView rejected = returnApi.request(
                delivered.memberId(),
                delivered.orderId(),
                delivered.orderItemId(),
                1,
                "SIZE_MISMATCH",
                null,
                UUID.randomUUID()
        );
        rejected = returnApi.changeStatus(adminId, rejected.getId(), "COLLECTING", null, UUID.randomUUID());
        rejected = returnApi.changeStatus(adminId, rejected.getId(), "INSPECTING", null, UUID.randomUUID());
        rejected = returnApi.changeStatus(adminId, rejected.getId(), "REJECTED", "사용 흔적", UUID.randomUUID());
        assertEquals("REJECTED", rejected.getStatus());

        ReturnView followUp = returnApi.request(
                delivered.memberId(),
                delivered.orderId(),
                delivered.orderItemId(),
                2,
                "DAMAGED",
                "https://example.com/damaged.jpg",
                UUID.randomUUID()
        );
        UUID collectingCommand = UUID.randomUUID();
        ReturnView collecting = returnApi.changeStatus(
                adminId,
                followUp.getId(),
                "COLLECTING",
                "수거 시작",
                collectingCommand
        );
        returnApi.changeStatus(adminId, followUp.getId(), "INSPECTING", "검수 중", UUID.randomUUID());
        ReturnView replay = returnApi.changeStatus(
                adminId,
                followUp.getId(),
                "collecting",
                " 수거 시작 ",
                collectingCommand
        );

        assertEquals("COLLECTING", replay.getStatus());
        assertEquals(collecting.getUpdatedAt(), replay.getUpdatedAt());
        assertEquals("수거 시작", replay.getAdminNote());
        assertEquals(2, jdbcTemplate.queryForObject(
                "select count(*) from returns.return_requests where order_item_id = ?",
                Integer.class,
                delivered.orderItemId()
        ));
    }

    private DeliveredOrder createDeliveredOrder(int quantity) {
        UUID memberId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID[] orderItemId = new UUID[1];
        transactionTemplate.executeWithoutResult(status -> {
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            BigDecimal total = new BigDecimal("10000.00")
                    .multiply(BigDecimal.valueOf(quantity));
            ShopOrder order = new ShopOrder(
                    orderId,
                    UUID.randomUUID(),
                    "return-integration-fingerprint",
                    "RETURN-" + orderId,
                    memberId,
                    total,
                    BigDecimal.ZERO.setScale(2),
                    now
            );
            order.transitionFulfillment(FulfillmentStatus.PREPARING, now.plusSeconds(1));
            order.transitionFulfillment(FulfillmentStatus.SHIPPED, now.plusSeconds(2));
            order.transitionFulfillment(FulfillmentStatus.DELIVERED, now.plusSeconds(3));
            OrderItem item = new OrderItem(
                    orderId,
                    productId,
                    "반품 동시성 테스트 상품",
                    new BigDecimal("10000.00"),
                    quantity
            );
            entityManager.persist(order);
            entityManager.persist(item);
            orderItemId[0] = item.id();
        });
        return new DeliveredOrder(memberId, orderId, orderItemId[0]);
    }

    private String requestOutcome(DeliveredOrder delivered, int quantity, UUID commandId) {
        try {
            returnApi.request(
                    delivered.memberId(),
                    delivered.orderId(),
                    delivered.orderItemId(),
                    quantity,
                    "DAMAGED",
                    null,
                    commandId
            );
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.errorCode().name();
        }
    }

    private <T> List<T> concurrently(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        java.util.function.Function<Callable<T>, Callable<T>> synchronizedCall = action -> () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return action.call();
        };
        try {
            Future<T> firstResult = executor.submit(synchronizedCall.apply(first));
            Future<T> secondResult = executor.submit(synchronizedCall.apply(second));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<T> results = new ArrayList<>();
            results.add(firstResult.get(20, TimeUnit.SECONDS));
            results.add(secondResult.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static final class DeliveredOrder {
        private final UUID memberId;
        private final UUID orderId;
        private final UUID orderItemId;

        private DeliveredOrder(UUID memberId, UUID orderId, UUID orderItemId) {
            this.memberId = memberId;
            this.orderId = orderId;
            this.orderItemId = orderItemId;
        }

        private UUID memberId() {
            return memberId;
        }

        private UUID orderId() {
            return orderId;
        }

        private UUID orderItemId() {
            return orderItemId;
        }
    }
}
