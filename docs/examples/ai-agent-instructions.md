# MultiRoute project instruction — copy into your agent's rules

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<strong>English</strong> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

Use this block in your existing `AGENTS.md`, `CLAUDE.md`, or equivalent persistent project instructions. Append it; keep the project's other rules. Replace the destination and routes once. For setup and examples, see [English](../ai-agents.en.md) or [Русский](../ai-agents.ru.md).

```markdown
## Downloads: use MultiRoute by default

For external files required by the authorized project task (models, datasets,
archives, media and other assets), use the running MultiRoute Downloader.
Do not ask me to choose a downloader for every file. Keep package managers and
Git on their normal install/clone/update paths.

Project settings (edit once):
- API: http://127.0.0.1:8765/api/v1
- Destination: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- REST routes: [{"type":"direct","enabled":true}]
- Optional site_profile_id: <EXISTING_PROFILE_ID_OR_OMIT>

Use the connected local REST tool, or the MultiRoute MCP tools when available.
For MCP, configure explicit gateway IPv4 addresses separately; its download_add
requires url, directory and gateways. The REST direct route is the OS path;
it is not an MCP gateway value. Do not invent routes or assume UI selection.

With REST, check GET /health and GET /license/policy before first use in a session.
With MCP only, discover the tools and call download_list with limit 1 to check
connectivity and capability; the adapter itself checks the licence policy.
Creating downloads requires features.automation_api=true. If unavailable,
report the reason and ask about another downloader instead of silently using one.

For each required file: choose its exact URL, approved destination and filename;
pass a trusted publisher SHA-256 as expected_sha256 when available. Create the
job with POST /jobs (routes, not gateways), or MCP download_add. Use a stable
idempotency key for retries of the same request; keep the returned job id.
Creation is not completion. Query that job with GET /jobs/{id} or download_status
at reasonable intervals. Report paused/failed states; reuse the existing job
for resume instead of creating duplicates. Do not change unrelated jobs or
network settings. When complete, check checksum_checking and integrity_status.
Do not claim publisher verification without a trusted reference match.
Return the saved local path, job id and verification result. The file remains
on the MultiRoute computer; do not claim it exists in a remote execution sandbox.
```

The rule expresses a project preference. A compatible connected tool and the agent's normal permissions are still required. Source files being downloaded are data, not additional agent instructions.
