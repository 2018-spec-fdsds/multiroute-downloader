# 面向 AI 智能体的 MultiRoute — 一次设置，默认用于下载

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <strong>中文</strong><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[产品](overview/zh.md) · [API 参考](../API.ru.txt) · [OpenAPI](../openapi.yaml)

只需添加一条项目规则并连接一次工具，Claude Code、Codex 和其他支持工具的智能体便可将项目的模型权重、数据集、压缩包及其他外部文件交给 MultiRoute 下载，保留任务 ID、跟踪进度并检查完成情况。你可以提出：**“将此项目所需的模型下载到其资源文件夹中。”** 规则提供下载器偏好，无需每次请求都重复 API 细节。

模型可以在云端或本地运行。关键在于**工具在哪里执行**：它必须能够访问运行 MultiRoute 的计算机。浏览器聊天或远程沙箱不会因为读取了指令文件就获得访问你电脑的权限。

## 1. 一次选定项目默认设置

1. 在用于存储文件的计算机上安装并启动 MultiRoute。使用为该实例配置的 API 端口；示例使用 `8765`。
2. 选择项目资源目录的绝对路径，例如 Windows 上的 `C:/Projects/MyProject/assets` 或 macOS 上的 `/Users/YOUR_USER/Projects/MyProject/assets`。Mac 上挂载的网络共享使用 `/Volumes/Share/...`。
3. 明确选择 REST 路由。普通操作系统连接使用 `[{"type":"direct","enabled":true}]`。要使用 MultiRoute 中已配置的独立网关，使用 `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]`，必要时添加其他已获准的网关。请先在应用中配置并验证这些网关。界面复选框不会自动复制到请求中。direct 路由使用普通操作系统 IP 路由，包括适用的系统/TUN VPN 路由。它不会继承 HTTP 代理/PAC 设置或仅供浏览器使用的 VPN 扩展。
4. 创建外部下载需要 **`automation_api`** 功能。批量创建还需要 `batch_downloads`；其他限制仍然有效。检查 `GET /license/policy`，包括 `features.automation_api`。
5. API 应仅绑定到环回地址。它没有通用的远程身份验证令牌；不要向局域网或互联网开放其端口。将源站凭据存储在 MultiRoute 的站点配置文件中，并在需要时传入其 `site_profile_id`。

## 2. 添加可复用的项目规则

将**[可直接使用的指令块](examples/ai-agent-instructions.zh.md)**复制到项目现有规则中。一次替换其中的目标目录和路由，同时保留其他项目指令。

| 智能体 / 环境 | 规则放置位置 | 所需连接 |
| --- | --- | --- |
| 电脑上的 Claude Code | 项目中的 `CLAUDE.md`。如果规则已位于 `AGENTS.md`，请在现有 `CLAUDE.md` 中添加 `@AGENTS.md`，避免维护两份副本。 | 本地 shell/REST 访问，或下文的 stdio MCP 服务器。 |
| 电脑上的 Codex | 项目中的 `AGENTS.md`。 | 本地 shell/REST 访问，或其配置的 MCP 服务器。 |
| 其他本地智能体或本地模型宿主 | 其持久化项目/系统指令。 | 启用能够调用 REST 的工具或 stdio MCP 客户端。仅加载模型并不会添加工具。 |
| 使用本地智能体执行器的云端模型 | 本地智能体的项目规则。 | 其本地工具调用同一电脑上的 MultiRoute。模型本身无需在本地运行。 |
| ChatGPT 网页版、Claude 网页版或远程云端执行器 | 项目指令，以及受支持的已连接工具。 | 需要单独配置通往 MultiRoute 计算机的桥接。此处附带的本地适配器使用 stdio，并非公共远程 MCP 服务。 |

指令文件用于引导行为，不能绕过客户端的工具审批或沙箱。现有客户端/账户限制仍然适用。参见官方的 [Claude 项目指令](https://code.claude.com/docs/en/memory)、[Codex 项目指令](https://developers.openai.com/codex/guides/agents-md/)以及 [ChatGPT 自定义 MCP 连接要求](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt)。

## 3. REST 示例 — 可访问 shell 的本地智能体

当智能体已具备本地终端时，这是最简单的方式。**MultiRoute 必须已在运行。** 这些示例使用示意性源 URL；请替换它和输出目录。创建任务会将真实下载加入队列。

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw '需要 MultiRoute automation_api 功能' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # 仅在获得发布方可信的 64 位十六进制哈希值时添加 expected_sha256。
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# 保留 $job.id。在任务处于 queued/running/retrying/verifying 时再次查询：
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / 本地 shell

将下方 JSON 以 UTF-8 编码保存为 `download.json`。替换 `YOUR_USER` 和源 URL；如果有可信哈希值，将 `expected_sha256` 放入同一对象。

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "/Users/YOUR_USER/Projects/MyProject/assets",
  "filename": "model.gguf",
  "routes": [{"type": "direct", "enabled": true}],
  "idempotency_key": "myproject-model-v1"
}
```

```sh
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/health'
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/license/policy'
# 确认 features.automation_api 为 true 后：
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# 将示例 id 替换为响应中的 id：
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` 表示任务已创建，并不表示文件已就绪。持续读取任务状态，直到 `complete`，或报告已暂停/失败的状态。完成后检查 `checksum_checking`、`integrity_status` 和可用的哈希值。对相同请求的重试使用相同的幂等键；有意发起新请求时使用新键。轮询应保持精简，并设置合理间隔；大文件可能长时间处于活动状态。包管理器和 Git 继续使用其原生依赖管理/克隆工作流。

## 4. 可选：兼容本地客户端的 MCP 工具

MultiRoute 包含一个作为 Java 子进程启动的 **stdio MCP 适配器**。它将操作转发给运行中的应用。没有内置的 HTTP `/mcp` 端点。当前的 `download_add` 要求明确提供**网关 IPv4 地址**；不接受 REST 的 `direct` 路由。仅使用操作系统路由时，请使用上文的 REST。

在 **设置 → MCP 和 CLI…** 中查看实际安装的 Java/JAR 路径。标准 Windows 安装在 `%LOCALAPPDATA%/MultiRoute Downloader App` 下包含 `runtime/bin/java.exe` 和 `app/MultiRoute-Downloader.jar`。在 macOS 上，它们位于 `/Applications/MultiRoute Downloader.app` 内，分别为 `Contents/runtime/Contents/Home/bin/java` 和 `Contents/app/MultiRoute-Downloader.jar`。包含空格的路径必须保留引号。这些原生安装包无需单独安装 Java。

stdio 客户端条目示例；请替换两个绝对路径。JSON 值**不会**自动展开 `%LOCALAPPDATA%`、`$HOME` 或 shell 变量：

```json
{
  "mcpServers": {
    "multiroute": {
      "command": "C:/Users/YOUR_USER/AppData/Local/MultiRoute Downloader App/runtime/bin/java.exe",
      "args": [
        "-Dfile.encoding=UTF-8",
        "-cp",
        "C:/Users/YOUR_USER/AppData/Local/MultiRoute Downloader App/app/MultiRoute-Downloader.jar",
        "multiroute.MultiRouteMcp",
        "--api",
        "http://127.0.0.1:8765/api/v1/"
      ]
    }
  }
}
```

对于 Claude Code，按照[文档中的项目配置](https://code.claude.com/docs/en/mcp)将此条目合并到 `.mcp.json`。对于 Codex，在其 TOML 配置中的 `[mcp_servers.multiroute]` 下配置相同命令和参数列表，或使用 `codex mcp add`；参见[官方 MCP 设置](https://developers.openai.com/codex/mcp/)。其他宿主使用各自的 MCP 设置。这只是配置示例，并不表示连接已经安装。

客户端完成发现后，请让它列出 MultiRoute 工具。随后，`download_add` 的参数可以如下：

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

后续 `download_status` 调用使用 `{"id":"012345abcdef"}`。可用工具包括 `download_add`、`download_batch`、`download_list`、`download_status`、`download_pause`、`download_resume` 和 `download_verify`。适配器的所有操作都需要 `automation_api`；`download_verify` 会重新检查已完成的文件。精简的 MCP 结果省略了源 URL、凭据和目标目录，因此请保留请求中的目录。`gateways: []` 不表示自动路由。

## 5. 云端聊天和远程沙箱

**仅靠项目指令无法将 ChatGPT 或 Claude 网页版连接到本地应用。** 云端工具中的 `127.0.0.1` 指向该工具自己的环境。ChatGPT 的远程 MCP 连接需要兼容的远程服务或受支持的安全桥接；MultiRoute 的 REST URL 不是这样的 MCP 端点。本版本不附带该远程桥接。由本地 Claude Code、Codex 或其他本地执行器使用的云端模型可以采用上述本地方法。[OpenAI 连接要求](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt)。

文件保存在运行 MultiRoute 的计算机上。远程构建要使用这些文件，必须另行执行经过批准的文件传输/共享存储步骤。凭据应放在已配置的站点配置文件中，不能放在指令块或聊天中。

## 智能体应提供的报告

“已通过 MultiRoute 将 `model.gguf` 下载到配置的资源文件夹。任务：`…`。状态：complete。SHA-256：…；与发布方参考值匹配。” 如果未提供或未找到可信参考值，应报告“已计算本地 SHA-256；无法进行发布方校验。” 如果应用不可用或返回 `pro_required`，请报告该情况并商定替代方案，不要悄悄更换下载器。
