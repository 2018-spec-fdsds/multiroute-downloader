# macOS: installation and first launch

<!-- macos-languages:start -->
**English** · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

[Choose another language](README.md) · [Download MultiRoute](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/latest)

1. Download the Apple Silicon or Intel PKG for your Mac. Before updating, choose **Quit** in MultiRoute; closing its window may leave it running in the tray. Open the PKG and follow the installer.
2. If macOS blocks the package or application because the developer is unidentified, first attempt to open it. For the official MultiRoute download, go to **System Settings → Privacy & Security**, find that specific blocked item, select **Open Anyway**, then confirm **Open**. Apply this exception only to the expected package or app, not an alert reporting malware or damaged software.
3. Open **MultiRoute Downloader** from **Applications**. If macOS requests local-network access, allow it. You can review an existing permission under **System Settings → Privacy & Security → Local Network**.
4. In MultiRoute **Network settings**, choose a free application IP or DHCP, then **Check and apply**. If macOS asks at app launch or after applying a network change, confirm its system dialog using your Mac administrator credentials. This starts the protected network component, not a router login.
5. Check gateways, then their speeds. Changing the application IP, DHCP mode or application MAC can restart the component and require authorization again. A healthy running component is reused for ordinary checks.

The virtual IP and MAC belong to MultiRoute. Your OS IP, default gateway and physical adapter MAC are not changed. Global security protections need not be disabled.

Apple: [opening downloaded applications](https://support.apple.com/en-us/102445) · [local-network privacy](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

[MultiRoute Downloader — English](../en.md) · [macOS — English / 12 languages](README.md)
