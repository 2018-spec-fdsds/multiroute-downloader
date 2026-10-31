# macOS：安装、首次启动与权限

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · **中文** · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## 安装并启动

更新前请等待传输结束，并在菜单栏 MultiRoute 菜单选择 **Quit/Exit**。红色关闭按钮可能只隐藏窗口。到[下载页](../../downloads/README.md)选择 Apple Silicon 或 Intel 的 PKG，核对 SHA-256 后打开，在 macOS 管理员系统对话框中确认安装。随后从 Finder 或 Spotlight 启动 `/Applications/MultiRoute Downloader.app`；不需要另装 Java 或使用终端。

## 确认允许打开

如果 macOS 无法验证开发者，先尝试打开 PKG 或应用。仅对已核对、来源可信的文件，进入 **系统设置 → 隐私与安全性 → Open Anyway（仍要打开）→ Open（打开）**；不同版本名称可能不同。若提示恶意软件或文件损坏，请停止并检查文件，不要忽略。[Apple 说明](https://support.apple.com/en-us/102445)。

## 网络权限与密码

首次启动应用或应用虚拟 IP、DHCP、应用 MAC 设置时，网络组件可能需要管理员授权以打开 Ethernet BPF。组件重启或配置变化后可能再次询问。正常的复查和测速复用已准备好的组件，不会按文件或网关索要密码。系统路由与物理网卡 MAC 不变。

若出现“本地网络”请求，请在“隐私与安全性 → 本地网络”允许确认属于 MultiRoute 的项目。并非每次安装都出现此请求。[Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy)。

Mac 管理员密码只输入系统对话框，不是许可证或 Hugging Face 令牌。请求者可能显示为 `DesktopLauncher`/Java；仅确认自己刚发起的可信 MultiRoute 操作。取消后明确重试网络检查并查看日志，不要全局关闭 Gatekeeper/SIP。

[MultiRoute Downloader — 中文](../zh.md) · [macOS — English / 12 languages](README.md)
