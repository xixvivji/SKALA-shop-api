package com.skala.shopping.order;

import java.util.List;
import java.util.UUID;

public interface OrderApi {

    /**
     * Places one product order. The command id is scoped to the member and can only be replayed
     * with the same product and quantity.
     */
    OrderView placeOrder(UUID memberId, UUID productId, int quantity, UUID commandId);

    /**
     * Cancels the requested quantity of a product from the member's newest cancelable purchases
     * first. The command id is scoped to the member and can only be replayed with the same product
     * and quantity.
     */
    CancellationView cancelProduct(UUID memberId, UUID productId, int quantity, UUID commandId);

    List<OrderView> getOrders(UUID memberId);

    List<PurchasedProductView> getPurchasedProducts(UUID memberId);
}
