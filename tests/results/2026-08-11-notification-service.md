# Notification Service 분리 검증 결과

기준일: 2026-08-11

| 검증 | 결과 | 확인 내용 |
| --- | --- | --- |
| 전체 Gradle 테스트 | 151/151 통과 | Backend 134, Search Service 13, Notification Service 4 |
| Notification Service 통합 | 4/4 통과 | PostgreSQL 17, Embedded Kafka, Inbox, DLT, JWT API, OpenAPI |
| 재입고 Outbox 집중 테스트 | 통과 | 재입고 구독 대상 이벤트가 업무 트랜잭션과 함께 Outbox에 저장 |
| 운영 Compose 렌더링 | 통과 | 전용 PostgreSQL 비공개 포트, service health와 환경변수 계약 |
| Docker 이미지 빌드 | 통과 | `notification-service/Dockerfile`, Java 21 runtime |
| GitHub Actions YAML | 통과 | CI Compose 검증과 multi-arch 이미지 publish workflow |

실행한 주요 명령은 다음과 같습니다.

```bash
./gradlew test --no-build-cache --no-daemon
./gradlew :notification-service:test --no-build-cache --no-daemon
./gradlew :test \
  --tests 'com.skala.shopping.ShoppingJourneyIntegrationTests.marksStockAlertNotifiedAndPreservesTrackingOnPartialUpdate' \
  --tests 'com.skala.shopping.outbox.internal.OutboxEventRecorderTests' \
  --no-build-cache --no-daemon
docker build -f notification-service/Dockerfile -t skala-shop-notification:test .
docker compose -f deploy/compose.notification.yml config --quiet
```

Notification Service의 운영 공개 URL과 프론트 연결은 이 검증 범위에 포함하지
않았습니다. 운영에 연결하려면 multi-arch 이미지를 게시한 뒤 사설 포트와 Nginx
`/api/notifications/**` 라우팅을 추가로 구성해야 합니다.
