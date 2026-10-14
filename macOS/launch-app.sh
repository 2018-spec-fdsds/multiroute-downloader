#!/bin/bash
set -eu
PATH=/usr/bin:/bin:/usr/sbin:/sbin
export PATH
bundle_contents=$(cd "$(dirname "$0")/.." && pwd -P)
# Terminal supplies an administrator password prompt to sudo without elevating Java.
exec /usr/bin/open -a Terminal "$bundle_contents/Resources/app/Start-macOS.command"
