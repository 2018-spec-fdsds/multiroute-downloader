# MultiRoute Downloader 4.0 — 日本語ガイド

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md) · [Home](../README.md)

プロジェクト所有者：[2018-spec-fdsds](https://github.com/2018-spec-fdsds)。

## 1. 目的と仕組み

MultiRoute Downloader は、同じ Ethernet LAN にある複数のインターネット**ゲートウェイ**を同時に使う Java デスクトップダウンローダーです。例えば固定回線とモバイル 4G/5G ルーターを組み合わせられます。選択した利用可能なゲートウェイが共有キューからファイルの断片を受け取り、高速な回線はより多くの断片を担当します。回線が一つ切れても他は継続し、未完了の断片をキューに戻して検証済みデータを保持します。リンク一覧の一括登録と他のアプリ向けのローカル API も利用できます。

速度向上は各回線の独立した帯域、配信元の制限、安全な再開の対応状況に依存し、ルーター数に比例する保証はありません。ブラウザー用の操作画面ではなく独立したデスクトップアプリです。新しいプロファイルは英語で開始します。英語、ロシア語、ドイツ語、スペイン語、フランス語、中国語、韓国語、アラビア語、イタリア語、日本語、ヒンディー語、ベトナム語を選べます。設定と登録済みメッセージ、ログ、ヒントは言語に追従します。外部サーバーやライブラリの未登録メッセージは原文の場合があります。ファイル名、パス、URL、JSON のキーは翻訳しません。

## 2. 必要な環境

| 環境 | OS と CPU | 必要な構成 |
| --- | --- | --- |
| Windows | Windows 11 x64。ARM64 パッケージはありません | ネイティブ版に Java 21 と x64 ネットワーク部品を同梱。Npcap は別途導入 |
| Apple Silicon Mac | M1/M2/M3/M4 以降、macOS 26+ | arm64 PKG に Java とコンパイル済み部品を同梱 |
| Intel Mac | x86_64、macOS 15+ | x86_64 PKG に Java とコンパイル済み部品を同梱 |
| Linux | glibc 系 x86_64/ARM64。torrent/magnet は glibc 2.38+ | 完全な Java 17+、sudo、C コンパイラー、binutils、pkg-config、libpcap 開発ファイル。GUI は X11 または Wayland + XWayland、フォントライブラリ |

Java には `java.desktop`、`java.net.http`、`jdk.httpserver`、`java.xml` が必要で、`--headless` でも省略できません。ネイティブ Windows/macOS 版に別の Java は不要です。旧 portable ZIP 3.0 と Linux 配備は必要に応じて公式配布元から専用 Java を取得します。異なるリリースのファイルを混在させないでください。

選んだすべてのゲートウェイと同じ IPv4 サブネットの**有線 Ethernet** アダプターが必要です。各ルーターがアプリの仮想アドレスからのインターネット接続を許可する必要があります。Wi-Fi のみ、VPN のみ、IPv6 のみの構成はこの転送方式の対応範囲外です。RAM は 4 GB 以上を推奨しますが測定済みの強制下限ではなく、GPU、CUDA、AI モデルは不要です。アプリ、データ、保存先に書き込み権限が必要です。配備には少なくとも 512 MiB の空きと、実ファイル・断片・一時データの追加領域を確保します。torrent は必要な全ピース、ファイル境界、保存先とキャッシュ分を見込んでください。

選んだ文字体系を表示できるフォントも必要です。最小構成の Linux ではディストリビューションの Noto CJK（日本語・中国語・韓国語）、アラビア語やデーヴァナーガリー用 Noto などを導入してください。Java はプラットフォームの代替フォントを使用します。アプリはフォントパックを同梱・インストールせず、表示範囲は端末によって異なります。

## 3. インストールと安全確認

[リリース 4.0](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.0) に添付されている場合、対応する一つのファイルを取得し、公開 SHA-256 と照合します。

- Windows：`MultiRoute-Downloader-4.0-Windows-x64-Setup.exe`。
- Apple Silicon、macOS 26+：`MultiRoute-Downloader-4.0-macOS-arm64.pkg`。
- Intel、macOS 15+：`MultiRoute-Downloader-4.0-macOS-x86_64.pkg`。

### Windows

EXE を開いて現在のユーザー用に導入し、スタートメニューまたはデスクトップショートカットから開きます。スクリプトのコンソールではなく通常のアプリとして動作します。独自セットアップ画面は英語、OS の安全確認は Windows の言語です。

初回起動で Npcap を確認します。ない場合はアプリ内でダウンロードに同意すると、指定された公式インストーラーの SHA-256 と Authenticode 発行者を確認してから通常の Npcap セットアップを開きます。ライセンスを読み、標準 UAC 要求を承認します。再起動を要求されたら Windows を再起動してください。Npcap は同梱もサイレント導入もしません。未導入の場合、この手順にはインターネットが必要です。AdminOnly 版ではキャプチャ権限の追加確認が出ることがありますが、Java 全体を管理者として実行しないでください。

アプリと EXE に発行者署名はありません。SmartScreen が不明な発行者を示した場合、公式配布元とハッシュを確認してから、提示される場合に限り「詳細情報 → 実行」を利用するか判断します。マルウェア検出は別の問題です。ウイルス対策を無効にしたり除外登録したりしないでください。

### macOS

CPU に対応する PKG を Apple Installer で開き、保護されたネットワーク部品の導入を管理者確認で承認します。その後 `/Applications/MultiRoute Downloader.app` を Finder、Spotlight、Dock から開きます。このパッケージには別 Java、Terminal、Command Line Tools、Homebrew は不要です。

Ethernet BPF デバイスを開く際、標準 macOS 管理者確認が表示されます。部品はキャプチャを開いた後に権限を下げ、Java は通常ユーザーで動作します。PKG とアプリは **Developer ID 署名・公証なし**です。開発元を確認できない警告で例外操作が提示される場合、配布元と SHA-256 を確認し、[Apple の手順](https://support.apple.com/en-us/102445) に従い「システム設定 → プライバシーとセキュリティ → このまま開く」から確認します。管理対象 Mac は例外を禁止する場合があります。

インストール承認、ネットワーク部品の承認、不明な開発元の例外は別の確認です。必要に応じてローカルネットワークと実際の保存フォルダーへのアクセスを許可します。Gatekeeper/SIP を無効にせず、隔離属性を一括除去せず、Java 全体を root にしないでください。マルウェアや破損ファイルの警告なら中止して確認します。

### Linux と portable ZIP 3.0

完全な ZIP を展開します。Linux は初回 `bash ./Deploy-Linux.sh`、以後 `bash ./Start-Linux.sh`。Windows portable は `Deploy-Windows.cmd`、以後 `Start-Windows.cmd`。macOS portable は `Deploy-macOS.command` と `Start-macOS.command`。旧 ZIP はスクリプト方式であり、4.0 ネイティブ版とは別です。Linux は配布元のコンパイラー/libpcap 依存関係、macOS portable は Apple ツールや sudo を要求する場合があります。必要な承認ターミナルを維持し、Java 全体を root で動かさないでください。Alpine/musl は含まず、配備は OS や glibc を置換しません。

12 言語の説明書をオフラインで収録します。Windows は `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`、macOS はアプリの「パッケージの内容を表示」から `Contents/app/docs/ja.md`。ブラウザーのバージョン別説明リンクにはインターネットが必要です。

## 4. ゲートウェイと仮想 IP

初期ゲートウェイは `192.168.1.1`、`192.168.1.2`、`192.168.1.3`、`192.168.1.4` の四つで、すべて選択済みです。変更・追加して **1–32** 個の異なるアドレスを使えます。PC が一つを既定経路にしていても、五つ目の回線が増えるわけではありません。

アプリの仮想 IPv4 は初期値 `192.168.1.70`。ネットワーク設定で未使用を確認して適用し、DHCP 自動割当の範囲外に予約します。内部転送専用で、**OS アダプターへのアドレス追加やシステム経路表の変更は行いません**。他のアプリは従来の接続を使います。OS のネットワーク画面で手動追加しないでください。隣の「?」とヒントを参照します。

ゲートウェイを検査して一つ・複数・すべてを選択します。速度テストは **16 MiB または 5 秒**までで、接続準備に追加時間がかかる場合があります。通常ダウンロードの上限ではありません。適切な直接 HTTP URL がなければ Selectel のテストファイルを使います。

転送中もプロファイルの追加/削除や作業の選択を変更できます。ただし未完了作業ごとに一つ以上のゲートウェイを残します。追加プロファイルは既存作業に自動選択されません。仮想 IP 自体を変更する前に活動中の作業を一時停止します。MAC アドレスは UI から隠しています。

## 5. ダウンロードと画面操作

1. 長い URL 欄に Ctrl+V、macOS は Cmd+V、または貼り付けボタンで入力します。magnet と `.torrent` も利用できます。
2. OS のフォルダー選択で保存先を指定します。既定値は Windows の `%USERPROFILE%\Downloads`、macOS/Linux の `~/Downloads`。手動指定は保持します。
3. ゲートウェイを選び URL の横の開始を押します。Hugging Face、WebDAV、Metalink はファイルをプレビュー選択できます。一括登録は 1 行 1 URL またはテキストファイルです。
4. 各作業の進捗、経過/残り時間、全体と各ゲートウェイの速度/転送量を確認します。HTTP 断片は標準 **20 MiB**、1–64 MiB に変更可能です。**60 秒**データが来ない断片は中止して共有プールに戻し、他ゲートウェイに渡します。torrent ピースサイズはメタデータによります。
5. 一時停止/再開、優先度、速度制限、予約、停止作業のフォルダー変更ができます。既定の同時作業は 2 件。再起動時の活動作業は停止状態で復元します。torrent シードは標準無効で、時間/比率の上限を設定できます。

FlatLaf のライト/ダーク/システムテーマを保存できます。サマリーはフィルターに関係なく全件、活動中、完了と全作業合計速度を示します。個別進捗バー、全幅列、ソート、検索、状態フィルター、読み位置を保つスクロールを利用できます。

進行中/完了行にカーソルを置くと**完全なファイル名と絶対保存パス**を表示します。ソートやフィルター後も正しい行に対応し、選択ファイル見出しにも同じヒントがあります。右クリックで完全パスをコピー。Ctrl+L/Cmd+L は URL、Ctrl+F/Cmd+F は検索、Escape は検索解除です。

選択作業の「詳細」と「履歴」を切り替えられます。レポートは更新・言語変更でも閲覧位置と選択を保持し、別作業では先頭を開きます。全体ログはアプリイベントとダウンロード履歴を分離し、末尾では新規イベントに追従、過去を読む際は強制移動しません。

窓を閉じてもトレイで継続します。アイコンは全作業の合計速度を 10 Gbit/s まで表示し、ヒントで正確な値を示します。トレイの「開く」で復帰、「終了」で保存・終了します。トレイ非対応ならタスクバーに最小化します。**更新前はトレイから終了**してください。

削除時のファイル削除チェックは**標準オフ**です。一覧だけから消して完了/部分ファイルを残せます。必要時だけファイル削除を選びます。作業やファイルを削除しても永続履歴は消えません。

## 6. プロトコルと整合性

| ソース | 動作・条件 |
| --- | --- |
| HTTP/HTTPS | 安全な識別と byte ranges がある場合に断片再開。なければ単一ゲートウェイの全体ストリームで切断後は最初から |
| Hugging Face | ファイル/リポジトリ選択と Hub/Xet 解決。非公開や制限付きは権限が必要 |
| FTP | パッシブ EPSV/PASV。平文 FTP は認証情報やデータを暗号化しません |
| FTPS | 制御/データに明示的 TLS。暗黙 FTPS は非対応 |
| SFTP | 独立に確認・固定した SHA256 SSH ホスト鍵指紋が必要。不明な鍵は自動受理しません |
| WebDAV | HTTPS 一覧とファイル選択 |
| Metalink | `.meta4`、信頼する SHA-256、ミラーと提供されたピースハッシュ |
| Torrent/magnet | 選択ゲートウェイで転送しメタデータによるピース検証が必須。選択/優先度に対応 |

配布元アクセスにユーザー/パスワード、Bearer token、Cookie、許可されたヘッダーを正確な origin ごとに暗号化保存できます。ルーター用 token ではありません。URL にパスワードを含めないでください。信頼できる公開/手動 SHA-256 または SHA-512 がない FTP/FTPS/SFTP は全体ストリームを使います。

組み立て後に一回の読み取りで **MD5、SHA-256、SHA-512** を計算します。主欄は 32/64/128 桁の 16 進数で種別を自動判定し、追加参照も設定可能。全体表現の摘要ヘッダー、`filename.md5`、`MD5SUMS`、`filename.sha256`、`SHA256SUMS`、`filename.sha512`、`SHA512SUMS`、Hugging Face LFS/Metalink を利用します。部分 HTTP 応答の摘要は全体の値としません。

既知参照は最終ファイル公開前にすべて一致が必要で、不一致なら部分データを保持して失敗します。参照がなければローカル計算値であり、公開元との一致確認ではありません。MD5 は偶発的破損用で、強い識別や SHA-256/SHA-512 の代用にはなりません。torrent ピース検証と全体ハッシュは別で、手動全体参照は選択ファイル一つの場合だけです。

<a name="multiroute-api"></a>

## 7. 他のアプリから API を使う

実行中は `http://127.0.0.1:8765/api/v1` にローカル API があります。JSON UTF-8、本文に `Content-Type: application/json` を指定します。ローカル作業管理に API token は不要で、配布元認証は別です。同じ PC の接続だけを受け付け、LAN/インターネットに公開しないでください。`--headless` は API 専用です。ネイティブ起動器を使い、`.cmd` 例は portable 用です。

確認は `GET /api/v1/health`、通常 `{"ok":true,"version":"4.0"}`。先に画面で設定するか `POST /api/v1/network` に次を送ります。

```json
{
  "virtual_ip": "192.168.1.70",
  "gateways": ["192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"]
}
```

`POST /api/v1/jobs` の例です。URL と実際の保存先、必要ならゲートウェイを変更します。Linux は `/home/your-user/Downloads`、Mac は `/Users/your-user/Downloads`。`routes` は明示し、画面のチェックを自動継承しません。

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

`HTTP 201` は登録成功で、完了ではありません。`id` を保持して `GET /api/v1/jobs/{id}` の `status: complete` を確認し、失敗時は `error` を読みます。`speed_bps` は**バイト/秒**、8 倍して 1,000,000 で割ると Mbit/s です。

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

停止/再開は `{}`。削除本文、既定のファイル保持、`Idempotency-Key`、キュー、一括、SSE、認証、制限の詳細は [OpenAPI](../openapi.yaml)。`expected_md5`、`expected_sha256`、`expected_sha512` を指定でき、結果は `md5`、`sha256`、`sha512`、`checksum_sources` と警告を返します。JSON キー/状態は言語不変。[追加例](../API.ru.txt) はロシア語です。

Python 3 例です。URL を置換し、先にネットワークを設定してアプリを開いたまま実行します。UTF-8 は日本語パスも保持します。

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

## 8. 履歴とデータの保存・更新

| 環境 | データ | アプリ/runtime |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

ネイティブ Mac 部品は `/Library/PrivilegedHelperTools/MultiRouteResume/4.0/gateway-net`。Windows portable 3.0 は起動元の `data/` で、`data-location.config` または `--data-dir` で変更可能。Linux の専用 Java は `~/.local/share/MultiRouteResume/runtime`、保護部品は `/usr/local/libexec/MultiRouteResume/3.0/gateway-net`。実ファイルはユーザーが選んだ別の場所にも保存されます。

`logs/downloads-history.jsonl` は UTF-8 の行単位 JSON、`logs/downloads-history.log` は日時付きテキストです。再起動をまたいで追記し、作業削除や言語変更で過去を書き換えません。テキストは記録時の言語、画面は現在の言語です。表示は最大直近 1,000 イベント/2 MiB ですが完全な履歴はディスクに残って増加します。必要なら別途アーカイブしてください。名前と絶対パスを含み、配布元 URL、パスワード、token を含みません。共有前に個人パスを確認します。

`lifecycle_history` の項目は `event_id`、`job_id`、`kind`、`time`、`filename`、`path`、torrent では `paths`、一時停止では `reason`（`pause`、`shutdown`、`maintenance`）を持つ場合があります。種類は `started`、`resumed`、`paused`、`completed`、`recovered`、`failed`。ISO 8601 UTC（`2026-10-18T12:34:56.123Z`）で記録し、テキストには現地日付・時刻・UTC オフセットを表示します。任意の `first_started_at`、`last_resumed_at`、`completed_at` は実際に観測した開始/再開/検証と公開後の完了です。キュー登録は開始でなく、古い日時を推測しません。`recovered` は旧公開ファイルの現在の検証で、過去の完了時刻が判明した意味ではありません。

更新前にトレイから終了し完全な新パッケージを導入し、履歴・断片を保持します。Windows は設定のインストール済みアプリから削除。Mac の `.app` 削除だけでは履歴/保護部品は消えません。アンインストールのために保存先全体を消さないでください。Linux の場所を移したら再配備します。

## 9. 問題解決と検証の範囲

- 起動失敗：版、OS/CPU、完全なパッケージと Java モジュール、`JAVA_EXE`/`JAVA_HOME` を確認します。3.9 の一時出力ロック対策と既存インスタンスの識別は保持しています。
- 速度ゼロ：有線アダプター、サブネット、未使用の仮想 IP、ゲートウェイ/配布元の権限を確認。速度テスト成功はすべての実ダウンロードの保証ではありません。
- torrent の glibc/macOS 非対応：対応 OS を用い、システムライブラリを手動置換しません。
- SFTP 鍵不一致：管理者に確認し、鍵検証を無効にしません。
- 他アプリが 8765 を使用：`--port 8766`。無関係なプロセスを停止せず、確認済み MultiRoute だけを置換します。
- 容量/権限：ファイル・断片・キャッシュを確認し、フォルダー変更前に停止。同じ保存先の作業を重複作成せず再開します。

版、OS/CPU、段階、正確なエラーを報告し、秘密情報、token、非公開 URL、個人情報を除きます。管理された Windows/POSIX テストや Mac CI、パッケージ検査は、実ルーター速度/故障切替やユーザーの導入、Finder/Gatekeeper/BPF 承認を証明しません。署名・公証なしの**プレリリース**です。既知の小ファイルを各ゲートウェイ、次に組み合わせで試してください。

## 10. ライセンス

原版は個人・組織・業務で無料利用できます。別途許可なくアプリ販売、ライセンス課金、有償配布は禁止です。独自ライセンスは一般的な改変・再配布権を与えず、OSI 承認オープンソースではありません。英語 [LICENSE.txt](../LICENSE.txt) が優先します。FlatLaf（Apache-2.0）など第三者は各ライセンスを保持します。所有者：[2018-spec-fdsds](https://github.com/2018-spec-fdsds)。

## 名称と既存プロファイル

MultiRoute Downloader の旧名称は MultiRoute Resume です。新規プロファイルは上記新パスを使いますが、新しいデータフォルダーがなく旧プロファイルがあれば、ファイルを移動/削除せず再利用します。旧データは `%LOCALAPPDATA%\MultiRoute Resume\data` または `~/Library/Application Support/MultiRoute Resume/data` に残る場合があります。保護フォルダー `MultiRouteResume`、Windows 更新 ID、内部 API 識別とポートは互換性を維持します。導入後は MultiRoute Downloader の新しいショートカットやアプリを利用します。旧リリースのファイル名は変わりません。

## 保存したサイト、アップロード、アーカイブのプレビュー（4.0）

**サイトプロファイル…** で作成・編集し、**ダウンロード** または **アップロード** の一覧から選択します。ゲートウェイと仮想 IP は **ネットワーク設定** にあります。**アップロード** では**リモートフォルダー URL** を入力し、**ファイルを選択してアップロード…** で OS の選択画面から一つ以上のローカルファイルを選びます。画面は名前を追加しますが、API `target_url` には完全なリモートファイル URL を指定します。アーカイブ URL を主入力欄に貼り **アーカイブの内容…** を押すと、ダウンロードせずにプレビューします。

サイトの名前、基本 URL、ローカル保存先、リモートフォルダー、ゲートウェイをプロファイルに保存できます。匿名、HTTP Basic/Bearer/Cookie/独自ヘッダー、FTP、確認済み SHA-256 ホスト鍵を使う SFTP に対応し、同一サーバーの複数アカウントを分離します。明示した保存先/ゲートウェイが優先し、相対 URL は基本 URL から解決されます。秘密情報は所有者限定の `site-profiles/vault.bin` にローカル鍵 `site-profiles/key.bin` で暗号化されますが、アカウントと鍵を制御できる人には読み取れます。公開 API は秘密を返しません。空欄は接続元と認証方式が同じ場合にだけ既存情報を保持し、`replace_credentials:true` は置換します。異なる接続元への HTTP リダイレクトには認証情報を渡しません。プロファイル削除はジョブやローカル/リモートファイルを削除しません。ジョブは元のプロファイルとの結び付きを保持し、アカウントを変更できません。元を削除した場合、必要に応じて有効なプロファイルでジョブを作り直してください。

アップロードとリモート参照は選択ゲートウェイ経由の **FTP、明示的 FTPS、SFTP** に対応します。通常のローカルファイルと、フォルダーだけでない完全なリモートファイル URL（`target_url`）を指定します。一時ファイルを使い、既存の先頭部分を比較してから再開します。成功にはリモート全体を読み戻して SHA-256 を確認するため、**読み取り・書き込み・名前変更**権限と追加通信量が必要です。不一致や読み取り拒否は失敗です。各ファイルは一度に一つのゲートウェイを使い、複数ファイルは分散して並列送信します。無通信期限後は別ゲートウェイへ切り替えられますが、一つの送信ファイルを並列分割しません。一時停止・再開・進捗は再起動後も保持されます。ジョブ削除でもローカルファイルとリモートの完成/一時ファイルは残ります。

上書きは初期状態では無効です。FTP/SFTP には「宛先がなければ原子的に名前変更」が常に備わるとは限らず、公開時に他のクライアントとの競合があり得ます。共有する宛先を調整してください。フォルダー参照は送信も削除もしません。

**ZIP、TAR、7Z、RAR4、RAR5** の名前、パス、サイズ、フォルダー/暗号化情報を、展開や実行なしで表示します。ゲートウェイ、バイト範囲、安定した ETag または Last-Modified が必要です。メタデータ 16 MiB、10,000 件、60 秒、7z ヘッダー解読メモリ 64 MiB の制限があります。一覧は省略される場合があり、内容の整合性確認ではありません。文字化けには元の文字コードを指定します。対応する暗号化 7z ヘッダー用パスワードは保存しません。暗号化 RAR ヘッダーは未対応/パスワード必要と報告され、分割アーカイブは一部だけの場合があります。

API はダウンロード、送信、参照、プレビューで `site_profile_id` を受け付けます。`GET /api/v1/site-profiles` は `{profiles:[...]}`、`POST`/`PUT` は平坦なプロファイルの保存と公開項目の返却、`DELETE /api/v1/site-profiles/{id}` はプロファイルだけの削除です。`POST /api/v1/uploads` は `local_path`、`target_url`、`routes`、任意の `site_profile_id`、`overwrite:false` を受け、HTTP 201 を返します。GET は `{uploads:[...]}`、個別 GET/DELETE、POST `/{id}/pause` / `/{id}/resume` も利用できます。`POST /api/v1/remote/list` は `url` と `route`/`routes`、`POST /api/v1/archives/preview` はさらに任意の `password`、`charset` を受け付けます。[openapi.yaml](../openapi.yaml) を参照してください。`data/uploads/` の UTF-8 マニフェストはパスワードではなくプロファイル ID を保持します。プレビューはダウンロードを作成しません。

## ダウンロードの予定と優先度

キュー/予定で `max_active_jobs`（1–32）、全体/ゲートウェイ速度制限、毎日の時間帯（`schedule_enabled`、`schedule_start`、`schedule_end`、HH:mm）を設定します。PC の現地時刻で、API は `schedule_timezone` を返します。日付をまたげます。開始と終了が同じなら終日です。時間外はキューに戻り、手動一時停止は維持されます。`priority` -100〜100 は待機中を高い順、同値なら作成順に開始し、実行中を中断しません。`start_after_ms` は最早開始の Unix ミリ秒で、0 は遅延解除です。設定は再起動後も保存され、独立したアップロードキューには適用されません。アプリと PC を起動してスリープさせないでください。OS の自動復帰機能ではありません。API：`GET`/`PUT`/`POST /api/v1/queue`、`PUT`/`POST /api/v1/jobs/{id}/options`。

## Chrome と Edge の連携

アプリの **Chrome / Edge** 連携でリンク送信を許可し、拡張フォルダーを開きます。一度 `chrome://extensions` または `edge://extensions` のデベロッパーモードを有効にし、「パッケージ化されていない拡張機能を読み込む」で `manifest.json` のある `browser-extension` を選びます。インストーラーは JAR の隣に含めます。ソースの [browser-extension/](../browser-extension/) も利用でき、選択後は移動しないでください。別の `MultiRoute-Downloader-4.0-Chrome-Edge.zip` は先に展開します。ストア公開版ではありません。

MultiRoute はトレイでもよいので実行しておきます。リンクの右クリックメニューまたは拡張ポップアップから URL を送ります。現在の保存先、ゲートウェイ、サイト設定を使い、必要なリポジトリ/torrent 選択は表示されます。既定ポート 8765 は API に合わせて変更できます。URL だけを送信し、Cookie/パスワードをコピーせず、他のブラウザーダウンロードを横取りしません。認証はアプリ側で設定します。ローカル接続の許可はこの選択した拡張だけに与えてください。

連携は初期状態で無効です。`chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` の `/api/v1/browser/status` と `/api/v1/browser/download` だけを許可し、他の API の制限は維持します。HTTP 202 は画面への受け渡しで、完了ではありません。headless サーバーだけでは選択画面を開けません。ブラウザーから拡張を削除するか連携を無効にできます。

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
