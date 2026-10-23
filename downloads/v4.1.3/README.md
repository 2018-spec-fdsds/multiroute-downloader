# MultiRoute Downloader 4.1.3 installers

This full release improves Civitai/CDN range probing and reuse of saved sequential data. Temporary HTTP 200 replies for nonzero ranges retry after 2–30 seconds without discarding saved data or permanently removing the gateway worker. A persistent per-gateway transfer journal separates received traffic from saved progress. The default segment size remains 20 MiB.

Open a file and choose **Download raw file**, or use [Release 4.1.3](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.1.3).

| Platform | File |
| --- | --- |
| Windows 11 x64 | [Setup EXE](MultiRoute-Downloader-4.1.3-Windows-x64-Setup.exe) |
| Apple Silicon, macOS 26+ | [ARM64 PKG](MultiRoute-Downloader-4.1.3-macOS-arm64.pkg) |
| Intel, macOS 15+ | [Intel PKG](MultiRoute-Downloader-4.1.3-macOS-x86_64.pkg) |
| Chrome / Edge | [Extension ZIP](MultiRoute-Downloader-4.1.3-Chrome-Edge.zip) |

The native installers include private Java 21 and the platform network component. Windows Setup uses English and creates a desktop shortcut; Npcap is a separately installed prerequisite. macOS uses system libpcap. Exit the running application through its tray menu before updating, then install a complete matching package. Preserve your data directory and downloaded parts. Version 4.1.2 was source-only; its existing tag is unchanged.

**Русский:** выберите установщик своей системы и нажмите **Download raw file**. Полный пакет 4.1.3 содержит отдельную Java 21 и сетевой компонент. В Windows мастер на английском создаёт ярлык на рабочем столе; Npcap устанавливается отдельно. Перед обновлением выйдите через трей и сохраните папку данных с загруженными частями. Докачка и постоянный журнал передачи описаны в инструкции.

Instructions and API: [en](../../docs/en.md#multiroute-api) · [ru](../../docs/ru.md#multiroute-api) · [de](../../docs/de.md#multiroute-api) · [es](../../docs/es.md#multiroute-api) · [fr](../../docs/fr.md#multiroute-api) · [zh](../../docs/zh.md#multiroute-api) · [ko](../../docs/ko.md#multiroute-api) · [ar](../../docs/ar.md#multiroute-api) · [it](../../docs/it.md#multiroute-api) · [ja](../../docs/ja.md#multiroute-api) · [hi](../../docs/hi.md#multiroute-api) · [vi](../../docs/vi.md#multiroute-api).

[Release notes](../../docs/releases/4.1.3.en.md) · [Описание исправлений](../../docs/releases/4.1.3.ru.md) · [MD5](MD5SUMS.txt) · [SHA-256](SHA256SUMS.txt) · [SHA-512](SHA512SUMS.txt) · [Verification report](NATIVE-INSTALLER-TESTS.json).

The installed application completed a real 10,164,675,942-byte transfer with matching expected SHA-256. This is separate from validation of these release packages. Consult the matching report for actual completed checks, source commit and package hashes. Gateway `192.168.1.4` currently has no working WAN connection; no claim is made that all four configured gateways are healthy. Live macOS gateway operation remains unverified.

Installers are unsigned; macOS packages are not Developer ID signed or notarized. Follow the language guide for normal security confirmations.

Owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds). [Licence](../../LICENSE.txt).
