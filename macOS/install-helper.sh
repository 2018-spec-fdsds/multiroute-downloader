#!/bin/bash
# This installer only commits a verified gateway binary to a fixed versioned directory.
set -eu
PATH=/usr/bin:/bin:/usr/sbin:/sbin
export PATH
umask 022
[ "$(id -u)" = 0 ] || { printf '%s\n' 'Run this component installer through Deploy-macOS.command.' >&2; exit 1; }
[ "$#" = 2 ] || exit 1
source_file=$1
expected=$2
case "$expected" in ''|*[!0-9a-f]*) exit 1;; esac
[ "${#expected}" = 64 ] || exit 1
[ -f "$source_file" ] && [ ! -L "$source_file" ] || exit 1
case "$source_file" in /*) ;; *) exit 1;; esac
destination='/Library/PrivilegedHelperTools/MultiRouteResume/3.0'

verify_directory() {
    [ -d "$1" ] && [ ! -L "$1" ] || { printf 'Unsafe helper directory: %s\n' "$1" >&2; exit 1; }
    [ "$(stat -f '%u' "$1")" = 0 ] || { printf 'Helper directory is not root-owned: %s\n' "$1" >&2; exit 1; }
    local mode
    mode=$(stat -f '%Lp' "$1")
    # Test group/other write bits using shell arithmetic, without changing existing dirs.
    [ "$((8#$mode & 0022))" = 0 ] || { printf 'Helper directory is writable by other accounts: %s\n' "$1" >&2; exit 1; }
}

verify_directory /Library
for component in '/Library/PrivilegedHelperTools' '/Library/PrivilegedHelperTools/MultiRouteResume' "$destination"; do
    if [ ! -e "$component" ] && [ ! -L "$component" ]; then mkdir -m 755 "$component"; chown root:wheel "$component"; fi
    verify_directory "$component"
done
target="$destination/gateway-net"
[ ! -L "$target" ] || { printf '%s\n' 'A symbolic link cannot be used as the installed gateway component.' >&2; exit 1; }
if [ -e "$target" ] && [ ! -f "$target" ]; then printf '%s\n' 'The gateway component destination must be a regular file.' >&2; exit 1; fi
temporary=$(mktemp "$destination/.gateway-net-XXXXXXXX")
trap '[ -z "${temporary:-}" ] || /bin/rm -f -- "$temporary"' EXIT
install -o root -g wheel -m 755 "$source_file" "$temporary"
actual=$(shasum -a 256 "$temporary")
actual=${actual%% *}
[ "$actual" = "$expected" ] || { printf '%s\n' 'Copied gateway component checksum mismatch.' >&2; exit 1; }
"$temporary" --self-test
mv -f "$temporary" "$target"
temporary=''
[ -f "$target" ] && [ ! -L "$target" ] && [ "$(stat -f '%u' "$target")" = 0 ] || exit 1
mode=$(stat -f '%Lp' "$target")
[ "$((8#$mode & 0022))" = 0 ] || exit 1
printf 'Protected gateway component installed: %s\n' "$target"
