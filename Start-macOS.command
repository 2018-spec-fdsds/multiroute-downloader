#!/bin/bash
set -eu
umask 077
PATH=/usr/bin:/bin:/usr/sbin:/sbin
export PATH
MR_SOURCE=$(cd "$(dirname "$0")" && pwd -P) || exit 1
. "$MR_SOURCE/macOS/deploy-common.sh"
mr_environment start "$@"
mr_java_choose
if [ -z "$MR_JAVA" ] || [ ! -x "$MR_HELPER_ROOT/gateway-net" ] || [ ! -f "$MR_USER_ROOT/gateway-helper.sha256" ]; then
    exec /bin/bash "$MR_SOURCE/Deploy-macOS.command" "$@"
fi
mr_launch "$@"
