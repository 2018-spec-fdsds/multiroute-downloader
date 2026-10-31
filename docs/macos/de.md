# macOS: Installation, Start und Berechtigungen

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · **Deutsch** · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## Installieren und öffnen

Beenden Sie vor einem Update laufende Übertragungen und wählen Sie im MultiRoute-Menü der Menüleiste **Quit/Exit**. Die rote Fenstertaste kann die App nur ausblenden. Laden Sie das passende PKG für Apple Silicon oder Intel von den [Downloads](../../downloads/README.md), prüfen Sie SHA-256 und öffnen Sie es. Bestätigen Sie die Installation im macOS-Administratordialog. Starten Sie danach `/Applications/MultiRoute Downloader.app` über Finder oder Spotlight; Java oder Terminal sind nicht separat nötig.

## Öffnen bestätigen

Kann macOS den Entwickler nicht bestätigen, versuchen Sie zuerst PKG/App zu öffnen. Für eine vertrauenswürdige, geprüfte Datei: **Systemeinstellungen → Datenschutz & Sicherheit → Open Anyway → Open**; Beschriftungen variieren. Bei einer Schadsoftware- oder Beschädigungswarnung stoppen und Datei prüfen. [Apple-Anleitung](https://support.apple.com/en-us/102445).

## Netzwerk und Passwort

Beim ersten App-Start oder beim Anwenden von virtueller IP, DHCP oder Anwendungs-MAC kann der Netzwerkhelfer Administratorrechte für Ethernet BPF benötigen. Ein Neustart oder geänderte Konfiguration kann erneut fragen. Gesunde Wiederholungsprüfungen und Geschwindigkeitstests verwenden den vorbereiteten Helfer; kein Passwort je Datei oder Gateway. OS-Routen und physische MAC bleiben unverändert.

Falls eine **Local Network**-Abfrage erscheint, erlauben Sie den erkannten MultiRoute-Eintrag unter Datenschutz & Sicherheit → Lokales Netzwerk. Nicht jede Installation zeigt diese Abfrage. [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

Das Mac-Administratorpasswort gehört ausschließlich in den Systemdialog, nicht in Lizenz-/Hugging-Face-Felder. Als Aufrufer kann `DesktopLauncher`/Java erscheinen; bestätigen Sie nur Ihre erwartete MultiRoute-Aktion. Nach Abbruch ausdrücklich erneut prüfen und das Protokoll lesen. Gatekeeper/SIP nicht global deaktivieren.

[MultiRoute Downloader — Deutsch](../de.md) · [macOS — English / 12 languages](README.md)
