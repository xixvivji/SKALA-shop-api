import {
  createCommandId,
  issueCsrfToken,
  shopApi,
} from "./api.js";

const REMEMBERED_CUSTOMER_ID_KEY = "skala-remembered-customer-id";
const PRODUCT_PAGE_SIZE = 12;
const ORDER_PAGE_SIZE = 5;
const MEMBER_PAGE_SIZE = 20;
const TRANSACTION_PAGE_SIZE = 10;
const ADMIN_ORDER_PAGE_SIZE = 10;

const state = {
  view: "shop",
  session: null,
  customer: null,
  products: [],
  productTotal: 0,
  productPage: -1,
  productTotalPages: 0,
  productsLoading: false,
  productsError: null,
  stocksError: null,
  categories: [],
  productFilters: { query: "", categoryId: "", minPrice: "", maxPrice: "" },
  cart: { items: [], itemCount: 0, totalQuantity: 0, totalAmount: 0 },
  cartLoading: false,
  cartError: null,
  addresses: [],
  addressesLoading: false,
  addressesError: null,
  wishlist: [],
  stockAlerts: [],
  reviews: [],
  myReview: null,
  orders: [],
  orderTotal: 0,
  orderPage: -1,
  orderTotalPages: 0,
  ordersLoading: false,
  ordersError: null,
  returns: [],
  wallet: null,
  transactions: [],
  transactionTotal: 0,
  transactionPage: -1,
  transactionTotalPages: 0,
  transactionsLoading: false,
  transactionsError: null,
  members: [],
  memberTotal: 0,
  memberPage: -1,
  memberTotalPages: 0,
  membersLoading: false,
  membersError: null,
  adminOrders: [],
  adminOrderTotal: 0,
  adminOrderPage: -1,
  adminOrderTotalPages: 0,
  adminOrdersLoading: false,
  adminOrdersError: null,
  adminReturns: [],
};

const $ = (selector, scope = document) => scope.querySelector(selector);
const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

const elements = {
  authDialog: $("#auth-dialog"),
  authFeedback: $("#auth-feedback"),
  authButton: $("#auth-button"),
  profileButton: $("#profile-button"),
  profileName: $("#profile-name"),
  profileRole: $("#profile-role"),
  profileAvatar: $("#profile-avatar"),
  adminNav: $("#admin-nav"),
  mobileAdminNav: $("#mobile-admin-nav"),
  addProductButton: $("#add-product-button"),
  adminAddProductButton: $("#admin-add-product-button"),
  productGrid: $("#product-grid"),
  productCount: $("#product-count"),
  productSearch: $("#product-search"),
  productLoadMore: $("#product-load-more"),
  categoryFilter: $("#category-filter"),
  catalogFilterForm: $("#catalog-filter-form"),
  cartButton: $("#cart-button"),
  cartCount: $("#cart-count"),
  cartDialog: $("#cart-dialog"),
  cartList: $("#cart-list"),
  cartTotalQuantity: $("#cart-total-quantity"),
  cartTotalAmount: $("#cart-total-amount"),
  checkoutDialog: $("#checkout-dialog"),
  checkoutForm: $("#checkout-form"),
  addressDialog: $("#address-dialog"),
  addressList: $("#address-list"),
  wishlistList: $("#wishlist-list"),
  stockAlertList: $("#stock-alert-list"),
  reviewDialog: $("#review-dialog"),
  reviewList: $("#review-list"),
  reviewForm: $("#review-form"),
  orderDialog: $("#order-dialog"),
  cancelDialog: $("#cancel-dialog"),
  returnDialog: $("#return-dialog"),
  returnList: $("#return-list"),
  adminReturnList: $("#admin-return-list"),
  productDialog: $("#product-dialog"),
  stockDialog: $("#stock-dialog"),
  ordersLoginGate: $("#orders-login-gate"),
  ordersContent: $("#orders-content"),
  orderList: $("#order-list"),
  orderCount: $("#order-count"),
  orderLoadMore: $("#order-load-more"),
  accountLoginGate: $("#account-login-gate"),
  accountContent: $("#account-content"),
  memberRole: $("#member-role"),
  memberName: $("#member-name"),
  memberId: $("#member-id"),
  memberBalance: $("#member-balance"),
  profileNameInput: $("#profile-name-input"),
  purchasedList: $("#purchased-list"),
  transactionList: $("#transaction-list"),
  transactionLoadMore: $("#transaction-load-more"),
  memberTableBody: $("#member-table-body"),
  memberLoadMore: $("#member-load-more"),
  adminProductCount: $("#admin-product-count"),
  adminMemberCount: $("#admin-member-count"),
  adminOrderCount: $("#admin-order-count"),
  adminOrderList: $("#admin-order-list"),
  adminOrderLoadMore: $("#admin-order-load-more"),
  toastRegion: $("#toast-region"),
  loadingOverlay: $("#loading-overlay"),
  loadingMessage: $("#loading-message"),
  connectionBanner: $("#connection-banner"),
  stockFormFeedback: $("#stock-form-feedback"),
};

let loadingDepth = 0;
let authGeneration = 0;
let reauthenticationPrompted = false;
let lastSessionCheckAt = 0;
let productsReloadQueued = false;
let ordersReloadQueued = false;
let membersReloadQueued = false;
let transactionsReloadQueued = false;
let adminOrdersReloadQueued = false;

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function points(value) {
  const numeric = Number(value || 0);
  return `${new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(numeric)} P`;
}

function money(value) {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(
    Number(value || 0),
  );
}

function dateTime(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function initials(value) {
  const text = String(value || "S").trim();
  return [...text][0]?.toUpperCase() || "S";
}

function safeImageUrl(value) {
  if (!value) return "";
  try {
    const url = new URL(value);
    return ["https:", "http:"].includes(url.protocol) ? url.href : "";
  } catch {
    return "";
  }
}

function toneFor(value) {
  const hash = [...String(value || "")].reduce(
    (sum, character) => sum + character.codePointAt(0),
    0,
  );
  return (hash % 5) + 1;
}

function isCustomer() {
  return state.session?.role === "CUSTOMER";
}

function isAdmin() {
  return state.session?.role === "ADMIN";
}

function invalidatePendingSessionRestore() {
  authGeneration += 1;
}

function captureSessionSnapshot() {
  if (!state.session) return null;
  return {
    memberId: state.session.memberId,
    customerId: state.session.customerId,
    role: state.session.role,
  };
}

function isCurrentSessionRequest(generation, snapshot) {
  return Boolean(
    snapshot &&
      generation === authGeneration &&
      state.session?.memberId === snapshot.memberId &&
      state.session?.customerId === snapshot.customerId &&
      state.session?.role === snapshot.role,
  );
}

function stockStatusLabel(stock) {
  if (!stock) return "재고 미설정";
  const labels = {
    PAYMENT_PENDING: "결제 대기",
    PAYMENT_FAILED: "결제 실패",
    IN_STOCK: `재고 ${stock.availableQuantity}개`,
    LOW_STOCK: `품절 임박 · ${stock.availableQuantity}개`,
    OUT_OF_STOCK: "품절",
    INACTIVE: "판매 중지",
  };
  return labels[stock.stockStatus] || "재고 확인 필요";
}

function stockStatusClass(stock) {
  return String(stock?.stockStatus || "UNAVAILABLE").toLowerCase().replaceAll("_", "-");
}

function fieldErrorDetail(error) {
  const first = Object.entries(error?.fieldErrors || {})[0];
  return first ? `${first[0]}: ${first[1]}` : "";
}

function fieldErrorId(form, fieldName) {
  return `${form.id}-${fieldName}-error`;
}

function clearFieldError(input) {
  if (!input?.form || !input.name) return;
  input.removeAttribute("aria-invalid");
  input.removeAttribute("aria-errormessage");
  $(`#${fieldErrorId(input.form, input.name)}`)?.remove();
}

function clearFormFieldErrors(form) {
  $$('[aria-invalid="true"]', form).forEach(clearFieldError);
  $$('.field-error', form).forEach((message) => message.remove());
}

function showFieldError(form, fieldName, message) {
  const input = form.elements[fieldName];
  if (!(input instanceof HTMLElement)) return null;

  clearFieldError(input);
  const error = document.createElement("small");
  error.className = "field-error";
  error.id = fieldErrorId(form, fieldName);
  error.textContent = message;

  const anchor = input.closest(".password-field") || input;
  anchor.insertAdjacentElement("afterend", error);
  input.setAttribute("aria-invalid", "true");
  input.setAttribute("aria-errormessage", error.id);
  return input;
}

function nativeFieldError(input) {
  if (input.validity.valueMissing) return "필수 입력 항목입니다.";
  if (input.validity.tooShort) return `${input.minLength}자 이상 입력해 주세요.`;
  if (input.validity.tooLong) return `${input.maxLength}자 이하로 입력해 주세요.`;
  if (input.validity.patternMismatch) {
    return "영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다.";
  }
  return input.validationMessage || "입력값을 확인해 주세요.";
}

function clearAuthFeedback() {
  elements.authFeedback.replaceChildren();
  elements.authFeedback.classList.add("is-hidden");
  elements.authFeedback.classList.remove("is-success");
}

function showAuthFeedback(title, detail = "", type = "error") {
  const heading = document.createElement("strong");
  heading.textContent = title;
  elements.authFeedback.replaceChildren(heading);

  if (detail) {
    const description = document.createElement("small");
    description.textContent = detail;
    elements.authFeedback.append(description);
  }

  elements.authFeedback.classList.toggle("is-success", type === "success");
  elements.authFeedback.classList.remove("is-hidden");
}

function showAuthError(error, fallback, form, fallbackField) {
  const responseMessage = error?.message || "";
  const genericResponse = /^요청이 실패했습니다\. \(\d+\)$/.test(responseMessage);
  const title = genericResponse ? fallback : responseMessage || fallback;
  let firstInvalidInput = null;

  clearFormFieldErrors(form);
  Object.entries(error?.fieldErrors || {}).forEach(([fieldName, message]) => {
    const input = showFieldError(form, fieldName, message);
    if (!firstInvalidInput && input) firstInvalidInput = input;
  });

  if (!firstInvalidInput && fallbackField) {
    firstInvalidInput = showFieldError(form, fallbackField, title);
  }

  showAuthFeedback(
    title,
    firstInvalidInput ? "표시된 입력 항목을 확인해 주세요." : fieldErrorDetail(error),
    "error",
  );
  firstInvalidInput?.focus();
}

function hasValidBcryptByteLength(password, input) {
  if (new TextEncoder().encode(password).length <= 72) return true;
  clearFormFieldErrors(input.form);
  showFieldError(
    input.form,
    input.name,
    "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.",
  );
  showAuthFeedback(
    "비밀번호가 너무 깁니다",
    "영문·숫자는 최대 72자이며 한글 등은 더 적은 글자만 입력할 수 있습니다.",
  );
  input.focus();
  return false;
}

function showToast(title, detail = "", type = "success") {
  const toast = document.createElement("article");
  toast.className = `toast${type === "error" ? " is-error" : ""}`;
  toast.setAttribute("role", type === "error" ? "alert" : "status");
  toast.innerHTML = `
    <span class="toast-icon" aria-hidden="true">${type === "error" ? "!" : "✓"}</span>
    <span class="toast-copy">
      <strong>${escapeHtml(title)}</strong>
      ${detail ? `<small>${escapeHtml(detail)}</small>` : ""}
    </span>
    <button type="button" aria-label="알림 닫기">×</button>
  `;
  toast.querySelector("button").addEventListener("click", () => toast.remove());
  elements.toastRegion.append(toast);
  window.setTimeout(() => toast.remove(), 5200);
}

function showApiError(error, fallback = "요청을 처리하지 못했습니다.") {
  if (error?.status === 401) {
    invalidatePendingSessionRestore();
    clearSession();
    $$('dialog[open]:not(#auth-dialog)').forEach(closeDialog);
    switchView("shop");
    if (!reauthenticationPrompted) {
      reauthenticationPrompted = true;
      showToast("로그인이 만료되었습니다", "계속 이용하려면 다시 로그인해 주세요.", "error");
      queueMicrotask(() => openAuth("login"));
    }
    return;
  }

  const detail = fieldErrorDetail(error);
  const statusMessage = {
    403: "요청 권한이 없습니다.",
    404: "요청한 정보를 찾을 수 없습니다.",
    409: "현재 상태와 충돌해 처리하지 못했습니다.",
    429: "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.",
  }[error?.status];
  showToast(error?.message || statusMessage || fallback, detail || error?.code || "", "error");
}

async function withLoading(message, task) {
  const activeButton = document.activeElement instanceof HTMLButtonElement
    ? document.activeElement
    : null;
  const buttonWasDisabled = activeButton?.disabled;
  if (activeButton) activeButton.disabled = true;
  loadingDepth += 1;
  document.body.setAttribute("aria-busy", "true");
  elements.loadingMessage.textContent = message;
  elements.loadingOverlay.classList.remove("is-hidden");
  try {
    return await task();
  } finally {
    if (activeButton?.isConnected) activeButton.disabled = Boolean(buttonWasDisabled);
    loadingDepth -= 1;
    if (loadingDepth === 0) {
      document.body.removeAttribute("aria-busy");
      elements.loadingOverlay.classList.add("is-hidden");
    }
  }
}

function rememberSession(session) {
  window.localStorage.setItem(
    "skala-session-hint",
    JSON.stringify({
      customerId: session.customerId,
      role: session.role,
      expiresAt: session.expiresAt,
    }),
  );
}

function sessionHint() {
  try {
    return JSON.parse(window.localStorage.getItem("skala-session-hint") || "null");
  } catch {
    return null;
  }
}

function clearSession() {
  state.session = null;
  state.customer = null;
  state.orders = [];
  state.orderTotal = 0;
  state.orderPage = -1;
  state.orderTotalPages = 0;
  state.ordersError = null;
  state.wallet = null;
  state.transactions = [];
  state.transactionTotal = 0;
  state.transactionPage = -1;
  state.transactionTotalPages = 0;
  state.transactionsError = null;
  state.members = [];
  state.memberTotal = 0;
  state.memberPage = -1;
  state.memberTotalPages = 0;
  state.membersError = null;
  state.adminOrders = [];
  state.adminOrderTotal = 0;
  state.adminOrderPage = -1;
  state.adminOrderTotalPages = 0;
  state.adminOrdersError = null;
  state.cart = { items: [], itemCount: 0, totalQuantity: 0, totalAmount: 0 };
  state.cartError = null;
  state.addresses = [];
  state.addressesError = null;
  state.wishlist = [];
  state.stockAlerts = [];
  state.reviews = [];
  state.myReview = null;
  window.localStorage.removeItem("skala-session-hint");
  renderSession();
  renderOrders();
  renderMembers();
  renderCart();
  renderAddresses();
  renderWishlist();
  renderStockAlerts();
  renderTransactions();
  renderAdminOrders();
}

function setSession(session, customer = null) {
  reauthenticationPrompted = false;
  lastSessionCheckAt = Date.now();
  state.session = session;
  state.customer = customer;
  rememberSession(session);
  renderSession();
}

function renderSession() {
  const authenticated = Boolean(state.session);
  const admin = isAdmin();
  const customer = isCustomer();
  const displayName =
    state.customer?.name || state.session?.name || state.session?.customerId || "고객";

  elements.authButton.classList.toggle("is-hidden", authenticated);
  elements.profileButton.classList.toggle("is-hidden", !authenticated);
  elements.adminNav.classList.toggle("is-hidden", !admin);
  elements.mobileAdminNav.classList.toggle("is-hidden", !admin);
  elements.addProductButton.classList.toggle("is-hidden", !admin);
  elements.cartButton.classList.toggle("is-hidden", !customer);

  $$('[data-view="orders"], [data-view="account"]').forEach((button) => {
    if (button.closest("nav")) {
      button.classList.toggle("is-hidden", admin);
    }
  });

  if (authenticated) {
    elements.profileName.textContent = displayName;
    elements.profileRole.textContent = state.session.role;
    elements.profileAvatar.textContent = initials(displayName);
    elements.profileButton.setAttribute(
      "aria-label",
      admin ? "관리자 콘솔 열기" : "내 정보 열기",
    );
  }

  elements.ordersLoginGate.classList.toggle("is-hidden", customer);
  elements.ordersContent.classList.toggle("is-hidden", !customer);
  elements.accountLoginGate.classList.toggle("is-hidden", customer);
  elements.accountContent.classList.toggle("is-hidden", !customer);

  renderCustomer();
  renderProducts();

  if (admin && ["orders", "account"].includes(state.view)) {
    switchView("admin");
  }
}

function renderCustomer() {
  const customer = state.customer;
  if (!customer || !isCustomer()) {
    elements.memberRole.textContent = "CUSTOMER";
    elements.memberName.textContent = "-";
    elements.memberId.textContent = "-";
    elements.memberBalance.textContent = "0 P";
    elements.profileNameInput.value = "";
    elements.purchasedList.innerHTML = '<div class="empty-inline">구매한 상품이 없습니다.</div>';
    return;
  }

  elements.memberRole.textContent = customer.role;
  elements.memberName.textContent = customer.name || customer.customerId;
  elements.memberId.textContent = customer.customerId;
  elements.memberBalance.textContent = points(state.wallet?.balance ?? customer.customerPoint);
  elements.profileNameInput.value = customer.name || "";

  const products = customer.products || [];
  elements.purchasedList.innerHTML = products.length
    ? products
        .map(
          (product) => `
            <div class="purchased-item">
              <span>${escapeHtml(initials(product.productName))}</span>
              <div>
                <strong>${escapeHtml(product.productName)}</strong>
                <small>${points(product.latestUnitPrice)}</small>
              </div>
              <b>× ${Number(product.quantity)}</b>
            </div>
          `,
        )
        .join("")
    : '<div class="empty-inline">아직 보유한 상품이 없습니다.</div>';
}

function productSkeletons() {
  return Array.from({ length: 4 }, () => '<div class="product-skeleton"></div>').join("");
}

function focusLoadedContent(button) {
  const controlledId = button.getAttribute("aria-controls");
  const controlledContent = controlledId ? document.getElementById(controlledId) : null;
  const target = controlledContent?.lastElementChild || controlledContent;
  if (!(target instanceof HTMLElement)) return;

  const previousTabIndex = target.getAttribute("tabindex");
  target.tabIndex = -1;
  target.focus();
  target.addEventListener(
    "blur",
    () => {
      if (previousTabIndex === null) {
        target.removeAttribute("tabindex");
      } else {
        target.setAttribute("tabindex", previousTabIndex);
      }
    },
    { once: true },
  );
}

function renderLoadMore(button, { loading, page, totalPages, loaded, total, label }) {
  const hasMore = page >= 0 && page + 1 < totalPages;
  const visible = hasMore || (loading && page >= 0);
  if (loading && document.activeElement === button) {
    button.dataset.restoreFocusAfterLoad = "true";
  }
  const shouldRestoreFocus =
    !loading && button.dataset.restoreFocusAfterLoad === "true";
  button.classList.toggle("is-hidden", !visible);
  button.disabled = loading;
  button.textContent = loading
    ? `${label} 불러오는 중…`
    : `${label} 더보기 · ${loaded}/${total}`;
  if (shouldRestoreFocus) {
    delete button.dataset.restoreFocusAfterLoad;
    queueMicrotask(() => {
      if (visible) {
        button.focus();
      } else {
        focusLoadedContent(button);
      }
    });
  }
}

function renderProducts() {
  elements.productGrid.setAttribute("aria-busy", String(state.productsLoading));
  renderLoadMore(elements.productLoadMore, {
    loading: state.productsLoading,
    page: state.productPage,
    totalPages: state.productTotalPages,
    loaded: state.products.length,
    total: state.productTotal,
    label: "상품",
  });

  if (state.productsLoading && !state.products.length) {
    elements.productGrid.innerHTML = productSkeletons();
    return;
  }

  if (state.productsError) {
    elements.productGrid.innerHTML = `
      <div class="error-state">
        <span aria-hidden="true">!</span>
        <h3>상품을 불러오지 못했어요</h3>
        <p>${escapeHtml(state.productsError.message)}</p>
        <button class="button button-outline product-retry" type="button" data-product-retry>다시 불러오기</button>
      </div>
    `;
    elements.productCount.textContent = "0";
    return;
  }

  const query = state.productFilters.query;
  const products = state.products;
  elements.productCount.textContent = String(state.productTotal);
  elements.adminProductCount.textContent = String(state.productTotal);

  if (!products.length) {
    const moreProductsAvailable = state.productPage + 1 < state.productTotalPages;
    const emptyDescription = query
      ? moreProductsAvailable
        ? "현재 불러온 상품에는 없습니다. 상품 더보기로 검색 범위를 넓혀보세요."
        : "다른 검색어로 다시 찾아보세요."
      : "관리자 계정으로 로그인하면 첫 상품을 등록할 수 있습니다.";
    elements.productGrid.innerHTML = `
      <div class="empty-state">
        <span aria-hidden="true">◇</span>
        <h3>${query ? "검색 결과가 없습니다" : "등록된 상품이 없습니다"}</h3>
        <p>${emptyDescription}</p>
      </div>
    `;
    return;
  }

  elements.productGrid.innerHTML = products
    .map((product, index) => {
      const tone = toneFor(product.id);
      const imageUrl = safeImageUrl(product.imageUrl);
      const stock = product.stock;
      const orderable = stock?.orderable === true;
      const stockLabel = state.stocksError ? "재고 확인 오류" : stockStatusLabel(stock);
      const controls = isAdmin()
        ? `
          <div class="admin-card-actions">
            <button class="mini-button" type="button" data-product-stock="${escapeHtml(product.id)}" ${state.stocksError ? "disabled" : ""}>${state.stocksError ? "재고 확인 오류" : stock ? "재고 조정" : "재고 초기화"}</button>
            <button class="mini-button" type="button" data-product-edit="${escapeHtml(product.id)}">수정</button>
            <button class="mini-button danger" type="button" data-product-delete="${escapeHtml(product.id)}">삭제</button>
          </div>
        `
        : `
          <div class="shop-card-actions">
            <button class="mini-button" type="button" data-product-reviews="${escapeHtml(product.id)}">리뷰</button>
            ${isCustomer() ? `<button class="mini-button" type="button" data-product-wishlist="${escapeHtml(product.id)}">♡</button>` : ""}
            ${isCustomer() && stock?.stockStatus === "OUT_OF_STOCK" ? `<button class="mini-button" type="button" data-product-alert="${escapeHtml(product.id)}">재입고</button>` : ""}
            <button class="mini-button" type="button" data-product-cart="${escapeHtml(product.id)}" ${orderable ? "" : "disabled"}>담기</button>
            <button class="buy-button" type="button" data-product-buy="${escapeHtml(product.id)}" aria-label="${escapeHtml(product.name)} ${orderable ? "바로 주문" : stockLabel}" ${orderable ? "" : "disabled"}>${orderable ? "→" : "×"}</button>
          </div>
        `;
      return `
        <article class="product-card">
          <div class="product-visual tone-${tone}${imageUrl ? " has-image" : ""}">
            ${imageUrl ? `<img src="${escapeHtml(imageUrl)}" alt="" loading="lazy" decoding="async" />` : `<span>${escapeHtml(initials(product.name))}</span>`}
            <small class="product-badge stock-${state.stocksError ? "unavailable" : stockStatusClass(stock)}">${escapeHtml(stockLabel)}</small>
          </div>
          <div class="product-body">
            <small>${escapeHtml(state.categories.find((category) => category.id === product.categoryId)?.name || "SKALA SELECT")} · ${String(index + 1).padStart(2, "0")}</small>
            <h3 title="${escapeHtml(product.name)}">${escapeHtml(product.name)}</h3>
            ${product.description ? `<p class="product-description">${escapeHtml(product.description)}</p>` : ""}
            <div class="product-card-foot">
              <div class="product-price">${money(product.price)} <small>P</small></div>
              ${controls}
            </div>
          </div>
        </article>
      `;
    })
    .join("");
}

function orderStatus(status) {
  const labels = {
    PAYMENT_PENDING: "결제 대기",
    PAID: "결제 완료",
    PARTIALLY_CANCELED: "부분 취소",
    CANCELED: "취소 완료",
  };
  return labels[status] || status;
}

function fulfillmentStatus(status) {
  const labels = {
    PAID: "결제 완료",
    PREPARING: "상품 준비 중",
    SHIPPED: "배송 중",
    DELIVERED: "배송 완료",
  };
  return labels[status] || status || "-";
}

function nextFulfillmentStatus(status) {
  return { PAID: "PREPARING", PREPARING: "SHIPPED", SHIPPED: "DELIVERED" }[status] || null;
}

function renderOrders() {
  elements.orderList.setAttribute("aria-busy", String(state.ordersLoading));
  elements.orderCount.textContent = String(state.orderTotal);
  renderLoadMore(elements.orderLoadMore, {
    loading: state.ordersLoading,
    page: state.orderPage,
    totalPages: state.orderTotalPages,
    loaded: state.orders.length,
    total: state.orderTotal,
    label: "주문",
  });
  if (!isCustomer()) {
    elements.orderList.innerHTML = "";
    return;
  }

  if (state.ordersLoading && !state.orders.length) {
    elements.orderList.innerHTML = productSkeletons();
    return;
  }

  if (state.ordersError) {
    elements.orderList.innerHTML = `
      <div class="error-state">
        <span aria-hidden="true">!</span>
        <h3>주문을 불러오지 못했어요</h3>
        <p>${escapeHtml(state.ordersError.message)}</p>
      </div>
    `;
    return;
  }

  if (!state.orders.length) {
    elements.orderList.innerHTML = `
      <div class="empty-state">
        <span aria-hidden="true">↳</span>
        <h3>첫 주문을 기다리고 있어요</h3>
        <p>Shop에서 상품을 선택하면 주문과 포인트 차감 결과가 여기에 기록됩니다.</p>
      </div>
    `;
    return;
  }

  elements.orderList.innerHTML = state.orders
    .map((order) => {
      const items = (order.items || [])
        .map((item) => {
          const available = Number(item.orderedQuantity) - Number(item.canceledQuantity);
          return `
            <div class="order-item">
              <span class="order-item-thumb">${escapeHtml(initials(item.productName))}</span>
              <span class="order-item-copy">
                <strong>${escapeHtml(item.productName)}</strong>
                <span>${money(item.unitPrice)} P · 주문 ${item.orderedQuantity}개 · 취소 ${item.canceledQuantity}개</span>
              </span>
              <span class="order-item-actions">
                <strong>${money(item.paidAmount ?? (Number(item.unitPrice) * Number(item.orderedQuantity)))} P</strong>
                ${
                  available > 0 && ["PAID", "PREPARING"].includes(order.fulfillmentStatus)
                    ? `<button class="mini-button" type="button" data-product-cancel="${escapeHtml(item.productId)}" data-product-name="${escapeHtml(item.productName)}" data-max-quantity="${available}">부분 취소</button>`
                    : ""
                }
                ${available > 0 && order.fulfillmentStatus === "DELIVERED" ? `<button class="mini-button" type="button" data-return-order="${escapeHtml(order.id)}" data-return-item="${escapeHtml(item.id)}" data-return-name="${escapeHtml(item.productName)}" data-return-max="${available}">반품 신청</button>` : ""}
              </span>
            </div>
          `;
        })
        .join("");
      return `
        <article class="order-card">
          <header class="order-card-head">
            <span class="order-number"><small>${dateTime(order.orderedAt)}</small><strong>${escapeHtml(order.orderNumber)}</strong></span>
            <span class="order-status ${String(order.status).toLowerCase().replaceAll("_", "-")}">${escapeHtml(orderStatus(order.status))} · ${escapeHtml(fulfillmentStatus(order.fulfillmentStatus))}</span>
          </header>
          <div>${items}</div>
          ${
            order.shippingAddress
              ? `<div class="order-shipping"><strong>배송지</strong><span>${escapeHtml(order.shippingAddress.recipientName)} · ${escapeHtml(order.shippingAddress.phoneNumber)}</span><small>[${escapeHtml(order.shippingAddress.postalCode)}] ${escapeHtml(order.shippingAddress.addressLine1)} ${escapeHtml(order.shippingAddress.addressLine2 || "")}</small></div>`
              : ""
          }
          ${order.usedCouponCode ? `<div class="order-shipping"><strong>쿠폰</strong><span>${escapeHtml(order.usedCouponCode)} · ${points(order.discountAmount)} 할인</span></div>` : ""}
          ${(order.trackingCarrier || order.trackingNumber || order.estimatedDeliveryAt) ? `<div class="order-shipping"><strong>배송 추적</strong><span>${escapeHtml(order.trackingCarrier || "택배사 미정")} · ${escapeHtml(order.trackingNumber || "운송장 미등록")}</span>${order.trackingUrl ? `<a href="${escapeHtml(order.trackingUrl)}" target="_blank" rel="noopener noreferrer">배송 조회</a>` : ""}${order.estimatedDeliveryAt ? `<small>예상 배송 ${dateTime(order.estimatedDeliveryAt)}</small>` : ""}</div>` : ""}
          <footer class="order-summary">
            <div><small>ORDER TOTAL</small><strong>${points(order.totalAmount)}</strong></div>
            <div><small>CANCELED</small><strong>${points(order.canceledAmount)}</strong></div>
            <div><small>BALANCE AFTER</small><strong>${points(order.remainingPoints)}</strong></div>
          </footer>
        </article>
      `;
    })
    .join("");
}

function returnStatusLabel(status) {
  return { REQUESTED: "접수", COLLECTING: "회수 중", INSPECTING: "검수 중",
    APPROVED: "승인", REJECTED: "거절", REFUNDED: "환불 완료" }[status] || status;
}

function renderReturns() {
  if (elements.returnList) {
    elements.returnList.innerHTML = state.returns.length ? state.returns.map((item) => `
      <article class="admin-order-card"><header><div><small>${dateTime(item.requestedAt)}</small><strong>${escapeHtml(item.productName)}</strong></div><span class="status-chip">${escapeHtml(returnStatusLabel(item.status))}</span></header>
      <p>${escapeHtml(item.reason)} · ${item.quantity}개</p><small>예상 환불 ${points(item.refundAmount)} · 배송비 ${points(item.shippingFee)}</small>
      ${item.adminNote ? `<small>${escapeHtml(item.adminNote)}</small>` : ""}</article>`).join("")
      : '<div class="empty-inline">반품 신청 내역이 없습니다.</div>';
  }
  if (!elements.adminReturnList) return;
  const next = { REQUESTED: "COLLECTING", COLLECTING: "INSPECTING", INSPECTING: "APPROVED", APPROVED: "REFUNDED" };
  elements.adminReturnList.innerHTML = state.adminReturns.length ? state.adminReturns.map((item) => `
    <article class="admin-order-card"><header><div><small>${escapeHtml(item.orderId)}</small><strong>${escapeHtml(item.productName)} × ${item.quantity}</strong></div><span class="status-chip">${escapeHtml(returnStatusLabel(item.status))}</span></header>
    <p>${escapeHtml(item.reason)} · 환불 ${points(item.refundAmount)}</p><footer>
    ${next[item.status] ? `<button class="button button-dark" type="button" data-return-transition="${escapeHtml(item.id)}" data-return-status="${next[item.status]}">${escapeHtml(returnStatusLabel(next[item.status]))} 처리</button>` : ""}
    ${item.status === "INSPECTING" ? `<button class="mini-button" type="button" data-return-transition="${escapeHtml(item.id)}" data-return-status="REJECTED">거절</button>` : ""}</footer></article>`).join("")
    : '<div class="empty-inline">접수된 반품이 없습니다.</div>';
}

function renderMembers() {
  elements.memberTableBody.setAttribute("aria-busy", String(state.membersLoading));
  elements.adminMemberCount.textContent = String(state.memberTotal);
  renderLoadMore(elements.memberLoadMore, {
    loading: state.membersLoading,
    page: state.memberPage,
    totalPages: state.memberTotalPages,
    loaded: state.members.length,
    total: state.memberTotal,
    label: "고객",
  });
  if (state.membersLoading && !state.members.length) {
    elements.memberTableBody.innerHTML = `
      <tr><td colspan="4">고객 목록을 불러오는 중입니다.</td></tr>
    `;
    return;
  }
  if (state.membersError) {
    elements.memberTableBody.innerHTML = `
      <tr><td colspan="4">${escapeHtml(state.membersError.message)}</td></tr>
    `;
    return;
  }
  if (!state.members.length) {
    elements.memberTableBody.innerHTML = `
      <tr><td colspan="4">조회된 고객이 없습니다.</td></tr>
    `;
    return;
  }

  elements.memberTableBody.innerHTML = state.members
    .map(
      (member) => `
        <tr>
          <td><strong>${escapeHtml(member.customerId)}</strong></td>
          <td>${escapeHtml(member.name || "-")}</td>
          <td><span class="status-chip">${escapeHtml(member.status)}</span></td>
          <td class="id-cell" title="${escapeHtml(member.memberId)}">${escapeHtml(member.memberId)}</td>
        </tr>
      `,
    )
    .join("");
}

function renderCategories() {
  const selected = state.productFilters.categoryId;
  elements.categoryFilter.innerHTML = [
    '<option value="">전체 카테고리</option>',
    ...state.categories.map(
      (category) =>
        `<option value="${escapeHtml(category.id)}">${escapeHtml(category.name)}</option>`,
    ),
  ].join("");
  elements.categoryFilter.value = selected;
  const productCategory = $("#product-form [name=categoryId]");
  const productSelected = productCategory.value;
  productCategory.innerHTML = [
    '<option value="">카테고리 없음</option>',
    ...state.categories.map(
      (category) =>
        `<option value="${escapeHtml(category.id)}">${escapeHtml(category.name)}</option>`,
    ),
  ].join("");
  productCategory.value = productSelected;
}

function renderCart() {
  elements.cartList.setAttribute("aria-busy", String(state.cartLoading));
  const cart = state.cart || { items: [] };
  const items = cart.items || [];
  elements.cartCount.textContent = String(cart.totalQuantity || 0);
  elements.cartTotalQuantity.textContent = String(cart.totalQuantity || 0);
  elements.cartTotalAmount.textContent = points(cart.totalAmount);
  $("#checkout-total").textContent = points(cart.totalAmount);
  $("#clear-cart-button").disabled = !items.length || state.cartLoading;
  $("#checkout-button").disabled = !items.length || state.cartLoading;

  if (state.cartLoading && !items.length) {
    elements.cartList.innerHTML = productSkeletons();
    return;
  }
  if (state.cartError) {
    elements.cartList.innerHTML = `<div class="error-state"><span>!</span><h3>장바구니를 불러오지 못했어요</h3><p>${escapeHtml(state.cartError.message)}</p></div>`;
    return;
  }
  if (!items.length) {
    elements.cartList.innerHTML = '<div class="empty-state"><span>Bag</span><h3>장바구니가 비어 있어요</h3><p>상품 카드의 담기 버튼으로 원하는 상품을 모아보세요.</p></div>';
    return;
  }

  elements.cartList.innerHTML = items
    .map(
      (item) => `
        <article class="cart-item${item.orderable ? "" : " is-unavailable"}">
          <span class="cart-item-thumb tone-${toneFor(item.productId)}">${escapeHtml(initials(item.productName))}</span>
          <div class="cart-item-copy">
            <strong>${escapeHtml(item.productName)}</strong>
            <small>${points(item.unitPrice)} · 재고 ${Number(item.availableQuantity)}개</small>
            ${item.orderable ? "" : '<span class="field-error">현재 주문할 수 없습니다.</span>'}
          </div>
          <label class="cart-quantity">
            <span class="visually-hidden">${escapeHtml(item.productName)} 수량</span>
            <input type="number" min="1" max="${Math.max(1, Number(item.availableQuantity))}" value="${Number(item.quantity)}" data-cart-quantity="${escapeHtml(item.variantId || item.productId)}" />
          </label>
          <strong>${points(item.lineAmount)}</strong>
          <button class="text-button text-danger" type="button" data-cart-remove="${escapeHtml(item.variantId || item.productId)}">삭제</button>
        </article>
      `,
    )
    .join("");
}

function addressSummary(address) {
  return `[${address.postalCode}] ${address.addressLine1}${address.addressLine2 ? ` ${address.addressLine2}` : ""}`;
}

function renderAddresses() {
  elements.addressList.setAttribute("aria-busy", String(state.addressesLoading));
  if (!isCustomer()) {
    elements.addressList.innerHTML = "";
    return;
  }
  if (state.addressesLoading) {
    elements.addressList.innerHTML = '<div class="empty-inline">배송지를 불러오는 중입니다.</div>';
    return;
  }
  if (state.addressesError) {
    elements.addressList.innerHTML = `<div class="error-state compact"><p>${escapeHtml(state.addressesError.message)}</p></div>`;
    return;
  }
  if (!state.addresses.length) {
    elements.addressList.innerHTML = '<div class="empty-inline">저장된 배송지가 없습니다. 첫 배송지를 추가해 주세요.</div>';
    return;
  }
  elements.addressList.innerHTML = state.addresses
    .map(
      (address) => `
        <article class="address-card">
          <div>
            <strong>${escapeHtml(address.addressName)}${address.defaultAddress ? ' <span class="status-chip">기본</span>' : ""}</strong>
            <span>${escapeHtml(address.recipientName)} · ${escapeHtml(address.phoneNumber)}</span>
            <small>${escapeHtml(addressSummary(address))}</small>
          </div>
          <div class="address-actions">
            <button class="mini-button" type="button" data-address-edit="${escapeHtml(address.id)}">수정</button>
            <button class="mini-button danger" type="button" data-address-delete="${escapeHtml(address.id)}">삭제</button>
          </div>
        </article>
      `,
    )
    .join("");
}

function renderWishlist() {
  if (!isCustomer() || !state.wishlist.length) {
    elements.wishlistList.innerHTML = '<div class="empty-inline">관심 상품이 없습니다.</div>';
    return;
  }
  elements.wishlistList.innerHTML = state.wishlist.map((item) => `
    <article class="address-card">
      <div><strong>${escapeHtml(item.productName)}</strong><small>${points(item.productPrice)} · ${dateTime(item.addedAt)}</small></div>
      <div class="address-actions"><button class="mini-button danger" type="button" data-wishlist-remove="${escapeHtml(item.productId)}">삭제</button></div>
    </article>
  `).join("");
}

function renderStockAlerts() {
  if (!isCustomer() || !state.stockAlerts.length) {
    elements.stockAlertList.innerHTML = '<div class="empty-inline">신청한 재입고 알림이 없습니다.</div>';
    return;
  }
  elements.stockAlertList.innerHTML = state.stockAlerts.map((alert) => `
    <article class="address-card">
      <div><strong>${escapeHtml(alert.productName)}</strong><span class="status-chip">${alert.status === "NOTIFIED" ? "재입고 완료" : "알림 대기"}</span><small>현재 재고 ${Number(alert.availableQuantity)}개</small></div>
      <div class="address-actions"><button class="mini-button danger" type="button" data-stock-alert-remove="${escapeHtml(alert.productId)}">해지</button></div>
    </article>
  `).join("");
}

function renderCheckoutAddresses() {
  const select = elements.checkoutForm.elements.addressId;
  const preferred = state.addresses.find((address) => address.defaultAddress) || state.addresses[0];
  select.innerHTML = state.addresses
    .map(
      (address) => `<option value="${escapeHtml(address.id)}">${escapeHtml(address.addressName)} · ${escapeHtml(address.recipientName)}</option>`,
    )
    .join("");
  if (preferred) select.value = preferred.id;
  updateCheckoutAddressPreview();
}

function updateCheckoutAddressPreview() {
  const address = state.addresses.find(
    (candidate) => candidate.id === elements.checkoutForm.elements.addressId.value,
  );
  $("#checkout-address-preview").innerHTML = address
    ? `<strong>${escapeHtml(address.recipientName)} · ${escapeHtml(address.phoneNumber)}</strong><span>${escapeHtml(addressSummary(address))}</span>`
    : '<span>배송지를 먼저 추가해 주세요.</span>';
}

function transactionType(type) {
  return {
    SIGN_UP: "가입 포인트",
    DEBIT: "주문 사용",
    REFUND: "취소 환급",
    ADJUSTMENT: "관리자 조정",
  }[type] || type;
}

function renderTransactions() {
  elements.transactionList.setAttribute("aria-busy", String(state.transactionsLoading));
  renderLoadMore(elements.transactionLoadMore, {
    loading: state.transactionsLoading,
    page: state.transactionPage,
    totalPages: state.transactionTotalPages,
    loaded: state.transactions.length,
    total: state.transactionTotal,
    label: "포인트 내역",
  });
  if (!isCustomer()) {
    elements.transactionList.innerHTML = "";
    return;
  }
  if (state.transactionsLoading && !state.transactions.length) {
    elements.transactionList.innerHTML = '<div class="empty-inline">포인트 내역을 불러오는 중입니다.</div>';
    return;
  }
  if (state.transactionsError) {
    elements.transactionList.innerHTML = `<div class="error-state compact"><p>${escapeHtml(state.transactionsError.message)}</p></div>`;
    return;
  }
  if (!state.transactions.length) {
    elements.transactionList.innerHTML = '<div class="empty-inline">포인트 거래내역이 없습니다.</div>';
    return;
  }
  elements.transactionList.innerHTML = state.transactions
    .map((transaction) => {
      const storedAmount = Number(transaction.amount || 0);
      const amount = transaction.type === "DEBIT"
        ? -Math.abs(storedAmount)
        : transaction.type === "ADJUSTMENT"
          ? storedAmount
          : Math.abs(storedAmount);
      return `
        <article class="transaction-item">
          <span class="transaction-symbol ${amount >= 0 ? "credit" : "debit"}">${amount >= 0 ? "+" : "−"}</span>
          <div><strong>${escapeHtml(transactionType(transaction.type))}</strong><small>${dateTime(transaction.createdAt)}</small></div>
          <div><strong>${amount >= 0 ? "+" : ""}${points(amount)}</strong><small>잔액 ${points(transaction.balanceAfter)}</small></div>
        </article>
      `;
    })
    .join("");
}

function renderAdminOrders() {
  elements.adminOrderList.setAttribute("aria-busy", String(state.adminOrdersLoading));
  elements.adminOrderCount.textContent = String(state.adminOrderTotal);
  renderLoadMore(elements.adminOrderLoadMore, {
    loading: state.adminOrdersLoading,
    page: state.adminOrderPage,
    totalPages: state.adminOrderTotalPages,
    loaded: state.adminOrders.length,
    total: state.adminOrderTotal,
    label: "관리자 주문",
  });
  if (!isAdmin()) {
    elements.adminOrderList.innerHTML = "";
    return;
  }
  if (state.adminOrdersLoading && !state.adminOrders.length) {
    elements.adminOrderList.innerHTML = productSkeletons();
    return;
  }
  if (state.adminOrdersError) {
    elements.adminOrderList.innerHTML = `<div class="error-state"><span>!</span><h3>주문을 불러오지 못했어요</h3><p>${escapeHtml(state.adminOrdersError.message)}</p></div>`;
    return;
  }
  if (!state.adminOrders.length) {
    elements.adminOrderList.innerHTML = '<div class="empty-state"><span>▤</span><h3>접수된 주문이 없습니다</h3><p>고객 주문이 생성되면 배송 상태를 관리할 수 있습니다.</p></div>';
    return;
  }
  elements.adminOrderList.innerHTML = state.adminOrders
    .map((order) => {
      const nextStatus = nextFulfillmentStatus(order.fulfillmentStatus);
      const itemSummary = (order.items || [])
        .map((item) => `${escapeHtml(item.productName)} × ${Number(item.orderedQuantity) - Number(item.canceledQuantity)}`)
        .join(" · ");
      return `
        <article class="admin-order-card">
          <header>
            <div><small>${dateTime(order.orderedAt)}</small><strong>${escapeHtml(order.orderNumber)}</strong></div>
            <span class="status-chip">${escapeHtml(fulfillmentStatus(order.fulfillmentStatus))}</span>
          </header>
          <p>${itemSummary || "주문 상품 없음"}</p>
          ${order.shippingAddress ? `<small>${escapeHtml(order.shippingAddress.recipientName)} · ${escapeHtml(addressSummary(order.shippingAddress))}</small>` : '<small>배송지 정보 없음</small>'}
          ${(order.trackingCarrier || order.trackingNumber) ? `<small>${escapeHtml(order.trackingCarrier || "택배사 미정")} · ${escapeHtml(order.trackingNumber || "운송장 미등록")}</small>` : ""}
          <footer>
            <strong>${points(Number(order.totalAmount) - Number(order.canceledAmount))}</strong>
            <button class="mini-button" type="button" data-order-history="${escapeHtml(order.id)}">이력</button>
            ${nextStatus ? `<button class="button button-dark" type="button" data-fulfillment-order="${escapeHtml(order.id)}" data-fulfillment-status="${nextStatus}">${escapeHtml(fulfillmentStatus(nextStatus))} 처리</button>` : '<span class="status-chip">처리 완료</span>'}
          </footer>
          <div class="order-history is-hidden" data-order-history-panel="${escapeHtml(order.id)}"></div>
        </article>
      `;
    })
    .join("");
}

function switchView(view, updateHash = true) {
  if (view === "admin" && !isAdmin()) {
    showToast("관리자 전용 화면입니다", "ADMIN 역할로 로그인해 주세요.", "error");
    view = "shop";
  }

  if (isAdmin() && ["orders", "account"].includes(view)) {
    view = "admin";
  }

  state.view = view;
  $$('[data-view-panel]').forEach((panel) => {
    panel.classList.toggle("is-active", panel.dataset.viewPanel === view);
  });
  $$('[data-view]').forEach((button) => {
    const active = button.dataset.view === view;
    button.classList.toggle("is-active", active);
    if (active) {
      button.setAttribute("aria-current", "page");
    } else {
      button.removeAttribute("aria-current");
    }
  });

  if (view === "orders" && isCustomer()) {
    loadOrders();
    loadReturns();
  }
  if (view === "account" && isCustomer()) {
    loadCustomer();
    loadAddresses();
    loadTransactions();
  }
  if (view === "admin" && isAdmin()) {
    loadMembers();
    loadAdminOrders();
    loadAdminReturns();
  }

  if (updateHash) {
    window.history.replaceState(null, "", `#${view}`);
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
}

async function loadProductStocks(products) {
  const stocksByProductId = new Map();
  for (let index = 0; index < products.length; index += 100) {
    const productIds = products.slice(index, index + 100).map((product) => product.id);
    const stocks = await shopApi.stocks(productIds);
    stocks.forEach((stock) => stocksByProductId.set(stock.productId, stock));
  }
  return stocksByProductId;
}

async function loadCategories() {
  try {
    state.categories = (await shopApi.categories()).filter(
      (category) => category.status === "ACTIVE",
    );
    renderCategories();
  } catch (error) {
    showApiError(error, "카테고리를 불러오지 못했습니다.");
  }
}

async function loadProducts({ append = false } = {}) {
  if (state.productsLoading) {
    if (!append) productsReloadQueued = true;
    return;
  }
  if (append && state.productPage + 1 >= state.productTotalPages) return;

  const targetPage = append ? state.productPage + 1 : 0;
  if (!append) {
    state.products = [];
    state.productTotal = 0;
    state.productPage = -1;
    state.productTotalPages = 0;
    state.productsError = null;
    state.stocksError = null;
  }
  state.productsLoading = true;
  renderProducts();
  try {
    let page;
    const searchOnly = state.productFilters.query && !state.productFilters.categoryId
      && !state.productFilters.minPrice && !state.productFilters.maxPrice;
    if (searchOnly) {
      try {
        page = await shopApi.searchProducts(state.productFilters.query, targetPage, PRODUCT_PAGE_SIZE);
      } catch (error) {
        if (![404, 503].includes(error?.status)) throw error;
      }
    }
    page ||= await shopApi.products({ page: targetPage, size: PRODUCT_PAGE_SIZE, ...state.productFilters });
    const products = page.content || [];
    let stocksByProductId = new Map();
    try {
      stocksByProductId = await loadProductStocks(products);
    } catch (error) {
      state.stocksError = error;
    }
    const loadedProducts = products.map((product) => ({
      ...product,
      stock: stocksByProductId.get(product.id) || null,
    }));
    const combined = append ? [...state.products, ...loadedProducts] : loadedProducts;
    state.products = [...new Map(combined.map((product) => [product.id, product])).values()];
    state.productPage = Number(page.page ?? targetPage);
    state.productTotalPages = Number(page.totalPages || 0);
    state.productTotal = Number(page.totalElements ?? state.products.length);
  } catch (error) {
    if (append) {
      showApiError(error, "다음 상품을 불러오지 못했습니다.");
    } else {
      state.productsError = error;
    }
  } finally {
    state.productsLoading = false;
    renderProducts();
    if (productsReloadQueued) {
      productsReloadQueued = false;
      loadProducts();
    }
  }
}

async function loadCart({ quiet = false } = {}) {
  if (!isCustomer() || state.cartLoading) return;
  state.cartLoading = true;
  state.cartError = null;
  renderCart();
  try {
    state.cart = await shopApi.cart();
  } catch (error) {
    state.cartError = error;
    if (!quiet || error?.status === 401) showApiError(error, "장바구니를 불러오지 못했습니다.");
  } finally {
    state.cartLoading = false;
    renderCart();
  }
}

async function loadAddresses({ quiet = false } = {}) {
  if (!isCustomer() || state.addressesLoading) return;
  state.addressesLoading = true;
  state.addressesError = null;
  renderAddresses();
  try {
    state.addresses = await shopApi.addresses();
  } catch (error) {
    state.addressesError = error;
    if (!quiet || error?.status === 401) showApiError(error, "배송지를 불러오지 못했습니다.");
  } finally {
    state.addressesLoading = false;
    renderAddresses();
  }
}

async function loadWishlist({ quiet = false } = {}) {
  if (!isCustomer()) return;
  try {
    state.wishlist = await shopApi.wishlist();
  } catch (error) {
    if (!quiet) showApiError(error, "관심 상품을 불러오지 못했습니다.");
  } finally {
    renderWishlist();
  }
}

async function loadStockAlerts({ quiet = false } = {}) {
  if (!isCustomer()) return;
  try {
    const page = await shopApi.stockAlerts();
    state.stockAlerts = page.content || [];
  } catch (error) {
    if (!quiet) showApiError(error, "재입고 알림을 불러오지 못했습니다.");
  } finally {
    renderStockAlerts();
  }
}

async function openReviews(product) {
  if (!product) return;
  elements.reviewDialog.dataset.productId = product.id;
  $("#review-product-name").textContent = product.name;
  elements.reviewList.innerHTML = '<div class="empty-inline">리뷰를 불러오는 중입니다.</div>';
  elements.reviewForm.classList.toggle("is-hidden", !isCustomer());
  state.myReview = null;
  openDialog(elements.reviewDialog);
  try {
    const requests = [shopApi.productReviews(product.id)];
    if (isCustomer()) requests.push(shopApi.myReview(product.id).catch((error) => {
      if (error?.status === 404) return null;
      throw error;
    }));
    const [page, mine] = await Promise.all(requests);
    state.reviews = page.content || [];
    state.myReview = mine || null;
    elements.reviewForm.elements.rating.value = String(mine?.rating || 5);
    elements.reviewForm.elements.comment.value = mine?.comment || "";
    $("#delete-review-button").classList.toggle("is-hidden", !mine);
    elements.reviewList.innerHTML = state.reviews.length
      ? state.reviews.map((review) => `<article class="address-card"><div><strong>${"★".repeat(Number(review.rating))}${"☆".repeat(5 - Number(review.rating))}</strong><span>${escapeHtml(review.comment || "내용 없는 리뷰")}</span><small>${dateTime(review.updatedAt)}</small></div></article>`).join("")
      : '<div class="empty-inline">아직 등록된 리뷰가 없습니다.</div>';
  } catch (error) {
    elements.reviewList.innerHTML = `<div class="error-state compact"><p>${escapeHtml(error.message)}</p></div>`;
  }
}

async function loadCustomer({ quiet = true } = {}) {
  if (!isCustomer()) return;
  const requestGeneration = authGeneration;
  const sessionSnapshot = captureSessionSnapshot();
  try {
    const customer = await shopApi.me();
    if (!isCurrentSessionRequest(requestGeneration, sessionSnapshot)) return;
    state.customer = customer;
    state.session = {
      ...state.session,
      memberId: customer.memberId,
      customerId: customer.customerId,
      name: customer.name,
      role: customer.role,
    };
    rememberSession(state.session);
    renderSession();
  } catch (error) {
    if (!isCurrentSessionRequest(requestGeneration, sessionSnapshot)) return;
    if (!quiet || error.status === 401) showApiError(error);
  }
}

async function loadOrders({ append = false } = {}) {
  if (!isCustomer()) return;
  if (state.ordersLoading) {
    if (!append) ordersReloadQueued = true;
    return;
  }
  if (append && state.orderPage + 1 >= state.orderTotalPages) return;

  const targetPage = append ? state.orderPage + 1 : 0;
  if (!append) {
    state.orders = [];
    state.orderTotal = 0;
    state.orderPage = -1;
    state.orderTotalPages = 0;
    state.ordersError = null;
  }
  state.ordersLoading = true;
  renderOrders();
  try {
    const page = await shopApi.orders(targetPage, ORDER_PAGE_SIZE);
    const loadedOrders = page.content || [];
    const combined = append ? [...state.orders, ...loadedOrders] : loadedOrders;
    state.orders = [...new Map(combined.map((order) => [order.id, order])).values()];
    state.orderPage = Number(page.page ?? targetPage);
    state.orderTotalPages = Number(page.totalPages || 0);
    state.orderTotal = Number(page.totalElements ?? state.orders.length);
  } catch (error) {
    if (append) {
      showApiError(error, "다음 주문을 불러오지 못했습니다.");
    } else {
      state.ordersError = error;
      if (error?.status === 401) showApiError(error);
    }
  } finally {
    state.ordersLoading = false;
    renderOrders();
    if (ordersReloadQueued) {
      ordersReloadQueued = false;
      loadOrders();
    }
  }
}

async function loadTransactions({ append = false } = {}) {
  if (!isCustomer()) return;
  if (state.transactionsLoading) {
    if (!append) transactionsReloadQueued = true;
    return;
  }
  if (append && state.transactionPage + 1 >= state.transactionTotalPages) return;
  const targetPage = append ? state.transactionPage + 1 : 0;
  if (!append) {
    state.transactions = [];
    state.transactionTotal = 0;
    state.transactionPage = -1;
    state.transactionTotalPages = 0;
    state.transactionsError = null;
  }
  state.transactionsLoading = true;
  renderTransactions();
  try {
    const [wallet, page] = await Promise.all([
      shopApi.wallet(),
      shopApi.walletTransactions(targetPage, TRANSACTION_PAGE_SIZE),
    ]);
    state.wallet = wallet;
    const loaded = page.content || [];
    const combined = append ? [...state.transactions, ...loaded] : loaded;
    state.transactions = [...new Map(combined.map((item) => [item.id, item])).values()];
    state.transactionPage = Number(page.page ?? targetPage);
    state.transactionTotalPages = Number(page.totalPages || 0);
    state.transactionTotal = Number(page.totalElements ?? state.transactions.length);
    renderCustomer();
  } catch (error) {
    if (append) showApiError(error, "다음 포인트 내역을 불러오지 못했습니다.");
    else state.transactionsError = error;
  } finally {
    state.transactionsLoading = false;
    renderTransactions();
    if (transactionsReloadQueued) {
      transactionsReloadQueued = false;
      loadTransactions();
    }
  }
}

async function loadMembers({ append = false } = {}) {
  if (!isAdmin()) return;
  if (state.membersLoading) {
    if (!append) membersReloadQueued = true;
    return;
  }
  if (append && state.memberPage + 1 >= state.memberTotalPages) return;

  const targetPage = append ? state.memberPage + 1 : 0;
  if (!append) {
    state.members = [];
    state.memberTotal = 0;
    state.memberPage = -1;
    state.memberTotalPages = 0;
    state.membersError = null;
  }
  state.membersLoading = true;
  renderMembers();
  try {
    const page = await shopApi.members(targetPage, MEMBER_PAGE_SIZE);
    const loadedMembers = page.content || [];
    const combined = append ? [...state.members, ...loadedMembers] : loadedMembers;
    state.members = [...new Map(combined.map((member) => [member.memberId, member])).values()];
    state.memberPage = Number(page.page ?? targetPage);
    state.memberTotalPages = Number(page.totalPages || 0);
    state.memberTotal = Number(page.totalElements ?? state.members.length);
  } catch (error) {
    if (append) {
      showApiError(error, "다음 고객을 불러오지 못했습니다.");
    } else {
      state.membersError = error;
      if (error?.status === 401) showApiError(error);
    }
  } finally {
    state.membersLoading = false;
    renderMembers();
    if (membersReloadQueued) {
      membersReloadQueued = false;
      loadMembers();
    }
  }
}

async function loadAdminOrders({ append = false } = {}) {
  if (!isAdmin()) return;
  if (state.adminOrdersLoading) {
    if (!append) adminOrdersReloadQueued = true;
    return;
  }
  if (append && state.adminOrderPage + 1 >= state.adminOrderTotalPages) return;
  const targetPage = append ? state.adminOrderPage + 1 : 0;
  if (!append) {
    state.adminOrders = [];
    state.adminOrderTotal = 0;
    state.adminOrderPage = -1;
    state.adminOrderTotalPages = 0;
    state.adminOrdersError = null;
  }
  state.adminOrdersLoading = true;
  renderAdminOrders();
  try {
    const page = await shopApi.adminOrders(targetPage, ADMIN_ORDER_PAGE_SIZE);
    const loaded = page.content || [];
    const combined = append ? [...state.adminOrders, ...loaded] : loaded;
    state.adminOrders = [...new Map(combined.map((order) => [order.id, order])).values()];
    state.adminOrderPage = Number(page.page ?? targetPage);
    state.adminOrderTotalPages = Number(page.totalPages || 0);
    state.adminOrderTotal = Number(page.totalElements ?? state.adminOrders.length);
  } catch (error) {
    if (append) showApiError(error, "다음 관리자 주문을 불러오지 못했습니다.");
    else state.adminOrdersError = error;
  } finally {
    state.adminOrdersLoading = false;
    renderAdminOrders();
    if (adminOrdersReloadQueued) {
      adminOrdersReloadQueued = false;
      loadAdminOrders();
    }
  }
}

async function loadReturns() {
  if (!isCustomer()) return;
  try { state.returns = (await shopApi.returns()).content || []; }
  catch (error) { showApiError(error, "반품 내역을 불러오지 못했습니다."); }
  finally { renderReturns(); }
}

async function loadAdminReturns() {
  if (!isAdmin()) return;
  try { state.adminReturns = (await shopApi.adminReturns()).content || []; }
  catch (error) { showApiError(error, "관리자 반품 내역을 불러오지 못했습니다."); }
  finally { renderReturns(); }
}

async function restoreSession() {
  const restoreGeneration = authGeneration;
  try {
    const customer = await shopApi.me();
    if (restoreGeneration !== authGeneration) return;
    setSession(
      {
        memberId: customer.memberId,
        customerId: customer.customerId,
        name: customer.name,
        role: customer.role,
      },
      customer,
    );
    await Promise.all([loadCart({ quiet: true }), loadAddresses({ quiet: true }), loadWishlist({ quiet: true }), loadStockAlerts({ quiet: true })]);
    return;
  } catch (error) {
    if (restoreGeneration !== authGeneration) return;
    if (![401, 403].includes(error.status)) return;
  }

  const hint = sessionHint();
  if (hint?.role !== "ADMIN") {
    if (restoreGeneration !== authGeneration) return;
    clearSession();
    return;
  }

  try {
    const page = await shopApi.members(0, MEMBER_PAGE_SIZE);
    if (restoreGeneration !== authGeneration) return;
    state.members = page.content || [];
    state.memberTotal = Number(page.totalElements || state.members.length);
    state.memberPage = Number(page.page || 0);
    state.memberTotalPages = Number(page.totalPages || 0);
    state.membersError = null;
    setSession({ ...hint, name: hint.customerId || "관리자" });
    renderMembers();
  } catch {
    if (restoreGeneration !== authGeneration) return;
    clearSession();
  }
}

function openDialog(dialog) {
  if (!dialog.open) {
    dialog.returnFocusTo = document.activeElement;
    dialog.showModal();
  }
}

function closeDialog(dialog) {
  if (dialog?.open) {
    const returnFocusTo = dialog.returnFocusTo;
    dialog.close();
    if (returnFocusTo instanceof HTMLElement && returnFocusTo.isConnected) {
      queueMicrotask(() => returnFocusTo.focus());
    }
  }
}

function selectAuthTab(tab) {
  clearAuthFeedback();
  $$('.auth-form', elements.authDialog).forEach(clearFormFieldErrors);
  $$('[data-auth-tab]', elements.authDialog).forEach((button) => {
    const selected = button.dataset.authTab === tab;
    button.classList.toggle("is-active", selected);
    button.setAttribute("aria-selected", String(selected));
    button.tabIndex = selected ? 0 : -1;
  });
  $$('[data-auth-panel]', elements.authDialog).forEach((panel) => {
    const selected = panel.dataset.authPanel === tab;
    panel.classList.toggle("is-hidden", !selected);
    panel.hidden = !selected;
  });
  $$('[data-password-toggle]', elements.authDialog).forEach((button) => {
    const input = $("input", button.closest(".password-field"));
    input.type = "password";
    button.textContent = "보기";
    button.setAttribute("aria-pressed", "false");
  });
}

function openAuth(tab = "login") {
  selectAuthTab(tab);
  if (tab === "login") restoreRememberedCustomerId();
  openDialog(elements.authDialog);
}

function productById(productId) {
  return state.products.find((product) => product.id === productId);
}

function refreshOrderCommand() {
  elements.orderDialog.dataset.commandKey = createCommandId();
}

async function completeFakePayment(order, testCardNumber) {
  if (Number(order.paymentAmount || 0) <= 0) return order;
  const payment = await shopApi.preparePayment(order.id, "CARD");
  const approved = await shopApi.approveFakePayment(payment.id, testCardNumber);
  if (approved.status !== "PAID") {
    const error = new Error(approved.failureMessage || "모의 결제가 승인되지 않았습니다.");
    error.code = approved.failureCode || "PAYMENT_DECLINED";
    error.status = 409;
    throw error;
  }
  return { ...order, status: "PAID", fulfillmentStatus: "PAID" };
}

async function openOrder(product) {
  if (!isCustomer()) {
    if (isAdmin()) {
      showToast("관리자는 주문할 수 없습니다", "CUSTOMER 계정으로 로그인해 주세요.", "error");
    } else {
      openAuth("login");
    }
    return;
  }

  if (!product?.stock?.orderable) {
    showToast(
      "현재 주문할 수 없는 상품입니다",
      stockStatusLabel(product?.stock),
      "error",
    );
    return;
  }

  const form = $("#order-form");
  form.elements.productId.value = product.id;
  let variants;
  try {
    variants = await shopApi.productVariants(product.id);
  } catch (error) {
    showApiError(error, "상품 옵션을 불러오지 못했습니다.");
    return;
  }
  const variantSelect = form.elements.variantId;
  variantSelect.innerHTML = variants.map((variant) => `<option value="${escapeHtml(variant.id)}" data-price="${Number(variant.price)}">${escapeHtml(variant.optionValue || "기본 옵션")} · ${money(variant.price)} P</option>`).join("");
  $("#order-variant-field").classList.toggle("is-hidden", variants.length <= 1);
  const selected = variants[0];
  const selectedStock = selected?.id === product.id ? product.stock : await shopApi.stock(selected.id);
  const maxOrderQuantity = Math.max(1, Number(selectedStock?.maxOrderQuantity || 1));
  form.elements.quantity.value = 1;
  form.elements.couponCode.value = "";
  form.elements.pointAmount.value = "";
  form.elements.testCardNumber.value = "4242-4242-4242-4242";
  form.elements.quantity.max = maxOrderQuantity;
  $("#order-quantity-label").textContent = `주문 수량 · 최대 ${maxOrderQuantity}개`;
  form.dataset.unitPrice = selected?.price ?? product.price;
  $("#order-product-name").textContent = product.name;
  $("#order-product-price").textContent = points(selected?.price ?? product.price);
  const imageUrl = safeImageUrl(product.imageUrl);
  $("#order-product-visual").innerHTML = imageUrl
    ? `<img src="${escapeHtml(imageUrl)}" alt="" />`
    : `<span>${escapeHtml(initials(product.name))}</span>`;
  $("#order-product-visual").className = `modal-product-visual tone-${toneFor(product.id)}${imageUrl ? " has-image" : ""}`;
  refreshOrderCommand();
  updateOrderTotal();
  openDialog(elements.orderDialog);
}

function updateOrderTotal() {
  const form = $("#order-form");
  const quantity = Math.max(1, Number(form.elements.quantity.value || 1));
  const price = Number(form.dataset.unitPrice || 0);
  $("#order-total").textContent = points(price * quantity);
}

function refreshCancelCommand() {
  elements.cancelDialog.dataset.commandKey = createCommandId();
}

function openCancel({ productId, productName, maxQuantity }) {
  const form = $("#cancel-form");
  const purchasedQuantity = state.customer?.products?.find(
    (product) => product.productId === productId,
  )?.quantity;
  const maximum = Math.max(1, Number(purchasedQuantity || maxQuantity || 1));
  form.elements.productId.value = productId;
  form.elements.quantity.value = 1;
  form.elements.quantity.max = maximum;
  $("#cancel-product-name").textContent = productName;
  refreshCancelCommand();
  openDialog(elements.cancelDialog);
}

function openProductEditor(product = null) {
  const form = $("#product-form");
  form.reset();
  form.elements.productId.value = product?.id || "";
  form.elements.productName.value = product?.name || "";
  form.elements.productPrice.value = product?.price || "";
  form.elements.categoryId.value = product?.categoryId || "";
  form.elements.description.value = product?.description || "";
  form.elements.imageUrl.value = product?.imageUrl || "";
  form.elements.initialQuantity.value = 100;
  form.elements.initialQuantity.disabled = Boolean(product);
  $("#initial-stock-field").classList.toggle("is-hidden", Boolean(product));
  $("#product-dialog-title").textContent = product ? "상품 정보 수정" : "새 상품 등록";
  $("#product-submit-button").textContent = product ? "수정 내용 저장" : "상품 등록";
  openDialog(elements.productDialog);
}

function clearStockFeedback() {
  elements.stockFormFeedback.replaceChildren();
  elements.stockFormFeedback.classList.add("is-hidden");
}

function showStockFeedback(message) {
  elements.stockFormFeedback.textContent = message;
  elements.stockFormFeedback.classList.remove("is-hidden");
}

function refreshStockCommand() {
  elements.stockDialog.dataset.commandKey = createCommandId();
}

function openStockEditor(product) {
  if (!product || state.stocksError) return;
  const form = $("#stock-form");
  const initializing = !product.stock;
  form.reset();
  clearStockFeedback();
  form.elements.productId.value = product.id;
  form.elements.mode.value = initializing ? "initialize" : "adjust";
  form.elements.quantity.value = initializing ? 100 : 1;
  form.elements.quantity.min = initializing ? 0 : -1_000_000;
  form.elements.reason.disabled = initializing;
  form.elements.reason.required = !initializing;
  $("#stock-reason-field").classList.toggle("is-hidden", initializing);
  $("#stock-quantity-label").textContent = initializing
    ? "초기 주문 가능 재고"
    : "재고 증감 수량";
  $("#stock-dialog-title").textContent = initializing
    ? `${product.name} 재고 초기화`
    : `${product.name} 재고 조정`;
  $("#stock-dialog-summary").textContent = initializing
    ? "아직 재고가 없는 기존 상품입니다. 최초 수량을 한 번 등록합니다."
    : `현재 주문 가능 재고 ${product.stock.availableQuantity}개 · 입고는 양수, 차감은 음수로 입력하세요.`;
  $("#stock-submit-button").textContent = initializing ? "재고 초기화" : "재고 반영";
  refreshStockCommand();
  openDialog(elements.stockDialog);
}

function openAddressEditor(address = null) {
  const form = $("#address-form");
  form.reset();
  form.elements.addressId.value = address?.id || "";
  ["addressName", "recipientName", "phoneNumber", "postalCode", "addressLine1", "addressLine2"]
    .forEach((name) => {
      form.elements[name].value = address?.[name] || "";
    });
  form.elements.defaultAddress.checked = Boolean(address?.defaultAddress);
  $("#address-dialog-title").textContent = address ? "배송지 수정" : "배송지 저장";
  $("#address-submit-button").textContent = address ? "수정 내용 저장" : "배송지 저장";
  openDialog(elements.addressDialog);
}

async function openCheckout() {
  if (!state.cart.items?.length) return;
  if (state.cart.items.some((item) => !item.orderable)) {
    showToast("주문할 수 없는 상품이 있습니다", "재고가 부족한 상품을 수정하거나 삭제해 주세요.", "error");
    return;
  }
  await loadAddresses();
  if (!state.addresses.length) {
    closeDialog(elements.cartDialog);
    showToast("배송지를 먼저 등록해 주세요", "내 정보에서 주문에 사용할 배송지를 저장합니다.", "error");
    switchView("account");
    openAddressEditor();
    return;
  }
  renderCheckoutAddresses();
  elements.checkoutForm.elements.pointAmount.value = "";
  elements.checkoutForm.elements.testCardNumber.value = "4242-4242-4242-4242";
  elements.checkoutDialog.dataset.commandKey = createCommandId();
  openDialog(elements.checkoutDialog);
}

function bindNavigation() {
  $(".brand").addEventListener("click", (event) => {
    event.preventDefault();
    switchView("shop");
  });
  $$('[data-view]').forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });
  elements.profileButton.addEventListener("click", () =>
    switchView(isAdmin() ? "admin" : "account"),
  );
  $("#hero-start-button").addEventListener("click", () => {
    if (state.session) {
      $("#products-title").scrollIntoView({ behavior: "smooth" });
    } else {
      openAuth("signup");
    }
  });
}

function bindDialogs() {
  $$('[data-open-auth]').forEach((button) => {
    button.addEventListener("click", () => openAuth(button.dataset.authTab || "login"));
  });
  elements.authButton.addEventListener("click", () => openAuth("login"));

  $$('[data-close-dialog]').forEach((button) => {
    button.addEventListener("click", () => closeDialog(button.closest("dialog")));
  });
  $$('dialog').forEach((dialog) => {
    dialog.addEventListener("click", (event) => {
      if (event.target === dialog) closeDialog(dialog);
    });
    dialog.addEventListener("cancel", (event) => {
      event.preventDefault();
      closeDialog(dialog);
    });
  });

  $$('[data-auth-tab]', elements.authDialog).forEach((button) => {
    button.addEventListener("click", () => selectAuthTab(button.dataset.authTab));
    button.addEventListener("keydown", (event) => {
      const tabs = $$('[role="tab"]', elements.authDialog);
      const currentIndex = tabs.indexOf(event.currentTarget);
      let nextIndex = currentIndex;
      if (["ArrowRight", "ArrowDown"].includes(event.key)) {
        nextIndex = (currentIndex + 1) % tabs.length;
      } else if (["ArrowLeft", "ArrowUp"].includes(event.key)) {
        nextIndex = (currentIndex - 1 + tabs.length) % tabs.length;
      } else if (event.key === "Home") {
        nextIndex = 0;
      } else if (event.key === "End") {
        nextIndex = tabs.length - 1;
      } else {
        return;
      }
      event.preventDefault();
      selectAuthTab(tabs[nextIndex].dataset.authTab);
      tabs[nextIndex].focus();
    });
  });

  $("[data-forgot-password]").addEventListener("click", () => {
    const loginId = $("#login-form").elements.customerId.value.trim();
    $("#password-reset-form").elements.customerId.value = loginId;
    selectAuthTab("reset");
    $("#auth-tab-reset").focus();
  });

  $$('[data-password-toggle]').forEach((button) => {
    button.addEventListener("click", () => {
      const input = $("input", button.closest(".password-field"));
      const revealing = input.type === "password";
      input.type = revealing ? "text" : "password";
      button.textContent = revealing ? "숨기기" : "보기";
      button.setAttribute("aria-pressed", String(revealing));
    });
  });
}

function bindProducts() {
  let searchTimer;
  elements.productSearch.addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
      state.productFilters.query = elements.productSearch.value.trim();
      loadProducts();
    }, 300);
  });
  elements.catalogFilterForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const values = new FormData(event.currentTarget);
    const minPrice = String(values.get("minPrice") || "").trim();
    const maxPrice = String(values.get("maxPrice") || "").trim();
    if (minPrice && maxPrice && Number(minPrice) > Number(maxPrice)) {
      showToast("가격 범위를 확인해 주세요", "최소 가격은 최대 가격보다 클 수 없습니다.", "error");
      return;
    }
    state.productFilters = {
      ...state.productFilters,
      categoryId: String(values.get("categoryId") || ""),
      minPrice,
      maxPrice,
    };
    loadProducts();
  });
  elements.catalogFilterForm.addEventListener("reset", () => {
    queueMicrotask(() => {
      state.productFilters = { query: "", categoryId: "", minPrice: "", maxPrice: "" };
      elements.productSearch.value = "";
      loadProducts();
    });
  });
  $("#refresh-products-button").addEventListener("click", () => loadProducts());
  elements.productLoadMore.addEventListener("click", () =>
    loadProducts({ append: true }),
  );
  elements.addProductButton.addEventListener("click", () => openProductEditor());
  elements.adminAddProductButton.addEventListener("click", () => openProductEditor());

  elements.productGrid.addEventListener("click", async (event) => {
    if (event.target.closest("[data-product-retry]")) {
      if (!state.productsError && !state.productsLoading) {
        return;
      }
      await loadProducts();
      return;
    }

    const buy = event.target.closest("[data-product-buy]");
    const cart = event.target.closest("[data-product-cart]");
    const stock = event.target.closest("[data-product-stock]");
    const reviews = event.target.closest("[data-product-reviews]");
    const wishlist = event.target.closest("[data-product-wishlist]");
    const stockAlert = event.target.closest("[data-product-alert]");
    const edit = event.target.closest("[data-product-edit]");
    const remove = event.target.closest("[data-product-delete]");

    if (buy) await openOrder(productById(buy.dataset.productBuy));
    if (reviews) await openReviews(productById(reviews.dataset.productReviews));
    if (wishlist) {
      try {
        await withLoading("관심 상품에 추가하고 있습니다", () =>
          shopApi.addWishlist(wishlist.dataset.productWishlist));
        await loadWishlist({ quiet: true });
        showToast("관심 상품에 추가했습니다");
      } catch (error) {
        showApiError(error, "관심 상품을 추가하지 못했습니다.");
      }
    }
    if (stockAlert) {
      try {
        await withLoading("재입고 알림을 신청하고 있습니다", () =>
          shopApi.subscribeStockAlert(stockAlert.dataset.productAlert));
        await loadStockAlerts({ quiet: true });
        showToast("재입고 알림을 신청했습니다");
      } catch (error) {
        showApiError(error, "재입고 알림을 신청하지 못했습니다.");
      }
    }
    if (cart) {
      if (!isCustomer()) {
        openAuth("login");
        return;
      }
      const product = productById(cart.dataset.productCart);
      try {
        state.cart = await withLoading("장바구니에 담고 있습니다", () =>
          shopApi.addCartItem(product.id, 1),
        );
        renderCart();
        showToast("장바구니에 담았습니다", product.name);
      } catch (error) {
        showApiError(error, "장바구니에 담지 못했습니다.");
      }
    }
    if (stock) openStockEditor(productById(stock.dataset.productStock));
    if (edit) openProductEditor(productById(edit.dataset.productEdit));
    if (remove) {
      const product = productById(remove.dataset.productDelete);
      if (!product || !window.confirm(`'${product.name}' 상품을 삭제할까요?`)) return;
      try {
        await withLoading("상품을 삭제하고 있습니다", () => shopApi.deleteProduct(product.id));
        showToast("상품을 삭제했습니다", "목록에서는 숨겨지고 이름은 다시 사용할 수 있습니다.");
        await loadProducts();
      } catch (error) {
        showApiError(error, "상품 삭제에 실패했습니다.");
      }
    }
  });
}

function bindForms() {
  $$('.auth-form input').forEach((input) => {
    input.addEventListener("input", () => clearFieldError(input));
    input.addEventListener("invalid", (event) => {
      event.preventDefault();
      showFieldError(input.form, input.name, nativeFieldError(input));
      showAuthFeedback("입력값을 확인해 주세요", "표시된 항목을 수정한 뒤 다시 시도해 주세요.");
      queueMicrotask(() => $('[aria-invalid="true"]', input.form)?.focus());
    });
  });

  $("#login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    clearFormFieldErrors(formElement);
    clearAuthFeedback();
    const form = new FormData(formElement);
    const payload = Object.fromEntries(form.entries());
    const rememberCustomerId = Boolean(payload.rememberCustomerId);
    delete payload.rememberCustomerId;
    payload.customerId = payload.customerId.trim();
    if (!hasValidBcryptByteLength(payload.customerPassword, formElement.elements.customerPassword)) return;
    invalidatePendingSessionRestore();
    try {
      const login = await withLoading("안전하게 로그인하고 있습니다", () => shopApi.login(payload));
      if (rememberCustomerId) {
        window.localStorage.setItem(REMEMBERED_CUSTOMER_ID_KEY, payload.customerId);
      } else {
        window.localStorage.removeItem(REMEMBERED_CUSTOMER_ID_KEY);
      }
      setSession({ ...login, name: login.customerId });
      if (login.role === "CUSTOMER") {
        await loadCustomer({ quiet: false });
        await Promise.all([loadCart({ quiet: true }), loadAddresses({ quiet: true }), loadWishlist({ quiet: true }), loadStockAlerts({ quiet: true })]);
        switchView("shop");
      } else {
        switchView("admin");
      }
      closeDialog(elements.authDialog);
      formElement.reset();
      showToast("로그인되었습니다", `${login.customerId} · ${login.role}`);
    } catch (error) {
      showAuthError(
        error,
        "아이디 또는 비밀번호가 올바르지 않습니다.",
        formElement,
        "customerPassword",
      );
    }
  });

  $("#signup-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    clearFormFieldErrors(formElement);
    clearAuthFeedback();
    const form = new FormData(formElement);
    const payload = Object.fromEntries(form.entries());
    const passwordConfirmation = payload.customerPasswordConfirm;
    delete payload.customerPasswordConfirm;
    payload.customerId = payload.customerId.trim();
    payload.customerName = payload.customerName.trim();

    if (!hasValidBcryptByteLength(payload.customerPassword, formElement.elements.customerPassword)) return;
    if (payload.customerPassword !== passwordConfirmation) {
      showFieldError(
        formElement,
        "customerPasswordConfirm",
        "입력한 비밀번호와 일치하지 않습니다.",
      );
      showAuthFeedback("비밀번호가 서로 다릅니다", "같은 비밀번호를 두 번 입력해 주세요.");
      formElement.elements.customerPasswordConfirm.focus();
      return;
    }

    invalidatePendingSessionRestore();

    let registered;

    try {
      registered = await withLoading("회원 계정과 포인트 지갑을 만들고 있습니다", () =>
        shopApi.register(payload),
      );
    } catch (error) {
      showAuthError(
        error,
        error?.status === 409
          ? "이미 사용 중인 고객 ID입니다."
          : "회원가입을 완료하지 못했습니다.",
        formElement,
        error?.status === 409 ? "customerId" : null,
      );
      return;
    }

    try {
      const login = await withLoading("가입한 계정으로 로그인하고 있습니다", () =>
        shopApi.login({
          customerId: payload.customerId,
          customerPassword: payload.customerPassword,
        }),
      );
      setSession({ ...login, name: registered.name });
      await loadCustomer({ quiet: false });
      await Promise.all([loadCart({ quiet: true }), loadAddresses({ quiet: true }), loadWishlist({ quiet: true }), loadStockAlerts({ quiet: true })]);
      closeDialog(elements.authDialog);
      formElement.reset();
      switchView("shop");
      showToast("가입을 완료했습니다", `${points(registered.customerPoint)}가 지급되었습니다.`);
    } catch (error) {
      const loginForm = $("#login-form");
      loginForm.elements.customerId.value = payload.customerId;
      loginForm.elements.customerPassword.value = "";
      formElement.reset();
      selectAuthTab("login");
      showAuthFeedback(
        "가입은 완료됐지만 자동 로그인에 실패했습니다",
        `${error?.message || "직접 로그인해 주세요."} · 로그인 화면에서 다시 시도해 주세요.`,
      );
    }
  });

  $("#password-reset-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formElement = event.currentTarget;
    clearFormFieldErrors(formElement);
    clearAuthFeedback();
    const form = new FormData(formElement);
    const payload = Object.fromEntries(form.entries());
    const passwordConfirmation = payload.newPasswordConfirm;
    delete payload.newPasswordConfirm;
    payload.customerId = payload.customerId.trim();
    payload.customerName = payload.customerName.trim();

    if (!hasValidBcryptByteLength(payload.newPassword, formElement.elements.newPassword)) return;
    if (payload.newPassword !== passwordConfirmation) {
      showFieldError(
        formElement,
        "newPasswordConfirm",
        "입력한 새 비밀번호와 일치하지 않습니다.",
      );
      showAuthFeedback("새 비밀번호가 서로 다릅니다", "같은 비밀번호를 두 번 입력해 주세요.");
      formElement.elements.newPasswordConfirm.focus();
      return;
    }

    invalidatePendingSessionRestore();

    try {
      await withLoading("비밀번호를 안전하게 변경하고 있습니다", () =>
        shopApi.resetPassword(payload),
      );
      const loginForm = $("#login-form");
      loginForm.elements.customerId.value = payload.customerId;
      loginForm.elements.customerPassword.value = "";
      formElement.reset();
      selectAuthTab("login");
      loginForm.elements.customerPassword.focus();
      showAuthFeedback(
        "비밀번호를 재설정했습니다",
        "새 비밀번호로 로그인해 주세요.",
        "success",
      );
    } catch (error) {
      showAuthError(
        error,
        error?.status === 400
          ? "고객 ID와 현재 등록된 이름을 확인해 주세요."
          : "비밀번호를 재설정하지 못했습니다.",
        formElement,
        error?.status === 400 ? "customerName" : null,
      );
    }
  });

  $("#profile-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = elements.profileNameInput.value.trim();
    try {
      await withLoading("프로필을 변경하고 있습니다", () => shopApi.updateMe(name));
      await loadCustomer({ quiet: false });
      showToast("이름을 변경했습니다", name);
    } catch (error) {
      showApiError(error, "이름 변경에 실패했습니다.");
    }
  });

  const orderForm = $("#order-form");
  orderForm.elements.quantity.addEventListener("input", () => {
    refreshOrderCommand();
    updateOrderTotal();
  });
  $$('[data-quantity-action]', orderForm).forEach((button) => {
    button.addEventListener("click", () => {
      const input = orderForm.elements.quantity;
      const next = Math.max(
        1,
        Number(input.value || 1) + (button.dataset.quantityAction === "plus" ? 1 : -1),
      );
      input.value = Math.min(Number(input.max || 1_000_000), next);
      refreshOrderCommand();
      updateOrderTotal();
    });
  });
  orderForm.elements.variantId.addEventListener("change", async () => {
    const selected = orderForm.elements.variantId.selectedOptions[0];
    if (!selected) return;
    try {
      const stock = await shopApi.stock(selected.value);
      const max = Math.max(1, Number(stock.maxOrderQuantity || 1));
      orderForm.elements.quantity.max = max;
      orderForm.elements.quantity.value = Math.min(Number(orderForm.elements.quantity.value || 1), max);
      orderForm.dataset.unitPrice = selected.dataset.price;
      $("#order-product-price").textContent = points(selected.dataset.price);
      $("#order-quantity-label").textContent = `주문 수량 · 최대 ${max}개`;
      refreshOrderCommand();
      updateOrderTotal();
    } catch (error) {
      showApiError(error, "옵션 재고를 확인하지 못했습니다.");
    }
  });
  orderForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const productId = orderForm.elements.productId.value;
    const variantId = orderForm.elements.variantId.value || null;
    const quantity = Number(orderForm.elements.quantity.value);
    const couponCode = orderForm.elements.couponCode.value.trim();
    const rawPointAmount = orderForm.elements.pointAmount.value.trim();
    const pointAmount = rawPointAmount === "" ? null : Number(rawPointAmount);
    const commandKey = elements.orderDialog.dataset.commandKey;
    try {
      const pendingOrder = await withLoading("주문과 결제를 준비하고 있습니다", () =>
        shopApi.order(productId, quantity, couponCode, pointAmount, commandKey, variantId),
      );
      const order = await withLoading("Fake PG 결제를 승인하고 있습니다", () =>
        completeFakePayment(pendingOrder, orderForm.elements.testCardNumber.value),
      );
      closeDialog(elements.orderDialog);
      await Promise.all([loadCustomer({ quiet: false }), loadProducts()]);
      switchView("orders");
      showToast("주문을 완료했습니다", `${order.orderNumber} · 잔액 ${points(order.remainingPoints)}`);
      refreshOrderCommand();
    } catch (error) {
      showApiError(error, "주문에 실패했습니다.");
      if (error?.status === 409 && error?.code === "INSUFFICIENT_STOCK") {
        await loadProducts();
      }
    }
  });

  const cancelForm = $("#cancel-form");
  cancelForm.elements.quantity.addEventListener("input", refreshCancelCommand);
  cancelForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const productId = cancelForm.elements.productId.value;
    const quantity = Number(cancelForm.elements.quantity.value);
    const commandKey = elements.cancelDialog.dataset.commandKey;
    try {
      const cancellation = await withLoading("취소 수량과 환급 포인트를 계산하고 있습니다", () =>
        shopApi.cancel(productId, quantity, commandKey),
      );
      closeDialog(elements.cancelDialog);
      await Promise.all([loadCustomer({ quiet: false }), loadOrders(), loadProducts()]);
      showToast("부분 취소를 완료했습니다", `${points(cancellation.refundAmount)}가 환급되었습니다.`);
      refreshCancelCommand();
    } catch (error) {
      showApiError(error, "부분 취소에 실패했습니다.");
    }
  });

  $("#product-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const productId = form.elements.productId.value;
    const productName = form.elements.productName.value.trim();
    const productPrice = form.elements.productPrice.value.trim();
    const initialQuantity = Number(form.elements.initialQuantity.value);
    const productPayload = {
      productName,
      productPrice,
      initialQuantity,
      categoryId: form.elements.categoryId.value,
      description: form.elements.description.value.trim() || null,
      imageUrl: form.elements.imageUrl.value.trim() || null,
    };
    try {
      await withLoading(productId ? "상품 정보를 변경하고 있습니다" : "새 상품을 등록하고 있습니다", () =>
        productId
          ? shopApi.updateProduct(productId, productPayload)
          : shopApi.createProduct(productPayload),
      );
      closeDialog(elements.productDialog);
      await loadProducts();
      showToast(productId ? "상품을 수정했습니다" : "상품을 등록했습니다", productName);
    } catch (error) {
      showApiError(error, "상품 저장에 실패했습니다.");
    }
  });

  const stockForm = $("#stock-form");
  stockForm.addEventListener("input", () => {
    clearStockFeedback();
    refreshStockCommand();
  });
  stockForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    clearStockFeedback();

    const form = event.currentTarget;
    const productId = form.elements.productId.value;
    const mode = form.elements.mode.value;
    const quantity = Number(form.elements.quantity.value);
    const reason = form.elements.reason.value.trim();
    const commandKey = elements.stockDialog.dataset.commandKey;
    const submitButton = $("#stock-submit-button");

    if (mode === "adjust" && quantity === 0) {
      showStockFeedback("조정 수량은 0이 아닌 값으로 입력해 주세요.");
      form.elements.quantity.focus();
      return;
    }

    submitButton.disabled = true;
    form.setAttribute("aria-busy", "true");
    try {
      if (mode === "initialize") {
        await shopApi.initializeStock(productId, quantity, commandKey);
      } else {
        await shopApi.adjustStock(productId, quantity, reason, commandKey);
      }
      closeDialog(elements.stockDialog);
      await loadProducts();
      showToast(
        mode === "initialize" ? "재고를 초기화했습니다" : "재고를 조정했습니다",
        mode === "initialize" ? `${quantity}개로 시작합니다.` : `${quantity > 0 ? "+" : ""}${quantity}개 반영`,
      );
      refreshStockCommand();
    } catch (error) {
      if (error?.status === 401) {
        showApiError(error);
        return;
      }
      const detail = fieldErrorDetail(error);
      showStockFeedback(
        [error?.message || "재고를 반영하지 못했습니다.", detail].filter(Boolean).join(" · "),
      );
    } finally {
      submitButton.disabled = false;
      form.removeAttribute("aria-busy");
    }
  });

  $("#address-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form).entries());
    const addressId = values.addressId;
    delete values.addressId;
    values.defaultAddress = form.elements.defaultAddress.checked;
    Object.keys(values).forEach((key) => {
      if (typeof values[key] === "string") values[key] = values[key].trim();
    });
    try {
      await withLoading(addressId ? "배송지를 수정하고 있습니다" : "배송지를 저장하고 있습니다", () =>
        addressId ? shopApi.updateAddress(addressId, values) : shopApi.createAddress(values),
      );
      closeDialog(elements.addressDialog);
      await loadAddresses();
      showToast(addressId ? "배송지를 수정했습니다" : "배송지를 저장했습니다", values.addressName);
    } catch (error) {
      showApiError(error, "배송지를 저장하지 못했습니다.");
    }
  });

  elements.checkoutForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const address = state.addresses.find(
      (candidate) => candidate.id === elements.checkoutForm.elements.addressId.value,
    );
    if (!address) {
      showToast("배송지를 선택해 주세요", "저장 배송지가 필요합니다.", "error");
      return;
    }
    const items = state.cart.items.map((item) => ({
      productId: item.productId,
      variantId: item.variantId,
      quantity: Number(item.quantity),
    }));
    const shippingAddress = {
      recipientName: address.recipientName,
      phoneNumber: address.phoneNumber,
      postalCode: address.postalCode,
      addressLine1: address.addressLine1,
      addressLine2: address.addressLine2 || "",
    };
    const commandKey = elements.checkoutDialog.dataset.commandKey;
    try {
      const order = await withLoading("장바구니 상품을 주문하고 있습니다", () =>
        shopApi.createOrder(
          items,
          shippingAddress,
          elements.checkoutForm.elements.couponCode.value.trim(),
          elements.checkoutForm.elements.pointAmount.value.trim() === ""
            ? null : Number(elements.checkoutForm.elements.pointAmount.value),
          commandKey,
        ),
      );
      await withLoading("Fake PG 결제를 승인하고 있습니다", () =>
        completeFakePayment(order, elements.checkoutForm.elements.testCardNumber.value),
      );
      try {
        state.cart = await shopApi.clearCart();
      } catch {
        await loadCart({ quiet: true });
      }
      closeDialog(elements.checkoutDialog);
      closeDialog(elements.cartDialog);
      renderCart();
      await Promise.all([loadCustomer({ quiet: false }), loadOrders(), loadProducts()]);
      switchView("orders");
      showToast("장바구니 주문을 완료했습니다", `${order.orderNumber} · 잔액 ${points(order.remainingPoints)}`);
      elements.checkoutDialog.dataset.commandKey = createCommandId();
    } catch (error) {
      showApiError(error, "장바구니 주문에 실패했습니다.");
      if (error?.status === 409) await Promise.all([loadCart({ quiet: true }), loadProducts()]);
    }
  });

  elements.reviewForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const productId = elements.reviewDialog.dataset.productId;
    try {
      await withLoading("리뷰를 저장하고 있습니다", () => shopApi.writeReview(
        productId,
        Number(elements.reviewForm.elements.rating.value),
        elements.reviewForm.elements.comment.value.trim(),
      ));
      await openReviews(productById(productId));
      showToast("리뷰를 저장했습니다");
    } catch (error) {
      showApiError(error, error?.status === 403 ? "구매한 상품만 리뷰를 작성할 수 있습니다." : "리뷰를 저장하지 못했습니다.");
    }
  });
  $("#delete-review-button").addEventListener("click", async () => {
    const productId = elements.reviewDialog.dataset.productId;
    if (!window.confirm("작성한 리뷰를 삭제할까요?")) return;
    try {
      await shopApi.deleteReview(productId);
      await openReviews(productById(productId));
      showToast("리뷰를 삭제했습니다");
    } catch (error) {
      showApiError(error, "리뷰를 삭제하지 못했습니다.");
    }
  });

}

function restoreRememberedCustomerId() {
  const rememberedId = window.localStorage.getItem(REMEMBERED_CUSTOMER_ID_KEY) || "";
  const form = $("#login-form");
  form.elements.customerId.value = rememberedId;
  form.elements.rememberCustomerId.checked = Boolean(rememberedId);
}

function bindAccountActions() {
  const logout = async () => {
    invalidatePendingSessionRestore();
    try {
      await withLoading("로그아웃하고 있습니다", () => shopApi.logout());
      clearSession();
      switchView("shop");
      showToast("로그아웃되었습니다");
    } catch (error) {
      showApiError(error, "로그아웃에 실패했습니다.");
    }
  };

  $("#logout-button").addEventListener("click", logout);
  $("#admin-logout-button").addEventListener("click", logout);

  $("#deactivate-button").addEventListener("click", async () => {
    if (!window.confirm("회원 탈퇴 후에는 기존 JWT도 즉시 사용할 수 없습니다. 정말 탈퇴할까요?")) {
      return;
    }
    invalidatePendingSessionRestore();
    try {
      await withLoading("계정을 안전하게 비활성화하고 있습니다", () => shopApi.deactivateMe());
      clearSession();
      switchView("shop");
      showToast("회원 탈퇴가 완료되었습니다");
    } catch (error) {
      showApiError(error, "회원 탈퇴에 실패했습니다.");
    }
  });

  elements.orderList.addEventListener("click", (event) => {
    const returnButton = event.target.closest("[data-return-order]");
    if (returnButton) {
      const form = $("#return-form");
      form.reset();
      form.elements.orderId.value = returnButton.dataset.returnOrder;
      form.elements.orderItemId.value = returnButton.dataset.returnItem;
      form.elements.quantity.value = 1;
      form.elements.quantity.max = returnButton.dataset.returnMax;
      $("#return-product-name").textContent = `${returnButton.dataset.returnName} 반품`;
      elements.returnDialog.dataset.commandKey = createCommandId();
      openDialog(elements.returnDialog);
      return;
    }
    const button = event.target.closest("[data-product-cancel]");
    if (!button) return;
    openCancel({
      productId: button.dataset.productCancel,
      productName: button.dataset.productName,
      maxQuantity: button.dataset.maxQuantity,
    });
  });

  $("#return-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const payload = {
      orderId: form.elements.orderId.value,
      orderItemId: form.elements.orderItemId.value,
      quantity: Number(form.elements.quantity.value),
      reason: form.elements.reason.value,
      evidenceImageUrl: form.elements.evidenceImageUrl.value.trim() || null,
    };
    try {
      await withLoading("반품 요청을 접수하고 있습니다", () =>
        shopApi.requestReturn(payload, elements.returnDialog.dataset.commandKey));
      closeDialog(elements.returnDialog);
      await loadReturns();
      showToast("반품 신청을 접수했습니다", "관리자 회수·검수 후 환불됩니다.");
    } catch (error) { showApiError(error, "반품 신청에 실패했습니다."); }
  });

  elements.orderLoadMore.addEventListener("click", () =>
    loadOrders({ append: true }),
  );
  $("#refresh-members-button").addEventListener("click", () => loadMembers());
  elements.memberLoadMore.addEventListener("click", () =>
    loadMembers({ append: true }),
  );
  $("#refresh-wallet-button").addEventListener("click", () => loadTransactions());
  $("#refresh-wishlist-button").addEventListener("click", () => loadWishlist());
  $("#refresh-stock-alerts-button").addEventListener("click", () => loadStockAlerts());
  elements.wishlistList.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-wishlist-remove]");
    if (!button) return;
    try {
      await shopApi.removeWishlist(button.dataset.wishlistRemove);
      await loadWishlist({ quiet: true });
      showToast("관심 상품에서 삭제했습니다");
    } catch (error) {
      showApiError(error, "관심 상품을 삭제하지 못했습니다.");
    }
  });
  elements.stockAlertList.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-stock-alert-remove]");
    if (!button) return;
    try {
      await shopApi.unsubscribeStockAlert(button.dataset.stockAlertRemove);
      await loadStockAlerts({ quiet: true });
      showToast("재입고 알림을 해지했습니다");
    } catch (error) {
      showApiError(error, "재입고 알림을 해지하지 못했습니다.");
    }
  });
  elements.transactionLoadMore.addEventListener("click", () =>
    loadTransactions({ append: true }),
  );
  $("#refresh-admin-orders-button").addEventListener("click", () => loadAdminOrders());
  $("#refresh-admin-returns-button").addEventListener("click", () => loadAdminReturns());
  elements.adminOrderLoadMore.addEventListener("click", () =>
    loadAdminOrders({ append: true }),
  );
  elements.adminOrderList.addEventListener("click", async (event) => {
    const fulfillmentButton = event.target.closest("[data-fulfillment-order]");
    const historyButton = event.target.closest("[data-order-history]");
    if (fulfillmentButton) {
      const nextStatus = fulfillmentButton.dataset.fulfillmentStatus;
      if (!window.confirm(`배송 상태를 '${fulfillmentStatus(nextStatus)}'(으)로 변경할까요?`)) return;
      const fulfillment = { status: nextStatus };
      if (nextStatus === "SHIPPED") {
        fulfillment.trackingCarrier = window.prompt("택배사를 입력해 주세요.", "")?.trim() || "";
        fulfillment.trackingNumber = window.prompt("운송장 번호를 입력해 주세요.", "")?.trim() || "";
        const trackingUrl = window.prompt("HTTPS 배송 조회 URL을 입력해 주세요. (선택)", "")?.trim();
        if (trackingUrl) fulfillment.trackingUrl = trackingUrl;
      }
      try {
        await withLoading("배송 상태를 변경하고 있습니다", () =>
          shopApi.updateFulfillment(fulfillmentButton.dataset.fulfillmentOrder, fulfillment),
        );
        await loadAdminOrders();
        showToast("배송 상태를 변경했습니다", fulfillmentStatus(nextStatus));
      } catch (error) {
        showApiError(error, "배송 상태를 변경하지 못했습니다.");
      }
    }
    if (historyButton) {
      const orderId = historyButton.dataset.orderHistory;
      const panel = elements.adminOrderList.querySelector(`[data-order-history-panel="${orderId}"]`);
      if (!panel) return;
      if (!panel.classList.contains("is-hidden")) {
        panel.classList.add("is-hidden");
        return;
      }
      panel.classList.remove("is-hidden");
      panel.textContent = "변경 이력을 불러오는 중입니다.";
      try {
        const history = await shopApi.orderHistory(orderId);
        panel.innerHTML = history.length
          ? history.map((item) => `<div><span>${escapeHtml(fulfillmentStatus(item.fromStatus))} → ${escapeHtml(fulfillmentStatus(item.toStatus))}</span><small>${dateTime(item.changedAt)}</small></div>`).join("")
          : '<div>아직 배송 상태 변경 이력이 없습니다.</div>';
      } catch (error) {
        panel.textContent = error?.message || "변경 이력을 불러오지 못했습니다.";
      }
    }
  });
  elements.adminReturnList.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-return-transition]");
    if (!button) return;
    const status = button.dataset.returnStatus;
    if (!window.confirm(`반품 상태를 '${returnStatusLabel(status)}'(으)로 변경할까요?`)) return;
    try {
      await withLoading("반품 상태와 환불을 처리하고 있습니다", () =>
        shopApi.updateReturnStatus(button.dataset.returnTransition, status, "관리자 처리"));
      await Promise.all([loadAdminReturns(), loadAdminOrders()]);
      showToast("반품 상태를 변경했습니다", returnStatusLabel(status));
    } catch (error) { showApiError(error, "반품 상태를 변경하지 못했습니다."); }
  });
}

function bindShoppingActions() {
  elements.cartButton.addEventListener("click", async () => {
    await loadCart();
    openDialog(elements.cartDialog);
  });
  $("#clear-cart-button").addEventListener("click", async () => {
    if (!state.cart.items?.length || !window.confirm("장바구니 상품을 모두 삭제할까요?")) return;
    try {
      state.cart = await withLoading("장바구니를 비우고 있습니다", () => shopApi.clearCart());
      renderCart();
      showToast("장바구니를 비웠습니다");
    } catch (error) {
      showApiError(error, "장바구니를 비우지 못했습니다.");
    }
  });
  $("#checkout-button").addEventListener("click", openCheckout);
  elements.checkoutForm.elements.addressId.addEventListener("change", updateCheckoutAddressPreview);

  elements.cartList.addEventListener("change", async (event) => {
    const input = event.target.closest("[data-cart-quantity]");
    if (!input) return;
    const quantity = Number(input.value);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > Number(input.max)) {
      showToast("수량을 확인해 주세요", `1~${input.max}개 사이로 입력해 주세요.`, "error");
      renderCart();
      return;
    }
    try {
      state.cart = await shopApi.updateCartItem(input.dataset.cartQuantity, quantity);
      renderCart();
    } catch (error) {
      showApiError(error, "장바구니 수량을 변경하지 못했습니다.");
      await loadCart({ quiet: true });
    }
  });
  elements.cartList.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-cart-remove]");
    if (!button) return;
    try {
      state.cart = await shopApi.removeCartItem(button.dataset.cartRemove);
      renderCart();
    } catch (error) {
      showApiError(error, "장바구니 상품을 삭제하지 못했습니다.");
    }
  });

  $("#add-address-button").addEventListener("click", () => openAddressEditor());
  elements.addressList.addEventListener("click", async (event) => {
    const edit = event.target.closest("[data-address-edit]");
    const remove = event.target.closest("[data-address-delete]");
    if (edit) openAddressEditor(state.addresses.find((address) => address.id === edit.dataset.addressEdit));
    if (remove) {
      const address = state.addresses.find((candidate) => candidate.id === remove.dataset.addressDelete);
      if (!address || !window.confirm(`'${address.addressName}' 배송지를 삭제할까요?`)) return;
      try {
        await shopApi.deleteAddress(address.id);
        await loadAddresses();
        showToast("배송지를 삭제했습니다", address.addressName);
      } catch (error) {
        showApiError(error, "배송지를 삭제하지 못했습니다.");
      }
    }
  });
}

function bindConnectionStatus() {
  let wasOffline = !navigator.onLine;
  const update = () => {
    const offline = !navigator.onLine;
    elements.connectionBanner.classList.toggle("is-hidden", !offline);
    if (!offline && wasOffline) {
      showToast("인터넷 연결이 복구되었습니다", "진행 중이던 요청을 다시 시도해 주세요.");
    }
    wasOffline = offline;
  };
  window.addEventListener("online", update);
  window.addEventListener("offline", update);
  update();
}

function bindSessionLifecycle() {
  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState !== "visible" || !state.session) return;
    if (Date.now() - lastSessionCheckAt < 60_000) return;
    lastSessionCheckAt = Date.now();
    if (isCustomer()) {
      loadCustomer({ quiet: false });
    } else if (isAdmin()) {
      loadMembers();
    }
  });
}

async function initialize() {
  bindNavigation();
  bindDialogs();
  bindProducts();
  bindForms();
  bindAccountActions();
  bindShoppingActions();
  bindConnectionStatus();
  bindSessionLifecycle();
  restoreRememberedCustomerId();
  renderSession();
  renderOrders();
  renderMembers();
  renderCart();
  renderAddresses();
  renderWishlist();
  renderStockAlerts();
  renderTransactions();
  renderAdminOrders();

  await Promise.allSettled([
    issueCsrfToken(),
    loadCategories(),
    loadProducts(),
    restoreSession(),
  ]);

  const requestedView = window.location.hash.replace("#", "");
  switchView(["shop", "orders", "account", "admin"].includes(requestedView) ? requestedView : "shop");
}

initialize().catch((error) => {
  console.error(error);
  showToast("화면 초기화에 실패했습니다", error.message, "error");
});
