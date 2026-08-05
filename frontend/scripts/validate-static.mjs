import { existsSync, readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const frontendRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const errors = [];

const htmlPath = resolve(frontendRoot, "index.html");
const html = readFileSync(htmlPath, "utf8");
const appSource = readFileSync(resolve(frontendRoot, "js/app.js"), "utf8");

const productPriceMaximum = html.match(
  /name=["']productPrice["'][^>]*\bmax=["']([^"']+)["']/,
)?.[1];
const orderFormHtml = html.match(/<form\b[^>]*id=["']order-form["'][\s\S]*?<\/form>/)?.[0];
const orderQuantityMaximum = orderFormHtml?.match(
  /name=["']quantity["'][^>]*\bmax=["']([^"']+)["']/,
)?.[1];

if (!productPriceMaximum || !orderQuantityMaximum) {
  errors.push("상품 가격 또는 주문 수량의 브라우저 최대값을 찾을 수 없습니다.");
} else {
  const maximumOrderTotal = (
    JSON.parse(productPriceMaximum) * JSON.parse(orderQuantityMaximum)
  ).toFixed(2);
  const expectedMaximumOrderTotal = "30000000000000.00";
  if (maximumOrderTotal !== expectedMaximumOrderTotal) {
    errors.push(
      `상품 가격과 주문 수량 최대값의 합계가 ${expectedMaximumOrderTotal}이어야 합니다.`,
    );
  }

  for (const boundary of ["29999999999999.99", expectedMaximumOrderTotal]) {
    if (JSON.parse(boundary).toFixed(2) !== boundary) {
      errors.push(`JavaScript Number가 금액 경계 ${boundary}의 센트 정밀도를 잃습니다.`);
    }
  }
}

// DOM Event.currentTarget is cleared after an async listener yields. Capture the form
// before the first await instead of dereferencing currentTarget in success/error paths.
if (/event\.currentTarget\.reset\(\)/.test(appSource)) {
  errors.push("비동기 submit 처리에서 event.currentTarget.reset()을 직접 호출할 수 없습니다.");
}
if (/showAuthError\([^)]*event\.currentTarget/.test(appSource)) {
  errors.push("비동기 인증 오류 처리에는 미리 캡처한 form 요소를 전달해야 합니다.");
}

const loadCustomerSource = appSource.slice(
  appSource.indexOf("async function loadCustomer"),
  appSource.indexOf("async function loadOrders"),
);
if (
  !loadCustomerSource.includes("const requestGeneration = authGeneration") ||
  !loadCustomerSource.includes("const sessionSnapshot = captureSessionSnapshot()") ||
  (loadCustomerSource.match(/isCurrentSessionRequest\(requestGeneration, sessionSnapshot\)/g) || [])
    .length < 2
) {
  errors.push("고객 정보 요청은 성공과 실패 모두 현재 인증 세션인지 검증해야 합니다.");
}

const renderLoadMoreSource = appSource.slice(
  appSource.indexOf("function renderLoadMore"),
  appSource.indexOf("function renderProducts"),
);
if (
  !renderLoadMoreSource.includes("document.activeElement === button") ||
  !renderLoadMoreSource.includes("restoreFocusAfterLoad") ||
  !renderLoadMoreSource.includes("focusLoadedContent(button)")
) {
  errors.push("마지막 더보기 버튼을 숨길 때 새로 불러온 목록으로 포커스를 이동해야 합니다.");
}

const forgotPasswordSource = appSource.slice(
  appSource.indexOf('$("[data-forgot-password]")'),
  appSource.indexOf("$$('[data-password-toggle]')"),
);
if (!forgotPasswordSource.includes('$("#auth-tab-reset").focus()')) {
  errors.push("비밀번호 재설정 탭으로 전환한 뒤 선택된 탭에 포커스를 이동해야 합니다.");
}

const orderSubmitSource = appSource.slice(
  appSource.indexOf('orderForm.addEventListener("submit"'),
  appSource.indexOf('const cancelForm = $("#cancel-form")'),
);
if (
  !orderSubmitSource.includes('error?.code === "INSUFFICIENT_STOCK"') ||
  !orderSubmitSource.includes("await loadProducts()")
) {
  errors.push("재고 부족 주문 충돌 뒤에는 상품 재고를 다시 불러와야 합니다.");
}

const stockSubmitSource = appSource.slice(
  appSource.indexOf('stockForm.addEventListener("submit"'),
  appSource.indexOf("function restoreRememberedCustomerId"),
);
if (
  !/error\?\.status === 401[\s\S]*showApiError\(error\)/.test(stockSubmitSource)
) {
  errors.push("재고 변경 중 인증 만료는 중앙 API 오류 처리로 위임해야 합니다.");
}

const localReferences = [...html.matchAll(/\b(?:href|src)=["']([^"']+)["']/g)]
  .map((match) => match[1])
  .filter((reference) => !/^(?:[a-z]+:|\/\/|#)/i.test(reference));

for (const reference of localReferences) {
  const pathWithoutQuery = reference.split(/[?#]/, 1)[0];
  if (!existsSync(resolve(frontendRoot, pathWithoutQuery))) {
    errors.push(`index.html에서 찾을 수 없는 파일을 참조합니다: ${reference}`);
  }
}

const ids = [...html.matchAll(/\bid=["']([^"']+)["']/g)].map((match) => match[1]);
const duplicateIds = [...new Set(ids.filter((id, index) => ids.indexOf(id) !== index))];
for (const id of duplicateIds) {
  errors.push(`index.html에 중복된 id가 있습니다: ${id}`);
}

for (const relativePath of [
  "runtime-config.js",
  "config.js",
  "js/api.js",
  "js/app.js",
]) {
  const sourcePath = resolve(frontendRoot, relativePath);
  const source = readFileSync(sourcePath, "utf8");
  const imports = [...source.matchAll(/\b(?:import|export)\s+(?:[^"']*?\s+from\s+)?["'](\.[^"']+)["']/g)]
    .map((match) => match[1]);

  for (const importedPath of imports) {
    if (!existsSync(resolve(dirname(sourcePath), importedPath))) {
      errors.push(`${relativePath}에서 찾을 수 없는 모듈을 참조합니다: ${importedPath}`);
    }
  }
}

if (errors.length > 0) {
  for (const error of errors) {
    console.error(error);
  }
  process.exitCode = 1;
} else {
  console.log("Frontend static file validation passed.");
}
