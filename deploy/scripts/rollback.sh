#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/release-lib.sh"

ROLLBACK_STARTED=0
ROLLBACK_COMPLETE=0
KEEP_CANDIDATE=0
PREVIOUS_METADATA=$ROLLBACK_PREVIOUS_RELEASE
TARGET_METADATA=$ROLLBACK_TARGET_RELEASE

require_shared_files
acquire_deployment_lock
recover_interrupted_release

preflight_cleanup() {
    exit_code=$?
    trap - EXIT INT TERM HUP
    rm -f "$CANDIDATE_RELEASE" "$PREVIOUS_METADATA" "$TARGET_METADATA"
    exit "$exit_code"
}
trap preflight_cleanup EXIT
trap 'exit 130' INT TERM HUP

for required_file in "$CURRENT_RELEASE" "$KNOWN_GOOD_RELEASE"; do
    if [ ! -f "$required_file" ]; then
        echo "missing required release metadata: $required_file" >&2
        exit 1
    fi
done

atomic_copy "$CURRENT_RELEASE" "$PREVIOUS_METADATA"
atomic_copy "$KNOWN_GOOD_RELEASE" "$TARGET_METADATA"
load_release_metadata "$PREVIOUS_METADATA"
PREVIOUS_RELEASE_ID=$METADATA_RELEASE_ID
load_release_metadata "$TARGET_METADATA"
TARGET_RELEASE_ID=$METADATA_RELEASE_ID

if [ "$PREVIOUS_RELEASE_ID" = "$TARGET_RELEASE_ID" ]; then
    echo "current and known-good already reference the same release" >&2
    exit 1
fi

# candidate.env is also the crash-recovery marker for a manual rollback.
atomic_copy "$TARGET_METADATA" "$CANDIDATE_RELEASE"

finish() {
    exit_code=$?
    trap - EXIT INT TERM HUP
    set +e

    if [ "$exit_code" -ne 0 ] && [ "$ROLLBACK_STARTED" -eq 1 ] \
            && [ "$ROLLBACK_COMPLETE" -eq 0 ]; then
        echo "rollback failed; restoring the release active before rollback" >&2
        if activate_release "$PREVIOUS_METADATA" \
                && atomic_copy "$PREVIOUS_METADATA" "$CURRENT_RELEASE" \
                && atomic_copy "$TARGET_METADATA" "$KNOWN_GOOD_RELEASE"; then
            echo "pre-rollback release restored" >&2
        else
            echo "rollback compensation failed; inspect the deployment immediately" >&2
            KEEP_CANDIDATE=1
        fi
    fi

    if [ "$KEEP_CANDIDATE" -ne 1 ]; then
        rm -f "$CANDIDATE_RELEASE"
        rm -f "$PREVIOUS_METADATA" "$TARGET_METADATA"
    fi
    exit "$exit_code"
}

trap finish EXIT
trap 'exit 130' INT TERM HUP

ROLLBACK_STARTED=1
activate_release "$TARGET_METADATA"
atomic_copy "$PREVIOUS_METADATA" "$FAILED_RELEASE"

# Exclude catchable signals while runtime and both state pointers become a
# consistent pair. SIGKILL leaves candidate.env for the next locked recovery.
trap '' INT TERM HUP
atomic_copy "$PREVIOUS_METADATA" "$KNOWN_GOOD_RELEASE"
mv "$CANDIDATE_RELEASE" "$CURRENT_RELEASE"
ROLLBACK_COMPLETE=1
trap 'exit 130' INT TERM HUP

rm -f "$PREVIOUS_METADATA" "$TARGET_METADATA"
trap - EXIT INT TERM HUP

echo "restored known-good immutable image and matching edge configuration"
