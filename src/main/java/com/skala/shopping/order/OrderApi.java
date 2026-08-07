package com.skala.shopping.order;

import com.skala.shopping.common.PageResponse;
import java.util.List;
import java.util.UUID;

public interface OrderApi {

    /**
     * Places one product order. The command id is scoped to the member and can only be replayed
     * with the same product and quantity.
     */
    OrderView placeOrder(UUID memberId, UUID productId, int quantity, UUID commandId);

    OrderView placeOrder(UUID memberId, List<OrderLineCommand> items,
                         ShippingAddressCommand shippingAddress, UUID commandId);

    /**
     * Cancels the requested quantity of a product from the member's newest cancelable purchases
     * first. The command id is scoped to the member and can only be replayed with the same product
     * and quantity.
     */
    CancellationView cancelProduct(UUID memberId, UUID productId, int quantity, UUID commandId);

    OrderView getOrder(UUID memberId, UUID orderId);

    PageResponse<OrderView> getOrders(UUID memberId, int page, int size);

    List<PurchasedProductView> getPurchasedProducts(UUID memberId);
}
