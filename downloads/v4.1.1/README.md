# MultiRoute Downloader 4.1.1 installers

Gateway-bound DNS now retries over TCP after UDP timeouts or truncated replies, using the same gateway and virtual IP. Automatic refresh preserves scrolling in the network, download, upload and log views. OS routes are unchanged; the default segment size remains 20 MiB.

Open a file and choose **Download raw file**, or use [Release 4.1.1](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.1.1).

| Platform | File |
| --- | --- |
| Windows 11 x64 | [Setup EXE](MultiRoute-Downloader-4.1.1-Windows-x64-Setup.exe) |
| Apple Silicon, macOS 26+ | [ARM64 PKG](MultiRoute-Downloader-4.1.1-macOS-arm64.pkg) |
| Intel, macOS 15+ | [Intel PKG](MultiRoute-Downloader-4.1.1-macOS-x86_64.pkg) |
| Chrome / Edge | [Extension ZIP](MultiRoute-Downloader-4.1.1-Chrome-Edge.zip) |

Java is included in the native installers. Windows Setup uses English and creates a desktop shortcut. Exit the running application through its tray menu before updating. Install a complete matching package rather than replacing individual files. The previously published 4.1 packages predate these fixes.

**Русский:** выберите установщик для своей системы и нажмите **Download raw file**. Полный пакет 4.1.1 содержит Java и сетевой компонент; перед обновлением выйдите из приложения через трей. Существующие задания и проверенные части сохраняются. Пакеты 4.1 были выпущены до этих исправлений.

Instructions and API: [en](../../docs/en.md#multiroute-api) · [ru](../../docs/ru.md#multiroute-api) · [de](../../docs/de.md#multiroute-api) · [es](../../docs/es.md#multiroute-api) · [fr](../../docs/fr.md#multiroute-api) · [zh](../../docs/zh.md#multiroute-api) · [ko](../../docs/ko.md#multiroute-api) · [ar](../../docs/ar.md#multiroute-api) · [it](../../docs/it.md#multiroute-api) · [ja](../../docs/ja.md#multiroute-api) · [hi](../../docs/hi.md#multiroute-api) · [vi](../../docs/vi.md#multiroute-api).

[Release notes](../../docs/releases/4.1.1.en.md) · [Описание исправлений](../../docs/releases/4.1.1.ru.md) · [MD5](MD5SUMS.txt) · [SHA-256](SHA256SUMS.txt) · [SHA-512](SHA512SUMS.txt) · [Verification report](NATIVE-INSTALLER-TESTS.json).

A Windows check completed 100 MiB through all four gateways in about 32 seconds. It does not establish universal speed or failover behavior, and live macOS gateway operation remains unverified. Check the report for exact package hashes and platform build checks. Installers are unsigned; macOS packages are not notarized.

Owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds). [Licence](../../LICENSE.txt).
