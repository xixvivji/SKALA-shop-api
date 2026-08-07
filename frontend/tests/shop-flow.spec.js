import { expect, test } from "@playwright/test";

const productId = "11111111-1111-4111-8111-111111111111";
const memberId = "22222222-2222-4222-8222-222222222222";
const addressId = "33333333-3333-4333-8333-333333333333";
const orderId = "44444444-4444-4444-8444-444444444444";

function json(route, body, status = 200) {
  return route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function mockShopApi(page) {
  const state = {
    role: null,
    customerId: "e2e-user",
    name: "테스트 고객",
    balance: 1_000_000,
    cart: [],
    addresses: [],
    orders: [],
    fulfillmentStatus: "PAID",
  };

  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname;
    const method = request.method();
    const body = request.postDataJSON?.() || {};

    if (path === "/api/auth/csrf") {
      return json(route, { headerName: "X-XSRF-TOKEN", token: "e2e-csrf" });
    }
    if (path === "/api/categories") {
      return json(route, [{ id: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", name: "디지털", description: "", status: "ACTIVE" }]);
    }
    if (path === "/api/products" || path === "/api/products/list") {
      const content = [{ id: productId, name: "E2E 키보드", price: 25000, status: "ACTIVE", categoryId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa" }];
      return json(route, { content, page: 0, size: 12, totalElements: 1, totalPages: 1 });
    }
    if (path === "/api/products/stocks") {
      return json(route, [{ productId, availableQuantity: 20, maxOrderQuantity: 20, orderable: true, stockStatus: "IN_STOCK" }]);
    }
    if (path === "/api/customers" && method === "POST") {
      state.customerId = body.customerId;
      state.name = body.customerName;
      return json(route, { memberId, customerId: state.customerId, name: state.name, role: "CUSTOMER", customerPoint: state.balance }, 201);
    }
    if (path === "/api/customers/login" && method === "POST") {
      state.role = body.customerId === "admin" ? "ADMIN" : "CUSTOMER";
      state.customerId = body.customerId;
      return json(route, { memberId, customerId: state.customerId, role: state.role });
    }
    if (path === "/api/customers/me" && method === "GET") {
      if (state.role !== "CUSTOMER") return json(route, { code: "NOT_AUTHENTICATED", message: "로그인이 필요합니다." }, 401);
      return json(route, {
        memberId,
        customerId: state.customerId,
        name: state.name,
        role: "CUSTOMER",
        customerPoint: state.balance,
        products: state.orders.length ? [{ productId, productName: "E2E 키보드", latestUnitPrice: 25000, quantity: 1 }] : [],
      });
    }
    if (path === "/api/customers/list") {
      return json(route, { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    }
    if (path === "/api/cart" && method === "GET") return json(route, cartView(state));
    if (path === "/api/cart/items" && method === "POST") {
      state.cart = [{ productId, productName: "E2E 키보드", unitPrice: 25000, quantity: body.quantity, availableQuantity: 20, orderable: true, lineAmount: 25000 * body.quantity }];
      return json(route, cartView(state));
    }
    if (path === "/api/cart" && method === "DELETE") {
      state.cart = [];
      return json(route, cartView(state));
    }
    if (path === "/api/customers/me/addresses" && method === "GET") return json(route, state.addresses);
    if (path === "/api/customers/me/addresses" && method === "POST") {
      const address = { id: addressId, ...body };
      state.addresses = [address];
      return json(route, address, 201);
    }
    if (path === "/api/orders" && method === "POST") {
      state.balance -= 25000;
      state.orders = [orderView(state, body.shippingAddress)];
      return json(route, state.orders[0], 201);
    }
    if (path === "/api/orders/me") {
      return json(route, { content: state.orders, page: 0, size: 5, totalElements: state.orders.length, totalPages: state.orders.length ? 1 : 0 });
    }
    if (path === "/api/wallet/me") return json(route, { memberId, balance: state.balance });
    if (path === "/api/wallet/me/transactions") {
      const content = [{ id: "55555555-5555-4555-8555-555555555555", type: "SIGN_UP", amount: 1000000, balanceAfter: 1000000, createdAt: new Date().toISOString() }];
      return json(route, { content, page: 0, size: 10, totalElements: 1, totalPages: 1 });
    }
    if (path === "/api/admin/orders") {
      const order = orderView(state, { recipientName: "테스트 고객", phoneNumber: "010-1234-5678", postalCode: "06236", addressLine1: "서울 강남구", addressLine2: "101호" });
      return json(route, { content: [order], page: 0, size: 10, totalElements: 1, totalPages: 1 });
    }
    if (path.endsWith("/fulfillment") && method === "PUT") {
      state.fulfillmentStatus = body.status;
      return json(route, orderView(state));
    }
    if (path.endsWith("/history")) {
      return json(route, [{ id: "66666666-6666-4666-8666-666666666666", fromStatus: "PAID", toStatus: state.fulfillmentStatus, changedBy: memberId, changedAt: new Date().toISOString() }]);
    }
    return json(route, { code: "E2E_UNHANDLED", message: `${method} ${path}` }, 404);
  });
}

function cartView(state) {
  return {
    items: state.cart,
    itemCount: state.cart.length,
    totalQuantity: state.cart.reduce((sum, item) => sum + item.quantity, 0),
    totalAmount: state.cart.reduce((sum, item) => sum + item.lineAmount, 0),
  };
}

function orderView(state, shippingAddress) {
  return {
    id: orderId,
    orderNumber: "ORD-E2E-001",
    status: "PAID",
    fulfillmentStatus: state.fulfillmentStatus,
    totalAmount: 25000,
    canceledAmount: 0,
    remainingPoints: state.balance,
    orderedAt: new Date().toISOString(),
    shippingAddress,
    items: [{ id: "77777777-7777-4777-8777-777777777777", productId, productName: "E2E 키보드", unitPrice: 25000, orderedQuantity: 1, canceledQuantity: 0 }],
  };
}

test.beforeEach(async ({ page }) => {
  await mockShopApi(page);
});

test("회원가입부터 장바구니 배송지 주문과 포인트 조회까지 동작한다", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByText("E2E 키보드")).toBeVisible();

  await page.getByRole("button", { name: "로그인" }).click();
  await page.getByRole("tab", { name: "회원가입" }).click();
  await page.locator("#signup-form [name=customerId]").fill("e2e-user");
  await page.locator("#signup-form [name=customerName]").fill("테스트 고객");
  await page.locator("#signup-form [name=customerPassword]").fill("e2e-pass");
  await page.locator("#signup-form [name=customerPasswordConfirm]").fill("e2e-pass");
  await page.getByRole("button", { name: "가입하고 시작하기" }).click();
  await expect(page.getByRole("button", { name: "장바구니 열기" })).toBeVisible();

  await page.getByRole("button", { name: "담기" }).click();
  await page.getByRole("button", { name: "장바구니 열기" }).click();
  await expect(page.locator("#cart-list")).toContainText("E2E 키보드");
  await page.getByRole("button", { name: "주문하기" }).click();

  await expect(page.locator("#address-dialog")).toBeVisible();
  await page.locator("#address-form [name=addressName]").fill("집");
  await page.locator("#address-form [name=recipientName]").fill("테스트 고객");
  await page.locator("#address-form [name=phoneNumber]").fill("010-1234-5678");
  await page.locator("#address-form [name=postalCode]").fill("06236");
  await page.locator("#address-form [name=addressLine1]").fill("서울 강남구");
  await page.locator("#address-form [name=addressLine2]").fill("101호");
  await page.getByRole("button", { name: "배송지 저장", exact: true }).click();
  await expect(page.locator("#address-list")).toContainText("서울 강남구");

  await page.getByRole("button", { name: "Shop" }).first().click();
  await page.getByRole("button", { name: "장바구니 열기" }).click();
  await page.getByRole("button", { name: "주문하기" }).click();
  await page.getByRole("button", { name: "전체 주문하기" }).click();
  await expect(page.locator("#order-list")).toContainText("ORD-E2E-001");
  await expect(page.locator("#order-list")).toContainText("결제 완료");

  await page.getByRole("button", { name: "My" }).first().click();
  await expect(page.locator("#transaction-list")).toContainText("가입 포인트");
});

test("관리자가 주문 배송 상태와 변경 이력을 관리한다", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "로그인" }).click();
  await page.locator("#login-form [name=customerId]").fill("admin");
  await page.locator("#login-form [name=customerPassword]").fill("admin-password");
  await page.locator("#login-form button[type=submit]").click();

  await expect(page.locator("#admin-order-list")).toContainText("ORD-E2E-001");
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "상품 준비 중 처리" }).click();
  await expect(page.locator("#admin-order-list")).toContainText("상품 준비 중");
  await page.getByRole("button", { name: "이력" }).click();
  await expect(page.locator(".order-history")).toContainText("결제 완료 → 상품 준비 중");
});
