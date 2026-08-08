#!/usr/bin/env bash
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y docker.io docker-compose-v2 curl jq
systemctl enable --now docker
usermod -aG docker ubuntu
install -d -o ubuntu -g ubuntu -m 755 /opt/skala-shop-platform

# Elasticsearch가 많은 메모리 매핑을 사용할 수 있도록 호스트 커널 값을 고정합니다.
cat >/etc/sysctl.d/99-skala-shop-platform.conf <<'EOF'
vm.max_map_count=262144
EOF
sysctl --system

# 플랫폼 Compose와 환경파일은 GitHub OIDC + SSM 배포 단계에서 전달합니다.
touch /var/lib/cloud/instance/platform-bootstrap-complete
