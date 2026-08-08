package com.skala.shopping.order;

import com.skala.shopping.common.PageResponse;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public interface OrderApi {

    /**
     * Places one product order. The command id is scoped to the member and can only be replayed
     * with the same product and quantity.
     */
    OrderView placeOrder(UUID memberId, UUID productId, int quantity, UUID commandId);

    OrderView placeOrder(
            UUID memberId,
            UUID productId,
            int quantity,
            UUID commandId,
            String couponCode
    );

    OrderView placeOrder(
            UUID memberId,
            List<OrderLineCommand> items,
            ShippingAddressCommand shippingAddress,
            UUID commandId,
            String couponCode,
            BigDecimal pointAmount
    );

    PaymentOrderView getPaymentOrder(UUID memberId, UUID orderId);

    OrderView confirmExternalPayment(UUID memberId, UUID orderId, UUID paymentId);

    OrderView failExternalPayment(UUID memberId, UUID orderId, UUID paymentId);

    ReturnableOrderItemView getReturnableItem(UUID memberId, UUID orderId, UUID orderItemId);

    ReturnSettlementView settleReturn(UUID memberId, UUID orderId, UUID orderItemId,
                                      int quantity, BigDecimal refundAmount,
                                      BigDecimal pointRefundAmount, UUID commandId);

    OrderView placeOrder(UUID memberId, List<OrderLineCommand> items,
                         ShippingAddressCommand shippingAddress, UUID commandId);

    OrderView placeOrder(
            UUID memberId,
            List<OrderLineCommand> items,
            ShippingAddressCommand shippingAddress,
            UUID commandId,
            String couponCode
    );

    /**
     * Cancels the requested quantity of a product from the member's newest cancelable purchases
     * first. The command id is scoped to the member and can only be replayed with the same product
     * and quantity.
     */
    CancellationView cancelProduct(UUID memberId, UUID productId, int quantity, UUID commandId);

    OrderView getOrder(UUID memberId, UUID orderId);

    PageResponse<OrderView> getOrders(UUID memberId, int page, int size);

    List<PurchasedProductView> getPurchasedProducts(UUID memberId);

    boolean hasPurchasedProduct(UUID memberId, UUID productId);
}
