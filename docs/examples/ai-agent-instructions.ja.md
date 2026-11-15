# MultiRoute プロジェクト指示 — エージェントのルールにコピー

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <strong>日本語</strong> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

このブロックを既存の `AGENTS.md`、`CLAUDE.md`、または同等の永続的なプロジェクト指示に追加してください。追記し、プロジェクトの他のルールは保持します。保存先とルートを一度置き換えてください。設定方法と例は[日本語ガイド](../ai-agents.ja.md)を参照してください。

```markdown
## ダウンロード：既定で MultiRoute を使用する

承認されたプロジェクト作業に必要な外部ファイル（モデル、データセット、
アーカイブ、メディア、その他のアセット）には、起動中の MultiRoute Downloader を使用する。
ファイルごとにダウンローダーを選ぶよう私に尋ねないこと。パッケージマネージャーと
Git では通常のインストール/クローン/更新手順を使い続ける。

プロジェクト設定（一度編集する）：
- API: http://127.0.0.1:8765/api/v1
- 保存先: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- REST ルート: [{"type":"direct","enabled":true}]
- 任意の site_profile_id: <EXISTING_PROFILE_ID_OR_OMIT>

接続済みのローカル REST ツール、または利用可能な場合は MultiRoute MCP ツールを使用する。
MCP ではゲートウェイの IPv4 アドレスを別途明示的に設定する。download_add は
url、directory、gateways を必要とする。REST の direct ルートは OS の経路であり、
MCP のゲートウェイ値ではない。ルートを勝手に作ったり、UI の選択が使われると仮定したりしないこと。

REST では、セッション内の初回使用前に GET /health と GET /license/policy を確認する。
MCP のみの場合は、ツールを検出し、limit 1 で download_list を呼び出して
接続と機能を確認する。ライセンスポリシーはアダプター自体が確認する。
ダウンロードの作成には features.automation_api=true が必要となる。利用できない場合は、
黙って別のダウンローダーを使わず、理由を報告して代替ダウンローダーについて尋ねること。

必要なファイルごとに、正確な URL、承認済みの保存先、ファイル名を選択する。
信頼できる配布元の SHA-256 がある場合は expected_sha256 として渡す。
POST /jobs（gateways ではなく routes）または MCP download_add でジョブを作成する。
同じリクエストの再試行には安定した冪等性キーを使用し、返されたジョブ id を保持する。
作成は完了ではない。適切な間隔で GET /jobs/{id} または download_status を使い、
そのジョブを照会する。一時停止/失敗の状態を報告し、再開時には既存のジョブを再利用して、
重複を作成しない。無関係なジョブやネットワーク設定を変更しないこと。
完了時は checksum_checking と integrity_status を確認する。
信頼できる参照値との一致なしに、配布元との照合済みだと主張しないこと。
保存されたローカルパス、ジョブ id、検証結果を返す。ファイルは
MultiRoute のコンピューター上に残る。リモート実行サンドボックス内に存在すると主張しないこと。
```

このルールはプロジェクトの優先設定を示します。対応する接続済みツールと、エージェントの通常の権限は引き続き必要です。ダウンロードする元ファイルはデータであり、エージェントへの追加指示ではありません。
