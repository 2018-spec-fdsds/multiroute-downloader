# Native Windows installer

The release artifact `MultiRoute-Resume-3.1-Windows-x64-Setup.exe` is one
installer file. It installs an ordinary `MultiRoute Resume.exe`, a private Java
runtime, a Start menu shortcut, an optional desktop shortcut and a Windows
uninstall entry. Shortcuts point directly to the EXE. Java does not need to be
installed separately, and normal launches do not open a CMD or PowerShell window.

The default installation folder is `%LOCALAPPDATA%\MultiRoute Resume App`.
The installer is for the current Windows user. Application data is kept separately
in `%LOCALAPPDATA%\MultiRoute Resume\data`; uninstalling the application does not
delete downloads or this profile. Only the official Npcap installer can request
administrator permission if the capture driver is missing. The application itself
continues to run as the normal user.
The installer displays the project's `LICENSE.txt`; third-party Java and native
library license notices remain included separately and retain their own terms.

## Build from the current saved sources

Requirements: Windows x64, Python 3.10 or newer, Internet access for the first build
and a saved application JAR containing `multiroute.DesktopLauncher`. No system Java
or WiX installation is required. The build downloads and verifies pinned official
portable Eclipse Temurin JDK/JRE and WiX 3.14.1 archives into `build/windows-tools`.
WiX 3.14.1 is the maintenance version containing the WiX 3 bundle security fixes.
The JDK and WiX are build tools; only the licensed full JRE is shipped to users.
Temurin hashes come from the official GitHub release asset digests. The WiX hash
was frozen from the official HTTPS archive; that historical GitHub asset does not
publish an asset digest. Every later download must match the pinned archive.

Fetch the private build tools and compile the current saved application sources:

```powershell
python packaging/windows/build.py --fetch-tools --tools-only
python scripts/build-java.py --platform windows --javac "build/windows-tools/jdk/jdk-21.0.12.1+1/bin/javac.exe"
```

Build the x64 native helper using `native/source/BUILD-README.txt` (the C++ compiler,
Windows SDK and Npcap SDK requirements are described there), then provide its path:

```powershell
python packaging/windows/build.py --fetch-tools --native-helper native/source/build/gateway-net.exe
```

The native application image is written below `build/windows-package/image`,
the single installer below `build/windows-package/installer`, and the payload
checksums into `build/windows-package/BUILD-REPORT.json`. Use `--build-dir` with a
new directory for another build; existing build directories are never deleted or
silently replaced. To reuse already verified tools, supply `--jpackage`, `--wix`
and `--runtime` explicitly. `--image-only` skips compiling the installer.

Verify the final installer without installing it:

```powershell
python packaging/windows/verify.py --build-dir build/windows-package
```

This extracts the embedded MSI and cabinets with read-only Windows Installer
APIs and `expand.exe`, compares the SHA-256 of every shipped application and Java
runtime file with the app image, and checks ordinary user privileges, native EXE
shortcut targets, installation scope and the Windows uninstall entry. A report
is saved as `VERIFICATION-REPORT.json`. No installer registration or driver
installation is performed.

The build packages only an explicit whitelist of application files, Windows
dependencies, native helper, bootstrap and documentation. User profiles, download
data, authentication, logs and build tools are excluded.

## Security and verification

Npcap is not bundled or silently installed. On first GUI launch, a missing driver
is downloaded from the official site, SHA-256 and the Authenticode publisher are
checked, and the standard interactive installer requests UAC approval. Cancelling
keeps existing data unchanged. There is no antivirus, firewall or global
PowerShell policy change.

The application and installer are not code signed. Windows may show a confirmation
for an unrecognized publisher; verify the GitHub release origin and published
SHA-256 before deciding to run it. Do not disable Windows security protections.

The build report proves payload contents and native packaging. Installing Npcap,
registering the installer on a real user's computer and transferring traffic
through physical routers require separate target-computer validation.

Build tooling references:

- [Oracle jpackage specification](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
- [WiX 3.14.1 official maintenance release](https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm)
- [Eclipse Temurin 21 official binaries](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1)
- [Npcap user guide](https://npcap.com/guide/npcap-users-guide.html)
