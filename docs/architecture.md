# Architecture

## 설계 목표

현재 시스템은 하나의 Spring Boot 프로세스와 PostgreSQL을 사용하는 모듈러
모놀리식입니다. 각 비즈니스 모듈은 공개 Java API, `internal` 구현 패키지와 전용
DB 스키마/테이블을 가지며 다른 모듈의 Entity와 Repository를 직접 사용하지
않습니다. 이 구조로 초기 운영 복잡도는 낮추고, 트래픽과 장애 격리가 실제로
필요해진 모듈만 별도 서비스로 옮길 수 있게 합니다.

## 모듈 의존 방향

~~~text
storefront ──> auth
     │       > member
     │       > wallet
     └──────> order

cart ───────> catalog
  └─────────> inventory

order ──────> catalog
      ├─────> inventory
      └─────> wallet

catalog ── ProductCreated ──> inventory
        └─ ProductDeleted ──> inventory, cart

auth, member, catalog, wallet ──> common
inventory ──> catalog, common
cart      ──> catalog, inventory, common
order     ──> catalog, inventory, wallet, common
storefront──> auth, member, order, wallet, common
~~~

- `storefront`는 회원가입, 고객 상세와 기존 교육용 API URI처럼 여러 모듈의
  결과가 필요한 흐름만 조합하며 테이블이나 핵심 도메인 로직을 소유하지 않습니다.
- `cart`는 상품과 재고의 공개 조회 API만 사용하고 상품 삭제 이벤트로 오래된
  장바구니 항목을 제거합니다.
- `order`는 `CatalogApi`, `InventoryApi`, `WalletApi`를 내부 포트 어댑터로 감싸서
  사용합니다. MSA 전환 시 이 로컬 어댑터를 HTTP/메시지 어댑터로 교체합니다.
- Catalog 상품 생성 이벤트는 같은 트랜잭션에서 초기 재고를 만들고, 삭제 이벤트는
  재고를 비활성화하고 장바구니 항목을 정리합니다.

## 패키지와 경계 규칙

- 모듈 최상위 패키지에는 외부에서 사용할 인터페이스, 이벤트와 일반 View/Command
  클래스만 공개합니다.
- Entity, Repository, 구현 Service와 HTTP DTO는 각 모듈의 `internal` 아래에 둡니다.
- 다른 모듈의 Entity나 Repository를 직접 참조하지 않습니다.
- 모듈 간 JPA 연관관계, 물리 FK와 SQL JOIN을 만들지 않습니다.
- 같은 모듈이 소유한 테이블 사이에서는 FK, JOIN과 JPA 매핑을 사용할 수 있습니다.
- 모듈 간 식별자는 UUID 값으로만 전달·저장합니다.
- HTTP 요청과 응답에 JPA Entity를 노출하지 않습니다.
- `common`에는 도메인 로직을 넣지 않습니다.

`ModularityTests`가 Spring Modulith로 허용되지 않은 패키지 의존을 검증합니다.

## PostgreSQL 소유권

~~~text
auth
└── accounts

member
├── members
└── member_addresses

catalog
├── categories
└── products

inventory
├── stocks
└── stock_movements

cart
├── carts
└── cart_items

wallet
├── point_accounts
└── point_transactions

orders
├── orders
├── order_items
├── order_cancellations
├── order_shipping_addresses
└── order_status_histories
~~~

모든 모듈이 현재 하나의 PostgreSQL 인스턴스를 사용하므로 주문·재고·포인트를 한
로컬 트랜잭션으로 처리할 수 있습니다. `member_addresses → members`,
`products → categories`, 주문 스키마 내부 테이블처럼 같은 모듈 안에서만 물리 FK를
사용합니다. `cart.member_id`, 주문의 `member_id/product_id`처럼 모듈을 넘는
식별자에는 물리 FK를 두지 않습니다.

## 핵심 트랜잭션

### 다중 상품 주문

~~~text
요청 모양, 중복 상품, 수량과 총액 검증
→ 상품 ID 순으로 정렬
→ 판매 상품과 주문 시점 상품명·단가 조회
→ 포인트 계정 잠금 및 전체 금액 한 번 차감
→ 동일 주문 멱등 재시도 여부 재확인
→ 고정된 상품 순서로 각 재고 잠금 및 예약
→ 주문, 순번을 가진 주문항목과 배송지 스냅샷 저장
→ 초기 배송 상태 이력 PAID 저장
→ 모두 commit 또는 모두 rollback
~~~

상품 ID 정렬은 동시 다중 상품 주문의 잠금 순서를 고정해 교착 가능성을 낮춥니다.
주문 명령의 fingerprint에는 회원, 정렬된 상품·수량과 배송지가 포함됩니다. 같은
회원의 동일 멱등성 키와 같은 요청은 최초 생성 결과를 반환하고, 내용이 다르면
`IDEMPOTENCY_CONFLICT`를 반환합니다.

### 취소

취소 가능한 주문항목을 잠그고 최신 구매부터 요청 수량을 차감합니다. 주문 당시
단가로 포인트를 환급하고 하나의 취소 ID로 재고를 복원합니다. 다중 상품 주문은
모든 항목의 잔여 수량이 0일 때만 주문 상태가 `CANCELED`가 됩니다. 포인트 환급,
주문 상태, 취소 이력과 재고 복원 중 하나라도 실패하면 전체를 롤백합니다.

배송 상태는 금전 상태 `PAID/PARTIALLY_CANCELED/CANCELED`와 별도로
`PAID → PREPARING → SHIPPED → DELIVERED`만 허용합니다. `SHIPPED` 이후에는
취소할 수 없고 관리자 변경 주체와 시점을 별도 이력에 보존합니다.

### 장바구니와 배송지

장바구니는 현재 상품 가격과 재고를 조회해 응답을 조립하므로 저장된 항목에는
가격을 중복 저장하지 않습니다. 주문에서는 가격 변동과 회원 배송지 수정의 영향을
받지 않도록 상품명·단가와 배송지를 스냅샷으로 저장합니다. 저장 배송지는 회원당
최대 10개이며 부분 unique index로 기본 배송지를 하나만 허용합니다.

## 읽기와 페이지 처리

- 상품·회원·주문·포인트·재고 원장은 안정적인 보조 `id` 정렬을 사용합니다.
- 주문 조회는 한 요청 안에서 `REPEATABLE_READ` snapshot으로 주문과 항목을 읽습니다.
- 목록 API의 `page`는 0부터 시작하고 `size`는 1~100입니다.
- 현재는 offset pagination이며 데이터가 크게 늘어나면 주문·원장부터
  `(createdAt, id)` cursor/keyset pagination으로 전환합니다.

## MSA 전환 순서

### 1. Cart 또는 Catalog

- Cart는 주문 트랜잭션에 참여하지 않아 비교적 먼저 분리할 수 있습니다.
- Cart 저장소를 옮기고 Catalog/Inventory 공개 조회를 HTTP 클라이언트로 바꿉니다.
- 상품 삭제 이벤트는 메시지 소비로 전환하고 소비자 멱등성을 추가합니다.
- Catalog는 스키마와 코드를 옮긴 뒤 `LocalProductReader`를 원격 어댑터로 바꿉니다.
- 주문의 상품명·단가 스냅샷 덕분에 과거 주문 조회는 Catalog 장애에 영향받지 않습니다.

### 2. Auth와 Member

- Auth 스키마와 JWT 발급을 Auth Service로 이동합니다.
- HMAC 공유 키를 비대칭 서명과 JWKS 방식으로 바꿉니다.
- Member와 저장 배송지를 옮길 때 주문 배송지 스냅샷은 Order에 그대로 둡니다.

### 3. Wallet

Wallet 분리부터 기존 DB 원자성을 사용할 수 없습니다. Outbox, 메시지 브로커,
Saga와 보상 처리, 재시도, 멱등성, Dead Letter Queue와 분산 추적을 준비한 뒤
분리합니다. `LocalPointManager`는 HTTP 또는 메시지 어댑터로 교체합니다.

### 4. Inventory

`LocalStockManager`를 원격 어댑터로 바꾸기 전에 주문 `PENDING`, 재고 예약 만료,
포인트 실패 시 재고 해제, 재고 실패 시 포인트 환급과 같은 보상 흐름이 필요합니다.
여러 상품 예약은 교착 방지뿐 아니라 부분 성공 복구도 설계해야 합니다.

### 5. Order

Order는 전체 구매 흐름을 조율하므로 가장 마지막에 분리합니다. 서비스 이동보다
Outbox relay, Saga 상태, 장애 복구, 재처리 운영 도구와 관측 가능성을 먼저
완성합니다.

## Flyway 변경 규칙

- 적용된 마이그레이션 파일은 수정하거나 삭제하지 않고 새 버전을 추가합니다.
- 새 마이그레이션은 가능하면 한 모듈의 테이블만 변경합니다. V13은 초기 장바구니
  확장 과정에서 Member와 Cart를 함께 만든 과거 예외이며 이후에는 반복하지 않습니다.
- 운영 컬럼 삭제와 이름 변경은 한 번의 배포로 수행하지 않습니다.
- 추가, 애플리케이션 전환, 기존 구조 제거 순서의 expand/contract를 사용합니다.
- 애플리케이션 이미지 롤백으로 DB 마이그레이션까지 복구된다고 가정하지 않습니다.
- 파괴적 변경 전에 RDS snapshot/backup과 forward-fix 절차를 준비합니다.
