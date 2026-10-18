# MultiRoute Downloader

**New in 4.0:** saved site profiles with isolated accounts, verified FTP/FTPS/SFTP uploads and remote browsing, plus bounded ZIP/TAR/7Z/RAR previews. Uploads require remote read permission for full SHA-256 verification. One upload file uses one gateway; multiple files can run in parallel. [Full instructions](docs/en.md#saved-sites-uploads-and-archive-previews-40).

Java desktop download manager with resumable segments through selectable LAN
gateways and a localhost API. Native installer version: **4.0**; portable ZIP
distributions for **3.0** remain available.

[English](docs/en.md) · [Русский](docs/ru.md) · [Deutsch](docs/de.md) ·
[Español](docs/es.md) · [Français](docs/fr.md)

[中文](docs/zh.md) · [한국어](docs/ko.md) · [العربية](docs/ar.md) · [Italiano](docs/it.md) · [日本語](docs/ja.md) · [हिन्दी](docs/hi.md) · [Tiếng Việt](docs/vi.md)

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

**New in 4.0:** a refreshed FlatLaf desktop with System/Light/Dark themes, queue totals and keyboard shortcuts. Hover over any download for its full file name and absolute path; right-click to copy the path. Dated download history records start, continuation and completion events and remains available after restarting the application.

**Retained from 3.9:** startup no longer depends on deleting temporary command-output files after a process lookup times out. Output is collected through bounded pipes; identity checks for replacing an earlier MultiRoute instance remain in place.

**Retained from 3.8:** visible **Instructions** and **API guide** buttons at the upper right open the versioned guide in your selected language using the default browser. The API button goes directly to its API section.

**Retained from 3.7:** the selected download’s lower details and checksum report keep the reading position and text selection during refresh. Selecting a different download opens its report at the top.

**Retained from 3.6:** the separate log viewer keeps its reading position during refresh, follows new entries when scrolled to the bottom, and preserves text selection.

**Retained from 3.5:** a progress bar for every download, a scrollable list that keeps its position on refresh, and columns that fill the downloads panel width. Sorting, filtering and details remain available.

**Retained from 3.4:** MD5 and SHA-512 whole-file verification alongside SHA-256, automatic reference lookup, manual expected values and per-algorithm results in the UI and API. The transparent tray icon with black digits is retained.

**Download installers:** [Windows and macOS files in the downloads folder](https://github.com/2018-spec-fdsds/multiroute-downloader/tree/main/downloads/v4.0).

The default download folder is resolved for the current user at launch: `%USERPROFILE%\Downloads` on Windows and `~/Downloads` on macOS/Linux. A saved default follows the current profile; a folder you select manually is retained.

## Why use this application?

MultiRoute Downloader helps you download files using several independent Internet
connections at the same time. For example, one gateway can use a fixed broadband
line and another a mobile 4G/5G connection. Both routers are available to your
computer on the same local Ethernet network.

- **Use the combined bandwidth.** File segments are downloaded concurrently
  through all selected available gateways. Faster gateways take more work from
  the shared queue, increasing aggregate download speed when the Internet
  connections and source server have capacity.
- **Keep downloading when a connection fails.** For resumable segmented
  downloads, working gateways continue transferring data, unfinished segments
  return to the shared queue and other gateways can download them. Already
  verified data is preserved.
- **Download groups of files.** Import a list of URLs, manage multiple files in
  the download queue or submit a batch through the local API.

The speed gain depends on independent upstream capacity and source-server
limits; it is not a guaranteed multiple of the number of gateways.

## Screenshots

Version 4.0, light and dark appearances. These are actual desktop components rendered with an isolated own demo profile, Unicode filenames and synthetic job counters. The speeds are illustrative, not an Internet benchmark.

![MultiRoute Downloader 4.0 with the light theme](docs/images/desktop-4.0-light-en.png)

![MultiRoute Downloader 4.0 with the dark theme](docs/images/desktop-4.0-dark-en.png)

Version 3.5: progress bars for individual downloads and columns filling the panel. This own demo uses synthetic jobs and counters.

![Individual download progress and full-width columns](docs/images/progress-downloads-3.5-en.png)

The 3.4 checksum editor automatically recognizes MD5, SHA-256 and SHA-512. This own demo profile uses illustrative values to show a checksum error; no real file was downloaded.

![Checksum type detected automatically, with optional additional references](docs/images/checksum-editor-3.4.png)

Desktop interface in English, rendered from an isolated **demo profile**. The main-window examples were captured in 3.1; 3.2 introduced the tray and Exit command; 3.3 changes the speed icon to transparent with black text; 3.4 adds MD5 and SHA-512 verification. Jobs and counters are illustrative data: these are not actual downloads or measured Internet speeds.

Tray icon examples rendered by the actual 3.3 drawing code at native icon sizes, enlarged for inspection. Speeds are synthetic examples; the tooltip displays the exact aggregate rate.

![Tray speed icon examples from the 3.3 drawing code](docs/images/tray-speeds-3.3.png)

![Downloads: queue, progress and per-gateway statistics](docs/images/desktop-downloads-en.png)

<details>
<summary>Network settings and batch import</summary>

Four selected gateways and an application virtual address. The demo does not perform gateway or speed checks.

![Four selected gateways and an application virtual address. The demo does not perform gateway or speed checks.](docs/images/desktop-network-en.png)

Batch import: one URL per line, preview before submission.

![Batch import: one URL per line, preview before submission.](docs/images/desktop-bulk-import-en.png)

</details>

## Download and install

### Native Windows and macOS installers (4.0)

Choose a single installer file from [release 4.0](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.0).
The application, full Java runtime and compiled
network component are included. After installation, open the app from its normal
icon; a command window or Terminal session is not required.

| Platform | Installer | Installed application |
| --- | --- | --- |
| Windows 11 x64 | `MultiRoute-Downloader-4.0-Windows-x64-Setup.exe` | **MultiRoute Downloader** in Start or its desktop shortcut |
| Apple Silicon, macOS 26+ | `MultiRoute-Downloader-4.0-macOS-arm64.pkg` | `/Applications/MultiRoute Downloader.app` |
| Intel, macOS 15+ | `MultiRoute-Downloader-4.0-macOS-x86_64.pkg` | `/Applications/MultiRoute Downloader.app` |

Windows checks Npcap from the application. If missing, it offers a verified
download of the official installer and a normal interactive UAC installation;
Npcap is not bundled or silently installed. The macOS package needs no separately
installed Java, Command Line Tools or Homebrew. Its protected network component
requests administrator authorization through a standard macOS dialog while the
Java application remains under your normal user account.

These installers are not publisher/Developer ID signed or notarized. The twelve
language guides explain SmartScreen/Gatekeeper, administrator, Local Network and
folder-access confirmations without disabling system protections. macOS native
packages have the strict 26+/15+ minimum above; the older ZIP's HTTP-only minimum
does not apply to them.

### Portable ZIPs (3.0) and Linux

Download and extract a **complete** ZIP from Releases. These retain their script
deployment and launch steps below. The source checkout requires a build; it does
not contain the compiled application JAR or the Windows gateway executable.

| Platform | First deployment | Subsequent launch |
| --- | --- | --- |
| Windows 11 x64 | `Deploy-Windows.cmd` | `Start-Windows.cmd` |
| Linux x86_64 / ARM64 | `bash ./Deploy-Linux.sh` | `bash ./Start-Linux.sh` |
| macOS Intel / Apple Silicon | `Deploy-macOS.command` | `~/Applications/MultiRoute Resume.app` or `Start-macOS.command` |

Portable deployment checks the environment and obtains missing software from official
sources. Java is installed privately for the application when needed. Npcap on
Windows and Apple Command Line Tools on macOS use their normal interactive
installers, including licence and administrator confirmations. See the individual
language guides for the exact security prompts and offline requirements. The
portable macOS 3.0 app keeps its Terminal authorization session open; the new
native `.pkg` application uses graphical authorization instead.

## Computer and network requirements

- A full Java 17+ runtime for the native CPU architecture, including
  `java.desktop`, `java.net.http`, `jdk.httpserver` and `java.xml`. Native 4.0
  installers include a suitable runtime; portable/Linux deployment checks or
  downloads one when necessary.
- Wired Ethernet in the same IPv4 LAN as the selected Internet gateways.
- An unused virtual IPv4 address in that LAN. Defaults: virtual address
  `192.168.1.70`; four selected gateways `192.168.1.1`–`192.168.1.4`.
- Packet capture support: Npcap on Windows, libpcap on Linux and macOS. The
  protected Linux/macOS helper needs administrator authorisation before dropping
  privileges. Java runs as the normal user.
- At least 512 MiB free for portable Linux/macOS deployment or Windows private Java, plus downloaded files and
  torrent working space. A measured hard RAM minimum has not been established;
  4 GiB or more is a practical recommendation for multiple downloads.

**Bundled BitTorrent library compatibility:** Linux needs glibc **2.38+**;
macOS needs **26+ on Apple Silicon** or **15+ on Intel**. The portable 3.0 macOS bootstrap
accepts 11+, but that is not the minimum OS for every bundled protocol or the
new native installer. These
requirements were checked from the actual ELF/Mach-O binary metadata. See
[the compatibility audit](docs/COMPATIBILITY.json).

## Features

- 1–32 gateway profiles, editable during transfers, with a shared segment queue.
- 20 MiB segments by default. A stalled segment is returned to the pool after
  60 seconds without new data, after the previous writer closes.
- Total and per-gateway speeds, elapsed time, estimated time remaining, priority,
  queue limits, scheduling, bulk URL import and speed graphs.
- HTTP/HTTPS, passive FTP, explicit FTPS, SFTP with a pinned host key,
  secure WebDAV, Metalink, Hugging Face, torrent and magnet.
- MD5, SHA-256 and SHA-512 whole-file sums, published references when available,
  verification of saved segments and the
  assembled file. A locally calculated hash alone is not a publisher verification.
- Source authentication, trusted mirrors and a versioned local JSON API with
  batch creation and server-sent events.
- English, Russian, German, Spanish, French, Chinese, Korean, Arabic, Italian, Japanese, Hindi and Vietnamese; English by default.

The gateway transport uses an application-owned lwIP stack. Its virtual address
and gateway selection leave the OS routing table unchanged. The local API is
`http://127.0.0.1:8765/api/v1`; the virtual address is not the API listen address.

## Build from source

Install **Python 3** and a **JDK 17+**, then run:

```sh
python scripts/build-java.py
```

Build the platform gateway component separately using
[native/source/BUILD-README.txt](native/source/BUILD-README.txt).
The Windows distribution needs `native/windows-x64/gateway-net.exe`; the Linux
and portable macOS deployment scripts compile and install their protected helper on the
target computer. SDKs and Npcap are obtained separately from their official sources.

For Windows, run the SDK fetch and native build from `native/source/`, then copy
the resulting `build/gateway-net.exe` to `native/windows-x64/gateway-net.exe`
under the repository root. `Deploy-Windows.cmd` requires this executable and the
built Java JAR before it downloads prerequisites.

## API, verification and limitations

- [API reference and examples](API.ru.txt); [OpenAPI schema](openapi.yaml).
- [Native installer checks for 4.0](https://github.com/2018-spec-fdsds/multiroute-downloader/blob/main/NATIVE-INSTALLER-TESTS.json), including
  package hashes, Windows native startup and both macOS architectures.
- Earlier 3.0 baseline: [application checks](CHECKS.ru.md),
  [application report](TEST-REPORT.json) and
  [deployment and documentation checks](DEPLOYMENT-TEST-REPORT.json).
- Both macOS packages were built and their native launcher/API, JNI and protected
  helper fixtures were checked on real GitHub Mac runners. Windows native
  startup, instance replacement and profile retention were checked separately.
  End-user installation/security dialogs, BPF capture and physical gateway WAN
  transfers remain unverified. Version 4.0 is a prerelease. The packages are
  unsigned with a publisher identity; macOS packages are not notarised.
- Installer payloads contain the instructions as saved at the package source
  commit recorded in the 4.0 report. Current verification results are published
  in that report and the release notes.
- The native gateway transport supports public IPv4. Wi-Fi-only/VPN-only
  adapters, IPv6-only sources and local LAN torrent peers are outside this mode.
- HTTP without Range uses one selected gateway and restarts from zero on a
  broken connection. The speed test's 16 MiB / 5 second limits apply to the test.
- DNS is gateway-bound UDP; DNSSEC, DoH, TCP truncation fallback and separate
  CNAME continuation queries are not implemented.

## Licences

You may install and use the original application free of charge for personal use and within organizations, including business activities. **Selling the application, charging for its licence or distributing it for payment is prohibited** without separate permission. The custom licence does not grant general modification or redistribution rights and is not an OSI-approved open-source licence. Third-party components retain their own licences. This is a summary; the controlling English terms are in [LICENSE.txt](LICENSE.txt).

See [LICENSING.md](LICENSING.md), [third-party notices](THIRD-PARTY-NOTICES.txt) and [dependency licences](lib/licenses/).

## Download scheduling and priority

Use the queue/schedule controls to set `max_active_jobs` (1–32), global or gateway speed limits and a daily download window (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm). Times use the computer’s local timezone, also reported as `schedule_timezone`; a window may cross midnight, and equal start/end means all day. Outside the enabled window, downloads return to the queue and continue when allowed. A manually paused job remains paused. In a selected download’s priority/schedule settings, `priority` ranges from -100 to 100: higher queued jobs start first; equal priorities use creation order. Changing priority does not interrupt an already active download. `start_after_ms` is an optional earliest Unix-epoch start time in milliseconds; 0 removes the one-time delay. These settings persist across restarts. They govern the download queue, not the independent upload queue. Keep the application running (the tray is sufficient) and the computer awake; this is not an OS wake-up scheduler. The existing `GET`/`PUT`/`POST /api/v1/queue` and `PUT`/`POST /api/v1/jobs/{id}/options` expose the same controls.

### Uploads in the desktop app

![Uploads with a saved site profile](docs/images/uploads-4.0-en.png)

The image uses owned synthetic Unicode demo tasks. Its counters are not a throughput benchmark.

<a name="browser-integration"></a>

## Browser integration

Open the desktop **Chrome / Edge** integration dialog, enable link delivery and open its extension folder. Once in Chrome, open `chrome://extensions`; in Edge open `edge://extensions`. Turn on Developer mode, choose **Load unpacked** and select the `browser-extension` folder containing `manifest.json`. The native installers include it beside the application JAR; a source checkout also provides [browser-extension/](browser-extension/). Keep this folder in place. If using the separate `MultiRoute-Downloader-4.0-Chrome-Edge.zip`, extract it first and select its folder. This release is loaded locally, not published in a browser store.

Keep MultiRoute running, including in the tray. Right-click a link and choose the MultiRoute download action, or use the extension popup to send a URL. The desktop uses the currently selected folder, gateways and site profile; repository/torrent choices still appear when needed. The popup port defaults to 8765; set it to the application’s actual local API port if changed. Only the URL is sent: browser cookies/passwords are not copied and ordinary browser downloads are not intercepted. Configure required source access in the application. If the browser requests permission for the local connection, approve it only for this chosen extension.

The bridge is off by default and accepts only this extension’s fixed origin `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` at `/api/v1/browser/status` and `/api/v1/browser/download`; other local API browser-origin rules remain unchanged. HTTP 202 means the URL reached the desktop flow, not that a file completed. A headless server alone cannot open the required desktop selection flow. Remove the extension on the browser’s extensions page or disable its desktop integration to stop link delivery.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
