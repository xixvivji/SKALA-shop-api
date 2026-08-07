#!/bin/sh

set -eu

if [ "$#" -ne 2 ]; then
    echo "usage: bootstrap-tls.sh <release-id> <repository@sha256:digest>" >&2
    exit 2
fi

RELEASE_ID_VALUE=$1
IMAGE_REF_VALUE=$2
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/release-lib.sh"

require_shared_files
acquire_deployment_lock

LETSENCRYPT_EMAIL_VALUE=$(infra_env_value LETSENCRYPT_EMAIL)
case "$LETSENCRYPT_EMAIL_VALUE" in
    ''|*[[:space:]]*|@*|*@|*@*@*)
        echo "LETSENCRYPT_EMAIL must be a plain email address" >&2
        exit 1
        ;;
    *@*) ;;
    *)
        echo "LETSENCRYPT_EMAIL must be a plain email address" >&2
        exit 1
        ;;
esac

bootstrap_metadata="$STATE_DIR/.bootstrap.$$"
write_release_metadata "$bootstrap_metadata" "$RELEASE_ID_VALUE" "$IMAGE_REF_VALUE"

cleanup() {
    exit_code=$?
    trap - EXIT INT TERM HUP
    compose_for "$bootstrap_metadata" --profile bootstrap stop nginx-bootstrap \
        >/dev/null 2>&1 || true
    compose_for "$bootstrap_metadata" --profile bootstrap rm -f nginx-bootstrap \
        >/dev/null 2>&1 || true
    rm -f "$bootstrap_metadata"
    exit "$exit_code"
}
trap cleanup EXIT
trap 'exit 130' INT TERM HUP

compose_for "$bootstrap_metadata" --profile bootstrap up -d nginx-bootstrap
compose_for "$bootstrap_metadata" --profile tools run --rm certbot certonly \
    --webroot \
    --webroot-path /var/www/certbot \
    --email "$LETSENCRYPT_EMAIL_VALUE" \
    --agree-tos \
    --no-eff-email \
    --non-interactive \
    --keep-until-expiring \
    --domain "$INFRA_API_DOMAIN"

compose_for "$bootstrap_metadata" --profile bootstrap stop nginx-bootstrap
compose_for "$bootstrap_metadata" --profile bootstrap rm -f nginx-bootstrap
rm -f "$bootstrap_metadata"
trap - EXIT INT TERM HUP

# Preserve the same flock descriptor across exec so no deploy/rollback can enter
# between certificate issuance and the first HTTPS deployment.
SKALA_DEPLOY_LOCK_INHERITED=$LOCK_FILE
export SKALA_DEPLOY_LOCK_INHERITED
exec "$SCRIPT_DIR/deploy.sh" "$RELEASE_ID_VALUE" "$IMAGE_REF_VALUE"
