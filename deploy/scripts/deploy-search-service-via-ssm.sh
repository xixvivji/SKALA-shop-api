#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 7 ]]; then
    echo "usage: deploy-search-service-via-ssm.sh <aws-region> <instance-id> <private-ip> <kafka-host:port> <catalog-base-url> <search-image@digest> <elasticsearch-image@digest>" >&2
    exit 2
fi

AWS_REGION_VALUE=$1
INSTANCE_ID=$2
PRIVATE_IP=$3
KAFKA_BOOTSTRAP_SERVERS_VALUE=$4
CATALOG_BASE_URL_VALUE=$5
SEARCH_IMAGE_REF=$6
ELASTICSEARCH_IMAGE_REF_VALUE=$7

[[ "$AWS_REGION_VALUE" =~ ^[a-z]{2}-[a-z]+-[0-9]+$ ]]
[[ "$INSTANCE_ID" =~ ^i-[0-9a-f]+$ ]]
[[ "$PRIVATE_IP" =~ ^10\.|^172\.(1[6-9]|2[0-9]|3[01])\.|^192\.168\. ]]
[[ "$KAFKA_BOOTSTRAP_SERVERS_VALUE" =~ ^(10\.|172\.(1[6-9]|2[0-9]|3[01])\.|192\.168\.)[0-9.]+:[0-9]+$ ]]
[[ "$CATALOG_BASE_URL_VALUE" =~ ^https://[A-Za-z0-9.-]+(:[0-9]+)?$ ]]
[[ "$SEARCH_IMAGE_REF" =~ ^[^[:space:]@]+@sha256:[0-9a-f]{64}$ ]]
[[ "$ELASTICSEARCH_IMAGE_REF_VALUE" =~ ^[^[:space:]@]+@sha256:[0-9a-f]{64}$ ]]

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_BASE64=$(base64 < "$DEPLOY_DIR/compose.search.yml" | tr -d '\n')

REMOTE_SCRIPT=$(cat <<EOF
set -euo pipefail
INSTALL_DIR=/opt/skala-shop-platform
CURRENT_ENV=\$INSTALL_DIR/.env
CURRENT_COMPOSE=\$INSTALL_DIR/compose.yml
CANDIDATE_ENV=\$INSTALL_DIR/.env.candidate
CANDIDATE_COMPOSE=\$INSTALL_DIR/compose.candidate.yml
KNOWN_GOOD_ENV=\$INSTALL_DIR/.env.known-good
KNOWN_GOOD_COMPOSE=\$INSTALL_DIR/compose.known-good.yml
PREVIOUS_AVAILABLE=0
DEPLOY_STARTED=0
DEPLOY_COMPLETE=0

install -d -m 755 -o ubuntu -g ubuntu "\$INSTALL_DIR"
if [[ -f "\$CURRENT_ENV" && -f "\$CURRENT_COMPOSE" ]]; then
    cp "\$CURRENT_ENV" "\$KNOWN_GOOD_ENV"
    cp "\$CURRENT_COMPOSE" "\$KNOWN_GOOD_COMPOSE"
    PREVIOUS_AVAILABLE=1
fi

printf '%s' '$COMPOSE_BASE64' | base64 -d >"\$CANDIDATE_COMPOSE"
cat >"\$CANDIDATE_ENV" <<ENV
ELASTICSEARCH_IMAGE_REF=$ELASTICSEARCH_IMAGE_REF_VALUE
ES_JAVA_OPTS=-Xms768m -Xmx768m
SEARCH_SERVICE_IMAGE_REF=$SEARCH_IMAGE_REF
SEARCH_SERVICE_BIND_IP=$PRIVATE_IP
KAFKA_BOOTSTRAP_SERVERS=$KAFKA_BOOTSTRAP_SERVERS_VALUE
SEARCH_KAFKA_TOPIC=skala-shop.domain-events
SEARCH_KAFKA_GROUP_ID=skala-shop-search-v1
CATALOG_BASE_URL=$CATALOG_BASE_URL_VALUE
ENV
chmod 600 "\$CANDIDATE_ENV"
chown ubuntu:ubuntu "\$CANDIDATE_ENV" "\$CANDIDATE_COMPOSE"

restore_previous() {
    if [[ "\$PREVIOUS_AVAILABLE" -ne 1 ]]; then
        return 1
    fi
    docker compose --env-file "\$KNOWN_GOOD_ENV" -f "\$KNOWN_GOOD_COMPOSE" up -d --remove-orphans
    cp "\$KNOWN_GOOD_ENV" "\$CURRENT_ENV"
    cp "\$KNOWN_GOOD_COMPOSE" "\$CURRENT_COMPOSE"
}

finish() {
    exit_code=\$?
    trap - EXIT INT TERM HUP
    if [[ "\$exit_code" -ne 0 && "\$DEPLOY_STARTED" -eq 1 && "\$DEPLOY_COMPLETE" -eq 0 ]]; then
        echo 'Search Service deployment failed; restoring previous search platform' >&2
        if ! restore_previous; then
            docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" down --remove-orphans || true
            echo 'No previous search platform was available for rollback' >&2
        fi
    fi
    rm -f "\$CANDIDATE_ENV" "\$CANDIDATE_COMPOSE"
    exit "\$exit_code"
}
trap finish EXIT
trap 'exit 130' INT TERM HUP

DEPLOY_STARTED=1
docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" config --quiet
docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" pull
docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" up -d --remove-orphans

for attempt in \$(seq 1 36); do
    ES_ID=\$(docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" ps -q elasticsearch)
    SEARCH_ID=\$(docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" ps -q search-service)
    ES_STATUS=\$(docker inspect --format '{{.State.Health.Status}}' "\$ES_ID" 2>/dev/null || true)
    SEARCH_STATUS=\$(docker inspect --format '{{.State.Health.Status}}' "\$SEARCH_ID" 2>/dev/null || true)
    if [[ "\$ES_STATUS" == 'healthy' && "\$SEARCH_STATUS" == 'healthy' ]]; then
        break
    fi
    if [[ "\$attempt" -eq 36 ]]; then
        docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" ps
        docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" logs --tail=160
        exit 1
    fi
    sleep 5
done

SEARCH_ID=\$(docker compose --env-file "\$CANDIDATE_ENV" -f "\$CANDIDATE_COMPOSE" ps -q search-service)
docker exec "\$SEARCH_ID" curl --fail --silent --get \
    --data-urlencode 'query=healthcheck-no-result' \
    --data-urlencode 'page=0' \
    --data-urlencode 'size=1' \
    http://localhost:8081/internal/search/products >/dev/null

mv "\$CANDIDATE_ENV" "\$CURRENT_ENV"
mv "\$CANDIDATE_COMPOSE" "\$CURRENT_COMPOSE"
DEPLOY_COMPLETE=1
trap - EXIT INT TERM HUP

docker compose --env-file "\$CURRENT_ENV" -f "\$CURRENT_COMPOSE" ps
echo 'deployed Search Service immutable image: $SEARCH_IMAGE_REF'
EOF
)

REMOTE_SCRIPT_BASE64=$(printf '%s' "$REMOTE_SCRIPT" | base64 | tr -d '\n')
REMOTE_COMMAND="printf '%s' '$REMOTE_SCRIPT_BASE64' | base64 -d | bash"
[[ "${#REMOTE_COMMAND}" -le 23000 ]]
if [[ "${SEARCH_DEPLOY_DRY_RUN:-false}" == "true" ]]; then
    printf 'validated Search Service SSM payload: %s bytes\n' "${#REMOTE_COMMAND}"
    exit 0
fi
PARAMETERS=$(jq -n --arg command "$REMOTE_COMMAND" '{commands: [$command]}')

COMMAND_ID=$(aws ssm send-command \
    --region "$AWS_REGION_VALUE" \
    --document-name AWS-RunShellScript \
    --instance-ids "$INSTANCE_ID" \
    --comment "Deploy SKALA Shop Search Service" \
    --timeout-seconds 900 \
    --parameters "$PARAMETERS" \
    --query 'Command.CommandId' \
    --output text)

INVOCATION=
COMMAND_STATUS=Pending
for attempt in $(seq 1 180); do
    if INVOCATION=$(aws ssm get-command-invocation \
        --region "$AWS_REGION_VALUE" \
        --command-id "$COMMAND_ID" \
        --instance-id "$INSTANCE_ID" \
        --output json 2>/dev/null); then
        COMMAND_STATUS=$(jq -r '.Status' <<<"$INVOCATION")
        case "$COMMAND_STATUS" in
            Success|Cancelled|TimedOut|Failed|Undeliverable|Terminated)
                break
                ;;
        esac
    fi
    sleep 5
done

[[ -n "$INVOCATION" ]]
jq -r '.StandardOutputContent' <<<"$INVOCATION"
jq -r '.StandardErrorContent' <<<"$INVOCATION" >&2
[[ "$COMMAND_STATUS" == "Success" ]]
