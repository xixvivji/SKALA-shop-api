# EC2 Deployment

EC2 단일 인스턴스의 Docker Compose에서 Backend, Nginx, Certbot을 실행하고
PostgreSQL은 RDS를 사용합니다. Backend 교체 중 짧은 API 중단은 허용합니다.

## 현재 구성 상태

저장소에는 다음 운영 구성이 구현되어 있습니다.

- production Docker Compose와 Backend healthcheck
- Nginx HTTPS reverse proxy와 인증 요청 IP rate limit
- Certbot 최초 발급 및 12시간 주기 갱신 컨테이너
- Docker Hub digest 기반 불변 이미지 배포
- candidate/current/known-good 릴리스 상태와 실패 시 자동 복구
- 동시 배포를 막는 GitHub Actions concurrency와 호스트 `flock`
- 로컬에서 실행하는 릴리스 전환·실패·롤백 시뮬레이션

구성 파일이 준비된 것과 실제 인프라가 배포된 것은 별개입니다. EC2, RDS,
Elastic IP, DNS, Docker Hub, Vercel 프로젝트와 GitHub Secrets/Variables는 운영자가
실제 계정에 생성·연결해야 합니다. `PRODUCTION_DEPLOY_ENABLED`의 기본 동작은
배포 비활성화이며 외부 리소스와 최초 TLS 설정을 확인한 뒤에만 활성화합니다.

## AWS와 호스트 준비

- EC2, Elastic IP, Docker Engine, Docker Compose plugin
- `flock` 명령을 제공하는 util-linux 패키지
- 암호화된 EBS
- EC2 보안 그룹의 80과 443 허용
- SSH 22는 관리자 IP로 제한하거나 SSM 사용
- 비공개 RDS PostgreSQL
- RDS 보안 그룹은 EC2 보안 그룹에서 오는 5432만 허용
- RDS 자동 백업, 저장소 자동 확장과 삭제 방지
- `api.example.com`이 EC2 Elastic IP를 가리키도록 DNS 설정

EC2 IAM Role을 사용하고 AWS Access Key를 파일에 저장하지 않습니다.

## EC2 디렉터리 구조

~~~text
/opt/skala-shop/
├── deploy/
│   ├── .env.infra            # 모든 릴리스가 공유하는 인프라 설정
│   └── .env.app              # Backend 비밀 환경변수
├── releases/
│   └── <release-id>/
│       ├── compose.prod.yml
│       ├── nginx/
│       └── scripts/
└── state/
    ├── current.env           # 현재 이미지 digest와 구성 경로
    ├── known-good.env        # 직전 정상 이미지 digest와 구성 경로
    ├── candidate.env         # 진행 중 작업 및 SIGKILL 복구 표식
    ├── deploy-*.env          # 배포의 crash-recovery journal
    ├── rollback-*.env        # 수동 롤백의 crash-recovery journal
    ├── failed.env            # 최근 실패 릴리스
    └── deploy.lock           # flock이 사용하는 일반 파일
~~~

Workflow는 매 실행마다 고유한 `releases/<commit-run-attempt>` 디렉터리를
만듭니다. 공용 Compose/Nginx 파일을 덮어쓰지 않으므로 실행 중인 배포와 다음
배포의 구성이 섞이지 않습니다. `current.env`와 `known-good.env`는 Backend의
불변 digest뿐 아니라 같은 릴리스의 Compose/Nginx 경로도 함께 기록합니다.
`deploy/.release.example`은 자동 생성되는 상태 메타데이터의 형식 참고용이며,
운영자가 `.release` 파일을 직접 유지하지 않습니다.

처음 한 번 다음 디렉터리와 공유 설정 파일을 준비합니다.

~~~bash
sudo install -d -m 755 /opt/skala-shop/deploy /opt/skala-shop/releases /opt/skala-shop/state
sudo chown -R "$USER":"$USER" /opt/skala-shop
cp deploy/.env.infra.example /opt/skala-shop/deploy/.env.infra
cp deploy/.env.app.example /opt/skala-shop/deploy/.env.app
chmod 600 /opt/skala-shop/deploy/.env.infra /opt/skala-shop/deploy/.env.app
~~~

`.env.infra` 예시:

~~~text
API_DOMAIN=api.example.com
FRONTEND_ORIGIN=https://example.com
LETSENCRYPT_EMAIL=admin@example.com
APP_ENV_FILE=/opt/skala-shop/deploy/.env.app
~~~

`FRONTEND_ORIGIN`은 슬래시로 끝나지 않는 실제 Vercel origin 하나를 정확히
입력합니다. 이 값은 Nginx가 직접 반환하는 429 응답의 CORS 허용 origin입니다.

`.env.app` 예시:

~~~text
DB_URL=jdbc:postgresql://<rds-endpoint>:5432/skala_shop?sslmode=require
DB_USERNAME=<application-user>
DB_PASSWORD=<password>
JWT_SECRET=<32-byte-or-longer-random-secret>
JWT_COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://example.com,https://www.example.com
BOOTSTRAP_ADMIN_ENABLED=false
BOOTSTRAP_ADMIN_LOGIN_ID=
BOOTSTRAP_ADMIN_PASSWORD=
~~~

최초 관리자 계정이 필요할 때만 `BOOTSTRAP_ADMIN_ENABLED=true`와 12자 이상의
비밀번호를 설정해 한 번 기동합니다. 생성이 끝나면 다시 `false`로 바꾸고
관리자 ID와 비밀번호 환경변수도 제거합니다. 이후 비밀번호 변경은 관리자
비밀번호 변경 API를 사용합니다.

비공개 Docker Hub 저장소라면 EC2에서 read-only 토큰으로 로그인합니다.

~~~bash
docker login
~~~

## 최초 인증서 발급

`api.example.com` DNS가 EC2를 가리키고 외부에서 80 포트에 접근 가능해야
합니다. 먼저 한 릴리스의 `compose.prod.yml`, `nginx/`, `scripts/`를 같은
버전으로 `/opt/skala-shop/releases/<release-id>`에 복사하고 이미지의 실제
Docker Hub digest를 확인합니다.

~~~bash
/opt/skala-shop/releases/<release-id>/scripts/bootstrap-tls.sh \
  <release-id> \
  '<dockerhub-user>/skala-shop-api@sha256:<64-hex-digest>'
~~~

HTTP 전용 Nginx로 인증서를 발급한 뒤 같은 릴리스의 정상 배포 절차를
실행합니다. Certbot은 12시간마다 갱신을 확인하고 Nginx는 6시간마다 reload
합니다. Certbot에는 Docker socket을 마운트하지 않습니다.

## GitHub Actions 설정

브랜치별 동작은 다음과 같습니다.

- `feature/*` push와 모든 PR: `.github/workflows/ci.yml`에서 백엔드, 프론트와
  릴리스 흐름을 검증합니다.
- `develop` push: CI만 실행하고 운영에는 배포하지 않습니다.
- `main` push: `.github/workflows/deploy-prod.yml`을 실행합니다. 단,
  `PRODUCTION_DEPLOY_ENABLED`가 `true`일 때만 이미지 게시와 EC2 배포를 진행합니다.
- `frontend/**`만 변경된 main push는 백엔드 production workflow를 건너뛰며
  Vercel이 프론트 배포를 담당합니다.

GitHub Actions Secrets:

~~~text
EC2_HOST
EC2_USER
EC2_SSH_PRIVATE_KEY
EC2_KNOWN_HOSTS
DOCKERHUB_USERNAME
DOCKERHUB_PUSH_TOKEN
DOCKERHUB_PULL_TOKEN
~~~

GitHub Actions Variables:

~~~text
DOCKERHUB_IMAGE=<dockerhub-username>/skala-shop-api
PRODUCTION_DEPLOY_ENABLED=true
~~~

`PRODUCTION_DEPLOY_ENABLED=true`로 설정하기 전에는 main 브랜치에서도 테스트만
실행하고 이미지 게시와 EC2 배포는 건너뜁니다. Docker Hub, EC2, 도메인과 최초
인증서 설정을 마친 뒤 활성화합니다.

main push 배포는 다음 순서로 동작합니다.

1. Java 21 테스트, 프론트 검사와 릴리스 흐름 시뮬레이션을 실행합니다.
2. `linux/amd64` 이미지를 Docker Hub에 게시하고 registry digest를 받습니다.
3. Compose/Nginx/스크립트를 실행별 버전 디렉터리에 복사합니다.
4. digest로 Candidate Backend를 기동하고 healthcheck를 확인합니다.
5. Candidate 구성으로 일회성 컨테이너의 실제 `nginx -t`를 실행합니다.
6. Nginx와 인증서 갱신 컨테이너를 같은 구성으로 전환하고 live `nginx -t`와
   reload까지 성공한 뒤에만 current로 승격합니다.

Workflow의 production concurrency는 `cancel-in-progress: false`이므로 main 배포가
겹치면 취소하지 않고 순서대로 실행합니다. 호스트에서는 `flock`이 자동 배포,
수동 배포, 롤백과 TLS bootstrap을 직렬화합니다. lock 파일이 남아 있어도 파일
descriptor 잠금이 해제된 상태라면 다음 실행을 막지 않습니다.

Docker Hub push 토큰에는 read/write 권한을, EC2 pull 토큰에는 read-only
권한만 부여합니다. Graviton EC2를 사용하면 workflow 플랫폼을
`linux/arm64`로 변경합니다.

## 수동 배포와 롤백

수동 배포도 먼저 고유한 버전 디렉터리에 완전한 릴리스 파일을 준비해야 합니다.

~~~bash
/opt/skala-shop/releases/<release-id>/scripts/deploy.sh \
  <release-id> \
  '<dockerhub-user>/skala-shop-api@sha256:<64-hex-digest>'
~~~

수동 롤백은 현재 릴리스의 스크립트에서 실행합니다.

~~~bash
CURRENT_RELEASE_DIR=$(sed -n 's/^RELEASE_DIR=//p' /opt/skala-shop/state/current.env)
"$CURRENT_RELEASE_DIR/scripts/rollback.sh"
~~~

Candidate의 pull, Backend 기동, healthcheck, `nginx -t`, Nginx 전환 중 실패하면
Backend digest와 Compose/Nginx 구성을 같은 이전 릴리스 쌍으로 복구합니다.
첫 배포에는 이전 릴리스가 없으므로 자동 복구 대상도 없습니다. Flyway의 파괴적
변경은 이미지/구성 롤백으로 되돌릴 수 없으므로 expand/contract 방식을
사용합니다.

`current.env`, `known-good.env`, `failed.env`가 가리키는 릴리스 디렉터리는
삭제하면 안 됩니다. 그 외 오래된 버전 디렉터리는 상태 파일을 확인한 뒤 별도의
보존 정책으로 정리할 수 있습니다.

## 상태 확인

~~~bash
CURRENT_METADATA=/opt/skala-shop/state/current.env
CURRENT_COMPOSE=$(sed -n 's/^RELEASE_COMPOSE_FILE=//p' "$CURRENT_METADATA")
docker compose \
  --project-name skala-shop \
  --env-file /opt/skala-shop/deploy/.env.infra \
  --env-file "$CURRENT_METADATA" \
  -f "$CURRENT_COMPOSE" ps
~~~

로컬에서 배포 상태 전환을 빠르게 검증할 수 있습니다.

~~~bash
sh deploy/tests/release-flow-test.sh
~~~

## 프록시, CORS와 요청 제한

Vercel 프론트는 `example.com`, API는 `api.example.com`처럼 같은 등록 도메인의
하위 도메인을 권장합니다. JWT 쿠키를 보내려면 프론트 fetch에
`credentials: "include"`가 필요합니다. 상태 변경 전 `GET /api/auth/csrf`를
호출하고 응답 token을 `X-XSRF-TOKEN` 헤더로 전송합니다.

Nginx는 회원가입, 로그인과 학습용 비밀번호 초기화 경로에 IP 기준 요청 제한을
적용합니다. CORS preflight인 `OPTIONS`는 제한 카운터에서 제외합니다. Nginx가
429를 직접 반환할 때는 정확히 일치하는 `FRONTEND_ORIGIN`에만 credentials와
`Retry-After` 노출 헤더를 보냅니다. Spring CORS도 `Retry-After`를 노출합니다.

현재 구성은 인터넷 요청이 단일 Nginx에 직접 도달하는 구조를 전제로 하며,
클라이언트가 보낸 `X-Forwarded-For`는 신뢰하지 않고 `$remote_addr`로
덮어씁니다. 앞에 ALB/CDN을 추가하면 trusted proxy 범위와 real IP 설정을 먼저
구성해야 합니다. 여러 EC2로 확장할 때는 애플리케이션 요청 제한 저장소도 Redis
같은 공유 저장소로 교체합니다.
