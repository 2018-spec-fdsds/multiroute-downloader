#!/bin/bash
# Native architecture build with Apple's Command Line Tools and system libpcap.
# Nothing installs Java, packages, network interfaces, routes or BPF permissions.
set -euo pipefail
BASE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)"
SYSTEM="$(uname -s)"
if [ "$SYSTEM" != Darwin ] && [ "${MULTIROUTE_POSIX_TEST:-0}" != 1 ]; then
  echo 'Build requires macOS 11 or later and Apple Command Line Tools.' >&2
  exit 2
fi
OUT="${MULTIROUTE_BUILD_OUTPUT:-$BASE/../macos/gateway-net}"
CC="${CC:-cc}"
if [ "$SYSTEM" = Darwin ]; then
  command -v xcrun >/dev/null || { echo 'Apple Command Line Tools are required. Run xcode-select --install.' >&2; exit 3; }
  CC="$(xcrun --find clang)"
  SDK="$(xcrun --sdk macosx --show-sdk-path)" || { echo 'macOS SDK could not be located. Install Apple Command Line Tools.' >&2; exit 3; }
  [ -d "$SDK" ] || { echo "macOS SDK directory is unavailable: $SDK" >&2; exit 3; }
  ARCH="${ARCH:-$(uname -m)}"
  case "$ARCH" in arm64|x86_64) ;; *) echo "Unsupported macOS architecture: $ARCH" >&2; exit 3;; esac
  PLATFORM=(-isysroot "$SDK" -arch "$ARCH" -mmacosx-version-min=11.0)
else
  PLATFORM=()
fi
[ -f "$BASE/lwip/src/include/lwip/init.h" ] || { echo 'Bundled lwIP sources are missing.' >&2; exit 4; }
BUILD="$(mktemp -d "${TMPDIR:-/tmp}/multiroute-macos-build.XXXXXXXX")"
trap 'rm -rf -- "$BUILD"' EXIT
SOURCES=("$BASE/gateway_posix.c" "$BASE/posix/sys_arch.c")
for DIRECTORY in "$BASE/lwip/src/core" "$BASE/lwip/src/core/ipv4" "$BASE/lwip/src/api"; do
  for SOURCE in "$DIRECTORY"/*.c; do SOURCES+=("$SOURCE"); done
done
SOURCES+=("$BASE/lwip/src/netif/ethernet.c")
"$CC" -std=c11 -O2 -fstack-protector-strong -D_DEFAULT_SOURCE -pthread \
  -Wall -Wextra -Werror=implicit-function-declaration -Werror=format \
  "${PLATFORM[@]}" ${MULTIROUTE_TEST_CFLAGS:-} \
  -I"$BASE" -I"$BASE/posix/include" -I"$BASE/lwip/src/include" \
  "${SOURCES[@]}" -lpcap ${MULTIROUTE_TEST_LDFLAGS:-} -o "$BUILD/gateway-net"
if [ "$SYSTEM" = Darwin ]; then
  /usr/bin/codesign --force --sign - "$BUILD/gateway-net"
fi
"$BUILD/gateway-net" --self-test
mkdir -p -- "$(dirname -- "$OUT")"
install -m 0755 "$BUILD/gateway-net" "$OUT"
echo "Built and checked: $OUT"
