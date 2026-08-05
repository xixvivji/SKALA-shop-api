package com.skala.shopping.order.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.order.CancellationView;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.OrderItemView;
import com.skala.shopping.order.OrderView;
import com.skala.shopping.order.PurchasedProductView;
import com.skala.shopping.order.internal.domain.OrderCancellation;
import com.skala.shopping.order.internal.domain.OrderItem;
import com.skala.shopping.order.internal.domain.ShopOrder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OrderApplicationService implements OrderApi {

    private static final DateTimeFormatter ORDER_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
    private static final int MAX_ORDER_QUANTITY = 1_000_000;

    private final ShopOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderCancellationRepository cancellationRepository;
    private final ProductReader productReader;
    private final PointManager pointManager;
    private final StockManager stockManager;
    private final Clock clock = Clock.systemUTC();

    OrderApplicationService(
            ShopOrderRepository orderRepository,
            OrderItemRepository itemRepository,
            OrderCancellationRepository cancellationRepository,
            ProductReader productReader,
            PointManager pointManager,
            StockManager stockManager
    ) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.cancellationRepository = cancellationRepository;
        this.productReader = productReader;
        this.pointManager = pointManager;
        this.stockManager = stockManager;
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, UUID productId, int quantity, UUID commandId) {
        requirePositiveQuantity(quantity);
        String fingerprint = OrderCommandFingerprint.order(memberId, productId, quantity);
        return orderRepository.findByMemberIdAndRequestId(memberId, commandId)
                .map(order -> replayOrder(order, fingerprint))
                .orElseGet(() -> createOrder(
                        memberId,
                        productId,
                        quantity,
                        commandId,
                        fingerprint
                ));
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
    @Transactional(readOnly = true)
    public List<OrderView> getOrders(UUID memberId) {
        List<ShopOrder> orders = orderRepository.findAllByMemberIdOrderByOrderedAtDesc(memberId);
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<OrderItemView>> itemsByOrder = itemRepository
                .findAllByOrderIdIn(orders.stream().map(ShopOrder::id).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        OrderItem::orderId,
                        Collectors.mapping(OrderItem::toView, Collectors.toList())
                ));
        return orders.stream()
                .map(order -> order.toView(itemsByOrder.getOrDefault(order.id(), List.of())))
                .toList();
    }

    @Override
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
                .sorted(Comparator.comparing(PurchasedProductView::getProductName))
                .toList();
    }

    private OrderView createOrder(
            UUID memberId,
            UUID productId,
            int quantity,
            UUID commandId,
            String fingerprint
    ) {
        var product = productReader.getSaleableProduct(productId);
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        UUID orderId = UUID.randomUUID();
        BigDecimal remainingPoints = pointManager.debit(
                memberId,
                totalAmount,
                orderId,
                commandId
        );
        var replay = orderRepository.findByMemberIdAndRequestId(memberId, commandId);
        if (replay.isPresent()) {
            return replayOrder(replay.get(), fingerprint);
        }
        stockManager.reserve(productId, quantity, orderId);
        var now = clock.instant();
        ShopOrder order = orderRepository.save(new ShopOrder(
                orderId,
                commandId,
                fingerprint,
                orderNumber(orderId, now),
                memberId,
                totalAmount,
                remainingPoints,
                now
        ));
        OrderItem item = itemRepository.save(new OrderItem(
                orderId,
                product.getId(),
                product.getName(),
                product.getPrice(),
                quantity
        ));
        return order.toView(List.of(item.toView()));
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
        var now = clock.instant();
        for (OrderItem item : items) {
            if (remaining == 0) {
                break;
            }
            int canceled = Math.min(remaining, item.availableQuantity());
            BigDecimal itemRefund = item.cancel(canceled);
            ShopOrder order = orderRepository.findByIdForUpdate(item.orderId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
            order.applyCancellation(itemRefund, item.availableQuantity() == 0, now);
            refund = refund.add(itemRefund);
            remaining -= canceled;
        }

        BigDecimal remainingPoints = pointManager.credit(
                memberId,
                refund,
                cancellationId,
                commandId
        );
        var concurrentReplay = cancellationRepository
                .findByMemberIdAndCommandId(memberId, commandId);
        if (concurrentReplay.isPresent()) {
            return replayCancellation(concurrentReplay.get(), fingerprint);
        }
        stockManager.release(productId, quantity, cancellationId);
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
        return toView(order);
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

    private OrderView toView(ShopOrder order) {
        return order.toView(
                itemRepository.findAllByOrderId(order.id()).stream()
                        .map(OrderItem::toView)
                        .toList()
        );
    }

    private String orderNumber(UUID id, java.time.Instant now) {
        return "SKALA-" + ORDER_DATE.format(now) + "-" +
                id.toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0 || quantity > MAX_ORDER_QUANTITY) {
            throw new BusinessException(
                    ErrorCode.INVALID_PARAMETER,
                    "수량은 1 이상 1,000,000 이하여야 합니다."
            );
        }
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
