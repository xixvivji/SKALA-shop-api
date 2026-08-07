package com.skala.shopping.order.internal;

import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.PageResponse;
import com.skala.shopping.order.CancellationView;
import com.skala.shopping.order.OrderApi;
import com.skala.shopping.order.OrderItemView;
import com.skala.shopping.order.OrderView;
import com.skala.shopping.order.OrderLineCommand;
import com.skala.shopping.order.ShippingAddressCommand;
import com.skala.shopping.order.ShippingAddressView;
import com.skala.shopping.order.OrderStatusHistoryView;
import com.skala.shopping.order.PurchasedProductView;
import com.skala.shopping.order.internal.domain.OrderCancellation;
import com.skala.shopping.order.internal.domain.OrderItem;
import com.skala.shopping.order.internal.domain.ShopOrder;
import com.skala.shopping.order.internal.domain.OrderShippingAddress;
import com.skala.shopping.order.internal.domain.FulfillmentStatus;
import com.skala.shopping.order.internal.domain.OrderStatusHistory;
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
    private final Clock clock = Clock.systemUTC();

    OrderApplicationService(
            ShopOrderRepository orderRepository,
            OrderItemRepository itemRepository,
            OrderCancellationRepository cancellationRepository,
            OrderShippingAddressRepository shippingAddressRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            ProductReader productReader,
            PointManager pointManager,
            StockManager stockManager
    ) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.cancellationRepository = cancellationRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.productReader = productReader;
        this.pointManager = pointManager;
        this.stockManager = stockManager;
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, UUID productId, int quantity, UUID commandId) {
        return placeOrder(memberId, List.of(new OrderLineCommand(productId, quantity)), null, commandId);
    }

    @Override
    @Transactional
    public OrderView placeOrder(UUID memberId, List<OrderLineCommand> items,
                                ShippingAddressCommand shippingAddress, UUID commandId) {
        List<OrderLineCommand> normalizedItems = validateAndSortItems(items);
        validateShippingAddress(shippingAddress);
        String fingerprint = OrderCommandFingerprint.order(memberId, normalizedItems, shippingAddress);
        return orderRepository.findByMemberIdAndRequestId(memberId, commandId)
                .map(order -> replayOrder(order, fingerprint))
                .orElseGet(() -> createOrder(
                        memberId,
                        normalizedItems,
                        shippingAddress,
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
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public OrderView getOrder(UUID memberId, UUID orderId) {
        ShopOrder order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다."));
        return attachAddress(order.toView(
                itemRepository.findAllByOrderIdOrderByLineNumberAsc(order.id())
                        .stream().map(OrderItem::toView).toList()
        ));
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
                .sorted(Comparator.comparing(PurchasedProductView::getProductName)
                        .thenComparing(PurchasedProductView::getProductId))
                .toList();
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
        FulfillmentStatus next;
        try { next = FulfillmentStatus.valueOf(requestedStatus); }
        catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배송 상태가 올바르지 않습니다.");
        }
        ShopOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "주문을 찾을 수 없습니다."));
        FulfillmentStatus previous = order.fulfillmentStatus();
        try { order.transitionFulfillment(next, clock.instant()); }
        catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER,
                    "배송 상태는 PAID → PREPARING → SHIPPED → DELIVERED 순서로만 변경할 수 있습니다.");
        }
        statusHistoryRepository.save(new OrderStatusHistory(
                order.id(), previous, next, adminId, clock.instant()));
        return attachAddress(order.toView(itemRepository.findAllByOrderIdOrderByLineNumberAsc(order.id())
                .stream().map(OrderItem::toView).toList()));
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
            String fingerprint
    ) {
        List<OrderProduct> products = lines.stream()
                .map(line -> productReader.getSaleableProduct(line.getProductId()))
                .toList();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (int index = 0; index < lines.size(); index++) {
            totalAmount = totalAmount.add(products.get(index).getPrice()
                    .multiply(BigDecimal.valueOf(lines.get(index).getQuantity())));
        }
        if (totalAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "주문 총액 한도를 초과했습니다.");
        }
        UUID orderId = UUID.randomUUID();
        // 포인트 모듈도 같은 commandId로 멱등 처리하므로 동시 재시도에서 중복 차감되지 않습니다.
        BigDecimal remainingPoints = pointManager.debit(
                memberId,
                totalAmount,
                orderId,
                commandId
        );
        var replay = orderRepository.findByMemberIdAndRequestId(memberId, commandId);
        if (replay.isPresent()) {
            // 포인트 계정 잠금 대기 중 다른 요청이 주문을 완료했다면 최초 응답 스냅샷을 재생합니다.
            return replayOrder(replay.get(), fingerprint);
        }
        for (OrderLineCommand line : lines) {
            stockManager.reserve(line.getProductId(), line.getQuantity(), orderId);
        }
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
        statusHistoryRepository.save(new OrderStatusHistory(
                orderId, null, FulfillmentStatus.PAID, null, now));
        List<OrderItem> savedItems = new java.util.ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            OrderProduct product = products.get(index);
            savedItems.add(itemRepository.save(new OrderItem(
                    orderId, product.getId(), product.getName(), product.getPrice(),
                    lines.get(index).getQuantity(), index)));
        }
        OrderView view = order.toView(savedItems.stream().map(OrderItem::toView).toList());
        if (shippingAddress != null) {
            OrderShippingAddress saved = shippingAddressRepository.save(new OrderShippingAddress(orderId, shippingAddress));
            view = view.withShippingAddress(saved.toView());
        }
        return view;
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
            // 여러 주문에 걸친 부분 취소가 서로 덮어쓰지 않도록 주문 행을 순서대로 잠급니다.
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

    private OrderView attachAddress(OrderView order){
        return shippingAddressRepository.findById(order.getId())
                .map(address -> order.withShippingAddress(address.toView())).orElse(order);
    }
    private Map<UUID,ShippingAddressView> shippingAddresses(List<UUID> orderIds){
        if(orderIds.isEmpty()) return Map.of();
        return shippingAddressRepository.findAllByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(OrderShippingAddress::orderId,OrderShippingAddress::toView));
    }
    private OrderView withAddress(OrderView order,Map<UUID,ShippingAddressView> addresses){
        ShippingAddressView address=addresses.get(order.getId());return address==null?order:order.withShippingAddress(address);
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
        long distinct = items.stream().map(OrderLineCommand::getProductId).distinct().count();
        if (distinct != items.size()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "같은 상품은 주문에 한 번만 포함할 수 있습니다.");
        }
        // 모든 주문이 같은 상품 순서로 재고를 잠그게 해 다중 상품 주문 간 교착 가능성을 낮춥니다.
        return items.stream()
                .sorted(Comparator.comparing(line -> line.getProductId().toString()))
                .toList();
    }

    private void validateShippingAddress(ShippingAddressCommand address) {
        if (address == null) return;
        if (isBlank(address.getRecipientName()) || isBlank(address.getPhoneNumber())
                || isBlank(address.getPostalCode()) || isBlank(address.getAddressLine1())) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "배송지 필수 항목을 입력해야 합니다.");
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

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
