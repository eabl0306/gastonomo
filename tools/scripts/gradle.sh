#!/usr/bin/env bash
# Llama al wrapper de Gradle de apps/android con un JDK resuelto a mano.
# Lo usan el justfile y los hooks, que no siempre tienen java en el PATH.
#
#   bash tools/scripts/gradle.sh check
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=./jdk-env.sh
source "${root}/tools/scripts/jdk-env.sh"

cd "${root}/apps/android"
exec ./gradlew "$@"
