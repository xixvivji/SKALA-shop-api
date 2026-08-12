#!/bin/sh

set -eu

TEST_SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SOURCE_DEPLOY_DIR=$(CDPATH= cd -- "$TEST_SCRIPT_DIR/.." && pwd)
TEMP_BASE=${TMPDIR:-/tmp}
TEST_ROOT=$(mktemp -d "${TEMP_BASE%/}/skala-release-test.XXXXXX")
SIM_STATE="$TEST_ROOT/mock-state"

cleanup() {
    rm -rf "$TEST_ROOT"
}
trap cleanup EXIT
trap 'exit 130' INT TERM HUP

fail() {
    echo "release flow test failed: $*" >&2
    exit 1
}

assert_equal() {
    expected=$1
    actual=$2
    description=$3
    if [ "$expected" != "$actual" ]; then
        fail "$description (expected '$expected', got '$actual')"
    fi
}

metadata_value() {
    metadata_file=$1
    metadata_key=$2
    sed -n "s/^${metadata_key}=//p" "$metadata_file"
}

assert_metadata_release() {
    metadata_file=$1
    expected_release=$2
    description=$3
    [ -f "$metadata_file" ] || fail "$description metadata is missing"
    actual_release=$(metadata_value "$metadata_file" RELEASE_ID)
    assert_equal "$expected_release" "$actual_release" "$description"
}

assert_log_order() {
    first=$1
    second=$2
    first_line=$(awk -v pattern="$first" '$0 == pattern { print NR; exit }' "$SIM_STATE/log")
    second_line=$(awk -v pattern="$second" '$0 == pattern { print NR; exit }' "$SIM_STATE/log")
    [ -n "$first_line" ] || fail "missing log entry: $first"
    [ -n "$second_line" ] || fail "missing log entry: $second"
    [ "$first_line" -lt "$second_line" ] \
        || fail "expected '$first' before '$second'"
}

prepare_release() {
    release_id=$1
    release_dir="$TEST_ROOT/releases/$release_id"
    mkdir -p "$release_dir"
    cp "$SOURCE_DEPLOY_DIR/compose.prod.yml" "$release_dir/compose.prod.yml"
    cp -R "$SOURCE_DEPLOY_DIR/monitoring" "$release_dir/monitoring"
    cp -R "$SOURCE_DEPLOY_DIR/nginx" "$release_dir/nginx"
    cp -R "$SOURCE_DEPLOY_DIR/scripts" "$release_dir/scripts"
    chmod 700 "$release_dir"/scripts/*.sh
}

prepare_legacy_release() {
    release_id=$1
    prepare_release "$release_id"
    release_dir="$TEST_ROOT/releases/$release_id"
    rm -rf "$release_dir/monitoring"
    printf '%s\n' \
        'name: skala-shop' \
        'services:' \
        '  backend:' \
        '    image: ${BACKEND_IMAGE_REF}' \
        '  redis:' \
        '    image: redis:alpine' \
        '  nginx:' \
        '    image: nginx:alpine' \
        '  certbot-renew:' \
        '    image: certbot/certbot:latest' \
        > "$release_dir/compose.prod.yml"
    printf '%s\n' \
        'server {' \
        '    listen 443 ssl;' \
        '    location / { proxy_pass http://backend:8080; }' \
        '}' \
        > "$release_dir/nginx/api.conf.template"
}

write_metadata() {
    target_file=$1
    release_id=$2
    image_ref=$3
    release_dir="$TEST_ROOT/releases/$release_id"
    {
        printf 'RELEASE_ID=%s\n' "$release_id"
        printf 'BACKEND_IMAGE_REF=%s\n' "$image_ref"
        printf 'RELEASE_DIR=%s\n' "$release_dir"
        printf 'RELEASE_COMPOSE_FILE=%s\n' "$release_dir/compose.prod.yml"
        printf 'RELEASE_NGINX_CONFIG_FILE=%s\n' \
            "$release_dir/nginx/api.conf.template"
    } > "$target_file"
}

mkdir -p "$TEST_ROOT/deploy" "$TEST_ROOT/releases" "$TEST_ROOT/state" \
    "$TEST_ROOT/bin" "$SIM_STATE"

printf '%s\n' \
    'API_DOMAIN=api.example.com' \
    'FRONTEND_ORIGIN=https://shop.example.com' \
    'LETSENCRYPT_EMAIL=admin@example.com' \
    "APP_ENV_FILE=$TEST_ROOT/deploy/.env.app" \
    > "$TEST_ROOT/deploy/.env.infra"
printf '%s\n' \
    'DB_URL=jdbc:postgresql://database.example.com:5432/skala_shop?sslmode=require' \
    'DB_USERNAME=skala_app' \
    'DB_PASSWORD=test-only-password' \
    'JWT_SECRET=test-only-jwt-secret-that-is-at-least-32-characters' \
    'JWT_COOKIE_SECURE=true' \
    'CORS_ALLOWED_ORIGINS=https://shop.example.com' \
    'BOOTSTRAP_ADMIN_ENABLED=false' \
    > "$TEST_ROOT/deploy/.env.app"
printf '%048d\n' 0 | tr '0' 'a' > "$TEST_ROOT/deploy/grafana-admin-password"
printf '%064d\n' 0 | tr '0' 'b' > "$TEST_ROOT/deploy/grafana-secret-key"
chmod 600 "$TEST_ROOT/deploy/grafana-admin-password" \
    "$TEST_ROOT/deploy/grafana-secret-key"

prepare_legacy_release release-a
prepare_release release-b
prepare_release release-c
prepare_release release-d
prepare_release release-e
prepare_release release-f
prepare_release release-g

IMAGE_A="example/skala-shop-api@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
IMAGE_B="example/skala-shop-api@sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
IMAGE_C="example/skala-shop-api@sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
IMAGE_D="example/skala-shop-api@sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
IMAGE_E="example/skala-shop-api@sha256:eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"
IMAGE_F="example/skala-shop-api@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
IMAGE_G="example/skala-shop-api@sha256:1111111111111111111111111111111111111111111111111111111111111111"

write_metadata "$TEST_ROOT/state/current.env" release-a "$IMAGE_A"

if command -v docker >/dev/null 2>&1 \
        && docker compose version >/dev/null 2>&1; then
    GRAFANA_ADMIN_PASSWORD=$(cat "$TEST_ROOT/deploy/grafana-admin-password") \
    GRAFANA_SECRET_KEY=$(cat "$TEST_ROOT/deploy/grafana-secret-key") \
    docker compose \
        --project-name skala-shop-config-test \
        --env-file "$TEST_ROOT/deploy/.env.infra" \
        --env-file "$TEST_ROOT/state/current.env" \
        -f "$TEST_ROOT/releases/release-a/compose.prod.yml" \
        config --quiet
fi

REAL_FLOCK_AVAILABLE=1
if ! command -v flock >/dev/null 2>&1; then
    REAL_FLOCK_AVAILABLE=0
    printf '%s\n' \
        '#!/bin/sh' \
        'case "${1:-}" in' \
        '    -n|-u) exit 0 ;;' \
        '    *) exit 1 ;;' \
        'esac' \
        > "$TEST_ROOT/bin/flock"
    chmod 700 "$TEST_ROOT/bin/flock"
fi

printf '%s\n' \
    '#!/bin/sh' \
    'set -eu' \
    '' \
    'record() {' \
    '    printf "%s\n" "$*" >> "$SIM_STATE/log"' \
    '}' \
    '' \
    'metadata_value() {' \
    '    sed -n "s/^${2}=//p" "$1"' \
    '}' \
    '' \
    'if [ "${1:-}" = "volume" ]; then' \
    '    exit 1' \
    'fi' \
    '' \
    'if [ "${1:-}" = "inspect" ]; then' \
    '    container_id=' \
    '    for argument in "$@"; do container_id=$argument; done' \
    '    service_name=${container_id%%-*}' \
    '    active_release=$(cat "$SIM_STATE/active-$service_name")' \
    '    record "INSPECT_${service_name} $active_release"' \
    '    cat "$SIM_STATE/health-$service_name"' \
    '    exit 0' \
    'fi' \
    '' \
    '[ "${1:-}" = "compose" ] || exit 1' \
    'shift' \
    'metadata_file=' \
    'while [ "$#" -gt 0 ]; do' \
    '    case "$1" in' \
    '        --project-name|--env-file|-f|--profile)' \
    '            [ "$#" -ge 2 ] || exit 1' \
    '            if [ "$1" = "--env-file" ]; then metadata_file=$2; fi' \
    '            shift 2' \
    '            ;;' \
    '        *) break ;;' \
    '    esac' \
    'done' \
    '' \
    '[ -n "$metadata_file" ] || exit 1' \
    'release_id=$(metadata_value "$metadata_file" RELEASE_ID)' \
    'release_dir=$(metadata_value "$metadata_file" RELEASE_DIR)' \
    'command_name=${1:-}' \
    '[ "$#" -eq 0 ] || shift' \
    '' \
    'case "$command_name" in' \
    '    config)' \
    '        case " $* " in' \
    '            *" --services "*)' \
    '                if [ -d "$release_dir/monitoring" ]; then' \
    '                    printf "%s\n" backend redis prometheus grafana nginx certbot-renew' \
    '                else' \
    '                    printf "%s\n" backend redis nginx certbot-renew' \
    '                fi' \
    '                ;;' \
    '            *) record "CONFIG $release_id" ;;' \
    '        esac' \
    '        ;;' \
    '    pull)' \
    '        record "PULL $release_id"' \
    '        ;;' \
    '    up)' \
    '        case " $* " in' \
    '            *" --remove-orphans "*)' \
    '                if [ ! -d "$release_dir/monitoring" ]; then' \
    '                    record "REMOVE_MONITORING_ORPHANS $release_id"' \
    '                    rm -f "$SIM_STATE/active-prometheus" "$SIM_STATE/active-grafana"' \
    '                fi' \
    '                ;;' \
    '        esac' \
    '        case " $* " in' \
    '            *" redis "*)' \
    '                record "UP_REDIS $release_id"' \
    '                ;;' \
    '        esac' \
    '        case " $* " in' \
    '            *" backend "*)' \
    '                record "UP_BACKEND $release_id"' \
    '                printf "%s\n" "$release_id" > "$SIM_STATE/active-backend"' \
    '                if [ -f "$release_dir/fail-health" ]; then' \
    '                    printf "%s\n" unhealthy > "$SIM_STATE/health-backend"' \
    '                else' \
    '                    printf "%s\n" healthy > "$SIM_STATE/health-backend"' \
    '                fi' \
    '                ;;' \
    '        esac' \
    '        case " $* " in' \
    '            *" prometheus "*)' \
    '                record "UP_PROMETHEUS $release_id"' \
    '                printf "%s\n" "$release_id" > "$SIM_STATE/active-prometheus"' \
    '                if [ -f "$release_dir/fail-prometheus-health" ]; then' \
    '                    printf "%s\n" unhealthy > "$SIM_STATE/health-prometheus"' \
    '                else' \
    '                    printf "%s\n" healthy > "$SIM_STATE/health-prometheus"' \
    '                fi' \
    '                ;;' \
    '        esac' \
    '        case " $* " in' \
    '            *" grafana "*)' \
    '                record "UP_GRAFANA $release_id"' \
    '                printf "%s\n" "$release_id" > "$SIM_STATE/active-grafana"' \
    '                if [ -f "$release_dir/fail-grafana-health" ]; then' \
    '                    printf "%s\n" unhealthy > "$SIM_STATE/health-grafana"' \
    '                else' \
    '                    printf "%s\n" healthy > "$SIM_STATE/health-grafana"' \
    '                fi' \
    '                ;;' \
    '        esac' \
    '        case " $* " in' \
    '            *" nginx "*)' \
    '                record "UP_EDGE $release_id"' \
    '                printf "%s\n" "$release_id" > "$SIM_STATE/active-edge"' \
    '                ;;' \
    '        esac' \
    '        ;;' \
    '    ps)' \
    '        service_name=${2:-backend}' \
    '        record "PS_${service_name} $release_id"' \
    '        printf "%s-%s-container\n" "$service_name" "$release_id"' \
    '        ;;' \
    '    run)' \
    '        record "NGINX_TEST $release_id"' \
    '        [ ! -f "$release_dir/fail-nginx" ]' \
    '        ;;' \
    '    exec)' \
    '        case " $* " in' \
    '            *"api/v1/query"*)' \
    '                record "PROMETHEUS_TARGET $release_id"' \
    '                if [ -f "$release_dir/fail-prometheus-target" ]; then' \
    '                    printf "%s\n" '"'"'{"status":"success","data":{"result":[{"metric":{"job":"skala-shop-api"},"value":[0,"0"]}]}}'"'" \
    '                else' \
    '                    printf "%s\n" '"'"'{"status":"success","data":{"result":[{"metric":{"job":"skala-shop-api"},"value":[0,"1"]}]}}'"'" \
    '                fi' \
    '                ;;' \
    '            *" nginx -t "*)' \
    '                record "LIVE_NGINX_TEST $release_id"' \
    '                [ ! -f "$release_dir/fail-live-nginx" ]' \
    '                ;;' \
    '            *" nginx -s reload "*)' \
    '                record "RELOAD $release_id"' \
    '                ;;' \
    '        esac' \
    '        ;;' \
    '    stop|rm)' \
    '        record "${command_name} $release_id"' \
    '        ;;' \
    '    *) exit 1 ;;' \
    'esac' \
    > "$TEST_ROOT/bin/docker"
chmod 700 "$TEST_ROOT/bin/docker"

printf '%s\n' \
    '#!/bin/sh' \
    'set -eu' \
    'url=' \
    'status_only=0' \
    'for argument in "$@"; do' \
    '    case "$argument" in' \
    '        --write-out) status_only=1 ;;' \
    '        https://*) url=$argument ;;' \
    '    esac' \
    'done' \
    '[ -n "$url" ] || exit 2' \
    'request_path=/${url#https://*/}' \
    'active_release=$(cat "$SIM_STATE/active-edge")' \
    'release_dir="$TEST_ROOT/releases/$active_release"' \
    'printf "CURL %s %s\n" "$active_release" "$request_path" >> "$SIM_STATE/log"' \
    'case "$request_path" in' \
    '    /actuator/health)' \
    '        if [ -f "$release_dir/transient-edge-health" ]; then' \
    '            rm -f "$release_dir/transient-edge-health"' \
    '            exit 55' \
    '        fi' \
    '        [ ! -f "$release_dir/fail-edge-health" ] || exit 22' \
    '        printf "%s\n" '"'"'{"status":"UP"}'"'" \
    '        ;;' \
    '    /grafana/api/health)' \
    '        if [ "$status_only" -eq 1 ]; then' \
    '            if [ -d "$release_dir/monitoring" ]; then printf 200; else printf 404; fi' \
    '        else' \
    '            [ -d "$release_dir/monitoring" ] || exit 22' \
    '            [ ! -f "$release_dir/fail-edge-grafana" ] || exit 22' \
    '            printf "%s\n" '"'"'{"database":"ok","version":"test"}'"'" \
    '        fi' \
    '        ;;' \
    '    /actuator/prometheus)' \
    '        if [ -f "$release_dir/fail-metrics-boundary" ]; then printf 200; else printf 404; fi' \
    '        ;;' \
    '    *) printf 404 ;;' \
    'esac' \
    > "$TEST_ROOT/bin/curl"
chmod 700 "$TEST_ROOT/bin/curl"

printf '%s\n' \
    '#!/bin/sh' \
    'if [ "${1:-}" = "5" ]; then exit 0; fi' \
    'exec /bin/sleep "$@"' \
    > "$TEST_ROOT/bin/sleep"
chmod 700 "$TEST_ROOT/bin/sleep"

PATH="$TEST_ROOT/bin:$PATH"
export PATH SIM_STATE TEST_ROOT

if [ "$REAL_FLOCK_AVAILABLE" -eq 1 ]; then
    (
        SCRIPT_DIR="$TEST_ROOT/releases/release-a/scripts"
        . "$SCRIPT_DIR/release-lib.sh"
        acquire_deployment_lock
        : > "$SIM_STATE/lock-held"
        sleep 2
    ) &
    lock_holder_pid=$!
    lock_wait_attempt=1
    while [ ! -f "$SIM_STATE/lock-held" ] && [ "$lock_wait_attempt" -le 20 ]; do
        sleep 0.1
        lock_wait_attempt=$((lock_wait_attempt + 1))
    done
    [ -f "$SIM_STATE/lock-held" ] || fail "lock holder did not start"
    if "$TEST_ROOT/releases/release-b/scripts/deploy.sh" release-b "$IMAGE_B"; then
        fail "a concurrent deployment unexpectedly acquired the host lock"
    fi
    wait "$lock_holder_pid"
fi

# A stale lock file must not block flock-based deployment.
: > "$SIM_STATE/log"
: > "$TEST_ROOT/state/deploy.lock"
: > "$TEST_ROOT/releases/release-b/transient-edge-health"
"$TEST_ROOT/releases/release-b/scripts/deploy.sh" release-b "$IMAGE_B"

assert_metadata_release "$TEST_ROOT/state/current.env" release-b "successful current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-a "successful known-good release"
assert_equal release-b "$(cat "$SIM_STATE/active-backend")" "active backend after deployment"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "active edge after deployment"
assert_equal release-b "$(cat "$SIM_STATE/active-prometheus")" "active Prometheus after deployment"
assert_equal release-b "$(cat "$SIM_STATE/active-grafana")" "active Grafana after deployment"
[ ! -f "$TEST_ROOT/state/candidate.env" ] || fail "candidate metadata was not cleaned"
assert_log_order "UP_REDIS release-b" "UP_BACKEND release-b"
assert_log_order "INSPECT_backend release-b" "UP_PROMETHEUS release-b"
assert_log_order "INSPECT_prometheus release-b" "UP_GRAFANA release-b"
assert_log_order "INSPECT_grafana release-b" "PROMETHEUS_TARGET release-b"
assert_log_order "PROMETHEUS_TARGET release-b" "NGINX_TEST release-b"
assert_log_order "NGINX_TEST release-b" "UP_EDGE release-b"
[ "$(grep -Fc 'CURL release-b /actuator/health' "$SIM_STATE/log")" -eq 2 ] \
    || fail "transient edge health failure was not retried exactly once"

# Monitoring health and scrape failures must not promote a candidate.
: > "$SIM_STATE/log"
: > "$TEST_ROOT/releases/release-e/fail-prometheus-health"
if "$TEST_ROOT/releases/release-e/scripts/deploy.sh" release-e "$IMAGE_E"; then
    fail "candidate with unhealthy Prometheus unexpectedly succeeded"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-b "Prometheus failure current release"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "edge after Prometheus failure rollback"
rm -f "$TEST_ROOT/releases/release-e/fail-prometheus-health"

: > "$SIM_STATE/log"
: > "$TEST_ROOT/releases/release-f/fail-grafana-health"
if "$TEST_ROOT/releases/release-f/scripts/deploy.sh" release-f "$IMAGE_F"; then
    fail "candidate with unhealthy Grafana unexpectedly succeeded"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-b "Grafana failure current release"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "edge after Grafana failure rollback"

: > "$SIM_STATE/log"
: > "$TEST_ROOT/releases/release-g/fail-prometheus-target"
if "$TEST_ROOT/releases/release-g/scripts/deploy.sh" release-g "$IMAGE_G"; then
    fail "candidate with a DOWN Backend scrape target unexpectedly succeeded"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-b "scrape failure current release"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "edge after scrape failure rollback"

# Public metrics exposure is also a release failure even when containers are healthy.
: > "$SIM_STATE/log"
: > "$TEST_ROOT/releases/release-e/fail-metrics-boundary"
if "$TEST_ROOT/releases/release-e/scripts/deploy.sh" release-e "$IMAGE_E"; then
    fail "candidate exposing Prometheus metrics unexpectedly succeeded"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-b "metrics boundary failure current release"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "edge after metrics boundary rollback"
rm -f "$TEST_ROOT/releases/release-e/fail-metrics-boundary"

# Failure before promotion must restore both the current backend and edge config.
: > "$SIM_STATE/log"
: > "$TEST_ROOT/releases/release-c/fail-nginx"
if "$TEST_ROOT/releases/release-c/scripts/deploy.sh" release-c "$IMAGE_C"; then
    fail "candidate with invalid Nginx configuration unexpectedly succeeded"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-b "pre-promotion failure current release"
assert_metadata_release "$TEST_ROOT/state/failed.env" release-c "pre-promotion failed release"
assert_equal release-b "$(cat "$SIM_STATE/active-backend")" "restored backend after pre-promotion failure"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "restored edge after pre-promotion failure"
assert_log_order "NGINX_TEST release-c" "UP_BACKEND release-b"
assert_log_order "UP_BACKEND release-b" "UP_EDGE release-b"

# Failure while switching the live edge must restore the original state pair.
: > "$SIM_STATE/log"
: > "$TEST_ROOT/releases/release-d/fail-live-nginx"
if "$TEST_ROOT/releases/release-d/scripts/deploy.sh" release-d "$IMAGE_D"; then
    fail "candidate with a failing live edge unexpectedly succeeded"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-b "edge-switch failure current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-a "edge-switch failure known-good release"
assert_metadata_release "$TEST_ROOT/state/failed.env" release-d "edge-switch failed release"
assert_equal release-b "$(cat "$SIM_STATE/active-backend")" "restored backend after edge failure"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "restored edge after edge failure"
assert_log_order "LIVE_NGINX_TEST release-d" "UP_BACKEND release-b"
assert_log_order "UP_BACKEND release-b" "UP_EDGE release-b"

# Manual rollback must restore the complete known-good release, not only its image.
write_metadata "$TEST_ROOT/state/known-good.env" release-a "$IMAGE_A"
: > "$SIM_STATE/log"
"$TEST_ROOT/releases/release-b/scripts/rollback.sh"
assert_metadata_release "$TEST_ROOT/state/current.env" release-a "manual rollback current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-b "manual rollback known-good release"
assert_metadata_release "$TEST_ROOT/state/failed.env" release-b "manual rollback failed release"
assert_equal release-a "$(cat "$SIM_STATE/active-backend")" "manual rollback backend"
assert_equal release-a "$(cat "$SIM_STATE/active-edge")" "manual rollback edge"
[ ! -f "$SIM_STATE/active-prometheus" ] || fail "legacy rollback left Prometheus running"
[ ! -f "$SIM_STATE/active-grafana" ] || fail "legacy rollback left Grafana running"
grep -Fx "REMOVE_MONITORING_ORPHANS release-a" "$SIM_STATE/log" >/dev/null \
    || fail "legacy rollback did not remove monitoring orphans"
grep -Fx "CURL release-a /grafana/api/health" "$SIM_STATE/log" >/dev/null \
    || fail "legacy rollback did not verify that Grafana was no longer public"

# A failed manual rollback must compensate runtime and both metadata pointers.
: > "$SIM_STATE/log"
: > "$TEST_ROOT/releases/release-b/fail-live-nginx"
if "$TEST_ROOT/releases/release-a/scripts/rollback.sh"; then
    fail "manual rollback with a failing target edge unexpectedly succeeded"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-a "failed rollback current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-b "failed rollback known-good release"
assert_equal release-a "$(cat "$SIM_STATE/active-backend")" "compensated backend after failed rollback"
assert_equal release-a "$(cat "$SIM_STATE/active-edge")" "compensated edge after failed rollback"
[ ! -f "$TEST_ROOT/state/candidate.env" ] || fail "rollback candidate marker was not cleaned"
rm -f "$TEST_ROOT/releases/release-b/fail-live-nginx"

# A deployment crash before candidate->current commit must restore the exact
# previous current/known-good pair, not collapse both pointers to current.
write_metadata "$TEST_ROOT/state/deploy-previous.env" release-a "$IMAGE_A"
write_metadata "$TEST_ROOT/state/deploy-known-good.env" release-b "$IMAGE_B"
write_metadata "$TEST_ROOT/state/candidate.env" release-c "$IMAGE_C"
write_metadata "$TEST_ROOT/state/known-good.env" release-a "$IMAGE_A"
printf '%s\n' release-c > "$SIM_STATE/active-backend"
printf '%s\n' release-c > "$SIM_STATE/active-edge"
if "$TEST_ROOT/releases/release-c/scripts/deploy.sh" \
        release-c example/skala-shop-api:mutable; then
    fail "mutable tag unexpectedly passed after pre-commit deployment recovery"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-a "deployment crash current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-b "deployment crash known-good release"
assert_equal release-a "$(cat "$SIM_STATE/active-backend")" "deployment crash backend recovery"
assert_equal release-a "$(cat "$SIM_STATE/active-edge")" "deployment crash edge recovery"
[ ! -f "$TEST_ROOT/state/deploy-previous.env" ] || fail "deployment previous journal was not cleaned"
[ ! -f "$TEST_ROOT/state/deploy-known-good.env" ] || fail "deployment known-good journal was not cleaned"

# If candidate is already renamed to current, absent candidate means commit.
write_metadata "$TEST_ROOT/state/deploy-previous.env" release-a "$IMAGE_A"
write_metadata "$TEST_ROOT/state/deploy-known-good.env" release-b "$IMAGE_B"
write_metadata "$TEST_ROOT/state/current.env" release-c "$IMAGE_C"
write_metadata "$TEST_ROOT/state/known-good.env" release-a "$IMAGE_A"
printf '%s\n' release-c > "$SIM_STATE/active-backend"
printf '%s\n' release-c > "$SIM_STATE/active-edge"
if "$TEST_ROOT/releases/release-c/scripts/deploy.sh" \
        release-c example/skala-shop-api:mutable; then
    fail "mutable tag unexpectedly passed after committed deployment recovery"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-c "committed deployment current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-a "committed deployment known-good release"
[ ! -f "$TEST_ROOT/state/deploy-previous.env" ] || fail "committed deployment journal was not cleaned"
[ ! -f "$TEST_ROOT/state/deploy-known-good.env" ] || fail "committed known-good journal was not cleaned"

# Reset to the pre-deployment state for rollback crash simulations.
write_metadata "$TEST_ROOT/state/current.env" release-a "$IMAGE_A"
write_metadata "$TEST_ROOT/state/known-good.env" release-b "$IMAGE_B"
printf '%s\n' release-a > "$SIM_STATE/active-backend"
printf '%s\n' release-a > "$SIM_STATE/active-edge"

# A rollback crash before its atomic candidate->current rename must abort back
# to the previous runtime and restore the original known-good pointer.
write_metadata "$TEST_ROOT/state/rollback-previous.env" release-a "$IMAGE_A"
write_metadata "$TEST_ROOT/state/rollback-target.env" release-b "$IMAGE_B"
write_metadata "$TEST_ROOT/state/candidate.env" release-b "$IMAGE_B"
write_metadata "$TEST_ROOT/state/known-good.env" release-a "$IMAGE_A"
printf '%s\n' release-b > "$SIM_STATE/active-backend"
printf '%s\n' release-b > "$SIM_STATE/active-edge"
if "$TEST_ROOT/releases/release-c/scripts/deploy.sh" \
        release-c example/skala-shop-api:mutable; then
    fail "mutable tag unexpectedly passed after pre-commit rollback recovery"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-a "pre-commit crash current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-b "pre-commit crash known-good release"
assert_equal release-a "$(cat "$SIM_STATE/active-backend")" "pre-commit crash backend recovery"
assert_equal release-a "$(cat "$SIM_STATE/active-edge")" "pre-commit crash edge recovery"
[ ! -f "$TEST_ROOT/state/rollback-previous.env" ] || fail "previous rollback journal was not cleaned"
[ ! -f "$TEST_ROOT/state/rollback-target.env" ] || fail "target rollback journal was not cleaned"

# After candidate->current rename, the absent marker means the swap committed;
# stale journals must be cleaned without undoing the completed rollback.
write_metadata "$TEST_ROOT/state/rollback-previous.env" release-a "$IMAGE_A"
write_metadata "$TEST_ROOT/state/rollback-target.env" release-b "$IMAGE_B"
write_metadata "$TEST_ROOT/state/current.env" release-b "$IMAGE_B"
write_metadata "$TEST_ROOT/state/known-good.env" release-a "$IMAGE_A"
printf '%s\n' release-b > "$SIM_STATE/active-backend"
printf '%s\n' release-b > "$SIM_STATE/active-edge"
if "$TEST_ROOT/releases/release-c/scripts/deploy.sh" \
        release-c example/skala-shop-api:mutable; then
    fail "mutable tag unexpectedly passed after committed rollback recovery"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-b "committed crash current release"
assert_metadata_release "$TEST_ROOT/state/known-good.env" release-a "committed crash known-good release"
assert_equal release-b "$(cat "$SIM_STATE/active-backend")" "committed crash backend"
assert_equal release-b "$(cat "$SIM_STATE/active-edge")" "committed crash edge"
[ ! -f "$TEST_ROOT/state/rollback-previous.env" ] || fail "committed previous journal was not cleaned"
[ ! -f "$TEST_ROOT/state/rollback-target.env" ] || fail "committed target journal was not cleaned"

# Reset to the pre-rollback state for generic deployment crash recovery.
write_metadata "$TEST_ROOT/state/current.env" release-a "$IMAGE_A"
write_metadata "$TEST_ROOT/state/known-good.env" release-b "$IMAGE_B"
printf '%s\n' release-a > "$SIM_STATE/active-backend"
printf '%s\n' release-a > "$SIM_STATE/active-edge"

# A SIGKILL-style leftover marker must restore current before the next action.
write_metadata "$TEST_ROOT/state/candidate.env" release-c "$IMAGE_C"
printf '%s\n' release-c > "$SIM_STATE/active-backend"
printf '%s\n' release-c > "$SIM_STATE/active-edge"

# A mutable tag must never enter release metadata after interrupted-state recovery.
if "$TEST_ROOT/releases/release-c/scripts/deploy.sh" \
        release-c example/skala-shop-api:mutable; then
    fail "mutable image tag unexpectedly passed validation"
fi
assert_metadata_release "$TEST_ROOT/state/current.env" release-a "current release after rejected tag"
assert_equal release-a "$(cat "$SIM_STATE/active-backend")" "recovered backend before rejected tag"
assert_equal release-a "$(cat "$SIM_STATE/active-edge")" "recovered edge before rejected tag"
[ ! -f "$TEST_ROOT/state/candidate.env" ] || fail "interrupted candidate marker was not cleaned"

echo "release flow simulation passed"
