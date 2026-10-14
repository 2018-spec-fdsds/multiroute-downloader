# MultiRoute Resume

Java desktop download manager with resumable segments through selectable LAN
gateways and a localhost API. Native installer version: **3.1**; portable ZIP
distributions for **3.0** remain available.

[English](docs/en.md) · [Русский](docs/ru.md) · [Deutsch](docs/de.md) ·
[Español](docs/es.md) · [Français](docs/fr.md)

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Why use this application?

MultiRoute Resume helps you download files using several independent Internet
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

Real desktop interface in English, rendered from an isolated **demo profile**. Jobs and counters are illustrative data: these are not actual downloads or measured Internet speeds.

![Downloads: queue, progress and per-gateway statistics](docs/images/desktop-downloads-en.png)

<details>
<summary>Network settings and batch import</summary>

Four selected gateways and an application virtual address. The demo does not perform gateway or speed checks.

![Four selected gateways and an application virtual address. The demo does not perform gateway or speed checks.](docs/images/desktop-network-en.png)

Batch import: one URL per line, preview before submission.

![Batch import: one URL per line, preview before submission.](docs/images/desktop-bulk-import-en.png)

</details>

## Download and install

### Native Windows and macOS installers (3.1)

Choose a single installer file from [Releases](https://github.com/2018-spec-fdsds/multiroute-resume/releases),
when it is attached to the release. The application, full Java runtime and compiled
network component are included. After installation, open the app from its normal
icon; a command window or Terminal session is not required.

| Platform | Installer | Installed application |
| --- | --- | --- |
| Windows 11 x64 | `MultiRoute-Resume-3.1-Windows-x64-Setup.exe` | **MultiRoute Resume** in Start or its desktop shortcut |
| Apple Silicon, macOS 26+ | `MultiRoute-Resume-3.1-macOS-arm64.pkg` | `/Applications/MultiRoute Resume.app` |
| Intel, macOS 15+ | `MultiRoute-Resume-3.1-macOS-x86_64.pkg` | `/Applications/MultiRoute Resume.app` |

Windows checks Npcap from the application. If missing, it offers a verified
download of the official installer and a normal interactive UAC installation;
Npcap is not bundled or silently installed. The macOS package needs no separately
installed Java, Command Line Tools or Homebrew. Its protected network component
requests administrator authorization through a standard macOS dialog while the
Java application remains under your normal user account.

These installers are not publisher/Developer ID signed or notarized. The five
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
  `java.desktop`, `java.net.http`, `jdk.httpserver` and `java.xml`. Native 3.1
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
- Published checksums when available, verification of saved segments and the
  assembled file. A locally calculated hash alone is not a publisher verification.
- Source authentication, trusted mirrors and a versioned local JSON API with
  batch creation and server-sent events.
- English, Russian, German, Spanish and French; English by default.

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
- [Application checks](CHECKS.ru.md); [application report](TEST-REPORT.json);
  [deployment and documentation checks](DEPLOYMENT-TEST-REPORT.json).
- Physical gateways and an actual Mac/Apple SDK/BPF have not been validated in
  this release cycle. The macOS app is not Developer ID signed or notarised.
- The native gateway transport supports public IPv4. Wi-Fi-only/VPN-only
  adapters, IPv6-only sources and local LAN torrent peers are outside this mode.
- HTTP without Range uses one selected gateway and restarts from zero on a
  broken connection. The speed test's 16 MiB / 5 second limits apply to the test.
- DNS is gateway-bound UDP; DNSSEC, DoH, TCP truncation fallback and separate
  CNAME continuation queries are not implemented.

## Licences

You may install and use the original application free of charge for personal use and within organizations, including business activities. **Selling the application, charging for its licence or distributing it for payment is prohibited** without separate permission. The custom licence does not grant general modification or redistribution rights and is not an OSI-approved open-source licence. Third-party components retain their own licences. This is a summary; the controlling English terms are in [LICENSE.txt](LICENSE.txt).

See [LICENSING.md](LICENSING.md), [third-party notices](THIRD-PARTY-NOTICES.txt) and [dependency licences](lib/licenses/).
