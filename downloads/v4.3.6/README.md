# MultiRoute Downloader 4.3.6 — files and verification

[Product page](../../README.md) · [Русский](README.ru.md) · [Release notes](../../docs/releases/4.3.6.en.md)

Choose the installer for your computer. Java 21 and the platform network component are included.

| Installer | Bytes | SHA-256 |
| --- | ---: | --- |
| [MultiRoute-Downloader-4.3.6-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.6/MultiRoute-Downloader-4.3.6-Windows-x64-Setup.exe) | 193080832 | `8401e63be1ac7f244881cfa2f8e0faac6b412801280393693b3fca24eb667df0` |
| [MultiRoute-Downloader-4.3.6-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.6/MultiRoute-Downloader-4.3.6-macOS-arm64.pkg) | 191614392 | `8e58bb393a73f85e1d53ebd26a4f1f738e01d505646a23e3d130560db6896d76` |
| [MultiRoute-Downloader-4.3.6-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.6/MultiRoute-Downloader-4.3.6-macOS-x86_64.pkg) | 185330849 | `43a00442b1f5a5c9b170e1b3e028470d76b27407fbad1fad31d5d26da4f05d0a` |

Windows 11 x64 Setup uses English and creates a desktop shortcut. Apple Silicon requires macOS 26 or later; Intel requires macOS 15 or later. macOS security confirmations are described in the [guide](../../docs/en.md).

[MD5](MD5SUMS.txt) · [SHA-256](SHA256SUMS.txt) · [SHA-512](SHA512SUMS.txt) · [Verification report](NATIVE-INSTALLER-TESTS.json)

The application has Free and optional signed Pro capabilities. Gateway priorities require their separate signed capability. [Capabilities and limits](../../docs/licensing.en.md).

Chrome / Edge integration is included in the installed application; open its folder from Settings. Preserve your application data and downloaded parts when updating. Wait for active transfers and checksum checks to finish before replacing program files.

Windows installer metadata and native application startup were checked. Both macOS packages passed their target Mac runner checks. These tests do not establish physical LAN/DHCP performance; the report records the validation limits.

The release contains compiled application installers and user documentation. The owner's license issuer and private signing keys are not distributed here.

Before running the macOS PKG updater, exit MultiRoute from its tray menu. The installer refuses to replace an in-use application and does not forcibly stop it. The protected Mac network component is version 4.3.1; first privileged Ethernet BPF access may still need normal administrator authorization.
