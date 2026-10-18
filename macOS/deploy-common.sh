#!/bin/bash
# Bash 3.4 compatible. Sourced by the two Finder launchers; no commands run on source.
MR_RELEASE=3.4
MR_JRE_RELEASE='jdk-21.0.12.1+1'
MR_JRE_ROOT='jdk-21.0.12.1+1-jre'
MR_HELPER_ROOT='/Library/PrivilegedHelperTools/MultiRouteResume/3.4'

mr_note() { printf '%s\n' "$*"; }
mr_die() { printf '\nMultiRoute Downloader: %s\n' "$*" >&2; exit 1; }

mr_environment() {
    [ "$(id -u)" != 0 ] || mr_die 'Run this launcher as your normal macOS account, without sudo. Only the protected gateway component is authorized separately.'
    [ "$(uname -s)" = Darwin ] || mr_die 'This distribution requires macOS. Use the Windows or Linux launcher on those systems.'
    MR_MAC_VERSION=$(sw_vers -productVersion) || mr_die 'Cannot read the macOS version.'
    MR_MAC_MAJOR=${MR_MAC_VERSION%%.*}
    case "$MR_MAC_MAJOR" in ''|*[!0-9]*) mr_die 'Unrecognized macOS version.';; esac
    [ "$MR_MAC_MAJOR" -ge 11 ] || mr_die 'macOS 11 or newer is required.'
    MR_ARCH=$(uname -m)
    # A Finder/Terminal process started under Rosetta can report x86_64 on an ARM Mac.
    if [ "$(sysctl -n hw.optional.arm64 2>/dev/null || true)" = 1 ]; then MR_ARCH=arm64; fi
    case "$MR_ARCH" in arm64) MR_JRE_ARCH=aarch64;; x86_64) MR_JRE_ARCH=x64;; *) mr_die "Unsupported processor architecture: $MR_ARCH";; esac
    MR_HEADLESS=0
    for MR_ARGUMENT in "$@";do [ "$MR_ARGUMENT" != --headless ] && [ "$MR_ARGUMENT" != --no-browser ] || MR_HEADLESS=1;done
    MR_USER_ROOT="$HOME/Library/Application Support/MultiRoute Resume"
    MR_DATA="$MR_USER_ROOT/data"
    MR_RUNTIME_ROOT="$MR_USER_ROOT/runtime"
    MR_APPLICATIONS="$HOME/Applications"
    MR_INSTALLED_APP="$MR_APPLICATIONS/MultiRoute Downloader.app"
    mkdir -p "$MR_USER_ROOT" "$MR_RUNTIME_ROOT" "$MR_APPLICATIONS" || mr_die 'Cannot create the application folders in your home directory.'
    [ -w "$MR_USER_ROOT" ] && [ -w "$MR_APPLICATIONS" ] || mr_die 'The application and data folders are not writable.'
    MR_FREE_KB=$(df -Pk "$MR_USER_ROOT" | awk 'END {print $4}')
    case "$MR_FREE_KB" in ''|*[!0-9]*) mr_die 'Cannot determine free disk space.';; esac
    if [ "$MR_FREE_KB" -lt 524288 ]; then
        if [ "${1:-deploy}" = deploy ]; then
            mr_die 'At least 512 MiB of free disk space is required for deployment (downloaded files need additional space).'
        else
            mr_note 'Warning: less than 512 MiB is free in the application folder. Check free space before starting new downloads.'
        fi
    fi
    [ -f "$MR_SOURCE/MultiRoute-Downloader.jar" ] || mr_die 'MultiRoute-Downloader.jar is missing. Extract the entire distribution before launching.'
    mr_note "macOS $MR_MAC_VERSION, native architecture $MR_ARCH; data: $MR_DATA"
    mr_torrent_compatibility
    mr_gui_check
    mr_network_check
}

mr_torrent_compatibility() {
    local required
    case "$MR_ARCH" in arm64) required=26;; x86_64) required=15;; *) return 1;; esac
    if [ "$MR_MAC_MAJOR" -ge "$required" ];then
        MR_TORRENT_COMPATIBLE=1
        mr_note "BitTorrent/magnet ABI prerequisite: macOS $MR_MAC_VERSION meets the bundled $MR_ARCH library target ($required+)."
    else
        MR_TORRENT_COMPATIBLE=0
        mr_note "Warning: bundled BitTorrent/magnet requires macOS $required+ on $MR_ARCH. Java/HTTP deployment can continue on macOS 11+, but these torrent libraries will not load. This installer does not upgrade macOS."
    fi
}

mr_gui_check() {
    MR_GUI_CHECK=headless
    [ "$MR_HEADLESS" != 1 ] || return 0
    if command -v launchctl >/dev/null && launchctl print "gui/$(id -u)" >/dev/null 2>&1;then
        MR_GUI_CHECK=available
    else
        MR_GUI_CHECK=unknown
        mr_note 'Warning: the macOS graphical user session could not be confirmed. Use a logged-in desktop/Terminal session for the window, or --headless for the local API. No DISPLAY variable is required for macOS Java.'
    fi
}

mr_network_check() {
    local links
    MR_ETHERNET_CHECK=unknown
    if command -v ifconfig >/dev/null;then
        links=$(ifconfig -a 2>/dev/null || true)
        if printf '%s\n' "$links" | awk '/^[^ \t]/ {ether=0; active=0} /[ \t]ether[ \t]/ {ether=1} /status: active/ {active=1} ether && active {found=1} END {exit !found}';then
            MR_ETHERNET_CHECK=candidate
            mr_note 'An active Ethernet-format link was found. The network helper will verify a supported Ethernet adapter and gateway subnet before transfer; Wi-Fi/VPN-only operation is unsupported.'
        else
            MR_ETHERNET_CHECK=unavailable
            mr_note 'Warning: no active Ethernet-format link was found. Connect an Ethernet adapter to the gateway LAN before testing or downloading. Deployment can continue.'
        fi
    else
        mr_note 'Warning: ifconfig is unavailable; the read-only adapter check was skipped. The network helper still validates the adapter before transfer.'
    fi
}

mr_java_ok() {
    [ -x "$1" ] || return 1
    local props major java_arch modules module
    props=$("$1" -XshowSettings:properties -version 2>&1) || return 1
    major=$(printf '%s\n' "$props" | sed -n 's/^[[:space:]]*java.specification.version = //p' | head -n 1)
    java_arch=$(printf '%s\n' "$props" | sed -n 's/^[[:space:]]*os.arch = //p' | head -n 1)
    case "$major" in ''|*[!0-9]*) return 1;; esac
    [ "$major" -ge 17 ] || return 1
    case "$MR_ARCH:$java_arch" in arm64:aarch64|arm64:arm64|x86_64:x86_64|x86_64:amd64) ;; *) return 1;; esac
    modules=$("$1" --list-modules 2>/dev/null) || return 1
    for module in java.base java.desktop java.net.http java.xml jdk.httpserver;do
        printf '%s\n' "$modules" | sed 's/@.*//' | grep -Fx "$module" >/dev/null || return 1
    done
    "$1" --add-modules java.desktop,java.net.http,java.xml,jdk.httpserver --validate-modules >/dev/null 2>&1 || return 1
}

mr_java_choose() {
    local discovered candidate
    MR_JAVA=''
    if [ -n "${JAVA_EXE:-}" ]; then
        if mr_java_ok "$JAVA_EXE";then MR_JAVA=$JAVA_EXE;return;fi
        mr_note 'Warning: JAVA_EXE is not a complete native Java 17+ runtime with java.desktop, java.net.http, java.xml and jdk.httpserver. Checking other runtimes; a verified private Temurin runtime will be installed if needed.'
    fi
    if [ -n "${JAVA_HOME:-}" ] && mr_java_ok "$JAVA_HOME/bin/java"; then MR_JAVA="$JAVA_HOME/bin/java"; return; fi
    if [ -f "$MR_USER_ROOT/java-path.config" ]; then
        IFS= read -r candidate < "$MR_USER_ROOT/java-path.config" || true
        if mr_java_ok "$candidate"; then MR_JAVA=$candidate; return; fi
    fi
    candidate="$MR_RUNTIME_ROOT/temurin-21-$MR_ARCH/Contents/Home/bin/java"
    if mr_java_ok "$candidate"; then MR_JAVA=$candidate; return; fi
    discovered=$( /usr/libexec/java_home -v '17+' -a "$MR_ARCH" 2>/dev/null || true )
    if [ -n "$discovered" ] && mr_java_ok "$discovered/bin/java"; then MR_JAVA="$discovered/bin/java"; return; fi
    candidate=$(command -v java || true)
    if [ -n "$candidate" ] && [ "$candidate" != /usr/bin/java ] && mr_java_ok "$candidate"; then MR_JAVA=$candidate; fi
}

mr_sha() {
    local checksum
    checksum=$(shasum -a 256 "$1") || return 1
    checksum=${checksum%% *}
    case "$checksum" in ''|*[!0-9a-f]*) return 1;; esac
    [ "${#checksum}" -eq 64 ] || return 1
    printf '%s\n' "$checksum"
}

mr_java_install() {
    local url expected actual archive unpack destination member candidate previous
    case "$MR_JRE_ARCH" in
        aarch64)
            url='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_aarch64_mac_hotspot_21.0.12.1_1.tar.gz'
            expected='dec50fc6f9fcd4fe3ae8cabf5a5fa68f6afc48841f7698e468e9aa5d54beed84';;
        x64)
            url='https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_x64_mac_hotspot_21.0.12.1_1.tar.gz'
            expected='6717ec641fd9ce0bb209ca083ee23b42202ac68cb6fcc5753496e0e4a0f41989';;
        *) mr_die 'Internal runtime architecture error.';;
    esac
    mr_note "Java 17+ was not found. Downloading official Eclipse Temurin 21 for $MR_ARCH into this application's private runtime folder."
    MR_TEMP=$(mktemp -d "$MR_RUNTIME_ROOT/.install-XXXXXXXX") || mr_die 'Cannot create a private runtime staging folder.'
    archive="$MR_TEMP/runtime.tar.gz"
    curl --fail --location --proto '=https' --proto-redir '=https' --connect-timeout 20 --max-time 900 --retry 2 --max-filesize 536870912 --output "$archive" "$url" || mr_die 'Java download failed. Check the Internet connection and launch Deploy-macOS.command again.'
    actual=$(mr_sha "$archive") || mr_die 'Cannot calculate the Java archive checksum.'
    [ "$actual" = "$expected" ] || mr_die 'Java archive SHA-256 mismatch. The archive will not be installed.'
    tar -tzf "$archive" > "$MR_TEMP/archive-members.txt" || mr_die 'The Java archive is unreadable.'
    [ -s "$MR_TEMP/archive-members.txt" ] || mr_die 'The Java archive is empty.'
    # Only the pinned archive's single, known top-level directory can be extracted.
    while IFS= read -r member; do
        case "$member" in "$MR_JRE_ROOT"|"$MR_JRE_ROOT/"|"$MR_JRE_ROOT/"*) ;;
            *) mr_die 'Unexpected path in the verified Java archive.';; esac
        case "/$member/" in */../*|*/./*) mr_die 'Unsafe path in the Java archive.';; esac
    done < "$MR_TEMP/archive-members.txt"
    unpack="$MR_TEMP/unpacked"
    mkdir "$unpack" || mr_die 'Cannot create the Java extraction folder.'
    tar -xzf "$archive" -C "$unpack" || mr_die 'Cannot unpack Java.'
    candidate="$unpack/$MR_JRE_ROOT/Contents/Home/bin/java"
    mr_java_ok "$candidate" || mr_die 'The downloaded Java runtime failed its native-architecture, version or required-module checks.'
    destination="$MR_RUNTIME_ROOT/temurin-21-$MR_ARCH"
    previous=''
    # Keep an older runtime as a rollback copy; do not recursively delete user files.
    if [ -e "$destination" ] || [ -L "$destination" ]; then
        [ ! -L "$destination" ] || mr_die 'The private runtime destination must not be a symbolic link.'
        previous="$destination.previous-$(date '+%Y%m%d%H%M%S')-$$"
        mv "$destination" "$previous" || mr_die 'Cannot preserve the previous Java runtime.'
    fi
    if ! mv "$unpack/$MR_JRE_ROOT" "$destination"; then
        if [ -n "$previous" ]; then mv "$previous" "$destination" || true; fi
        mr_die 'Cannot install the verified private Java runtime. The previous runtime was restored when possible.'
    fi
    MR_JAVA="$destination/Contents/Home/bin/java"
    rm -f "$archive" "$MR_TEMP/archive-members.txt"
    rmdir "$unpack" "$MR_TEMP" || true
    MR_TEMP=''
    mr_note 'Private Java installation and SHA-256 verification completed. System Java was not changed.'
}

mr_ensure_java() {
    mr_java_choose
    if [ -z "$MR_JAVA" ]; then mr_java_install; fi
    printf '%s\n' "$MR_JAVA" > "$MR_USER_ROOT/java-path.config.new" || mr_die 'Cannot save the selected Java runtime.'
    mv "$MR_USER_ROOT/java-path.config.new" "$MR_USER_ROOT/java-path.config" || mr_die 'Cannot commit the Java runtime selection.'
    mr_note "Java: $MR_JAVA"
}

mr_helper_build() {
    MR_HELPER="$MR_SOURCE/native/macos/gateway-net"
    if [ -x "$MR_HELPER" ] && mr_helper_arch_ok "$MR_HELPER" && "$MR_HELPER" --self-test; then return; fi
    if ! mr_clt_ready; then
        mr_note 'Apple Command Line Tools are required to build the gateway component for this Mac.'
        mr_note 'The Apple installer dialog will open. Choose Install and complete the Apple licence/administrator dialogs. Deployment will continue automatically.'
        if ! xcode-select --install; then
            if ! mr_clt_ready; then
                mr_note 'The Apple installer request could not be started. Install Command Line Tools using Apple, then reopen Deploy-macOS.command.'
                exit 2
            fi
        fi
        mr_clt_wait || exit 2
    fi
    mr_note 'Building the gateway component against the macOS system libpcap; no Homebrew or system route changes are needed.'
    [ -f "$MR_SOURCE/native/source/build_macos.sh" ] || mr_die 'The macOS gateway build script is missing.'
    ARCH="$MR_ARCH" /bin/bash "$MR_SOURCE/native/source/build_macos.sh" || mr_die 'Gateway component compilation failed. The detailed compiler output appears above.'
    [ -x "$MR_HELPER" ] && mr_helper_arch_ok "$MR_HELPER" && "$MR_HELPER" --self-test || mr_die 'The gateway component failed its native-architecture or offline self-test.'
}

mr_clt_ready() {
    local developer
    # xcrun itself can open Apple's installer when tools are absent. Check the
    # selected developer folder and an actual compiler first, without invoking it.
    developer=$(xcode-select -p 2>/dev/null) || return 1
    [ -d "$developer" ] || return 1
    if [ ! -x "$developer/usr/bin/clang" ] && [ ! -x "$developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/clang" ]; then return 1; fi
    xcrun --find clang >/dev/null 2>&1
}

mr_clt_pause() { sleep "$1"; }

mr_clt_wait() { mr_clt_wait_impl 1800 5; }

mr_clt_wait_impl() {
    local budget interval waited step
    budget=$1
    interval=$2
    waited=0
    mr_note 'Waiting up to 30 minutes for usable Apple Command Line Tools. Keep this Terminal open; press Ctrl+C here to cancel.'
    while [ "$waited" -lt "$budget" ]; do
        if mr_clt_ready; then
            mr_note 'Apple Command Line Tools are ready. Continuing deployment automatically.'
            return 0
        fi
        if [ "$((waited % 30))" = 0 ]; then
            mr_note "Waiting for Apple installation: $((waited / 60)) min $((waited % 60)) sec elapsed. Complete the Apple installer dialogs."
        fi
        step=$interval
        if [ "$((waited + step))" -gt "$budget" ]; then step=$((budget - waited)); fi
        if ! mr_clt_pause "$step"; then
            mr_note 'Waiting was cancelled. Finish installing Apple Command Line Tools when ready, then reopen Deploy-macOS.command to resume.'
            return 2
        fi
        waited=$((waited + step))
    done
    if mr_clt_ready; then
        mr_note 'Apple Command Line Tools are ready. Continuing deployment automatically.'
        return 0
    fi
    mr_note 'Apple Command Line Tools were not ready within 30 minutes. Finish or restart the Apple installation, then reopen Deploy-macOS.command to resume.'
    return 2
}

mr_helper_arch_ok() {
    local description
    description=$(file -b "$1") || return 1
    case "$description" in *Mach-O*) ;; *) return 1;; esac
    case "$MR_ARCH:$description" in arm64:*arm64*|x86_64:*x86_64*) return 0;; *) return 1;; esac
}

mr_helper_install() {
    local hash
    hash=$(mr_sha "$MR_HELPER") || mr_die 'Cannot calculate the built gateway component checksum.'
    [ "${#hash}" -eq 64 ] || mr_die 'Invalid gateway component checksum.'
    mr_note 'macOS administrator authorization is needed for the verified gateway component only.'
    mr_note 'The Java interface remains under your user account. The component opens its BPF device, then permanently drops administrator privileges.'
    sudo /bin/bash "$MR_SOURCE/macOS/install-helper.sh" "$MR_HELPER" "$hash" || mr_die 'The gateway component was not installed. Deployment can be resumed by launching this command again.'
    printf '%s\n' "$hash" > "$MR_USER_ROOT/gateway-helper.sha256" || mr_die 'Cannot save the gateway checksum.'
}

mr_copy_payload() {
    local destination entry
    destination=$1
    mkdir -p "$destination" || mr_die 'Cannot create application resources.'
    # Distribution allowlist: user downloads/data are never copied into a new app.
    for entry in MultiRoute-Downloader.jar lib native src macOS browser-extension docs README.md Start-macOS.command Deploy-macOS.command README-macOS.en.txt README-macOS.ru.txt API.ru.txt API.en.txt openapi.yaml; do
        if [ -e "$MR_SOURCE/$entry" ]; then cp -R "$MR_SOURCE/$entry" "$destination/" || mr_die "Cannot copy application component: $entry"; fi
    done
    chmod 755 "$destination/Start-macOS.command" "$destination/Deploy-macOS.command" "$destination/macOS/launch-app.sh" "$destination/macOS/install-helper.sh" || mr_die 'Cannot set launcher permissions.'
}

mr_app_install() {
    local staged resources previous
    staged=$(mktemp -d "$MR_APPLICATIONS/.MultiRoute-Resume-XXXXXXXX") || mr_die 'Cannot stage the desktop application.'
    previous=''
    MR_APP_TEMP=$staged
    resources="$staged/MultiRoute Downloader.app/Contents/Resources/app"
    mkdir -p "$staged/MultiRoute Downloader.app/Contents/MacOS" || mr_die 'Cannot create the macOS app bundle.'
    mr_copy_payload "$resources"
    cp "$MR_SOURCE/macOS/launch-app.sh" "$staged/MultiRoute Downloader.app/Contents/MacOS/MultiRoute" || mr_die 'Cannot install the Finder launcher.'
    chmod 755 "$staged/MultiRoute Downloader.app/Contents/MacOS/MultiRoute" || mr_die 'Cannot make the Finder launcher executable.'
    cat > "$staged/MultiRoute Downloader.app/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleName</key><string>MultiRoute Downloader</string>
<key>CFBundleDisplayName</key><string>MultiRoute Downloader</string>
<key>CFBundleIdentifier</key><string>local.multiroute.resume</string>
<key>CFBundleExecutable</key><string>MultiRoute</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleShortVersionString</key><string>3.4</string>
<key>CFBundleVersion</key><string>3.4</string>
<key>LSMinimumSystemVersion</key><string>11.0</string>
<key>NSHighResolutionCapable</key><true/>
<key>NSLocalNetworkUsageDescription</key><string>MultiRoute Downloader communicates with the gateways you select on your local network and uses an unused application virtual IP to download without changing system routes.</string>
</dict></plist>
PLIST
    plutil -lint "$staged/MultiRoute Downloader.app/Contents/Info.plist" >/dev/null || mr_die 'The Finder application manifest is invalid.'
    if [ -e "$MR_INSTALLED_APP" ] || [ -L "$MR_INSTALLED_APP" ]; then
        [ ! -L "$MR_INSTALLED_APP" ] || mr_die 'The installed app destination must not be a symbolic link.'
        previous="$MR_APPLICATIONS/MultiRoute Downloader.previous-$(date '+%Y%m%d%H%M%S')-$$.app"
        mv "$MR_INSTALLED_APP" "$previous" || mr_die 'Cannot preserve the previous desktop app.'
        mr_note "Previous app kept for rollback: $previous"
    fi
    if ! mv "$staged/MultiRoute Downloader.app" "$MR_INSTALLED_APP"; then
        if [ -n "$previous" ]; then mv "$previous" "$MR_INSTALLED_APP" || true; fi
        mr_die 'Cannot commit the desktop app installation. The previous app was restored when possible.'
    fi
    rmdir "$staged" || true
    MR_APP_TEMP=''
    mr_note "Installed: $MR_INSTALLED_APP"
}

mr_authorize() {
    local installed hash expected
    installed="$MR_HELPER_ROOT/gateway-net"
    mr_helper_protected "$installed" || mr_die 'The gateway component or its directory is not protected by root ownership and permissions. Launch Deploy-macOS.command again.'
    [ -x "$installed" ] || mr_die 'The protected gateway component is missing. Launch Deploy-macOS.command first.'
    [ -f "$MR_USER_ROOT/gateway-helper.sha256" ] || mr_die 'The installed gateway checksum is missing. Launch Deploy-macOS.command again.'
    IFS= read -r expected < "$MR_USER_ROOT/gateway-helper.sha256" || true
    case "$expected" in ''|*[!0-9a-f]*) mr_die 'The saved gateway component checksum is invalid. Launch Deploy-macOS.command again.';; esac
    [ "${#expected}" -eq 64 ] || mr_die 'The saved gateway component checksum is invalid. Launch Deploy-macOS.command again.'
    hash=$(mr_sha "$installed") || mr_die 'Cannot verify the installed gateway component.'
    [ "$hash" = "$expected" ] || mr_die 'The installed gateway component changed. Launch Deploy-macOS.command to verify and reinstall it.'
    mr_note 'Authorizing access to the selected gateways. Enter your macOS administrator password if Terminal requests it.'
    sudo -v || mr_die 'Administrator authorization was declined. The application was not started.'
    sudo -n -- "$installed" --self-test || mr_die 'The protected gateway component could not be authorized in this Terminal session.'
}

mr_helper_protected() {
    local component mode
    for component in /Library /Library/PrivilegedHelperTools /Library/PrivilegedHelperTools/MultiRouteResume "$MR_HELPER_ROOT"; do
        [ -d "$component" ] && [ ! -L "$component" ] || return 1
        [ "$(stat -f '%u' "$component")" = 0 ] || return 1
        mode=$(stat -f '%Lp' "$component") || return 1
        case "$mode" in ''|*[!0-7]*) return 1;; esac
        [ "$((8#$mode & 0022))" = 0 ] || return 1
    done
    [ -f "$1" ] && [ ! -L "$1" ] || return 1
    [ "$(stat -f '%u' "$1")" = 0 ] || return 1
    mode=$(stat -f '%Lp' "$1") || return 1
    case "$mode" in ''|*[!0-7]*) return 1;; esac
    [ "$((8#$mode & 0022))" = 0 ]
}

mr_launch() {
    mkdir -p "$MR_DATA" || mr_die 'Cannot create the download history/settings folder.'
    mr_authorize
    cd "$MR_SOURCE" || mr_die 'Cannot open the application folder.'
    mr_note 'Starting the user-level Java desktop application. Leave this Terminal session open while it runs.'
    "$MR_JAVA" -Xdock:name='MultiRoute Downloader' -jar "$MR_SOURCE/MultiRoute-Downloader.jar" --data-dir "$MR_DATA" "$@"
}

mr_lock() {
    MR_LOCK="$MR_USER_ROOT/deployment.lock"
    if ! mkdir "$MR_LOCK" 2>/dev/null; then
        mr_die "Another deployment is active, or an interrupted deployment left $MR_LOCK. If no installer is running, remove only that empty lock folder and launch again."
    fi
    MR_LOCK_OWNED=1
}

mr_cleanup() {
    # Only remove our empty lock. Incomplete staging/downloads remain inspectable.
    if [ "${MR_LOCK_OWNED:-0}" = 1 ]; then rmdir "$MR_LOCK" 2>/dev/null || true; fi
}
