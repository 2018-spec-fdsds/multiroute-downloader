#!/bin/bash
set -euo pipefail
PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin
export PATH
umask 077
MR_SOURCE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)"
# Keep the authorization and Java process in one terminal session.
if [ ! -t 0 ] && [ "${MR_IN_TERMINAL:-0}" != 1 ] && [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ]; then
    if command -v x-terminal-emulator >/dev/null; then exec x-terminal-emulator -e /bin/bash "$MR_SOURCE/Deploy-Linux.sh" "$@"; fi
    if command -v gnome-terminal >/dev/null; then exec gnome-terminal -- /bin/bash "$MR_SOURCE/Deploy-Linux.sh" "$@"; fi
    if command -v konsole >/dev/null; then exec konsole -e /bin/bash "$MR_SOURCE/Deploy-Linux.sh" "$@"; fi
fi
export MR_IN_TERMINAL=1
. "$MR_SOURCE/linux/deploy-common.sh"
trap 'mr_cleanup' EXIT
mr_environment deploy "$@"
mr_dependencies
mr_java_choose
if [ -z "$MR_JAVA" ]; then mr_java_install; fi
mr_helper_deploy
mr_desktop_install
mr_java_save
mr_note 'Deployment is complete. Starting MultiRoute Downloader as your normal user.'
mr_launch "$@"
