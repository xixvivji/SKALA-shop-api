#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 5 ]]; then
    echo "usage: deploy-platform-via-ssm.sh <aws-region> <kafka|search> <instance-id> <private-ip> <image@sha256:digest>" >&2
    exit 2
fi

AWS_REGION_VALUE=$1
ROLE=$2
INSTANCE_ID=$3
PRIVATE_IP=$4
IMAGE_REF=$5

[[ "$AWS_REGION_VALUE" =~ ^[a-z]{2}-[a-z]+-[0-9]+$ ]]
[[ "$ROLE" == "kafka" || "$ROLE" == "search" ]]
[[ "$INSTANCE_ID" =~ ^i-[0-9a-f]+$ ]]
[[ "$PRIVATE_IP" =~ ^10\.|^172\.(1[6-9]|2[0-9]|3[01])\.|^192\.168\. ]]
[[ "$IMAGE_REF" =~ ^[^[:space:]@]+@sha256:[0-9a-f]{64}$ ]]

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.$ROLE.yml"
COMPOSE_BASE64=$(base64 < "$COMPOSE_FILE" | tr -d '\n')

if [[ "$ROLE" == "kafka" ]]; then
    ROLE_SETUP=$(cat <<EOF
CLUSTER_ID=\$(sed -n 's/^KAFKA_CLUSTER_ID=//p' "\$ENV_FILE" 2>/dev/null || true)
if [[ -z "\$CLUSTER_ID" ]]; then
    CLUSTER_ID=\$(openssl rand -base64 16 | tr -d '=\n')
fi
cat >"\$ENV_FILE" <<ENV
KAFKA_IMAGE_REF=$IMAGE_REF
KAFKA_ADVERTISED_HOST=$PRIVATE_IP
KAFKA_CLUSTER_ID=\$CLUSTER_ID
ENV
EOF
    )
    HEALTH_CONTAINER=skala-shop-kafka-kafka-1
else
    ROLE_SETUP=$(cat <<EOF
sysctl -w vm.max_map_count=262144
printf '%s\n' 'vm.max_map_count=262144' >/etc/sysctl.d/99-skala-shop-platform.conf
cat >"\$ENV_FILE" <<ENV
ELASTICSEARCH_IMAGE_REF=$IMAGE_REF
ES_JAVA_OPTS=-Xms1g -Xmx1g
ENV
EOF
    )
    HEALTH_CONTAINER=skala-shop-search-elasticsearch-1
fi

REMOTE_SCRIPT=$(cat <<EOF
set -euo pipefail
INSTALL_DIR=/opt/skala-shop-platform
ENV_FILE=\$INSTALL_DIR/.env
install -d -m 755 -o ubuntu -g ubuntu "\$INSTALL_DIR"
printf '%s' '$COMPOSE_BASE64' | base64 -d >"\$INSTALL_DIR/compose.yml"
$ROLE_SETUP
chmod 600 "\$ENV_FILE"
chown -R ubuntu:ubuntu "\$INSTALL_DIR"
cd "\$INSTALL_DIR"
docker compose --env-file "\$ENV_FILE" -f compose.yml pull
docker compose --env-file "\$ENV_FILE" -f compose.yml up -d --remove-orphans
for attempt in \$(seq 1 36); do
    STATUS=\$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' '$HEALTH_CONTAINER' 2>/dev/null || true)
    if [[ "\$STATUS" == "healthy" ]]; then
        docker compose --env-file "\$ENV_FILE" -f compose.yml ps
        exit 0
    fi
    sleep 5
done
docker compose --env-file "\$ENV_FILE" -f compose.yml ps
docker compose --env-file "\$ENV_FILE" -f compose.yml logs --tail=120
exit 1
EOF
)

REMOTE_SCRIPT_BASE64=$(printf '%s' "$REMOTE_SCRIPT" | base64 | tr -d '\n')
REMOTE_COMMAND="printf '%s' '$REMOTE_SCRIPT_BASE64' | base64 -d | bash"
PARAMETERS=$(jq -n --arg command "$REMOTE_COMMAND" '{commands: [$command]}')

COMMAND_ID=$(aws ssm send-command \
    --region "$AWS_REGION_VALUE" \
    --document-name AWS-RunShellScript \
    --instance-ids "$INSTANCE_ID" \
    --comment "Deploy SKALA Shop $ROLE platform" \
    --timeout-seconds 900 \
    --parameters "$PARAMETERS" \
    --query 'Command.CommandId' \
    --output text)

set +e
aws ssm wait command-executed \
    --region "$AWS_REGION_VALUE" \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID"
WAIT_STATUS=$?
set -e

INVOCATION=$(aws ssm get-command-invocation \
    --region "$AWS_REGION_VALUE" \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --output json)
jq -r '.StandardOutputContent' <<<"$INVOCATION"
jq -r '.StandardErrorContent' <<<"$INVOCATION" >&2
COMMAND_STATUS=$(jq -r '.Status' <<<"$INVOCATION")
[[ "$WAIT_STATUS" -eq 0 && "$COMMAND_STATUS" == "Success" ]]
