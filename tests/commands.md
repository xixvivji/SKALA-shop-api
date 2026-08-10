# 테스트 실행 명령

모든 명령은 저장소 루트에서 실행합니다.

## 전체 회귀 검증

```bash
sh tests/run-regression.sh
```

의존성이 이미 설치되어 있으면 npm 재설치만 생략할 수 있습니다.

```bash
SKIP_NPM_CI=true sh tests/run-regression.sh
```

## Backend와 Search Service

```bash
# Backend와 Search Service 전체
./gradlew clean test --no-build-cache --no-daemon

# Backend만
./gradlew :test --no-build-cache --no-daemon

# Search Service만
./gradlew :search-service:test --no-build-cache --no-daemon

# 모듈 경계
./gradlew :test --tests 'com.skala.shopping.ModulithStructureTests' --no-daemon

# 재고 5개에 고객 20명 동시 주문
./gradlew :test \
  --tests 'com.skala.shopping.ShoppingJourneyIntegrationTests.preventsOversellingWhenTwentyCustomersCompeteForFiveUnits' \
  --no-daemon

# 마지막 재고 주문
./gradlew :test \
  --tests 'com.skala.shopping.ShoppingJourneyIntegrationTests.allowsOnlyOneCustomerToOrderTheLastUnit' \
  --no-daemon

# 주문 취소 동시성
./gradlew :test \
  --tests 'com.skala.shopping.ShoppingJourneyIntegrationTests.allowsOnlyOneConcurrentCancellationForOnePurchasedUnit' \
  --no-daemon

# 동시 반품 수량 예약
./gradlew :test --tests 'com.skala.shopping.returns.internal.ReturnConcurrencyIntegrationTests' --no-daemon

# Kafka 정상 소비와 재시도 후 DLT
./gradlew :search-service:test \
  --tests 'com.skala.shopping.searchservice.messaging.KafkaEventIntegrationTests' \
  --no-daemon

# 운영 API 포트와 Actuator 관리 포트 분리
./gradlew :test --tests 'com.skala.shopping.ManagementPortIntegrationTests' --no-daemon
```

다중 프로젝트이므로 선택 실행에는 `:test` 또는 `:search-service:test`를 명시합니다.
루트에서 `test --tests ...`만 사용하면 다른 하위 프로젝트에도 같은 필터가 적용될 수
있습니다.

HTML 결과는 `build/reports/tests/test/index.html`과
`search-service/build/reports/tests/test/index.html`에서 확인합니다.

## Frontend

```bash
npm --prefix frontend ci
node --check frontend/runtime-config.js
node --check frontend/config.js
node --check frontend/js/api.js
node --check frontend/js/app.js
node frontend/scripts/validate-static.mjs
npm --prefix frontend run build
npm --prefix frontend run test:e2e
```

운영 데이터를 변경하는 E2E는 명시적으로 활성화할 때만 실행합니다.

```bash
LIVE_E2E_ENABLED=true \
E2E_BASE_URL=https://skala-shop-bice.vercel.app \
npm --prefix frontend run test:e2e:live
```

## 배포와 모니터링

```bash
sh deploy/tests/monitoring-config-test.sh
sh deploy/tests/release-flow-test.sh

SKALA_FRONTEND_ORIGIN=https://skala-shop-bice.vercel.app \
SKALA_API_BASE_URL=https://api-3-39-64-119.sslip.io \
node deploy/tools/smoke-production.mjs
```

## 읽기 전용 부하 측정

로컬 기본값은 15초, 동시 요청 5개, 허용 오류율 1%, 허용 p95 1초입니다.

```bash
node tests/load/read-only-load.mjs

LOAD_SCENARIO=search \
LOAD_DURATION_SECONDS=30 \
LOAD_CONCURRENCY=10 \
node tests/load/read-only-load.mjs
```

운영 주소는 실수로 부하를 주지 않도록 별도 동의 변수가 필요합니다.

```bash
ALLOW_PRODUCTION_LOAD=true \
LOAD_BASE_URL=https://api-3-39-64-119.sslip.io \
LOAD_DURATION_SECONDS=10 \
LOAD_CONCURRENCY=3 \
LOAD_MAX_P95_MS=1500 \
node tests/load/read-only-load.mjs
```

이 코드는 카테고리·상품·검색 GET만 호출하며 회원·주문·재고를 변경하지 않습니다.
