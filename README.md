# SKALA Shop

Java 21과 Spring Boot로 구현한 쇼핑몰 백엔드와 순수 HTML, CSS,
JavaScript 프론트엔드를 함께 관리하는 모노레포입니다. 백엔드는 하나의
애플리케이션과 PostgreSQL을 사용하지만 도메인별 코드와 테이블 소유권을
분리한 모듈러 모놀리식으로 구성합니다.

## 저장소 구조

~~~text
./            Spring Boot 백엔드, Docker 및 배포 설정
frontend/     정적 쇼핑몰 프론트엔드
docs/         백엔드 아키텍처 문서
deploy/       EC2, Nginx, Certbot 배포 구성
~~~

## 기술 스택

백엔드:

- Java 21
- Spring Boot 3.5
- Spring Modulith
- Spring Security 및 JWT 쿠키 인증
- Springdoc OpenAPI 및 Swagger UI
- Spring Data JPA
- PostgreSQL
- Flyway
- Gradle
- Testcontainers

프론트엔드:

- HTML5
- CSS3
- Vanilla JavaScript
- Vercel

## 모듈

~~~text
auth        계정, 비밀번호 해시, JWT
member      고객 프로필
catalog     상품
wallet      포인트 계정과 원장
order       주문, 주문항목, 취소
storefront  여러 모듈을 조합하는 HTTP 호환 계층
common      예외와 페이지 응답 등 최소 공통 코드
~~~

자세한 의존 방향과 MSA 전환 방법은
[docs/architecture.md](docs/architecture.md)를 참고합니다.

## 백엔드 로컬 실행

필수 조건은 Java 21, Docker 및 Docker Compose입니다.

~~~bash
docker compose -f compose.local.yml up -d
./gradlew bootRun
~~~

기본값:

~~~text
API         http://localhost:8080
Swagger UI  http://localhost:8080/swagger-ui.html
OpenAPI     http://localhost:8080/v3/api-docs
PostgreSQL  localhost:5432/skala_shop
DB user     skala
DB password skala
~~~

환경변수 이름은 [.env.example](.env.example)에 정리되어 있습니다. 실제
비밀값 파일은 Git에 커밋하지 않습니다.

## 프론트엔드 로컬 실행

백엔드를 `http://localhost:8080`에서 실행한 뒤 저장소 루트에서 정적 서버를
실행합니다.

~~~bash
python3 -m http.server 3000 --directory frontend
~~~

브라우저에서 `http://localhost:3000`으로 접속합니다. API 주소는
[frontend/config.js](frontend/config.js)에서 설정하며 자세한 사용 방법은
[frontend/README.md](frontend/README.md)를 참고합니다.

## 테스트

~~~bash
./gradlew test
~~~

테스트는 모듈 경계, Flyway, 회원가입과 로그인, 역할 기반 권한, 실제 쿠키
CSRF 흐름, 상품 등록, 주문과 포인트 차감의 원자성, 동시 재시도 멱등성,
부분 취소 환급을 실제 PostgreSQL로 검증합니다.

## 주요 API

~~~text
POST   /api/customers
GET    /api/auth/csrf
POST   /api/customers/login
POST   /api/customers/logout
GET    /api/customers/me
GET    /api/customers/{customerId}
GET    /api/customers/list
PUT    /api/customers/me
DELETE /api/customers/me

GET    /api/products
GET    /api/products/{productId}
POST   /api/products
PUT    /api/products/{productId}
DELETE /api/products/{productId}

POST   /api/orders
GET    /api/orders/me
POST   /api/orders/cancellations

POST   /api/customers/order
POST   /api/customers/cancel
~~~

브라우저는 먼저 `GET /api/auth/csrf`를 호출한 뒤, 응답 토큰을 상태 변경
요청의 `X-XSRF-TOKEN` 헤더로 전달해야 합니다. 쿠키 전송을 위해 프론트의
`fetch`에는 `credentials: "include"`를 사용합니다.

모든 주문과 취소 요청에는 UUID 형식의 `X-Idempotency-Key` 헤더가
필수입니다. 같은 사용자가 같은 키와 요청 내용으로 재시도하면 최초 결과를
반환하고, 같은 키를 다른 내용에 재사용하면 `409 IDEMPOTENCY_CONFLICT`를
반환합니다.

상품 등록·수정·삭제와 고객 목록 조회는 `ADMIN` 역할만 호출할 수 있습니다.
초기 관리자 생성은 기본적으로 꺼져 있으며, 필요할 때만
`BOOTSTRAP_ADMIN_*` 환경변수로 한 번 활성화한 뒤 다시 비활성화합니다.

## 운영 배포

- 프론트엔드: Vercel
- 백엔드: AWS EC2의 Docker Compose
- Reverse proxy 및 TLS: Nginx와 Certbot 컨테이너
- 데이터베이스: 비공개 RDS PostgreSQL
- 이미지 저장소: Docker Hub
- CI/CD: GitHub Actions

Vercel에서 이 저장소를 가져올 때 Root Directory를 `frontend`로 지정합니다.
운영 API 주소와 쿠키·CORS 설정은 실제 프론트 및 API 도메인에 맞춰야 합니다.

EC2 최초 설정과 인증서 발급 방법은
[deploy/README.md](deploy/README.md)를 참고합니다.
