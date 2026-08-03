#!/bin/sh

set -eu

if [ "$#" -ne 1 ]; then
    echo "usage: deploy.sh <image-tag>" >&2
    exit 2
fi

IMAGE_TAG_VALUE=$1
case "$IMAGE_TAG_VALUE" in
    *[!A-Za-z0-9._-]*|'')
        echo "invalid image tag" >&2
        exit 2
        ;;
esac

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.prod.yml"
INFRA_ENV="$DEPLOY_DIR/.env.infra"
RELEASE_ENV="$DEPLOY_DIR/.release"
PREVIOUS_RELEASE="$DEPLOY_DIR/.release.previous"

for required_file in "$INFRA_ENV" "$DEPLOY_DIR/.env.app"; do
    if [ ! -f "$required_file" ]; then
        echo "missing required file: $required_file" >&2
        exit 1
    fi
done

compose() {
    docker compose \
        --env-file "$INFRA_ENV" \
        --env-file "$RELEASE_ENV" \
        -f "$COMPOSE_FILE" "$@"
}

if [ -f "$RELEASE_ENV" ]; then
    cp "$RELEASE_ENV" "$PREVIOUS_RELEASE"
fi
printf 'IMAGE_TAG=%s\n' "$IMAGE_TAG_VALUE" > "$RELEASE_ENV"

compose pull backend
compose up -d --no-deps backend

CONTAINER_ID=$(compose ps -q backend)
if [ -z "$CONTAINER_ID" ]; then
    echo "backend container was not created" >&2
    "$SCRIPT_DIR/rollback.sh"
    exit 1
fi

attempt=1
while [ "$attempt" -le 24 ]; do
    STATUS=$(docker inspect --format '{{.State.Health.Status}}' "$CONTAINER_ID")
    if [ "$STATUS" = "healthy" ]; then
        compose up -d nginx certbot-renew
        compose exec -T nginx nginx -s reload
        echo "deployed image tag: $IMAGE_TAG_VALUE"
        exit 0
    fi
    if [ "$STATUS" = "unhealthy" ]; then
        break
    fi
    sleep 5
    attempt=$((attempt + 1))
done

echo "backend failed its health check; restoring previous image" >&2
"$SCRIPT_DIR/rollback.sh"
exit 1
