#!/bin/bash
# Sourced by launchers. No commands run merely by sourcing this file.
MR_RELEASE=3.3
MR_HELPER_ROOT=/usr/local/libexec/MultiRouteResume/3.3
MR_JRE_ROOT='jdk-21.0.12.1+1-jre'
mr_note(){ printf '%s\n' "$*"; }
mr_die(){ printf '\nMultiRoute Resume: %s\n' "$*" >&2; exit 1; }
mr_cleanup(){
    if [ -n "${MR_TEMP:-}" ] && [ -d "$MR_TEMP" ]; then
        case "$MR_TEMP" in "$MR_USER_ROOT"/.install-*|"$MR_USER_ROOT"/.build-*) rm -rf -- "$MR_TEMP";; esac
    fi
}
mr_environment(){
    [ "$(id -u)" != 0 ] || mr_die 'Run the launcher as your normal user, without sudo. Only the network helper is elevated.'
    [ "$(uname -s)" = Linux ] || mr_die 'This launcher requires Linux.'
    MR_ARCH=$(uname -m)
    case "$MR_ARCH" in x86_64) MR_JRE_ARCH=x64;; aarch64|arm64) MR_ARCH=aarch64;MR_JRE_ARCH=aarch64;; *) mr_die "Unsupported architecture: $MR_ARCH";; esac
    if command -v ldd >/dev/null && ldd --version 2>&1 | grep -qi musl; then mr_die 'This package requires a glibc Linux distribution. Alpine/musl needs a separate build.'; fi
    MR_HEADLESS=0
    for MR_ARGUMENT in "$@"; do [ "$MR_ARGUMENT" != --headless ] && [ "$MR_ARGUMENT" != --no-browser ] || MR_HEADLESS=1; done
    [ "$MR_HEADLESS" = 1 ] || [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ] || mr_die 'No desktop session was found. Launch from the desktop or add --headless for the local API.'
    if [ "$MR_HEADLESS" != 1 ] && [ -z "${DISPLAY:-}" ] && [ -n "${WAYLAND_DISPLAY:-}" ]; then
        mr_note 'Warning: Wayland was found without DISPLAY. Swing requires an available XWayland/X11 display; enable XWayland in your desktop session or use --headless.'
    fi
    [ -d "$HOME" ] && [ -w "$HOME" ] || mr_die 'Your home directory must exist and be writable.'
    MR_FREE_KB=$(df -Pk "$HOME" | awk 'END {print $4}')
    case "$MR_FREE_KB" in ''|*[!0-9]*) mr_die 'Cannot determine available disk space.';; esac
    if [ "$MR_FREE_KB" -lt 524288 ]; then
        [ "${1:-deploy}" != deploy ] || mr_die 'Deployment requires at least 512 MiB of free disk space; downloads need additional space.'
        mr_note 'Warning: less than 512 MiB is available. Check space before downloading.'
    fi
    MR_USER_ROOT="$HOME/.local/share/MultiRouteResume"
    MR_DATA="$MR_USER_ROOT/data"
    MR_RUNTIME_ROOT="$MR_USER_ROOT/runtime"
    mkdir -p -- "$MR_USER_ROOT" "$MR_RUNTIME_ROOT" "$MR_DATA" || mr_die 'Cannot create application directories.'
    [ -w "$MR_USER_ROOT" ] || mr_die 'The application directory is not writable.'
    [ -f "$MR_SOURCE/MultiRoute-Resume.jar" ] || mr_die 'MultiRoute-Resume.jar is missing. Extract the full distribution.'
    command -v sudo >/dev/null || mr_die 'sudo is required to install and authorize the protected network helper.'
    mr_note "Linux $MR_ARCH; application data: $MR_DATA"
    mr_torrent_compatibility
    mr_network_check
}
mr_torrent_compatibility(){
    local description major minor
    MR_GLIBC_VERSION='unknown';MR_TORRENT_COMPATIBLE=unknown
    description=$(getconf GNU_LIBC_VERSION 2>/dev/null || true)
    if [ -z "$description" ];then description=$(ldd --version 2>/dev/null | head -n 1 || true);fi
    MR_GLIBC_VERSION=$(printf '%s\n' "$description" | sed -n 's/.* \([0-9][0-9]*\.[0-9][0-9]*\)\([[:space:]].*\)\{0,1\}$/\1/p' | head -n 1)
    case "$MR_GLIBC_VERSION" in ''|*[!0-9.]*)
        MR_GLIBC_VERSION=unknown
        mr_note 'Warning: the glibc version could not be confirmed. Bundled BitTorrent/magnet libraries require glibc 2.38 or newer; deployment alone does not prove torrent compatibility.'
        return;;esac
    major=${MR_GLIBC_VERSION%%.*};minor=${MR_GLIBC_VERSION#*.}
    if [ "$major" -gt 2 ] || { [ "$major" -eq 2 ] && [ "$minor" -ge 38 ]; };then
        MR_TORRENT_COMPATIBLE=1
        mr_note "BitTorrent/magnet ABI prerequisite: glibc $MR_GLIBC_VERSION is compatible with the bundled native libraries."
    else
        MR_TORRENT_COMPATIBLE=0
        mr_note "Warning: glibc $MR_GLIBC_VERSION is older than 2.38. Bundled BitTorrent/magnet will not load; Java/HTTP deployment may continue. This installer does not upgrade glibc or your operating system."
    fi
}
mr_network_check(){
    local links
    MR_ETHERNET_CHECK=unknown
    if command -v ip >/dev/null;then
        links=$(ip -o link show 2>/dev/null || true)
        if printf '%s\n' "$links" | awk '/link\/ether/ && /<[^>]*UP[^>]*>/ {found=1} END {exit !found}';then
            MR_ETHERNET_CHECK=candidate
            mr_note 'An active Ethernet-format link was found. The network helper will validate its adapter and gateway subnet before transfer.'
        else
            MR_ETHERNET_CHECK=unavailable
            mr_note 'Warning: no active Ethernet-format link was found. Connect an Ethernet adapter on the gateway LAN before testing or downloading. Installation can continue.'
        fi
    elif command -v ifconfig >/dev/null;then
        links=$(ifconfig -a 2>/dev/null || true)
        if printf '%s\n' "$links" | awk '/^[^ \t]/ {up=($0 ~ /UP/)} /ether[ \t]/ && up {found=1} END {exit !found}';then
            MR_ETHERNET_CHECK=candidate
            mr_note 'An active Ethernet-format link was found. The network helper performs the final adapter/subnet validation.'
        else
            MR_ETHERNET_CHECK=unavailable
            mr_note 'Warning: no active Ethernet-format link was found. Connect Ethernet to the gateway LAN before downloading; installation can continue.'
        fi
    else
        mr_note 'Warning: ip/ifconfig is unavailable, so the read-only adapter check was skipped. Deployment installs the needed iproute tool; no OS network settings are changed.'
    fi
}
mr_java_ok(){
    [ -x "$1" ] || return 1
    local props major java_arch modules module
    props=$("$1" -XshowSettings:properties -version 2>&1) || return 1
    major=$(printf '%s\n' "$props" | sed -n 's/^[[:space:]]*java.specification.version = //p' | head -n 1)
    java_arch=$(printf '%s\n' "$props" | sed -n 's/^[[:space:]]*os.arch = //p' | head -n 1)
    case "$major" in ''|*[!0-9]*) return 1;; esac
    [ "$major" -ge 17 ] || return 1
    case "$MR_ARCH:$java_arch" in x86_64:amd64|x86_64:x86_64|aarch64:aarch64|aarch64:arm64) ;; *) return 1;; esac
    modules=$("$1" --list-modules 2>/dev/null) || return 1
    for module in java.base java.desktop java.net.http java.xml jdk.httpserver;do
        printf '%s\n' "$modules" | sed 's/@.*//' | grep -Fx "$module" >/dev/null || return 1
    done
    "$1" --add-modules java.desktop,java.net.http,java.xml,jdk.httpserver --validate-modules >/dev/null 2>&1 || return 1
}
mr_java_choose(){
    local candidate
    MR_JAVA=''
    if [ -n "${JAVA_EXE:-}" ]; then
        if mr_java_ok "$JAVA_EXE";then MR_JAVA=$JAVA_EXE;return;fi
        mr_note 'Warning: JAVA_EXE is not a complete native Java 17+ runtime with java.desktop, java.net.http, java.xml and jdk.httpserver. Checking other runtimes; a verified private Temurin runtime will be installed if needed.'
    fi
    if [ -n "${JAVA_HOME:-}" ] && mr_java_ok "$JAVA_HOME/bin/java"; then MR_JAVA="$JAVA_HOME/bin/java";return;fi
    if [ -f "$MR_USER_ROOT/java-path.config" ]; then
        IFS= read -r candidate < "$MR_USER_ROOT/java-path.config" || true
        if mr_java_ok "$candidate"; then MR_JAVA=$candidate;return;fi
    fi
    candidate="$MR_RUNTIME_ROOT/temurin-21-$MR_ARCH/bin/java"
    if mr_java_ok "$candidate"; then MR_JAVA=$candidate;return;fi
    candidate=$(command -v java || true)
    if [ -n "$candidate" ] && mr_java_ok "$candidate"; then MR_JAVA=$candidate;fi
}
mr_sha(){ local value;value=$(sha256sum -- "$1") || return 1;value=${value%% *};[ "${#value}" = 64 ] || return 1;printf '%s\n' "$value"; }
mr_compile_available(){
    command -v cc >/dev/null && command -v pkg-config >/dev/null && pkg-config --exists libpcap && command -v readelf >/dev/null
}
mr_runtime_libraries_available(){
    [ "$MR_HEADLESS" != 1 ] || return 0
    local available library
    command -v ldconfig >/dev/null || return 1
    available=$(ldconfig -p 2>/dev/null) || return 1
    for library in libX11.so.6 libXext.so.6 libXi.so.6 libXrender.so.1 libXtst.so.6 libfontconfig.so.1;do
        printf '%s\n' "$available" | grep -F "$library " >/dev/null || return 1
    done
}
mr_dependencies(){
    if mr_compile_available && mr_runtime_libraries_available && command -v curl >/dev/null && command -v tar >/dev/null && command -v sha256sum >/dev/null && { command -v ip >/dev/null || command -v ifconfig >/dev/null; }; then return;fi
    mr_note 'Installing the C compiler, libpcap development files and runtime prerequisites using the system package manager.'
    sudo -v || mr_die 'Administrator authorization was declined.'
    if command -v apt-get >/dev/null; then
        sudo -- apt-get update
        sudo -- apt-get install --no-install-recommends -y build-essential binutils libpcap-dev pkg-config curl ca-certificates tar gzip iproute2 libx11-6 libxext6 libxi6 libxrender1 libxtst6 fontconfig
    elif command -v dnf >/dev/null; then
        sudo -- dnf install -y gcc glibc-devel make binutils libpcap-devel pkgconf-pkg-config curl ca-certificates tar gzip iproute libX11 libXext libXi libXrender libXtst fontconfig
    elif command -v pacman >/dev/null; then
        sudo -- pacman -S --needed --noconfirm gcc make binutils pkgconf libpcap curl ca-certificates tar gzip iproute2 libx11 libxext libxi libxrender libxtst fontconfig
    else mr_die 'Unsupported package manager. Install a C compiler, binutils, pkg-config, libpcap headers, curl and desktop Java runtime libraries, then launch again.';fi
    mr_compile_available || mr_die 'The C compiler or libpcap headers are still missing.'
    mr_runtime_libraries_available || mr_die 'Required X11 or font libraries are still missing. Use --headless for the API, or complete desktop runtime setup.'
    mr_network_check
}
mr_java_install(){
    local url expected archive actual member unpack destination candidate previous
    case "$MR_JRE_ARCH" in
        x64) url='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_x64_linux_hotspot_21.0.12.1_1.tar.gz';expected=2413149700df0f7d440500a84a8f764c535f21e5a5e87d38328b64eec2c5b500;;
        aarch64) url='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_aarch64_linux_hotspot_21.0.12.1_1.tar.gz';expected=14be1f35ebdbd1f6e8d57eb911a3ffb74d6d9aa255abc5daf2b1302002cf2cf2;;
        *) mr_die 'Unsupported Java architecture.';;
    esac
    mr_note 'Downloading the pinned official Temurin 21 runtime for this application only.'
    MR_TEMP=$(mktemp -d "$MR_USER_ROOT/.install-XXXXXXXX") || mr_die 'Cannot create runtime staging directory.'
    archive="$MR_TEMP/runtime.tar.gz"
    curl --fail --location --proto '=https' --proto-redir '=https' --connect-timeout 20 --max-time 900 --retry 2 --max-filesize 536870912 --output "$archive" "$url" || mr_die 'Java download failed. Check the connection and try again.'
    actual=$(mr_sha "$archive") || mr_die 'Cannot calculate the Java checksum.'
    [ "$actual" = "$expected" ] || mr_die 'Java archive SHA-256 mismatch; installation stopped.'
    tar -tzf "$archive" > "$MR_TEMP/members" || mr_die 'The runtime archive cannot be read.'
    [ -s "$MR_TEMP/members" ] || mr_die 'The runtime archive is empty.'
    while IFS= read -r member;do
        case "$member" in "$MR_JRE_ROOT"|"$MR_JRE_ROOT/"|"$MR_JRE_ROOT/"*) ;;*) mr_die 'Unexpected Java archive path.';;esac
        case "/$member/" in */../*|*/./*) mr_die 'Unsafe Java archive path.';;esac
    done < "$MR_TEMP/members"
    unpack="$MR_TEMP/unpacked";mkdir -- "$unpack";tar -xzf "$archive" -C "$unpack" || mr_die 'Java extraction failed.'
    candidate="$unpack/$MR_JRE_ROOT/bin/java";[ ! -L "$candidate" ] && mr_java_ok "$candidate" || mr_die 'The downloaded Java runtime failed version, native architecture or required-module validation.'
    destination="$MR_RUNTIME_ROOT/temurin-21-$MR_ARCH";previous=''
    [ ! -L "$destination" ] || mr_die 'A runtime destination cannot be a symbolic link.'
    if [ -e "$destination" ]; then previous="$destination.previous-$(date +%Y%m%d-%H%M%S)-$$";mv -- "$destination" "$previous";fi
    if ! mv -- "$unpack/$MR_JRE_ROOT" "$destination";then [ -z "$previous" ] || mv -- "$previous" "$destination";mr_die 'Cannot commit Java; the previous runtime was restored.';fi
    MR_JAVA="$destination/bin/java";mr_cleanup;MR_TEMP=''
}
mr_java_save(){
    printf '%s\n' "$MR_JAVA" > "$MR_USER_ROOT/java-path.config"
    chmod 600 "$MR_USER_ROOT/java-path.config"
}
mr_safe_root_directory(){
    local mode
    [ -d "$1" ] && [ ! -L "$1" ] && [ "$(stat -c '%u' -- "$1")" = 0 ] || return 1
    mode=$(stat -c '%a' -- "$1") || return 1
    [ "$((8#$mode & 0022))" = 0 ]
}
mr_helper_ok(){
    local directory mode expected
    for directory in / /usr /usr/local /usr/local/libexec /usr/local/libexec/MultiRouteResume "$MR_HELPER_ROOT";do mr_safe_root_directory "$directory" || return 1;done
    [ -f "$MR_HELPER_ROOT/gateway-net" ] && [ ! -L "$MR_HELPER_ROOT/gateway-net" ] && [ -x "$MR_HELPER_ROOT/gateway-net" ] && [ "$(stat -c '%u' -- "$MR_HELPER_ROOT/gateway-net")" = 0 ] || return 1
    mode=$(stat -c '%a' -- "$MR_HELPER_ROOT/gateway-net");[ "$((8#$mode & 0022))" = 0 ] || return 1
    [ -f "$MR_USER_ROOT/helper-sha256.config" ] || return 1
    IFS= read -r expected < "$MR_USER_ROOT/helper-sha256.config" || return 1
    [ "${#expected}" = 64 ] && [ "$(mr_sha "$MR_HELPER_ROOT/gateway-net")" = "$expected" ]
}
mr_helper_deploy(){
    local built expected
    MR_TEMP=$(mktemp -d "$MR_USER_ROOT/.build-XXXXXXXX") || mr_die 'Cannot create native build directory.'
    built="$MR_TEMP/gateway-net"
    MULTIROUTE_BUILD_OUTPUT="$built" /bin/bash "$MR_SOURCE/native/source/build_linux.sh" || mr_die 'Network helper compilation or its self-test failed.'
    expected=$(mr_sha "$built") || mr_die 'Cannot calculate the helper checksum.'
    sudo -- /bin/bash "$MR_SOURCE/linux/install-helper.sh" "$built" "$expected" || mr_die 'Protected network helper installation failed.'
    printf '%s\n' "$expected" > "$MR_USER_ROOT/helper-sha256.config";chmod 600 "$MR_USER_ROOT/helper-sha256.config"
    mr_helper_ok || mr_die 'The installed helper failed protected-path validation.'
    mr_cleanup;MR_TEMP=''
}
mr_desktop_install(){
    local applications entry launch quoted
    applications="$HOME/.local/share/applications";mkdir -p -- "$applications"
    printf '%s\n' "$MR_SOURCE" > "$MR_USER_ROOT/program-path.config";chmod 600 "$MR_USER_ROOT/program-path.config"
    launch="$MR_USER_ROOT/launch-installed.sh"
    cat > "$launch" <<'LAUNCH'
#!/bin/bash
set -eu
MR_USER_ROOT="$HOME/.local/share/MultiRouteResume"
IFS= read -r MR_PROGRAM < "$MR_USER_ROOT/program-path.config"
[ -f "$MR_PROGRAM/Start-Linux.sh" ] || { printf '%s\n' 'The application folder moved. Run Deploy-Linux.sh from its new location.' >&2; exit 1; }
exec /bin/bash "$MR_PROGRAM/Start-Linux.sh" "$@"
LAUNCH
    chmod 700 "$launch"
    quoted=${launch//\\/\\\\};quoted=${quoted//\"/\\\"};quoted=${quoted//\$/\\\$};quoted=${quoted//\`/\\\`}
    # Desktop entries decode string escapes before argument quoting; preserve both layers.
    quoted=${quoted//\\/\\\\};quoted=${quoted//%/%%}
    entry="$applications/MultiRouteResume.desktop"
    {
        printf '%s\n' '[Desktop Entry]' 'Version=1.0' 'Type=Application' 'Name=MultiRoute Resume' 'Comment=Resumable downloads through selected gateways' 'Comment[ru]=Докачка через выбранные шлюзы'
        printf 'Exec=/bin/bash "%s"\n' "$quoted"
        printf '%s\n' 'TryExec=/bin/bash' 'Terminal=true' 'Icon=folder-download' 'Categories=Network;FileTransfer;'
    } > "$entry"
    chmod 700 "$entry"
    if command -v update-desktop-database >/dev/null;then update-desktop-database "$applications" || true;fi
    mr_note "Created application launcher: $entry"
}
mr_launch(){
    mr_helper_ok || mr_die 'Protected helper validation failed. Run Deploy-Linux.sh.'
    sudo -v || mr_die 'Administrator authorization was declined.'
    sudo -n -- "$MR_HELPER_ROOT/gateway-net" --self-test || mr_die 'Helper authorization or offline self-test failed.'
    mr_note 'Keep this terminal open while downloading; Java itself runs without administrator privileges.'
    cd -- "$MR_SOURCE"
    "$MR_JAVA" -jar "$MR_SOURCE/MultiRoute-Resume.jar" --data-dir "$MR_DATA" "$@"
}
