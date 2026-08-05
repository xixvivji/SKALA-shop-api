import {
  createCommandId,
  issueCsrfToken,
  shopApi,
} from "./api.js";

const REMEMBERED_CUSTOMER_ID_KEY = "skala-remembered-customer-id";

const state = {
  view: "shop",
  session: null,
  customer: null,
  products: [],
  productTotal: 0,
  productsLoading: true,
  productsError: null,
  orders: [],
  ordersLoading: false,
  members: [],
  memberTotal: 0,
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
  orderDialog: $("#order-dialog"),
  cancelDialog: $("#cancel-dialog"),
  productDialog: $("#product-dialog"),
  ordersLoginGate: $("#orders-login-gate"),
  ordersContent: $("#orders-content"),
  orderList: $("#order-list"),
  orderCount: $("#order-count"),
  accountLoginGate: $("#account-login-gate"),
  accountContent: $("#account-content"),
  memberRole: $("#member-role"),
  memberName: $("#member-name"),
  memberId: $("#member-id"),
  memberBalance: $("#member-balance"),
  profileNameInput: $("#profile-name-input"),
  purchasedList: $("#purchased-list"),
  memberTableBody: $("#member-table-body"),
  adminProductCount: $("#admin-product-count"),
  adminMemberCount: $("#admin-member-count"),
  toastRegion: $("#toast-region"),
  loadingOverlay: $("#loading-overlay"),
  loadingMessage: $("#loading-message"),
};

let loadingDepth = 0;

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
    clearSession();
    showToast("로그인이 만료되었습니다", "다시 로그인해 주세요.", "error");
    return;
  }

  const detail = fieldErrorDetail(error);
  showToast(error?.message || fallback, detail || error?.code || "", "error");
}

async function withLoading(message, task) {
  loadingDepth += 1;
  elements.loadingMessage.textContent = message;
  elements.loadingOverlay.classList.remove("is-hidden");
  try {
    return await task();
  } finally {
    loadingDepth -= 1;
    if (loadingDepth === 0) {
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
  state.members = [];
  state.memberTotal = 0;
  window.localStorage.removeItem("skala-session-hint");
  renderSession();
  renderOrders();
  renderMembers();
}

function setSession(session, customer = null) {
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
  elements.memberBalance.textContent = points(customer.customerPoint);
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

function renderProducts() {
  if (state.productsLoading) {
    elements.productGrid.innerHTML = productSkeletons();
    return;
  }

  if (state.productsError) {
    elements.productGrid.innerHTML = `
      <div class="error-state">
        <span aria-hidden="true">!</span>
        <h3>상품을 불러오지 못했어요</h3>
        <p>${escapeHtml(state.productsError.message)}</p>
      </div>
    `;
    elements.productCount.textContent = "0";
    return;
  }

  const query = elements.productSearch.value.trim().toLocaleLowerCase("ko-KR");
  const products = state.products.filter((product) =>
    product.name.toLocaleLowerCase("ko-KR").includes(query),
  );
  elements.productCount.textContent = String(state.productTotal);
  elements.adminProductCount.textContent = String(state.productTotal);

  if (!products.length) {
    elements.productGrid.innerHTML = `
      <div class="empty-state">
        <span aria-hidden="true">◇</span>
        <h3>${query ? "검색 결과가 없습니다" : "등록된 상품이 없습니다"}</h3>
        <p>${query ? "다른 검색어로 다시 찾아보세요." : "관리자 계정으로 로그인하면 첫 상품을 등록할 수 있습니다."}</p>
      </div>
    `;
    return;
  }

  elements.productGrid.innerHTML = products
    .map((product, index) => {
      const tone = toneFor(product.id);
      const controls = isAdmin()
        ? `
          <div class="admin-card-actions">
            <button class="mini-button" type="button" data-product-edit="${escapeHtml(product.id)}">수정</button>
            <button class="mini-button danger" type="button" data-product-delete="${escapeHtml(product.id)}">삭제</button>
          </div>
        `
        : `
          <button class="buy-button" type="button" data-product-buy="${escapeHtml(product.id)}" aria-label="${escapeHtml(product.name)} 주문하기">→</button>
        `;
      return `
        <article class="product-card">
          <div class="product-visual tone-${tone}">
            <span>${escapeHtml(initials(product.name))}</span>
            <small class="product-badge">${index < 2 ? "NEW DROP" : "ACTIVE"}</small>
          </div>
          <div class="product-body">
            <small>SKALA SELECT · ${String(index + 1).padStart(2, "0")}</small>
            <h3 title="${escapeHtml(product.name)}">${escapeHtml(product.name)}</h3>
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
    PAID: "결제 완료",
    PARTIALLY_CANCELED: "부분 취소",
    CANCELED: "취소 완료",
  };
  return labels[status] || status;
}

function renderOrders() {
  elements.orderCount.textContent = String(state.orders.length);
  if (!isCustomer()) {
    elements.orderList.innerHTML = "";
    return;
  }

  if (state.ordersLoading) {
    elements.orderList.innerHTML = productSkeletons();
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
                <strong>${money(Number(item.unitPrice) * Number(item.orderedQuantity))} P</strong>
                ${
                  available > 0
                    ? `<button class="mini-button" type="button" data-product-cancel="${escapeHtml(item.productId)}" data-product-name="${escapeHtml(item.productName)}" data-max-quantity="${available}">부분 취소</button>`
                    : ""
                }
              </span>
            </div>
          `;
        })
        .join("");
      return `
        <article class="order-card">
          <header class="order-card-head">
            <span class="order-number"><small>${dateTime(order.orderedAt)}</small><strong>${escapeHtml(order.orderNumber)}</strong></span>
            <span class="order-status ${String(order.status).toLowerCase().replaceAll("_", "-")}">${escapeHtml(orderStatus(order.status))}</span>
          </header>
          <div>${items}</div>
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

function renderMembers() {
  elements.adminMemberCount.textContent = String(state.memberTotal);
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
          <td class="id-cell" title="${escapeHtml(member.id)}">${escapeHtml(member.id)}</td>
        </tr>
      `,
    )
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
    button.classList.toggle("is-active", button.dataset.view === view);
  });

  if (view === "orders" && isCustomer()) loadOrders();
  if (view === "account" && isCustomer()) loadCustomer();
  if (view === "admin" && isAdmin()) loadMembers();

  if (updateHash) {
    window.history.replaceState(null, "", `#${view}`);
  }
  window.scrollTo({ top: 0, behavior: "smooth" });
}

async function loadEveryPage(loader, size) {
  const first = await loader(0, size);
  const content = [...(first.content || [])];
  const totalPages = Number(first.totalPages || 0);

  for (let page = 1; page < totalPages; page += 1) {
    const next = await loader(page, size);
    content.push(...(next.content || []));
  }

  return {
    content,
    totalElements: Number(first.totalElements ?? content.length),
  };
}

async function loadProducts() {
  state.productsLoading = true;
  state.productsError = null;
  renderProducts();
  try {
    const page = await loadEveryPage(shopApi.products, 100);
    state.products = page.content || [];
    state.productTotal = page.totalElements;
  } catch (error) {
    state.productsError = error;
  } finally {
    state.productsLoading = false;
    renderProducts();
  }
}

async function loadCustomer({ quiet = true } = {}) {
  if (!isCustomer()) return;
  try {
    const customer = await shopApi.me();
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
    if (!quiet || error.status === 401) showApiError(error);
  }
}

async function loadOrders() {
  if (!isCustomer() || state.ordersLoading) return;
  state.ordersLoading = true;
  renderOrders();
  try {
    state.orders = await shopApi.orders();
  } catch (error) {
    showApiError(error, "주문 목록을 불러오지 못했습니다.");
  } finally {
    state.ordersLoading = false;
    renderOrders();
  }
}

async function loadMembers() {
  if (!isAdmin()) return;
  try {
    const page = await loadEveryPage(shopApi.members, 50);
    state.members = page.content || [];
    state.memberTotal = page.totalElements;
    renderMembers();
  } catch (error) {
    showApiError(error, "고객 목록을 불러오지 못했습니다.");
  }
}

async function restoreSession() {
  try {
    const customer = await shopApi.me();
    setSession(
      {
        memberId: customer.memberId,
        customerId: customer.customerId,
        name: customer.name,
        role: customer.role,
      },
      customer,
    );
    return;
  } catch (error) {
    if (![401, 403].includes(error.status)) return;
  }

  const hint = sessionHint();
  if (hint?.role !== "ADMIN") {
    clearSession();
    return;
  }

  try {
    const page = await shopApi.members(0, 50);
    state.members = page.content || [];
    state.memberTotal = Number(page.totalElements || state.members.length);
    setSession({ ...hint, name: hint.customerId || "관리자" });
    renderMembers();
  } catch {
    clearSession();
  }
}

function openDialog(dialog) {
  if (!dialog.open) dialog.showModal();
}

function closeDialog(dialog) {
  if (dialog?.open) dialog.close();
}

function selectAuthTab(tab) {
  clearAuthFeedback();
  $$('.auth-form', elements.authDialog).forEach(clearFormFieldErrors);
  $$('[data-auth-tab]', elements.authDialog).forEach((button) => {
    button.classList.toggle("is-active", button.dataset.authTab === tab);
    button.setAttribute("aria-selected", String(button.dataset.authTab === tab));
  });
  $$('[data-auth-panel]', elements.authDialog).forEach((panel) => {
    panel.classList.toggle("is-hidden", panel.dataset.authPanel !== tab);
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

function openOrder(product) {
  if (!isCustomer()) {
    if (isAdmin()) {
      showToast("관리자는 주문할 수 없습니다", "CUSTOMER 계정으로 로그인해 주세요.", "error");
    } else {
      openAuth("login");
    }
    return;
  }

  const form = $("#order-form");
  form.elements.productId.value = product.id;
  form.elements.quantity.value = 1;
  form.dataset.unitPrice = product.price;
  $("#order-product-name").textContent = product.name;
  $("#order-product-price").textContent = points(product.price);
  $("#order-product-visual span").textContent = initials(product.name);
  $("#order-product-visual").className = `modal-product-visual tone-${toneFor(product.id)}`;
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
  $("#product-dialog-title").textContent = product ? "상품 정보 수정" : "새 상품 등록";
  $("#product-submit-button").textContent = product ? "수정 내용 저장" : "상품 등록";
  openDialog(elements.productDialog);
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
  });

  $$('[data-auth-tab]', elements.authDialog).forEach((button) => {
    button.addEventListener("click", () => selectAuthTab(button.dataset.authTab));
  });

  $("[data-forgot-password]").addEventListener("click", () => {
    const loginId = $("#login-form").elements.customerId.value.trim();
    $("#password-reset-form").elements.customerId.value = loginId;
    selectAuthTab("reset");
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
  elements.productSearch.addEventListener("input", renderProducts);
  $("#refresh-products-button").addEventListener("click", loadProducts);
  elements.addProductButton.addEventListener("click", () => openProductEditor());
  elements.adminAddProductButton.addEventListener("click", () => openProductEditor());

  elements.productGrid.addEventListener("click", async (event) => {
    const buy = event.target.closest("[data-product-buy]");
    const edit = event.target.closest("[data-product-edit]");
    const remove = event.target.closest("[data-product-delete]");

    if (buy) openOrder(productById(buy.dataset.productBuy));
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
    clearFormFieldErrors(event.currentTarget);
    clearAuthFeedback();
    const form = new FormData(event.currentTarget);
    const payload = Object.fromEntries(form.entries());
    const rememberCustomerId = Boolean(payload.rememberCustomerId);
    delete payload.rememberCustomerId;
    payload.customerId = payload.customerId.trim();
    if (!hasValidBcryptByteLength(payload.customerPassword, event.currentTarget.elements.customerPassword)) return;
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
        switchView("shop");
      } else {
        switchView("admin");
      }
      closeDialog(elements.authDialog);
      event.currentTarget.reset();
      showToast("로그인되었습니다", `${login.customerId} · ${login.role}`);
    } catch (error) {
      showAuthError(
        error,
        "아이디 또는 비밀번호가 올바르지 않습니다.",
        event.currentTarget,
        "customerPassword",
      );
    }
  });

  $("#signup-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormFieldErrors(event.currentTarget);
    clearAuthFeedback();
    const form = new FormData(event.currentTarget);
    const payload = Object.fromEntries(form.entries());
    const passwordConfirmation = payload.customerPasswordConfirm;
    delete payload.customerPasswordConfirm;
    payload.customerId = payload.customerId.trim();
    payload.customerName = payload.customerName.trim();

    if (!hasValidBcryptByteLength(payload.customerPassword, event.currentTarget.elements.customerPassword)) return;
    if (payload.customerPassword !== passwordConfirmation) {
      showFieldError(
        event.currentTarget,
        "customerPasswordConfirm",
        "입력한 비밀번호와 일치하지 않습니다.",
      );
      showAuthFeedback("비밀번호가 서로 다릅니다", "같은 비밀번호를 두 번 입력해 주세요.");
      event.currentTarget.elements.customerPasswordConfirm.focus();
      return;
    }

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
        event.currentTarget,
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
      closeDialog(elements.authDialog);
      event.currentTarget.reset();
      switchView("shop");
      showToast("가입을 완료했습니다", `${points(registered.customerPoint)}가 지급되었습니다.`);
    } catch (error) {
      const loginForm = $("#login-form");
      loginForm.elements.customerId.value = payload.customerId;
      loginForm.elements.customerPassword.value = "";
      event.currentTarget.reset();
      selectAuthTab("login");
      showAuthFeedback(
        "가입은 완료됐지만 자동 로그인에 실패했습니다",
        `${error?.message || "직접 로그인해 주세요."} · 로그인 화면에서 다시 시도해 주세요.`,
      );
    }
  });

  $("#password-reset-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    clearFormFieldErrors(event.currentTarget);
    clearAuthFeedback();
    const form = new FormData(event.currentTarget);
    const payload = Object.fromEntries(form.entries());
    const passwordConfirmation = payload.newPasswordConfirm;
    delete payload.newPasswordConfirm;
    payload.customerId = payload.customerId.trim();
    payload.customerName = payload.customerName.trim();

    if (!hasValidBcryptByteLength(payload.newPassword, event.currentTarget.elements.newPassword)) return;
    if (payload.newPassword !== passwordConfirmation) {
      showFieldError(
        event.currentTarget,
        "newPasswordConfirm",
        "입력한 새 비밀번호와 일치하지 않습니다.",
      );
      showAuthFeedback("새 비밀번호가 서로 다릅니다", "같은 비밀번호를 두 번 입력해 주세요.");
      event.currentTarget.elements.newPasswordConfirm.focus();
      return;
    }

    try {
      await withLoading("비밀번호를 안전하게 변경하고 있습니다", () =>
        shopApi.resetPassword(payload),
      );
      const loginForm = $("#login-form");
      loginForm.elements.customerId.value = payload.customerId;
      loginForm.elements.customerPassword.value = "";
      event.currentTarget.reset();
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
        event.currentTarget,
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
      input.value = Math.min(99, next);
      refreshOrderCommand();
      updateOrderTotal();
    });
  });
  orderForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const productId = orderForm.elements.productId.value;
    const quantity = Number(orderForm.elements.quantity.value);
    const commandKey = elements.orderDialog.dataset.commandKey;
    try {
      const order = await withLoading("포인트를 확인하고 주문을 처리하고 있습니다", () =>
        shopApi.order(productId, quantity, commandKey),
      );
      closeDialog(elements.orderDialog);
      await Promise.all([loadCustomer({ quiet: false }), loadOrders()]);
      switchView("orders");
      showToast("주문을 완료했습니다", `${order.orderNumber} · 잔액 ${points(order.remainingPoints)}`);
      refreshOrderCommand();
    } catch (error) {
      showApiError(error, "주문에 실패했습니다.");
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
      await Promise.all([loadCustomer({ quiet: false }), loadOrders()]);
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
    const productPrice = Number(form.elements.productPrice.value);
    try {
      await withLoading(productId ? "상품 정보를 변경하고 있습니다" : "새 상품을 등록하고 있습니다", () =>
        productId
          ? shopApi.updateProduct(productId, productName, productPrice)
          : shopApi.createProduct(productName, productPrice),
      );
      closeDialog(elements.productDialog);
      await loadProducts();
      showToast(productId ? "상품을 수정했습니다" : "상품을 등록했습니다", productName);
    } catch (error) {
      showApiError(error, "상품 저장에 실패했습니다.");
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
    const button = event.target.closest("[data-product-cancel]");
    if (!button) return;
    openCancel({
      productId: button.dataset.productCancel,
      productName: button.dataset.productName,
      maxQuantity: button.dataset.maxQuantity,
    });
  });

  $("#refresh-members-button").addEventListener("click", loadMembers);
}

async function initialize() {
  bindNavigation();
  bindDialogs();
  bindProducts();
  bindForms();
  bindAccountActions();
  restoreRememberedCustomerId();
  renderSession();
  renderOrders();
  renderMembers();

  await Promise.allSettled([
    issueCsrfToken(),
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
