# MultiRoute Resume 4.0 — 中文使用说明

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [Home](../README.md)

项目所有者：[2018-spec-fdsds](https://github.com/2018-spec-fdsds)。

## 1. 用途与工作方式

MultiRoute Resume 是 Java 桌面下载管理器，可通过同一 Ethernet 局域网中的多个互联网网关并行下载文件。例如，网关可以分别连接有线宽带和移动 4G/5G 网络。所有选中的可用网关从共享队列领取文件片段；速度更快的网关可以领取更多片段。一个连接中断时，其他网关继续下载，未完成的片段重新进入队列，已经校验的数据保留。也可以批量导入链接，或由其他程序通过本地 API 创建任务。

速度提升取决于各出口是否具有独立带宽、服务器限速和协议能力，并非网关数量的固定倍数。本程序是独立桌面应用，不需要浏览器来操作。新配置默认使用英语；可选择英语、俄语、德语、西班牙语、法语、中文、韩语、阿拉伯语和意大利语。语言设置会保存，已注册的状态、日志和提示随语言切换；服务器或外部库的原始消息可能保留原文。文件名、路径、URL 和 JSON 字段不会翻译。

## 2. 电脑与网络要求

| 平台 | 系统与处理器 | 所需组件 |
| --- | --- | --- |
| Windows | Windows 11 x64；不提供 ARM64 安装包 | 原生安装包包含 Java 21 和 x64 网络组件；仍需单独安装 Npcap |
| macOS Apple Silicon | M1/M2/M3/M4 或更新型号，macOS 26+ | arm64 PKG 内置 Java 和已编译网络组件 |
| macOS Intel | x86_64，macOS 15+ | x86_64 PKG 内置 Java 和已编译网络组件 |
| Linux | glibc 发行版，x86_64 或 ARM64；torrent/magnet 需要 glibc 2.38+ | 完整 Java 17+、sudo、C 编译器、binutils、pkg-config、libpcap 开发文件；桌面需要 X11 或 Wayland + XWayland 及字体库 |

完整 Java 必须包含 `java.desktop`、`java.net.http`、`jdk.httpserver`、`java.xml`；`--headless` 也需要这些模块。原生 Windows/macOS 安装包无需另装 Java。旧版 portable ZIP 3.0 和 Linux 部署可按需从官方来源下载专用 Java；请勿混用不同版本的单个文件。

需要与所选网关位于同一 IPv4 子网的**有线 Ethernet** 网卡。每个网关都必须允许程序虚拟地址访问互联网。仅 Wi-Fi、仅 VPN 或仅 IPv6 不是此网关传输的支持配置。建议至少 4 GB 内存，但这不是经过基准测试的硬性下限；不需要 GPU、CUDA 或 AI 模型。程序、数据和下载目录必须可写。部署检查至少 512 MiB 空间，实际下载还需要完整文件及临时片段空间；torrent 应为片段边界、目标文件和缓存预留额外空间。

所选语言需要系统已安装覆盖其文字的字体。在最小化 Linux 中，请从发行版仓库安装适当字体，例如中文/韩文使用 Noto CJK、阿拉伯文使用相应 Noto 字体。Java 使用平台字体回退；本应用不安装或打包字体包。不同电脑的字符覆盖可能不同。

## 3. 安装与安全确认

从 [4.0 发布页](https://github.com/2018-spec-fdsds/multiroute-resume/releases/tag/v4.0) 下载安装包（如果已附在该版本中），并与发布页的 SHA-256 比较：

- Windows：`MultiRoute-Resume-4.0-Windows-x64-Setup.exe`。
- Apple Silicon，macOS 26+：`MultiRoute-Resume-4.0-macOS-arm64.pkg`。
- Intel，macOS 15+：`MultiRoute-Resume-4.0-macOS-x86_64.pkg`。

### Windows

打开 EXE，按向导完成当前用户安装，再使用开始菜单或桌面快捷方式启动。程序不以命令脚本窗口运行。安装向导文字为英语；系统安全对话框遵循 Windows 语言。

首次启动会检查 Npcap。缺少时，程序征得同意后从官方来源下载固定版本，校验 SHA-256 和 Authenticode 发布者，再打开 Npcap 的普通安装程序。阅读许可证并批准正常的 UAC 请求；如果要求重启，重启后再打开应用。Npcap 不随本程序打包，也不会静默安装。此步骤需要联网。AdminOnly 驱动可能再次要求捕获授权；Java 窗口仍应作为普通用户运行。

本应用和安装包没有发布者签名。只有在确认官方来源和校验和后，才自行决定是否使用 SmartScreen 提供的“更多信息 → 仍要运行”。病毒检测与“未知发布者”不同；不要关闭杀毒软件或为安装设置排除项。

### macOS

选择与原生处理器匹配的 PKG，在 Apple Installer 中安装并批准放置受保护网络组件的管理员请求。然后从 Finder、Spotlight 或 Dock 打开 `/Applications/MultiRoute Resume.app`。此安装包不需要 Terminal、Command Line Tools、Homebrew 或单独 Java。

应用需要网络组件打开 Ethernet BPF 设备时，会显示正常的 macOS 管理员授权；组件打开捕获设备后降低权限，Java 以普通用户运行。程序和 PKG **没有 Developer ID 签名，也未公证**。如系统提示无法验证开发者，且提供例外入口，请先核对来源和 SHA-256，再按 [Apple 官方说明](https://support.apple.com/en-us/102445) 使用“系统设置 → 隐私与安全性 → 仍要打开”，然后确认“打开”。受管理设备可能禁止例外。

安装授权、网络组件授权和未验证开发者例外是不同确认。系统询问时允许访问“本地网络”，以及您实际选择的下载文件夹。不要关闭 Gatekeeper/SIP、整体移除隔离标记或把全部 Java 作为 root 运行。“恶意软件/文件损坏”警告不是普通开发者提示，应停止并检查文件。

### Linux 与旧 portable ZIP 3.0

下载并解压完整 ZIP。Linux 首次执行 `bash ./Deploy-Linux.sh`，以后执行 `bash ./Start-Linux.sh`；Windows portable 首次 `Deploy-Windows.cmd`、以后 `Start-Windows.cmd`；macOS portable 使用 `Deploy-macOS.command` 和 `Start-macOS.command`。这些旧发行包保留脚本启动方式，不能当作 4.0 原生安装包。Linux 部署按发行版安装缺少的编译和 libpcap 依赖；macOS portable 可要求 Apple Command Line Tools 和 sudo。保留其授权终端；不要让整个 Java 进程作为 root 运行。Alpine/musl 不在发行范围内。部署不会升级操作系统或替换 glibc。

九种语言的说明包含在离线安装中。Windows：`%LOCALAPPDATA%/MultiRoute Resume App/app/docs/`；macOS：对应用选择“显示包内容”，打开 `Contents/app/docs/zh.md`。浏览器中的版本说明链接需要联网。

## 4. 配置网关与虚拟地址

默认四个网关为 `192.168.1.1`、`192.168.1.2`、`192.168.1.3`、`192.168.1.4`，默认全部选中。可编辑地址并通过“添加网关”扩展到 **1–32** 个不重复网关。电脑使用其中一个作为默认出口，并不意味着还存在第五个网关。

应用虚拟 IPv4 默认 `192.168.1.70`。在网络设置中检查并应用，确认该地址未被设备占用，并在 DHCP 自动分配范围之外保留。它只用于程序内部传输，**不会加到系统网卡，也不会修改系统路由表**；其他应用继续使用原连接。不要在 Windows/macOS/Linux 网卡设置中手动添加此地址。旁边的问号和悬浮提示解释用途。

检查各网关后选择一个、多个或全部。速度测试最多传输 **16 MiB 或 5 秒**，建立连接可能额外耗时；这只是测试限制，正常下载不受此限制。没有合适的直接 HTTP 链接时使用 Selectel 测试文件。

下载中可以增删网关配置或调整任务的已选网关，但每个未完成任务至少保留一个有效网关。新增配置不会自动加入已有任务，需在该任务中选择。更改虚拟 IP 需要先暂停活动任务。MAC 地址不会在用户界面显示。

## 5. 下载、续传与桌面界面

1. 在长 URL 输入框中按 Ctrl+V（macOS 为 Cmd+V）粘贴，或使用粘贴按钮。可粘贴 magnet 或打开 `.torrent` 文件。
2. 用系统文件夹选择器指定保存位置。默认 Windows 为 `%USERPROFILE%\Downloads`，macOS/Linux 为 `~/Downloads`；手动指定的目录会保留。
3. 选择网关，点击 URL 旁的开始下载。Hugging Face 仓库、WebDAV 列表和 Metalink 可预览并选择文件；批量导入支持每行一个链接和文本文件。
4. 观察每个任务的进度条、已用/剩余时间、总速度和各网关速度。共享队列的 HTTP 片段默认 **20 MiB**，可设为 1–64 MiB。某片段 **60 秒**没有收到数据时取消该传输并回到共享池，由其他网关领取。torrent 片段大小来自其元数据。
5. 可暂停/继续、调整优先级、限速和计划；默认同时活动任务数为 2。重启后原活动任务恢复为暂停，按需继续。已停止任务可更改文件夹。torrent 做种默认关闭，可设置时间和分享率上限。

界面使用 FlatLaf，支持保存的浅色、深色和系统主题。顶部汇总所有/活动/已完成任务和全部任务的合计速度，不受表格筛选影响。每个任务有独立进度条；列表和窄窗口可滚动，刷新保留位置，支持排序和筛选。

悬停正在下载或已完成的行，可见**完整文件名和绝对保存路径**；排序/筛选后也对应正确文件。所选文件标题有相同提示。右键菜单可复制完整路径。Ctrl+L/Cmd+L 定位 URL，Ctrl+F/Cmd+F 定位搜索，Escape 清空搜索。

所选任务有“详细信息”和“历史”页签。报告刷新或切换语言时保留阅读位置和文本选择；选择另一任务时从其报告开头显示。全局日志分为应用事件与下载历史；滚动到末尾时跟随新条目，向上阅读时不强制跳回。

关闭主窗口会继续在托盘下载，图标显示全部任务总速度，可显示到 10 Gbit/s；悬浮提示更精确。用托盘“打开应用”恢复窗口，“退出应用”才真正保存并结束。无托盘支持时最小化到任务栏。升级前请从托盘退出。

删除任务时，“同时删除文件”默认**不勾选**。可只从任务列表移除而保留完整或部分文件；仅在明确需要时勾选删除。持久历史日志不会因删除任务或文件而清空。

## 6. 协议、访问与校验

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

## 7. 其他应用使用本地 API

启动桌面应用后，API 位于 `http://127.0.0.1:8765/api/v1`。请求与响应为 JSON、UTF-8；写请求使用 `Content-Type: application/json`。本地创建/管理任务不需要 API token，来源凭据另行配置。只接受本机连接，不要把端口开放到局域网或互联网。支持 `--headless` 仅运行 API；原生应用应使用正常启动器，`.cmd` 示例只用于 portable 发行包。

健康检查：`GET /api/v1/health`，正常响应含 `{"ok":true,"version":"4.0"}`。先在界面配置网络，或向 `POST /api/v1/network` 发送：

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

## 8. 持久历史、数据与更新

| 系统 | 数据目录 | 应用与运行时 |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Resume\data` | `%LOCALAPPDATA%\MultiRoute Resume App` |
| macOS | `~/Library/Application Support/MultiRoute Resume/data` | `/Applications/MultiRoute Resume.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

原生 macOS 网络组件为 `/Library/PrivilegedHelperTools/MultiRouteResume/4.0/gateway-net`。portable Windows 3.0 使用启动目录下 `data/`，可由 `data-location.config` 或 `--data-dir` 覆盖；Linux 私有运行时在 `~/.local/share/MultiRouteResume/runtime`，受保护组件在 `/usr/local/libexec/MultiRouteResume/3.0/gateway-net`。下载文件在您所选目录，可能在上述位置之外。

`logs/downloads-history.jsonl` 是 UTF-8 逐行 JSON，`logs/downloads-history.log` 是带日期时间的可读日志。它们跨重启追加，不因任务删除或切换语言重写。文本日志保留写入时语言，界面按当前语言显示历史。查看窗口最多读取最近 1,000 个事件/2 MiB，完整日志保存在磁盘并持续增长，可另行归档。日志保留文件名和绝对路径，不写来源 URL、密码或 token；分享前检查个人路径。

API 的 `lifecycle_history` 事件含 `event_id`、`job_id`、`kind`、`time`、`filename`、`path`；torrent 可含 `paths`，暂停可含 `reason`（`pause`、`shutdown`、`maintenance`）。事件为 `started`、`resumed`、`paused`、`completed`、`recovered`、`failed`，时间是 ISO 8601 UTC（如 `2026-10-18T12:34:56.123Z`）；可读日志含本地完整日期、时间和 UTC 偏移。可选 `first_started_at`、`last_resumed_at`、`completed_at` 概括实际观察到的开始、最近继续和校验发布后的完成。排队还不算开始；旧任务不补造日期，`recovered` 是现在确认旧文件完整，并非知道旧完成时间。

升级前从托盘退出，再安装完整新包，保留数据和部分文件。Windows 通过“设置 → 应用 → 已安装的应用”卸载；macOS 删除 `.app` 不会一并删除历史或受保护组件。不要为了卸载而删下载目录。Linux 移动应用目录后重新部署。

## 9. 故障排查与验证范围

- 启动失败：检查版本、处理器、完整安装包和 Java 模块；显式错误的 `JAVA_EXE`/`JAVA_HOME` 应更正。4.0 保留 3.9 的临时输出文件锁修复和已有实例身份检查。
- 网关零速：先检查有线网卡、同子网、虚拟地址未占用、网关实际互联网权限及来源 URL/访问限制。速度测试通过不证明所有实际下载都能成功。
- torrent 报 glibc/macOS 库不兼容：使用满足上述最低版本的系统，不手动替换系统库。
- SFTP 主机密钥不符：与服务器管理员核实，不关闭主机密钥验证。
- 端口 8765 被其他应用占用：使用 `--port 8766`；只替换经过验证属于本应用的旧实例，不结束无关进程。
- 空间不足或文件夹不可用：检查权限与文件、缓存和片段所需空间，先暂停再改目录。相同目标已有任务时选择原任务继续，不重复创建。

报告问题时提供版本、系统/处理器、阶段和确切错误；不要发送密码或 token，先移除私有链接与个人信息。Windows/POSIX 受控测试、原生 Mac CI 和离线包检查不代表真实路由器吞吐、故障切换、终端用户安装或 Finder/Gatekeeper/BPF 授权已经验证。发布包无发布者签名，macOS 未公证，应视为**预发布**。安装后先对每个网关测试已知小文件，再测试组合网关。

## 10. 许可证

原版应用可免费用于个人和组织，包括商业活动；未经单独许可，不得销售应用、收取许可证费用或有偿分发。此自定义许可不一般性授权修改/再分发，也不是 OSI 认可的开源许可证。以英文 [LICENSE.txt](../LICENSE.txt) 为准；FlatLaf（Apache-2.0）及其他第三方组件保留其自身许可。项目所有者：[2018-spec-fdsds](https://github.com/2018-spec-fdsds)。
