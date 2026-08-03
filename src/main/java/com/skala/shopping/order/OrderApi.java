package com.skala.shopping.order;

import java.util.List;
import java.util.UUID;

public interface OrderApi {

    OrderView placeOrder(UUID memberId, UUID productId, int quantity, UUID commandId);

    CancellationView cancelProduct(UUID memberId, UUID productId, int quantity, UUID commandId);

    List<OrderView> getOrders(UUID memberId);

    List<PurchasedProductView> getPurchasedProducts(UUID memberId);
}
