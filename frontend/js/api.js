const configuredBaseUrl =
  window.SKALA_CONFIG?.API_BASE_URL || window.location.origin;

export const API_BASE_URL = configuredBaseUrl.replace(/\/+$/, "");

const activityListeners = new Set();
let csrfToken = null;
let csrfRequest = null;

export class ApiError extends Error {
  constructor({ status = 0, code = "UNKNOWN_ERROR", message, fieldErrors = {} }) {
    super(message || "요청을 처리하지 못했습니다.");
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export function subscribeApiActivity(listener) {
  activityListeners.add(listener);
  return () => activityListeners.delete(listener);
}

function publishActivity(activity) {
  activityListeners.forEach((listener) => listener(activity));
}

function isMutation(method) {
  return !["GET", "HEAD", "OPTIONS"].includes(method);
}

async function readBody(response) {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }

  const text = await response.text();
  return text || null;
}

function errorFromResponse(response, payload) {
  return new ApiError({
    status: response.status,
    code: payload?.code || `HTTP_${response.status}`,
    message: payload?.message || `요청이 실패했습니다. (${response.status})`,
    fieldErrors: payload?.fieldErrors || {},
  });
}

export async function issueCsrfToken(force = false) {
  if (csrfToken && !force) {
    return csrfToken;
  }

  if (csrfRequest && !force) {
    return csrfRequest;
  }

  csrfRequest = fetch(`${API_BASE_URL}/api/auth/csrf`, {
    method: "GET",
    credentials: "include",
    headers: { Accept: "application/json" },
  })
    .then(async (response) => {
      const payload = await readBody(response);
      if (!response.ok) {
        throw errorFromResponse(response, payload);
      }
      csrfToken = payload;
      return payload;
    })
    .catch((error) => {
      csrfToken = null;
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError({
        code: "NETWORK_ERROR",
        message: "백엔드에 연결할 수 없습니다. API 주소와 서버 상태를 확인해 주세요.",
      });
    })
    .finally(() => {
      csrfRequest = null;
    });

  return csrfRequest;
}

async function request(
  path,
  {
    method = "GET",
    body,
    headers = {},
    idempotencyKey,
    retryCsrf = true,
  } = {},
) {
  const normalizedMethod = method.toUpperCase();
  const requestHeaders = {
    Accept: "application/json",
    ...headers,
  };

  if (body !== undefined) {
    requestHeaders["Content-Type"] = "application/json";
  }

  if (idempotencyKey) {
    requestHeaders["X-Idempotency-Key"] = idempotencyKey;
  }

  if (isMutation(normalizedMethod)) {
    const csrf = await issueCsrfToken();
    requestHeaders[csrf.headerName || "X-XSRF-TOKEN"] = csrf.token;
  }

  const startedAt = performance.now();
  let response;
  let payload;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: normalizedMethod,
      credentials: "include",
      headers: requestHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    payload = await readBody(response);
  } catch (error) {
    publishActivity({
      method: normalizedMethod,
      path,
      status: "ERR",
      duration: Math.round(performance.now() - startedAt),
      at: new Date(),
    });
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError({
      code: "NETWORK_ERROR",
      message: "백엔드에 연결할 수 없습니다. API 주소와 CORS 설정을 확인해 주세요.",
    });
  }

  publishActivity({
    method: normalizedMethod,
    path,
    status: response.status,
    duration: Math.round(performance.now() - startedAt),
    at: new Date(),
  });

  if (!response.ok) {
    if (response.status === 403 && isMutation(normalizedMethod) && retryCsrf) {
      await issueCsrfToken(true);
      return request(path, {
        method: normalizedMethod,
        body,
        headers,
        idempotencyKey,
        retryCsrf: false,
      });
    }
    throw errorFromResponse(response, payload);
  }

  return payload;
}

export function createCommandId() {
  if (window.crypto?.randomUUID) {
    return window.crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (character) => {
    const random = Math.floor(Math.random() * 16);
    const value = character === "x" ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

export const shopApi = {
  health: () => request("/actuator/health"),
  products: ({ page = 0, size = 100, query, categoryId, minPrice, maxPrice } = {}) => {
    const parameters = new URLSearchParams({ page, size });
    if (query) parameters.set("query", query);
    if (categoryId) parameters.set("categoryId", categoryId);
    if (minPrice !== undefined && minPrice !== "") parameters.set("minPrice", minPrice);
    if (maxPrice !== undefined && maxPrice !== "") parameters.set("maxPrice", maxPrice);
    return request(`/api/products?${parameters.toString()}`);
  },
  categories: () => request("/api/categories"),
  register: (payload) => request("/api/customers", { method: "POST", body: payload }),
  resetPassword: (payload) =>
    request("/api/customers/password/reset", { method: "POST", body: payload }),
  login: (payload) =>
    request("/api/customers/login", { method: "POST", body: payload }),
  logout: () => request("/api/customers/logout", { method: "POST" }),
  me: () => request("/api/customers/me"),
  updateMe: (name) =>
    request("/api/customers/me", { method: "PUT", body: { name } }),
  deactivateMe: () => request("/api/customers/me", { method: "DELETE" }),
  addresses: () => request("/api/customers/me/addresses"),
  createAddress: (payload) =>
    request("/api/customers/me/addresses", { method: "POST", body: payload }),
  updateAddress: (addressId, payload) =>
    request(`/api/customers/me/addresses/${addressId}`, { method: "PUT", body: payload }),
  deleteAddress: (addressId) =>
    request(`/api/customers/me/addresses/${addressId}`, { method: "DELETE" }),
  cart: () => request("/api/cart"),
  addCartItem: (productId, quantity = 1) =>
    request("/api/cart/items", { method: "POST", body: { productId, quantity } }),
  updateCartItem: (productId, quantity) =>
    request(`/api/cart/items/${productId}`, { method: "PUT", body: { quantity } }),
  removeCartItem: (productId) =>
    request(`/api/cart/items/${productId}`, { method: "DELETE" }),
  clearCart: () => request("/api/cart", { method: "DELETE" }),
  orders: (page = 0, size = 10) =>
    request(`/api/orders/me?page=${page}&size=${size}`),
  wallet: () => request("/api/wallet/me"),
  walletTransactions: (page = 0, size = 20) =>
    request(`/api/wallet/me/transactions?page=${page}&size=${size}`),
  order: (productId, quantity, couponCode, idempotencyKey = createCommandId()) =>
    request("/api/orders", {
      method: "POST",
      body: { productId, quantity, couponCode: couponCode || null },
      idempotencyKey,
    }),
  createOrder: (items, shippingAddress, couponCode, idempotencyKey = createCommandId()) =>
    request("/api/orders", {
      method: "POST",
      body: { items, shippingAddress, couponCode: couponCode || null },
      idempotencyKey,
    }),
  cancel: (productId, quantity, idempotencyKey = createCommandId()) =>
    request("/api/orders/cancellations", {
      method: "POST",
      body: { productId, quantity },
      idempotencyKey,
    }),
  members: (page = 0, size = 50) =>
    request(`/api/customers/list?page=${page}&size=${size}`),
  adminOrders: (page = 0, size = 20) =>
    request(`/api/admin/orders?page=${page}&size=${size}`),
  updateFulfillment: (orderId, fulfillment) =>
    request(`/api/admin/orders/${orderId}/fulfillment`, {
      method: "PUT",
      body: typeof fulfillment === "string" ? { status: fulfillment } : fulfillment,
    }),
  orderHistory: (orderId) => request(`/api/admin/orders/${orderId}/history`),
  stocks: (productIds) => {
    const query = new URLSearchParams();
    productIds.forEach((productId) => query.append("productIds", productId));
    return request(`/api/products/stocks?${query.toString()}`);
  },
  initializeStock: (
    productId,
    availableQuantity,
    idempotencyKey = createCommandId(),
  ) =>
    request(`/api/products/${productId}/stock`, {
      method: "POST",
      body: { availableQuantity },
      idempotencyKey,
    }),
  adjustStock: (
    productId,
    quantityDelta,
    reason,
    idempotencyKey = createCommandId(),
  ) =>
    request(`/api/products/${productId}/stock/adjustments`, {
      method: "POST",
      body: { quantityDelta, reason },
      idempotencyKey,
    }),
  createProduct: ({ productName, productPrice, initialQuantity = 100, categoryId, description, imageUrl }) =>
    request("/api/products", {
      method: "POST",
      body: { productName, productPrice, initialQuantity, categoryId: categoryId || null, description, imageUrl },
    }),
  updateProduct: (productId, { productName, productPrice, categoryId, description, imageUrl }) =>
    request(`/api/products/${productId}`, {
      method: "PUT",
      body: { productName, productPrice, categoryId: categoryId || null, description, imageUrl },
    }),
  deleteProduct: (productId) =>
    request(`/api/products/${productId}`, { method: "DELETE" }),
  wishlist: () => request("/api/wishlist"),
  addWishlist: (productId) =>
    request("/api/wishlist", { method: "POST", body: { productId } }),
  removeWishlist: (productId) =>
    request(`/api/wishlist/${productId}`, { method: "DELETE" }),
  productReviews: (productId, page = 0, size = 20) =>
    request(`/api/products/${productId}/reviews?page=${page}&size=${size}`),
  myReview: (productId) => request(`/api/products/${productId}/reviews/me`),
  writeReview: (productId, rating, comment) =>
    request(`/api/products/${productId}/reviews`, {
      method: "POST",
      body: { rating, comment },
    }),
  deleteReview: (productId) =>
    request(`/api/products/${productId}/reviews`, { method: "DELETE" }),
  stockAlerts: (page = 0, size = 100) =>
    request(`/api/stock-alerts?page=${page}&size=${size}`),
  subscribeStockAlert: (productId) =>
    request(`/api/stock-alerts/${productId}`, { method: "POST" }),
  unsubscribeStockAlert: (productId) =>
    request(`/api/stock-alerts/${productId}`, { method: "DELETE" }),
};
