MULTIROUTE RESUME 3.0 — APPLICATION ETHERNET TRANSPORT

This helper uses an application-owned lwIP IPv4 stack and packet capture.
It never edits OS routes, interface addresses or global capture permissions.
The virtual IPv4 must be unused and in the physical Ethernet subnet of all
selected routers. Wi-Fi and IPv6-only sources are not supported by this stack.

WINDOWS x64
Bundled native/windows-x64/gateway-net.exe is compiled from these sources.
Runtime requires an installed Npcap with capture access for this user.
No Npcap driver/DLL, UAC invocation or installer is bundled in the executable.
Build requires MSVC x64 C++ tools, Python and Windows/Npcap SDK headers.
  python fetch_sdk.py
  python build_native.py
Optional build-only roots: MULTIROUTE_BUILD_DEPS_ROOT, MULTIROUTE_MSVC_ROOT,
MULTIROUTE_NPCAP_SDK, MULTIROUTE_BUILD_OUTPUT_DIR.

macOS Intel / Apple Silicon
Run Deploy-macOS.command from the distribution. Apple Command Line Tools and
system libpcap build the helper with build_macos.sh. It is installed under
/Library/PrivilegedHelperTools/MultiRouteResume/3.0/gateway-net. The helper opens
BPF, permanently drops supplementary groups/GID/UID, then handles network/state
work as the invoking user. Java never runs as root. This release has not been
executed on actual macOS hardware or signed/notarized with an Apple account.

LINUX x86_64 / aarch64 (glibc)
Run Deploy-Linux.sh as the desktop user. It checks/installs Java and required
compiler/libpcap packages, builds with build_linux.sh and runs --self-test.
Protected helper: /usr/local/libexec/MultiRouteResume/3.0/gateway-net.
Only helper capture startup uses sudo, followed by permanent privilege drop.
The actual build_linux.sh was compiled and self-tested with isolated WSL
dependencies. No global packages were installed during release verification.

GATEWAYS AND CONTROL
Default addresses: 192.168.1.1, .2, .3, .4; virtual IP 192.168.1.70.
Configure 1..32 distinct gateways. Stable profile objects/listener ports let
profiles be added/removed without restarting the other active connections.
Private local control files use revision acknowledgements and fail-safe rollback.
Removed profiles remain inactive internal slots; 128 different profile slots
per helper lifetime bound storage. Restart can reclaim retired slots if exhausted.

DNS uses UDP bound to the virtual IP and selected gateway netif. UDP failure or
a matching truncated reply retries DNS over TCP through that same gateway and
virtual IP. Router DNS is tried first, then 1.1.1.1/8.8.8.8; no OS resolver is
used. TCP has a two-second limit per attempt and DNS an eight-second total limit.
ID/question/source/port/public IPv4/CNAME bounds are checked. Cache has 64
profile-specific entries, TTL <=300 seconds. DNSSEC/DoH/IPv6 and separate CNAME
continuation queries are not implemented; source TLS/SSH identity is still checked.

VERIFICATION
  gateway-net --self-test
This checks SOCKS, UDP fragments, hot profiles and bounded DNS parsing without
opening any network capture handles or changing routes. Windows and POSIX builds
passed. The standalone tests/gateway_dns_transport_test.c adds 169 checks for
UDP timeout/truncation, TCP framing/partial writes, profile binding, rejection,
cancellation and cleanup. An owned synthetic Ethernet/lwIP harness verified four DNS gateway MACs,
virtual IP/netif/UDP ports, invalid reply rejection, cache affinity, timeout and
cancellation. Physical router/Internet capture and actual macOS BPF/Apple SDK
were not tested in this cycle. See CHECKS.ru.md and TEST-REPORT.json at app root.

Primary references: https://savannah.nongnu.org/projects/lwip/
https://www.rfc-editor.org/rfc/rfc1035.html
https://www.rfc-editor.org/rfc/rfc5452.html
https://npcap.com/guide/
