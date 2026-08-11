# 테스트 시나리오

## 핵심 도메인과 API

| ID | 준비 조건 | 실행 | 통과 기준 | 대표 테스트 코드 |
| --- | --- | --- | --- | --- |
| AUTH-01 | 신규 고객과 관리자 | 가입, 로그인, 갱신, 로그아웃, CSRF·권한 위반 요청 | Cookie 회전, BCrypt, 역할 인가와 공통 오류 계약 유지 | `ShoppingJourneyIntegrationTests`, `AuthenticationHardeningIntegrationTests` |
| VAL-01 | 잘못된 body·query·UUID | 필드·객체 검증과 DB 제약 위반 요청 | 400/404/409와 오류 코드·필드 메시지 반환 | `ShoppingJourneyIntegrationTests`, `GlobalExceptionHandlerTests` |
| CAT-01 | 카테고리·상품·SKU | 목록, 옵션, 금액 경계, 상태 변경 조회 | 정렬·페이지·정밀도·활성 상태 계약 유지 | `ShoppingJourneyIntegrationTests` |
| INV-01 | SKU 재고와 멱등성 키 | 입고, 조정, 삭제 경합, 같은 명령 재시도 | 수량·이동 원장·최초 응답 재생 일치 | `InventoryApplicationServiceTests`, `ShoppingJourneyIntegrationTests` |
| ORD-01 | 회원, 배송지, 장바구니, SKU | 다중 상품 주문과 배송지 누락 주문 | 정상 주문은 snapshot 저장, 누락은 400이며 데이터 무변경 | `ShoppingJourneyIntegrationTests`, `OrderApplicationServiceTests` |
| PAY-01 | 포인트·Fake PG 혼합 결제 | 준비, 승인, 실패, 중복 웹훅, 부분 환불 | 주문·결제 원장·포인트 환급 합계 일치 | `ShoppingJourneyIntegrationTests`, Payment 단위 테스트 |
| RET-01 | 배송 완료 주문 | 부분 반품, 회수, 검수, 승인·거절·재신청 | 허용된 상태 전이만 수행하고 환불·재고 복원 일치 | `ReturnApplicationServiceTests`, `ReturnConcurrencyIntegrationTests` |
| OUT-01 | 주문 트랜잭션과 Outbox | 이벤트 저장, 발행 성공·실패·재시도 | 비즈니스 데이터와 이벤트 원자 저장, 상태 전이 일치 | `OutboxApplicationServiceTests`, `OutboxEventPublisherTests` |

## 동시성·멱등성·일관성

| ID | 시나리오 | 통과 기준 | 대표 테스트 코드 |
| --- | --- | --- | --- |
| CON-01 | 마지막 재고 1개에 고객 2명이 동시에 주문 | 1건만 성공, 나머지 409, 재고 0, 주문·포인트 원장 각 1건 | `ordersLastUnitOnlyOnceUnderConcurrency` |
| CON-02 | 재고 5개에 고객 20명이 동시에 주문 | 성공 5건, `INSUFFICIENT_STOCK` 15건, 재고 0, 주문·재고·포인트 원장 각 5건 | `preventsOversellingWhenTwentyCustomersCompeteForFiveUnits` |
| CON-03 | 같은 주문 수량을 서로 다른 키로 동시에 취소 | 취소 가능 수량을 넘지 않고 재고·환불 원장 합계 일치 | `ShoppingJourneyIntegrationTests` 동시 취소 테스트 |
| CON-04 | 같은 주문 항목에 서로 다른 키로 동시에 반품 신청 | 활성 반품 예약 합계가 구매 수량을 넘지 않음 | `ReturnConcurrencyIntegrationTests` |
| IDEM-01 | 동일 키·동일 요청과 동일 키·다른 요청 | 동일 요청은 최초 snapshot 재생, 다른 요청은 409 | 주문·취소·재고·결제 멱등 테스트 |
| TX-01 | 재고·잔액 부족 또는 환불 단계 실패 | 주문·재고·포인트·결제 중 일부만 반영되지 않고 전체 rollback | `ShoppingJourneyIntegrationTests` 실패 원자성 테스트 |

## Search Service와 Kafka

| ID | 준비 조건 | 실행 | 통과 기준 | 대표 테스트 코드 |
| --- | --- | --- | --- | --- |
| KAFKA-01 | Embedded Kafka와 상품 이벤트 | 실제 broker에 정상 JSON 발행 | Search consumer가 key/header/payload를 읽고 색인 서비스 호출 | `KafkaEventIntegrationTests.consumesProductEventThroughEmbeddedKafkaBroker` |
| KAFKA-02 | Embedded Kafka와 잘못된 상품 이벤트 | 소비 실패 이벤트 발행 | 설정 횟수만큼 재시도한 뒤 `.DLT` topic으로 원본 key·payload 이동 | `KafkaEventIntegrationTests.sendsInvalidProductEventToDltAfterConfiguredRetries` |
| SEARCH-01 | Search Service 정상·장애 응답 | 상품 검색, DB fallback, 재색인 | 정상은 원격 결과, 장애는 PostgreSQL fallback, 재색인 불가는 503 | `ProductSearchServiceTests`, Backend 검색 통합 테스트 |
| NOTIFY-01 | 주문·재입고 이벤트 중복 전달 | 같은 메시지를 두 번 발행 | Inbox fingerprint와 알림이 한 번만 저장 | `NotificationServiceIntegrationTests` |
| NOTIFY-02 | 잘못된 지원 이벤트와 회원별 조회 | 잘못된 JSON 발행, 다른 회원 JWT 조회 | 재시도 후 DLT 이동, 자신의 알림만 조회·읽음 처리 | `NotificationServiceIntegrationTests` |

## 구조·문서·관측

| ID | 실행 | 통과 기준 | 대표 테스트 코드 |
| --- | --- | --- | --- |
| MOD-01 | Spring Modulith 구조 검사 | 모듈 간 내부 패키지 직접 참조 없음 | `ModulithStructureTests` |
| DOC-01 | OpenAPI 문서 생성 | 모든 operation에 한국어 tag·summary·description, 민감정보 비노출 | `ShoppingJourneyIntegrationTests.exposesOpenApiDocumentation` |
| OBS-01 | prod profile로 API·management 포트 기동 | 8080에서 metrics 비노출, 9090 내부 health·Prometheus 정상 | `ManagementPortIntegrationTests` |
| OPS-01 | 모니터링 clean start | Prometheus target UP, Grafana datasource·dashboard와 secret 권한 정상 | `deploy/tests/monitoring-config-test.sh` |
| OPS-02 | candidate 배포 실패 조건 주입 | current 미승격 또는 known-good rollback, mutable tag 거부 | `deploy/tests/release-flow-test.sh` |
| OPS-03 | 공개 URL 읽기 smoke | Frontend·health·Grafana·API·OpenAPI 200, 외부 metrics 404 | `deploy/tools/smoke-production.mjs` |

## Frontend 브라우저

각 시나리오는 Desktop Chromium과 Pixel 5 viewport에서 실행해 총 10개가 됩니다.

| ID | 실행 | 통과 기준 |
| --- | --- | --- |
| FE-01 | 가입 → 장바구니 → 배송지 → 다중 주문 → 포인트 내역 | 고객 쇼핑 흐름과 화면 상태 일치 |
| FE-02 | 관리자 로그인 → 주문 조회 → 배송 상태 변경 | 관리자 권한과 변경 이력 표시 |
| FE-03 | SKU 미선택·품절 옵션 주문 시도 | 구매 차단과 구체적인 안내 표시 |
| FE-04 | 전액 카드 Fake PG 주문 | `PAYMENT_PENDING → PAID`, 포인트 잔액 무변경 |
| FE-05 | 계정 전환 중 늦은 401·리뷰 응답 도착 | 새 세션과 최신 화면을 과거 응답이 덮어쓰지 않음 |

## 읽기 부하

`LOAD-01`은 카테고리·상품·검색 API를 지정한 시간과 동시성으로 반복 호출합니다.
전체 요청의 오류율과 p50·p95·p99·최대 지연을 계산하고, 오류율 또는 p95가 설정한
기준을 넘으면 종료 코드 1을 반환합니다. 쓰기 API는 호출하지 않습니다.
