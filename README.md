# SKALA Shop

Java 21과 Spring Boot로 구현한 쇼핑몰 백엔드와 순수 HTML, CSS, JavaScript
프론트엔드를 함께 관리하는 모노레포입니다. 백엔드는 하나의 애플리케이션과
PostgreSQL을 사용하되 도메인별 패키지, 공개 API와 DB 스키마를 분리한 모듈러
모놀리식입니다. 경계를 유지한 채 필요한 모듈부터 MSA로 옮길 수 있도록
구성했습니다.

## 현재 구현 범위

- 회원가입, 로그인, 로그아웃, 프로필 수정·탈퇴와 데모용 비밀번호 재설정
- HttpOnly JWT 쿠키 인증, CSRF 보호, 역할 기반 권한과 인증 요청 제한
- 저장 배송지 관리와 회원별 기본 배송지
- 카테고리, 상품 상세 정보, 이미지 URL, 검색·가격 필터와 관리자 상품 관리
- 장바구니 추가·수량 변경·삭제·비우기와 실시간 상품·재고 검증
- 다중 상품 주문, 포인트 차감, 재고 예약과 UUID 멱등성
- 주문 시점 상품·가격·배송지 스냅샷과 부분 취소·환급
- 결제/취소 상태와 별도로 관리하는 배송 상태 및 변경 이력
- 관리자 전체 주문, 배송 상태, 재고 조정·변경 이력 조회
- 고객 포인트 잔액과 거래 원장 조회
- Swagger/OpenAPI, Actuator health와 민감정보를 제외한 API 처리시간 AOP 로그
- Flyway와 Testcontainers 단위·통합 테스트
- EC2·RDS·Docker Compose·Nginx·Certbot·Docker Hub 기반 배포 구성

현재 정적 프론트는 회원·상품·카테고리 검색, 장바구니, 저장 배송지, 다중 상품
주문·취소, 포인트 원장과 관리자 상품·재고·주문·배송 관리까지 연결되어 있습니다.

## 저장소 구조

~~~text
./            Spring Boot 백엔드, Dockerfile, 로컬 Compose와 CI 설정
src/          모듈러 모놀리식 소스, Flyway 마이그레이션과 테스트
frontend/     Vercel에 배포하는 정적 쇼핑몰 프론트엔드
docs/         모듈 경계, DB 소유권과 MSA 전환 문서
deploy/       EC2, Nginx, Certbot, 배포·롤백 구성
~~~

## 기술 스택

백엔드:

- Java 21, Spring Boot 3.5.16
- Spring Modulith 1.4.12
- Spring Security, OAuth2 Resource Server와 JWT 쿠키 인증
- Spring Data JPA, PostgreSQL 17과 Flyway
- Springdoc OpenAPI, Swagger UI와 Actuator
- Gradle, JUnit 5, Testcontainers

프론트엔드 및 배포:

- HTML5, CSS3, Vanilla JavaScript와 Vercel
- Docker, Docker Compose, Nginx와 Certbot
- AWS EC2, 비공개 RDS PostgreSQL과 Docker Hub
- GitHub Actions CI/CD

## 백엔드 모듈

~~~text
auth        계정, BCrypt 비밀번호, JWT와 인증 요청 제한
member      고객 프로필과 저장 배송지
catalog     카테고리, 상품, 검색과 상품 생명주기 이벤트
inventory   주문 가능 재고와 재고 변경 원장
cart        회원 장바구니와 상품·재고 검증
wallet      포인트 계정과 거래 원장
order       다중 상품 주문, 스냅샷, 취소, 배송 상태와 이력
storefront  회원가입·고객 상세·기존 URI를 조합하는 호환 계층
common      예외, API 오류와 페이지 응답 등 최소 공통 코드
~~~

모듈 의존 방향, 테이블 소유권, 주문 트랜잭션과 MSA 전환 순서는
[아키텍처 문서](docs/architecture.md)를 참고합니다.

## 로컬 실행

필수 조건은 Java 21, Docker와 Docker Compose plugin입니다.

~~~bash
docker compose -f compose.local.yml up -d
./gradlew bootRun
~~~

기본 주소:

~~~text
API             http://localhost:8080
Swagger UI      http://localhost:8080/swagger-ui.html
OpenAPI JSON    http://localhost:8080/v3/api-docs
Actuator health http://localhost:8080/actuator/health
PostgreSQL      localhost:5432/skala_shop
~~~

로컬 기본 DB 계정은 `skala / skala`입니다. 전체 환경변수는
[.env.example](.env.example)에 있으며 실제 비밀값 파일은 커밋하지 않습니다.

프론트는 백엔드 기동 후 저장소 루트에서 실행합니다.

~~~bash
python3 -m http.server 3000 --directory frontend
~~~

`http://localhost:3000`으로 접속합니다. 로컬에서는 같은 호스트의 8080 포트를
자동으로 API 주소로 사용합니다. 자세한 설정은
[프론트엔드 문서](frontend/README.md)를 참고합니다.

## 테스트

~~~bash
./gradlew clean test
~~~

현재 전체 82개 테스트가 다음 범위를 검증합니다.

- Spring Modulith 모듈 경계와 Flyway V1~V16
- 회원가입·로그인·권한·쿠키 JWT·CSRF·BCrypt와 요청 제한
- 상품·재고·포인트·주문의 단위 및 실제 PostgreSQL 통합 동작
- 다중 상품 주문, 배송지 스냅샷, 배송 상태와 원장 조회
- 주문·취소 멱등 재시도와 마지막 재고 동시 주문
- 재고·포인트·주문 원자성, 실패 롤백과 취소 동시성
- Swagger/OpenAPI 계약
- API 로그의 경로 변수·요청 본문·예외 상세 비노출

GitHub Actions는 82개 Gradle 테스트에 더해 프론트 JavaScript 문법·정적 파일·
배포 빌드 검사, Chromium 고객·관리자 E2E와 배포 릴리스 흐름 시뮬레이션을
실행합니다.

## 주요 API

전체 요청·응답 모델과 오류 코드는 Swagger UI가 기준입니다. 아래는 현재 공개된
API 그룹입니다.

~~~text
# 인증·회원
GET    /api/auth/csrf
POST   /api/customers
POST   /api/customers/login
POST   /api/customers/logout
POST   /api/customers/password/reset
GET    /api/customers/me
PUT    /api/customers/me
DELETE /api/customers/me
GET    /api/customers/{customerId}
GET    /api/customers/list                         ADMIN
PUT    /api/admin/password                         ADMIN

# 저장 배송지
GET    /api/customers/me/addresses
POST   /api/customers/me/addresses
PUT    /api/customers/me/addresses/{addressId}
DELETE /api/customers/me/addresses/{addressId}

# 카테고리·상품
GET    /api/categories
POST   /api/categories                             ADMIN
PUT    /api/categories/{id}                        ADMIN
DELETE /api/categories/{id}                        ADMIN
GET    /api/products?query=&categoryId=&minPrice=&maxPrice=&page=&size=
GET    /api/products/list                            기존 목록 URI 호환
GET    /api/products/{productId}
POST   /api/products                               ADMIN
PUT    /api/products/{productId}                   ADMIN
DELETE /api/products/{productId}                   ADMIN

# 재고
GET    /api/products/stocks?productIds={productId}
GET    /api/products/{productId}/stock
POST   /api/products/{productId}/stock             ADMIN
POST   /api/products/{productId}/stock/adjustments ADMIN
GET    /api/products/{productId}/stock/movements   ADMIN

# 장바구니
GET    /api/cart
DELETE /api/cart
POST   /api/cart/items
PUT    /api/cart/items/{productId}
DELETE /api/cart/items/{productId}

# 주문·배송
POST   /api/orders
GET    /api/orders/me?page=0&size=10
POST   /api/orders/cancellations
GET    /api/admin/orders?page=0&size=20             ADMIN
PUT    /api/admin/orders/{orderId}/fulfillment      ADMIN
GET    /api/admin/orders/{orderId}/history          ADMIN

# 포인트
GET    /api/wallet/me
GET    /api/wallet/me/transactions?page=0&size=20

# 기존 프론트 호환 URI
POST   /api/customers/order
POST   /api/customers/cancel
~~~

브라우저는 먼저 `GET /api/auth/csrf`를 호출하고 상태 변경 요청에 응답 토큰을
`X-XSRF-TOKEN` 헤더로 전달해야 합니다. 쿠키 전송을 위해 `fetch`에는
`credentials: "include"`를 사용합니다.

API AOP 로그는 HTTP 메서드, 실제 값이 제거된 매핑 경로, 컨트롤러 메서드,
상태 코드와 처리 시간만 기록합니다. 요청 본문·쿼리·헤더·쿠키·예외 상세는
기록하지 않으며, 필요하면 `API_LOGGING_ENABLED=false`로 끌 수 있습니다.

주문·취소와 재고 초기화·조정에는 UUID 형식의 `X-Idempotency-Key`가
필수입니다. 동일 사용자와 동일 요청 내용으로 재시도하면 최초 결과를 반환하고,
같은 키를 다른 내용에 사용하면 `409 IDEMPOTENCY_CONFLICT`를 반환합니다.

신규 주문 요청은 최대 50종의 중복 없는 상품을 받습니다. 상품 ID 순으로 재고를
예약해 잠금 순서를 고정하고, 포인트는 전체 주문 금액으로 한 번 차감합니다.
상품명·단가와 배송지는 주문 시점 값으로 보존합니다. 배송 상태는
`PAID → PREPARING → SHIPPED → DELIVERED` 순서로만 변경할 수 있으며 배송이
시작된 주문은 취소할 수 없습니다. 기존 단일 상품 주문 JSON과 호환 URI도
유지합니다.

상품 가격은 `0.01`~`30,000,000.00`, 상품별 수량은 최대 1,000,000,
주문 총액과 초기 회원 포인트는 최대 `30,000,000,000,000.00`입니다. 장바구니는
회원당 최대 50종, 저장 배송지는 최대 10개이며 DB에서도 기본 배송지를 회원당
하나만 허용합니다.

비밀번호는 BCrypt 해시로만 저장합니다. 현재 비밀번호 재설정은 학습용으로 고객
ID와 현재 이름을 확인하며 기존 JWT를 무효화합니다. 실제 서비스에서는 이메일·
휴대전화 소유 확인과 만료되는 일회용 토큰으로 교체해야 합니다. 로그인·회원가입·
재설정에는 IP·계정별 요청 제한을 적용하며 여러 인스턴스로 확장할 때는 메모리
저장소를 Redis 같은 공유 저장소로 교체합니다.

## GitFlow

- `main`: 운영 배포 기준
- `develop`: 다음 릴리스 통합
- `feature/*`: 기능 및 문서 작업
- `hotfix/*`: 운영 긴급 수정

기능 브랜치는 `develop` 대상 PR과 CI 통과 후 병합합니다. 운영 배포는 검증된
`develop`을 `main`에 병합할 때 시작합니다.

## 운영 배포

- 프론트엔드: Vercel (`frontend`, Production Branch `main`)
- 백엔드: AWS EC2의 Docker Compose
- Reverse proxy/TLS: Nginx와 Certbot 컨테이너
- 데이터베이스: 비공개 RDS PostgreSQL
- 이미지 저장소: Docker Hub의 digest 고정 이미지
- CI/CD: GitHub Actions

저장소에는 운영 Compose, Nginx, 인증서 갱신, 이미지 배포, healthcheck,
known-good 롤백과 릴리스 흐름 테스트가 이미 포함되어 있습니다. 실제 배포 전에는
AWS·Docker Hub·Vercel 리소스와 GitHub Secrets/Variables를 연결해야 합니다.
자세한 절차는 [EC2 배포 문서](deploy/README.md)를 참고합니다.
