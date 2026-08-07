import { expect, test } from "@playwright/test";

const liveEnabled = process.env.LIVE_E2E_ENABLED === "true";
const runId = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 7)}`;
const customerId = `smoke_${runId}`.slice(0, 50);
const customerPassword = `Smoke-${runId}-pw`;

test.describe("@live 실제 배포 환경", () => {
  test.skip(!liveEnabled, "LIVE_E2E_ENABLED=true일 때만 실제 운영 데이터를 변경합니다.");
  test.describe.configure({ mode: "serial" });

  test.afterEach(async ({ page }) => {
    const csrfResponse = await page.request.get("/api/auth/csrf").catch(() => null);
    if (!csrfResponse?.ok()) return;
    const csrf = await csrfResponse.json();
    await page.request.delete("/api/customers/me", {
      headers: { [csrf.headerName || "X-XSRF-TOKEN"]: csrf.token },
    }).catch(() => {});
  });

  test("@live 가입부터 주문·취소·탈퇴까지 실제 API로 동작한다", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator("#product-grid .product-card").first()).toBeVisible();

    await page.getByRole("button", { name: "로그인" }).click();
    await page.getByRole("tab", { name: "회원가입" }).click();
    await page.locator("#signup-form [name=customerId]").fill(customerId);
    await page.locator("#signup-form [name=customerName]").fill("운영 점검 고객");
    await page.locator("#signup-form [name=customerPassword]").fill(customerPassword);
    await page.locator("#signup-form [name=customerPasswordConfirm]").fill(customerPassword);
    await page.getByRole("button", { name: "가입하고 시작하기" }).click();
    await expect(page.getByRole("button", { name: "장바구니 열기" })).toBeVisible();

    const orderableProduct = page.locator("[data-product-cart]:not([disabled])").first();
    await expect(orderableProduct).toBeVisible();
    await orderableProduct.click();
    await page.getByRole("button", { name: "장바구니 열기" }).click();
    await page.getByRole("button", { name: "주문하기" }).click();

    await expect(page.locator("#address-dialog")).toBeVisible();
    await page.locator("#address-form [name=addressName]").fill("운영 점검지");
    await page.locator("#address-form [name=recipientName]").fill("운영 점검 고객");
    await page.locator("#address-form [name=phoneNumber]").fill("010-0000-0000");
    await page.locator("#address-form [name=postalCode]").fill("06236");
    await page.locator("#address-form [name=addressLine1]").fill("서울특별시 강남구 테헤란로");
    await page.locator("#address-form [name=addressLine2]").fill("운영 점검용");
    await page.getByRole("button", { name: "배송지 저장", exact: true }).click();

    await page.getByRole("button", { name: "Shop" }).first().click();
    await page.getByRole("button", { name: "장바구니 열기" }).click();
    await page.getByRole("button", { name: "주문하기" }).click();
    await page.getByRole("button", { name: "전체 주문하기" }).click();
    await expect(page.locator("#order-list")).toContainText("결제 완료");

    const cancelButton = page.locator("[data-product-cancel]").first();
    await expect(cancelButton).toBeVisible();
    await cancelButton.click();
    await page.locator("#cancel-form button[type=submit]").click();
    await expect(page.locator("#order-list")).toContainText("취소 완료");

    await page.getByRole("button", { name: "My" }).first().click();
    await expect(page.locator("#transaction-list")).toContainText("취소 환급");
    page.once("dialog", (dialog) => dialog.accept());
    await page.locator("#deactivate-button").click();
    await expect(page.getByRole("button", { name: "로그인" })).toBeVisible();
  });

  test("@live 관리자 세션과 운영 목록을 실제 API로 확인한다", async ({ page }) => {
    test.skip(
      !process.env.SKALA_ADMIN_ID || !process.env.SKALA_ADMIN_PASSWORD,
      "관리자 읽기 점검에는 SKALA_ADMIN_ID와 SKALA_ADMIN_PASSWORD가 필요합니다.",
    );
    await page.goto("/");
    await page.getByRole("button", { name: "로그인" }).click();
    await page.locator("#login-form [name=customerId]").fill(process.env.SKALA_ADMIN_ID);
    await page.locator("#login-form [name=customerPassword]").fill(process.env.SKALA_ADMIN_PASSWORD);
    await page.locator("#login-form button[type=submit]").click();
    await expect(page.getByRole("button", { name: "Admin" }).first()).toBeVisible();
    await expect(page.locator("#member-table-body")).not.toContainText("불러오지 못");
    await expect(page.locator("#admin-order-list")).not.toContainText("불러오지 못");
    await page.locator("#admin-logout-button").click();
    await expect(page.getByRole("button", { name: "로그인" })).toBeVisible();
  });
});
