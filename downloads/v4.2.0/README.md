# MultiRoute Downloader 4.2.0 installers

Choose the installer for your platform from [release 4.2.0](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.2.0). Match its version and checksums; see the matching `NATIVE-INSTALLER-TESTS.json` for platform and asset verification results. Source archives require a build and are not native installers.

**Русский:** выберите установщик своей платформы в релизе 4.2.0 и сверьте версию и контрольные суммы. Результаты проверки платформ и файлов приведены в соответствующем `NATIVE-INSTALLER-TESTS.json`. Архив исходного кода требует сборки и не является установщиком.

| Platform | File |
| --- | --- |
| Windows 11 x64 | [Windows Setup EXE](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MultiRoute-Downloader-4.2.0-Windows-x64-Setup.exe) |
| Apple Silicon, macOS 26+ | [ARM64 PKG](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MultiRoute-Downloader-4.2.0-macOS-arm64.pkg) |
| Intel, macOS 15+ | [Intel PKG](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MultiRoute-Downloader-4.2.0-macOS-x86_64.pkg) |
| Chrome / Edge | [Extension ZIP](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MultiRoute-Downloader-4.2.0-Chrome-Edge.zip) |

Verification files: [MD5](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/MD5SUMS.txt) · [SHA-256](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/SHA256SUMS.txt) · [SHA-512](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/SHA512SUMS.txt) · [NATIVE-INSTALLER-TESTS.json](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.2.0/NATIVE-INSTALLER-TESTS.json). Match package version, source commit and hashes to the corresponding report; it records the actual platform and asset results.

4.2.0 adds application DNS settings: strict Cloudflare DoH by default, DoT, an explicit System/OS/VPN mode, fixed provider presets and custom authenticated servers. Test an unsaved draft through selected gateways before applying it. Secure modes have no silent plaintext/system fallback. OS DNS, routes and interface addresses are unchanged. Read the release notes for current provider interoperability limits; one successful query does not prove WAN access through every configured router.

Native installers include private Java 21 and the platform network component. Windows Setup uses English and creates a desktop shortcut; Npcap is separately required. macOS uses system libpcap. Exit through the tray before an update and preserve the data directory and downloaded parts.

Installers are unsigned; macOS packages are not Developer ID signed or notarized. Follow the normal security confirmations in your language guide; the native macOS minimum is 26+ on Apple Silicon and 15+ on Intel.

Instructions and API: [en](../../docs/en.md#dns-settings) · [ru](../../docs/ru.md#dns-settings) · [de](../../docs/de.md#dns-settings) · [es](../../docs/es.md#dns-settings) · [fr](../../docs/fr.md#dns-settings) · [zh](../../docs/zh.md#dns-settings) · [ko](../../docs/ko.md#dns-settings) · [ar](../../docs/ar.md#dns-settings) · [it](../../docs/it.md#dns-settings) · [ja](../../docs/ja.md#dns-settings) · [hi](../../docs/hi.md#dns-settings) · [vi](../../docs/vi.md#dns-settings).

[Release notes](../../docs/releases/4.2.0.en.md) · [Описание релиза](../../docs/releases/4.2.0.ru.md) · [API](../../API.ru.txt) · [OpenAPI](../../openapi.yaml).

Owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds). [Licence](../../LICENSE.txt). Free use; sale of the application or its licence requires separate permission. Third-party licences remain applicable.
