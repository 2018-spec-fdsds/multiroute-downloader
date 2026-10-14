#!/bin/bash
set -euo pipefail
MR_SOURCE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)"
exec /bin/bash "$MR_SOURCE/Start-Linux.sh" "$@"
