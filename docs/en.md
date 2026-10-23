# MultiRoute Downloader 4.1.3 — English guide

**English** · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [Home](../README.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

**4.1.3 fixes:** CDNs such as Civitai that return HTTP 200 for a range starting at zero are checked with a nonzero range. The first segment stays bounded; compatible saved sequential data can resume as segments. A temporary HTTP 200 for a nonzero range retries after 2–30 seconds while preserving saved data and keeping the gateway worker available. A persistent per-gateway transfer journal distinguishes received traffic from saved progress. [Release notes and validation limits](releases/4.1.3.en.md) · [Transfer diagnostics](transfer-diagnostics.en.md).

[4.1.3 installers](https://github.com/2018-spec-fdsds/multiroute-downloader/tree/main/downloads/v4.1.3) include a private Java 21 runtime. Windows Setup uses English and creates a desktop shortcut; Npcap remains separately required. macOS uses system libpcap. Follow the installation and security confirmations below.

MultiRoute Downloader is a Java desktop application that downloads files through one or more **gateways** on your local network. It assigns available parts to those gateways, preserves verified data and assembles the completed file. A local API lets other applications submit and manage downloads. The desktop window is not a web interface.

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Why use it?

If your network has several independent Internet connections, such as a fixed broadband line and a mobile 4G/5G router, the application can use their bandwidth simultaneously. Your computer reaches the gateways on the same local Ethernet network.

- **Higher aggregate speed:** all selected available gateways download file segments concurrently from a shared queue. Faster gateways can take more segments.
- **Resilience:** during resumable segmented downloads, a failed gateway leaves the remaining gateways working; unfinished segments return to the queue for another gateway, and verified data is retained.
- **Batch downloads:** import a list of URLs, manage several files in the queue or submit a group of downloads through the local API.

The speed gain depends on independent upstream capacity and source-server limits. Resuming and reassigning segments require a source and protocol that support safe resumption.

**Portable ZIP 3.0 only:** download the package for your platform from [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-downloader/releases) and extract **the entire package**. Keep the JAR, libraries, launchers and native files together. GitHub's “Source code” archive contains source files: build it first or use a ready-to-run release. The deployment scripts report missing application binaries before downloading prerequisites.

## Screenshots

Real desktop interface in English, rendered from an isolated **demo profile**. Jobs and counters are illustrative data: these are not actual downloads or measured Internet speeds.

![Downloads: queue, progress and per-gateway statistics](images/desktop-downloads-en.png)

<details>
<summary>Network settings and batch import</summary>

Four selected gateways and an application virtual address. The demo does not perform gateway or speed checks.

![Four selected gateways and an application virtual address. The demo does not perform gateway or speed checks.](images/desktop-network-en.png)

Batch import: one URL per line, preview before submission.

![Batch import: one URL per line, preview before submission.](images/desktop-bulk-import-en.png)

</details>

## 1. Required environment

The native Windows/macOS installers described below include Java and the compiled network component. The Java selection, private-runtime downloads and local-compilation requirements in this environment section describe the **portable 3.0 ZIPs and Linux deployment**. Npcap remains separately required on Windows. New native macOS packages have the stricter 26+/15+ minimum shown below.

| Platform | Operating system and CPU | Java and other components |
| --- | --- | --- |
| Windows | Windows 11, native 64-bit x86-64 | Full 64-bit Java 17+ runtime; Npcap capture driver. The supplied gateway component is Windows x64. Windows ARM64 is not included. |
| Linux | glibc distribution, x86-64 or ARM64/aarch64; **glibc 2.38+ for torrent/magnet** | Native Java 17+; `sudo`; C compiler, binutils, pkg-config and libpcap development files. Desktop: X11, or Wayland with XWayland, plus X11/font libraries. Alpine/musl is not included. |
| macOS | Deployment supports macOS 11+, Intel x86-64 or Apple Silicon ARM64. **Torrent/magnet: macOS 15+ on Intel, macOS 26+ on Apple Silicon**, as required by the bundled JNI library. | Native Java 17+; Apple Command Line Tools; system libpcap; BPF access through the component authorized with `sudo`. Homebrew and the full Xcode application are not required. |

Use a **complete JRE or JDK**, including `java.desktop`, `java.net.http`, `jdk.httpserver` and `java.xml`. `--headless` runs without a window; it still needs those modules. A stripped Java image may not work. The launchers check the runtime architecture and modules before using it.

The bundled torrent library has stricter OS requirements than Java or deployment: its files were inspected without loading them, and require the glibc/macOS versions above. The HTTP transport and locally compiled gateway component have separate requirements; compatibility with every older release or distribution has not been certified. Deployment warns about incompatible torrent libraries; it does **not** upgrade your OS or replace glibc. Install a supported operating system through its normal upgrade procedure before using torrent on an incompatible machine. The bundled Linux JNI files do not dynamically require `libstdc++.so.6` or `GLIBCXX`/`CXXABI` versions; your distribution's other components may have their own dependencies.

Common requirements:

- An active **wired Ethernet** adapter on the same IPv4 subnet as every selected gateway. Each router must provide working Internet access for the application's virtual address. To add bandwidth, the routers need independent usable WAN capacity; a shared upstream link or a server limit can prevent a speed increase.
- A free IPv4 address on that subnet for the application. Do not use your computer's address, another device's address, or a network/broadcast address.
- Program and download folders writable by your user.
- Linux/macOS deployment and Windows private-Java installation check for at least **512 MiB free**, in addition to download space. Apple developer tools may need substantially more space than this deployment check.
- Additional torrent space for pieces and temporary storage. Reserve several times the selected content size: the conservative check includes full needed pieces and file boundaries, the destination and temporary data. It is not an exact universal multiple of selected file sizes.
- **Recommendation:** at least 4 GB RAM for comfortable use; more for many routers or simultaneous torrents. This is guidance, not a measured or enforced RAM minimum. Downloading does not require a GPU, CUDA or AI models.

Version 3.0's gateway transport uses IPv4 over Ethernet. Wi-Fi-only, VPN-only and IPv6-only sources are **not supported configurations** for it.

The selected language also needs installed fonts covering its script. On minimal Linux, install suitable fonts through the distribution, for example Noto CJK for Chinese/Korean and an Arabic Noto family. Java uses platform font fallback; this application does not install or bundle font packs. Coverage can differ between computers.

## Native installers 4.1.3: Windows and macOS

Choose the installer for your system from [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-downloader/releases), when it is attached to the release. Each is **one installer file**, containing the application, native launcher, full Java runtime and platform network component. The source checkout and portable 3.0 ZIPs are separate distributions.

| System | Installer | Launch after installation |
| --- | --- | --- |
| Windows 11 x64 | `MultiRoute-Downloader-4.1.3-Windows-x64-Setup.exe` | **MultiRoute Downloader** in the Start menu or its desktop shortcut |
| Apple Silicon, macOS 26+ | `MultiRoute-Downloader-4.1.3-macOS-arm64.pkg` | **MultiRoute Downloader** in Applications, Spotlight or the Dock |
| Intel, macOS 15+ | `MultiRoute-Downloader-4.1.3-macOS-x86_64.pkg` | **MultiRoute Downloader** in Applications, Spotlight or the Dock |

The twelve language guides and screenshots are included for offline use. Windows: `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`. macOS: in Finder choose **Show Package Contents** on the installed app, then open `Contents/app/docs/`. Open `en.md`, `ru.md`, `de.md`, `es.md`, `fr.md`, `zh.md`, `ko.md`, `ar.md`, `it.md`, `ja.md`, `hi.md` or `vi.md` in a text or Markdown viewer.

### Windows: install and open the application

1. Download the `.exe` and check it against the release's published SHA-256. Open it and follow the installation wizard. Installation is for your user account; select the offered shortcut options.
2. Open **MultiRoute Downloader** from the Start menu or shortcut. It opens as a normal desktop application, without a command window. Java is included; you do not need to install Java separately.
3. The application checks Npcap. If the driver is missing, approve its download in the application. It obtains the fixed official installer, verifies SHA-256 and its Authenticode publisher, then opens Npcap's normal installer. Read the licence and approve its Windows UAC request. If a restart is required, restart Windows and open MultiRoute again. Npcap is a separate driver, **not embedded or silently installed**; Internet is needed for this step when Npcap is missing.

The application and installer are not publisher-signed. SmartScreen may identify an unknown publisher: verify the official release and file before deciding whether to use **More info → Run anyway**, if that option is offered. A malware detection is a different case. Do not disable antivirus or create an exception as an installation step. An AdminOnly Npcap installation can require another capture-authorization prompt. Open the Java application normally, rather than running the whole application as administrator.

### macOS: install and open the application

1. Use the `.pkg` matching the **native** processor: arm64 for Apple Silicon, x86_64 for Intel. The new packages require macOS **26+ on Apple Silicon** or **15+ on Intel**, including torrent/magnet support. The older portable HTTP-only minimum does not apply to these packages.
2. Download the `.pkg`, check the published SHA-256 and open it in Apple's Installer. Confirm installation and the administrator authorization for placing the protected network component.
3. Open **`/Applications/MultiRoute Downloader.app`** in Finder or Spotlight. You can add its icon to the Dock. Java and the compiled network component are included: **Terminal, Command Line Tools, Homebrew and a separate Java installation are not required** for this package.
4. When requested, confirm the standard macOS administrator dialog so the protected component can open the Ethernet BPF device. It drops privileges after opening capture devices; the Java window runs under your normal user. No Terminal session is needed during operation.

#### macOS security confirmations for the native package

The package is **not Developer ID signed or notarized**. An ad-hoc code seal is not an Apple developer identity. First try opening the `.pkg` or application normally. If macOS reports that the developer cannot be verified and offers an exception, use **System Settings → Privacy & Security → Open Anyway**, then **Open**, only after checking the release's origin and checksum. A managed Mac may prohibit this. See [Apple's instructions](https://support.apple.com/en-us/102445).

Installation authorization, later network-component authorization and an unidentified-developer exception are separate confirmations. If requested, allow **Local Network** access for MultiRoute/Java to reach your LAN gateways. If a folder-access prompt appears, authorize the download destination you selected. Cancellation does not mean you should run all of Java as root.

The installer does not disable Gatekeeper/SIP, alter system routes, install a global `sudoers` exception or remove quarantine attributes globally. A malware/damaged-file warning is not the ordinary unidentified-developer dialog: stop and check the file. Actual permission wording can vary with macOS and organizational policy.

## Portable ZIPs 3.0 and Linux: deployment and first launch

### Windows

1. Extract the complete Windows package to a permanent writable folder, for example inside your user folder.
2. Double-click `Deploy-Windows.cmd`. It checks Windows/CPU, the application files, Java and Npcap. It starts the application when the prerequisites are ready.
3. If Java is missing or incompatible, it downloads a pinned official **Eclipse Temurin 21 JRE**, verifies SHA-256 and places it in the application's `runtime/` folder. It does not replace system Java. A compatible existing `JAVA_EXE`, `runtime/bin/java.exe`, `JAVA_HOME` or Java from `PATH` can be reused offline.
4. If Npcap is missing, it downloads the installer from **npcap.com**, checks its Authenticode signature and publisher, and opens the **interactive** installer. Read the licence, choose installation options and approve Windows **UAC** when you decide to install the driver. The driver is not bundled, silently installed or installed by granting administrative rights to the whole Java application.

Subsequent launches use `Start-Windows.cmd`; it performs the same prerequisite checks and can install missing software. Internet is required only when files or missing prerequisites must be downloaded. [Official Temurin installation information](https://adoptium.net/installation/) and the [Npcap user guide](https://npcap.com/guide/npcap-users-guide.html) describe those components.

If Npcap was installed with **“Restrict Npcap driver's access to Administrators only”**, opening capture devices requires authorization and may show another UAC prompt. A Java process without the required capture access cannot use that driver. The launcher does not change antivirus settings or create exceptions.

To select an existing Java executable, run from `cmd.exe` in the program folder:

```bat
set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe"
Start-Windows.cmd
```

Replace the path with your actual installation. If you explicitly set an invalid or incompatible `JAVA_EXE`, Windows deployment stops: correct or remove that override, then retry. It does not silently ignore your chosen executable. For API-only operation:

```bat
Start-Windows.cmd --headless
```

### Linux

1. Extract the Linux package to a permanent folder owned by your user.
2. Open `Deploy-Linux.desktop`. If your file manager does not support this portable launcher, open a terminal in the folder and run:

   ```bash
   bash ./Deploy-Linux.sh
   ```

3. Enter your password when `sudo` requests it. Missing dependencies are installed as specific packages through `apt-get`, `dnf` or `pacman`; deployment does not perform a complete distribution upgrade. Arch repositories must already be maintained through your normal distribution update procedure.
4. The installer compiles the gateway component for your CPU, runs its offline self-test and installs a protected copy owned by `root`.
5. Java starts under your normal user. **Keep the terminal open** while using the application: the helper authorization is tied to that session.

Native compatible Java 17+ is reused. Otherwise, a pinned official Temurin 21 JRE is downloaded, SHA-256 verified and installed privately for the application. Internet is needed for missing Java or packages.

| Distribution | Build/network packages |
| --- | --- |
| Debian/Ubuntu | `build-essential`, `binutils`, `libpcap-dev`, `pkg-config` |
| Fedora | `gcc`, `glibc-devel`, `make`, `binutils`, `libpcap-devel`, `pkgconf-pkg-config` |
| Arch | `gcc`, `make`, `binutils`, `pkgconf`, `libpcap` |

Deployment may also install `curl`, CA certificates, `tar`, `gzip` and required GUI libraries. For the desktop it checks `libX11`, `libXext`, `libXi`, `libXrender`, `libXtst` and `libfontconfig`. A server without a desktop should use `--headless`.

Use the application menu entry or `bash ./Start-Linux.sh` later. API-only deployment and launch:

```bash
bash ./Deploy-Linux.sh --headless
# Subsequent launches:
bash ./Start-Linux.sh --headless
```

Run those launchers as your normal user, **without a `sudo` prefix**. Only the capture component is authorized administratively; after opening capture devices it permanently drops supplementary groups, GID and UID to the launching user. System routes and adapter IP settings are not changed.

### macOS — portable 3.0 ZIP only

1. Extract the complete macOS package to a writable folder. Do not run deployment from inside a ZIP.
2. Open `Deploy-macOS.command` in Terminal. It checks macOS, native CPU, free space, Java and the gateway component.
3. If Apple Command Line Tools are missing, the **Apple installer** opens. Choose **Install**, accept Apple's licence if you agree, and wait for completion. The launcher waits for up to **30 minutes**; keep Terminal open. If installation is cancelled or times out, finish installing the tools and reopen deployment. A full Xcode or Homebrew installation is not required. See [Apple's installation instructions](https://developer.apple.com/documentation/xcode/installing-the-command-line-tools/).
4. It reuses native Java 17+ or downloads a pinned private Temurin 21 JRE and verifies SHA-256. On Apple Silicon it uses ARM64 even when Terminal is running under Rosetta.
5. When requested, enter an administrator password to install and authorize the gateway component. Terminal's `sudo` password prompt normally shows **no characters** as you type; enter the password and press Return. This authorization is for the protected helper, not for running the whole Java application as root.
6. Deployment creates `~/Applications/MultiRoute Resume.app` and opens it. For later launches use that application or `Start-macOS.command`. **Keep its Terminal session open** during operation.

#### macOS security confirmations

This release is **not signed with Developer ID or notarized**. After trying to open the downloaded launcher/app, an unidentified-developer warning may require **System Settings → Privacy & Security → Open Anyway**, then **Open** if macOS offers it. Confirm only after checking the package's origin. See [Apple's guidance](https://support.apple.com/en-us/102445). Older macOS versions use System Preferences → Security & Privacy. Managed Macs may prohibit an exception.

If macOS requests **Local Network** access for Terminal, Java or the application, allow it if you want to use the LAN gateways. Administrator authorization and Local Network access are separate from the developer warning. File access prompts may also appear for the folder you select; grant only the destination access you need.

Do not disable Gatekeeper/SIP or remove quarantine attributes globally. A warning that a file contains malware or will damage your computer is not the normal unidentified-developer confirmation; stop and investigate it rather than treating it as an installation step. The installer does not disable these protections. The portable 3.0 Finder/permission flow remains unverified on an end-user Mac. Native 4.0 builds and launcher/API checks ran on real GitHub Mac runners; end-user security dialogs and BPF capture remain unverified.

API-only launch from the extracted folder:

```bash
./Start-macOS.command --headless
```

## 3. Configure your network

The initial list is `192.168.1.1`, `192.168.1.2`, `192.168.1.3`, `192.168.1.4`, all selected by default. Your computer's usual connection is **not added as a fifth gateway**.

1. Open network settings and edit the gateway addresses to match your LAN.
2. Set the application's virtual address, initially `192.168.1.70`.
3. Check the address and gateways before downloading. Choose another address if it is occupied. Reserve it outside automatic DHCP allocation to prevent a router assigning it later.
4. Select one, several or all gateways.
5. Use **Add gateway** to expand the list: **1–32** distinct addresses are supported. You can add/remove profiles while downloading, provided every affected unfinished job keeps at least one active gateway. Also select the new gateway in the active job's gateway selection if that job should use it.

The virtual IP belongs to MultiRoute's internal transport. **It is not added to the OS adapter, and the OS routing table is not changed**; other programs keep their normal connection. Do not add this IP manually to Windows, Linux or macOS networking. The visible **?** beside the virtual IP and hover tips explain the controls.

Speed tests transfer up to **16 MiB or 5 seconds**; connection preparation can take extra time. These limits apply only to tests, not normal downloads. Without a suitable direct HTTP link, a Selectel test file is used.

## 4. Download, resume and manage

1. New profiles use **English**. The language selector also offers Russian, German, Spanish, French, Chinese, Korean, Arabic, Italian, Japanese, Hindi and Vietnamese; saved preferences are preserved.
2. Paste a URL in the long field with **Ctrl+V**, or **Cmd+V** on macOS. A paste button is also available. You can enter a magnet link or open a `.torrent` file.
3. Choose the download folder through your OS folder picker. New profiles initially use your user's `Downloads` folder; confirm the actual saved destination.
4. Select gateways and click **Start download** beside the URL. For a Hugging Face repository, WebDAV listing or Metalink, preview and select the files.
5. Watch total speed, each gateway's speed and transferred volume, elapsed time, estimated remaining time and the optional graph.

HTTP parts are **20 MiB by default**, configurable from 1 to 64 MiB. All gateways take work from a shared queue: a faster gateway can download more parts. If a part receives no data for **60 seconds**, its transfer is cancelled and it returns to the pool for another gateway. Torrent piece sizes come from torrent metadata, independently of the HTTP setting.

Pause/resume, change a running job's selected gateways, set priority/limits/schedules or change a stopped job's folder. The queue permits two active jobs by default. Jobs that were running are restored paused after restarting; resume them when appropriate.

Bulk import accepts link lists and text files. Search, status filters, context menus and torrent file selection are available. Seeding completed torrents is disabled by default; when enabled, set its time and ratio limits.

Select several download rows with **Ctrl+click** on Windows/Linux or **Cmd+click** on macOS; **Shift+click** selects a range. With the table focused, **Ctrl+A** or **Cmd+A** selects all currently visible rows, respecting search and status filters. Use the removal action for the selection: one confirmation covers the whole group. The option to also delete complete files and downloaded parts is **unchecked by default**, so you can remove only the list entries and keep the data. Select it only when you intend to delete those jobs’ files; other files and the destination folder are kept. Results show how many entries were removed. Jobs that could not be removed remain in the list with an error summary, so you can inspect them and retry; successful removals are not repeated. Removing jobs or files does not erase the persistent download history.

## 5. Protocols and verification

| Source | Behaviour and requirements |
| --- | --- |
| HTTP/HTTPS | Parts when byte ranges and safe file identity are available; discovers a published SHA-256 and compares the final file when one is available. |
| Hugging Face | File links and repository selection; Hub/Xet download resolution. Private access requires origin credentials and appropriate permissions. |
| FTP | Passive EPSV/PASV. Plain FTP does not encrypt credentials or data. |
| FTPS | Explicit TLS for FTP control and data connections; implicit FTPS is not supported. |
| SFTP | SSH with a **verified, pinned SHA256 server host-key fingerprint**. Configure credentials and the fingerprint first; unknown keys are not silently accepted. |
| WebDAV | HTTPS listing and file download; select files from the listing. |
| Metalink | `.meta4`, trusted SHA-256, mirrors and piece hashes when present in metadata. |
| Torrent/magnet | Verifies pieces against torrent metadata; file selection/priority and transport through selected gateways. Check the stricter native-library requirements in section 1. |

Configure usernames/passwords, Bearer tokens, cookies or permitted headers in **source access** settings if the source requires them. These are source credentials: your routers do not need access tokens for this program. Stored credentials are encrypted locally and restricted to your user; do not put passwords in URLs.

When an HTTP server ignores ranges, a whole-file stream uses **one** gateway: it cannot distribute/resume parts, and interruption restarts that stream from zero. FTP/FTPS/SFTP without a trusted published or supplied SHA-256 or SHA-512 also use the whole-file mode.

Not every server publishes SHA-256. A locally calculated hash is distinct from **comparison with a publisher's expected hash**; calculation alone does not prove publisher identity or an exact match. Torrent verification uses its metadata and does not establish the uploader's identity. You can supply an expected SHA-256 from a trusted source.

<a name="multiroute-api"></a>

## 6. API for other applications

While MultiRoute runs, its API is at `http://127.0.0.1:8765/api/v1`. JSON requests/responses use UTF-8. Creating and managing jobs does not require an access token. The API accepts local connections only; do not expose this port as a remote service.

Health check:

```http
GET http://127.0.0.1:8765/api/v1/health
```

Create a job with `POST http://127.0.0.1:8765/api/v1/jobs` and `Content-Type: application/json`:

```json
{
  "url": "https://example.org/large-file.bin",
  "directory": "C:/Users/your-user/Downloads",
  "chunk_mb": 20,
  "routes": [
    {"type": "gateway", "gateway": "192.168.1.1", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.2", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.3", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.4", "enabled": true}
  ]
}
```

Replace the URL and directory. Linux example: `/home/your-user/Downloads`; macOS: `/Users/your-user/Downloads`. The destination is on the computer running MultiRoute. Set `routes` explicitly: desktop checkboxes are not automatically copied into API requests. Do not supply worker addresses or router tokens.

`HTTP 201` means job creation, not completion. Retain the response `id`; poll `GET /jobs/{id}` until `status: "complete"`, or inspect `error` if it fails. API `speed_bps` values are **bytes per second**: multiply by 8 and divide by 1,000,000 for Mbit/s. Pause/resume, job gateway changes, batch import, queue settings and SSE `/events` are available.

Set network configuration in the window or with `POST /network` first. See the endpoint schemas in [openapi.yaml](../openapi.yaml) and the detailed [API.ru.txt](../API.ru.txt) (Russian). JSON keys and status values stay the same in every UI language.

## 7. Data, updates and removal

### Native installers 4.1.3

| Platform | Settings, history and logs | Application/runtime | Network component |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App`, with Java included | Included x64 component; Npcap installed separately |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app`, including Java | `/Library/PrivilegedHelperTools/MultiRouteResume/4.1.3/gateway-net` |

Close the application before updating with a new installer. Windows removal uses **Settings → Apps → Installed apps**. On macOS, removing the `.app` does not also remove saved history or the separately protected helper. Downloaded files stay in the folder you chose; do not delete that folder merely to uninstall the program. The data paths differ from Windows portable 3.0: preserve any earlier profile before migrating.

### Portable 3.0 and Linux

| Platform | Settings, history and logs | Private Java if installed | Protected component |
| --- | --- | --- | --- |
| Windows | `data/` under the launch folder, unless overridden by `data-location.config` or `--data-dir` | `runtime/` in the application folder | `native/windows-x64/gateway-net.exe` in the package; Npcap is a separately installed driver |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/runtime` | `/usr/local/libexec/MultiRouteResume/3.0/gateway-net` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `~/Library/Application Support/MultiRoute Downloader/runtime` | `/Library/PrivilegedHelperTools/MultiRouteResume/3.0/gateway-net` |

Downloads are in the destination you select, which may be outside these folders. Linux's menu entry is `~/.local/share/applications/MultiRouteResume.desktop`; macOS's application is `~/Applications/MultiRoute Resume.app`.

To update, close the application and deploy the new **complete** package. Rerun Linux deployment if you move the program folder. Preserve history and partial data to resume downloads. Removing a shortcut/window does not automatically remove private Java, saved data, protected helpers or installed OS packages. Do not delete your download folder just to uninstall the application.

## 8. Troubleshooting

| Symptom | Check |
| --- | --- |
| Prerequisite download fails | Internet/HTTPS access to official Temurin GitHub releases, `npcap.com` or your distribution repositories; free space. Retry the launcher or install the official component manually. Do not substitute an unverified binary. |
| Java missing/wrong CPU or no window | Full native Java 17+, complete package, `JAVA_EXE`/`JAVA_HOME`. Linux needs a desktop/XWayland and GUI libraries; use `--headless` for API-only operation. |
| Torrent reports `GLIBC_2.38 not found` or rejects macOS library | Torrent JNI needs glibc 2.38+, macOS 15+ Intel or 26+ Apple Silicon. Java's minimum does not override this. Do not manually replace OS libraries. |
| Npcap missing or capture access denied | Official x64 driver installation and capture authorization; an Admin-only installation can require UAC. Do not copy DLLs from an unknown source. |
| No Ethernet adapter | Active wired Ethernet, virtual IP and routers on the same subnet. Wi-Fi/VPN-only are not substitutes. Resolve ambiguity if two adapters match that LAN. |
| Virtual IP occupied | Choose and check another free address. It must not be assigned to your OS adapter; avoid automatic DHCP allocation. |
| Gateway unavailable or zero speed | LAN response and actual Internet access for the virtual IP; test each gateway. Check the URL, source permissions, file identity and job error. |
| SFTP host-key rejected | Verify the SHA256 fingerprint with the server administrator and configure the same value; retain host-key checking. |
| macOS/Linux authorization expired | Native macOS: reopen the app and approve the administrator dialog when requested. Portable 3.0/Linux: reopen the terminal launcher to renew `sudo` and keep its terminal open. Do not run all of Java as root. |
| Destination inaccessible or full | Destination permissions and space for file, pieces and torrent temporary data. Pause before changing a job's directory. |
| Port 8765 occupied | Startup replaces its own earlier instance. If another application owns the port, use `--port 8766`; do not terminate unrelated processes. |
| Existing download duplicated | Select the compatible existing job and resume it. Before removing it, choose whether to preserve its files and examine the job error. |

When reporting an issue, include version, OS/CPU, stage and exact error. Remove credentials, cookies, private URLs and personal data from any logs you decide to share.

## 9. Verification boundary

Release checks cover Java, protocols with synthetic fixtures, scheduling/resume logic, SHA-256 comparison and offline native self-tests on Windows/POSIX. Installer changes are checked with controlled fixtures; that does not establish successful live installation of every third-party package.

Native 4.0 packages were built and checked on Apple Silicon/macOS 26 and Intel/macOS 15 GitHub runners, including launcher/API startup, torrent JNI, package contents and helper fixtures. The physical-router combination, end-user installation/Finder/security dialogs and BPF capture remain unverified. Portable macOS 3.0 builds its helper on the target computer. The native 4.0 packages include separate precompiled Intel/Apple Silicon helpers; they are not Developer ID signed or notarized, and neither packaging checks nor CI prove operation on your Mac. After installing, test a known small download through each gateway, then through the selected set, before relying on combined speed. Detailed check reports accompany the release. Treat this public release as a **pre-release**.

## Tray and exit (4.0)

Closing the main window keeps the application and downloads running in the system tray. The icon displays the total download speed of **all jobs**, independent of the selected job or table filter. Hover over it for the exact rate in Mbit/s or Gbit/s (from 1 Gbit/s), plus bytes/s, including speeds up to 10 Gbit/s; the small icon rounds the value. Choose **Open application** from the tray menu to restore the window. On Windows/Linux, clicking the icon also opens it. Choose **Exit application** in the tray menu or in the application window to save state and stop the program. Use this command before installing an update.

If the desktop does not support a tray, closing minimizes the window to the taskbar and downloads continue; **Exit application** remains available. If the tray disappears, the hidden window reopens. Windows may place the icon in its hidden notification area.

Setup creates a desktop shortcut by default. Windows also adds a Start menu entry. macOS creates a Desktop link to the installed `.app` for the signed-in user and retries at the first graphical launch; existing Desktop items are preserved. If Desktop privacy permissions prevent creation, allow access when requested or open the app from Applications. Our setup texts are English regardless of the application language. System security and permission dialogs follow the OS language.

## Licence

You may install and use the original application free of charge for personal use and within organizations, including business activities. **Selling the application, charging for its licence or distributing it for payment is prohibited** without separate permission. The custom licence does not grant general modification or redistribution rights and is not an OSI-approved open-source licence. Third-party components retain their own licences. This is a summary; the controlling English terms are in [LICENSE.txt](../LICENSE.txt).

In 4.0 the speed icon has a transparent background with black digits and units, including 10 Gbit/s. The aggregate speed and tray controls are unchanged.

## MD5, SHA-256 and SHA-512 verification

The application calculates all three whole-file sums in one reading pass after assembly. Paste a reference into the main optional checksum field with Ctrl+V or Cmd+V. Its type is detected automatically: 32 hexadecimal characters mean MD5, 64 mean SHA-256, and 128 mean SHA-512. The optional Additional checksums section lets you provide multiple references.

Published references are discovered from supported whole-representation digest headers and sidecars: `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` and `SHA512SUMS`. A partial HTTP response digest is not treated as a checksum of the complete file. Hugging Face LFS SHA-256 and Metalink metadata retain their source-specific validation.

Every known reference must match before the completed file is published. A mismatch marks the job failed and preserves partial data. Without a reference, the displayed value is a locally calculated sum, not a confirmed match with the source. MD5 detects accidental corruption; use SHA-256 or SHA-512 for stronger integrity checks. MD5 does not replace strong source identity or trusted-mirror requirements.

Torrent piece verification remains mandatory. MD5/SHA-256/SHA-512 values for assembled torrent files are separate whole-file sums. A manual whole-file checksum requires exactly one selected file; for a group, provide a separate reference for each download.

The local API accepts `expected_md5`, `expected_sha256` and `expected_sha512` in `POST /api/v1/jobs`. Results expose `md5`, `sha256`, `sha512` and reference provenance in `checksum_sources`. Existing `expected_sha256` requests remain compatible.

## Individual download progress (4.0)

Each download has its own bar in the Progress column. Columns expand to fill the downloads panel. The list scrolls vertically when it contains many jobs; narrow panels also scroll horizontally without squeezing columns below readable widths. Refresh preserves the first visible job. Sorting and filtering keep each bar linked to its own download. If a total size is unknown, the bar shows received bytes instead of a percentage; hover for size, status and error details. Selecting a row still shows its detailed progress, time and gateway speeds.

The default download folder is resolved for the current user at launch: `%USERPROFILE%\Downloads` on Windows and `~/Downloads` on macOS/Linux. A saved default follows the current profile; a folder you select manually is retained.

## Log viewer (3.6)

The log window keeps the current reading position during automatic and manual refresh. When you scroll to the bottom, new entries remain visible. Scrolling up pauses following without stopping logging. Text selection remains available for copying. Changing the interface language preserves the visible event; reopening the window shows the latest entries.

## Download details and checksum report (3.7)

The lower details panel for the selected download keeps its reading position and text selection during repeated automatic refreshes. You can read the end of the checksum report without the panel returning to the beginning. Selecting a different download opens its report at the top. The separate log window retains its existing scroll behavior.

## Instructions and API guide (3.8)

The **Instructions** and **API guide** buttons are at the upper right, on a separate row below the language selector. They open the guide for the selected language in your default browser. **API guide** jumps directly to the local API section of that guide. The links use the application’s release version; Internet access is required to open the GitHub pages.

## Startup command handling (3.9)

Version 3.9 addresses a startup failure when a process lookup command times out and leaves its temporary output file locked on Windows. Command output now uses bounded pipes instead of temporary output files. Timed-out or interrupted lookup commands are stopped and awaited. Replacing an earlier MultiRoute instance still requires a verified application identity. The download protocols and local API contract are unchanged.

## Desktop interface and download history (4.0)

The desktop uses FlatLaf with **System**, **Light** and **Dark** themes. Choose the theme in the header; the choice is saved. The queue retains individual progress bars, full-width columns, sorting and filtering. A summary shows the total jobs, active jobs, completed jobs and aggregate speed across all jobs, independently of the current table filter.

Hover over a download row, including an active or completed file, to see its **full file name and absolute destination path**. This also works after sorting or filtering. The selected file heading has the same tooltip. Right-click a row and choose **Copy full path** to copy its destination.

Keyboard shortcuts: **Ctrl+L** (Windows/Linux) or **Cmd+L** (macOS) focuses the download URL; **Ctrl+F** or **Cmd+F** focuses the queue search; **Escape** clears the search. Normal Ctrl+V/Cmd+V paste remains available.

The selected download has **Details** and **History** tabs. The history records dated download events, including the first start, each continuation and successful completion. Old jobs without recorded dates do not receive invented timestamps. See the history storage and API fields below.

The application retains all download protocols, selectable gateways, the virtual IP, route speed tests, batch import, source access settings, MD5/SHA-256/SHA-512 checks, the tray, the local API and the existing log/details scroll behavior. The redesigned interface does not change system routes.

### History storage and API

The data folder contains `logs/downloads-history.jsonl` (UTF-8, one JSON object per line) and `logs/downloads-history.log` (readable dated entries). They are appended across restarts and retained when a job is removed; removing a job or its downloaded files does not erase this audit history. The human log keeps the language used when an event was written; the UI renders stored events in the currently selected language. The log window reads only a bounded tail of up to 1,000 events / 2 MiB; the complete journal stays on disk. It grows with use, so archive it separately when needed. Names and full destination paths are retained; source URLs, passwords and access tokens are not included in these lifecycle events. Review paths before sharing a log.

Native Windows: `%LOCALAPPDATA%\MultiRoute Downloader\data\logs\`. Native macOS: `~/Library/Application Support/MultiRoute Downloader/data/logs/`. Linux/portable profiles use the data folder described above; `--data-dir` selects another folder.

`GET /api/v1/jobs` and an individual job response expose `lifecycle_history`. Each entry has `event_id`, `job_id`, `kind`, `time`, `filename` and `path`; torrent entries may also have `paths`, and pause entries may have `reason`. Kinds are `started`, `resumed`, `paused`, `completed`, `recovered` and `failed`. Timestamps use ISO 8601 UTC (for example `2026-10-18T12:34:56.123Z`); readable logs include a full local date, time and UTC offset. Optional `first_started_at`, `last_resumed_at` and `completed_at` summarize observed events. Queued jobs have no start date until execution actually begins; completion is recorded after verification and publication. `recovered` means a previously published file was verified now, not that its original completion time is known. Existing jobs are not assigned guessed historical dates. All existing API operations remain available; no separate history endpoint is required.

The global **Log** window separates **Application events** from **Download history**. Use the history tab to review dated events across jobs, including removed jobs; the selected download’s History tab shows only that job.

## Name and existing profiles

MultiRoute Downloader was previously named MultiRoute Resume. Fresh profiles use the new data paths above. If no new-name data folder exists and an earlier MultiRoute Resume profile is available, the application reuses that saved profile without moving or deleting its files. Previous Windows data can remain at `%LOCALAPPDATA%\MultiRoute Resume\data` and macOS data at `~/Library/Application Support/MultiRoute Resume/data`. The protected `MultiRouteResume` helper directory, Windows upgrade identifier, internal API identity and port are retained for compatibility. After installing, use the MultiRoute Downloader shortcut or `.app`. Previous release assets keep their original filenames.

## Saved sites, uploads and archive previews (4.0)

Open **Site profiles…** to create/edit a profile, then choose it in the site selector on **Downloads** or **Uploads**. Network and virtual-IP controls are on **Network settings**. On **Uploads**, enter the remote **folder URL** and use **Choose files and upload…** to choose one or several local files with the system picker. The UI appends each filename; the API `target_url` below instead names one complete remote file. Paste an archive URL into the main URL field and click **Archive contents…**; the preview lists metadata without starting a download.

Save a site profile with a name, base URL, local download folder, remote folder and selected gateways. Profiles support anonymous access, HTTP Basic/Bearer/Cookie/custom headers, FTP credentials and SFTP credentials with a previously verified SHA-256 host key. Several profiles may use the same server with different accounts. Selecting a profile supplies its folders, gateways and credentials; explicit request settings can override folder/gateway defaults. Relative download or upload URLs are resolved against the profile base URL. Secrets are encrypted in the owner-only `site-profiles/vault.bin` using the local `site-profiles/key.bin`; this protects storage from casual inspection, not from someone controlling your account and its key. Public API responses omit credentials. Blank credential fields retain saved credentials only when the origin and authentication type are unchanged; `replace_credentials:true` deliberately replaces them. A foreign HTTP redirect never receives the profile credentials. Deleting a profile leaves jobs and local/remote files intact; a dependent task retains its original profile binding and cannot switch accounts in place; recreate the task with a valid profile if the original profile was removed.

Uploads and remote directory browsing support **FTP, explicit FTPS and SFTP** through selected application gateways. Select a regular local file and a full remote file URL (`target_url`); a directory URL alone is insufficient. Uploads use a temporary remote file and can continue from a verified matching prefix. Completion requires a full remote SHA-256 readback: the account needs remote **read, write and rename** permissions. Reading the uploaded file again consumes bandwidth. A readback mismatch or denied read permission is an error, not successful completion. Each file uses one gateway at a time; independent files run in parallel and are distributed over the selected gateways. A stopped gateway can be replaced by another selected gateway after the idle limit. This is not parallel splitting of one upload across gateways. Pause/resume and progress are retained across restarts. Removing an upload task preserves both the local file and remote final/partial files.

Overwrite is off by default. Enable it only when replacing the remote destination is intended. Standard FTP/SFTP servers do not universally provide an atomic “rename only if destination is absent”; an external client changing the same target can race the final rename. Coordinate shared remote destinations. Directory browsing does not upload or delete files.

Archive preview lists names, paths, sizes, directory flags and encryption flags for remote **ZIP, TAR, 7Z, RAR4 and RAR5** archives without extraction or execution. It requires a selected gateway, byte-range support and a stable ETag or Last-Modified validator. The metadata budget is 16 MiB, at most 10,000 entries and 60 seconds; 7z header decoding is limited to 64 MiB of memory. The preview can be truncated and is not an integrity check of archive contents. Supply the original filename charset if decoding fails. A password may be used for supported encrypted 7z headers; it is not persisted. Encrypted RAR headers are reported as unsupported/password-required. Split volumes and unsupported/encrypted headers may produce an incomplete listing.

The local API accepts `site_profile_id` for downloads, uploads, remote listings and archive previews. `GET /api/v1/site-profiles` returns `{profiles:[...]}`; `POST` (or `PUT`) saves a flat profile object and returns public metadata. `DELETE /api/v1/site-profiles/{id}` removes only that profile. `POST /api/v1/uploads` accepts `local_path`, `target_url`, `routes`, optional `site_profile_id` and `overwrite:false`; it returns HTTP 201. `GET /api/v1/uploads` returns `{uploads:[...]}`. Individual GET/DELETE and POST `/{id}/pause` / `/{id}/resume` operate on uploads. `POST /api/v1/remote/list` takes `url` and `route`/`routes`; `POST /api/v1/archives/preview` also accepts optional `password` and `charset`. See [openapi.yaml](../openapi.yaml) for typed schemas. Upload manifests are UTF-8 files under `data/uploads/`; they reference a profile ID, not a password. Archive previews do not create a download job.

## Download scheduling and priority

Use the queue/schedule controls to set `max_active_jobs` (1–32), global or gateway speed limits and a daily download window (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm). Times use the computer’s local timezone, also reported as `schedule_timezone`; a window may cross midnight, and equal start/end means all day. Outside the enabled window, downloads return to the queue and continue when allowed. A manually paused job remains paused. In a selected download’s priority/schedule settings, `priority` ranges from -100 to 100: higher queued jobs start first; equal priorities use creation order. Changing priority does not interrupt an already active download. `start_after_ms` is an optional earliest Unix-epoch start time in milliseconds; 0 removes the one-time delay. These settings persist across restarts. They govern the download queue, not the independent upload queue. Keep the application running (the tray is sufficient) and the computer awake; this is not an OS wake-up scheduler. The existing `GET`/`PUT`/`POST /api/v1/queue` and `PUT`/`POST /api/v1/jobs/{id}/options` expose the same controls.

## Chrome and Edge integration

Open the desktop **Chrome / Edge** integration dialog, enable link delivery and open its extension folder. Once in Chrome, open `chrome://extensions`; in Edge open `edge://extensions`. Turn on Developer mode, choose **Load unpacked** and select the `browser-extension` folder containing `manifest.json`. The native installers include it beside the application JAR; a source checkout also provides [browser-extension/](../browser-extension/). Keep this folder in place. If using the separate `MultiRoute-Downloader-4.1.3-Chrome-Edge.zip`, extract it first and select its folder. This release is loaded locally, not published in a browser store.

Keep MultiRoute running, including in the tray. Right-click a link and choose the MultiRoute download action, or use the extension popup to send a URL. The desktop uses the currently selected folder, gateways and site profile; repository/torrent choices still appear when needed. The popup port defaults to 8765; set it to the application’s actual local API port if changed. Only the URL is sent: browser cookies/passwords are not copied and ordinary browser downloads are not intercepted. Configure required source access in the application. If the browser requests permission for the local connection, approve it only for this chosen extension.

The bridge is off by default and accepts only this extension’s fixed origin `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` at `/api/v1/browser/status` and `/api/v1/browser/download`; other local API browser-origin rules remain unchanged. HTTP 202 means the URL reached the desktop flow, not that a file completed. A headless server alone cannot open the required desktop selection flow. Remove the extension on the browser’s extensions page or disable its desktop integration to stop link delivery.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
