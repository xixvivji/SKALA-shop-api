#!/bin/sh

# Shared deployment functions. The caller must set SCRIPT_DIR before sourcing this file.

RELEASE_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
RELEASES_DIR=$(CDPATH= cd -- "$RELEASE_DIR/.." && pwd)
INSTALL_ROOT=$(CDPATH= cd -- "$RELEASES_DIR/.." && pwd)
SHARED_DIR="$INSTALL_ROOT/deploy"
STATE_DIR="$INSTALL_ROOT/state"
INFRA_ENV="$SHARED_DIR/.env.infra"
APP_ENV="$SHARED_DIR/.env.app"
GRAFANA_ADMIN_PASSWORD_FILE="$SHARED_DIR/grafana-admin-password"
GRAFANA_SECRET_KEY_FILE="$SHARED_DIR/grafana-secret-key"
GRAFANA_DATA_VOLUME="skala-shop_grafana-data"
LOCK_FILE="$STATE_DIR/deploy.lock"
CURRENT_RELEASE="$STATE_DIR/current.env"
KNOWN_GOOD_RELEASE="$STATE_DIR/known-good.env"
CANDIDATE_RELEASE="$STATE_DIR/candidate.env"
FAILED_RELEASE="$STATE_DIR/failed.env"
DEPLOY_PREVIOUS_RELEASE="$STATE_DIR/deploy-previous.env"
DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE="$STATE_DIR/deploy-known-good.env"
DEPLOY_ORIGINAL_KNOWN_GOOD_ABSENT="$STATE_DIR/deploy-known-good.absent"
ROLLBACK_PREVIOUS_RELEASE="$STATE_DIR/rollback-previous.env"
ROLLBACK_TARGET_RELEASE="$STATE_DIR/rollback-target.env"

require_shared_files() {
    for required_file in "$INFRA_ENV" "$APP_ENV"; do
        if [ ! -f "$required_file" ]; then
            echo "missing required file: $required_file" >&2
            return 1
        fi
    done

    INFRA_API_DOMAIN=$(infra_env_value API_DOMAIN) || return 1
    INFRA_FRONTEND_ORIGIN=$(infra_env_value FRONTEND_ORIGIN) || return 1
    INFRA_APP_ENV_FILE=$(infra_env_value APP_ENV_FILE) || return 1
    validate_api_domain "$INFRA_API_DOMAIN" || return 1
    validate_frontend_origin "$INFRA_FRONTEND_ORIGIN" || return 1
    if [ "$INFRA_APP_ENV_FILE" != "$APP_ENV" ]; then
        echo "APP_ENV_FILE must be exactly $APP_ENV" >&2
        return 1
    fi
    validate_app_environment || return 1
}

ensure_secret_file() {
    secret_file=$1
    secret_label=$2
    random_bytes=$3
    created_secret=0

    if [ -L "$secret_file" ]; then
        echo "$secret_label must not be a symbolic link: $secret_file" >&2
        return 1
    fi

    if [ -e "$secret_file" ]; then
        if [ ! -f "$secret_file" ]; then
            echo "$secret_label must be a regular file: $secret_file" >&2
            return 1
        fi
    else
        # Grafana only consumes its initial admin password when it creates the
        # database. Silently replacing a lost secret while its data volume
        # remains would create a credential that cannot log in.
        if docker volume inspect "$GRAFANA_DATA_VOLUME" >/dev/null 2>&1; then
            echo "$secret_label is missing while Grafana data already exists" >&2
            echo "recover the original secret or reset the Grafana credential explicitly" >&2
            return 1
        fi

        temporary_secret="${secret_file}.tmp.$$"
        if ! (
            umask 077
            od -An -N "$random_bytes" -tx1 /dev/urandom \
                | tr -d ' \n' > "$temporary_secret"
            chmod 600 "$temporary_secret"
            mv "$temporary_secret" "$secret_file"
        ); then
            rm -f "$temporary_secret"
            echo "could not create $secret_label" >&2
            return 1
        fi
        created_secret=1
    fi

    chmod 600 "$secret_file" || return 1
    secret_mode=$(portable_stat_mode "$secret_file") || return 1
    if [ "$secret_mode" != "600" ]; then
        echo "$secret_label must have mode 600: $secret_file" >&2
        return 1
    fi
    secret_owner=$(portable_stat_uid "$secret_file") || return 1
    if [ "$secret_owner" != "$(id -u)" ]; then
        echo "$secret_label must be owned by the deployment user: $secret_file" >&2
        return 1
    fi
    if ! validate_secret_file "$secret_file" "$secret_label"; then
        if [ "$created_secret" -eq 1 ]; then
            rm -f "$secret_file"
        fi
        return 1
    fi
}

portable_stat_mode() {
    stat -c '%a' "$1" 2>/dev/null || stat -f '%Lp' "$1" 2>/dev/null
}

portable_stat_uid() {
    stat -c '%u' "$1" 2>/dev/null || stat -f '%u' "$1" 2>/dev/null
}

validate_secret_file() {
    secret_file=$1
    secret_label=$2
    secret_value=$(cat "$secret_file") || return 1

    if [ "${#secret_value}" -lt 48 ] || [ "${#secret_value}" -gt 128 ]; then
        echo "$secret_label must contain 48 to 128 hexadecimal characters" >&2
        return 1
    fi
    case "$secret_value" in
        *[!0-9a-f]*)
            echo "$secret_label must contain lowercase hexadecimal characters only" >&2
            return 1
            ;;
    esac
}

ensure_grafana_secrets() {
    ensure_secret_file "$GRAFANA_ADMIN_PASSWORD_FILE" \
        "Grafana admin password" 24 || return 1
    ensure_secret_file "$GRAFANA_SECRET_KEY_FILE" \
        "Grafana secret key" 32 || return 1
}

secret_file_value() {
    secret_file=$1
    secret_label=$2
    validate_secret_file "$secret_file" "$secret_label" || return 1
    cat "$secret_file"
}

app_env_value() {
    app_key=$1
    allow_empty=${2:-false}
    awk -v prefix="${app_key}=" -v allow_empty="$allow_empty" '
        index($0, prefix) == 1 {
            count += 1
            value = substr($0, length(prefix) + 1)
        }
        END {
            if (count != 1 || (allow_empty != "true" && value == "")) exit 1
            print value
        }
    ' "$APP_ENV" || {
        echo "$app_key must occur exactly once in $APP_ENV" >&2
        return 1
    }
}

validate_app_environment() {
    db_url=$(app_env_value DB_URL) || return 1
    db_username=$(app_env_value DB_USERNAME) || return 1
    db_password=$(app_env_value DB_PASSWORD) || return 1
    jwt_secret=$(app_env_value JWT_SECRET) || return 1
    cookie_secure=$(app_env_value JWT_COOKIE_SECURE) || return 1
    cors_origins=$(app_env_value CORS_ALLOWED_ORIGINS) || return 1
    bootstrap_enabled=$(app_env_value BOOTSTRAP_ADMIN_ENABLED) || return 1

    case "$db_url" in
        jdbc:postgresql://*) ;;
        *) echo "DB_URL must be a PostgreSQL JDBC URL" >&2; return 1 ;;
    esac
    case "$db_username:$db_password" in
        *replace-me*|*'<application-user>'*|*'<password>'*)
            echo "database credentials still contain example placeholders" >&2
            return 1
            ;;
    esac
    if [ "${#jwt_secret}" -lt 32 ]; then
        echo "JWT_SECRET must contain at least 32 characters" >&2
        return 1
    fi
    if [ "$cookie_secure" != "true" ]; then
        echo "JWT_COOKIE_SECURE must be true in production" >&2
        return 1
    fi
    if ! printf '%s\n' "$cors_origins" | awk -F, -v expected="$INFRA_FRONTEND_ORIGIN" '
        { for (i = 1; i <= NF; i += 1) if ($i == expected) found = 1 }
        END { exit found ? 0 : 1 }
    '; then
        echo "CORS_ALLOWED_ORIGINS must contain FRONTEND_ORIGIN exactly" >&2
        return 1
    fi
    if [ "$bootstrap_enabled" != "false" ] \
            && [ "${ALLOW_BOOTSTRAP_ADMIN_ONCE:-false}" != "true" ]; then
        echo "BOOTSTRAP_ADMIN_ENABLED must be false for normal deployments" >&2
        echo "use ALLOW_BOOTSTRAP_ADMIN_ONCE=true only for the first manual admin bootstrap" >&2
        return 1
    fi
}

infra_env_value() {
    infra_key=$1
    awk -v prefix="${infra_key}=" '
        index($0, prefix) == 1 {
            count += 1
            value = substr($0, length(prefix) + 1)
        }
        END {
            if (count != 1 || value == "") {
                exit 1
            }
            print value
        }
    ' "$INFRA_ENV" || {
        echo "$infra_key must occur exactly once with a non-empty value in $INFRA_ENV" >&2
        return 1
    }
}

validate_api_domain() {
    api_domain=$1
    case "$api_domain" in
        *[!A-Za-z0-9.-]*|''|.*|*.|*..*)
            echo "API_DOMAIN must be a plain DNS name" >&2
            return 1
            ;;
    esac
}

validate_frontend_origin() {
    frontend_origin=$1
    case "$frontend_origin" in
        https://*) origin_authority=${frontend_origin#https://} ;;
        *)
            echo "FRONTEND_ORIGIN must be one exact HTTPS origin" >&2
            return 1
            ;;
    esac
    case "$origin_authority" in
        *[!A-Za-z0-9.:-]*|''|.*|*.|*..*)
            echo "FRONTEND_ORIGIN must not include a path, query, fragment, or trailing slash" >&2
            return 1
            ;;
    esac
}

acquire_deployment_lock() {
    mkdir -p "$STATE_DIR"
    if ! command -v flock >/dev/null 2>&1; then
        echo "flock is required on the deployment host" >&2
        return 1
    fi

    if [ -n "${SKALA_DEPLOY_LOCK_INHERITED:-}" ]; then
        if [ "$SKALA_DEPLOY_LOCK_INHERITED" != "$LOCK_FILE" ]; then
            echo "inherited deployment lock path is invalid" >&2
            return 1
        fi
        inherited_lock_path=$(readlink "/proc/$$/fd/9" 2>/dev/null || true)
        if [ "$inherited_lock_path" != "$LOCK_FILE" ] || ! flock -n 9; then
            echo "inherited deployment lock descriptor is invalid" >&2
            return 1
        fi
        unset SKALA_DEPLOY_LOCK_INHERITED
        return 0
    fi

    exec 9>"$LOCK_FILE"
    if ! flock -n 9; then
        echo "another deployment, rollback, or TLS bootstrap is already running" >&2
        return 1
    fi
}

validate_release_id() {
    release_id=$1
    case "$release_id" in
        *[!A-Za-z0-9._-]*|'')
            echo "invalid release ID" >&2
            return 1
            ;;
    esac
}

validate_image_ref() {
    image_ref=$1
    repository=${image_ref%@sha256:*}
    digest=${image_ref##*@sha256:}

    if [ "$repository" = "$image_ref" ] || [ -z "$repository" ]; then
        echo "backend image must be an immutable repository@sha256 digest" >&2
        return 1
    fi
    case "$repository" in
        *[!A-Za-z0-9._/-]*)
            echo "invalid Docker Hub repository in backend image reference" >&2
            return 1
            ;;
    esac
    if [ "${#digest}" -ne 64 ]; then
        echo "backend image digest must contain 64 hexadecimal characters" >&2
        return 1
    fi
    case "$digest" in
        *[!0-9a-f]*)
            echo "backend image digest must be lowercase hexadecimal" >&2
            return 1
            ;;
    esac
}

atomic_copy() {
    source_file=$1
    target_file=$2
    temporary_file="${target_file}.tmp.$$"
    cp "$source_file" "$temporary_file" || {
        rm -f "$temporary_file"
        return 1
    }
    chmod 600 "$temporary_file" || {
        rm -f "$temporary_file"
        return 1
    }
    mv "$temporary_file" "$target_file" || {
        rm -f "$temporary_file"
        return 1
    }
}

atomic_marker() {
    marker_file=$1
    temporary_file="${marker_file}.tmp.$$"
    printf 'present\n' > "$temporary_file" || {
        rm -f "$temporary_file"
        return 1
    }
    chmod 600 "$temporary_file" || {
        rm -f "$temporary_file"
        return 1
    }
    mv "$temporary_file" "$marker_file" || {
        rm -f "$temporary_file"
        return 1
    }
}

write_release_metadata() {
    target_file=$1
    release_id=$2
    image_ref=$3
    expected_release_dir="$RELEASES_DIR/$release_id"

    validate_release_id "$release_id" || return 1
    validate_image_ref "$image_ref" || return 1
    if [ "$RELEASE_DIR" != "$expected_release_dir" ]; then
        echo "release script path does not match release ID: $release_id" >&2
        return 1
    fi
    if [ ! -f "$RELEASE_DIR/compose.prod.yml" ] \
            || [ ! -f "$RELEASE_DIR/nginx/api.conf.template" ] \
            || [ ! -f "$RELEASE_DIR/monitoring/prometheus/prometheus.yml" ] \
            || [ ! -f "$RELEASE_DIR/monitoring/grafana/provisioning/datasources/prometheus.yml" ] \
            || [ ! -f "$RELEASE_DIR/monitoring/grafana/provisioning/dashboards/dashboards.yml" ] \
            || [ ! -f "$RELEASE_DIR/monitoring/grafana/dashboards/skala-shop-overview.json" ]; then
        echo "release configuration is incomplete: $RELEASE_DIR" >&2
        return 1
    fi

    temporary_file="${target_file}.tmp.$$"
    if ! {
        printf 'RELEASE_ID=%s\n' "$release_id"
        printf 'BACKEND_IMAGE_REF=%s\n' "$image_ref"
        printf 'RELEASE_DIR=%s\n' "$RELEASE_DIR"
        printf 'RELEASE_COMPOSE_FILE=%s\n' "$RELEASE_DIR/compose.prod.yml"
        printf 'RELEASE_NGINX_CONFIG_FILE=%s\n' "$RELEASE_DIR/nginx/api.conf.template"
    } > "$temporary_file"; then
        rm -f "$temporary_file"
        return 1
    fi
    chmod 600 "$temporary_file" || {
        rm -f "$temporary_file"
        return 1
    }
    mv "$temporary_file" "$target_file" || {
        rm -f "$temporary_file"
        return 1
    }
}

metadata_value() {
    metadata_file=$1
    metadata_key=$2
    sed -n "s/^${metadata_key}=//p" "$metadata_file"
}

load_release_metadata() {
    metadata_file=$1
    if [ ! -f "$metadata_file" ]; then
        echo "missing release metadata: $metadata_file" >&2
        return 1
    fi

    METADATA_RELEASE_ID=$(metadata_value "$metadata_file" RELEASE_ID)
    METADATA_IMAGE_REF=$(metadata_value "$metadata_file" BACKEND_IMAGE_REF)
    METADATA_RELEASE_DIR=$(metadata_value "$metadata_file" RELEASE_DIR)
    METADATA_COMPOSE_FILE=$(metadata_value "$metadata_file" RELEASE_COMPOSE_FILE)
    METADATA_NGINX_CONFIG_FILE=$(
        metadata_value "$metadata_file" RELEASE_NGINX_CONFIG_FILE
    )

    validate_release_id "$METADATA_RELEASE_ID" || return 1
    validate_image_ref "$METADATA_IMAGE_REF" || return 1
    if [ "$METADATA_RELEASE_DIR" != "$RELEASES_DIR/$METADATA_RELEASE_ID" ] \
            || [ "$METADATA_COMPOSE_FILE" != "$METADATA_RELEASE_DIR/compose.prod.yml" ] \
            || [ "$METADATA_NGINX_CONFIG_FILE" \
                != "$METADATA_RELEASE_DIR/nginx/api.conf.template" ]; then
        echo "release metadata paths are inconsistent: $metadata_file" >&2
        return 1
    fi
    if [ ! -f "$METADATA_COMPOSE_FILE" ] \
            || [ ! -f "$METADATA_NGINX_CONFIG_FILE" ]; then
        echo "release files referenced by metadata are missing: $metadata_file" >&2
        return 1
    fi
}

compose_for() {
    metadata_file=$1
    shift
    load_release_metadata "$metadata_file" || return 1
    grafana_admin_password=$(secret_file_value \
        "$GRAFANA_ADMIN_PASSWORD_FILE" "Grafana admin password") || return 1
    grafana_secret_key=$(secret_file_value \
        "$GRAFANA_SECRET_KEY_FILE" "Grafana secret key") || return 1
    BACKEND_IMAGE_REF="$METADATA_IMAGE_REF" \
    APP_ENV_FILE="$APP_ENV" \
    GRAFANA_ADMIN_PASSWORD="$grafana_admin_password" \
    GRAFANA_SECRET_KEY="$grafana_secret_key" \
    docker compose \
        --project-name skala-shop \
        --env-file "$INFRA_ENV" \
        --env-file "$metadata_file" \
        -f "$METADATA_COMPOSE_FILE" "$@" || return 1
}

wait_for_backend() {
    metadata_file=$1
    wait_for_service_health "$metadata_file" backend 24
}

wait_for_service_health() {
    metadata_file=$1
    service_name=$2
    max_attempts=$3
    container_id=$(compose_for "$metadata_file" ps -q "$service_name")
    if [ -z "$container_id" ]; then
        echo "$service_name container was not created" >&2
        return 1
    fi

    attempt=1
    while [ "$attempt" -le "$max_attempts" ]; do
        status=$(docker inspect --format '{{.State.Health.Status}}' "$container_id" 2>/dev/null || true)
        if [ "$status" = "healthy" ]; then
            return 0
        fi
        if [ "$status" = "unhealthy" ] || [ -z "$status" ]; then
            return 1
        fi
        sleep 5
        attempt=$((attempt + 1))
    done
    return 1
}

release_has_monitoring() {
    metadata_file=$1
    services=$(compose_for "$metadata_file" config --services) || return 2
    prometheus_present=0
    grafana_present=0
    if printf '%s\n' "$services" | grep -qx prometheus; then
        prometheus_present=1
    fi
    if printf '%s\n' "$services" | grep -qx grafana; then
        grafana_present=1
    fi

    if [ "$prometheus_present" -eq 1 ] && [ "$grafana_present" -eq 1 ]; then
        return 0
    fi
    if [ "$prometheus_present" -eq 0 ] && [ "$grafana_present" -eq 0 ]; then
        return 1
    fi
    echo "release must define Prometheus and Grafana together" >&2
    return 2
}

wait_for_prometheus_target() {
    metadata_file=$1
    attempt=1
    while [ "$attempt" -le 24 ]; do
        response=$(compose_for "$metadata_file" exec -T prometheus \
            wget --quiet --output-document=- \
            'http://localhost:9090/api/v1/query?query=up%7Bjob%3D%22skala-shop-api%22%7D' \
            2>/dev/null || true)
        if printf '%s' "$response" | grep -q '"job":"skala-shop-api"' \
                && printf '%s' "$response" | grep -q ',"1"\]'; then
            return 0
        fi
        sleep 5
        attempt=$((attempt + 1))
    done
    echo "Prometheus did not report the Backend scrape target as UP" >&2
    return 1
}

activate_monitoring() {
    metadata_file=$1
    compose_for "$metadata_file" up -d --no-deps prometheus || return 1
    wait_for_service_health "$metadata_file" prometheus 24 || return 1
    # The host has no swap and limited free memory. Start Grafana only after
    # Prometheus is healthy so peak initialization memory does not overlap.
    compose_for "$metadata_file" up -d --no-deps grafana || return 1
    wait_for_service_health "$metadata_file" grafana 24 || return 1
    wait_for_prometheus_target "$metadata_file" || return 1
}

test_nginx_config() {
    metadata_file=$1
    compose_for "$metadata_file" run --rm --no-deps nginx nginx -t
}

activate_edge() {
    metadata_file=$1
    # --remove-orphans is required when rolling back the first monitoring
    # release to an older Compose that did not define Prometheus or Grafana.
    # It removes only services absent from the target project definition and
    # leaves its Backend, Redis and edge services intact.
    compose_for "$metadata_file" up -d --no-deps --remove-orphans \
        nginx certbot-renew || return 1
    compose_for "$metadata_file" exec -T nginx nginx -t || return 1
    compose_for "$metadata_file" exec -T nginx nginx -s reload || return 1
    verify_edge_routes "$metadata_file" || return 1
}

edge_response_body() {
    request_path=$1
    attempt=1
    while [ "$attempt" -le 6 ]; do
        if response=$(curl --fail --silent --show-error \
                --noproxy '*' \
                --connect-timeout 5 \
                --max-time 15 \
                --resolve "$INFRA_API_DOMAIN:443:127.0.0.1" \
                "https://$INFRA_API_DOMAIN$request_path"); then
            printf '%s' "$response"
            return 0
        fi
        [ "$attempt" -lt 6 ] || break
        sleep 2
        attempt=$((attempt + 1))
    done
    return 1
}

edge_status_code() {
    request_path=$1
    attempt=1
    while [ "$attempt" -le 6 ]; do
        if status_code=$(curl --silent --show-error \
                --noproxy '*' \
                --connect-timeout 5 \
                --max-time 15 \
                --output /dev/null \
                --write-out '%{http_code}' \
                --resolve "$INFRA_API_DOMAIN:443:127.0.0.1" \
                "https://$INFRA_API_DOMAIN$request_path"); then
            printf '%s' "$status_code"
            return 0
        fi
        [ "$attempt" -lt 6 ] || break
        sleep 2
        attempt=$((attempt + 1))
    done
    return 1
}

verify_edge_routes() {
    metadata_file=$1
    health_response=$(edge_response_body /actuator/health) || {
        echo "public health route did not return a successful response" >&2
        return 1
    }
    if ! printf '%s' "$health_response" \
            | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
        echo "public health route did not report UP" >&2
        return 1
    fi

    monitoring_status=0
    release_has_monitoring "$metadata_file" || monitoring_status=$?
    case "$monitoring_status" in
        0)
            grafana_response=$(edge_response_body /grafana/api/health) || {
                echo "public Grafana health route did not return a successful response" >&2
                return 1
            }
            if ! printf '%s' "$grafana_response" \
                    | grep -Eq '"database"[[:space:]]*:[[:space:]]*"ok"'; then
                echo "public Grafana health route did not report an available database" >&2
                return 1
            fi
            metrics_status=$(edge_status_code /actuator/prometheus) || return 1
            if [ "$metrics_status" != "404" ]; then
                echo "public Prometheus endpoint must return 404, got $metrics_status" >&2
                return 1
            fi
            ;;
        1)
            # An older rollback target has no Grafana route. Any 2xx response
            # means the candidate edge or an orphan monitoring proxy survived.
            grafana_status=$(edge_status_code /grafana/api/health) || true
            case "$grafana_status" in
                2??)
                    echo "Grafana route remained public after rollback to a release without monitoring" >&2
                    return 1
                    ;;
            esac
            ;;
        *) return 1 ;;
    esac
}

activate_release() {
    metadata_file=$1
    compose_for "$metadata_file" pull backend || true
    compose_for "$metadata_file" up -d --no-deps backend || return 1
    wait_for_backend "$metadata_file" || return 1
    monitoring_status=0
    release_has_monitoring "$metadata_file" || monitoring_status=$?
    case "$monitoring_status" in
        0)
            compose_for "$metadata_file" pull prometheus grafana || true
            activate_monitoring "$metadata_file" || return 1
            ;;
        1)
            # Backward-compatible rollback target. activate_edge removes the
            # monitoring containers as project orphans after the old Backend
            # and Nginx configuration are ready.
            ;;
        *) return 1 ;;
    esac
    test_nginx_config "$metadata_file" || return 1
    activate_edge "$metadata_file" || return 1
}

recover_interrupted_release() {
    if [ ! -f "$CANDIDATE_RELEASE" ]; then
        # Release operations remove candidate.env as their atomic commit
        # marker. Journals left without it describe an already consistent or
        # not-yet-started operation and are safe to discard.
        rm -f "$ROLLBACK_PREVIOUS_RELEASE" "$ROLLBACK_TARGET_RELEASE"
        rm -f "$DEPLOY_PREVIOUS_RELEASE" \
            "$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE" \
            "$DEPLOY_ORIGINAL_KNOWN_GOOD_ABSENT"
        return 0
    fi

    echo "recovering state left by an interrupted release operation" >&2
    atomic_copy "$CANDIDATE_RELEASE" "$FAILED_RELEASE" || return 1

    if [ -f "$ROLLBACK_PREVIOUS_RELEASE" ] \
            || [ -f "$ROLLBACK_TARGET_RELEASE" ]; then
        if [ ! -f "$ROLLBACK_PREVIOUS_RELEASE" ] \
                || [ ! -f "$ROLLBACK_TARGET_RELEASE" ]; then
            echo "interrupted rollback journals are incomplete" >&2
            return 1
        fi
        activate_release "$ROLLBACK_PREVIOUS_RELEASE" || {
            echo "could not compensate the interrupted rollback" >&2
            return 1
        }
        atomic_copy "$ROLLBACK_PREVIOUS_RELEASE" "$CURRENT_RELEASE" || return 1
        atomic_copy "$ROLLBACK_TARGET_RELEASE" "$KNOWN_GOOD_RELEASE" || return 1
        rm -f "$CANDIDATE_RELEASE"
        rm -f "$ROLLBACK_PREVIOUS_RELEASE" "$ROLLBACK_TARGET_RELEASE"
        return 0
    fi

    if [ -f "$DEPLOY_PREVIOUS_RELEASE" ] \
            || [ -f "$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE" ] \
            || [ -f "$DEPLOY_ORIGINAL_KNOWN_GOOD_ABSENT" ]; then
        if [ ! -f "$DEPLOY_PREVIOUS_RELEASE" ]; then
            echo "interrupted deployment journal is incomplete" >&2
            return 1
        fi
        if [ -f "$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE" ] \
                && [ -f "$DEPLOY_ORIGINAL_KNOWN_GOOD_ABSENT" ]; then
            echo "interrupted deployment known-good journal is ambiguous" >&2
            return 1
        fi
        if [ ! -f "$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE" ] \
                && [ ! -f "$DEPLOY_ORIGINAL_KNOWN_GOOD_ABSENT" ]; then
            echo "interrupted deployment known-good journal is incomplete" >&2
            return 1
        fi
        activate_release "$DEPLOY_PREVIOUS_RELEASE" || {
            echo "could not compensate the interrupted deployment" >&2
            return 1
        }
        atomic_copy "$DEPLOY_PREVIOUS_RELEASE" "$CURRENT_RELEASE" || return 1
        if [ -f "$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE" ]; then
            atomic_copy "$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE" \
                "$KNOWN_GOOD_RELEASE" || return 1
        else
            rm -f "$KNOWN_GOOD_RELEASE"
        fi
        rm -f "$CANDIDATE_RELEASE"
        rm -f "$DEPLOY_PREVIOUS_RELEASE" \
            "$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE" \
            "$DEPLOY_ORIGINAL_KNOWN_GOOD_ABSENT"
        return 0
    fi

    if [ -f "$CURRENT_RELEASE" ]; then
        activate_release "$CURRENT_RELEASE" || {
            echo "could not restore current release after interruption" >&2
            return 1
        }
    else
        echo "no current release exists for interrupted-operation recovery" >&2
    fi
    rm -f "$CANDIDATE_RELEASE"
}
