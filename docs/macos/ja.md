# macOS：インストール、初回起動と権限

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · **日本語** · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## インストールと起動

更新前に転送の完了を待ち、メニューバーの MultiRoute メニューから **Quit/Exit** を選びます。赤い閉じるボタンはウィンドウを隠すだけの場合があります。[ダウンロード](../../downloads/README.md)から Apple Silicon または Intel 用 PKG を選び、SHA-256 を確認して開きます。macOS の管理者システム画面でインストールを承認します。Finder または Spotlight から `/Applications/MultiRoute Downloader.app` を起動してください。別の Java やターミナルは不要です。

## 開くことを許可

開発元を確認できない場合、まず PKG やアプリを開こうとしてください。確認済みで信頼できるファイルに限り、**システム設定 → プライバシーとセキュリティ → Open Anyway（このまま開く）→ Open（開く）** を使います。名称は環境で異なります。マルウェアや破損の警告なら中止してファイルを確認してください。[Apple の案内](https://support.apple.com/en-us/102445)。

## ネットワークとパスワード

アプリの初回起動時や仮想 IP、DHCP、アプリ MAC の適用時には Ethernet BPF を開く管理者確認が必要な場合があります。設定変更や部品の再起動で再確認される場合もあります。通常の再確認や速度テストは準備済み部品を使い、ファイルやゲートウェイごとにパスワードを求めません。OS の経路や物理 MAC は変えません。

**ローカルネットワーク**の要求が出た場合は、プライバシーとセキュリティ → ローカルネットワークで確認できる MultiRoute 項目を許可します。すべての環境で要求が出るとは限りません。[Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy)。

Mac 管理者パスワードはシステム画面にだけ入力し、ライセンスや Hugging Face 欄には入力しません。要求元が `DesktopLauncher`/Java と表示される場合もあるため、自分が開始した信頼できる MultiRoute 操作だけ承認します。取り消したら明示的に再確認しログを読んでください。Gatekeeper/SIP を全体で無効化しないでください。

[MultiRoute Downloader — 日本語](../ja.md) · [macOS — English / 12 languages](README.md)
