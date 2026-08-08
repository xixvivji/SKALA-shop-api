package com.skala.shopping.outbox.internal;

import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.shopping.catalog.ProductCreated; import com.skala.shopping.inventory.StockReplenished;
import com.skala.shopping.order.OrderPlaced; import java.time.Clock; import java.util.UUID;
import org.springframework.stereotype.Component; import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
class OutboxEventRecorder {
    private final OutboxEventRepository repository; private final ObjectMapper mapper; private final Clock clock=Clock.systemUTC();
    OutboxEventRecorder(OutboxEventRepository repository,ObjectMapper mapper){this.repository=repository;this.mapper=mapper;}
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT) void on(OrderPlaced event){record("ORDER",event.getOrderId(),event);}
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT) void on(ProductCreated event){record("PRODUCT",event.getProductId(),event);}
    @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT) void on(StockReplenished event){record("STOCK",event.getProductId(),event);}
    private void record(String type,UUID id,Object event){try{repository.save(new OutboxEvent(type,id,event,mapper.writeValueAsString(event),clock.instant()));}
        catch(JsonProcessingException exception){throw new IllegalStateException("Outbox 이벤트 직렬화에 실패했습니다.",exception);}}
}
