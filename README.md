# MultiRoute Resume

Java desktop download manager with resumable segments through selectable LAN
gateways and a localhost API. Current application version: **3.0**.

[English](docs/en.md) · [Русский](docs/ru.md) · [Deutsch](docs/de.md) ·
[Español](docs/es.md) · [Français](docs/fr.md)

## Download and install

Download and extract a complete ZIP from [Releases](https://github.com/2018-spec-fdsds/multiroute-resume/releases).
The source checkout requires a build; it does not contain the compiled application
JAR or the Windows gateway executable.

| Platform | First deployment | Subsequent launch |
| --- | --- | --- |
| Windows 11 x64 | `Deploy-Windows.cmd` | `Start-Windows.cmd` |
| Linux x86_64 / ARM64 | `bash ./Deploy-Linux.sh` | `bash ./Start-Linux.sh` |
| macOS Intel / Apple Silicon | `Deploy-macOS.command` | `~/Applications/MultiRoute Resume.app` or `Start-macOS.command` |

Deployment checks the environment and obtains missing software from official
sources. Java is installed privately for the application when needed. Npcap on
Windows and Apple Command Line Tools on macOS use their normal interactive
installers, including licence and administrator confirmations. See the individual
language guides for the exact security prompts and offline requirements.

## Computer and network requirements

- A full Java 17+ runtime for the native CPU architecture, including
  `java.desktop`, `java.net.http`, `jdk.httpserver` and `java.xml`.
- Wired Ethernet in the same IPv4 LAN as the selected Internet gateways.
- An unused virtual IPv4 address in that LAN. Defaults: virtual address
  `192.168.1.70`; four selected gateways `192.168.1.1`–`192.168.1.4`.
- Packet capture support: Npcap on Windows, libpcap on Linux and macOS. The
  protected Linux/macOS helper needs administrator authorisation before dropping
  privileges. Java runs as the normal user.
- At least 512 MiB free for Linux/macOS deployment or Windows private Java, plus downloaded files and
  torrent working space. A measured hard RAM minimum has not been established;
  4 GiB or more is a practical recommendation for multiple downloads.

**Bundled BitTorrent library compatibility:** Linux needs glibc **2.38+**;
macOS needs **26+ on Apple Silicon** or **15+ on Intel**. The macOS bootstrap
accepts 11+, but that is not the minimum OS for every bundled protocol. These
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
and macOS deployment scripts compile and install their protected helper on the
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

See [LICENSING.md](LICENSING.md), [third-party notices](THIRD-PARTY-NOTICES.txt)
and [dependency licences](lib/licenses/). The maintainer has not yet selected an
overall licence for the original application code.
