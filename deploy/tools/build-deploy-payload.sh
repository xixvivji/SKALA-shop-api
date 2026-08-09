#!/bin/sh

set -eu

if [ "$#" -ne 4 ]; then
    echo "usage: build-deploy-payload.sh <release-id> <source-revision> <repository@sha256:digest> <deploy-script>" >&2
    exit 2
fi

RELEASE_ID_VALUE=$1
SOURCE_REVISION_VALUE=$2
BACKEND_IMAGE_REF_VALUE=$3
DEPLOY_SCRIPT_VALUE=$4

case "$RELEASE_ID_VALUE" in
    *[!A-Za-z0-9._-]*|'')
        echo "invalid release ID" >&2
        exit 1
        ;;
esac
case "$SOURCE_REVISION_VALUE" in
    *[!0-9a-f]*|'')
        echo "invalid source revision" >&2
        exit 1
        ;;
esac
[ "${#SOURCE_REVISION_VALUE}" -eq 40 ] || {
    echo "source revision must be a full commit SHA" >&2
    exit 1
}
case "$BACKEND_IMAGE_REF_VALUE" in
    *[!A-Za-z0-9._/@:-]*|'')
        echo "invalid backend image reference" >&2
        exit 1
        ;;
esac
case "$BACKEND_IMAGE_REF_VALUE" in
    *@sha256:*) ;;
    *)
        echo "backend image must use an immutable sha256 digest" >&2
        exit 1
        ;;
esac
IMAGE_DIGEST_VALUE=${BACKEND_IMAGE_REF_VALUE##*@sha256:}
[ "${#IMAGE_DIGEST_VALUE}" -eq 64 ] || {
    echo "backend image digest must contain 64 hexadecimal characters" >&2
    exit 1
}
case "$IMAGE_DIGEST_VALUE" in
    *[!0-9a-f]*)
        echo "backend image digest must be lowercase hexadecimal" >&2
        exit 1
        ;;
esac
case "$DEPLOY_SCRIPT_VALUE" in
    deploy.sh|bootstrap-tls.sh) ;;
    *)
        echo "invalid deployment script" >&2
        exit 1
        ;;
esac

TOOL_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_DIR=$(CDPATH= cd -- "$TOOL_DIR/../.." && pwd)
REMOTE_RELEASE_DIR_VALUE="/opt/skala-shop/releases/$RELEASE_ID_VALUE"
TEMP_BASE=${TMPDIR:-/tmp}
ARCHIVE_SOURCE=$(mktemp "${TEMP_BASE%/}/skala-deploy-payload.XXXXXX")

cleanup() {
    rm -f "$ARCHIVE_SOURCE"
}
trap cleanup EXIT
trap 'exit 130' INT TERM HUP

(
    cd "$REPOSITORY_DIR"
    # macOS tar의 extended attribute와 pipe block padding도 제외해 Linux runner와
    # 동일하게 작은 payload를 만듭니다.
    COPYFILE_DISABLE=1 tar --no-xattrs -czf "$ARCHIVE_SOURCE" \
        deploy/compose.prod.yml deploy/nginx deploy/monitoring \
        deploy/scripts/deploy.sh \
        deploy/scripts/bootstrap-tls.sh \
        deploy/scripts/release-lib.sh \
        deploy/scripts/rollback.sh
)
DEPLOY_ARCHIVE_BASE64=$(base64 < "$ARCHIVE_SOURCE" | tr -d '\n')

# SSM command에 archive를 한 번만 포함합니다. 이전 방식처럼 이 전체 script를 다시
# base64로 감싸면 24KB API 제한을 불필요하게 소모합니다. 호출자는 최종 길이를
# 검사한 뒤 jq --arg로 AWS CLI parameter JSON에 안전하게 넣습니다.
cat <<EOF
set -eu
RELEASE_ID='$RELEASE_ID_VALUE'
SOURCE_REVISION='$SOURCE_REVISION_VALUE'
BACKEND_IMAGE_REF='$BACKEND_IMAGE_REF_VALUE'
DEPLOY_SCRIPT='$DEPLOY_SCRIPT_VALUE'
REMOTE_RELEASE_DIR='$REMOTE_RELEASE_DIR_VALUE'
DEPLOY_ARCHIVE_BASE64='$DEPLOY_ARCHIVE_BASE64'
ARCHIVE_FILE="/tmp/skala-shop-$RELEASE_ID_VALUE.tar.gz"
trap 'rm -f "\$ARCHIVE_FILE"' EXIT
install -d -m 755 -o ubuntu -g ubuntu "\$REMOTE_RELEASE_DIR"
printf '%s' "\$DEPLOY_ARCHIVE_BASE64" | base64 -d > "\$ARCHIVE_FILE"
tar -xzf "\$ARCHIVE_FILE" -C "\$REMOTE_RELEASE_DIR" --strip-components=1
chown -R ubuntu:ubuntu "\$REMOTE_RELEASE_DIR"
chmod 700 "\$REMOTE_RELEASE_DIR"/scripts/*.sh
runuser -u ubuntu -- env HOME=/home/ubuntu "\$REMOTE_RELEASE_DIR/scripts/\$DEPLOY_SCRIPT" "\$RELEASE_ID" "\$BACKEND_IMAGE_REF"
EOF
