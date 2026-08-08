package com.skala.shopping.order.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.coupon.CouponDiscount;
import com.skala.shopping.order.CancellationView;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.OrderItemView;
import com.skala.shopping.order.OrderLineCommand;
import com.skala.shopping.order.OrderStatusHistoryView;
import com.skala.shopping.order.OrderView;
import com.skala.shopping.order.PurchasedProductView;
import com.skala.shopping.order.PaymentOrderView;
import com.skala.shopping.order.ReturnableOrderItemView;
import com.skala.shopping.order.ReturnSettlementView;
import com.skala.shopping.order.ShippingAddressCommand;
import com.skala.shopping.order.ShippingAddressView;
import com.skala.shopping.order.internal.domain.OrderCancellation;
import com.skala.shopping.order.internal.domain.FulfillmentStatus;
import com.skala.shopping.order.internal.domain.OrderItem;
import com.skala.shopping.order.internal.domain.OrderShippingAddress;
import com.skala.shopping.order.internal.domain.OrderStatusHistory;
import com.skala.shopping.order.internal.domain.ShopOrder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService implements OrderApi {

    private static final DateTimeFormatter ORDER_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final int MAX_ORDER_QUANTITY = 1_000_000;
    private static final int MAX_ORDER_ITEMS = 50;
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("30000000000000.00");

    private final ShopOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderCancellationRepository cancellationRepository;
    private final OrderShippingAddressRepository shippingAddressRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ProductReader productReader;
    private final PointManager pointManager;
    private final StockManager stockManager;
    private final CouponManager couponManager;
    private final Clock clock = Clock.systemUTC();

    OrderApplicationService(
            ShopOrderRepository orderRepository,
            OrderItemRepository itemRepository,
            OrderCancellationRepository cancellationRepository,
            OrderShippingAddressRepository shippingAddressRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            ProductReader productReader,
            PointManager pointManager,
            StockManager stockManager,
            CouponManager couponManager
    ) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.cancellationRepository = cancellationRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productReader = productReader;
        this.pointManager = pointManager;
        this.stockManager = stockManager;
        this.couponManager = couponManager;
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, UUID productId, int quantity, UUID commandId) {
        return placeOrder(memberId, List.of(new OrderLineCommand(productId, quantity)), null, commandId, null);
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, UUID productId, int quantity,
                               UUID commandId, String couponCode) {
        return placeOrder(memberId, List.of(new OrderLineCommand(productId, quantity)), null,
                commandId, couponCode);
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, List<OrderLineCommand> items,
                                ShippingAddressCommand shippingAddress, UUID commandId) {
        return placeOrder(memberId, items, shippingAddress, commandId, null);
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, List<OrderLineCommand> items,
                                ShippingAddressCommand shippingAddress, UUID commandId,
                                String couponCode) {
        return placeOrder(memberId, items, shippingAddress, commandId, couponCode, null);
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, List<OrderLineCommand> items,
                                ShippingAddressCommand shippingAddress, UUID commandId,
                                String couponCode, BigDecimal pointAmount) {
        List<OrderLineCommand> normalizedItems = validateAndSortItems(items);
        validateShippingAddress(shippingAddress);
        String normalizedCoupon = normalizeText(couponCode);
        String fingerprint = OrderCommandFingerprint.order(
                memberId, normalizedItems, shippingAddress, normalizedCoupon, pointAmount);
        return orderRepository.findByMemberIdAndRequestId(memberId, commandId)
                .map(order -> replayOrder(order, fingerprint))
                .orElseGet(() -> createOrder(
                        memberId,
                        normalizedItems,
                        shippingAddress,
                        commandId,
                        fingerprint,
                        normalizedCoupon,
                        pointAmount
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOrderView getPaymentOrder(UUID memberId, UUID orderId) {
        ShopOrder order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다."));
        return new PaymentOrderView(order.id(), order.memberId(), order.paymentAmount(),
                order.isPaymentPending() ? "PAYMENT_PENDING" : "NOT_PAYABLE");
    }

    @Override
    @Transactional
    public OrderView confirmExternalPayment(UUID memberId, UUID orderId, UUID paymentId) {
        ShopOrder order = lockedMemberOrder(memberId, orderId);
        boolean wasPending = order.isPaymentPending();
        order.confirmPayment(clock.instant());
        if (wasPending) {
            statusHistoryRepository.save(new OrderStatusHistory(
                    orderId, FulfillmentStatus.PAYMENT_PENDING, FulfillmentStatus.PAID,
                    null, clock.instant()));
            if (order.usedCouponCode() != null) {
                CouponDiscount coupon = couponManager.applyPreview(
                        memberId, order.usedCouponCode(), order.originalAmount());
                couponManager.applyUsage(memberId, orderId, order.requestId(), coupon);
            }
        }
        return toCreationView(order);
    }

    @Override
    @Transactional
    public OrderView failExternalPayment(UUID memberId, UUID orderId, UUID paymentId) {
        ShopOrder order = lockedMemberOrder(memberId, orderId);
        if (!order.isPaymentPending()) return toCreationView(order);
        BigDecimal restoredBalance = order.pointUsedAmount().signum() == 0
                ? pointManager.balance(memberId)
                : pointManager.credit(memberId, order.pointUsedAmount(), orderId, paymentId);
        for (OrderItem item : itemRepository.findAllByOrderIdOrderByLineNumberAsc(orderId)) {
            stockManager.release(item.variantId(), item.availableQuantity(), paymentId);
        }
        order.failPayment(restoredBalance, clock.instant());
        return toCreationView(order);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnableOrderItemView getReturnableItem(UUID memberId, UUID orderId, UUID orderItemId) {
        ShopOrder order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (order.fulfillmentStatus() != FulfillmentStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배송 완료 주문만 반품할 수 있습니다.");
        }
        OrderItem item = itemRepository.findById(orderItemId)
                .filter(candidate -> candidate.orderId().equals(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문 항목을 찾을 수 없습니다."));
        BigDecimal pointRatio = order.totalAmount().signum() == 0 ? BigDecimal.ZERO
                : order.pointUsedAmount().divide(order.totalAmount(), 8, RoundingMode.DOWN);
        return new ReturnableOrderItemView(orderId, item.id(), memberId, item.productId(),
                item.productName(), item.availableQuantity(),
                item.refundableAmount(item.availableQuantity()), pointRatio);
    }

    @Override
    @Transactional
    public ReturnSettlementView settleReturn(UUID memberId, UUID orderId, UUID orderItemId,
                                             int quantity, BigDecimal refundAmount,
                                             BigDecimal pointRefundAmount, UUID commandId) {
        requirePositiveQuantity(quantity);
        ShopOrder order = lockedMemberOrder(memberId, orderId);
        if (order.fulfillmentStatus() != FulfillmentStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배송 완료 주문만 반품 정산할 수 있습니다.");
        }
        OrderItem item = itemRepository.findByIdAndOrderIdForUpdate(orderItemId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문 항목을 찾을 수 없습니다."));
        if (refundAmount == null || pointRefundAmount == null || refundAmount.signum() < 0
                || pointRefundAmount.signum() < 0 || pointRefundAmount.compareTo(refundAmount) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "반품 정산 금액이 올바르지 않습니다.");
        }
        item.returnQuantity(quantity, refundAmount);
        BigDecimal balance = pointRefundAmount.signum() == 0
                ? pointManager.balance(memberId)
                : pointManager.credit(memberId, pointRefundAmount, orderId, commandId);
        stockManager.release(item.variantId(), quantity, commandId);
        boolean fullyReturned = itemRepository.findAllByOrderIdOrderByLineNumberAsc(orderId)
                .stream().allMatch(candidate -> candidate.availableQuantity() == 0);
        order.applyCancellation(refundAmount, fullyReturned, clock.instant());
        return new ReturnSettlementView(refundAmount, pointRefundAmount, balance);
    }

    @Override
    @Transactional
    public CancellationView cancelProduct(
            UUID memberId,
            UUID productId,
            int quantity,
            UUID commandId
    ) {
        requirePositiveQuantity(quantity);
        String fingerprint = OrderCommandFingerprint.cancellation(memberId, productId, quantity);
        return cancellationRepository.findByMemberIdAndCommandId(memberId, commandId)
                .map(cancellation -> replayCancellation(cancellation, fingerprint))
                .orElseGet(() -> executeCancellation(
                        memberId,
                        productId,
                        quantity,
                        commandId,
                        fingerprint
                ));
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderView getOrder(UUID memberId, UUID orderId) {
        ShopOrder order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다."));
        List<OrderItemView> items = itemRepository.findAllByOrderIdOrderByLineNumberAsc(order.id())
                .stream().map(OrderItem::toView).toList();
        OrderView orderView = order.toView(items);
        List<OrderStatusHistoryView> history = statusHistoryRepository
                .findAllByOrderIdOrderByChangedAtAscIdAsc(orderId)
                .stream().map(OrderStatusHistory::toView).toList();
        return attachAddress(orderView.withStatusHistory(history));
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<OrderView> getOrders(UUID memberId, int page, int size) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("orderedAt"), Sort.Order.desc("id"))
        );
        var orders = orderRepository.findAllByMemberId(memberId, pageable);
        Map<UUID, List<OrderItemView>> itemsByOrder = orders.isEmpty()
                ? Map.of()
                : itemRepository
                        .findAllByOrderIdInOrderByOrderIdAscLineNumberAsc(
                                orders.stream().map(ShopOrder::id).toList()
                        )
                        .stream()
                        .collect(Collectors.groupingBy(
                                OrderItem::orderId,
                                Collectors.mapping(OrderItem::toView, Collectors.toList())
                        ));
        Map<UUID, ShippingAddressView> addresses = shippingAddresses(
                orders.stream().map(ShopOrder::id).toList());
        return PageResponse.from(orders.map(
                order -> withAddress(order.toView(itemsByOrder.getOrDefault(order.id(), List.of())), addresses)
        ));
    }

    @Transactional(readOnly = true)
    public List<PurchasedProductView> getPurchasedProducts(UUID memberId) {
        Map<UUID, ProductAccumulator> products = new LinkedHashMap<>();
        for (OrderItem item : itemRepository.findPurchasedItems(memberId)) {
            products.computeIfAbsent(
                    item.productId(),
                    ignored -> new ProductAccumulator(
                            item.productId(),
                            item.productName(),
                            item.unitPrice()
                    )
            ).add(item.availableQuantity());
        }
        return products.values().stream()
                .map(ProductAccumulator::toView)
                .sorted(Comparator.comparing(PurchasedProductView::getProductName)
                        .thenComparing(PurchasedProductView::getProductId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPurchasedProduct(UUID memberId, UUID productId) {
        return memberId != null && productId != null
                && itemRepository.hasPurchasedProduct(memberId, productId);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PageResponse<OrderView> getAllOrders(int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("orderedAt"), Sort.Order.desc("id")));
        var orders = orderRepository.findAll(pageable);
        Map<UUID, List<OrderItemView>> itemsByOrder = orders.isEmpty() ? Map.of()
                : itemRepository.findAllByOrderIdInOrderByOrderIdAscLineNumberAsc(
                        orders.stream().map(ShopOrder::id).toList()).stream()
                .collect(Collectors.groupingBy(OrderItem::orderId,
                        Collectors.mapping(OrderItem::toView, Collectors.toList())));
        Map<UUID, ShippingAddressView> addresses = shippingAddresses(
                orders.stream().map(ShopOrder::id).toList());
        return PageResponse.from(orders.map(order ->
                withAddress(order.toView(itemsByOrder.getOrDefault(order.id(), List.of())), addresses)));
    }

    @Transactional
    public OrderView changeFulfillment(UUID adminId, UUID orderId, String requestedStatus) {
        return changeFulfillment(adminId, orderId, requestedStatus, null, null, null, null);
    }

    @Transactional
    public OrderView changeFulfillment(
            UUID adminId,
            UUID orderId,
            String requestedStatus,
            String trackingCarrier,
            String trackingNumber,
            String trackingUrl,
            Instant estimatedDeliveryAt
    ) {
        ShopOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (hasText(requestedStatus)) {
            FulfillmentStatus next;
            try {
                next = FulfillmentStatus.valueOf(requestedStatus);
            } catch (RuntimeException exception) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배송 상태가 올바르지 않습니다.");
            }
            FulfillmentStatus previous = order.fulfillmentStatus();
            try {
                order.transitionFulfillment(next, clock.instant());
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                        "배송 상태는 PAID → PREPARING → SHIPPED → DELIVERED 순서로만 변경할 수 있습니다.");
            }
            statusHistoryRepository.save(new OrderStatusHistory(
                    order.id(), previous, next, adminId, clock.instant()));
        }

        if (trackingCarrier != null || trackingNumber != null || trackingUrl != null
                || estimatedDeliveryAt != null) {
            order.applyTracking(
                    trackingCarrier,
                    trackingNumber,
                    trackingUrl,
                    estimatedDeliveryAt,
                    clock.instant()
            );
        }
        return attachAddress(order.toView(
                itemRepository.findAllByOrderIdOrderByLineNumberAsc(order.id())
                        .stream().map(OrderItem::toView).toList()
        ));
    }

    @Transactional(readOnly=true)
    public List<OrderStatusHistoryView> getStatusHistory(UUID orderId){
        if(!orderRepository.existsById(orderId)) throw new BusinessException(ErrorCode.DATA_NOT_FOUND,"주문을 찾을 수 없습니다.");
        return statusHistoryRepository.findAllByOrderIdOrderByChangedAtAscIdAsc(orderId)
                .stream().map(OrderStatusHistory::toView).toList();
    }

    private OrderView createOrder(
            UUID memberId,
            List<OrderLineCommand> lines,
            ShippingAddressCommand shippingAddress,
            UUID commandId,
            String fingerprint,
            String couponCode,
            BigDecimal requestedPointAmount
    ) {
        List<OrderProduct> products = lines.stream()
                .map(line -> productReader.getSaleableProduct(line.getProductId(), line.getVariantId()))
                .toList();
        BigDecimal originalAmount = BigDecimal.ZERO;
        for (int index = 0; index < lines.size(); index++) {
            originalAmount = originalAmount.add(products.get(index).getPrice()
                    .multiply(BigDecimal.valueOf(lines.get(index).getQuantity())));
        }
        if (originalAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "주문 총액 한도를 초과했습니다.");
        }
        CouponDiscount coupon = couponManager.applyPreview(memberId, couponCode, originalAmount);
        BigDecimal discountAmount = coupon.getDiscountAmount();
        BigDecimal finalAmount = originalAmount.subtract(discountAmount);
        if (finalAmount.signum() <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "쿠폰 할인액은 주문 금액보다 작아야 합니다."
            );
        }
        BigDecimal pointAmount = normalizePointAmount(requestedPointAmount, finalAmount);
        BigDecimal paymentAmount = finalAmount.subtract(pointAmount);
        UUID orderId = UUID.randomUUID();
        BigDecimal remainingPoints = pointAmount.signum() == 0
                ? pointManager.balance(memberId)
                : pointManager.debit(memberId, pointAmount, orderId, commandId);
        // 포인트 계정 잠금을 기다리는 사이 동일 멱등 요청이 완료됐을 수 있다.
        // 이 시점에 재확인해야 중복 재고 차감과 주문 UNIQUE 충돌을 피할 수 있다.
        var concurrentReplay = orderRepository.findByMemberIdAndRequestId(memberId, commandId);
        if (concurrentReplay.isPresent()) {
            return replayOrder(concurrentReplay.get(), fingerprint);
        }
        for (OrderLineCommand line : lines) {
            stockManager.reserve(line.getVariantId(), line.getQuantity(), orderId);
        }
        var now = clock.instant();
        ShopOrder order = orderRepository.save(new ShopOrder(
                orderId,
                commandId,
                fingerprint,
                orderNumber(orderId, now),
                memberId,
                finalAmount,
                originalAmount,
                discountAmount,
                coupon.getCouponCode(),
                pointAmount,
                paymentAmount,
                remainingPoints,
                now
        ));
        if (paymentAmount.signum() == 0) {
            statusHistoryRepository.save(new OrderStatusHistory(
                    orderId, null, FulfillmentStatus.PAID, null, now));
        }
        List<BigDecimal> paidAmounts = allocatePaidAmounts(lines, products, finalAmount);
        for (int index = 0; index < lines.size(); index++) {
            OrderProduct product = products.get(index);
            itemRepository.save(new OrderItem(
                    orderId, product.getId(), product.getVariantId(), product.getSku(),
                    product.getOptionName(), product.getOptionValue(), product.getName(), product.getPrice(),
                    lines.get(index).getQuantity(), paidAmounts.get(index), index));
        }
        if (paymentAmount.signum() == 0 && coupon.getCouponId() != null) {
            couponManager.applyUsage(memberId, orderId, commandId, coupon);
        }
        OrderView view = order.toView(
                itemRepository.findAllByOrderIdOrderByLineNumberAsc(orderId)
                        .stream().map(OrderItem::toView).toList()
        );
        if (shippingAddress != null) {
            ShippingAddressView savedAddress = orderShippingAddressRepositorySave(
                    order.id(),
                    shippingAddress
            );
            return view.withShippingAddress(savedAddress);
        }
        return view;
    }

    /**
     * 쿠폰이 주문 전체에 적용되므로 실제 결제액을 각 주문 항목에 원가 비율로 배분한다.
     * 마지막 항목이 소수점 절사 오차를 흡수하여 항목 결제액 합계가 주문 결제액과 정확히 일치한다.
     */
    private List<BigDecimal> allocatePaidAmounts(
            List<OrderLineCommand> lines,
            List<OrderProduct> products,
            BigDecimal finalAmount
    ) {
        BigDecimal originalAmount = BigDecimal.ZERO;
        for (int index = 0; index < lines.size(); index++) {
            originalAmount = originalAmount.add(products.get(index).getPrice()
                    .multiply(BigDecimal.valueOf(lines.get(index).getQuantity())));
        }
        List<BigDecimal> allocations = new java.util.ArrayList<>(lines.size());
        BigDecimal allocated = BigDecimal.ZERO.setScale(2);
        for (int index = 0; index < lines.size(); index++) {
            BigDecimal amount;
            if (index == lines.size() - 1) {
                amount = finalAmount.subtract(allocated);
            } else {
                BigDecimal lineOriginal = products.get(index).getPrice()
                        .multiply(BigDecimal.valueOf(lines.get(index).getQuantity()));
                amount = finalAmount.multiply(lineOriginal)
                        .divide(originalAmount, 2, RoundingMode.DOWN);
            }
            allocations.add(amount);
            allocated = allocated.add(amount);
        }
        return allocations;
    }

    private ShippingAddressView orderShippingAddressRepositorySave(
            UUID orderId,
            ShippingAddressCommand shippingAddress
    ) {
        OrderShippingAddress saved = shippingAddressRepository.save(new OrderShippingAddress(orderId, shippingAddress));
        return saved.toView();
    }

    private CancellationView executeCancellation(
            UUID memberId,
            UUID productId,
            int quantity,
            UUID commandId,
            String fingerprint
    ) {
        List<OrderItem> items = itemRepository.findCancelableItems(memberId, productId);
        var replay = cancellationRepository.findByMemberIdAndCommandId(memberId, commandId);
        if (replay.isPresent()) {
            return replayCancellation(replay.get(), fingerprint);
        }
        int available = items.stream().mapToInt(OrderItem::availableQuantity).sum();
        if (available < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_QUANTITY);
        }

        UUID cancellationId = UUID.randomUUID();
        int remaining = quantity;
        BigDecimal refund = BigDecimal.ZERO;
        Map<UUID, Integer> releasedByVariant = new LinkedHashMap<>();
        var now = clock.instant();
        for (OrderItem item : items) {
            if (remaining == 0) {
                break;
            }
            int canceled = Math.min(remaining, item.availableQuantity());
            BigDecimal itemRefund = item.cancel(canceled);
            ShopOrder order = orderRepository.findByIdForUpdate(item.orderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
            if (!order.isCancelable()) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배송이 시작된 주문은 취소할 수 없습니다.");
            }
            boolean orderFullyCanceled = itemRepository
                    .findAllByOrderIdOrderByLineNumberAsc(order.id())
                    .stream()
                    .allMatch(orderItem -> orderItem.availableQuantity() == 0);
            order.applyCancellation(itemRefund, orderFullyCanceled, now);
            refund = refund.add(itemRefund);
            releasedByVariant.merge(item.variantId(), canceled, Integer::sum);
            remaining -= canceled;
        }

        BigDecimal remainingPoints = refund.signum() == 0
                ? pointManager.balance(memberId)
                : pointManager.credit(memberId, refund, cancellationId, commandId);
        var concurrentReplay = cancellationRepository
                .findByMemberIdAndCommandId(memberId, commandId);
        if (concurrentReplay.isPresent()) {
            return replayCancellation(concurrentReplay.get(), fingerprint);
        }
        releasedByVariant.forEach((variantId, releasedQuantity) ->
                stockManager.release(variantId, releasedQuantity, cancellationId));
        OrderCancellation cancellation = cancellationRepository.save(new OrderCancellation(
                cancellationId,
                commandId,
                fingerprint,
                memberId,
                productId,
                quantity,
                refund,
                remainingPoints,
                now
        ));
        return toCancellationView(cancellation);
    }

    private OrderView replayOrder(ShopOrder order, String fingerprint) {
        if (!order.hasFingerprint(fingerprint)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return toCreationView(order);
    }

    private CancellationView replayCancellation(
            OrderCancellation cancellation,
            String fingerprint
    ) {
        if (!cancellation.hasFingerprint(fingerprint)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
        return toCancellationView(cancellation);
    }

    private CancellationView toCancellationView(OrderCancellation cancellation) {
        return new CancellationView(
                cancellation.id(),
                cancellation.productId(),
                cancellation.quantity(),
                cancellation.refundAmount(),
                cancellation.balanceAfter()
        );
    }

    private OrderView toCreationView(ShopOrder order) {
        return attachAddress(order.toCreationView(
                itemRepository.findAllByOrderIdOrderByLineNumberAsc(order.id()).stream()
                        .map(OrderItem::toCreationView)
                        .toList()
        ));
    }

    private OrderView attachAddress(OrderView order) {
        return attachAddress(order, shippingAddresses(List.of(order.getId())));
    }

    private OrderView attachAddress(OrderView order, Map<UUID, ShippingAddressView> addresses){
        ShippingAddressView address = addresses.get(order.getId());
        return address == null ? order : order.withShippingAddress(address);
    }

    private OrderView withAddress(OrderView order,Map<UUID,ShippingAddressView> addresses){
        return attachAddress(order, addresses);
    }

    private Map<UUID,ShippingAddressView> shippingAddresses(List<UUID> orderIds){
        if(orderIds.isEmpty()) return Map.of();
        return shippingAddressRepository.findAllByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(OrderShippingAddress::orderId, OrderShippingAddress::toView));
    }

    private void validateShippingAddress(ShippingAddressCommand address) {
        if (address == null) return;
        if (isBlank(address.getRecipientName()) || isBlank(address.getPhoneNumber())
                || isBlank(address.getPostalCode()) || isBlank(address.getAddressLine1())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배송지 필수 항목을 입력해야 합니다.");
        }
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0 || quantity > MAX_ORDER_QUANTITY) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "수량은 1 이상 1,000,000 이하여야 합니다."
            );
        }
    }

    private List<OrderLineCommand> validateAndSortItems(List<OrderLineCommand> items) {
        if (items == null || items.isEmpty() || items.size() > MAX_ORDER_ITEMS) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "주문 상품은 1종 이상 50종 이하여야 합니다.");
        }
        for (OrderLineCommand item : items) {
            if (item == null || item.getProductId() == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER);
            }
            requirePositiveQuantity(item.getQuantity());
        }
        long distinct = items.stream().map(OrderLineCommand::getVariantId).distinct().count();
        if (distinct != items.size()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "같은 상품은 주문에 한 번만 포함할 수 있습니다.");
        }
        return items.stream()
                .sorted(Comparator.comparing(line -> line.getVariantId().toString()))
                .toList();
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    private String normalizeText(String value) { return value == null ? null : value.trim(); }

    private BigDecimal normalizePointAmount(BigDecimal requested, BigDecimal total) {
        BigDecimal value = requested == null ? total : requested;
        try {
            value = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "포인트 사용액은 소수점 둘째 자리까지 입력해야 합니다.");
        }
        if (value.signum() < 0 || value.compareTo(total) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "포인트 사용액은 0 이상 주문 결제액 이하여야 합니다.");
        }
        return value;
    }

    private ShopOrder lockedMemberOrder(UUID memberId, UUID orderId) {
        ShopOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (!order.memberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다.");
        }
        return order;
    }

    private String orderNumber(UUID id, java.time.Instant now) {
        return "SKALA-" + ORDER_DATE.format(now) + "-" +
                id.toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private static final class ProductAccumulator {

        private final UUID productId;
        private final String productName;
        private final BigDecimal latestUnitPrice;
        private int quantity;

        private ProductAccumulator(UUID productId, String productName, BigDecimal latestUnitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.latestUnitPrice = latestUnitPrice;
        }

        private void add(int amount) {
            quantity += amount;
        }

        private PurchasedProductView toView() {
            return new PurchasedProductView(productId, productName, latestUnitPrice, quantity);
        }
    }
}
