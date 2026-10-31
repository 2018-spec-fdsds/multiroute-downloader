# MultiRoute Downloader 4.3.7 — files and verification

[Product page](../../README.md) · [Русский](README.ru.md) · [Release notes](../../docs/releases/4.3.7.en.md)

Choose the installer for your computer. Java 21 and the platform network component are included.

| Installer | Bytes | SHA-256 |
| --- | ---: | --- |
| [MultiRoute-Downloader-4.3.7-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.7/MultiRoute-Downloader-4.3.7-Windows-x64-Setup.exe) | 196078080 | `80ee5e79b0654968978f85c3d05db22b6c99b824bf4426c2e4d91418ae9dbbb4` |
| [MultiRoute-Downloader-4.3.7-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.7/MultiRoute-Downloader-4.3.7-macOS-arm64.pkg) | 195579840 | `d05bd6df44ea8bb723273ed02267f8a3f31f721b39765ff2e2ed19f259b50896` |
| [MultiRoute-Downloader-4.3.7-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.7/MultiRoute-Downloader-4.3.7-macOS-x86_64.pkg) | 189293898 | `4dfce98a2e399984874d2e601d982dda44fa25ca6fe17e0626b2870ddbff7438` |

Windows 11 x64 Setup uses English and creates a desktop shortcut. Apple Silicon requires macOS 26 or later; Intel requires macOS 15 or later. macOS security confirmations are described in the [guide](../../docs/en.md).

[MD5](MD5SUMS.txt) · [SHA-256](SHA256SUMS.txt) · [SHA-512](SHA512SUMS.txt) · [Verification report](NATIVE-INSTALLER-TESTS.json)

The application has Free and optional signed Pro capabilities. Gateway priorities require their separate signed capability. [Capabilities and limits](../../docs/licensing.en.md).

Chrome / Edge integration is included in the installed application; open its folder from Settings. Preserve your application data and downloaded parts when updating. Wait for active transfers and checksum checks to finish before replacing program files.

Windows installer metadata and native application startup were checked. Both macOS packages passed their target Mac runner checks. These tests do not establish physical LAN/DHCP performance; the report records the validation limits.

The release contains compiled application installers and user documentation. The owner's license issuer and private signing keys are not distributed here.

Before running the macOS PKG updater, exit MultiRoute from its tray menu. The installer refuses to replace an in-use application and does not forcibly stop it. The protected Mac network component is version 4.3.2; first privileged Ethernet BPF access may still need normal administrator authorization.

4.3.7 corrects the POSIX capture-mode difference between manual application addressing and DHCP, and preserves the existing twelve-language DHCP diagnostics. API and interface controls remain unchanged. Physical Mac transfers with a manual address remain outside the automated hardware-validation scope.
