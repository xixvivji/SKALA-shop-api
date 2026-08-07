package com.skala.shopping.stockalert.internal;

import com.skala.shopping.inventory.StockReplenished;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class StockReplenishedListener {

    private final StockAlertApplicationService service;

    StockReplenishedListener(StockAlertApplicationService service) {
        this.service = service;
    }

    @EventListener
    void on(StockReplenished event) {
        service.notifySubscribers(
                event.getProductId(),
                event.getAvailableQuantity(),
                event.getOccurredAt()
        );
    }
}
