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
| 동시성 | JUnit + PostgreSQL | 마지막 재고 주문, 주문·취소 경합, 잠금 |
| API 계약 | Springdoc/OpenAPI test | endpoint와 스키마 문서 |
| 프론트 정적 | Node.js 검사 script | 파일 참조, 문법, 금액 경계, 접근성 회귀 조건 |
| 브라우저 E2E | Playwright Chromium | 고객·관리자 화면, 데스크톱·모바일 |
| 배포 시뮬레이션 | POSIX shell | candidate/current/known-good 전환과 롤백 |
| 운영 smoke | Node.js, Playwright | Vercel·EC2·RDS 실제 연결 |

## 백엔드 테스트

```bash
./gradlew test
```

현재 103개 테스트가 다음을 포함합니다.

- Spring Modulith 모듈 경계
- Flyway V1~V24 적용과 JPA schema validation
- 회원가입·로그인·로그아웃·Refresh Token 회전·권한·CSRF·BCrypt
- Validation과 공통 오류 JSON 계약
- 로그인·회원가입·비밀번호 초기화 요청 제한
- 상품·카테고리·재고·장바구니·배송지
- SKU 기반 다중 상품 주문, 부분 취소, 배송 상태와 포인트 원장
- Fake PG 준비·승인·실패·중복 웹훅·재처리와 부분 환불
- 배송 후 반품 상태 머신, 결제 수단별 환불과 재고 복원
- Outbox 성공·재시도·DEAD 처리와 Kafka 발행 계약
- Elasticsearch 색인·검색과 PostgreSQL 장애 폴백
- 쿠폰 사용 이력, 할인 결제액 기준 환불과 쿠폰 재사용 방지
- 구매 인증 리뷰, 공개 응답 개인정보 비노출과 리뷰 권한
- 재입고 이벤트 알림 상태와 배송 추적 부분 수정
- 주문·취소·재고 멱등 재시도
- 포인트·재고·주문의 원자성 및 실패 rollback
- 마지막 재고 동시 주문과 주문·취소 동시성
- Swagger/OpenAPI와 민감정보 비노출 API 로그

Gradle이 `UP-TO-DATE`로 표시되는 상황에서 전체를 실제로 다시 실행하려면 다음을
사용합니다.

```bash
./gradlew test --rerun-tasks
```

Testcontainers가 PostgreSQL 컨테이너를 시작하므로 Docker가 실행 중이어야 합니다.

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

이 테스트는 실제 운영 데이터에 의존하지 않아 CI에서 반복 실행할 수 있습니다.

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

공개 페이지, health, 카테고리, 상품과 OpenAPI가 응답하는지만 읽기 전용으로
확인합니다.

```bash
SKALA_FRONTEND_ORIGIN=https://skala-shop-bice.vercel.app \
SKALA_API_BASE_URL=https://api-3-39-64-119.sslip.io \
node deploy/tools/smoke-production.mjs
```

## 배포 시뮬레이션

```bash
sh deploy/tests/release-flow-test.sh
```

실제 AWS나 Docker Hub를 변경하지 않고 다음을 검사합니다.

- 중복 배포 lock
- 정상 candidate 승격
- Backend health 또는 Nginx 검사 실패 rollback
- 중단된 배포 journal 복구
- 수동 rollback과 rollback 실패 보상
- mutable image tag 거부

## GitHub Actions

- 모든 PR과 `feature/*`, `develop` push: 백엔드, 프론트와 배포 시뮬레이션
- `main` push: 전체 검사 후 Docker image 게시와 EC2 운영 배포
- `Production smoke`: 수동 공개 smoke, 선택적 live E2E
- Vercel: PR preview와 `main` production 배포

운영 live E2E는 GitHub production environment의 보호 규칙과 secret을 사용할 수
있습니다. `run_mutating_e2e`를 선택하지 않으면 읽기 전용 smoke만 실행합니다.
