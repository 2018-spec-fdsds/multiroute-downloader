# AI エージェント向け MultiRoute — 一度設定して、既定のダウンロード手段に

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <strong>日本語</strong> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[製品](overview/ja.md) · [API リファレンス](../API.ru.txt) · [OpenAPI](../openapi.yaml)

プロジェクトルールを 1 つ追加し、一度ツールを接続するだけで、Claude Code、Codex などのツール対応エージェントは、プロジェクトのモデルの重み、データセット、アーカイブ、その他の外部ファイルのダウンロードを MultiRoute に任せ、ジョブ ID を保持し、進捗を追跡して完了を確認できます。たとえば、**「このプロジェクトに必要なモデルを、そのアセットフォルダーにダウンロードして」**と依頼できます。ルールでダウンローダーの優先設定を指定するため、リクエストのたびに API の詳細を繰り返す必要はありません。

モデルはクラウドでもローカルでも実行できます。重要なのは**ツールがどこで実行されるか**です。ツールから MultiRoute が動作しているコンピューターにアクセスできる必要があります。ブラウザーのチャットやリモートサンドボックスは、指示ファイルを読むだけで PC にアクセスできるようにはなりません。

## 1. プロジェクトの既定値を一度設定する

1. ファイルを保存するコンピューターに MultiRoute をインストールし、起動します。そのインスタンスに設定した API ポートを使用してください。例では `8765` を使用します。
2. プロジェクトのアセットディレクトリを絶対パスで選択します。Windows では `C:/Projects/MyProject/assets`、macOS では `/Users/YOUR_USER/Projects/MyProject/assets` などです。Mac にマウントしたネットワーク共有では `/Volumes/Share/...` を使用します。
3. REST のルートを明示的に選択します。通常の OS 接続には `[{"type":"direct","enabled":true}]` を使用します。MultiRoute に設定済みの独立したゲートウェイを使用する場合は `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]` を使用し、必要に応じて他の承認済みゲートウェイを追加します。まずアプリでそれらのゲートウェイを設定し、検証してください。UI のチェックボックスはリクエストに自動では反映されません。direct ルートは通常の OS の IP ルーティングを使用し、適用されるシステム/TUN VPN のルートも含みます。HTTP プロキシ/PAC 設定やブラウザー専用の VPN 拡張機能は引き継ぎません。
4. 外部からのダウンロード作成には **`automation_api`** 機能が必要です。バッチ作成には `batch_downloads` も必要で、他の制限も引き続き適用されます。`features.automation_api` を含む `GET /license/policy` を確認してください。
5. API はループバック上に維持してください。汎用のリモート認証トークンはないため、ポートを LAN やインターネットに公開しないでください。ダウンロード元の認証情報は MultiRoute のサイトプロファイルに保存し、必要に応じてその `site_profile_id` を渡します。

## 2. 再利用できるプロジェクトルールを追加する

**[そのまま使える指示ブロック](examples/ai-agent-instructions.ja.md)**をプロジェクトの既存ルールにコピーします。他のプロジェクト指示を保持しながら、保存先とルートを一度置き換えてください。

| エージェント / 環境 | ルールの配置先 | 必要な接続 |
| --- | --- | --- |
| PC 上の Claude Code | プロジェクト内の `CLAUDE.md`。すでに `AGENTS.md` にルールがある場合は、2 つのコピーを管理する代わりに、既存の `CLAUDE.md` に `@AGENTS.md` を追加します。 | ローカルの shell/REST アクセス、または後述の stdio MCP サーバー。 |
| PC 上の Codex | プロジェクト内の `AGENTS.md`。 | ローカルの shell/REST アクセス、または設定済みの MCP サーバー。 |
| その他のローカルエージェントやローカルモデルのホスト | その環境の永続的なプロジェクト/システム指示。 | REST を呼び出せるツール、または stdio MCP クライアントを有効にします。モデルを読み込むだけではツールは追加されません。 |
| ローカルのエージェント実行環境を使うクラウドモデル | ローカルエージェントのプロジェクトルール。 | ローカルツールが同じ PC 上の MultiRoute を呼び出します。モデル自体をローカルで実行する必要はありません。 |
| ChatGPT Web、Claude Web、またはリモートのクラウド実行環境 | プロジェクト指示と、対応する接続済みツール。 | MultiRoute のコンピューターへのブリッジを別途設定する必要があります。同梱のローカルアダプターは stdio を使用し、公開リモート MCP サービスではありません。 |

指示ファイルは動作を導くものであり、クライアントのツール承認やサンドボックスを回避するものではありません。既存のクライアント/アカウントの制限は引き続き適用されます。公式の [Claude のプロジェクト指示](https://code.claude.com/docs/en/memory)、[Codex のプロジェクト指示](https://developers.openai.com/codex/guides/agents-md/)、[ChatGPT のカスタム MCP 接続要件](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt)を参照してください。

## 3. REST の例 — shell を使用できるローカルエージェント

エージェントにローカル端末がすでにある場合は、これが最も簡単な方法です。**MultiRoute は事前に起動しておく必要があります。** 以下は例示用のダウンロード元 URL を使ったサンプルです。URL と出力ディレクトリを置き換えてください。ジョブを作成すると、実際のダウンロードがキューに登録されます。

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'MultiRoute の automation_api が必要です' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # 配布元の信頼できる 64 桁の 16 進ハッシュ値がある場合のみ expected_sha256 を追加します。
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# $job.id を保持します。queued/running/retrying/verifying の間は再度照会します：
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / ローカル shell

以下の JSON を UTF-8 で `download.json` として保存します。`YOUR_USER` とダウンロード元 URL を置き換え、信頼できるハッシュ値がある場合は同じオブジェクトに `expected_sha256` を追加してください。

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
# features.automation_api が true であることを確認した後：
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# 例の id をレスポンスの id に置き換えます：
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` はジョブが作成されたことを意味し、ファイルの準備完了を意味するものではありません。`complete` になるまでステータスの取得を続けるか、一時停止/失敗の状態を報告します。完了時には `checksum_checking`、`integrity_status`、利用可能なハッシュ値を確認してください。同一リクエストの再試行には同じ冪等性キーを使用し、意図して新しいリクエストを作る場合は新しいキーを使用します。ポーリングは簡潔にし、適切な間隔を空けてください。大きなファイルは長時間処理が続くことがあります。パッケージマネージャーと Git は、それぞれ本来の依存関係管理/クローンのワークフローを使い続けます。

## 4. 任意：対応するローカルクライアント用 MCP ツール

MultiRoute には、Java の子プロセスとして起動する **stdio MCP アダプター**が含まれています。操作を起動中のアプリに転送します。HTTP `/mcp` エンドポイントは内蔵されていません。現在の `download_add` では**ゲートウェイの IPv4 アドレス**を明示的に指定する必要があり、REST の `direct` ルートは受け付けません。OS のルートのみを使う場合は、上記の REST を使用してください。

インストール済みの Java/JAR の正確なパスは、**設定 → MCP と CLI…** で確認できます。Windows の標準インストールでは、`%LOCALAPPDATA%/MultiRoute Downloader App` の下に `runtime/bin/java.exe` と `app/MultiRoute-Downloader.jar` があります。macOS では `/Applications/MultiRoute Downloader.app` 内の `Contents/runtime/Contents/Home/bin/java` と `Contents/app/MultiRoute-Downloader.jar` です。スペースを含むパスは引用符で囲んでください。これらのネイティブパッケージでは Java を別途インストールする必要はありません。

stdio クライアント設定の例です。2 つの絶対パスを置き換えてください。JSON の値では `%LOCALAPPDATA%`、`$HOME`、shell 変数は自動展開**されません**。

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

Claude Code では、[文書化されたプロジェクト設定](https://code.claude.com/docs/en/mcp)に従って、このエントリーを `.mcp.json` に統合します。Codex では TOML 設定の `[mcp_servers.multiroute]` に同じコマンドと引数リストを設定するか、`codex mcp add` を使用します。[公式の MCP 設定](https://developers.openai.com/codex/mcp/)を参照してください。他のホストでは各自の MCP 設定を使用します。これは設定例であり、接続がインストール済みであることを示すものではありません。

クライアントでの検出が完了したら、MultiRoute のツール一覧を表示するよう依頼してください。その後、`download_add` の引数には次のような値を使用できます。

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

続く `download_status` 呼び出しには `{"id":"012345abcdef"}` を渡します。利用できるツールは `download_add`、`download_batch`、`download_list`、`download_status`、`download_pause`、`download_resume`、`download_verify` です。アダプターのすべての操作に `automation_api` が必要です。`download_verify` は完了済みファイルを再確認します。簡潔な MCP 結果にはダウンロード元 URL、認証情報、保存先ディレクトリが含まれないため、リクエストのディレクトリを保持してください。`gateways: []` は自動ルーティングを意味しません。

## 5. クラウドチャットとリモートサンドボックス

**プロジェクト指示だけでは、ChatGPT や Claude Web をローカルアプリに接続できません。** クラウドツール内の `127.0.0.1` は、そのツールの環境を指します。ChatGPT のリモート MCP 接続には、対応するリモートサービスまたはサポートされた安全なブリッジが必要です。MultiRoute の REST URL は、そのような MCP エンドポイントではありません。このリリースには、そのリモートブリッジは付属していません。ローカルの Claude Code、Codex、その他のローカル実行環境で使われるクラウドモデルは、上記のローカル方式を利用できます。[OpenAI の接続要件](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt)。

ファイルは MultiRoute が動作するコンピューターに保存されます。リモートビルドで利用するには、承認されたファイル転送/共有ストレージの手順を別途実行する必要があります。認証情報は設定済みのサイトプロファイルに保存し、指示ブロックやチャットには記載しないでください。

## エージェントに期待される報告

「MultiRoute を使用して、設定済みのアセットフォルダーに `model.gguf` をダウンロードしました。ジョブ：`…`。状態：complete。SHA-256：…；配布元の参照値と一致しました。」信頼できる参照値が提供されておらず、見つからなかった場合は、「ローカルの SHA-256 を計算しました。配布元との照合はできません。」と報告します。アプリを利用できない場合や `pro_required` が返された場合は、その旨を報告し、黙ってダウンローダーを切り替えずに代替方法について合意してください。
