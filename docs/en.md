# MultiRoute Downloader 4.3.9 — English guide

[macOS: installation and first launch](macos/en.md)

<!-- product-overview:start -->
[**Product overview**](../README.md)
<!-- product-overview:end -->

**English** · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [Home](../README.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

<!-- current437:start -->

<a name="current-free-pro"></a>

## 4.3.9 — Current behavior, Free and Pro

This guide covers MultiRoute Downloader 4.3.9. Download native installers and published checksums from the [4.3.9 release](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9). The following sections describe its current features and license capabilities.

### License limits and features

Free includes ordinary single-file downloads, supported resume, full available speed, integrity checks, protected DNS and live gateway speeds/status. It does not deliberately throttle bandwidth. The following signed capabilities are independent; a partial Pro license unlocks only those it contains.

| Feature | Free | With the capability | ID |
| --- | --- | --- | --- |
| Gateways used together | ≤ 2 | ≤ 8 | `extra_gateways` |
| Simultaneous downloads | ≤ 3 | ≤ 32 | `extra_concurrent_downloads` |
| Site profiles | ≤ 3 | No license quota | `extra_site_profiles` |
| Download priorities | Normal only | Selectable | `download_priorities` |
| Gateway priorities | 0 | Selectable | `gateway_priorities` |
| Gateway history and reports | Not included | Included | `gateway_statistics` |
| Batch downloads / uploads | Not included | Included | `batch_downloads` |
| External application API | Not included | Included | `automation_api` |
| Additional skins | Graphite / System | All skins | `premium_appearance` |

For a batch, open **New download…** and choose **URL list…** in the fixed action row at the bottom of that window.

Only rights included in your signed license are unlocked. Existing files and profiles are retained. Extra files wait in the queue; live gateway speed and status remain available. Storage safety limits still apply.

The gateway limit is the total set in use across downloads and uploads, not an extra system gateway. The limit of three concurrent tasks applies to downloads, not uploads; other download tasks wait. A smaller configured queue limit still applies. Existing gateway lists, profiles, progress and saved priorities are retained. The profile limit applies to creating new profiles; older profiles remain usable. Historical traffic/CSV needs its capability; live meters remain Free. Scheduling remains available; non-default priorities require their capability. Technical storage and source limits still apply.

The signed license belongs to the installation key, not to a MAC, IP address or router. It has no expiry date; the major range 4–4 covers 4.x. Changing gateways, DHCP or the application MAC does not require reactivation. A new computer, user account or data profile may require a new request. This is not a claim of unpatchable code or universal hardware binding.

### Application MAC

MultiRoute uses its own stable MAC in manual-IP and DHCP modes. The OS network adapter MAC is unchanged. Observed devices and other MultiRoute instances are checked for conflicts; no reply does not prove that powered-off devices are absent. Pool: locally administered unicast MAC addresses, 46 random bits. This is not a DHCP address pool.

**Change MAC…** — Use this button only after active and queued transfers are paused or finished and local checksum checks finish. MultiRoute checks observed source addresses and cooperative announcements from other MultiRoute instances. “No conflict detected” is not a guarantee that an offline or silent device does not use the address. The application MAC must differ from the physical adapter MAC; changing it does not change the adapter or OS routes.

### Checksum…

The reference is saved without restarting or clearing progress. MD5, SHA-256 or SHA-512 is detected automatically from 32, 64 or 128 hex digits. The assembled file will be checked after downloading. Save a reference, then choose Check now. This reads the local completed file. A mismatch is reported without downloading again or deleting the file.

The latest saved reference is used for final assembly verification. Editing it does not clear bytes or restart the transfer. A completed-file recheck keeps the file and reports verified, mismatch or error separately from download completion. Publisher references are retained independently. One whole-file reference cannot describe several torrent files. Hash comparison checks integrity against a reference, not whether content is harmless; prefer a trusted SHA-256/SHA-512 reference for security decisions.

### API access in 4.3.9

The localhost JSON API uses UTF-8 and no ordinary API token. External management requires automation_api; other capabilities are checked too, so that right alone does not permit batch creation or extra gateways. Free retains narrow read-only diagnostics, the private authenticated lifecycle endpoint, and the opt-in single-link browser bridge. See [API reference](../API.ru.txt) and [OpenAPI](../openapi.yaml); do not expose the port to the LAN/Internet.


The original application uses [License 1.2](../LICENSE.txt): Free is allowed for personal and organizational/business use; paid Pro terms are offered by the rights holder. Other recipients may not sell/resell it without separate permission. OpenJDK GPLv2 with Classpath Exception requires its notices/corresponding sources; JNA is used under Apache-2.0 with required notices; separate JLayer LGPL-2.1 retains replacement/debugging and source rights. See [licensing](../LICENSING.md), [full third-party notices](../THIRD-PARTY-NOTICES.txt) and the application's licenses page.

System appearance keeps table text, fields and controls readable against the selected OS background; Graphite and System are included in Free.

File names and sizes have separate columns. Sort size numerically, independently of file names; unknown size is shown as a dash. Full names and paths are available in tooltips.

Hover over a file name to see its full name, save path, transfer protocol and source address or local source path. Torrent tasks show BitTorrent even when their metadata was opened through HTTPS. When available, the repository address appears below the actual file URL. Embedded credentials, query values and fragments are hidden; a recognized public magnet infohash may remain visible. This changes only the tooltip, not the saved download URL or transfer state.


### Gateway priorities and transfer diagnostics in 4.3.9

In Network settings, set each gateway’s priority from **0 to 1000**; the main gateway meter shows its value. A higher number prefers that gateway for a small or whole-file download and gives it more parallel segments for a large file. Selected working gateways still help. Equal values preserve the selected order; a failed or stalled preferred gateway can hand work to another selected gateway. This is a preference, not a promised bandwidth ratio.

Gateway priorities require the independent signed **`gateway_priorities`** capability. The file-queue priority right **`download_priorities`** does not include it. Without the gateway right, every effective gateway priority is 0; saved values are retained. You can change priorities during a download without clearing verified parts. The allocator uses one to four parallel segment workers per selected gateway, with at most 32 workers per job.

Settings expose the number of simultaneous downloads within the signed limit and finite or unlimited automatic retry attempts. Manual pauses stay paused. Right-click one or several downloads to pause/resume supported tasks or copy file names. Double-click a file to open its folder; file properties, statistics and the individual download log are in the context menu.

The individual log remains available for completed files. Settings → **Errors and solutions** opens an offline searchable guide with 25 topics in all 12 interface languages. Save-location controls and file properties show available space in GB. A local full-disk condition is reported as **Not enough space on disk**, retains saved parts and does not become an endless network retry. Saved-prefix progress stays visible while a resumed small segment waits for new data.

### Remaining download data · 4.3.9

The download table now has a separate **Remaining data** column. It shows the bytes still to download, independently of the remaining-time estimate, and sorts numerically. The value uses current saved and in-flight progress, never falls below zero, and is shown as a dash while the total size is unknown.

### Safer Mac startup and gateway setup · 4.3.9

Native Mac startup recovers the recognizable legacy relative default, including a missing leading slash, using a verified existing profile or an empty user-owned default. Ambiguous custom locations still require an explicit choice instead of guessing.

After gateway preparation fails or is cancelled, remaining gateways and background checks stop repeating authorization requests until you explicitly retry network setup. The first privileged start may still require the normal macOS administrator confirmation to open the Ethernet BPF device.

The Mac network component 4.3.2 handles an adapter MAC hidden by macOS while retaining checks that protect the physical adapter. The application keeps its own MAC; OS routes and the physical adapter MAC are not changed. Mac icon and application name now use the same MultiRoute identity as Windows. Exit the app before running the PKG updater. The caller label of a real authorization dialog and physical LAN/DHCP transfers still require hardware confirmation; wired Ethernet remains required for this transport.

**macOS:** `/Applications/MultiRoute Downloader.app` · `/Library/PrivilegedHelperTools/MultiRouteResume/4.3.2/{gateway-net,gateway-launch}`.

### Mac gateway replies with a manual address · 4.3.9

The POSIX capture setup is corrected so both a manually selected application address and DHCP use a receive mode that accepts replies addressed to the application’s own MAC, matching the existing Windows behavior. A working system ping tests the computer’s own connection; it does not prove that the application’s virtual address can receive gateway replies.

Existing DHCP diagnostics keep their twelve-language wording instead of being replaced by a generic error. The existing authorization behavior is unchanged: normal network checks reuse the prepared component. This changes neither API endpoints nor interface controls, OS routes or the physical adapter MAC. First privileged Ethernet BPF access may still need normal administrator authorization.

This addresses one capture-mode mismatch, not every cause of an unreachable gateway. Address availability, subnet and DHCP-lease/conflict checks remain active. Physical Mac transfers with a manual address still require hardware confirmation; wired Ethernet remains required.

<!-- current437:end -->

<a name="appearance-and-sounds"></a>

## Appearance and completion sounds

Choose from nine appearances: Graphite, Nordic, Streams Studio, Neon, Community, Chat, Studio, Rose and System. Graphite is the default. Graphite and System are included in Free; the other choices require the corresponding Pro appearance capability. Open Settings → Appearance to change the style and rounded controls. Your downloads continue while you choose. **Rounded windows and buttons** is a separate option. Rounded controls work across styles; the outer window frame depends on the OS. Windows 11 may keep maximized or snapped windows rectangular; macOS and Linux control their own window outlines. The desktop includes a speed graph, and Network settings use compact gateway cards.

Completion sounds are **off by default**. Enable them with the master checkbox, then independently choose notifications for **each file** and **all downloads**. Choose **Soft bells**, **Piano**, or **Marimba**, and preview either the file or final melody. When all downloads finish and the all-downloads option is enabled, only the final melody plays for the last file, avoiding a second overlapping cue. Paused or failed jobs are not finished downloads. Previously completed history stays silent at startup and when preferences change. Audibility also depends on the system volume and selected audio output.

For your own audio, choose **Custom** and select separate **MP3 or WAV** files for the file-complete and all-downloads cues. Each clip must be **10 seconds or shorter**; the application validates its decoded duration, rather than trusting its extension or displayed metadata. Accepted audio is copied into the local application profile, so the original file may be moved afterward.

The optional desktop courier turns download progress into a visible load. Choose the man or woman in Appearance settings and set the size from 40% to 100%. The bag shows the combined known remaining data of active downloads and shrinks as bytes arrive. Movement follows their combined download speed; at zero speed the courier stands still. Small loads are carried on the shoulder, larger loads are dragged and very large loads show more effort.

Use the Avatar button to show or hide the courier. Drag it to move it, double-click with the left mouse button to show the application, or right-click for its options. This is a visual companion: files remain in their selected folders.

When a download finishes, the courier puts down the delivered load and fades. If work continues, it returns with the remaining data.

Appearance, rounding and sound preferences are local desktop settings. They do not add download API endpoints or change transfer, checksum or DNS security settings.

<a name="startup-profile-location"></a>

## Application data folder

Native application launches use a saved absolute data-folder path, independent of the working directory at sign-in. Download history, settings and saved parts are stored in the selected profile. Portable command-line paths are supported; an explicit `--data-dir` selects the data folder.

<a name="automatic-startup"></a>

## Automatic startup

Use **Start with the operating system** beside the language and appearance controls. It is **off by default**. Enabling it registers MultiRoute for your account's next sign-in; disabling it removes the application's own startup entry. It is a user-login setting, not a system service or a launch before sign-in. Enrolling this entry does not require administrator access.

Automatic startup uses the tray when supported and otherwise leaves a normal reachable window. Existing queue priorities, pauses and schedules remain in force; startup does not override them or force every paused download to resume. Complete initial installation and network prerequisites by opening the application manually first. Keep the installed application and, for portable use, Java and libraries at their registered paths. After moving them, disable and enable startup from the new location. Disable startup before uninstalling.

Windows or desktop policy may disable a registered entry. On macOS, review **System Settings → General → Login Items & Extensions** if approval is requested; network-component authorization is separate. Registration status alone cannot establish that a future login will launch successfully. Actual reboot/sign-out testing is not included in the owned-profile checks; see the matching release report.

If the application cannot be opened, remove **only its own entry**:

- **Windows:** the application stores its own `REG_SZ` value **MultiRoute Downloader** under `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`. Prefer turning startup off in the app. If the app cannot open, use Registry Editor to delete **only that value**, keeping the Run key and other entries. For an OS-disabled entry, use **Settings → Apps → Startup**; the app does not override that decision. Windows limits the complete startup command to 260 characters; if it is too long, use a shorter application/profile path and register again.
- **macOS:** remove `~/Library/LaunchAgents/io.github.2018-spec-fdsds.multiroute-downloader.plist`.
- **Linux:** remove `${XDG_CONFIG_HOME:-~/.config}/autostart/io.github.2018-spec-fdsds.multiroute-downloader.desktop` from your actual XDG configuration directory. A graphical desktop supporting XDG autostart is required; this setting does not install a systemd service.

<a name="dns-settings"></a>

## DNS settings

Platform and installer verification results are in [NATIVE-INSTALLER-TESTS.json](../NATIVE-INSTALLER-TESTS.json).

Open **Network → DNS resolution**. The application defaults to **DNS over HTTPS (DoH), Cloudflare**. This setting belongs to MultiRoute; it does not change the computer's DNS servers, routes or interface IP addresses.

DoH supports bounded HTTP/2 selected by TLS ALPN and HTTP/1.1 when that protocol is negotiated. Provider interoperability and live gateway results belong to the matching final release report.

- **DoH:** encrypted HTTPS DNS through each selected gateway. The URL hostname authenticates the TLS certificate.
- **DoT:** encrypted DNS over TLS through each selected gateway; port 853 by default. The server name authenticates the TLS certificate.
- **System DNS (computer):** explicitly use the computer/OS/VPN resolver policy. DNS is not guaranteed to follow each selected gateway, although file connections still do. This mode is not a guarantee of encrypted DNS.

Choose Cloudflare, Quad9, Google or **Custom**. Presets are fixed; Custom enables the relevant URL/server name, port and numeric public IPv4 bootstrap addresses. Bootstrap addresses connect directly without first resolving the DNS provider through the OS. DoH/DoT verify trusted certificates, hostname and SNI; neither silently falls back to unencrypted DNS or System mode. A failed secure lookup fails that attempt. Use a provider reachable through the selected gateway.

Edit a draft, select gateways, then choose **Test DNS**. Testing uses the draft without saving it and shows a result, elapsed time and public IPv4 addresses or an error for each gateway. A partial failure remains visible. In System mode a successful lookup checks OS resolution, not independent WAN reachability through every gateway. Choose **Apply DNS settings** to validate and save the configuration. The returned revision identifies the applied configuration; background status refresh and language changes preserve an unsaved draft, caret and reading position.

Each lookup has an 8-second total budget, including auxiliary validation queries. Validated TTL also respects signature expiry; native reuse is capped at 300 seconds and separated by gateway/configuration revision. TTL zero and DoH HTTP Age remain significant. System mode follows the OS/JVM cache policy. Public IPv4 file destinations are supported; IPv6 resolution is not supported.

API: `GET /api/v1/network/dns` reads configuration and readonly state; `PUT` (or `POST`) at the same path applies configuration. `POST /api/v1/network/dns/test` tests a draft; `GET /api/v1/network/dns/providers` lists presets. Send only configuration keys, not the revision/status fields from GET. See the [API reference](../API.ru.txt) and [OpenAPI schema](../openapi.yaml).

<a name="dnssec-validation"></a>

## Local DNSSEC validation

In **Network → DNS resolution → DNSSEC validation**, choose **Validate signed domains** (default for DoH/DoT), **Require secure DNSSEC answers**, or **DNSSEC disabled**. Validation runs locally in Java with dnsjava and bundled root trust anchors; an upstream AD flag alone is not proof. DoH/DoT encrypts the transport; DNSSEC separately authenticates DNS data.

**Validate** accepts secure and insecure answers; **Require** accepts only secure answers. Insecure can mean an authenticated unsigned delegation or unsupported algorithms, so read the reason. Bogus and indeterminate results are rejected while validation is enabled. System DNS disables local validation; returning to DoH/DoT restores your last encrypted-mode policy. Changing provider presets preserves that policy. Apply saves it; Test DNS checks the unsaved draft.

Per-query results distinguish `secure`, `insecure`, `bogus`, `indeterminate` and `not_checked`; missing results are shown as unavailable. A successful transport is not a secure DNSSEC verdict. Expired signatures, missing keys/proofs, lookup failure and validation timeouts have separate reasons. The local clock must be correct. Auxiliary validation lookups use the same selected gateway and total query budget. DNSSEC does not hide names or inspect downloaded file contents.

API configuration adds `dnssec`: `off`, `validate` or `require` (System mode accepts only `off`). Readonly fields include `dnssec_validation`, `dnssec_trust_anchors`, and per-query `dnssec_status`, `dnssec_reason_code`, `dnssec_reason`, `dnssec_validated_ttl`, `dnssec_aux_queries`. Do not echo these fields into settings writes. See the API/OpenAPI links above and the [DNSSEC](#dnssec-validation).

**HTTP range handling:** CDNs such as Civitai that return HTTP 200 for a range starting at zero are checked with a nonzero range. The first segment stays bounded; compatible saved sequential data can resume as segments. A temporary HTTP 200 for a nonzero range retries after 2–30 seconds while preserving saved data and keeping the gateway worker available. A persistent per-gateway transfer journal distinguishes received traffic from saved progress. [Transfer diagnostics](transfer-diagnostics.en.md).

[4.3.9 installers](../downloads/v4.3.9/README.md) include a private Java 21 runtime. Windows Setup uses English and creates a desktop shortcut; Npcap remains separately required. macOS uses system libpcap. Follow the installation and security confirmations below.

MultiRoute Downloader is a Java desktop application that downloads files through one or more **gateways** on your local network. It assigns available parts to those gateways, preserves verified data and assembles the completed file. A local API lets other applications submit and manage downloads. The desktop window is not a web interface.

Project owner: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Why use it?

If your network has several independent Internet connections, such as a fixed broadband line and a mobile 4G/5G router, the application can use their bandwidth simultaneously. Your computer reaches the gateways on the same local Ethernet network.

- **Higher aggregate speed:** all selected available gateways download file segments concurrently from a shared queue. Faster gateways can take more segments.
- **Resilience:** during resumable segmented downloads, a failed gateway leaves the remaining gateways working; unfinished segments return to the queue for another gateway, and verified data is retained.
- **Batch downloads:** import a list of URLs, manage several files in the queue or submit a group of downloads through the local API.

The speed gain depends on independent upstream capacity and source-server limits. Resuming and reassigning segments require a source and protocol that support safe resumption.

<a name="application-virtual-ip"></a>

### Why the application uses a virtual IP

**Choose gateways for MultiRoute without changing the connection used by your other applications.** The application uses its own unused local IPv4 address for its internal network transport. This lets it establish connections through the selected gateways independently of the computer’s default gateway, without changing OS interface addresses, the system routing table or the system default gateway. Browsers and other programs keep their existing network policy.

For example, in a `192.168.1.0/24` LAN, the computer keeps its normal `192.168.1.x` address, MultiRoute can use the free address `192.168.1.70`, and downloads can use gateways `192.168.1.1`–`192.168.1.4`. The computer, virtual address and selected gateways must belong to the same actual IPv4 subnet. Check and apply a free address in the app’s Network settings; do not add it manually to the OS adapter. The virtual IP is not another Internet connection: the routers and their upstream links provide the bandwidth.

#### Choosing and checking the address

1. Check the computer’s Ethernet IPv4 address and subnet mask. Choose a usable host address in that same subnet. `192.168.1.70` is only an example; do not choose the computer’s own address, any gateway address, the network address or the broadcast address.
2. Open your router’s administration page and check the actual DHCP allocation pool. Use an address outside automatic allocation, or reserve/exclude it on the DHCP server. If several DHCP servers serve this LAN, the address must be protected from allocation by all of them.
3. In MultiRoute, choose **Check and apply** for the virtual address, then check the gateways. The application validates the subnet and performs an ARP conflict check. No ARP reply means no conflict was detected at that moment; it does not prove the address is outside the DHCP pool or prevent a later DHCP allocation.

## Screenshots

Current application windows are shown below. Open the full-size image to read individual controls. Example transfer values are illustrative.

![MultiRoute Downloader — download queue and gateway speeds](images/v4.3.7/downloads-en-ae5b55131583.png)

*Current Windows interface, captured in an isolated demonstration profile. Example files, progress, speeds and Pro-enabled views illustrate the controls and are not performance measurements.*

<table>
<tr>
<td width="50%" valign="top"><strong>Application settings</strong><br /><a href="images/v4.3.7/settings-8abcb12a782d.png"><img src="images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Application settings" /></a></td>
<td width="50%" valign="top"><strong>Appearance, courier and sounds</strong><br /><a href="images/v4.3.7/appearance-cebf121d5de7.png"><img src="images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Appearance, courier and sounds" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Application address and protected DNS</strong><br /><a href="images/v4.3.7/network-dns-e29dfdba1007.png"><img src="images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Application address and protected DNS" /></a></td>
<td width="50%" valign="top"><strong>Gateway selection and speed checks</strong><br /><a href="images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Gateway selection and speed checks" /></a></td>
</tr>
</table>






## Required environment

Private Java 21 and the compiled network component are included in the Windows/macOS installers. Windows requires the separate Npcap driver. No separate Java installation is needed.

| Platform | Operating system | Included components |
| --- | --- | --- |
| Windows x64 | Windows 11 | Java 21 |
| Apple Silicon | macOS 26+ | Java 21 |
| Intel Mac | macOS 15+ | Java 21 |

Use a wired Ethernet adapter on the same IPv4 subnet as all selected gateways, a free virtual IPv4 address, writable download/data folders and enough space for the file and temporary parts. Independent WAN connections and source-server capacity determine the possible speed gain. A GPU is not required.

Linux is supported by the application, but no Linux 4.x installer is published in this distribution repository. Do not use a Windows or macOS package on Linux.

Before updating, wait for active downloads, uploads and final file verification to finish. Then exit from the tray and run the complete new installer. Preserve the existing data folder, downloaded files and saved parts.

## Native installers 4.3.9: Windows and macOS

| System | Installer | Launch after installation |
| --- | --- | --- |
| Windows 11 x64 | [MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe) | **MultiRoute Downloader** in the Start menu or its desktop shortcut |
| Apple Silicon, macOS 26+ | [MultiRoute-Downloader-4.3.9-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg) | **MultiRoute Downloader** in Applications, Spotlight or the Dock |
| Intel, macOS 15+ | [MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg) | **MultiRoute Downloader** in Applications, Spotlight or the Dock |

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

<a name="3-configure-your-network"></a>

## Configure your network

The editable example gateways are `192.168.1.1`, `192.168.1.2`, `192.168.1.3` and `192.168.1.4`. Select gateways manually for your LAN: Free allows up to **2 gateways** in use together; the corresponding Pro capability allows up to **8**. The computer's usual connection is **not added as an extra gateway**.

1. Open network settings and edit the gateway addresses to match your LAN.
2. Set the application's virtual address, initially `192.168.1.70`.
3. Check the address and gateways before downloading. Choose another address if it is occupied. Reserve it outside automatic DHCP allocation to prevent a router assigning it later.
4. Select at least one gateway, within your license capability limit.
5. Use **Add gateway** to expand the list: **1–8** distinct addresses are supported. You can add/remove profiles while downloading, provided every affected unfinished job keeps at least one active gateway. Also select the new gateway in the active job's gateway selection if that job should use it.

The virtual IP belongs to MultiRoute's internal transport. **It is not added to the OS adapter, and the OS routing table is not changed**; other programs keep their normal connection. Do not add this IP manually to Windows, Linux or macOS networking. The visible **?** beside the virtual IP and hover tips explain the controls.

Speed tests transfer up to **16 MiB or 5 seconds**; connection preparation can take extra time. These limits apply only to tests, not normal downloads. Without a suitable direct HTTP link, a Selectel test file is used.

<a name="4-download-resume-and-manage"></a>

## Download, resume and manage

1. New profiles use **English**. The language selector also offers Russian, German, Spanish, French, Chinese, Korean, Arabic, Italian, Japanese, Hindi and Vietnamese; saved preferences are preserved.
2. Paste a URL in the long field with **Ctrl+V**, or **Cmd+V** on macOS. A paste button is also available. You can enter a magnet link or open a `.torrent` file.
3. Choose the download folder through your OS folder picker. New profiles initially use your user's `Downloads` folder; confirm the actual saved destination.
4. Select gateways and click **Start download** beside the URL. For a Hugging Face repository, WebDAV listing or Metalink, preview and select the files.
5. Watch total speed, each gateway's speed and transferred volume, elapsed time, estimated remaining time and the optional graph.

HTTP parts are **20 MiB by default**, configurable from 1 to 64 MiB. All gateways take work from a shared queue: a faster gateway can download more parts. If a part receives no data for **60 seconds**, its transfer is cancelled and it returns to the pool for another gateway. Torrent piece sizes come from torrent metadata, independently of the HTTP setting.

Pause/resume, change a running job's selected gateways, set priority/limits/schedules or change a stopped job's folder. The queue permits two active jobs by default. Jobs that were running are restored paused after restarting; resume them when appropriate.

Bulk import accepts link lists and text files. Search, status filters, context menus and torrent file selection are available. Seeding completed torrents is disabled by default; when enabled, set its time and ratio limits.

Select several download rows with **Ctrl+click** on Windows/Linux or **Cmd+click** on macOS; **Shift+click** selects a range. With the table focused, **Ctrl+A** or **Cmd+A** selects all currently visible rows, respecting search and status filters. Use the removal action for the selection: one confirmation covers the whole group. The option to also delete complete files and downloaded parts is **unchecked by default**, so you can remove only the list entries and keep the data. Select it only when you intend to delete those jobs’ files; other files and the destination folder are kept. Results show how many entries were removed. Jobs that could not be removed remain in the list with an error summary, so you can inspect them and retry; successful removals are not repeated. Removing jobs or files does not erase the persistent download history.

<a name="5-protocols-and-verification"></a>

## Protocols and verification

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

<a name="6-api-for-other-applications"></a>

## API for other applications

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

<a name="7-data-updates-and-removal"></a>

## Data, updates and removal

### Native installers 4.3.9

| Platform | Settings, history and logs | Application/runtime | Network component |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App`, with Java included | Included x64 component; Npcap installed separately |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app`, including Java | `/Library/PrivilegedHelperTools/MultiRouteResume/` |

<a name="8-troubleshooting"></a>

## Troubleshooting

When reporting an issue, include version, OS/CPU, stage and exact error. Remove credentials, cookies, private URLs and personal data from any logs you decide to share.

<a name="tray-and-exit-40"></a>

## Tray and exit

Closing the main window keeps the application and downloads running in the system tray. The icon displays the total download speed of **all jobs**, independent of the selected job or table filter. Hover over it for the exact rate in Mbit/s or Gbit/s (from 1 Gbit/s), plus bytes/s, including speeds up to 10 Gbit/s; the small icon rounds the value. Choose **Open application** from the tray menu to restore the window. On Windows/Linux, clicking the icon also opens it. Choose **Exit application** in the tray menu or in the application window to save state and stop the program. Use this command before installing an update.

If the desktop does not support a tray, closing minimizes the window to the taskbar and downloads continue; **Exit application** remains available. If the tray disappears, the hidden window reopens. Windows may place the icon in its hidden notification area.

Setup creates a desktop shortcut by default. Windows also adds a Start menu entry. macOS creates a Desktop link to the installed `.app` for the signed-in user and retries at the first graphical launch; existing Desktop items are preserved. If Desktop privacy permissions prevent creation, allow access when requested or open the app from Applications. Our setup texts are English regardless of the application language. System security and permission dialogs follow the OS language.

## Licence

You may install and use the original application free of charge for personal use and within organizations, including business activities. **Selling the application, charging for its licence or distributing it for payment is prohibited** without separate permission. The custom licence does not grant general modification or redistribution rights and is not an OSI-approved open-source licence. Third-party components retain their own licences. This is a summary; the controlling English terms are in [LICENSE.txt](../LICENSE.txt).

The speed icon has a transparent background with black digits and units, including 10 Gbit/s. It displays aggregate speed and provides the tray controls described above.

## MD5, SHA-256 and SHA-512 verification

The application calculates all three whole-file sums in one reading pass after assembly. Paste a reference into the main optional checksum field with Ctrl+V or Cmd+V. Its type is detected automatically: 32 hexadecimal characters mean MD5, 64 mean SHA-256, and 128 mean SHA-512. The optional Additional checksums section lets you provide multiple references.

Published references are discovered from supported whole-representation digest headers and sidecars: `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` and `SHA512SUMS`. A partial HTTP response digest is not treated as a checksum of the complete file. Hugging Face LFS SHA-256 and Metalink metadata retain their source-specific validation.

Every known reference must match before the completed file is published. A mismatch marks the job failed and preserves partial data. Without a reference, the displayed value is a locally calculated sum, not a confirmed match with the source. MD5 detects accidental corruption; use SHA-256 or SHA-512 for stronger integrity checks. MD5 does not replace strong source identity or trusted-mirror requirements.

Torrent piece verification remains mandatory. MD5/SHA-256/SHA-512 values for assembled torrent files are separate whole-file sums. A manual whole-file checksum requires exactly one selected file; for a group, provide a separate reference for each download.

The local API accepts `expected_md5`, `expected_sha256` and `expected_sha512` in `POST /api/v1/jobs`. Results expose `md5`, `sha256`, `sha512` and reference provenance in `checksum_sources`. Existing `expected_sha256` requests remain compatible.

<a name="individual-download-progress-40"></a>

## Individual download progress

Each download has its own bar in the Progress column. Columns expand to fill the downloads panel. The list scrolls vertically when it contains many jobs; narrow panels also scroll horizontally without squeezing columns below readable widths. Refresh preserves the first visible job. Sorting and filtering keep each bar linked to its own download. If a total size is unknown, the bar shows received bytes instead of a percentage; hover for size, status and error details. Selecting a row still shows its detailed progress, time and gateway speeds.

The default download folder is resolved for the current user at launch: `%USERPROFILE%\Downloads` on Windows and `~/Downloads` on macOS/Linux. A saved default follows the current profile; a folder you select manually is retained.

<a name="log-viewer-36"></a>

## Log viewer

The log window keeps the current reading position during automatic and manual refresh. When you scroll to the bottom, new entries remain visible. Scrolling up pauses following without stopping logging. Text selection remains available for copying. Changing the interface language preserves the visible event; reopening the window shows the latest entries.

<a name="download-details-and-checksum-report-37"></a>

## Download details and checksum report

The lower details panel for the selected download keeps its reading position and text selection during repeated automatic refreshes. You can read the end of the checksum report without the panel returning to the beginning. Selecting a different download opens its report at the top. The separate log window retains its existing scroll behavior.

<a name="instructions-and-api-guide-38"></a>

## Instructions and API guide

The **Instructions** and **API guide** buttons are at the upper right, on a separate row below the language selector. They open the guide for the selected language in your default browser. **API guide** jumps directly to the local API section of that guide. The links use the application’s release version; Internet access is required to open the GitHub pages.

<a name="desktop-interface-and-download-history-40"></a>

## Desktop interface and download history

The desktop uses FlatLaf. Choose **Graphite** or **System** in **Appearance settings**; additional styles require the corresponding Pro capability. The appearance choice is saved locally. The queue retains individual progress bars, full-width columns, sorting and filtering. A summary shows the total jobs, active jobs, completed jobs and aggregate speed across all jobs, independently of the current table filter.

Hover over a download row, including an active or completed file, to see its **full file name and absolute destination path**. This also works after sorting or filtering. The selected file heading has the same tooltip. Right-click a row and choose **Copy full path** to copy its destination.

Keyboard shortcuts: **Ctrl+L** (Windows/Linux) or **Cmd+L** (macOS) focuses the download URL; **Ctrl+F** or **Cmd+F** focuses the queue search; **Escape** clears the search. Normal Ctrl+V/Cmd+V paste remains available.

The selected download has **Details** and **History** tabs. The history records dated download events, including the first start, each continuation and successful completion. Old jobs without recorded dates do not receive invented timestamps. See the history storage and API fields below.

The application supports download protocols, selectable gateways, the virtual IP, route speed tests, batch import, source access settings, MD5/SHA-256/SHA-512 checks, the tray, the local API and readable scrolling in the log and details. The interface does not change system routes.

### History storage and API

The data folder contains `logs/downloads-history.jsonl` (UTF-8, one JSON object per line) and `logs/downloads-history.log` (readable dated entries). They are appended across restarts and retained when a job is removed; removing a job or its downloaded files does not erase this audit history. The human log keeps the language used when an event was written; the UI renders stored events in the currently selected language. The log window reads only a bounded tail of up to 1,000 events / 2 MiB; the complete journal stays on disk. It grows with use, so archive it separately when needed. Names and full destination paths are retained; source URLs, passwords and access tokens are not included in these lifecycle events. Review paths before sharing a log.

Native Windows: `%LOCALAPPDATA%\MultiRoute Downloader\data\logs\`. Native macOS: `~/Library/Application Support/MultiRoute Downloader/data/logs/`. Linux/portable profiles use the data folder described above; `--data-dir` selects another folder.

`GET /api/v1/jobs` and an individual job response expose `lifecycle_history`. Each entry has `event_id`, `job_id`, `kind`, `time`, `filename` and `path`; torrent entries may also have `paths`, and pause entries may have `reason`. Kinds are `started`, `resumed`, `paused`, `completed`, `recovered` and `failed`. Timestamps use ISO 8601 UTC (for example `2026-10-18T12:34:56.123Z`); readable logs include a full local date, time and UTC offset. Optional `first_started_at`, `last_resumed_at` and `completed_at` summarize observed events. Queued jobs have no start date until execution actually begins; completion is recorded after verification and publication. `recovered` means a previously published file was verified now, not that its original completion time is known. Existing jobs are not assigned guessed historical dates. All existing API operations remain available; no separate history endpoint is required.

The global **Log** window separates **Application events** from **Download history**. Use the history tab to review dated events across jobs, including removed jobs; the selected download’s History tab shows only that job.

## Saved application profiles

Profiles use the data paths listed above. The selected data folder stores settings, download history and saved parts; keep it and your downloaded files when updating. The protected `MultiRouteResume` network-component directory and internal application identifiers are used by the installed application. Launch it through the MultiRoute Downloader shortcut or `.app`.

<a name="saved-sites-uploads-and-archive-previews-40"></a>

## Saved sites, uploads and archive previews

Open **Site profiles…** to create/edit a profile, then choose it in the site selector on **Downloads** or **Uploads**. Network and virtual-IP controls are on **Network settings**. On **Uploads**, enter the remote **folder URL** and use **Choose files and upload…** to choose one or several local files with the system picker. The UI appends each filename; the API `target_url` below instead names one complete remote file. Paste an archive URL into the main URL field and click **Archive contents…**; the preview lists metadata without starting a download.

Save a site profile with a name, base URL, local download folder, remote folder and selected gateways. Profiles support anonymous access, HTTP Basic/Bearer/Cookie/custom headers, FTP credentials and SFTP credentials with a previously verified SHA-256 host key. Several profiles may use the same server with different accounts. Selecting a profile supplies its folders, gateways and credentials; explicit request settings can override folder/gateway defaults. Relative download or upload URLs are resolved against the profile base URL. Secrets are encrypted in the owner-only `site-profiles/vault.bin` using the local `site-profiles/key.bin`; this protects storage from casual inspection, not from someone controlling your account and its key. Public API responses omit credentials. Blank credential fields retain saved credentials only when the origin and authentication type are unchanged; `replace_credentials:true` deliberately replaces them. A foreign HTTP redirect never receives the profile credentials. Deleting a profile leaves jobs and local/remote files intact; a dependent task retains its original profile binding and cannot switch accounts in place; recreate the task with a valid profile if the original profile was removed.

Uploads and remote directory browsing support **FTP, explicit FTPS and SFTP** through selected application gateways. Select a regular local file and a full remote file URL (`target_url`); a directory URL alone is insufficient. Uploads use a temporary remote file and can continue from a verified matching prefix. Completion requires a full remote SHA-256 readback: the account needs remote **read, write and rename** permissions. Reading the uploaded file again consumes bandwidth. A readback mismatch or denied read permission is an error, not successful completion. Each file uses one gateway at a time; independent files run in parallel and are distributed over the selected gateways. A stopped gateway can be replaced by another selected gateway after the idle limit. This is not parallel splitting of one upload across gateways. Pause/resume and progress are retained across restarts. Removing an upload task preserves both the local file and remote final/partial files.

Overwrite is off by default. Enable it only when replacing the remote destination is intended. Standard FTP/SFTP servers do not universally provide an atomic “rename only if destination is absent”; an external client changing the same target can race the final rename. Coordinate shared remote destinations. Directory browsing does not upload or delete files.

Archive preview lists names, paths, sizes, directory flags and encryption flags for remote **ZIP, TAR, 7Z, RAR4 and RAR5** archives without extraction or execution. It requires a selected gateway, byte-range support and a stable ETag or Last-Modified validator. The metadata budget is 16 MiB, at most 10,000 entries and 60 seconds; 7z header decoding is limited to 64 MiB of memory. The preview can be truncated and is not an integrity check of archive contents. Supply the original filename charset if decoding fails. A password may be used for supported encrypted 7z headers; it is not persisted. Encrypted RAR headers are reported as unsupported/password-required. Split volumes and unsupported/encrypted headers may produce an incomplete listing.

The local API accepts `site_profile_id` for downloads, uploads, remote listings and archive previews. `GET /api/v1/site-profiles` returns `{profiles:[...]}`; `POST` (or `PUT`) saves a flat profile object and returns public metadata. `DELETE /api/v1/site-profiles/{id}` removes only that profile. `POST /api/v1/uploads` accepts `local_path`, `target_url`, `routes`, optional `site_profile_id` and `overwrite:false`; it returns HTTP 201. `GET /api/v1/uploads` returns `{uploads:[...]}`. Individual GET/DELETE and POST `/{id}/pause` / `/{id}/resume` operate on uploads. `POST /api/v1/remote/list` takes `url` and `route`/`routes`; `POST /api/v1/archives/preview` also accepts optional `password` and `charset`. See [openapi.yaml](../openapi.yaml) for typed schemas. Upload manifests are UTF-8 files under `data/uploads/`; they reference a profile ID, not a password. Archive previews do not create a download job.

## Download scheduling and priority

Use the queue/schedule controls to set `max_active_jobs` (1–32), global or gateway speed limits and a daily download window (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm). Times use the computer’s local timezone, also reported as `schedule_timezone`; a window may cross midnight, and equal start/end means all day. Outside the enabled window, downloads return to the queue and continue when allowed. A manually paused job remains paused. In a selected download’s priority/schedule settings, `priority` ranges from -100 to 100: higher queued jobs start first; equal priorities use creation order. Changing priority does not interrupt an already active download. `start_after_ms` is an optional earliest Unix-epoch start time in milliseconds; 0 removes the one-time delay. These settings persist across restarts. They govern the download queue, not the independent upload queue. Keep the application running (the tray is sufficient) and the computer awake; this is not an OS wake-up scheduler. The existing `GET`/`PUT`/`POST /api/v1/queue` and `PUT`/`POST /api/v1/jobs/{id}/options` expose the same controls.

## Chrome and Edge integration

Open the desktop **Chrome / Edge** integration dialog, enable link delivery and open the extension folder included with the native installer. In Chrome open `chrome://extensions`, or in Edge `edge://extensions`; enable Developer mode, choose **Load unpacked** and select that folder. Keep the chosen folder in place.

Keep MultiRoute running, including in the tray. Right-click a link and choose the MultiRoute download action, or use the extension popup to send a URL. The desktop uses the currently selected folder, gateways and site profile; repository/torrent choices still appear when needed. The popup port defaults to 8765; set it to the application’s actual local API port if changed. Only the URL is sent: browser cookies/passwords are not copied and ordinary browser downloads are not intercepted. Configure required source access in the application. If the browser requests permission for the local connection, approve it only for this chosen extension.

The bridge is off by default and accepts only this extension’s fixed origin `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` at `/api/v1/browser/status` and `/api/v1/browser/download`; other local API browser-origin rules remain unchanged. HTTP 202 means the URL reached the desktop flow, not that a file completed. A headless server alone cannot open the required desktop selection flow. Remove the extension on the browser’s extensions page or disable its desktop integration to stop link delivery.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
