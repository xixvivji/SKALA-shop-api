# SKALA Shop API

Java 21과 Spring Boot로 구현한 쇼핑몰 백엔드입니다. 하나의 애플리케이션과
PostgreSQL을 사용하지만 도메인별 코드와 테이블 소유권을 분리한 모듈러
모놀리식으로 구성합니다.

## 기술 스택

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

## 로컬 실행

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

## 테스트

~~~bash
./gradlew test
~~~

테스트는 모듈 경계, Flyway, 회원가입과 로그인, 상품 등록, 주문과 포인트
차감의 원자성, 멱등성, 부분 취소 환급을 실제 PostgreSQL로 검증합니다.

## 주요 API

~~~text
POST   /api/customers
POST   /api/customers/login
POST   /api/customers/logout
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

주문과 취소 요청에는 가능하면 UUID 형식의 X-Idempotency-Key 헤더를
전달합니다.

## 운영 배포

- 프론트엔드: Vercel
- 백엔드: AWS EC2의 Docker Compose
- Reverse proxy 및 TLS: Nginx와 Certbot 컨테이너
- 데이터베이스: 비공개 RDS PostgreSQL
- 이미지 저장소: Docker Hub
- CI/CD: GitHub Actions

EC2 최초 설정과 인증서 발급 방법은
[deploy/README.md](deploy/README.md)를 참고합니다.
