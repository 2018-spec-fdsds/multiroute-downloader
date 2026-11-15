# MultiRoute for AI agents — set up once, download by default

[Русский](ai-agents.ru.md) · [Product](../README.md) · [API reference](../API.ru.txt) · [OpenAPI](../openapi.yaml)

Add one project rule and connect a tool once. Claude Code, Codex and other tool-enabled agents can then hand the project's model weights, datasets, archives and other external files to MultiRoute, retain job IDs, follow progress and check completion. You can ask: **“Download the model needed by this project into its assets folder.”** The rule supplies the downloader preference; you do not need to repeat API details with each request.

The model may run in the cloud or locally. What matters is **where its tool executes**: it must be able to reach the computer running MultiRoute. A browser chat or a remote sandbox does not gain access to your PC by reading an instruction file.

## 1. Choose the project defaults once

1. Install and start MultiRoute on the computer that will store the files. Use the API port configured for that instance; examples use `8765`.
2. Choose an absolute project assets directory, such as `C:/Projects/MyProject/assets` on Windows or `/Users/YOUR_USER/Projects/MyProject/assets` on macOS. A mounted Mac network share uses `/Volumes/Share/...`.
3. Choose the REST routes explicitly. The ordinary OS connection uses `[{"type":"direct","enabled":true}]`. To use MultiRoute's configured independent gateways, use `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]`, adding other approved gateways if needed. Configure and verify those gateways in the app first. UI checkboxes are not automatically copied into requests. The direct route uses ordinary OS IP routing, including applicable system/TUN VPN routes. It does not inherit HTTP proxy/PAC settings or browser-only VPN extensions.
4. External download creation requires the **`automation_api`** capability. Batch creation also requires `batch_downloads`; other limits remain in effect. Check `GET /license/policy`, including `features.automation_api`.
5. Keep the API on loopback. It has no general remote authentication token; do not publish its port on the LAN or Internet. Store source credentials in a site profile in MultiRoute and pass its `site_profile_id` when needed.

## 2. Add the reusable project rule

Copy the **[ready-to-use instruction block](examples/ai-agent-instructions.md)** into the project's existing rules. Replace its destination and routes once, preserving the other project instructions.

| Agent / environment | Where to put the rule | Required connection |
| --- | --- | --- |
| Claude Code on your PC | `CLAUDE.md` in the project. If the rule already lives in `AGENTS.md`, add `@AGENTS.md` to an existing `CLAUDE.md` rather than maintaining two copies. | Local shell/REST access, or the stdio MCP server below. |
| Codex on your PC | `AGENTS.md` in the project. | Local shell/REST access, or its configured MCP server. |
| Other local agent or local-model host | Its persistent project/system instructions. | Enable a tool capable of calling REST or a stdio MCP client. Loading a model alone does not add tools. |
| Cloud model with a local agent executor | The local agent's project rules. | Its local tool calls MultiRoute on the same PC. The model itself need not run locally. |
| ChatGPT web, Claude web, or a remote cloud executor | Project instructions, plus a supported connected tool. | Needs a separately configured bridge to the MultiRoute computer. The local adapter shipped here is stdio, not a public remote MCP service. |

Instruction files guide behavior; they do not bypass the client's tool approvals or sandbox. Existing client/account restrictions still apply. See the official [Claude project instructions](https://code.claude.com/docs/en/memory), [Codex project instructions](https://developers.openai.com/codex/guides/agents-md/) and [ChatGPT custom MCP connection requirements](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

## 3. REST example — a local agent with shell access

This is the simplest path when the agent already has a local terminal. **MultiRoute must already be running.** These are examples with an illustrative source URL; replace it and the output directory. Creating a job queues a real download.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'MultiRoute automation_api is required' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # Add expected_sha256 only when you have the publisher's trusted 64-character hex hash.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# Keep $job.id. Query it again while it is queued/running/retrying/verifying:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / local shell

Save the following JSON as `download.json` in UTF-8. Replace `YOUR_USER` and the source URL; put `expected_sha256` in the same object when a trusted hash is available.

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
# After confirming features.automation_api is true:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# Replace the example id with the id from the response:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` means the job was created, not that the file is ready. Continue reading its status until `complete`, or report a paused/failed state. At completion check `checksum_checking`, `integrity_status` and available hashes. Use the same idempotency key for an identical retry; use a new key for a deliberately new request. Keep polling compact and reasonably spaced; large files can remain active for a long time. Package managers and Git keep their native dependency/clone workflows.

## 4. Optional: MCP tools for a compatible local client

MultiRoute includes a **stdio MCP adapter**, launched as a Java subprocess. It forwards operations to the running app. There is no built-in HTTP `/mcp` endpoint. Its current `download_add` requires explicit **gateway IPv4 addresses**; it does not accept the REST `direct` route. Use REST above for an OS-only route.

Find the exact installed Java/JAR paths in **Settings → MCP and CLI…**. A standard Windows installation has `runtime/bin/java.exe` and `app/MultiRoute-Downloader.jar` beneath `%LOCALAPPDATA%/MultiRoute Downloader App`. On macOS they are `Contents/runtime/Contents/Home/bin/java` and `Contents/app/MultiRoute-Downloader.jar` inside `/Applications/MultiRoute Downloader.app`. Keep paths containing spaces quoted. A separate Java install is not needed for these native packages.

Example stdio client entry; replace both absolute paths. JSON values do **not** expand `%LOCALAPPDATA%`, `$HOME` or shell variables automatically:

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

For Claude Code, merge this entry into `.mcp.json` using the [documented project configuration](https://code.claude.com/docs/en/mcp). For Codex, configure the same command and argument list under `[mcp_servers.multiroute]` in its TOML configuration, or use `codex mcp add`; see [official MCP setup](https://developers.openai.com/codex/mcp/). Other hosts use their own MCP settings. This is an example configuration, not an installed connection.

After client discovery, ask it to list the MultiRoute tools. Then its `download_add` arguments can be:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

The subsequent `download_status` call takes `{"id":"012345abcdef"}`. Available tools are `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume` and `download_verify`. All adapter operations require `automation_api`; `download_verify` rechecks a completed file. Compact MCP results omit source URLs, credentials and the destination directory, so retain the directory from the request. `gateways: []` is not automatic routing.

## 5. Cloud chats and remote sandboxes

**A project instruction alone cannot connect ChatGPT or Claude web to your local app.** `127.0.0.1` in a cloud tool points at that tool's environment. ChatGPT's remote MCP connection requires a compatible remote service or supported secure bridge; MultiRoute's REST URL is not such an MCP endpoint. This release does not ship that remote bridge. A cloud-hosted model used by a local Claude Code, Codex or another local executor can use the local methods above. [OpenAI connection requirements](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

Files are saved on the MultiRoute computer. A remote build needs a separate approved file-transfer/shared-storage step before it can consume them. Credentials belong in configured site profiles, not in the instruction block or a chat.

## Expected agent report

“Downloaded `model.gguf` through MultiRoute to the configured assets folder. Job: `…`. Status: complete. SHA-256: …; publisher reference matched.” If no trusted reference was provided or found, report “local SHA-256 calculated; publisher verification unavailable.” If the app is unavailable or returns `pro_required`, report that and agree on a fallback rather than silently switching downloaders.
