#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.prod.yml"
INFRA_ENV="$DEPLOY_DIR/.env.infra"
RELEASE_ENV="$DEPLOY_DIR/.release"

for required_file in "$INFRA_ENV" "$RELEASE_ENV" "$DEPLOY_DIR/.env.app"; do
    if [ ! -f "$required_file" ]; then
        echo "missing required file: $required_file" >&2
        exit 1
    fi
done

set -a
. "$INFRA_ENV"
set +a

if [ -z "${API_DOMAIN:-}" ] || [ -z "${LETSENCRYPT_EMAIL:-}" ]; then
    echo "API_DOMAIN and LETSENCRYPT_EMAIL are required" >&2
    exit 1
fi

compose() {
    docker compose \
        --env-file "$INFRA_ENV" \
        --env-file "$RELEASE_ENV" \
        -f "$COMPOSE_FILE" "$@"
}

cleanup() {
    compose --profile bootstrap stop nginx-bootstrap >/dev/null 2>&1 || true
    compose --profile bootstrap rm -f nginx-bootstrap >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

compose --profile bootstrap up -d nginx-bootstrap
compose --profile tools run --rm certbot certonly \
    --webroot \
    --webroot-path /var/www/certbot \
    --email "$LETSENCRYPT_EMAIL" \
    --agree-tos \
    --no-eff-email \
    --domain "$API_DOMAIN"

cleanup
trap - EXIT INT TERM
compose up -d

echo "TLS certificate issued for $API_DOMAIN"
