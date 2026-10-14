#!/bin/bash
set -euo pipefail
PATH=/usr/bin:/bin:/usr/sbin:/sbin
export PATH
umask 022
[ "$(id -u)" = 0 ] || { printf '%s\n' 'Run this installer through Deploy-Linux.sh.' >&2;exit 1; }
[ "$#" = 2 ] || exit 1
source_file=$1;expected=$2
case "$source_file" in /*) ;;*) exit 1;;esac
case "$expected" in ''|*[!0-9a-f]*) exit 1;;esac
[ "${#expected}" = 64 ] && [ -f "$source_file" ] && [ ! -L "$source_file" ] || exit 1
destination=/usr/local/libexec/MultiRouteResume/3.0
verify_directory(){
    local mode
    [ -d "$1" ] && [ ! -L "$1" ] && [ "$(stat -c '%u' -- "$1")" = 0 ] || { printf 'Unsafe protected helper directory: %s\n' "$1" >&2;exit 1; }
    mode=$(stat -c '%a' -- "$1")
    [ "$((8#$mode & 0022))" = 0 ] || { printf 'Protected directory is writable by another account: %s\n' "$1" >&2;exit 1; }
}
for component in / /usr /usr/local;do verify_directory "$component";done
for component in /usr/local/libexec /usr/local/libexec/MultiRouteResume "$destination";do
    if [ ! -e "$component" ] && [ ! -L "$component" ];then mkdir -m 755 -- "$component";chown 0:0 "$component";fi
    verify_directory "$component"
done
target="$destination/gateway-net"
[ ! -L "$target" ] || { printf '%s\n' 'A helper destination cannot be a symbolic link.' >&2;exit 1; }
if [ -e "$target" ] && [ ! -f "$target" ];then printf '%s\n' 'The helper destination must be a regular file.' >&2;exit 1;fi
temporary=$(mktemp "$destination/.gateway-net-XXXXXXXX")
trap '[ -z "${temporary:-}" ] || rm -f -- "$temporary"' EXIT
install -o 0 -g 0 -m 755 -- "$source_file" "$temporary"
actual=$(sha256sum -- "$temporary");actual=${actual%% *}
[ "$actual" = "$expected" ] || { printf '%s\n' 'Copied helper SHA-256 mismatch.' >&2;exit 1; }
machine=$(readelf -h -- "$temporary" | sed -n 's/^[[:space:]]*Machine:[[:space:]]*//p')
case "$(uname -m):$machine" in x86_64:*X86-64*|aarch64:*AArch64*|arm64:*AArch64*) ;;*) printf '%s\n' 'The helper does not match the native architecture.' >&2;exit 1;;esac
"$temporary" --self-test
mv -f -- "$temporary" "$target";temporary=''
[ -f "$target" ] && [ ! -L "$target" ] && [ "$(stat -c '%u' -- "$target")" = 0 ] || exit 1
mode=$(stat -c '%a' -- "$target");[ "$((8#$mode & 0022))" = 0 ] || exit 1
printf 'Protected network helper installed: %s\n' "$target"
