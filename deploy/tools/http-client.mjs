export class ApiClient {
  #cookies = new Map();
  #csrf = null;

  constructor(baseUrl) {
    this.baseUrl = new URL(baseUrl).origin;
  }

  async issueCsrf(force = false) {
    if (this.#csrf && !force) return this.#csrf;
    const response = await this.#fetch("/api/auth/csrf");
    const payload = await this.#read(response);
    this.#assert(response, payload);
    this.#csrf = payload;
    return payload;
  }

  async request(path, { method = "GET", body, headers = {} } = {}) {
    const normalizedMethod = method.toUpperCase();
    const requestHeaders = { Accept: "application/json", ...headers };
    if (body !== undefined) requestHeaders["Content-Type"] = "application/json";
    if (!["GET", "HEAD", "OPTIONS"].includes(normalizedMethod)) {
      const csrf = await this.issueCsrf();
      requestHeaders[csrf.headerName || "X-XSRF-TOKEN"] = csrf.token;
    }

    let response = await this.#fetch(path, {
      method: normalizedMethod,
      headers: requestHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    let payload = await this.#read(response);
    if (response.status === 403 && !["GET", "HEAD", "OPTIONS"].includes(normalizedMethod)) {
      const csrf = await this.issueCsrf(true);
      requestHeaders[csrf.headerName || "X-XSRF-TOKEN"] = csrf.token;
      response = await this.#fetch(path, {
        method: normalizedMethod,
        headers: requestHeaders,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
      payload = await this.#read(response);
    }
    this.#assert(response, payload);
    return payload;
  }

  async #fetch(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (this.#cookies.size) {
      headers.set("Cookie", [...this.#cookies].map(([name, value]) => `${name}=${value}`).join("; "));
    }
    const response = await fetch(new URL(path, this.baseUrl), { ...options, headers, redirect: "error" });
    this.#captureCookies(response.headers);
    return response;
  }

  #captureCookies(headers) {
    const values = typeof headers.getSetCookie === "function"
      ? headers.getSetCookie()
      : [headers.get("set-cookie")].filter(Boolean);
    for (const value of values) {
      const cookie = value.split(";", 1)[0];
      const separator = cookie.indexOf("=");
      if (separator <= 0) continue;
      const name = cookie.slice(0, separator).trim();
      const contents = cookie.slice(separator + 1).trim();
      if (contents) this.#cookies.set(name, contents);
      else this.#cookies.delete(name);
    }
  }

  async #read(response) {
    if (response.status === 204) return null;
    const text = await response.text();
    if (!text) return null;
    return response.headers.get("content-type")?.includes("application/json")
      ? JSON.parse(text)
      : text;
  }

  #assert(response, payload) {
    if (response.ok) return;
    const detail = payload?.message || (typeof payload === "string" ? payload : response.statusText);
    const error = new Error(`${response.status} ${detail}`);
    error.status = response.status;
    error.payload = payload;
    throw error;
  }
}

export function requiredEnvironment(name) {
  const value = process.env[name]?.trim();
  if (!value) throw new Error(`${name} 환경변수가 필요합니다.`);
  return value;
}

export function confirmedOrigin(value, confirmation) {
  const origin = new URL(value).origin;
  if (new URL(confirmation).origin !== origin || confirmation.replace(/\/+$/, "") !== origin) {
    throw new Error(`변경 대상 확인값이 일치하지 않습니다. --confirm-origin=${origin}을 사용하세요.`);
  }
  return origin;
}
