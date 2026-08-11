# 테스트 증적 안내

테스트를 다시 실행하거나 제출 자료를 확인할 때 이 디렉터리를 시작점으로 사용합니다.

- [commands.md](commands.md): 전체·선택·운영 테스트 실행 명령
- [scenarios.md](scenarios.md): 테스트 목적, 준비 조건, 동작과 통과 기준
- [results/2026-08-10.md](results/2026-08-10.md): 기준 커밋에서 실제 실행한 결과
- [Notification Service 분리 결과](results/2026-08-11-notification-service.md): 독립 DB·Kafka Inbox 검증
- [load/read-only-load.mjs](load/read-only-load.mjs): 데이터를 변경하지 않는 HTTP 부하 측정 코드
- [run-regression.sh](run-regression.sh): 로컬 전체 회귀 검증 실행 코드

빠른 전체 검증은 저장소 루트에서 다음과 같이 실행합니다.

```bash
sh tests/run-regression.sh
```

Docker가 실행 중이어야 하며, Backend 통합 테스트는 Testcontainers가 PostgreSQL 17을
자동으로 시작합니다. 운영 데이터를 변경하는 live E2E와 운영 부하 검증은 전체 회귀
명령에 포함하지 않았습니다.
