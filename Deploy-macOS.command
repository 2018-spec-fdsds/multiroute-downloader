#!/bin/bash
set -eu
umask 077
PATH=/usr/bin:/bin:/usr/sbin:/sbin
export PATH
MR_SOURCE=$(cd "$(dirname "$0")" && pwd -P) || exit 1
. "$MR_SOURCE/macOS/deploy-common.sh"
trap mr_cleanup EXIT
trap 'printf "\nDeployment was cancelled. Reopen Deploy-macOS.command when ready to resume.\n" >&2; exit 130' INT TERM
mr_environment deploy "$@"
mr_lock
mr_helper_build
mr_ensure_java
mr_helper_install
mr_app_install
mr_cleanup
MR_LOCK_OWNED=0
MR_SOURCE="$MR_INSTALLED_APP/Contents/Resources/app"
mr_launch "$@"
