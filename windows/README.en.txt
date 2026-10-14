MULTIROUTE RESUME — WINDOWS ENVIRONMENT SETUP

First use: extract the complete runnable Windows distribution to a writable
folder with Windows ACL support (a local NTFS user folder is suitable), then
double-click Deploy-Windows.cmd. Later, use Start-Windows.cmd. Both launchers
check the environment and install missing prerequisites before starting the app.
The source-only GitHub checkout has no application JAR/native executable;
build them or download the runnable ZIP from GitHub Releases first.

Windows 11 x64 and Windows PowerShell 5.1 are required. Existing full native
Java 17+ is checked for java.desktop, java.net.http, jdk.httpserver and java.xml.
JAVA_EXE explicitly selects Java; an invalid override is reported, not ignored.
Without that override, runtime/bin/java.exe, JAVA_HOME and PATH are checked.
If Java is missing, a pinned official Eclipse Temurin 21 Windows x64 JRE ZIP
is downloaded over HTTPS, SHA-256 checked, safely extracted and installed in
this application's runtime folder. At least 512 MiB must be free for this
deployment; downloaded files need additional space. An existing private runtime
is preserved as runtime.previous-*. System Java is not replaced.

Npcap is checked through its driver service and Windows System32/Npcap x64
DLLs without opening a capture device. If it is missing, the official Npcap
1.89 installer is downloaded. Its pinned SHA-256 and valid Authenticode signer
Nmap Software LLC are verified before its visible installer receives UAC.
Read and accept the official license; free-edition limits still apply. The
installer is never bundled or run silently. Declining UAC/license stops setup.
If a restart is required, restart when convenient and reopen the launcher.
An installed but stopped service produces guidance rather than a silent change
to system services. Admin-only Npcap can require its own UAC capture prompt
when checking gateways. The Java application starts as your normal user;
do not launch the whole application with Run as administrator.

Existing compatible Java and Npcap are reused without network downloads.
Use Internet for missing software and actual remote downloads. Download
failures leave the previous runtime intact and do not weaken verification.
The launcher uses process-local PowerShell execution policy; no permanent
execution-policy, antivirus, firewall, system address or route changes are made.

CLI arguments are forwarded, for example:
    Start-Windows.cmd --headless --port 8766
    Start-Windows.cmd --data-dir "C:\Users\your-user\MultiRoute data"

The GUI/API and network requirements remain in docs/en.md. The selected
routers must be reachable through wired Ethernet and a free virtual LAN IP.

Official references:
https://adoptium.net/installation/
https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1
https://npcap.com/#download
https://npcap.com/guide/npcap-users-guide.html
