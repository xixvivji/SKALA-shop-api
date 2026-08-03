#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.prod.yml"
INFRA_ENV="$DEPLOY_DIR/.env.infra"
RELEASE_ENV="$DEPLOY_DIR/.release"
PREVIOUS_RELEASE="$DEPLOY_DIR/.release.previous"
FAILED_RELEASE="$DEPLOY_DIR/.release.failed"

if [ ! -f "$PREVIOUS_RELEASE" ]; then
    echo "no previous release is available" >&2
    exit 1
fi

if [ -f "$RELEASE_ENV" ]; then
    cp "$RELEASE_ENV" "$FAILED_RELEASE"
fi
cp "$PREVIOUS_RELEASE" "$RELEASE_ENV"

docker compose \
    --env-file "$INFRA_ENV" \
    --env-file "$RELEASE_ENV" \
    -f "$COMPOSE_FILE" \
    up -d --no-deps backend

echo "restored previous backend release"
