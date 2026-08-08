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
const runId = `${Date.now().toString(36)}${randomUUID().slice(0, 5)}`;
const startIndex = Number.parseInt(argumentsMap.get("--start-index") || "0", 10);
const requestedCount = Number.parseInt(
  argumentsMap.get("--count") || String(6 - startIndex),
  10,
);
const delayMilliseconds = Number.parseInt(argumentsMap.get("--delay-ms") || "13000", 10);

const reviewers = [
  {
    name: "김민준",
    reviews: [
      { rating: 5, comment: "포장이 깔끔했고 실제 색감도 사진과 비슷해요. 며칠 써보니 기본기가 탄탄해서 만족합니다." },
      { rating: 4, comment: "사용법이 단순하고 마감도 괜찮아요. 배송도 예상보다 빨랐습니다." },
    ],
  },
  {
    name: "이서연",
    reviews: [
      { rating: 5, comment: "디자인이 과하지 않아서 어디에 두어도 잘 어울려요. 선물용으로도 좋을 것 같아요." },
      { rating: 5, comment: "생각했던 것보다 소재가 탄탄하고 사용감이 편안합니다. 재구매 의향 있어요." },
    ],
  },
  {
    name: "박지훈",
    reviews: [
      { rating: 4, comment: "가격 대비 완성도가 좋습니다. 옵션 선택도 알아보기 쉬웠고 제품 상태도 좋았어요." },
      { rating: 5, comment: "일주일 정도 사용했는데 기대한 역할을 충분히 해줍니다. 주변에도 추천하고 싶어요." },
    ],
  },
  {
    name: "최유나",
    reviews: [
      { rating: 5, comment: "사진에서 본 분위기 그대로예요. 포인트가 되면서도 실용적이라 자주 쓰게 됩니다." },
      { rating: 4, comment: "크기와 무게가 적당해서 부담 없이 사용하기 좋아요. 전체적으로 만족스러워요." },
    ],
  },
  {
    name: "정하늘",
    reviews: [
      { rating: 5, comment: "마감이 세심하고 패키지도 단정합니다. 직접 받아보니 더 마음에 들어요." },
      { rating: 4, comment: "기본에 충실한 제품이에요. 오래 두고 편하게 사용할 수 있을 것 같습니다." },
    ],
  },
  {
    name: "윤지호",
    reviews: [
      { rating: 5, comment: "주문부터 수령까지 매끄러웠고 제품 품질도 기대 이상입니다. 잘 사용하고 있어요." },
      { rating: 5, comment: "옵션 설명이 정확해서 선택하기 편했어요. 실제 사용감도 좋아서 만족합니다." },
    ],
  },
];

if (!Number.isInteger(startIndex) || startIndex < 0 || startIndex >= reviewers.length) {
  throw new Error(`--start-index는 0부터 ${reviewers.length - 1} 사이여야 합니다.`);
}
if (!Number.isInteger(requestedCount) || requestedCount < 1
    || startIndex + requestedCount > reviewers.length) {
  throw new Error(`--count는 선택 가능한 리뷰어 범위 안의 양수여야 합니다.`);
}
if (!Number.isInteger(delayMilliseconds) || delayMilliseconds < 0) {
  throw new Error("--delay-ms는 0 이상의 정수여야 합니다.");
}

const selectedReviewers = reviewers.slice(startIndex, startIndex + requestedCount);

function wait(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function commandHeaders() {
  return { "X-Idempotency-Key": randomUUID() };
}

async function orderableCatalog(client) {
  const page = await client.request("/api/products?page=0&size=100");
  const result = [];
  for (const product of page.content || []) {
    const variants = await client.request(`/api/products/${product.id}/variants`);
    for (const variant of variants) {
      const stock = await client.request(`/api/products/${variant.id}/stock`).catch(() => null);
      if (stock?.orderable && Number(stock.availableQuantity) >= 2) {
        result.push({ product, variant });
        break;
      }
    }
  }
  if (result.length < (startIndex + selectedReviewers.length) * 2) {
    throw new Error(`리뷰용 주문 가능 상품이 부족합니다: ${result.length}개`);
  }
  return result;
}

const catalogClient = new ApiClient(origin);
const catalog = await orderableCatalog(catalogClient);

for (const [selectionIndex, reviewer] of selectedReviewers.entries()) {
  const reviewerIndex = startIndex + selectionIndex;
  const client = new ApiClient(origin);
  const loginId = `showcase_${runId}_${reviewerIndex + 1}`.slice(0, 50);
  const password = `Showcase-${randomUUID()}-pw`;
  const selections = catalog.slice(reviewerIndex * 2, reviewerIndex * 2 + 2);

  await client.issueCsrf();
  await client.request("/api/customers", {
    method: "POST",
    body: { customerId: loginId, customerName: reviewer.name, customerPassword: password },
  });
  await client.request("/api/customers/login", {
    method: "POST",
    body: { customerId: loginId, customerPassword: password },
  });

  const order = await client.request("/api/orders", {
    method: "POST",
    headers: commandHeaders(),
    body: {
      items: selections.map(({ product, variant }) => ({
        productId: product.id,
        variantId: variant.id,
        quantity: 1,
      })),
      shippingAddress: {
        recipientName: reviewer.name,
        phoneNumber: `010-${String(2100 + reviewerIndex).padStart(4, "0")}-${String(7300 + reviewerIndex).padStart(4, "0")}`,
        postalCode: "06236",
        addressLine1: "서울특별시 강남구 테헤란로",
        addressLine2: "쇼케이스 주문",
      },
      pointAmount: "0.00",
    },
  });

  const payment = await client.request("/api/payments", {
    method: "POST",
    headers: commandHeaders(),
    body: { orderId: order.id, method: "CARD" },
  });
  await client.request(`/api/payments/${payment.id}/approve`, {
    method: "POST",
    headers: commandHeaders(),
    body: { testCardNumber: "4242-4242-4242-4242" },
  });

  for (const [reviewIndex, selection] of selections.entries()) {
    const review = reviewer.reviews[reviewIndex];
    await client.request(`/api/products/${selection.product.id}/reviews`, {
      method: "POST",
      body: review,
    });
    console.log(`review created: ${selection.product.name} (${review.rating}/5)`);
  }

  await client.request("/api/customers/logout", { method: "POST" }).catch(() => {});
  if (selectionIndex < selectedReviewers.length - 1 && delayMilliseconds > 0) {
    await wait(delayMilliseconds);
  }
}

console.log(`demo review seed completed: ${selectedReviewers.length} customers, ${selectedReviewers.length * 2} reviews`);
