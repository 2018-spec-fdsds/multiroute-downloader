# MultiRoute Resume 3.1 — English guide

**English** · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [Home](../README.md)

MultiRoute Resume is a Java desktop application that downloads files through one or more **gateways** on your local network. It assigns available parts to those gateways, preserves verified data and assembles the completed file. A local API lets other applications submit and manage downloads. The desktop window is not a web interface.

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Why use it?

If your network has several independent Internet connections, such as a fixed broadband line and a mobile 4G/5G router, the application can use their bandwidth simultaneously. Your computer reaches the gateways on the same local Ethernet network.

- **Higher aggregate speed:** all selected available gateways download file segments concurrently from a shared queue. Faster gateways can take more segments.
- **Resilience:** during resumable segmented downloads, a failed gateway leaves the remaining gateways working; unfinished segments return to the queue for another gateway, and verified data is retained.
- **Batch downloads:** import a list of URLs, manage several files in the queue or submit a group of downloads through the local API.

The speed gain depends on independent upstream capacity and source-server limits. Resuming and reassigning segments require a source and protocol that support safe resumption.

**Portable ZIP 3.0 only:** download the package for your platform from [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-resume/releases) and extract **the entire package**. Keep the JAR, libraries, launchers and native files together. GitHub's “Source code” archive contains source files: build it first or use a ready-to-run release. The deployment scripts report missing application binaries before downloading prerequisites.

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

## Native installers 3.1: Windows and macOS

Choose the installer for your system from [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-resume/releases), when it is attached to the release. Each is **one installer file**, containing the application, native launcher, full Java runtime and platform network component. The source checkout and portable 3.0 ZIPs are separate distributions.

| System | Installer | Launch after installation |
| --- | --- | --- |
| Windows 11 x64 | `MultiRoute-Resume-3.1-Windows-x64-Setup.exe` | **MultiRoute Resume** in the Start menu or its desktop shortcut |
| Apple Silicon, macOS 26+ | `MultiRoute-Resume-3.1-macOS-arm64.pkg` | **MultiRoute Resume** in Applications, Spotlight or the Dock |
| Intel, macOS 15+ | `MultiRoute-Resume-3.1-macOS-x86_64.pkg` | **MultiRoute Resume** in Applications, Spotlight or the Dock |

### Windows: install and open the application

1. Download the `.exe` and check it against the release's published SHA-256. Open it and follow the installation wizard. Installation is for your user account; select the offered shortcut options.
2. Open **MultiRoute Resume** from the Start menu or shortcut. It opens as a normal desktop application, without a command window. Java is included; you do not need to install Java separately.
3. The application checks Npcap. If the driver is missing, approve its download in the application. It obtains the fixed official installer, verifies SHA-256 and its Authenticode publisher, then opens Npcap's normal installer. Read the licence and approve its Windows UAC request. If a restart is required, restart Windows and open MultiRoute again. Npcap is a separate driver, **not embedded or silently installed**; Internet is needed for this step when Npcap is missing.

The application and installer are not publisher-signed. SmartScreen may identify an unknown publisher: verify the official release and file before deciding whether to use **More info → Run anyway**, if that option is offered. A malware detection is a different case. Do not disable antivirus or create an exception as an installation step. An AdminOnly Npcap installation can require another capture-authorization prompt. Open the Java application normally, rather than running the whole application as administrator.

### macOS: install and open the application

1. Use the `.pkg` matching the **native** processor: arm64 for Apple Silicon, x86_64 for Intel. The new packages require macOS **26+ on Apple Silicon** or **15+ on Intel**, including torrent/magnet support. The older portable HTTP-only minimum does not apply to these packages.
2. Download the `.pkg`, check the published SHA-256 and open it in Apple's Installer. Confirm installation and the administrator authorization for placing the protected network component.
3. Open **`/Applications/MultiRoute Resume.app`** in Finder or Spotlight. You can add its icon to the Dock. Java and the compiled network component are included: **Terminal, Command Line Tools, Homebrew and a separate Java installation are not required** for this package.
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

Do not disable Gatekeeper/SIP or remove quarantine attributes globally. A warning that a file contains malware or will damage your computer is not the normal unidentified-developer confirmation; stop and investigate it rather than treating it as an installation step. The installer does not disable these protections. Actual Finder/permission prompts and Apple hardware operation have not been verified for this release.

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

1. New profiles use **English**. The language selector also offers Russian, German, Spanish and French; saved preferences are preserved.
2. Paste a URL in the long field with **Ctrl+V**, or **Cmd+V** on macOS. A paste button is also available. You can enter a magnet link or open a `.torrent` file.
3. Choose the download folder through your OS folder picker. New profiles initially use your user's `Downloads` folder; confirm the actual saved destination.
4. Select gateways and click **Start download** beside the URL. For a Hugging Face repository, WebDAV listing or Metalink, preview and select the files.
5. Watch total speed, each gateway's speed and transferred volume, elapsed time, estimated remaining time and the optional graph.

HTTP parts are **20 MiB by default**, configurable from 1 to 64 MiB. All gateways take work from a shared queue: a faster gateway can download more parts. If a part receives no data for **60 seconds**, its transfer is cancelled and it returns to the pool for another gateway. Torrent piece sizes come from torrent metadata, independently of the HTTP setting.

Pause/resume, change a running job's selected gateways, set priority/limits/schedules or change a stopped job's folder. The queue permits two active jobs by default. Jobs that were running are restored paused after restarting; resume them when appropriate.

Bulk import accepts link lists and text files. Search, status filters, context menus and torrent file selection are available. Seeding completed torrents is disabled by default; when enabled, set its time and ratio limits.

When removing a job, the option to delete its files is **unchecked by default**. You can remove its history entry and retain the complete or partial file. Select file deletion only when you intend to remove that job's data.

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

When an HTTP server ignores ranges, a whole-file stream uses **one** gateway: it cannot distribute/resume parts, and interruption restarts that stream from zero. FTP/FTPS/SFTP without a trusted published or supplied SHA-256 also use the whole-file mode.

Not every server publishes SHA-256. A locally calculated hash is distinct from **comparison with a publisher's expected hash**; calculation alone does not prove publisher identity or an exact match. Torrent verification uses its metadata and does not establish the uploader's identity. You can supply an expected SHA-256 from a trusted source.

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

### Native installers 3.1

| Platform | Settings, history and logs | Application/runtime | Network component |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Resume\data` | `%LOCALAPPDATA%\MultiRoute Resume App`, with Java included | Included x64 component; Npcap installed separately |
| macOS | `~/Library/Application Support/MultiRoute Resume/data` | `/Applications/MultiRoute Resume.app`, including Java | `/Library/PrivilegedHelperTools/MultiRouteResume/3.1/gateway-net` |

Close the application before updating with a new installer. Windows removal uses **Settings → Apps → Installed apps**. On macOS, removing the `.app` does not also remove saved history or the separately protected helper. Downloaded files stay in the folder you chose; do not delete that folder merely to uninstall the program. The data paths differ from Windows portable 3.0: preserve any earlier profile before migrating.

### Portable 3.0 and Linux

| Platform | Settings, history and logs | Private Java if installed | Protected component |
| --- | --- | --- | --- |
| Windows | `data/` under the launch folder, unless overridden by `data-location.config` or `--data-dir` | `runtime/` in the application folder | `native/windows-x64/gateway-net.exe` in the package; Npcap is a separately installed driver |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/runtime` | `/usr/local/libexec/MultiRouteResume/3.0/gateway-net` |
| macOS | `~/Library/Application Support/MultiRoute Resume/data` | `~/Library/Application Support/MultiRoute Resume/runtime` | `/Library/PrivilegedHelperTools/MultiRouteResume/3.0/gateway-net` |

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

The physical-router combination and a complete real-Mac deployment/Finder flow remain unverified. Portable macOS 3.0 builds its helper on the target computer. The native 3.1 packages include separate precompiled Intel/Apple Silicon helpers; they are not Developer ID signed or notarized, and neither packaging checks nor CI prove operation on your Mac. After installing, test a known small download through each gateway, then through the selected set, before relying on combined speed. Detailed check reports accompany the release. Treat this public release as a **pre-release**.

## Licence

You may install and use the original application free of charge for personal use and within organizations, including business activities. **Selling the application, charging for its licence or distributing it for payment is prohibited** without separate permission. The custom licence does not grant general modification or redistribution rights and is not an OSI-approved open-source licence. Third-party components retain their own licences. This is a summary; the controlling English terms are in [LICENSE.txt](../LICENSE.txt).
