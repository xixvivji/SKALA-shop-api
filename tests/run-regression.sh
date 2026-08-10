#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

if [ "${SKIP_NPM_CI:-false}" != "true" ]; then
  npm --prefix frontend ci
fi

./gradlew clean test --no-build-cache --no-daemon
node --check frontend/runtime-config.js
node --check frontend/config.js
node --check frontend/js/api.js
node --check frontend/js/app.js
node frontend/scripts/validate-static.mjs
npm --prefix frontend run build
npm --prefix frontend run test:e2e
sh deploy/tests/monitoring-config-test.sh
sh deploy/tests/release-flow-test.sh

echo "All local regression checks passed."
