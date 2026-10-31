# MultiRoute Downloader 4.3.9 — 中文使用说明

[macOS：安装、首次启动与权限](macos/zh.md)

<!-- product-overview:start -->
[**产品概览**](overview/zh.md)
<!-- product-overview:end -->

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [Home](../README.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

<!-- current437:start -->

<a name="current-free-pro"></a>

## 4.3.9 — 当前功能、Free 与 Pro

本指南介绍当前版本 4.3.9 的安装、网络配置和日常操作。请从[已发布版本](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/latest)获取与系统匹配的安装包，并核对公布的校验和。Free 与 Pro 的功能和上限如下。

### 许可证限制与功能

Free 提供普通单文件下载、受支持的断点续传、全部可用带宽、完整性检查、加密 DNS 和网关实时速度/状态，不会人为限速。各项签名权限相互独立：部分 Pro 许可证仅启用其中列出的权限。

| 功能 | Free | 许可证包含此权限 | ID |
| --- | --- | --- | --- |
| 同时使用的网关 | ≤ 2 | ≤ 8 | `extra_gateways` |
| 同时下载数 | ≤ 3 | ≤ 32 | `extra_concurrent_downloads` |
| 站点配置 | ≤ 3 | 无许可证配额 | `extra_site_profiles` |
| 下载优先级 | 仅普通优先级 | 可选择 | `download_priorities` |
| 网关优先级 | 0 | 可选择 | `gateway_priorities` |
| 网关历史与报告 | 不包含 | 包含 | `gateway_statistics` |
| 批量下载 / 上传 | 不包含 | 包含 | `batch_downloads` |
| 外部应用 API | 不包含 | 包含 | `automation_api` |
| 其他外观主题 | Graphite / System | 全部主题 | `premium_appearance` |

批量操作：打开 **新建下载…**，然后在窗口底部固定的操作栏中选择 **URL 列表…**。

仅启用签名许可证中包含的权限。现有文件和配置会保留。其他文件在队列中等待；网关实时速度和状态仍可查看。存储安全限制仍然适用。

网关上限计算下载和上传共同使用的网关集合，不额外增加系统网关。三个并发任务的上限只针对下载，不针对上传；其他下载在队列中等待。用户设置的更低队列上限仍然有效。已有网关列表、站点配置、进度和优先级不会删除。站点配额只限制新增配置，已有配置仍可使用。历史流量/CSV 需要对应权限，实时仪表仍属于 Free。定时下载仍可用，非默认优先级需要权限。存储和下载源的技术限制仍然适用。

签名许可证绑定安装密钥，不绑定 MAC、IP 或路由器。没有到期日期；4–4 覆盖 4.x。更改网关、DHCP 或应用 MAC 无需重新激活。更换电脑、用户或数据配置可能需要新申请。这不代表代码无法修改或具备通用硬件绑定。

### 应用 MAC

MultiRoute 在手动 IP 和 DHCP 模式下使用独立固定 MAC，不更改系统网卡 MAC。会检查与已观察设备及其他 MultiRoute 实例的冲突；未收到回复并不能证明没有关机设备。 地址池：本地管理的单播 MAC，含 46 个随机位。这不是 DHCP IP 地址池。

**更改 MAC…** — 请在活动和排队传输暂停或完成、且本地校验结束后使用按钮。应用检查已观察到的源地址以及其他 MultiRoute 实例的协作通告。“未发现冲突”不能保证没有关机或静默设备使用该地址。应用 MAC 必须不同于物理网卡 MAC；更改它不会修改网卡或系统路由。

### 校验和…

保存参考值不会重新开始或清除进度。将根据 32、64 或 128 个十六进制字符自动识别 MD5、SHA-256 或 SHA-512，下载完成并合并后校验文件。 保存参考值后选择“立即检查”。校验只读取已完成的本地文件；不匹配时不会重新下载或删除文件。

最终校验使用最近保存的参考值。编辑参考值不会清除已下载字节或重启传输。本地复查保留完成的文件，单独报告匹配、不匹配或错误。发布者的参考值独立保留。一个完整文件校验值不能代表多个 torrent 文件。完整性校验不保证内容无害；安全用途优先采用可信 SHA-256/SHA-512 参考值。

### 4.3.9 的 API 访问

本地 JSON API 使用 UTF-8，无普通 API 令牌。外部管理需要 automation_api，同时还会检查其他权限；仅此权限不能启用批量任务或更多网关。Free 保留有限的只读诊断、私密认证的生命周期操作和可选的浏览器单链接桥接。参阅 [API](../API.ru.txt) 和 [OpenAPI](../openapi.yaml)；不要向局域网或互联网开放端口。


原始应用使用[许可证 1.2](../LICENSE.txt)：Free 可用于个人和组织/商业活动，权利人可提供付费 Pro；其他接收者未经单独许可不得销售。OpenJDK GPLv2 加 Classpath Exception 要求保留声明和对应源代码；JNA 使用 Apache-2.0 并保留声明；独立 JLayer LGPL-2.1 保留替换、调试和源代码权利。参阅[许可说明](../LICENSING.md)、[完整组件目录](../THIRD-PARTY-NOTICES.txt)和应用内许可页面。

System 外观为表格文字、输入框和控件提供与系统背景相适应的对比度；Free 包含 Graphite 和 System。

文件名和大小分别显示在独立列中。大小按数值排序，不受文件名影响；未知大小显示为破折号。提示信息中可查看完整文件名和路径。

将指针悬停在文件名上，可查看完整名称、保存路径、传输协议和来源地址或本地来源路径。即使元数据通过 HTTPS 打开，种子任务也显示 BitTorrent。如有仓库地址，它会显示在实际文件 URL 之后。嵌入的凭据、查询参数值和片段均被隐藏；已识别的公开 magnet infohash 可以保留。保存的下载地址和传输状态不会改变。


### 4.3.9 的网关优先级与传输诊断

在网络设置中，为每个网关设置 **0 到 1000** 的优先级；主界面的网关指示器旁也显示该值。数值越高，小文件或整文件传输越优先使用该网关；大文件也会分配更多并行分段。其他已选且正常的网关仍会参与。相同数值保留选择顺序；首选网关故障或停滞时，其他已选网关可以接手。这不保证固定的速度比例。

此功能需要独立的签名权限 **`gateway_priorities`**。任务优先级权限 **`download_priorities`** 不包含它。没有网关优先级权限时，所有实际优先级均为 0，已保存的值仍会保留。下载过程中可以修改，不会清除已验证部分。每个选定网关使用一到四个并行分段工作线程，每项任务合计最多 32 个。

设置中可以选择许可证范围内的同时下载数量，以及有限或无限的自动重试次数。手动暂停的任务仍保持暂停。右键菜单可暂停或继续所选的适用任务，也可复制文件名。双击只打开文件夹；属性、统计信息和单独下载日志通过右键菜单打开。

已完成文件仍可查看单独日志。设置中的“错误与解决方案”提供离线搜索指南，含 25 个主题，覆盖全部 12 种界面语言。保存位置和文件属性显示可用空间（GB）。本地磁盘已满会明确提示，保留已保存部分，不会作为网络错误无限重试。恢复小分段后等待新数据时，已保存前缀仍计入进度。

### 剩余下载数据 · 4.3.9

下载列表新增独立的 **剩余下载量** 列。它显示尚需下载的字节数，与预计剩余时间分开，并支持按数值排序。该值包含已保存和正在接收的进度，不会小于零；总大小未知时显示短横线。

### 更可靠的 Mac 启动和网关设置 · 4.3.9

Mac 原生启动可识别旧版相对默认路径，包括缺少开头斜杠的情况，并使用已验证的现有配置，或初始化用户拥有的空默认目录。存在歧义的自定义位置仍需用户明确选择，不会盲目猜测。

网关准备失败或被取消后，其余网关和后台检查不会反复申请授权，直到用户明确重试网络设置。首次以特权启动组件时，macOS 仍可能要求正常的管理员确认，以打开以太网 BPF 设备。

Mac 网络组件 4.3.2 可处理被 macOS 隐藏的网卡 MAC，同时保留保护物理网卡的检查。应用继续使用自己的 MAC，不修改系统路由或物理网卡 MAC。Mac 图标和应用名称采用与 Windows 一致的 MultiRoute 标识。运行 PKG 更新前请退出应用。真实系统授权窗口中的调用者名称和实际 LAN/DHCP 传输仍需硬件确认；此传输方式仍要求有线以太网。

**macOS:** `/Applications/MultiRoute Downloader.app` · `/Library/PrivilegedHelperTools/MultiRouteResume/4.3.2/{gateway-net,gateway-launch}`.

### Mac 手动地址的网关回复 · 4.3.9

修正 POSIX 捕获设置，让手动应用地址与 DHCP 都能接收发往应用自身 MAC 的回复，与现有 Windows 行为一致。系统 ping 成功只证明电脑自身的连接正常，不能证明应用虚拟地址能收到网关回复。

DHCP 诊断保留十二种语言的具体说明，不再被通用错误替代。现有授权行为不变：普通网络检查复用已准备好的组件。API 接口、界面控件、系统路由和物理网卡 MAC 均不改变。首次以特权访问 Ethernet BPF 仍可能需要正常的管理员确认。

这修正了一种捕获模式差异，并不涵盖网关不可达的所有原因。地址空闲、子网以及 DHCP 租约和冲突检查仍然有效。Mac 手动地址传输仍需实际硬件验证；仍要求有线以太网。

<!-- current437:end -->

<a name="appearance-and-sounds"></a>

<a name="424-的外观与完成提示音"></a>

## 外观与完成提示音

打开**外观设置**，可选择 **StreamsStudio**、**Graphite**、**Nordic**、**Neon**、**Community**、**Chat**、**Studio**、**Rose** 或 **System**。Free 包含 Graphite 和 System；其他样式需要对应的 Pro 外观权限。选择保存在本机，不会重启下载。**圆角窗口和按钮**是独立选项，各种样式的控件均可使用圆角；窗口外框由操作系统决定。Windows 11 的最大化或贴靠窗口可能仍为直角，macOS 和 Linux 也由系统控制外框。速度图提供宽裕的绘图区域，网络设置使用紧凑的网关卡片。

完成提示音**默认关闭**。先勾选总开关，再分别选择**每个文件完成**和**所有下载完成**时的通知。提供**柔和铃声**、**钢琴**和**马林巴**三种音色，可分别试听文件完成和全部完成的旋律。所有下载都完成且已启用全部完成通知时，最后一个文件只播放最终旋律，不叠加第二段提示音。暂停或失败的任务不算完成。启动应用或更改设置时不会重新播放旧记录的提示音。能否听到声音还取决于系统音量和音频输出设备。

如需使用自己的音频，请选择**自定义旋律**，分别为文件完成和全部下载完成选择 **MP3 或 WAV** 文件。每段音频必须**不超过 10 秒**；应用会检查解码后的实际时长，而非仅相信扩展名或元数据。通过检查的音频会复制到本机应用配置中，此后可以移动原文件。

在外观设置中可选择写实的成年 **MultiRoute 快递员**，男性或女性，穿合身的夏季服装。快递员显示在独立的桌面小组件中。袋子的标签和大小反映**所有活动下载的剩余数据总量**；无论使用多少网关，每个文件只计算一次。随着字节不断接收，袋子会逐渐缩小，负重状态由剩余数据量决定：**小于 1 GB** 时扛在肩上，**1 GB 至不足 10 GB** 时在地面拖行，**10 GB 及以上** 时更加吃力。行走或奔跑的速度另由实际总下载速度决定；空闲时站立。新文件完成后，快递员走到该桌面小组件的右侧边缘，放下袋子并平滑淡出；如果仍有活动下载，就带着新货物再次出现。送货时，袋子上的标签显示本次送达货物的数据量；返回后，则再次显示所有活动下载的剩余数据总量。这只是动画，真实文件仍保存在所选文件夹中。显示状态会保存；隐藏或空闲时停止动画，启动时不重播历史完成事件。送货动画独立于声音设置。这些原创生成角色与 Codex 或 OpenAI 无关联。

外观、圆角和声音属于本机桌面设置，不会增加下载 API 端点，也不改变传输、校验和或 DNS 安全设置。

<a name="startup-profile-location"></a>

<a name="423-启动配置目录修复"></a>

## 启动时的数据目录

原生应用以绝对路径保存数据目录，不依赖登录时的工作目录。保存的相对配置路径只有在能唯一确认所指的现有配置时，才会转换为绝对路径。下载历史、设置和已保存的分段保留在原处；便携版命令行的相对路径和显式 `--data-dir` 参数保持各自的指定行为。

<a name="automatic-startup"></a>

<a name="421-自动启动"></a>

## 自动启动

在语言和外观选项旁启用**随操作系统启动**。此选项**默认关闭**。启用后会为当前账户的下次登录注册 MultiRoute；关闭后会删除应用自己的启动项。这是用户登录时启动，不是系统服务，也不会在登录前启动。创建启动项不需要管理员权限。

自动启动时会在托盘可用时使用托盘，否则显示可正常访问的窗口。队列的优先级、暂停和计划保持不变，不会强制恢复所有暂停的下载。请先手动打开应用，完成安装和网络组件配置。请保留已注册的应用路径；便携使用时也应保留 Java 和库的位置。移动后请在新位置关闭并重新启用自动启动。卸载前请关闭自动启动。

操作系统或桌面策略可能禁用已注册的启动项。macOS 请求许可时，请检查**系统设置 → 通用 → 登录项与扩展**；网络组件授权是独立步骤。存在启动项不等于未来登录时一定能成功启动。自有测试配置的检查不包括真实重启或注销；请查看相应版本报告。

如果无法打开应用，请**只删除其自己的启动项**：

- **Windows：**应用在 `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run` 中保存自己名为 **MultiRoute Downloader** 的 `REG_SZ` 值。请优先在应用中关闭自动启动。如果无法打开应用，请在注册表编辑器中**只删除此值**，保留 Run 项及其他条目。如果被系统禁用，请使用**设置 → 应用 → 启动**；应用不会覆盖您的决定。Windows 将完整启动命令限制为 260 个字符；超过时请改用更短的应用或配置目录路径，然后重新注册。
- **macOS：**删除 `~/Library/LaunchAgents/io.github.2018-spec-fdsds.multiroute-downloader.plist`。
- **Linux：**从实际的 XDG 配置目录删除 `${XDG_CONFIG_HOME:-~/.config}/autostart/io.github.2018-spec-fdsds.multiroute-downloader.desktop`。需要支持 XDG 自动启动的图形桌面；此设置不会安装 systemd 服务。

<a name="dns-settings"></a>

<a name="422-的-dns-设置"></a>

## DNS 设置

**请核对 4.3.9 安装包版本和公布的校验和；平台及安装文件验证结果见对应的 NATIVE-INSTALLER-TESTS.json。**

打开**网络 → DNS 解析**。默认是 **DNS over HTTPS（DoH），Cloudflare**。此设置仅属于 MultiRoute，不修改系统 DNS、路由或网卡 IP。

DoH 支持 TLS ALPN 选择的有界 HTTP/2，以及协商为 HTTP/1.1 时的传输。服务商兼容性和真实网关结果以对应最终发行报告为准。

- **DoH：**经每个选定网关发送加密 HTTPS DNS；按 URL 主机名验证 TLS 证书。
- **DoT：**经每个选定网关发送 TLS DNS，默认端口 853；按服务器名称验证证书。
- **系统 DNS（计算机）：**明确采用计算机、操作系统或 VPN 的解析策略。DNS 不保证经过各个选定网关，文件连接仍使用其网关。此模式本身不保证 DNS 加密。

可选 Cloudflare、Quad9、Google 或**自定义**。预设值固定；自定义可编辑相应 URL/服务器名、端口和公网 IPv4 引导地址。数字地址让程序直接连接 DNS 服务，无需先通过系统解析服务商名称。DoH/DoT 验证受信证书链、主机名及 SNI，绝不会静默回退到明文或系统 DNS。安全查询失败会结束该次尝试；服务商必须能通过所选网关访问。

修改草稿，选择网关，再点击**测试 DNS**。测试不保存配置，逐网关显示结果、耗时、公网 IPv4 或错误；部分失败仍会显示。系统模式成功只说明系统能解析，不证明每个网关各自的 WAN 可用。点击**应用 DNS 设置**验证并保存；返回的修订号表示生效配置。后台状态更新及切换语言会保留未保存草稿、光标和滚动位置。

每次查询总时限为 8 秒，包括 DNSSEC 辅助查询。已验证 TTL 也受签名过期时间限制；原生缓存最多 300 秒，并按网关和配置修订号隔离。零 TTL 和 DoH HTTP Age 仍然生效。系统模式使用 OS/JVM 缓存策略。文件目标支持公共 IPv4，不提供 IPv6 解析。

API：`GET /api/v1/network/dns` 读取配置和只读状态；同路径 `PUT` 或 `POST` 应用配置。`POST /api/v1/network/dns/test` 测试草稿；`GET /api/v1/network/dns/providers` 列出预设。仅发送配置字段，不要把 GET 的修订号/状态写回。[API](../API.ru.txt) · [OpenAPI](../openapi.yaml)。

<a name="dnssec-validation"></a>

<a name="422-本地-dnssec-验证"></a>

## 本地 DNSSEC 验证

在 **网络 → DNS 解析 → DNSSEC 验证** 中选择 **验证已签名域名**（DoH/DoT 默认）、**要求安全的 DNSSEC 应答** 或 **DNSSEC 已关闭**。Java 使用 dnsjava 和内置信任根在本地验证。仅有上游 AD 标志不能作为证明。DoH/DoT 加密传输，DNSSEC 独立验证 DNS 数据的真实性。

**验证**允许安全及保护未确认的应答；**要求**只接受安全应答。保护未确认可能表示已认证的未签名委派或不支持的算法，请查看原因。启用验证时，无效和无法判定的应答被拒绝。系统 DNS 会关闭本地验证；切回 DoH/DoT 恢复上次策略。切换提供商预设保留策略。应用会保存；测试 DNS 使用未保存的草稿。

每次查询区分 `secure`、`insecure`、`bogus`、`indeterminate`、`not_checked`；缺少结果显示为不可用。连接成功不代表 DNSSEC 安全。签名过期、缺少密钥或证明、查询失败、超时具有独立原因。请保持系统时间正确。辅助验证查询使用同一网关和总时限。DNSSEC 不隐藏名称，也不检查下载文件内容。

API 的 `dnssec` 接受 `off`、`validate`、`require`；系统模式仅接受 `off`。`dnssec_validation`、`dnssec_trust_anchors` 以及每次查询的 `dnssec_status`、`dnssec_reason_code`、`dnssec_reason`、`dnssec_validated_ttl`、`dnssec_aux_queries` 为只读，保存时不要回传。参见上方 API/OpenAPI 和 [DNSSEC](#422-本地-dnssec-验证)。

**分段下载与 CDN：** 对于 Civitai 等 CDN，若从零开始的字节范围请求返回 HTTP 200，会再检查非零起点的范围。首段读取限制在该段长度内；身份匹配的已有顺序下载数据可转换为分段续传。非零范围临时返回 HTTP 200 时，保留已存数据及网关工作线程，等待 2–30 秒后重试。持久化的分网关传输日志区分接收流量与已保存进度。[传输诊断](transfer-diagnostics.en.md)。

[4.3.9 安装包](../downloads/v4.3.9/README.md)包含独立 Java 21。Windows 安装向导使用英语并创建桌面快捷方式；Npcap 仍需单独安装。macOS 使用系统 libpcap。请遵循下方的完整安装与安全确认步骤。

项目所有者：[2018-spec-fdsds](https://github.com/2018-spec-fdsds)。

<a name="1-用途与工作方式"></a>

## 用途与工作方式

MultiRoute Downloader 是 Java 桌面下载管理器，可通过同一 Ethernet 局域网中的多个互联网网关并行下载文件。例如，网关可以分别连接有线宽带和移动 4G/5G 网络。所有选中的可用网关从共享队列领取文件片段；速度更快的网关可以领取更多片段。一个连接中断时，其他网关继续下载，未完成的片段重新进入队列，已经校验的数据保留。也可以批量导入链接，或由其他程序通过本地 API 创建任务。

速度提升取决于各出口是否具有独立带宽、服务器限速和协议能力，并非网关数量的固定倍数。本程序是独立桌面应用，不需要浏览器来操作。新配置默认使用英语；可选择英语、俄语、德语、西班牙语、法语、中文、韩语、阿拉伯语、意大利语、日语、印地语和越南语。语言设置会保存，已注册的状态、日志和提示随语言切换；服务器或外部库的原始消息可能保留原文。文件名、路径、URL 和 JSON 字段不会翻译。

<a name="application-virtual-ip"></a>

### 应用为什么使用虚拟 IP

**为 MultiRoute 选择网关，同时保留其他应用原有的网络连接。** 应用使用一个未被占用的专用本地 IPv4 地址进行内部网络传输，因此可以通过选定网关建立连接，不必依赖电脑的默认网关，也不会更改系统网卡地址、路由表或系统默认网关。浏览器和其他程序继续遵循原有网络设置。

例如，在 `192.168.1.0/24` 局域网中，电脑保留自己的正常地址 `192.168.1.x`，MultiRoute 可以使用空闲的 `192.168.1.70`，下载则可以通过 `192.168.1.1`–`192.168.1.4`。电脑、虚拟地址和选定网关必须处于同一个实际 IPv4 子网。请在应用的网络设置中检查并应用空闲地址，不要手动把它添加到系统网卡。虚拟 IP 不会创建额外的互联网连接；带宽来自路由器及其上游线路。

#### 如何选择并检查地址

1. 查看电脑 Ethernet 网卡的 IPv4 地址和子网掩码，选择同一子网中的有效主机地址。`192.168.1.70` 只是示例，不能使用电脑本身、任何网关、网络或广播地址。
2. 打开路由器管理页面，查看实际 DHCP 地址池。选择自动分配范围外的地址，或在 DHCP 服务器上将其保留/排除。如果同一 LAN 有多个 DHCP 服务器，必须确保每个服务器都不会把该地址分配给其他设备。
3. 在 MultiRoute 中对虚拟地址选择“检查并应用”，然后检查网关。应用会验证子网并通过 ARP 检查冲突。没有 ARP 回复只表示当时未检测到冲突，并不证明地址位于 DHCP 池之外，也不能阻止 DHCP 随后分配该地址。

## 所需环境

Windows/macOS 安装包包含独立 Java 21 和已编译的网络组件。Windows 另需 Npcap 驱动，无需另行安装 Java。

| 平台 | 操作系统 | 组件 |
| --- | --- | --- |
| Windows x64 | Windows 11 | Java 21 |
| Apple Silicon | macOS 26+ | Java 21 |
| Intel Mac | macOS 15+ | Java 21 |

需要有线 Ethernet 网卡，与所选网关处于同一 IPv4 子网；还需要未占用的虚拟 IPv4、可写的数据和下载目录，以及文件和临时分块所需空间。提速取决于独立 WAN 容量和来源服务器，无需 GPU。

应用支持 Linux，但此发行仓库没有发布 Linux 4.x 安装包。请勿在 Linux 中使用 Windows 或 macOS 安装包。

更新前，请等待正在进行的下载、上传和最终文件校验完成，然后从托盘退出并运行完整的新安装包。保留原数据目录、已下载文件和保存的分块。

<a name="3-安装与安全确认"></a>

## 安装与安全确认

从 [4.3.9 发布页](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9) 选择与系统匹配的单文件安装包，并与公布的 SHA-256 比较：

- Windows：[MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe)。
- Apple Silicon，macOS 26+：[MultiRoute-Downloader-4.3.9-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg)。
- Intel，macOS 15+：[MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg)。

### Windows

打开 EXE，按向导完成当前用户安装，再使用开始菜单或桌面快捷方式启动。程序不以命令脚本窗口运行。安装向导文字为英语；系统安全对话框遵循 Windows 语言。

首次启动会检查 Npcap。缺少时，程序征得同意后从官方来源下载固定版本，校验 SHA-256 和 Authenticode 发布者，再打开 Npcap 的普通安装程序。阅读许可证并批准正常的 UAC 请求；如果要求重启，重启后再打开应用。Npcap 不随本程序打包，也不会静默安装。此步骤需要联网。AdminOnly 驱动可能再次要求捕获授权；Java 窗口仍应作为普通用户运行。

本应用和安装包没有发布者签名。只有在确认官方来源和校验和后，才自行决定是否使用 SmartScreen 提供的“更多信息 → 仍要运行”。病毒检测与“未知发布者”不同；不要关闭杀毒软件或为安装设置排除项。

### macOS

选择与原生处理器匹配的 PKG，在 Apple Installer 中安装并批准放置受保护网络组件的管理员请求。然后从 Finder、Spotlight 或 Dock 打开 `/Applications/MultiRoute Downloader.app`。此安装包不需要 Terminal、Command Line Tools、Homebrew 或单独 Java。

应用需要网络组件打开 Ethernet BPF 设备时，会显示正常的 macOS 管理员授权；组件打开捕获设备后降低权限，Java 以普通用户运行。程序和 PKG **没有 Developer ID 签名，也未公证**。如系统提示无法验证开发者，且提供例外入口，请先核对来源和 SHA-256，再按 [Apple 官方说明](https://support.apple.com/en-us/102445) 使用“系统设置 → 隐私与安全性 → 仍要打开”，然后确认“打开”。受管理设备可能禁止例外。

安装授权、网络组件授权和未验证开发者例外是不同确认。系统询问时允许访问“本地网络”，以及您实际选择的下载文件夹。不要关闭 Gatekeeper/SIP、整体移除隔离标记或把全部 Java 作为 root 运行。“恶意软件/文件损坏”警告不是普通开发者提示，应停止并检查文件。

<a name="4-配置网关与虚拟地址"></a>

## 配置网关与虚拟地址

预置网关地址为 `192.168.1.1`、`192.168.1.2`、`192.168.1.3`、`192.168.1.4`；请根据实际网络编辑，并选择可达的网关。可通过添加、编辑或删除来管理 **1–8** 个不重复的网关配置。Free 同时最多使用 **2 个网关**；包含 `extra_gateways` 权限的 Pro 同时最多使用 **8 个网关**。四个预置地址不代表四个网关都会启用。电脑使用其中一个作为默认出口，并不意味着还存在第五个网关。

应用虚拟 IPv4 默认 `192.168.1.70`。在网络设置中检查并应用，确认该地址未被设备占用，并在 DHCP 自动分配范围之外保留。它只用于程序内部传输，**不会加到系统网卡，也不会修改系统路由表**；其他应用继续使用原连接。不要在 Windows/macOS/Linux 网卡设置中手动添加此地址。旁边的问号和悬浮提示解释用途。

检查各网关后选择一个、多个或全部可用网关；实际同时使用的数量受上述许可上限限制。速度测试最多传输 **16 MiB 或 5 秒**，建立连接可能额外耗时；这只是测试限制，正常下载不受此限制。没有合适的直接 HTTP 链接时使用 Selectel 测试文件。

下载中可以增删网关配置或调整任务的已选网关，但每个未完成任务至少保留一个有效网关。新增配置不会自动加入已有任务，需在该任务中选择。更改虚拟 IP 需要先暂停活动任务。应用 MAC 的用途、检查与更改条件见上方“应用 MAC”说明。

<a name="5-下载续传与桌面界面"></a>

## 下载、续传与桌面界面

1. 在长 URL 输入框中按 Ctrl+V（macOS 为 Cmd+V）粘贴，或使用粘贴按钮。可粘贴 magnet 或打开 `.torrent` 文件。
2. 用系统文件夹选择器指定保存位置。默认 Windows 为 `%USERPROFILE%\Downloads`，macOS/Linux 为 `~/Downloads`；手动指定的目录会保留。
3. 选择网关，点击 URL 旁的开始下载。Hugging Face 仓库、WebDAV 列表和 Metalink 可预览并选择文件；批量导入支持每行一个链接和文本文件。
4. 观察每个任务的进度条、已用/剩余时间、总速度和各网关速度。共享队列的 HTTP 片段默认 **20 MiB**，可设为 1–64 MiB。某片段 **60 秒**没有收到数据时取消该传输并回到共享池，由其他网关领取。torrent 片段大小来自其元数据。
5. 可暂停/继续、调整优先级、限速和计划；默认同时活动任务数为 2。重启后原活动任务恢复为暂停，按需继续。已停止任务可更改文件夹。torrent 做种默认关闭，可设置时间和分享率上限。

界面使用 FlatLaf，支持保存的浅色、深色和系统主题。顶部汇总所有/活动/已完成任务和全部任务的合计速度，不受表格筛选影响。每个任务有独立进度条；列表和窄窗口可滚动，刷新保留位置，支持排序和筛选。

悬停正在下载或已完成的行，可见**完整文件名和绝对保存路径**；排序/筛选后也对应正确文件。所选文件标题有相同提示。右键菜单可复制完整路径。Ctrl+L/Cmd+L 定位 URL，Ctrl+F/Cmd+F 定位搜索，Escape 清空搜索。

所选任务有“详细信息”和“历史”页签。报告刷新或切换语言时保留阅读位置和文本选择；选择另一任务时从其报告开头显示。全局日志分为应用事件与下载历史；滚动到末尾时跟随新条目，向上阅读时不强制跳回。

关闭主窗口会继续在托盘下载，图标显示全部任务总速度，可显示到 10 Gbit/s；悬浮提示更精确。用托盘“打开应用”恢复窗口，“退出应用”才真正保存并结束。无托盘支持时最小化到任务栏。升级前请从托盘退出。

在 Windows/Linux 中按住 **Ctrl** 单击、在 macOS 中按住 **Cmd** 单击，可选择多个下载任务；按住 **Shift** 单击可选择连续范围。焦点位于表格时，**Ctrl+A** 或 **Cmd+A** 仅选择当前搜索和状态筛选后可见的所有行。对所选任务执行删除时，整组只显示一次确认。“同时删除已完成文件和已下载片段”默认**不勾选**，因此可只移除列表记录并保留数据。仅在确实需要删除这些任务的文件时勾选；其他文件和保存文件夹保持不变。完成后会显示已删除记录数。删除失败的任务仍保留在列表中，并显示错误摘要，便于检查后重试；已成功删除的任务不会重复处理。删除任务或文件不会清空持久下载历史。

<a name="6-协议访问与校验"></a>

## 协议、访问与校验

| 来源 | 支持与限制 |
| --- | --- |
| HTTP/HTTPS | 支持安全文件身份和字节范围时分片续传；否则单网关整文件流，中断后从零重启 |
| Hugging Face | 文件/仓库链接、文件选择及 Hub/Xet 解析；私有或受限资源需要获授权的来源凭据 |
| FTP | 被动 EPSV/PASV；明文 FTP 不加密凭据或数据 |
| FTPS | 显式 TLS 加密控制和数据；不支持隐式 FTPS |
| SFTP | SSH；必须先独立确认并固定 SHA256 服务器主机密钥指纹，未知密钥不自动接受 |
| WebDAV | HTTPS 目录与文件选择 |
| Metalink | `.meta4`、可信 SHA-256、镜像以及元数据提供的分片哈希 |
| Torrent/magnet | 通过已选网关传输，强制按元数据验证片段，支持文件选择与优先级 |

来源访问设置可保存用户名/密码、Bearer token、Cookie 或允许的标头，按准确 origin 限定并在本地加密。网关不需要这些 token；不要在 URL 写密码。没有可信已发布或手动提供的 SHA-256/SHA-512 时，FTP/FTPS/SFTP 使用整文件方式。

组装后一次读取计算 **MD5、SHA-256、SHA-512**。主校验输入按 32/64/128 个十六进制字符自动识别，可展开输入更多参考值。会查询受支持的整文件摘要标头，以及 `filename.md5`、`MD5SUMS`、`filename.sha256`、`SHA256SUMS`、`filename.sha512`、`SHA512SUMS`；保留 Hugging Face LFS 和 Metalink 校验。部分 HTTP 响应摘要不是整文件校验和。

发布完成文件前，所有已知参考值都必须匹配；不匹配时任务失败并保留部分数据。没有来源参考值时，显示的是本地计算值，不表示与发布者确认匹配。MD5 用于偶发损坏检测，不代替 SHA-256/SHA-512 或可信文件身份。torrent 片段校验与完整文件哈希相互独立；手动整文件哈希要求只选择一个文件。

<a name="multiroute-api"></a>

<a name="7-其他应用使用本地-api"></a>

## 其他应用使用本地 API

启动桌面应用后，API 位于 `http://127.0.0.1:8765/api/v1`。请求与响应为 JSON、UTF-8；写请求使用 `Content-Type: application/json`。本地创建/管理任务不需要 API token，来源凭据另行配置。只接受本机连接，不要把端口开放到局域网或互联网。支持 `--headless` 仅运行 API；原生应用应使用正常启动器，`.cmd` 示例只用于 portable 发行包。

以下外部管理示例需要 `automation_api` 权限。示例中的四网关配置还需要 `extra_gateways` 权限；请按实际网络和许可调整 `gateways` 与 `routes`。Free 最多同时使用 2 个网关，具备对应 Pro 权限时最多使用 8 个。

健康检查：`GET /api/v1/health`，正常响应含 `{"ok":true,"version":"4.3.9"}`。先在界面配置网络，或向 `POST /api/v1/network` 发送：

```json
{
  "virtual_ip": "192.168.1.70",
  "gateways": ["192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"]
}
```

向 `POST /api/v1/jobs` 发送以下 JSON；替换 URL 和本机真实目录。Linux 可用 `/home/your-user/Downloads`，macOS 可用 `/Users/your-user/Downloads`。API 的 `routes` 必须明确指定，不会自动复制窗口中的复选框。

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

`HTTP 201` 只表示创建成功，不代表下载完成。保存响应的 `id`，轮询 `GET /api/v1/jobs/{id}`，直到 `status` 为 `complete`；失败时读取 `error`。`speed_bps` 的单位是**字节/秒**，乘 8 再除 1,000,000 得到 Mbit/s。

| HTTP | API |
| --- | --- |
| GET | `/api/v1/health` |
| GET / POST | `/api/v1/jobs` |
| GET | `/api/v1/jobs/{id}` |
| POST | `/api/v1/jobs/{id}/pause` |
| POST | `/api/v1/jobs/{id}/resume` |
| POST | `/api/v1/jobs/{id}/routes` |
| DELETE | `/api/v1/jobs/{id}` |
| GET / POST | `/api/v1/network` |
| POST | `/api/v1/jobs/batch` |
| GET / POST | `/api/v1/queue` |
| GET | `/api/v1/events` |

暂停、继续使用空 JSON 对象 `{}`；删除行为及请求体按 [OpenAPI](../openapi.yaml) 选择，默认保留文件。`Idempotency-Key` 可避免重复创建；队列、批量导入、SSE 事件、来源访问和限速的完整字段以该文档为准。`expected_md5`、`expected_sha256`、`expected_sha512` 可随创建请求传入；结果包含 `md5`、`sha256`、`sha512`、`checksum_sources` 和警告。JSON 字段名和状态值不随界面语言变化。[详细 API 示例](../API.ru.txt) 为俄语。

Python 3 示例（先替换示例 URL，在界面完成网络配置并保持应用运行；UTF-8 编码也适用于中文路径）：

```python
import json
import urllib.request
from pathlib import Path

data = {
    "url": "https://example.org/large-file.bin",
    "directory": str(Path.home() / "Downloads"),
    "chunk_mb": 20,
    "routes": [
        {"type": "gateway", "gateway": f"192.168.1.{n}", "enabled": True}
        for n in range(1, 5)
    ],
}
request = urllib.request.Request(
    "http://127.0.0.1:8765/api/v1/jobs",
    data=json.dumps(data, ensure_ascii=False).encode("utf-8"),
    headers={"Content-Type": "application/json; charset=utf-8"},
    method="POST",
)
with urllib.request.urlopen(request, timeout=30) as response:
    job = json.loads(response.read().decode("utf-8"))
print(job["id"])
```

<a name="8-持久历史数据与更新"></a>

## 持久历史、数据与更新

| 系统 | 数据目录 | 应用与运行时 |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

`logs/downloads-history.jsonl` 是 UTF-8 逐行 JSON，`logs/downloads-history.log` 是带日期时间的可读日志。它们跨重启追加，不因任务删除或切换语言重写。文本日志保留写入时语言，界面按当前语言显示历史。查看窗口最多读取最近 1,000 个事件/2 MiB，完整日志保存在磁盘并持续增长，可另行归档。日志保留文件名和绝对路径，不写来源 URL、密码或 token；分享前检查个人路径。

API 的 `lifecycle_history` 事件含 `event_id`、`job_id`、`kind`、`time`、`filename`、`path`；torrent 可含 `paths`，暂停可含 `reason`（`pause`、`shutdown`、`maintenance`）。事件为 `started`、`resumed`、`paused`、`completed`、`recovered`、`failed`，时间是 ISO 8601 UTC（如 `2026-10-18T12:34:56.123Z`）；可读日志含本地完整日期、时间和 UTC 偏移。可选 `first_started_at`、`last_resumed_at`、`completed_at` 概括实际观察到的开始、最近继续和校验发布后的完成。排队还不算开始；旧任务不补造日期，`recovered` 是现在确认旧文件完整，并非知道旧完成时间。

升级前从托盘退出，再安装完整新包，保留数据和部分文件。Windows 通过“设置 → 应用 → 已安装的应用”卸载；macOS 删除 `.app` 不会一并删除历史或受保护组件。不要为了卸载而删下载目录。Linux 移动应用目录后重新部署。

<a name="10-许可证"></a>

## 许可证

原版应用可免费用于个人和组织，包括商业活动；未经单独许可，不得销售应用、收取许可证费用或有偿分发。此自定义许可不一般性授权修改/再分发，也不是 OSI 认可的开源许可证。以英文 [LICENSE.txt](../LICENSE.txt) 为准；FlatLaf（Apache-2.0）及其他第三方组件保留其自身许可。项目所有者：[2018-spec-fdsds](https://github.com/2018-spec-fdsds)。

<a name="名称与旧配置兼容"></a>

## 数据文件夹与现有配置

数据保存在上表所示的用户目录中。如果标准数据目录不存在，而应用检测到可用的现有配置，就会直接复用该配置，不移动或删除文件。现有配置可能位于 `%LOCALAPPDATA%\MultiRoute Resume\data` 或 `~/Library/Application Support/MultiRoute Resume/data`；请保留实际使用的数据目录。受保护的 `MultiRouteResume` 目录、Windows 升级标识、内部 API 身份和端口保持兼容。请通过已安装的 MultiRoute Downloader 快捷方式或应用启动。

<a name="已保存站点上传和压缩包预览40"></a>

## 已保存站点、上传和压缩包预览

打开 **网站配置…** 新建或编辑配置，再在 **下载** 或 **上传** 的站点列表选择它。网关和虚拟 IP 位于 **网络设置**。在 **上传** 输入**远程目录 URL**，按 **选择文件并上传…** 通过系统选择器选择一个或多个本地文件；界面自动附加文件名，而 API 的 `target_url` 必须指向完整远程文件。预览时将压缩包 URL 粘贴到主 URL 栏并按 **压缩包内容…**，不会开始下载。

站点配置保存名称、基础 URL、本地下载目录、远程目录及网关。支持匿名访问、HTTP Basic/Bearer/Cookie/自定义请求头、FTP，以及带有事先核实的服务器 SHA-256 指纹的 SFTP。同一服务器的多个账户可以分别保存，凭据相互隔离。明确指定的目录和网关优先；相对下载/上传 URL 以基础 URL 为准。凭据在仅限所有者访问的 `site-profiles/vault.bin` 中加密，本地密钥是 `site-profiles/key.bin`；控制您的账户和密钥的人仍可读取。公共 API 不返回凭据。只有来源和认证类型不变时，空字段才保留旧凭据；`replace_credentials:true` 明确替换。HTTP 跳转到其他来源不会携带该配置的凭据。删除配置不会删除任务或本地/远程文件；任务保留原配置绑定，不能直接切换账户；若已删除原配置，请按需使用有效配置重新创建任务。

上传和远程目录浏览支持通过所选网关的 **FTP、显式 FTPS 和 SFTP**。选择普通本地文件和完整远程文件 URL（`target_url`），不能仅填写目录。上传使用远程临时文件，比较现有前缀后才能续传。成功必须完整读回远程文件并核对 SHA-256，因此账户需要**读取、写入和重命名**权限；读回会再次消耗带宽。校验不符或无法读取都属于失败。单个文件一次使用一个网关，多个文件可分配到不同网关并行上传；闲置超时后可切换网关。不会将同一上传文件拆分到多个网关。暂停、继续和进度可跨重启保存。删除上传任务保留本地文件以及远程成品和临时文件。

默认不覆盖。FTP/SFTP 并非总能保证“仅目标不存在时原子重命名”，其他客户端可能在发布时同时修改目标；共享远程路径应协调使用。浏览目录不会上传或删除文件。

预览可列出远程 **ZIP、TAR、7Z、RAR4 和 RAR5** 的名称、路径、大小、目录和加密标志，不解压、不执行。需要选择网关、支持字节范围，并具有稳定的 ETag 或 Last-Modified。元数据上限为 16 MiB、10,000 条、60 秒；7z 头解码内存上限 64 MiB。列表可能截断，不代表内容完整性校验。文件名错误时可指定原始字符集。支持的加密 7z 头可提供不保存的密码；加密 RAR 头会标为不支持/需要密码。分卷等情况可能只能部分列出。

API 在下载、上传、目录浏览和预览中接受 `site_profile_id`。`GET /api/v1/site-profiles` 返回 `{profiles:[...]}`；`POST` 或 `PUT` 保存扁平配置并返回公共字段；`DELETE /api/v1/site-profiles/{id}` 仅删除配置。`POST /api/v1/uploads` 接受 `local_path`、`target_url`、`routes`、可选 `site_profile_id`、`overwrite:false`，返回 HTTP 201。`GET /api/v1/uploads` 返回 `{uploads:[...]}`；单项支持 GET/DELETE 和 POST `/{id}/pause`、`/{id}/resume`。`POST /api/v1/remote/list` 使用 `url` 与 `route`/`routes`；`POST /api/v1/archives/preview` 另接受可选 `password` 和 `charset`。详见 [openapi.yaml](../openapi.yaml)。`data/uploads/` 中 UTF-8 清单只引用配置 ID，不保存密码。预览不创建下载任务。

## 下载计划与优先级

队列/计划设置支持 `max_active_jobs`（1–32）、总/网关限速和每日窗口（`schedule_enabled`、`schedule_start`、`schedule_end`，HH:mm）。使用电脑本地时区，API 的 `schedule_timezone` 会报告它；可跨午夜，起止相同表示全天。窗口外下载回到队列，手动暂停的任务仍暂停。`priority` 为 -100 至 100，等待任务先按高优先级、再按创建时间启动，不抢占已运行任务。`start_after_ms` 是最早启动的 Unix 毫秒时间，0 取消一次性延迟。设置跨重启保存，仅管理下载，不管理独立上传队列。应用和电脑必须运行且不休眠，不提供系统唤醒。API：`GET`/`PUT`/`POST /api/v1/queue`、`PUT`/`POST /api/v1/jobs/{id}/options`。

## Chrome 和 Edge 集成

在应用中打开 **Chrome / Edge** 集成，启用链接传送并打开原生安装包内的扩展目录。进入 `chrome://extensions` 或 `edge://extensions`，启用开发者模式，选择“加载已解压的扩展程序”并指定该目录。请保持所选目录的位置不变。

MultiRoute 必须正在运行，托盘中也可以。右键链接选择 MultiRoute 下载，或从扩展弹窗发送 URL。使用当前保存目录、网关和站点配置，必要时仍显示仓库/torrent 文件选择。默认端口 8765，API 改端口时同步修改。仅发送 URL，不复制浏览器 cookies/密码，不接管其他浏览器下载；源站认证在应用中配置。若浏览器请求本地连接权限，仅为选定扩展确认。

桥接默认关闭，只允许 `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` 使用 `/api/v1/browser/status` 和 `/api/v1/browser/download`，其他 API 来源规则不变。HTTP 202 只表示送达桌面流程，不代表下载完成。仅 headless 服务无法打开桌面选择。可从浏览器扩展页删除或在应用禁用集成。

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).

## 界面截图

下方展示当前应用窗口。打开原尺寸图片可查看各项控件。传输数值均为示例。

![MultiRoute Downloader — 下载队列与网关速度](images/v4.3.7/downloads-en-ae5b55131583.png)

*当前 Windows 界面，使用独立演示配置截取。示例文件、进度、速度及已启用 Pro 功能的视图用于说明各项控件，并非性能测量结果。*

<table>
<tr>
<td width="50%" valign="top"><strong>应用设置</strong><br /><a href="images/v4.3.7/settings-8abcb12a782d.png"><img src="images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="应用设置" /></a></td>
<td width="50%" valign="top"><strong>外观、快递员与声音</strong><br /><a href="images/v4.3.7/appearance-cebf121d5de7.png"><img src="images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="外观、快递员与声音" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>应用地址与受保护的 DNS</strong><br /><a href="images/v4.3.7/network-dns-e29dfdba1007.png"><img src="images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="应用地址与受保护的 DNS" /></a></td>
<td width="50%" valign="top"><strong>网关选择与速度检查</strong><br /><a href="images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="网关选择与速度检查" /></a></td>
</tr>
</table>
