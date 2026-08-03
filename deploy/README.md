# EC2 Deployment

EC2 단일인스턴스 Docker Compose에서 Backend, Nginx와 Certbot을 실행하고
PostgreSQL은 RDS를 사용합니다. Backend 교체 중 짧은 API 중단을 허용합니다.

## AWS 준비

- EC2, Elastic IP, Docker Engine, Docker Compose plugin
- 암호화된 EBS
- EC2 보안 그룹의 80과 443 허용
- SSH 22는 관리자 IP로 제한하거나 SSM 사용
- 비공개 RDS PostgreSQL
- RDS 보안 그룹은 EC2 보안 그룹에서 오는 5432만 허용
- RDS 자동 백업, 저장소 자동 확장과 삭제 방지
- api.example.com이 EC2 Elastic IP를 가리키도록 DNS 설정

EC2 IAM Role을 사용하고 AWS Access Key를 파일에 저장하지 않습니다.

## EC2 파일 준비

배포 파일 기본 위치는 /opt/skala-shop/deploy입니다.

~~~bash
cd /opt/skala-shop/deploy
cp .env.infra.example .env.infra
cp .env.app.example .env.app
cp .release.example .release
chmod 600 .env.infra .env.app .release
chmod 700 scripts/*.sh
~~~

.env.infra:

~~~text
BACKEND_IMAGE=<dockerhub-username>/skala-shop-api
API_DOMAIN=api.example.com
LETSENCRYPT_EMAIL=admin@example.com
~~~

.env.app:

~~~text
DB_URL=jdbc:postgresql://<rds-endpoint>:5432/skala_shop?sslmode=require
DB_USERNAME=<application-user>
DB_PASSWORD=<password>
JWT_SECRET=<32-byte-or-longer-random-secret>
CORS_ALLOWED_ORIGINS=https://example.com,https://www.example.com
~~~

.release에는 최초 실행할 Docker Hub 이미지의 커밋 SHA를 기록합니다.
저장소가 비공개라면 EC2에서 read-only 토큰으로 로그인합니다.

~~~bash
docker login
~~~

## 최초 인증서 발급

api.example.com DNS가 EC2를 가리키고 외부에서 80 포트에 접근 가능해야
합니다.

~~~bash
./scripts/bootstrap-tls.sh
~~~

HTTP 전용 Nginx를 잠시 실행하여 인증서를 발급한 뒤 HTTPS 스택으로
전환합니다. Certbot은 12시간마다 갱신을 확인하고 Nginx는 6시간마다 reload
합니다. Certbot에는 Docker socket을 마운트하지 않습니다.

## GitHub Actions Secrets

~~~text
EC2_HOST
EC2_USER
EC2_SSH_PRIVATE_KEY
EC2_KNOWN_HOSTS
DOCKERHUB_USERNAME
DOCKERHUB_PUSH_TOKEN
DOCKERHUB_PULL_TOKEN
~~~

GitHub Actions Variables에는 다음 값을 등록합니다.

~~~text
DOCKERHUB_IMAGE=<dockerhub-username>/skala-shop-api
PRODUCTION_DEPLOY_ENABLED=true
~~~

PRODUCTION_DEPLOY_ENABLED를 true로 설정하기 전에는 main 브랜치에서도
테스트만 실행하고 이미지 게시와 EC2 배포는 건너뜁니다. Docker Hub, EC2와
도메인 설정을 모두 마친 뒤 활성화합니다.

main push 시 Java 21 테스트, Docker Hub 이미지 push, EC2 이미지 교체,
healthcheck, Nginx reload 순서로 배포합니다. 현재 workflow는 x86 EC2용
linux/amd64 이미지를 만듭니다. Graviton이면 linux/arm64로 변경합니다.

Docker Hub의 push 토큰에는 read/write 권한을, EC2에 전달하는 pull 토큰에는
read-only 권한만 부여합니다.

## 수동 배포와 롤백

~~~bash
./scripts/deploy.sh <git-commit-sha>
./scripts/rollback.sh
~~~

healthcheck 실패 시 이전 이미지 태그를 복구합니다. Flyway의 파괴적 변경은
이미지 롤백만으로 복구되지 않으므로 expand/contract 방식을 사용합니다.

## 상태 확인

~~~bash
docker compose \
  --env-file .env.infra \
  --env-file .release \
  -f compose.prod.yml ps
~~~

Vercel 프론트는 example.com, API는 api.example.com처럼 동일한 등록
도메인의 하위 도메인을 권장합니다. JWT 쿠키를 보내려면 프론트 fetch에
credentials: "include"가 필요합니다.
