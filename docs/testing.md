# 테스트 전략

SKALA Shop은 빠른 단위 테스트, 실제 PostgreSQL 통합 테스트, 브라우저 E2E와 운영
smoke를 구분합니다. 테스트마다 검증하려는 실패 종류가 다르므로 하나의 테스트
계층으로 대체하지 않습니다.

## 테스트 구성

| 계층 | 도구 | 검증 범위 |
| --- | --- | --- |
| 단위 | JUnit 5, Mockito | 도메인 규칙, 계산, 상태 전이 |
| 모듈 경계 | Spring Modulith Test | 허용되지 않은 패키지 의존 |
| 백엔드 통합 | Spring Boot Test, Testcontainers | Security, MVC, JPA, Flyway, 실제 PostgreSQL |
| 검색 서비스 | JUnit, Embedded Kafka | 이벤트 header·JSON 계약, 소비·재시도 경계, 색인 로직 |
| 알림 서비스 | Testcontainers, Embedded Kafka | 독립 DB, Inbox 멱등성, DLT, 사용자 데이터 격리 |
| 동시성 | JUnit + PostgreSQL | 마지막 재고 주문, 주문·취소 경합, 잠금 |
| API 계약 | Springdoc/OpenAPI test | endpoint와 스키마 문서 |
| 프론트 정적 | Node.js 검사 script | 파일 참조, 문법, 금액 경계, 접근성 회귀 조건 |
| 브라우저 E2E | Playwright Chromium | 고객·관리자 화면, 데스크톱·모바일 |
| 모니터링 구성 | Docker Compose, promtool, jq | 내부 scrape, provisioning, 외부 노출 경계 |
| 배포 시뮬레이션 | POSIX shell | candidate/current/known-good 전환과 롤백 |
| 운영 smoke | Node.js, Playwright | Vercel·EC2·RDS와 Grafana 실제 연결 |

## 백엔드 테스트

```bash
./gradlew test
```

현재 백엔드 134개 테스트는 다음을 포함합니다.

- Spring Modulith 모듈 경계
- Flyway V1~V26 적용과 JPA schema validation
- 회원가입·로그인·로그아웃·Refresh Token 회전·권한·CSRF·BCrypt
- 손상된 Access Cookie와 정상 Refresh Cookie가 함께 온 갱신 요청
- Validation과 공통 오류 JSON 계약
- 로그인·회원가입·비밀번호 초기화 요청 제한
- 상품·카테고리·재고·장바구니·배송지
- SKU 기반 다중 상품 주문, 주문 항목별 부분 취소, 배송 상태와 포인트 원장
- 모든 주문 경로의 배송지 필수 검증과 실패 시 주문·재고·포인트 무변경
- 혼합 결제 취소의 포인트·Fake PG 분할 환급과 Payment 원장 정합성
- Fake PG 준비·승인·실패·중복 웹훅, 승인 최초 결과 재생과 부분 환불
- 배송 후 부분 반품, 거절 후 재신청, 동시 초과 신청 차단과 상태 변경 멱등성
- Outbox 이벤트 저장과 Kafka publisher 성공·실패 전달 계약
- Search Service HTTP 호출과 PostgreSQL 장애 폴백
- 쿠폰 사용 이력, 할인 결제액 기준 환불과 쿠폰 재사용 방지
- 구매 인증 리뷰, 공개 응답 개인정보 비노출과 리뷰 권한
- 재입고 이벤트 알림 상태와 배송 추적 부분 수정
- 주문·취소·재고 멱등 재시도
- 포인트·재고·주문의 원자성 및 실패 rollback
- 마지막 재고 동시 주문, 재고 5개에 고객 20명 고경합 주문, 전액 카드 주문
  멱등성과 주문·취소·반품 동시성
- JPA 낙관적 잠금 충돌의 `409 CONCURRENT_MODIFICATION` 응답
- Swagger/OpenAPI와 민감정보 비노출 API 로그
- 운영 API 8080과 Actuator management 9090 분리, management endpoint 접근 정책

Gradle이 `UP-TO-DATE`로 표시되는 상황에서 전체를 실제로 다시 실행하려면 다음을
사용합니다.

```bash
./gradlew test --rerun-tasks
```

Testcontainers가 PostgreSQL 컨테이너를 시작하므로 Docker가 실행 중이어야 합니다.

## Search Service 테스트

```bash
./gradlew :search-service:test
```

Search Service 13개 테스트는 Catalog snapshot 페이지 조회, Elasticsearch 색인·삭제·
재색인, Kafka health, 이벤트 타입 필터, 잘못된 이벤트의 재시도 후 DLT 이동과 실제
Embedded Kafka broker를 통과한 `ProductSearchChanged` 소비를 검증합니다. 전체
`./gradlew test`는 백엔드 134개와 Search Service 13개를 함께 실행합니다.

## Notification Service 테스트

```bash
./gradlew :notification-service:test
```

실제 PostgreSQL 17과 Embedded Kafka로 동일 이벤트 재전달 시 Inbox와 알림이 한 번만
저장되는지, 잘못된 지원 이벤트가 DLT로 이동하는지, 인증된 회원이 자신의 알림만
조회하고 읽음 처리할 수 있는지를 검증합니다. 이 테스트도 전체 `./gradlew test`에
포함됩니다.

전체 실행 명령, 시나리오별 준비 조건·통과 기준과 기준일 실행 결과는
[테스트 증적](../tests/README.md)에 별도로 정리했습니다.

## 프론트 정적 검사

```bash
node --check frontend/runtime-config.js
node --check frontend/config.js
node --check frontend/js/api.js
node --check frontend/js/app.js
node frontend/scripts/validate-static.mjs
npm --prefix frontend run build
```

정적 검사는 단순 문법 외에도 다음 회귀 조건을 확인합니다.

- HTML에서 참조하는 로컬 파일과 중복 ID
- JavaScript import 경로
- 상품 가격 × 최대 주문 수량의 센트 정밀도
- 비동기 form event 사용
- 초기 세션 복구와 새 로그인 경합 방지
- 모든 계정 종속 loader의 세션 세대 검증
- 옵션 SKU 선택, 품절 옵션 차단과 주문 항목 ID 취소 계약
- 리뷰 대화상자의 오래된 조회·저장 응답 차단
- 재고 부족 이후 상품 재조회
- 더보기 버튼이 사라질 때 키보드 포커스 이동

## 로컬 브라우저 E2E

```bash
npm --prefix frontend ci
npx --prefix frontend playwright install chromium
npm --prefix frontend run test:e2e
```

Playwright가 정적 서버를 자동으로 열고 API 응답을 모킹합니다. 동일한 시나리오를
Desktop Chrome과 Pixel 5 viewport에서 실행합니다.

- 회원가입 → 장바구니 → 배송지 → 다중 주문 → 포인트 원장
- 관리자 로그인 → 전체 주문 → 배송 상태 변경 → 변경 이력
- 옵션 미선택·품절 SKU 주문 차단과 Fake PG `PAYMENT_PENDING → PAID`
- 계정 전환 뒤 늦은 401 응답과 리뷰 조회 순서 경합

위 시나리오를 데스크톱·모바일에서 각각 실행하는 10개 테스트이며, 실제 운영
데이터에 의존하지 않아 CI에서 반복 실행할 수 있습니다.

## 실제 운영 E2E

```bash
LIVE_E2E_ENABLED=true \
E2E_BASE_URL=https://skala-shop-bice.vercel.app \
npm --prefix frontend run test:e2e:live
```

실제 Vercel, EC2 API와 RDS를 사용해 브라우저 고객 흐름을 확인합니다.

```text
임시 회원가입
→ 장바구니
→ 배송지 저장
→ 주문
→ 전량 취소
→ 포인트 환급 확인
→ 임시 고객 비활성화
```

운영 주문과 비활성 고객 기록은 감사·원장 일관성을 위해 DB에 남습니다. 따라서
일반 CI에서는 실행하지 않고 `LIVE_E2E_ENABLED=true`를 명시할 때만 실행합니다.

관리자 읽기 흐름까지 확인하려면 비밀값을 저장소 파일이나 명령 인수에 넣지 말고
환경변수로 전달합니다.

```bash
SKALA_ADMIN_ID=<관리자-ID> \
SKALA_ADMIN_PASSWORD=<안전하게-주입> \
LIVE_E2E_ENABLED=true \
E2E_BASE_URL=https://skala-shop-bice.vercel.app \
npm --prefix frontend run test:e2e:live
```

`Production smoke` workflow의 mutating E2E는 같은 관리자 비밀값을 사용해 다음
운영 상태 전이를 추가 검증합니다.

```text
회원가입 → 로그인 → 장바구니 → 포인트 0원 사용 주문
→ 테스트 카드 결제 승인
→ 상품 준비 → 배송 중 → 배송 완료
→ 반품 접수 → 회수 → 검수 → 승인 → 카드 환불
→ 고객 반품 내역 확인 → 임시 고객 비활성화
```

`load_catalog` 입력을 함께 선택하면 카테고리·상품·옵션 SKU·옵션별 재고를 먼저
중복 없이 적재합니다.

## 공개 smoke

공개 페이지, health, Grafana health, 카테고리, 상품과 OpenAPI를 읽기 전용으로
확인합니다. `/actuator/prometheus`가 404인지도 함께 검사해 원본 메트릭이 외부에
노출되지 않았음을 확인합니다.

```bash
SKALA_FRONTEND_ORIGIN=https://skala-shop-bice.vercel.app \
SKALA_API_BASE_URL=https://api-3-39-64-119.sslip.io \
node deploy/tools/smoke-production.mjs
```

## 배포 시뮬레이션

```bash
sh deploy/tests/monitoring-config-test.sh
sh deploy/tests/release-flow-test.sh
```

실제 AWS나 Docker Hub를 변경하지 않고 다음을 검사합니다.

- 운영 Compose, Prometheus 설정과 Grafana provisioning 정적 검증
- 중복 배포 lock
- 정상 candidate 승격
- Backend·Prometheus·Grafana health 또는 scrape target 실패 rollback
- Nginx 검사 실패 rollback
- 중단된 배포 journal 복구
- 수동 rollback과 rollback 실패 보상
- mutable image tag 거부

## GitHub Actions

- 모든 PR과 `feature/*`, `develop` push: 백엔드, 프론트, 모니터링 구성과 배포 시뮬레이션
- `main` push: 전체 검사 후 Docker image 게시와 EC2 운영 배포
- `Production smoke`: 수동 공개 API·Grafana smoke, 선택적 live E2E
- Vercel: PR preview와 `main` production 배포

운영 live E2E는 GitHub production environment의 보호 규칙과 secret을 사용할 수
있습니다. `run_mutating_e2e`를 선택하지 않으면 읽기 전용 smoke만 실행합니다.
