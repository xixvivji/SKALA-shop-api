# Architecture

## 모듈 의존 방향

~~~text
storefront ──> auth
     │       > member
     │       > wallet
     └──────> order

order ──────> catalog
      └─────> wallet

auth / member / catalog / wallet
└─ 다른 비즈니스 모듈에 의존하지 않음
~~~

storefront는 테이블과 핵심 도메인 로직을 소유하지 않습니다. 회원가입,
고객 상세 조회, 기존 교육용 API URI처럼 여러 모듈의 결과가 필요한 흐름만
조합합니다.

## 경계 규칙

- 모듈 최상위 패키지의 인터페이스와 일반 DTO 클래스만 외부에 공개합니다.
- Entity, Repository와 구현 Service는 각 모듈의 internal 아래에 둡니다.
- 다른 모듈의 Entity나 Repository를 직접 참조하지 않습니다.
- 모듈 간 JPA 연관관계, 물리 FK와 SQL JOIN을 만들지 않습니다.
- 같은 모듈 내부에서는 FK와 JOIN을 사용합니다.
- HTTP 요청과 응답에 JPA Entity를 노출하지 않습니다.
- common에는 도메인 로직을 넣지 않습니다.

이 규칙은 ModularityTests가 검증합니다.

## PostgreSQL 소유권

~~~text
auth.accounts
member.members
catalog.products
wallet.point_accounts
wallet.point_transactions
orders.orders
orders.order_items
orders.order_cancellations
~~~

모듈들은 현재 하나의 PostgreSQL 인스턴스를 사용하지만 별도 스키마로
구분됩니다. 모듈 간 식별자는 UUID 값으로만 저장합니다.

## 트랜잭션

~~~text
판매 상품과 가격 조회
→ 포인트 계정 잠금 및 차감
→ 상품명과 단가 스냅샷을 가진 주문 저장
→ 모두 commit 또는 모두 rollback
~~~

취소도 주문항목과 포인트 계정을 잠근 뒤 주문 당시 단가로 환급합니다.
주문과 취소 명령에는 멱등성 키를 저장하여 동일 요청의 중복 실행을 방지합니다.
포인트 차감이나 환급을 트랜잭션 커밋 이후 이벤트로 처리하면 안 됩니다.

## MSA 전환

### 1. Catalog

- catalog 스키마와 모듈 코드를 Catalog Service로 이동
- Order의 LocalProductReader를 HTTP 클라이언트 구현으로 교체
- Order의 상품명과 가격 스냅샷 덕분에 과거 조회 영향 없음

### 2. Auth

- auth 스키마와 JWT 발급을 Auth Service로 이동
- HMAC 공유 키에서 비대칭 서명과 JWKS 방식으로 변경
- 각 서비스는 공개키로 JWT 검증

### 3. Wallet

Wallet 분리부터는 기존 DB 원자성을 사용할 수 없습니다. Outbox, 메시지
브로커, Saga와 보상 처리, 멱등성, 재시도, Dead Letter Queue와 분산 추적이
준비된 후 분리합니다. LocalPointManager는 HTTP 또는 메시지 어댑터로
교체합니다.

### 4. Order

전체 구매 흐름을 조율하므로 가장 마지막에 분리합니다. 모듈을 옮기는
것보다 장애 복구, 상태 전이와 관측 가능성을 먼저 설계합니다.

## Flyway 변경 규칙

- 하나의 마이그레이션에서 여러 모듈 테이블을 동시에 변경하지 않습니다.
- 운영 컬럼 삭제와 이름 변경은 한 번의 배포로 수행하지 않습니다.
- 추가, 애플리케이션 전환, 기존 구조 제거 순서의 expand/contract 방식을
  사용합니다.
- 이미지 롤백으로 DB 마이그레이션까지 복구된다고 가정하지 않습니다.
