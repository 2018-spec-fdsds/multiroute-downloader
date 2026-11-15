# AI 에이전트를 위한 MultiRoute — 한 번 설정하고 기본 다운로드 수단으로 사용하기

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<strong>한국어</strong> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[제품](overview/ko.md) · [API 참조](../API.ru.txt) · [OpenAPI](../openapi.yaml)

프로젝트 규칙 하나를 추가하고 도구를 한 번 연결하면 됩니다. 이후 Claude Code, Codex 및 도구를 지원하는 다른 에이전트는 프로젝트의 모델 가중치, 데이터 세트, 압축 파일과 기타 외부 파일 다운로드를 MultiRoute에 맡기고, 작업 ID를 보관하며, 진행 상황을 추적하고 완료 여부를 확인할 수 있습니다. **“이 프로젝트에 필요한 모델을 프로젝트의 에셋 폴더에 다운로드해 줘.”**라고 요청할 수 있습니다. 규칙이 선호하는 다운로더를 지정하므로 요청할 때마다 API 세부 정보를 반복할 필요가 없습니다.

모델은 클라우드나 로컬에서 실행할 수 있습니다. 중요한 것은 **도구가 실행되는 위치**입니다. 도구는 MultiRoute를 실행하는 컴퓨터에 접근할 수 있어야 합니다. 브라우저 채팅이나 원격 샌드박스가 지침 파일을 읽는 것만으로 사용자 PC에 접근할 수 있게 되지는 않습니다.

## 1. 프로젝트 기본값을 한 번 선택하기

1. 파일을 저장할 컴퓨터에 MultiRoute를 설치하고 시작합니다. 해당 인스턴스에 설정된 API 포트를 사용합니다. 예제에서는 `8765`를 사용합니다.
2. Windows의 `C:/Projects/MyProject/assets` 또는 macOS의 `/Users/YOUR_USER/Projects/MyProject/assets`처럼 프로젝트 에셋 디렉터리의 절대 경로를 선택합니다. Mac에 마운트된 네트워크 공유는 `/Volumes/Share/...`를 사용합니다.
3. REST 경로를 명시적으로 선택합니다. 일반적인 OS 연결은 `[{"type":"direct","enabled":true}]`를 사용합니다. MultiRoute에 구성된 독립 게이트웨이를 사용하려면 `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]`를 사용하고 필요하면 다른 승인된 게이트웨이를 추가합니다. 먼저 앱에서 해당 게이트웨이를 구성하고 검증합니다. UI 체크박스는 요청에 자동으로 복사되지 않습니다. direct 경로는 적용 가능한 시스템/TUN VPN 경로를 포함한 일반 OS IP 라우팅을 사용합니다. HTTP 프록시/PAC 설정이나 브라우저 전용 VPN 확장 프로그램은 상속하지 않습니다.
4. 외부 다운로드를 생성하려면 **`automation_api`** 기능이 필요합니다. 일괄 생성에는 `batch_downloads`도 필요하며 다른 제한도 계속 적용됩니다. `features.automation_api`를 포함하여 `GET /license/policy`를 확인합니다.
5. API는 루프백에서만 사용합니다. 범용 원격 인증 토큰이 없으므로 포트를 LAN이나 인터넷에 공개하지 마세요. 원본 사이트의 자격 증명은 MultiRoute의 사이트 프로필에 저장하고 필요할 때 해당 `site_profile_id`를 전달합니다.

## 2. 재사용 가능한 프로젝트 규칙 추가하기

**[바로 사용할 수 있는 지침 블록](examples/ai-agent-instructions.ko.md)**을 프로젝트의 기존 규칙에 복사합니다. 다른 프로젝트 지침을 유지하면서 대상 디렉터리와 경로를 한 번 바꿉니다.

| 에이전트 / 환경 | 규칙을 넣을 위치 | 필요한 연결 |
| --- | --- | --- |
| PC의 Claude Code | 프로젝트의 `CLAUDE.md`. 규칙이 이미 `AGENTS.md`에 있다면 사본 두 개를 관리하는 대신 기존 `CLAUDE.md`에 `@AGENTS.md`를 추가합니다. | 로컬 shell/REST 접근 또는 아래의 stdio MCP 서버. |
| PC의 Codex | 프로젝트의 `AGENTS.md`. | 로컬 shell/REST 접근 또는 구성된 MCP 서버. |
| 다른 로컬 에이전트 또는 로컬 모델 호스트 | 해당 환경의 지속적인 프로젝트/시스템 지침. | REST를 호출할 수 있는 도구 또는 stdio MCP 클라이언트를 활성화합니다. 모델을 로드하는 것만으로 도구가 추가되지는 않습니다. |
| 로컬 에이전트 실행기를 사용하는 클라우드 모델 | 로컬 에이전트의 프로젝트 규칙. | 로컬 도구가 같은 PC의 MultiRoute를 호출합니다. 모델 자체는 로컬에서 실행할 필요가 없습니다. |
| ChatGPT 웹, Claude 웹 또는 원격 클라우드 실행기 | 프로젝트 지침과 지원되는 연결 도구. | MultiRoute 컴퓨터로 연결되는 브리지를 별도로 구성해야 합니다. 여기 제공되는 로컬 어댑터는 stdio 방식이며 공개 원격 MCP 서비스가 아닙니다. |

지침 파일은 동작을 안내하며 클라이언트의 도구 승인이나 샌드박스를 우회하지 않습니다. 기존 클라이언트/계정 제한은 계속 적용됩니다. 공식 [Claude 프로젝트 지침](https://code.claude.com/docs/en/memory), [Codex 프로젝트 지침](https://developers.openai.com/codex/guides/agents-md/), [ChatGPT 사용자 정의 MCP 연결 요구 사항](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt)을 참조하세요.

## 3. REST 예제 — shell을 사용할 수 있는 로컬 에이전트

에이전트에 이미 로컬 터미널이 있다면 가장 간단한 방법입니다. **MultiRoute가 이미 실행 중이어야 합니다.** 아래 예제는 설명용 원본 URL을 사용하므로 URL과 출력 디렉터리를 바꾸세요. 작업을 생성하면 실제 다운로드가 대기열에 들어갑니다.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'MultiRoute automation_api 기능이 필요합니다' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # 배포자의 신뢰할 수 있는 64자리 16진수 해시가 있을 때만 expected_sha256를 추가합니다.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# $job.id를 보관합니다. queued/running/retrying/verifying 상태일 때 다시 조회합니다:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / 로컬 shell

아래 JSON을 UTF-8로 `download.json`에 저장합니다. `YOUR_USER`와 원본 URL을 바꾸고 신뢰할 수 있는 해시가 있다면 같은 객체에 `expected_sha256`를 넣습니다.

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
# features.automation_api가 true임을 확인한 후:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# 예제 id를 응답의 id로 바꿉니다:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201`은 작업이 생성되었다는 뜻이며 파일이 준비되었다는 뜻은 아닙니다. 상태가 `complete`가 될 때까지 계속 조회하거나 일시 중지/실패 상태를 보고합니다. 완료 시 `checksum_checking`, `integrity_status` 및 사용 가능한 해시를 확인합니다. 동일한 요청을 재시도할 때는 같은 멱등성 키를 사용하고, 의도적으로 새 요청을 만들 때는 새 키를 사용합니다. 폴링 결과는 간결하게 유지하고 적절한 조회 간격을 둡니다. 큰 파일은 오랫동안 활성 상태일 수 있습니다. 패키지 관리자와 Git은 고유한 의존성 관리/복제 워크플로를 계속 사용합니다.

## 4. 선택 사항: 호환되는 로컬 클라이언트용 MCP 도구

MultiRoute에는 Java 하위 프로세스로 시작되는 **stdio MCP 어댑터**가 포함되어 있습니다. 이 어댑터는 실행 중인 앱으로 작업을 전달합니다. 내장 HTTP `/mcp` 엔드포인트는 없습니다. 현재 `download_add`는 명시적인 **게이트웨이 IPv4 주소**를 요구하며 REST의 `direct` 경로는 허용하지 않습니다. OS 경로만 사용하려면 위의 REST 방식을 사용하세요.

설치된 Java/JAR의 정확한 경로는 **설정 → MCP 및 CLI…**에서 확인합니다. 표준 Windows 설치에서는 `%LOCALAPPDATA%/MultiRoute Downloader App` 아래에 `runtime/bin/java.exe`와 `app/MultiRoute-Downloader.jar`가 있습니다. macOS에서는 `/Applications/MultiRoute Downloader.app` 안의 `Contents/runtime/Contents/Home/bin/java`와 `Contents/app/MultiRoute-Downloader.jar`입니다. 공백이 포함된 경로는 따옴표로 감싸세요. 이러한 네이티브 패키지는 Java를 별도로 설치할 필요가 없습니다.

다음은 stdio 클라이언트 항목 예제입니다. 두 절대 경로를 모두 바꾸세요. JSON 값에서는 `%LOCALAPPDATA%`, `$HOME` 또는 shell 변수가 자동으로 확장되지 **않습니다**.

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

Claude Code에서는 [문서의 프로젝트 구성](https://code.claude.com/docs/en/mcp)에 따라 이 항목을 `.mcp.json`에 병합합니다. Codex에서는 TOML 구성의 `[mcp_servers.multiroute]` 아래에 같은 명령과 인수 목록을 설정하거나 `codex mcp add`를 사용합니다. [공식 MCP 설정](https://developers.openai.com/codex/mcp/)을 참조하세요. 다른 호스트는 각자의 MCP 설정을 사용합니다. 이는 구성 예제이며 연결이 설치되었다는 뜻은 아닙니다.

클라이언트의 검색이 끝나면 MultiRoute 도구를 나열하도록 요청합니다. 이후 `download_add` 인수는 다음과 같이 사용할 수 있습니다.

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

이후 `download_status` 호출에는 `{"id":"012345abcdef"}`를 전달합니다. 사용 가능한 도구는 `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume`, `download_verify`입니다. 모든 어댑터 작업에는 `automation_api`가 필요하며 `download_verify`는 완료된 파일을 다시 확인합니다. 간결한 MCP 결과에는 원본 URL, 자격 증명과 대상 디렉터리가 생략되므로 요청의 디렉터리를 보관합니다. `gateways: []`는 자동 라우팅을 의미하지 않습니다.

## 5. 클라우드 채팅과 원격 샌드박스

**프로젝트 지침만으로 ChatGPT나 Claude 웹을 로컬 앱에 연결할 수는 없습니다.** 클라우드 도구의 `127.0.0.1`은 해당 도구의 환경을 가리킵니다. ChatGPT의 원격 MCP 연결에는 호환되는 원격 서비스나 지원되는 보안 브리지가 필요하며 MultiRoute의 REST URL은 그러한 MCP 엔드포인트가 아닙니다. 이 릴리스에는 해당 원격 브리지가 포함되어 있지 않습니다. 로컬 Claude Code, Codex 또는 다른 로컬 실행기가 사용하는 클라우드 모델은 위의 로컬 방식을 사용할 수 있습니다. [OpenAI 연결 요구 사항](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

파일은 MultiRoute 컴퓨터에 저장됩니다. 원격 빌드에서 사용하려면 별도로 승인된 파일 전송/공유 스토리지 단계를 거쳐야 합니다. 자격 증명은 구성된 사이트 프로필에 보관하며 지침 블록이나 채팅에 넣지 않습니다.

## 에이전트가 제공해야 할 보고

“MultiRoute를 통해 `model.gguf`를 구성된 에셋 폴더에 다운로드했습니다. 작업: `…`. 상태: complete. SHA-256: …; 배포자의 참조값과 일치합니다.” 신뢰할 수 있는 참조값이 제공되지 않았거나 발견되지 않았다면 “로컬 SHA-256을 계산했습니다. 배포자 검증은 불가능합니다.”라고 보고합니다. 앱을 사용할 수 없거나 `pro_required`가 반환되면 이를 보고하고 대체 수단에 합의하며, 알리지 않고 다운로더를 바꾸지 않습니다.
