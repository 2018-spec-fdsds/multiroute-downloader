#!/bin/bash
# Native Linux build. Packages/privileges are handled by the separate user bootstrap.
set -euo pipefail
BASE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)"
[ "$(uname -s)" = Linux ] || { printf '%s\n' 'Linux is required.' >&2;exit 2; }
case "$(uname -m)" in x86_64|aarch64|arm64) ;;*) printf '%s\n' 'Unsupported Linux architecture.' >&2;exit 2;;esac
CC="${CC:-cc}"
command -v "$CC" >/dev/null && command -v pkg-config >/dev/null && pkg-config --exists libpcap || { printf '%s\n' 'C compiler, pkg-config and libpcap development headers are required.' >&2;exit 3; }
[ -f "$BASE/lwip/src/include/lwip/init.h" ] || { printf '%s\n' 'Bundled lwIP source is missing.' >&2;exit 4; }
OUT="${MULTIROUTE_BUILD_OUTPUT:-$BASE/../linux-$(uname -m)/gateway-net}"
BUILD=$(mktemp -d "${TMPDIR:-/tmp}/multiroute-linux-build.XXXXXXXX")
trap 'rm -rf -- "$BUILD"' EXIT
SOURCES=("$BASE/gateway_posix.c" "$BASE/posix/sys_arch.c")
for DIRECTORY in "$BASE/lwip/src/core" "$BASE/lwip/src/core/ipv4" "$BASE/lwip/src/api";do
    for SOURCE in "$DIRECTORY"/*.c;do SOURCES+=("$SOURCE");done
done
SOURCES+=("$BASE/lwip/src/netif/ethernet.c")
read -r -a PCAP_CFLAGS <<< "$(pkg-config --cflags libpcap)"
read -r -a PCAP_LIBS <<< "$(pkg-config --libs libpcap)"
"$CC" -std=c11 -O2 -fstack-protector-strong -D_DEFAULT_SOURCE -pthread -Wall -Wextra -Werror=implicit-function-declaration -Werror=format \
    -I"$BASE" -I"$BASE/posix/include" -I"$BASE/lwip/src/include" "${PCAP_CFLAGS[@]}" "${SOURCES[@]}" "${PCAP_LIBS[@]}" -o "$BUILD/gateway-net"
"$BUILD/gateway-net" --self-test
mkdir -p -- "$(dirname -- "$OUT")"
install -m 0755 -- "$BUILD/gateway-net" "$OUT"
printf 'Built and checked native Linux helper: %s\n' "$OUT"
