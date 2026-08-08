#!/bin/sh

set -eu

if [ "$#" -ne 2 ]; then
    echo "usage: deploy.sh <release-id> <repository@sha256:digest>" >&2
    exit 2
fi

RELEASE_ID_VALUE=$1
IMAGE_REF_VALUE=$2
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/release-lib.sh"

DEPLOY_STARTED=0
DEPLOY_COMPLETE=0
KEEP_CANDIDATE=0
PREVIOUS_AVAILABLE=0
PREVIOUS_METADATA=$DEPLOY_PREVIOUS_RELEASE
ORIGINAL_KNOWN_GOOD_AVAILABLE=0
ORIGINAL_KNOWN_GOOD_METADATA=$DEPLOY_ORIGINAL_KNOWN_GOOD_RELEASE
ORIGINAL_KNOWN_GOOD_ABSENT=$DEPLOY_ORIGINAL_KNOWN_GOOD_ABSENT

require_shared_files
acquire_deployment_lock
recover_interrupted_release

preflight_cleanup() {
    exit_code=$?
    trap - EXIT INT TERM HUP
    rm -f "$CANDIDATE_RELEASE" "$PREVIOUS_METADATA" \
        "$ORIGINAL_KNOWN_GOOD_METADATA" "$ORIGINAL_KNOWN_GOOD_ABSENT"
    exit "$exit_code"
}
trap preflight_cleanup EXIT
trap 'exit 130' INT TERM HUP

if [ -f "$CURRENT_RELEASE" ]; then
    atomic_copy "$CURRENT_RELEASE" "$PREVIOUS_METADATA"
    PREVIOUS_AVAILABLE=1
    if [ -f "$KNOWN_GOOD_RELEASE" ]; then
        atomic_copy "$KNOWN_GOOD_RELEASE" "$ORIGINAL_KNOWN_GOOD_METADATA"
        rm -f "$ORIGINAL_KNOWN_GOOD_ABSENT"
        ORIGINAL_KNOWN_GOOD_AVAILABLE=1
    else
        rm -f "$ORIGINAL_KNOWN_GOOD_METADATA"
        atomic_marker "$ORIGINAL_KNOWN_GOOD_ABSENT"
    fi
elif [ -f "$KNOWN_GOOD_RELEASE" ]; then
    echo "known-good metadata exists without current metadata" >&2
    exit 1
fi

write_release_metadata "$CANDIDATE_RELEASE" "$RELEASE_ID_VALUE" "$IMAGE_REF_VALUE"

restore_previous_release() {
    if [ "$PREVIOUS_AVAILABLE" -ne 1 ]; then
        echo "no previous release metadata is available for automatic rollback" >&2
        return 1
    fi
    activate_release "$PREVIOUS_METADATA" || return 1
    atomic_copy "$PREVIOUS_METADATA" "$CURRENT_RELEASE" || return 1
    if [ "$ORIGINAL_KNOWN_GOOD_AVAILABLE" -eq 1 ]; then
        atomic_copy "$ORIGINAL_KNOWN_GOOD_METADATA" "$KNOWN_GOOD_RELEASE" || return 1
    else
        rm -f "$KNOWN_GOOD_RELEASE"
    fi
}

finish() {
    exit_code=$?
    trap - EXIT INT TERM HUP
    set +e

    if [ "$exit_code" -ne 0 ] && [ "$DEPLOY_STARTED" -eq 1 ] \
            && [ "$DEPLOY_COMPLETE" -eq 0 ]; then
        if [ -f "$CANDIDATE_RELEASE" ]; then
            atomic_copy "$CANDIDATE_RELEASE" "$FAILED_RELEASE"
        fi
        echo "deployment failed; restoring the previous backend and edge configuration" >&2
        if restore_previous_release; then
            echo "previous release restored" >&2
        else
            echo "automatic rollback was unavailable or failed; inspect the deployment immediately" >&2
            KEEP_CANDIDATE=1
        fi
    fi

    if [ "$KEEP_CANDIDATE" -ne 1 ]; then
        rm -f "$CANDIDATE_RELEASE"
        rm -f "$PREVIOUS_METADATA" "$ORIGINAL_KNOWN_GOOD_METADATA" \
            "$ORIGINAL_KNOWN_GOOD_ABSENT"
    fi
    exit "$exit_code"
}

trap finish EXIT
trap 'exit 130' INT TERM HUP

DEPLOY_STARTED=1
compose_for "$CANDIDATE_RELEASE" config --quiet
# prod profile은 인증 세션과 요청 제한에 Redis를 사용합니다. 새 Compose에 Redis가
# 처음 추가되는 배포에서도 backend보다 먼저 생성하고 health가 통과할 때까지 기다립니다.
compose_for "$CANDIDATE_RELEASE" pull backend redis
compose_for "$CANDIDATE_RELEASE" up -d --wait redis
compose_for "$CANDIDATE_RELEASE" up -d --no-deps backend

if ! wait_for_backend "$CANDIDATE_RELEASE"; then
    echo "candidate backend failed its health check" >&2
    exit 1
fi

if ! test_nginx_config "$CANDIDATE_RELEASE"; then
    echo "candidate Nginx configuration failed nginx -t" >&2
    exit 1
fi

# The actual edge is switched while current still points to the recoverable
# previous release. Only a fully started and tested edge is promoted.
activate_edge "$CANDIDATE_RELEASE"

# Keep catchable signals out of the single atomic metadata promotion. A SIGKILL
# can leave candidate.env behind; the next locked invocation recovers it above.
trap '' INT TERM HUP
if [ "$PREVIOUS_AVAILABLE" -eq 1 ]; then
    atomic_copy "$PREVIOUS_METADATA" "$KNOWN_GOOD_RELEASE"
fi
mv "$CANDIDATE_RELEASE" "$CURRENT_RELEASE"
DEPLOY_COMPLETE=1
trap 'exit 130' INT TERM HUP

rm -f "$PREVIOUS_METADATA" "$ORIGINAL_KNOWN_GOOD_METADATA" \
    "$ORIGINAL_KNOWN_GOOD_ABSENT"
trap - EXIT INT TERM HUP

echo "deployed immutable image: $IMAGE_REF_VALUE"
echo "release configuration: $RELEASE_DIR"
