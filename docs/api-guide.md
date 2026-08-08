# API 사용 가이드

이 문서는 프론트엔드 개발자가 SKALA Shop API를 처음 연결할 때 필요한 공통 규칙을
설명합니다. 전체 스키마와 상태 코드별 응답은 Swagger UI가 최종 기준입니다.

- 로컬 Swagger: <http://localhost:8080/swagger-ui.html>
- 운영 Swagger: <https://api-3-39-64-119.sslip.io/swagger-ui/index.html>
- 운영 OpenAPI JSON: <https://api-3-39-64-119.sslip.io/v3/api-docs>

## 1. 기본 주소

로컬 프론트는 API를 `http://localhost:8080`으로 직접 호출합니다. 운영 프론트는
같은 Origin의 `/api`, `/actuator`를 호출하고 Vercel rewrite가 EC2 API로 전달합니다.

```javascript
fetch("/api/products?page=0&size=12", {
  credentials: "include",
});
```

운영 브라우저 코드에 EC2 IP나 DB 주소를 직접 넣지 않습니다.

## 2. Request DTO와 Response DTO

- Request DTO: 프론트가 보낸 JSON을 Controller의 `@RequestBody`가 받는 객체
- Response DTO: Service 처리 결과를 Controller가 JSON으로 돌려주는 객체
- View/Command: 모듈 사이에서 사용하는 공개 Java 계약
- Entity: DB 저장 모델이며 HTTP로 직접 노출하지 않음

예를 들어 회원 이름 변경은 다음 순서입니다.

```text
Frontend JSON { "name": "새 이름" }
→ UpdateMemberRequest
→ MemberController
→ MemberApplicationService
→ MemberRepository
→ PostgreSQL
→ MemberResponse JSON
```

## 3. CSRF와 쿠키 인증

로그인 전에도 먼저 CSRF 토큰을 발급받습니다.

```javascript
const csrfResponse = await fetch("/api/auth/csrf", {
  credentials: "include",
});
const csrf = await csrfResponse.json();
```

상태 변경 요청은 응답의 토큰을 헤더로 전달합니다.

```javascript
await fetch("/api/customers/login", {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
    [csrf.headerName]: csrf.token,
  },
  body: JSON.stringify({
    customerId: "skala01",
    customerPassword: "password",
  }),
});
```

로그인 성공 시 Access JWT는 JSON이 아니라 `bff-access` HttpOnly 쿠키로 설정됩니다.
동시에 회전 가능한 opaque Refresh Token이 `bff-refresh` HttpOnly 쿠키에 저장됩니다.
브라우저 JavaScript는 토큰을 읽지 않고 모든 요청에 `credentials: "include"`만
사용합니다.

Access Token이 만료되면 프론트는 `POST /api/customers/refresh`를 한 번 호출해 두
쿠키를 회전하고 원래 요청을 재시도합니다. Refresh Token은 서버에 hash만 저장하며
한 번 사용한 토큰은 재사용할 수 없습니다. 비밀번호 변경·탈퇴로 credential version이
바뀌면 기존 세션도 거부됩니다.

## 4. 역할

| 역할 | 접근 범위 |
| --- | --- |
| 비로그인 | CSRF, 회원가입·로그인·비밀번호 재설정, 상품·카테고리·재고 조회 |
| `CUSTOMER` | 내 정보, 배송지, 장바구니, 주문·취소, 포인트, 위시리스트·리뷰·재입고 알림 |
| `ADMIN` | 고객 목록, 카테고리·상품·재고, 전체 주문·배송 관리 |

관리자 계정으로 고객 주문 API를 호출하거나 고객 계정으로 관리자 API를 호출하면
`403 ACCESS_DENIED`를 반환합니다.

## 5. 공통 오류 형식

Validation, 인증·인가와 비즈니스 오류는 같은 JSON 형식을 사용합니다.

```json
{
  "code": "INVALID_PARAMETER",
  "message": "요청 값이 올바르지 않습니다.",
  "status": 400,
  "timestamp": "2026-08-07T07:00:00Z",
  "fieldErrors": {
    "customerId": "크기가 3에서 50 사이여야 합니다"
  }
}
```

주요 오류 코드:

| HTTP | code | 의미 |
| --- | --- | --- |
| 400 | `INVALID_PARAMETER` | DTO, path, query 또는 header 값 오류 |
| 401 | `NOT_AUTHENTICATED` | 로그인 필요 또는 만료된 인증 |
| 403 | `ACCESS_DENIED` | 역할 또는 CSRF 권한 부족 |
| 404 | `DATA_NOT_FOUND` | 대상 리소스 없음 |
| 409 | `DATA_DUPLICATED` | 고유 데이터 중복 |
| 409 | `IDEMPOTENCY_CONFLICT` | 같은 키를 다른 명령에 재사용 |
| 409 | `INSUFFICIENT_FUNDS` | 포인트 부족 |
| 409 | `INSUFFICIENT_STOCK` | 재고 부족 |
| 409 | `INSUFFICIENT_QUANTITY` | 취소 가능 수량 부족 |
| 409 | `PRODUCT_NOT_SALEABLE` | 판매 중지 상품 |
| 429 | `TOO_MANY_REQUESTS` | 인증 요청 제한 초과 |
| 500 | `INTERNAL_ERROR` | 예상하지 못한 서버 오류 |

프론트는 `message`를 사용자 안내에 사용하고, 폼에서는 `fieldErrors`의 필드명을
각 input과 연결합니다. `INTERNAL_ERROR`는 내부 예외나 SQL을 노출하지 않습니다.

## 6. 멱등성

다음 API는 `X-Idempotency-Key`에 UUID가 필요합니다.

- `POST /api/orders`
- `POST /api/orders/cancellations`
- `POST /api/payments`
- `POST /api/payments/{paymentId}/approve`
- `POST /api/admin/payments/{paymentId}/refunds`
- `POST /api/returns`
- `PUT /api/admin/returns/{returnId}/status`
- `POST /api/products/{productId}/stock`
- `POST /api/products/{productId}/stock/adjustments`

```javascript
await fetch("/api/orders", {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
    "X-XSRF-TOKEN": csrf.token,
    "X-Idempotency-Key": crypto.randomUUID(),
  },
  body: JSON.stringify({
    items: [{ productId, quantity: 2 }],
    shippingAddress,
  }),
});
```

네트워크 오류로 같은 명령을 재시도할 때는 새 UUID를 만들지 않고 기존 키와 동일한
본문을 사용합니다. 같은 키에 다른 본문을 보내면 `409 IDEMPOTENCY_CONFLICT`입니다.

## 7. Pagination과 검색

목록 응답은 다음 형식입니다.

```json
{
  "content": [],
  "page": 0,
  "size": 12,
  "totalElements": 0,
  "totalPages": 0
}
```

- `page`: 0부터 시작
- `size`: 1~100
- 상품 검색: `query`, `categoryId`, `minPrice`, `maxPrice`

잘못된 UUID, `page=-1`, `size=0` 같은 값은 `400 INVALID_PARAMETER`로 분류됩니다.

## 8. API 목록

실행 중인 버전의 정확한 operation 수와 계약은 OpenAPI 문서를 기준으로 합니다.

| 그룹 | 대표 경로 |
| --- | --- |
| 인증·회원 | `/api/auth/csrf`, `/api/customers/**`, `/api/admin/password` |
| 배송지 | `/api/customers/me/addresses/**` |
| 카테고리 | `/api/categories/**` |
| 상품 | `/api/products/**` |
| SKU 옵션 | `/api/products/{productId}/variants/**` |
| 재고 | `/api/products/{id}/stock/**`, `/api/products/stocks` |
| 장바구니 | `/api/cart/**` |
| 주문·취소 | `/api/orders/**` |
| 결제 | `/api/payments/**`, `/api/admin/payments/**` |
| 반품 | `/api/returns/**`, `/api/admin/returns/**` |
| 검색 | `/api/search/products`, `/api/admin/search/reindex` |
| 관리자 주문 | `/api/admin/orders/**` |
| 포인트 | `/api/wallet/me/**` |
| 위시리스트 | `/api/wishlist/**` |
| 상품 리뷰 | `/api/products/{id}/reviews`, `/api/reviews/me` |
| 재입고 알림 | `/api/stock-alerts/**` |

각 endpoint의 정확한 Request/Response DTO, 예시와 상태 코드는 Swagger에서
확인합니다. `/api/customers/order`, `/api/customers/cancel`,
`/api/products/list`는 초기 교육용 프론트와의 호환을 위해 유지하는 경로입니다.

## 9. 입력 제한

| 항목 | 제한 |
| --- | --- |
| 고객 ID | 3~50자, 영문·숫자·`_`·`-` |
| 비밀번호 | 6~72자이며 BCrypt의 UTF-8 72바이트 이하 |
| 고객 이름 | 최대 100자 |
| 상품 가격 | `0.01`~`30,000,000.00` |
| 상품별 재고·주문 수량 | 최대 1,000,000 |
| 한 주문 상품 종류 | 최대 50종, 중복 variantId 불가 |
| 장바구니 | 회원당 최대 50종 |
| 저장 배송지 | 회원당 최대 10개 |
| 주문 총액·초기 포인트 | 최대 `30,000,000,000,000.00` |

쿠폰은 주문의 `couponCode`로 전달합니다. 현재 제공 코드(`WELCOME10`, `NEWBIE5`,
`SAVE5000`, `SAVE10000`)는 회원별 한 번만 사용할 수 있으며, 취소 환불액은 상품
원가가 아니라 쿠폰 할인 후 각 항목에 배분된 실제 결제액을 기준으로 계산합니다.

리뷰는 취소되지 않은 구매 수량이 있는 고객만 작성할 수 있습니다. 공개 리뷰
응답에는 내부 회원 UUID가 포함되지 않습니다. 재입고 알림은 현재 재고가 0인
상품만 신청할 수 있고, 재고가 양수로 전환되면 상태가 `WAITING`에서 `NOTIFIED`로
변경됩니다.

## 10. Fake PG 결제와 반품

주문은 포인트만 사용하거나 포인트와 Fake PG 금액을 나눠 결제할 수 있습니다.
Fake PG가 필요한 주문은 `PAYMENT_PENDING`으로 생성한 뒤 결제를 준비·승인합니다.
`4242-4242-4242-4242`는 성공 테스트 카드이며 다른 문서화된 번호로 승인 실패를
재현할 수 있습니다. 실제 카드 정보나 돈은 처리하지 않습니다.

배송 전 취소는 기존 주문 취소 API를 사용합니다. 배송 완료 후에는 주문 항목별로
반품을 신청하고 관리자가 회수·검수·승인 또는 거절 상태를 변경합니다. 최종
`REFUNDED`에서 결제 수단별 환불과 재고 복원이 완료됩니다.

## 11. 비밀번호 재설정 주의

현재 재설정 API는 교육용 요구사항에 맞춰 고객 ID와 현재 이름을 확인합니다. 두
값은 강한 본인 인증 수단이 아닙니다. 공개 상용 서비스에서는 이메일·휴대전화
소유 확인, 짧은 만료 시간의 일회용 토큰과 재사용 방지를 추가해야 합니다.
