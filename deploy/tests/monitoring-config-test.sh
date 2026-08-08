#!/bin/sh

set -eu

TEST_SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.prod.yml"
PROMETHEUS_CONFIG="$DEPLOY_DIR/monitoring/prometheus/prometheus.yml"
GRAFANA_DATASOURCE="$DEPLOY_DIR/monitoring/grafana/provisioning/datasources/prometheus.yml"
GRAFANA_PROVIDER="$DEPLOY_DIR/monitoring/grafana/provisioning/dashboards/dashboards.yml"
GRAFANA_DASHBOARD="$DEPLOY_DIR/monitoring/grafana/dashboards/skala-shop-overview.json"
NGINX_CONFIG="$DEPLOY_DIR/nginx/api.conf.template"
TEMP_BASE=${TMPDIR:-/tmp}
TEST_ROOT=$(mktemp -d "${TEMP_BASE%/}/skala-monitoring-test.XXXXXX")
MONITORING_STARTED=0

cleanup() {
    exit_code=$?
    trap - EXIT INT TERM HUP
    if [ "$MONITORING_STARTED" -eq 1 ]; then
        if [ "$exit_code" -ne 0 ]; then
            monitoring_compose logs --no-color || true
        fi
        monitoring_compose down -v --remove-orphans >/dev/null 2>&1 || true
    fi
    rm -rf "$TEST_ROOT"
    exit "$exit_code"
}
trap cleanup EXIT
trap 'exit 130' INT TERM HUP

for required_command in docker jq ruby; do
    command -v "$required_command" >/dev/null 2>&1 || {
        echo "monitoring config test requires $required_command" >&2
        exit 1
    }
done

export API_DOMAIN=api.example.com
export FRONTEND_ORIGIN=https://shop.example.com
export LETSENCRYPT_EMAIL=admin@example.com
export APP_ENV_FILE="$DEPLOY_DIR/.env.app.example"
export BACKEND_IMAGE_REF=example/skala-shop-api@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
export GRAFANA_ADMIN_PASSWORD=static-test-only-admin-password
export GRAFANA_SECRET_KEY=static-test-only-secret-key-with-32-bytes

monitoring_compose() {
    docker compose \
        --project-name skala-shop-monitoring-config-test \
        -f "$COMPOSE_FILE" "$@"
}

monitoring_compose config --quiet
monitoring_compose config --format json > "$TEST_ROOT/compose.json"

# 운영 메트릭 서비스가 host port 없이 내부 network에만 있고, 소형 EC2에 맞춘
# resource/retention 경계를 잃지 않았는지 Compose 정규화 결과로 검사합니다.
jq -e '
    (.services.backend.expose | index("9090") != null)
    and (.services.prometheus.ports == null)
    and (.services.grafana.ports == null)
    and ((.services.prometheus.networks | keys) == ["internal"])
    and ((.services.grafana.networks | keys) == ["internal"])
    and (.networks.internal.internal == true)
    and (.services.prometheus.mem_limit == "201326592")
    and (.services.grafana.mem_limit == "268435456")
    and (.services.prometheus.mem_reservation == "67108864")
    and (.services.grafana.mem_reservation == "100663296")
    and (.services.prometheus.cpus == 0.5)
    and (.services.grafana.cpus == 0.5)
    and (.services.prometheus.read_only == true)
    and (.services.prometheus.command | index("--storage.tsdb.retention.time=3d") != null)
    and (.services.prometheus.command | index("--storage.tsdb.retention.size=512MB") != null)
    and (.services.grafana.environment.GF_AUTH_ANONYMOUS_ENABLED == "false")
    and (.services.grafana.environment.GF_USERS_ALLOW_SIGN_UP == "false")
    and (.services.grafana.environment.GF_ANALYTICS_CHECK_FOR_PLUGIN_UPDATES == "false")
    and (.services.grafana.environment.GF_PLUGINS_PREINSTALL_DISABLED == "true")
    and (.services.grafana.environment.GF_PLUGINS_PLUGIN_ADMIN_ENABLED == "false")
    and ([.services.grafana.volumes[] | select(.type == "bind") | .target] | sort == [
        "/etc/grafana/dashboards",
        "/etc/grafana/provisioning/dashboards",
        "/etc/grafana/provisioning/datasources"
    ])
    and (.services.prometheus.image | test("@sha256:[0-9a-f]{64}$"))
    and (.services.grafana.image | test("@sha256:[0-9a-f]{64}$"))
    and (.secrets["grafana-admin-password"].environment == "GRAFANA_ADMIN_PASSWORD")
    and (.secrets["grafana-secret-key"].environment == "GRAFANA_SECRET_KEY")
' "$TEST_ROOT/compose.json" >/dev/null

PROMETHEUS_IMAGE=$(jq -r '.services.prometheus.image' "$TEST_ROOT/compose.json")
docker run --rm \
    --entrypoint /bin/promtool \
    --volume "$PROMETHEUS_CONFIG:/etc/prometheus/prometheus.yml:ro" \
    "$PROMETHEUS_IMAGE" \
    check config /etc/prometheus/prometheus.yml

# 실제 container startup으로 Grafana SQLite volume, Compose secret, provisioning과
# subpath healthcheck 계약까지 확인합니다. Backend가 없어 target은 DOWN이지만
# Prometheus/Grafana 자체 기동 검증에는 영향을 주지 않습니다.
MONITORING_STARTED=1
monitoring_compose up -d --no-deps --wait prometheus
monitoring_compose up -d --no-deps --wait grafana
monitoring_compose exec -T prometheus \
    wget --quiet --spider http://localhost:9090/-/healthy
monitoring_compose exec -T grafana \
    wget --quiet --spider http://localhost:3000/grafana/api/health

# Grafana provisioning YAML과 dashboard JSON을 실제 parser로 읽고 핵심 연결 계약을
# 확인합니다. 단순 문자열 grep으로 깨진 YAML/JSON을 통과시키지 않습니다.
ruby - "$PROMETHEUS_CONFIG" "$GRAFANA_DATASOURCE" "$GRAFANA_PROVIDER" <<'RUBY'
require "yaml"

prometheus_config = YAML.safe_load(File.read(ARGV.fetch(0)), aliases: false)
datasource = YAML.safe_load(File.read(ARGV.fetch(1)), aliases: false)
provider = YAML.safe_load(File.read(ARGV.fetch(2)), aliases: false)

abort "unexpected Prometheus scrape interval" unless prometheus_config.dig("global", "scrape_interval") == "30s"
job = prometheus_config.fetch("scrape_configs").find { |entry| entry["job_name"] == "skala-shop-api" }
abort "missing Backend management scrape target" unless job&.dig("static_configs", 0, "targets") == ["backend:9090"]
abort "unexpected Backend metrics path" unless job["metrics_path"] == "/actuator/prometheus"

prometheus = datasource.fetch("datasources").find { |entry| entry["uid"] == "prometheus" }
abort "missing internal Prometheus datasource" unless prometheus&.fetch("url") == "http://prometheus:9090"
abort "Prometheus datasource must not be editable" unless prometheus["editable"] == false

dashboard_provider = provider.fetch("providers").first
abort "dashboard provider must be file-backed" unless dashboard_provider&.fetch("type") == "file"
abort "dashboard provider must be immutable" unless dashboard_provider["editable"] == false
RUBY

jq -e '
    (.uid == "skala-shop-overview")
    and (.title | length > 0)
    and (.refresh == "30s")
    and (.panels | length >= 9)
    and ([.panels[].targets[]?.expr] | any(contains("up{job=\"skala-shop-api\"}")))
    and ([.panels[].targets[]?.expr] | any(contains("http_server_requests_seconds")))
    and ([.panels[].targets[]?.expr] | any(contains("shopping_business_errors_total")))
    and ([.panels[].targets[]?.expr] | any(contains("shopping_payment_results_total")))
    and ([.panels[].targets[]?.expr] | any(contains("shopping_payment_refunds_total")))
    and ([.panels[].targets[]?.expr] | any(contains("shopping_payment_reconciliations_total")))
    and ([.panels[].targets[]?.expr] | any(contains("shopping_payment_webhooks_total")))
' "$GRAFANA_DASHBOARD" >/dev/null

grep -F 'location = /actuator/health {' "$NGINX_CONFIG" >/dev/null
grep -F 'set $management backend:9090;' "$NGINX_CONFIG" >/dev/null
grep -F 'location ^~ /actuator/ {' "$NGINX_CONFIG" >/dev/null
grep -F 'location /grafana/ {' "$NGINX_CONFIG" >/dev/null
grep -F 'set $grafana grafana:3000;' "$NGINX_CONFIG" >/dev/null

REMOTE_COMMAND=$(sh "$DEPLOY_DIR/tools/build-deploy-payload.sh" \
    aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-123456789-1 \
    aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \
    example/skala-shop-api@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \
    deploy.sh)
[ "${#REMOTE_COMMAND}" -le 23000 ] || {
    echo "SSM deployment command exceeds the 23KB safety limit: ${#REMOTE_COMMAND}" >&2
    exit 1
}
printf '%s\n' "$REMOTE_COMMAND" | grep -F "DEPLOY_ARCHIVE_BASE64='" >/dev/null
# Archive 본문은 encoded 상태이므로 payload builder의 source 목록도 따로 고정합니다.
grep -F 'deploy/monitoring' "$DEPLOY_DIR/tools/build-deploy-payload.sh" >/dev/null
printf '%s\n' "$REMOTE_COMMAND" > "$TEST_ROOT/remote-command.sh"
sh -n "$TEST_ROOT/remote-command.sh"
grep -Fx 'set -eu' "$TEST_ROOT/remote-command.sh" >/dev/null

echo "monitoring configuration test passed"
