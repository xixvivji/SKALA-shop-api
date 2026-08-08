import { randomUUID } from "node:crypto";
import { ApiClient, confirmedOrigin, requiredEnvironment } from "./http-client.mjs";

const argumentsMap = new Map(
  process.argv.slice(2).map((argument) => {
    const separator = argument.indexOf("=");
    return separator < 0 ? [argument, ""] : [argument.slice(0, separator), argument.slice(separator + 1)];
  }),
);

const apiUrl = requiredEnvironment("SKALA_API_BASE_URL");
const origin = confirmedOrigin(apiUrl, argumentsMap.get("--confirm-origin") || "");
const adminId = requiredEnvironment("SKALA_ADMIN_ID");
const adminPassword = requiredEnvironment("SKALA_ADMIN_PASSWORD");
const customer = new ApiClient(origin);
const admin = new ApiClient(origin);
const runId = `${Date.now().toString(36)}${randomUUID().slice(0, 6)}`;
const customerId = `flow_${runId}`.slice(0, 50);
const customerPassword = `Flow-${runId}-pw`;
let customerAuthenticated = false;

function commandHeaders() {
  return { "X-Idempotency-Key": randomUUID() };
}

function check(condition, message) {
  if (!condition) throw new Error(message);
}

async function findOrderableVariant() {
  const page = await customer.request("/api/products?page=0&size=100");
  for (const product of page.content || []) {
    const variants = await customer.request(`/api/products/${product.id}/variants`);
    for (const variant of variants) {
      const stock = await customer.request(`/api/products/${variant.id}/stock`).catch(() => null);
      if (stock?.orderable && Number(stock.availableQuantity) > 0) {
        return { product, variant, stock };
      }
    }
  }
  throw new Error("주문 가능한 운영 상품 옵션이 없습니다. 카탈로그와 재고를 먼저 적재하세요.");
}

await customer.issueCsrf();
await admin.issueCsrf();

try {
  await admin.request("/api/customers/login", {
    method: "POST",
    body: { customerId: adminId, customerPassword: adminPassword },
  });
  console.log("ok 관리자 로그인");

  await customer.request("/api/customers", {
    method: "POST",
    body: { customerId, customerName: "운영 흐름 점검 고객", customerPassword },
  });
  await customer.request("/api/customers/login", {
    method: "POST",
    body: { customerId, customerPassword },
  });
  customerAuthenticated = true;
  console.log("ok 회원가입 및 로그인");

  const { product, variant } = await findOrderableVariant();
  const cart = await customer.request("/api/cart/items", {
    method: "POST",
    body: { productId: product.id, variantId: variant.id, quantity: 1 },
  });
  check(cart.items?.some((item) => item.variantId === variant.id), "장바구니에 선택한 옵션이 없습니다.");
  console.log(`ok 장바구니 담기: ${product.name} / ${variant.optionValue}`);

  const shippingAddress = {
    recipientName: "운영 흐름 점검 고객",
    phoneNumber: "010-0000-0000",
    postalCode: "06236",
    addressLine1: "서울특별시 강남구 테헤란로",
    addressLine2: "SKALA 운영 점검",
  };
  const order = await customer.request("/api/orders", {
    method: "POST",
    headers: commandHeaders(),
    body: {
      items: [{ productId: product.id, variantId: variant.id, quantity: 1 }],
      shippingAddress,
      pointAmount: "0.00",
    },
  });
  check(order.status === "PAYMENT_PENDING", `결제 대기 주문이 아닙니다: ${order.status}`);
  check(Number(order.paymentAmount) > 0, "카드 결제 금액이 생성되지 않았습니다.");
  console.log(`ok 주문 생성: ${order.orderNumber}`);

  const payment = await customer.request("/api/payments", {
    method: "POST",
    headers: commandHeaders(),
    body: { orderId: order.id, method: "CARD" },
  });
  const approved = await customer.request(`/api/payments/${payment.id}/approve`, {
    method: "POST",
    headers: commandHeaders(),
    body: { testCardNumber: "4242-4242-4242-4242" },
  });
  check(approved.status === "PAID", `결제가 완료되지 않았습니다: ${approved.status}`);
  check(Boolean(approved.providerTransactionId), "결제 거래 식별자가 없습니다.");
  console.log("ok 카드 결제 승인");

  await admin.request(`/api/admin/orders/${order.id}/fulfillment`, {
    method: "PUT",
    body: { status: "PREPARING" },
  });
  await admin.request(`/api/admin/orders/${order.id}/fulfillment`, {
    method: "PUT",
    body: {
      status: "SHIPPED",
      trackingCarrier: "SKALA 택배",
      trackingNumber: `SK${Date.now()}`,
      trackingUrl: "https://example.com/tracking",
      estimatedDeliveryAt: new Date(Date.now() + 86_400_000).toISOString(),
    },
  });
  const delivered = await admin.request(`/api/admin/orders/${order.id}/fulfillment`, {
    method: "PUT",
    body: { status: "DELIVERED" },
  });
  check(delivered.fulfillmentStatus === "DELIVERED", "배송 완료 상태가 반영되지 않았습니다.");
  console.log("ok 상품 준비 → 배송 중 → 배송 완료");

  const orderedItem = delivered.items?.find((item) => item.variantId === variant.id) || delivered.items?.[0];
  check(Boolean(orderedItem?.id), "반품할 주문 항목을 찾지 못했습니다.");
  const requestedReturn = await customer.request("/api/returns", {
    method: "POST",
    headers: commandHeaders(),
    body: {
      orderId: order.id,
      orderItemId: orderedItem.id,
      quantity: 1,
      reason: "DEFECTIVE",
      evidenceImageUrl: null,
    },
  });
  check(requestedReturn.status === "REQUESTED", "반품이 접수되지 않았습니다.");

  let processedReturn = requestedReturn;
  for (const status of ["COLLECTING", "INSPECTING", "APPROVED", "REFUNDED"]) {
    processedReturn = await admin.request(`/api/admin/returns/${requestedReturn.id}/status`, {
      method: "PUT",
      headers: commandHeaders(),
      body: { status, adminNote: "운영 전체 흐름 자동 점검" },
    });
  }
  check(processedReturn.status === "REFUNDED", "반품 환불이 완료되지 않았습니다.");
  check(Number(processedReturn.paymentRefundAmount) > 0, "카드 환불 금액이 반영되지 않았습니다.");
  console.log("ok 반품 접수 → 회수 → 검수 → 승인 → 환불");

  const mine = await customer.request("/api/returns/me?page=0&size=20");
  check(mine.content?.some((item) => item.id === requestedReturn.id && item.status === "REFUNDED"),
    "고객 반품 내역에서 환불 완료 상태를 확인하지 못했습니다.");
  console.log(`commerce flow passed: ${origin}`);
} finally {
  if (customerAuthenticated) {
    await customer.request("/api/customers/me", { method: "DELETE" }).catch(() => {});
  }
  await admin.request("/api/customers/logout", { method: "POST" }).catch(() => {});
}
