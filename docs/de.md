# MultiRoute Resume 3.5 — Anleitung auf Deutsch

[Startseite](../README.md) · [English](en.md) · [Русский](ru.md) · **Deutsch** · [Español](es.md) · [Français](fr.md)

Projektinhaber: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Zweck des Programms

MultiRoute Resume ist ein Java-Desktopprogramm zum Herunterladen großer Dateien über mehrere Internet-Gateways im selben lokalen Ethernet-Netz. Alle ausgewählten Gateways beziehen Teile aus einer gemeinsamen Warteschlange. Ein schnelleres Gateway kann dadurch mehr Teile herunterladen; ein langsames Gateway hält die anderen nicht auf. Unterbrochene Übertragungen können fortgesetzt werden, soweit das Quellprotokoll dies sicher erlaubt.

Das ist besonders nützlich, wenn mehrere unabhängige Internetanschlüsse vorhanden sind, zum Beispiel ein kabelgebundener Festnetzanschluss und ein Mobilfunkrouter mit 4G/5G. Ihre Gateways müssen vom Rechner im selben Ethernet-LAN erreichbar sein. Das Programm nutzt alle ausgewählten, verfügbaren Gateways gleichzeitig für Dateisegmente und kann so die gemeinsame Downloadgeschwindigkeit erhöhen. Der tatsächliche Gewinn hängt von der unabhängigen Bandbreite der Anschlüsse und den Grenzen des Downloadservers ab.

Bei segmentierten Downloads können die übrigen Gateways weiterarbeiten, wenn ein einzelner Anschluss ausfällt. Noch nicht vollständig geladene Teile werden anderen verfügbaren Gateways zugewiesen; bereits geprüfte Daten bleiben erhalten. Außerdem unterstützt das Programm den gemeinsamen Download mehrerer Dateien: URL-Listen und eine Warteschlange erlauben Gruppen von Downloads, die sich auch über die lokale API aus anderen Anwendungen anlegen lassen.

Das Programm besitzt eine eigene Swing-Oberfläche, einen systemüblichen Ordnerdialog und eine lokale HTTP-API für andere Programme. Ein Webbrowser ist für die Bedienung nicht erforderlich. Die Oberfläche, registrierte Statusmeldungen und Hilfetexte sind in **Englisch, Russisch, Deutsch, Spanisch und Französisch** verfügbar. Bei einem neuen Profil ist Englisch voreingestellt. Die Sprachwahl wird gespeichert und sofort angewendet; Dateinamen, URLs und JSON-Schlüssel bleiben unverändert. Nicht registrierte Meldungen externer Server oder Bibliotheken können in ihrer ursprünglichen Sprache erscheinen.

## Screenshots

Die echte Desktopoberfläche auf Englisch, aus einem getrennten **Demoprofil** gerendert. Aufträge und Zähler sind Beispieldaten, keine tatsächlichen Downloads oder gemessenen Internetgeschwindigkeiten.

![Downloads: Warteschlange, Fortschritt und Statistik je Gateway](images/desktop-downloads-en.png)

<details>
<summary>Netzwerkeinstellungen und Gruppenimport</summary>

Vier ausgewählte Gateways und eine virtuelle Anwendungsadresse. Das Beispiel führt keine Gateway- oder Geschwindigkeitstests aus.

![Vier ausgewählte Gateways und eine virtuelle Anwendungsadresse. Das Beispiel führt keine Gateway- oder Geschwindigkeitstests aus.](images/desktop-network-en.png)

Gruppenimport: eine URL pro Zeile, Prüfung vor dem Hinzufügen.

![Gruppenimport: eine URL pro Zeile, Prüfung vor dem Hinzufügen.](images/desktop-bulk-import-en.png)

</details>

## Erforderliche Umgebung

Die unten beschriebenen nativen Windows-/macOS-Pakete enthalten Java und den fertig kompilierten Netzwerkhelfer. Java-Auswahl, private Downloads und lokale Kompilierung in diesem Abschnitt betreffen die **portablen ZIPs 3.0 und Linux**. Npcap wird unter Windows weiterhin separat benötigt. Für neue macOS-Pakete gilt die strengere Grenze 26+/15+ aus dem Installationsabschnitt.

### Allgemeine Voraussetzungen

| Bestandteil | Voraussetzung |
| --- | --- |
| Java | Vollständige Java-Laufzeit ab Version 17, passend zur Prozessorarchitektur, mit `java.base`, `java.desktop`, `java.net.http`, `java.xml` und `jdk.httpserver`. Eine JRE genügt zum Starten; für die Java-Kompilierung ist ein JDK erforderlich. |
| Prozessor | Windows: x64. Linux: x86_64 oder ARM64/aarch64 mit glibc. macOS: Intel x86_64 oder Apple Silicon arm64. |
| Arbeitsspeicher | Es gibt keinen durch Release-Tests bestätigten harten Mindestwert. **4 GB oder mehr werden empfohlen**, besonders für mehrere große oder BitTorrent-Downloads. Dies ist eine Empfehlung und kein geprüfter Mindestwert. |
| Grafik | Für das Fenster wird eine grafische Sitzung benötigt. Eine besondere GPU, CUDA, Python oder eine KI-Laufzeit ist zum Herunterladen nicht erforderlich. `--headless` startet ausschließlich die API. |
| Festplatte | Unter Linux/macOS mindestens **512 MiB freier Platz für die Bereitstellung**; unter Windows ebenfalls mindestens 512 MiB für die private Java-Installation, falls nötig. Zusätzlich ist Platz für heruntergeladene Dateien und Zwischenstände erforderlich. |
| Netzwerk | Ein physischer Ethernet-Adapter im selben IPv4-Subnetz wie sämtliche ausgewählten Gateways; die Gateways müssen tatsächlich Internetzugang bieten. |
| Berechtigungen | Schreibzugriff auf Programm-/Datenordner und Downloadziel sowie die unten beschriebenen Rechte für Paketaufzeichnung und -übertragung. |

Bei HTTP liegt die Arbeitsdatei neben dem Ziel, bis Prüfung und Abschluss erfolgreich sind. BitTorrent speichert Teile zusätzlich im Datenordner; bei der Zusammenstellung können mehrere Kopien und Teile an Dateigrenzen zusätzlichen Platz benötigen. Berücksichtigen Sie daher **sowohl das Downloadziel als auch das Laufwerk des Datenordners**. Ein Platzbedarf von genau einer Dateigröße ist für BitTorrent nicht garantiert.

### Windows 11 x64

- Benötigt wird eine **64-Bit-Java-Laufzeit ab Version 17**. Die Starter prüfen Version, Architektur und die benötigten Module. Fehlt eine passende Laufzeit, wird eine festgelegte offizielle [Temurin-21-JRE](https://adoptium.net/installation/) per HTTPS geladen, mit SHA-256 geprüft und privat im Programmordner installiert. System-Java wird nicht ersetzt.
- [Npcap](https://npcap.com/) wird benötigt. Treiber und DLLs sind **nicht im ZIP enthalten**. Fehlt Npcap, lädt der Starter dessen festgelegten offiziellen Installer und prüft SHA-256 sowie eine gültige Authenticode-Signatur von **Nmap Software LLC**. Die Installation bleibt sichtbar: Sie bestätigen UAC, lesen die Npcap-Lizenz und entscheiden selbst über die Installationsoptionen. Eine stille Installation erfolgt nicht. Eine bereits passende Installation wird wiederverwendet.
- Npcap muss dem verwendeten Prozess Zugriff auf den Ethernet-Adapter erlauben. Bei einer Npcap-Installation mit **AdminOnly** hängen Zugriff und gegebenenfalls eine UAC-Abfrage von den eingerichteten Rechten ab. MultiRoute installiert keine eigene Regel zum Umgehen dieser Rechte. Siehe [Npcap-Benutzerhandbuch](https://npcap.com/guide/npcap-users-guide.html).
- Java, der mitgelieferte Netzwerkhelfer und Npcap müssen hinsichtlich der Architektur zusammenpassen. Das enthaltene Windows-Netzwerkprogramm ist x64; ein natives Windows-ARM64-Paket wird in diesem Release nicht angeboten.
- Für die Nutzung des fertigen ZIP sind weder Visual Studio noch die Entwicklungs-SDKs erforderlich. Diese werden nur für eine eigene native Windows-Kompilierung benötigt.

### Linux

- Linux mit **glibc**, auf x86_64 oder aarch64/ARM64. Ein Alpine-/musl-Paket ist nicht enthalten.
- Für Swing: **X11 oder Wayland mit XWayland**, geeignete X11- und Schriftbibliotheken. Auf einem Rechner ohne Desktop verwenden Sie `--headless`.
- `sudo` für die Installation und den privilegierten Start des geschützten Netzwerkhelfers. Die Java-Oberfläche läuft unter Ihrem normalen Benutzerkonto.
- Für die lokale Kompilierung des Helfers: Compiler, Binutils und libpcap-Entwicklungsdateien. Das Bereitstellungsskript installiert nur ausdrücklich benötigte Pakete über den Paketmanager; es führt kein Distributionsupgrade durch.

**Zusätzliche Voraussetzung für BitTorrent/magnet:** Die mitgelieferten nativen Linux-Bibliotheken für beide Architekturen benötigen **glibc 2.38 oder neuer**. Eine Standardinstallation von Ubuntu 24.04 erfüllt diese ABI-Voraussetzung; Ubuntu 22.04 und Debian 12 erfüllen sie für die mitgelieferten Torrent-Bibliotheken nicht. Der Starter prüft diese Versionsgrenze und zeigt bei einer zu alten oder unbekannten glibc eine Warnung; die Java-/HTTP-Bereitstellung darf fortgesetzt werden. Er versucht kein glibc-/Betriebssystemupgrade. Prüfen Sie die Version auch mit `ldd --version`; eine erfolgreiche Bereitstellung allein bestätigt keine tatsächlich geladene Torrent-Bibliothek.

| Distribution | Typische Kompilierungspakete |
| --- | --- |
| Debian/Ubuntu | `build-essential`, `binutils`, `libpcap-dev`, `pkg-config` |
| Fedora | `gcc`, `glibc-devel`, `make`, `binutils`, `libpcap-devel`, `pkgconf-pkg-config` |
| Arch | `gcc`, `make`, `binutils`, `pkgconf`, `libpcap` |

Bei Bedarf werden auch `curl`, CA-Zertifikate, `tar`, `gzip` und Desktopbibliotheken für Java benötigt. Unter Arch muss die Paketdatenbank durch die normale Distributionspflege aktuell gehalten werden. Der Installer führt keinen vollständigen Systemwechsel und keine pauschale Aktualisierung aller Pakete durch.

Vorhandenes passendes Java 17+ wird wiederverwendet. Andernfalls wird eine festgelegte offizielle Temurin-21-JRE mit SHA-256-Prüfung **nur für diese Anwendung** installiert. Internetzugang ist nötig, wenn Pakete oder diese JRE fehlen.

### macOS — nur portables ZIP 3.0

- Zielplattform für Bereitstellung und Java-/HTTP-Funktionen: **macOS 11 oder neuer**, Intel oder Apple Silicon. Die Laufzeit muss zur nativen CPU passen; auf Apple Silicon wird nicht automatisch eine Intel-JRE unter Rosetta gewählt.
- Apples **Command Line Tools** und das System-libpcap werden zum lokalen Kompilieren des Netzwerkhelfers benötigt. Vollständiges Xcode und Homebrew sind nicht erforderlich. Siehe [Apples Installationsanleitung](https://developer.apple.com/documentation/xcode/installing-the-command-line-tools/).
- Der Helfer braucht `sudo` zum Öffnen des Ethernet-BPF-Geräts. Anschließend gibt er zusätzliche Gruppen sowie Gruppen- und Benutzerrechte dauerhaft ab. Die Java-Oberfläche und die Downloads laufen als normaler Benutzer.
- Eventuell angefragten Zugriff auf das lokale Netzwerk müssen Sie selbst in den macOS-Dialogen für die verwendeten Programme freigeben.
- Fehlt passendes Java 17+, wird eine festgelegte, per SHA-256 geprüfte Temurin-21-JRE privat installiert. System-Java wird nicht ersetzt.

**Zusätzliche Voraussetzung für BitTorrent/magnet:** Die tatsächlich mitgelieferten Torrent-Bibliotheken haben höhere macOS-Ziele: **Apple Silicon benötigt macOS 26 oder neuer; Intel benötigt macOS 15 oder neuer**. macOS 11+ bedeutet daher nicht, dass alle Protokolle dieser Ausgabe dort verfügbar sind. Der Starter erkennt diese Versionsgrenze, warnt bei einem älteren System und erlaubt die Java-/HTTP-Bereitstellung, ohne macOS zu aktualisieren. Eine erfolgreiche Installation bestätigt keine tatsächlich geladene Torrent-Bibliothek. Betriebssystem und Architektur lassen sich mit `sw_vers -productVersion` und `uname -m` prüfen; die tatsächliche Ausführung auf einem Mac bleibt noch zu verifizieren.

Das erzeugte `.app` ist in diesem Release **nicht mit Developer ID signiert und nicht notarisiert**. Der portable Finder/Gatekeeper-Ablauf 3.0 wurde auf einem Benutzer-Mac nicht geprüft. Native 3.5-Builds und Launcher/API wurden auf echten GitHub-Mac-Runnern geprüft; Sicherheitsdialoge beim Benutzer und BPF-Aufzeichnung bleiben ungeprüft. Die macOS-Sicherheitsabfragen und nötigen Bestätigungen sind unten separat erklärt.

### Was die Starter automatisch prüfen

Die Bereitstellung prüft Betriebssystem, native CPU-Architektur, Schreibrechte, freien Platz und eine vollständige passende Java-Laufzeit. Linux/macOS prüfen außerdem die vorstehenden BitTorrent-Versionsgrenzen und untersuchen vorhandene Netzwerklinks ausschließlich lesend. Ein Ethernet-formatierter Link ist nur ein Kandidat; daraus folgt noch keine bestätigte physische Gateway-Verbindung. Die endgültige Adapter- und Subnetzprüfung führt der Netzwerkhelfer vor der Übertragung aus. Eine VM mit geeigneter überbrückter Ethernet-Verbindung wird nicht allein wegen ihrer virtuellen Hardware vom Installer ausgeschlossen.

Fehlende Java-Laufzeiten werden aus festgelegten offiziellen Quellen geladen und vor der Nutzung geprüft. Unter Linux werden fehlende Compiler-/libpcap-/Runtime-Pakete über den Paketmanager angefordert; unter macOS werden fehlende Apple-Werkzeuge über Apples sichtbaren Installer angefordert. Eine ungeeignete verkleinerte Java-Laufzeit wird unter Linux/macOS übersprungen; falls nötig wird die vollständige private Laufzeit installiert. Administrator-, Lizenz- und Datenschutzabfragen müssen Sie selbst bestätigen. Bei Download-, Hash- oder Modulfehlern bricht der betreffende Installationsschritt ab.

## Netzwerk und virtuelle IP-Adresse

Voreingestellt und bei einem neuen Profil ausgewählt sind diese vier Gateways:

```text
192.168.1.1
192.168.1.2
192.168.1.3
192.168.1.4
```

Die virtuelle Adresse der Anwendung ist standardmäßig **`192.168.1.70`**. Passen Sie diese Adressen an Ihr eigenes Netz an. Das Programm unterstützt **1 bis 32 unterschiedliche Gateways**, die über einen geeigneten physischen Ethernet-Adapter in einem gemeinsamen lokalen IPv4-Subnetz erreichbar sein müssen. Das vom Betriebssystem benutzte Gateway wird nicht zusätzlich als fünfter Weg angelegt.

Die virtuelle IP gehört zum **privaten Netzwerkstack der Anwendung**. Sie ermöglicht unabhängige Verbindungen über die gewählten Gateways, ohne die Schnittstellenadressen oder Routingtabelle des Betriebssystems und damit die Verbindungen anderer Programme zu ändern. Es ist keine zusätzliche Adresse, die Sie zuvor im Betriebssystem eintragen müssen. Sie muss jedoch im LAN frei sein und zum Subnetz passen; reservieren Sie sie möglichst außerhalb des DHCP-Vergabebereichs. Die Anwendung prüft einen Adresskonflikt vor der Nutzung.

WLAN, reine VPN-Adapter und IPv6-Verbindungen werden von diesem Ethernet-Transport nicht unterstützt. Auch Verbindungen zu lokalen LAN-BitTorrent-Peers werden nicht über ihn geführt. Bei mehreren passenden Ethernet-Adaptern kann die Auswahl als mehrdeutig abgelehnt werden. Ein nicht erreichbares Gateway wird durch die Anwendung nicht zu einem nutzbaren Internetanschluss.

DNS-Anfragen laufen über das ausgewählte Gateway: zunächst zum Router, dann bei Bedarf zu `1.1.1.1` oder `8.8.8.8` über denselben Weg. Es gibt keinen Rückfall auf den DNS-Resolver des Betriebssystems. DNSSEC, DoH und ein TCP-Rückfall bei gekürzten DNS-Antworten sind in diesem Release nicht implementiert.

## Native Installationspakete 3.5: Windows und macOS

Wählen Sie das Paket für Ihr System aus den [GitHub-Releases](https://github.com/2018-spec-fdsds/multiroute-resume/releases), sofern es dort angehängt ist. Jedes Paket ist **eine Installationsdatei** mit Anwendung, nativem Starter, vollständiger Java-Laufzeit und passendem Netzwerkhelfer. Quellcode und portable ZIPs der Version 3.0 sind separate Angebote.

| System | Installationsdatei | Start nach der Installation |
| --- | --- | --- |
| Windows 11 x64 | `MultiRoute-Resume-3.5-Windows-x64-Setup.exe` | **MultiRoute Resume** im Startmenü oder über die Desktopverknüpfung |
| Apple Silicon, macOS 26+ | `MultiRoute-Resume-3.5-macOS-arm64.pkg` | **MultiRoute Resume** in Programme, Spotlight oder Dock |
| Intel, macOS 15+ | `MultiRoute-Resume-3.5-macOS-x86_64.pkg` | **MultiRoute Resume** in Programme, Spotlight oder Dock |

Die fünf Sprach-Anleitungen und Screenshots sind auch offline enthalten. Windows: `%LOCALAPPDATA%/MultiRoute Resume App/app/docs/`. macOS: im Finder beim installierten Programm **Paketinhalt zeigen** wählen, dann `Contents/app/docs/` öffnen. `en.md`, `ru.md`, `de.md`, `es.md` oder `fr.md` in einem Text- oder Markdown-Programm öffnen.

### Windows: installieren und öffnen

1. Laden Sie die `.exe` herunter und vergleichen Sie deren SHA-256 mit dem Release. Öffnen Sie die Datei und folgen Sie dem Installationsassistenten. Die Installation gilt für Ihr Benutzerkonto; wählen Sie die angebotenen Verknüpfungsoptionen.
2. Öffnen Sie **MultiRoute Resume** im Startmenü oder über die Verknüpfung. Ein normales Programmfenster erscheint, ohne Kommandozeilenfenster. Java ist enthalten und muss nicht separat installiert werden.
3. Das Programm prüft Npcap. Fehlt der Treiber, bestätigen Sie seinen Download im Programm. Es lädt den festgelegten offiziellen Installer, prüft SHA-256, Authenticode-Signatur und Herausgeber und öffnet die normale Npcap-Installation. Lesen Sie die Lizenz und bestätigen Sie Windows UAC. Nach einem erforderlichen Neustart öffnen Sie MultiRoute erneut. Npcap bleibt ein separater Treiber: **Er wird nicht mitgeliefert oder still installiert**. Wenn er fehlt, ist für diesen Schritt Internetzugang nötig.

Programm und Installer haben keine Herausgebersignatur. SmartScreen kann einen unbekannten Herausgeber melden: Prüfen Sie das offizielle Release und die Datei, bevor Sie gegebenenfalls **Weitere Informationen → Trotzdem ausführen** wählen. Eine Malware-Erkennung ist ein anderer Fall. Deaktivieren Sie keinen Virenschutz und legen Sie keine Ausnahme als Installationsschritt an. Eine AdminOnly-Npcap-Installation kann eine weitere Autorisierung für den Gerätezugriff erfordern. Starten Sie die Java-Anwendung als normalen Benutzer.

### macOS: installieren und öffnen

1. Wählen Sie die `.pkg` für die **native** CPU: arm64 für Apple Silicon, x86_64 für Intel. Die neuen Pakete benötigen **macOS 26+ auf Apple Silicon** beziehungsweise **15+ auf Intel**, einschließlich Torrent/magnet. Die ältere portable HTTP-Mindestversion gilt für diese Pakete nicht.
2. Laden Sie die `.pkg` herunter, vergleichen Sie die veröffentlichte SHA-256 und öffnen Sie Apples Installer. Bestätigen Sie Installation und Administratorfreigabe zum Einrichten des geschützten Netzwerkhelfers.
3. Öffnen Sie **`/Applications/MultiRoute Resume.app`** in Finder oder Spotlight. Das Symbol kann ins Dock übernommen werden. Java und der fertig kompilierte Helfer sind enthalten: **Terminal, Command Line Tools, Homebrew und eine separate Java-Installation werden für dieses Paket nicht benötigt**.
4. Bestätigen Sie bei Bedarf den normalen macOS-Administratordialog, damit der geschützte Helfer das Ethernet-BPF-Gerät öffnen kann. Anschließend gibt er die erhöhten Rechte ab; das Java-Fenster läuft als normaler Benutzer. Während der Nutzung ist keine Terminalsitzung nötig.

### macOS: Sicherheitsbestätigungen des nativen Pakets

Das Paket ist **nicht mit Developer ID signiert und nicht notarisiert**. Eine technische Ad-hoc-Signatur bestätigt keine Apple-Entwickleridentität. Versuchen Sie zunächst, `.pkg` oder Anwendung normal zu öffnen. Falls macOS einen nicht verifizierten Entwickler meldet und eine Ausnahme anbietet, wählen Sie **Systemeinstellungen → Datenschutz & Sicherheit → Dennoch öffnen**, dann **Öffnen**, erst nach Prüfung von Release-Herkunft und Hash. Ein verwalteter Mac kann dies verbieten. Siehe [Apple](https://support.apple.com/de-de/102445).

Installationsfreigabe, spätere Helferfreigabe und Entwicklerausnahme sind getrennte Bestätigungen. Erlauben Sie bei Nachfrage **Lokales Netzwerk** für MultiRoute/Java, damit Ihre LAN-Gateways erreichbar sind. Einen Ordnerzugriff erlauben Sie für das gewählte Downloadziel. Eine abgelehnte Abfrage bedeutet nicht, dass die gesamte Java-Anwendung als root gestartet werden soll.

Der Installer deaktiviert weder Gatekeeper/SIP noch globale Quarantäneprüfungen, verändert keine Systemrouten und richtet keine globale `sudoers`-Ausnahme ein. Eine Warnung vor Malware oder einer beschädigten Datei ist keine normale Entwicklerbestätigung: Halten Sie an und prüfen Sie die Datei. Der Dialogtext kann je nach macOS und Organisationsrichtlinien abweichen.

## Portable ZIPs 3.0 und Linux: Installation und Start

Laden Sie das zu Ihrem System gehörende ZIP aus den [GitHub-Releases](https://github.com/2018-spec-fdsds/multiroute-resume/releases) herunter und entpacken Sie **den vollständigen Inhalt**. Das fertige Release und eine Kopie der Quellen sind nicht dasselbe: eine reine Quellkopie muss zuerst gebaut werden. Hinweise dazu stehen im [Build-Handbuch](../native/source/BUILD-README.txt).

### Windows

1. Entpacken Sie das ZIP in einen beschreibbaren, dauerhaften Ordner.
2. Starten Sie beim ersten Mal **`Deploy-Windows.cmd`** per Doppelklick. Die Umgebungsprüfung und gegebenenfalls der verifizierte Java-/Npcap-Download erfolgen automatisch.
3. Bestätigen Sie bei Bedarf die sichtbare Npcap-Installation einschließlich UAC und Lizenz. Falls sie einen Neustart verlangt, starten Sie Windows neu und öffnen Sie den Starter nochmals.
4. Für spätere Starts verwenden Sie **`Start-Windows.cmd`**; auch dieser führt die Prüfungen aus. Öffnen Sie ihn als normaler Benutzer, nicht mit „Als Administrator ausführen“.

Falls Sie Java ausdrücklich vorgeben möchten, setzen Sie `JAVA_EXE` auf eine vollständige passende `java.exe`, oder konfigurieren Sie `JAVA_HOME` beziehungsweise `PATH`. Eine fehlerhafte ausdrückliche Windows-`JAVA_EXE`-Vorgabe müssen Sie korrigieren oder entfernen.

Beispiel in `cmd.exe`; ersetzen Sie den Pfad:

```bat
set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe"
Start-Windows.cmd
```

Nur API beziehungsweise abweichender API-Port:

```bat
Start-Windows.cmd --headless
Start-Windows.cmd --headless --port 8766
```

### Linux

1. Entpacken Sie das ZIP dauerhaft in einen beschreibbaren Ordner Ihres Benutzerkontos.
2. Öffnen Sie **`Deploy-Linux.desktop`** und erlauben Sie den Start des Launchers, falls Ihr Dateimanager danach fragt. Alternativ führen Sie in diesem Ordner `bash ./Deploy-Linux.sh` aus.
3. Bestätigen Sie die angeforderten `sudo`-Aktionen. Das Skript prüft die Umgebung, installiert fehlende Abhängigkeiten, baut den Helfer und prüft ihn mit einem Offline-Selbsttest vor der geschützten Installation.
4. Das Fenster startet als normaler Benutzer. **Lassen Sie das zugehörige Terminal offen**; die vorübergehende `sudo`-Autorisierung gehört zu dieser Sitzung.
5. Für weitere Starts verwenden Sie den Menüeintrag **MultiRoute Resume** oder `bash ./Start-Linux.sh`. Fehlt Java oder der Helfer, setzt der Starter die Bereitstellung fort.

Ohne grafische Oberfläche:

```sh
bash ./Deploy-Linux.sh --headless
bash ./Start-Linux.sh --headless
```

Starten Sie nicht die gesamte Anwendung mit `sudo`.

### macOS — nur portables ZIP 3.0

1. Entpacken Sie das vollständige macOS-ZIP; starten Sie das Skript nicht innerhalb eines Archivs.
2. Öffnen Sie **`Deploy-macOS.command`** mit Terminal. Der Starter prüft System, native CPU, freien Platz, Schreibrechte, Java und Helfer.
3. Fehlen die Command Line Tools, öffnet sich Apples Installer. Bestätigen Sie dessen Installations-, Lizenz- und Administratorabfragen selbst. Das Terminal wartet **bis zu 30 Minuten** und setzt danach automatisch fort. Mit `Ctrl+C` können Sie das Warten abbrechen; nach Abbruch oder Zeitüberschreitung starten Sie den Starter erneut, sobald die Tools installiert sind.
4. Bestätigen Sie die Administratorabfrage für den geschützten Helfer. Das Skript prüft den Helfer vor der endgültigen Installation.
5. Die Anwendung wird unter **`~/Applications/MultiRoute Resume.app`** installiert und geöffnet. **Lassen Sie das Terminal während der Nutzung offen.**
6. Spätere Starts erfolgen über die installierte `.app` oder **`Start-macOS.command`**. Ist die temporäre Autorisierung abgelaufen, schließen Sie die Anwendung und öffnen Sie den Starter erneut.

Eine eigene native Java-Laufzeit können Sie so vorgeben:

```sh
JAVA_EXE="/absolute/path/to/java" ./Deploy-macOS.command
./Start-macOS.command --headless
```

Starten Sie Java und den gesamten Starter nicht mit `sudo`. Die Bereitstellung ändert keine globalen BPF-Rechte und installiert keine dauerhafte `sudoers`-Ausnahme.

### macOS: Sicherheitsabfragen beim ersten Start

1. Prüfen Sie Herkunft und die veröffentlichte SHA-256 des ZIP. Das Paket ist nicht signiert/notarisiert; die Installation erteilt ihm keine Apple-Vertrauensbestätigung.
2. Versuchen Sie den normalen Start. Wird nur ein nicht verifizierter Entwickler gemeldet, können Sie unter **Systemeinstellungen → Datenschutz & Sicherheit → Dennoch öffnen** und in der folgenden Abfrage **Öffnen** wählen, sofern macOS diese Option anbietet und Sie dem geprüften Paket vertrauen. Das gilt auch für den üblichen Ablauf unter macOS 15/26; siehe [Apple](https://support.apple.com/en-us/102445). Eine Warnung vor schädlicher oder beschädigter Software ist ein anderer Fall und sollte nicht umgangen werden.
3. Die Apple-Werkzeuginstallation verlangt Ihre eigenen Installations- und Lizenzbestätigungen. Das wartende Terminal muss offen bleiben.
4. Beim `sudo`-Passwort im Terminal erscheinen normalerweise keine Zeichen. Es autorisiert den geschützten Netzwerkhelfer; Java bleibt unter Ihrem Benutzerkonto.
5. Wird **Lokales Netzwerk** angefragt, erlauben Sie dem betroffenen Terminal-/Java-/Anwendungsprozess den Zugriff auf Ihre Gateways. Nach Ablehnung kann dies unter **Systemeinstellungen → Datenschutz & Sicherheit → Lokales Netzwerk** geändert werden. Siehe [Apple-Netzwerkberechtigungen](https://support.apple.com/guide/mac-help/control-access-to-your-local-network-on-mac-mchla4f49138/mac).

Der Installer deaktiviert weder Gatekeeper/SIP noch Virenschutz und entfernt nicht global Download-Quarantäneattribute. Sicherheits- und Administratorbestätigungen werden nicht automatisch beantwortet.

## Erster Download

1. Wählen Sie oben im Fenster bei **Language** die Sprache **Deutsch**.
2. Öffnen Sie die Netzwerkeinstellungen. Bearbeiten oder ergänzen Sie die Gateways und wählen Sie eine freie virtuelle IP. Die sichtbare Hilfe **(?)** erklärt deren Zweck. Hinweise zu Feldern erscheinen beim Darüberfahren nach etwa 250 ms.
3. Prüfen und übernehmen Sie die virtuelle Adresse, dann prüfen Sie die Gateways. Wählen Sie mindestens eines aus.
4. Fügen Sie die Datei-URL mit **Ctrl+V**, auf macOS mit **Cmd+V**, oder über die Einfügeschaltfläche in das lange Linkfeld ein. Links sind nicht auf 128 Zeichen begrenzt.
5. Wählen Sie das Downloadziel über den systemüblichen Ordnerdialog. Bei einem neuen Profil ist dies der Ordner `Downloads` Ihres Benutzerkontos.
6. Starten Sie den Download mit der Schaltfläche direkt neben dem Linkfeld. Bei Quellen mit mehreren Dateien bestätigen Sie zuerst die gewünschte Auswahl.
7. Beobachten Sie Gesamtgeschwindigkeit, vergangene und geschätzte verbleibende Zeit sowie die Geschwindigkeit und Datenmenge jedes Gateways. Die optionale Grafik lässt sich einblenden.

**Der Geschwindigkeitstest lädt höchstens 16 MiB oder misst bis zu fünf Sekunden. Diese Grenzen gelten nur für den Test**, nicht für normale Downloads. Ohne direkte HTTP-Datei-URL, etwa bei magnet oder einer Repository-Seite, verwendet er eine Selectel-Testdatei. Er misst keine vollständige BitTorrent-Verfügbarkeit. Für die eigentlichen Downloads können Sie separate Warteschlangen- und Geschwindigkeitslimits festlegen.

HTTP-Segmente sind standardmäßig **20 MiB** groß, einstellbar von 1 bis 64 MiB. Alle Gateways holen neue Teile, sobald sie frei sind. Nach **60 Sekunden ohne neue Daten** wird eine festhängende Übertragung geschlossen und ihr Teil für andere ausgewählte Gateways freigegeben; zwei Gateways schreiben denselben Teil nicht gleichzeitig. Bei BitTorrent gibt die Quelle die Teilgröße vor.

Sie können Gateways während eines laufenden Downloads hinzufügen oder entfernen. Ein neu angelegtes Gateway wird einem vorhandenen Auftrag über dessen Gateway-Auswahl hinzugefügt, nicht automatisch allen Aufträgen. Bei jedem unfertigen Auftrag muss mindestens eines verbleiben. Bereits geprüfte Teile bleiben erhalten. Eine entfernte Profilzeile löscht keine heruntergeladenen Dateien.

Das Löschen eines Auftrags fragt separat nach seinen Dateien und Teilen. **Das Löschen der Dateien ist standardmäßig nicht angekreuzt**. Ohne diese Auswahl wird nur der Auftrag aus der Liste entfernt. Mit ihr werden nach Beendigung der Übertragung nur die Dateien dieses Auftrags gelöscht; andere Dateien und der Zielordner bleiben erhalten.

## Warteschlange und mehrere Links

In den Warteschlangen- und Limit-Einstellungen legen Sie die Zahl gleichzeitig aktiver Aufträge fest; standardmäßig sind es **zwei**. Auftragsprioritäten, globale und einzelne Gateway-Limits sowie ein Zeitplan nach der lokalen Uhr des Computers sind einstellbar. Ein Geschwindigkeitslimit von `0` bedeutet keine Begrenzung. Gemessen werden Nutzdaten, ohne Netzwerkheader. Manuell pausierte Aufträge werden nicht einfach durch die Warteschlange neu gestartet.

Der Linkimport akzeptiert mehrere Zeilen, TXT-Dateien sowie hineingezogene URLs oder `.torrent`-Dateien. Eine Vorschau ermöglicht die bewusste Auswahl; bis zu 1000 Links pro Import werden in API-Paketen von höchstens 128 verarbeitet. Suche, Statusfilter und Sortierung erleichtern die Auswahl alter Aufträge. Die angezeigte Restzeit ist eine Schätzung anhand der aktuellen Geschwindigkeit und enthält nicht die abschließende Prüfung oder Zusammenstellung.

## Unterstützte Quellen und Integritätsprüfung

| Quelle | Verhalten |
| --- | --- |
| HTTP/HTTPS | Verteilung über Range-Anfragen mit Antwort 206, wenn die Quelle eine sichere Teilübertragung erlaubt. TLS-Zertifikate und Hostnamen werden geprüft. |
| HTTP ohne Range | Ein kompletter Datenstrom über **ein** gewähltes Gateway, auch ohne `Content-Length`. Bei Abbruch oder Wegwechsel beginnt dieser Strom bei Byte 0. |
| FTP/FTPS | Passives FTP beziehungsweise explizites FTPS mit TLS für Steuer- und Datenverbindung. Für sichere Segmentierung ist eine vertrauenswürdige SHA-256 oder SHA-512 erforderlich; sonst wird vollständig über ein Gateway übertragen. |
| SFTP | SSH/SFTP mit vorher unabhängig geprüftem, festgelegtem **SHA-256-Hostschlüssel-Fingerabdruck**. Ein abweichender Schlüssel wird abgelehnt. Eine vertrauenswürdige Datei-SHA-256 erlaubt sichere Segmentierung. |
| WebDAV | HTTPS-Auflistung und Dateiauswahl; Quelladresse `webdavs://…` gemäß API-Schema. |
| Metalink | `.meta4` mit Dateiauswahl, Spiegeln und veröffentlichten Datei-/Teilhashes. Spiegel werden nur bei vertrauenswürdiger SHA-256 als dieselbe Datei verwendet; gleiche Dateigröße allein reicht nicht. |
| Hugging Face | Repository-/`tree`-/`blob`-/`resolve`-Links, bewusste Dateiauswahl, GGUF-Filter, LFS-/Xet-Metadaten und HTTP-Download. Python oder `hf_xet` sind nicht nötig. Private Quellen benötigen den ausdrücklich eingetragenen Zugang und die entsprechende Serverberechtigung. |
| BitTorrent | Lokale `.torrent`, direkte Torrent-URL und magnet; Metadaten-/Teilhashprüfung, Datei-Auswahl, Peer-/Seed-/Trackerstatus und Datei-Prioritäten. Ausgewählte Gateways transportieren Peer-, Tracker-, DHT- und Webseed-Daten. |

BitTorrent-Seeding ist **standardmäßig ausgeschaltet**. Es kann mit Zeit- und Verhältnislimit aktiviert werden und verwendet die gewählten Gateways. Magnet-Metadaten benötigen erreichbare Teilnehmer; ein Link allein garantiert keinen Download. SSL-Torrents mit separatem Clientzertifikat sowie IPv6-/LAN-Peers werden von diesem Release nicht unterstützt.

Eine veröffentlichte SHA-256 sucht die Anwendung unter anderem in Hugging-Face-LFS-Metadaten, `Repr-Digest`, `filename.sha256` und `SHA256SUMS`. Eine Summe eines Teil-HTTP-Antwortkörpers wird nicht als Summe der ganzen Datei ausgegeben. Sie können eine vertrauenswürdige Gesamt-SHA-256 auch manuell eintragen.

Nach Zusammenstellung wird die SHA-256 des Ergebnisses berechnet. Bei Abweichung vom bekannten Referenzwert wird das Ergebnis nicht als fertige geprüfte Datei veröffentlicht. **Nicht jede Quelle veröffentlicht eine Referenzsumme**: ohne sie ist die angezeigte SHA-256 nur ein lokal berechneter Wert und keine bestätigte Übereinstimmung mit dem Quellserver. BitTorrent prüft die in seinen Metadaten festgelegten Teilhashes; eine lokale Gesamt-SHA-256 ist davon zu unterscheiden. Für eine sichere HTTP-Fortsetzung ist eine geeignete starke Quellidentität, etwa ein starker ETag oder eine vertrauenswürdige Gesamt-SHA-256, erforderlich; ungeeignete Quellen können abgelehnt werden.

Unter **Source access** lassen sich Basic/Bearer-Zugang, Cookies, erlaubte Header und FTP-/SFTP-Zugang ausdrücklich konfigurieren. Sie gelten für den genauen Quell-Origin und werden nicht einem anderen Server beim Weiterleiten mitgegeben. Das lokale AES-GCM-Zugangsarchiv ist benutzerbeschränkt. Browser-Cookies und fremde Schlüssel werden nicht automatisch gelesen. Diese optionalen Quellzugänge sind keine Gateway-Tokens.

## Lokale API für andere Programme

Basisadresse: **`http://127.0.0.1:8765/api/v1`**. Die Anwendung muss laufen. Die API verwendet JSON/UTF-8 und benötigt keinen Gateway- oder API-Token. Sie ist auf denselben Computer beschränkt und bietet keine Remote-Authentifizierung. Anfragen aus fremden Browser-Origins werden abgelehnt; für eine Browserintegration ist ein lokaler serverseitiger Vermittler erforderlich.

JSON-Schlüssel und Statuswerte bleiben bei jedem Sprachwechsel stabil. Die im Fenster markierten Gateways werden in externen API-Anfragen **nicht automatisch übernommen**; geben Sie `routes` ausdrücklich an.

Speichern Sie dieses Beispiel als `job.json`, ersetzen Sie URL und Zielordner:

```json
{
  "url": "https://example.org/large-file.bin",
  "directory": "D:/Downloads",
  "chunk_mb": 20,
  "routes": [
    {"type": "gateway", "gateway": "192.168.1.1", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.2", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.3", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.4", "enabled": true}
  ]
}
```

Unter Linux/macOS ist beispielsweise `"directory": "/home/your-user/Downloads"` beziehungsweise `"/Users/your-user/Downloads"` zu verwenden. Ein JSON-Pfad mit Windows-Backslashes muss diese verdoppeln. Bei Bedarf kann `expected_sha256` als vertrauenswürdiger 64-stelliger Hexwert ergänzt werden.

```sh
curl --request POST --header "Content-Type: application/json" --data-binary @job.json http://127.0.0.1:8765/api/v1/jobs
curl http://127.0.0.1:8765/api/v1/health
curl http://127.0.0.1:8765/api/v1/jobs
```

Verwenden Sie in Windows PowerShell ausdrücklich **`curl.exe`**, falls `curl` ein PowerShell-Alias ist. Ein `201` bestätigt die Auftragserstellung, nicht den abgeschlossenen Download. Speichern Sie die zurückgegebene `id` und verfolgen Sie `GET /api/v1/jobs/{id}`. Ein vorhandener kompatibler Auftrag kann mit `reused: true` zurückgegeben werden und muss gegebenenfalls ausdrücklich fortgesetzt werden.

| Anfrage | Zweck |
| --- | --- |
| `GET /health` | Verfügbarkeit und Version |
| `POST /jobs` | Einen Auftrag erstellen |
| `POST /jobs/batch` | Mehrere Aufträge, bis zu 128 Eingaben pro Paket |
| `POST /jobs/{id}/pause`, `POST /jobs/{id}/resume` | Pausieren beziehungsweise fortsetzen |
| `PUT /jobs/{id}/routes` | Vollständigen Gateway-Satz dieses Auftrags ändern |
| `DELETE /jobs/{id}` | Auftrag entfernen, Dateien behalten |
| `DELETE /jobs/{id}?delete_files=true` | Auch die Dateien und Teile dieses Auftrags entfernen |
| `GET /network`, `POST /network` | Virtuelle IP und Gateway-Profile lesen beziehungsweise konfigurieren |
| `GET /queue`, `PUT /queue` | Parallelität, Limits und lokalen Zeitplan |
| `POST /sources/inspect`, `POST /sources/download` | Quell-Dateien auflisten beziehungsweise gewählte Dateien laden |
| `GET /sources/auth`, `PUT /sources/auth` | Explizite, optionale Quellzugänge verwalten |
| `GET /events` | Ereignisstrom im SSE-Format |

Alle Pfade in der Tabelle beziehen sich auf `/api/v1`. Vollständige Felder und Fehler sind im [OpenAPI-Schema](../openapi.yaml) dokumentiert; weitere ausführliche Beispiele enthält die [API-Anleitung auf Russisch](../API.ru.txt).

## Daten, Aktualisierung und Fehlersuche

### Native Pakete 3.5

| System | Einstellungen, Verlauf und Protokolle | Programm/Laufzeit | Netzwerkhelfer |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Resume\data` | `%LOCALAPPDATA%\MultiRoute Resume App`, einschließlich Java | Enthaltener x64-Helfer; Npcap separat installiert |
| macOS | `~/Library/Application Support/MultiRoute Resume/data` | `/Applications/MultiRoute Resume.app`, einschließlich Java | `/Library/PrivilegedHelperTools/MultiRouteResume/3.5/gateway-net` |

Schließen Sie die Anwendung vor einem Update mit einem neuen Installer. Unter Windows erfolgt die Entfernung über **Einstellungen → Apps → Installierte Apps**. Unter macOS löscht das Entfernen der `.app` nicht automatisch den Verlauf und den separat geschützten Helfer. Heruntergeladene Dateien bleiben im gewählten Ordner; löschen Sie ihn nicht nur zur Deinstallation. Der Datenpfad unterscheidet sich vom Windows-ZIP 3.0; sichern Sie ein früheres Profil vor einer Migration.

### Portable Pakete 3.0 und Linux

| System | Daten und Verlauf | Privater Java-Ordner / Helfer |
| --- | --- | --- |
| Windows | Standardmäßig `data` neben dem Programm; `data-location.config` oder `--data-dir` kann auf ein anderes Profil verweisen. | Optional `runtime` neben dem Programm; Helfer `native/windows-x64/gateway-net.exe`. |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/runtime`; `/usr/local/libexec/MultiRouteResume/3.0/gateway-net`. |
| macOS | `~/Library/Application Support/MultiRoute Resume/data` | Daneben `runtime`; `/Library/PrivilegedHelperTools/MultiRouteResume/3.0/gateway-net`. |

Das Downloadziel ist unabhängig vom Datenordner. UTF-8-Protokolle liegen relativ zum tatsächlich verwendeten Datenordner unter `logs/application.log` und `network/gateway-network.log`. Registrierte Meldungen werden beim Anzeigen übersetzt; interne technische Schlüssel bleiben unverändert. IP-Adressen, Links und Dateipfade bleiben erhalten, MAC-Adressen und Parameter signierter Links werden in den Benutzerprotokollen maskiert.

| Problem | Überprüfung |
| --- | --- |
| Java startet nicht / falsche Architektur | `java -version`, `JAVA_EXE`, `JAVA_HOME`, Architektur und vollständige Module prüfen. Den Bereitstellungsstarter erneut öffnen; eine fehlerhafte ausdrückliche Windows-`JAVA_EXE`-Vorgabe korrigieren oder entfernen. |
| Npcap-Fehler | Npcap-Installation und Zugriffsrechte prüfen, insbesondere AdminOnly. Keine Antivirus-Ausnahme automatisch einrichten. |
| Kein passender Adapter / mehrere passende Adapter | Physisches Ethernet, gemeinsames IPv4-LAN und Adapterkonfiguration prüfen. WLAN/VPN ist kein Ersatz für den unterstützten Transport. |
| Virtuelle Adresse bereits belegt | Eine andere freie LAN-Adresse wählen und erneut prüfen. Nicht die Betriebssystem-Routen ändern. |
| Linux/macOS-Autorisierung abgelaufen | Natives macOS: Anwendung erneut öffnen und bei Bedarf den Administratordialog bestätigen. Portable 3.0/Linux: Terminalstarter erneut öffnen und dort autorisieren; Terminal offen lassen. Java nicht als root starten. |
| Keine GUI unter Linux | X11 beziehungsweise XWayland/Schriftbibliotheken prüfen oder mit `--headless` ausschließlich die API starten. |
| Download wartet, obwohl ein Geschwindigkeitstest funktioniert | Quell-URL, DNS, Zugriffsrechte, Netzwerk-/Serverfehler in den Details prüfen. Ein Test einer anderen Datei beweist keine Erreichbarkeit des gewünschten Servers. |
| HTTP 401/403 oder private Hugging-Face-Quelle | Erforderliche Quellberechtigung und origingebundenen Zugang konfigurieren. Der Gateway-Test ersetzt keine Serverberechtigung. |
| SFTP-Hostschlüssel abweichend | Fingerabdruck über eine unabhängige vertrauenswürdige Quelle prüfen; die Prüfung nicht abschalten. |
| Magnet liefert keine Metadaten | Verfügbare Teilnehmer/Tracker und Erreichbarkeit prüfen; eine inaktive Verteilung kann nicht erzwungen werden. |
| Speicherplatz fehlt | Ziel- und Datenlaufwerk prüfen; Auftrag anhalten, den Ordner über die Oberfläche ändern, dann fortsetzen. |
| API-Port belegt | Eigenen Port mit `--port 8766` wählen. Ein früherer MultiRoute-Prozess wird geordnet geschlossen; fremde Programme werden nicht beendet. |

Für ein Update der portablen Pakete 3.0 unter Linux/macOS führen Sie den jeweiligen Bereitstellungsstarter aus dem neu entpackten Release aus. Nach einem Umzug des Programmordners führen Sie ihn erneut aus, damit der Launcher auf den neuen Ort zeigt. Sichern Sie Daten und Auftragsverlauf vor bewusster Entfernung; löschen Sie zum Deinstallieren nicht rekursiv den Downloadordner.

## Umfang der Release-Prüfung

Java-, API-, Protokoll-, Oberflächen- und native Offline-Prüfungen sowie eigene synthetische Netzwerkfälle wurden ausgeführt. Sie belegen **keine tatsächliche Übertragung über Ihre physischen Router**. Native 3.5-Pakete wurden auf GitHub-Runnern mit Apple Silicon/macOS 26 und Intel/macOS 15 gebaut und geprüft: Launcher/API, Torrent-JNI, Paketinhalt und Helfer-Testfälle. Installation beim Benutzer, Finder-/Sicherheitsdialoge und BPF-Aufzeichnung bleiben ungeprüft. Nur das portable Paket 3.0 kompiliert den Helfer lokal beim Benutzer; native Pakete 3.5 enthalten separate vorkompilierte Intel-/Apple-Silicon-Komponenten, ohne Developer-ID-Signatur oder Notarisierung. Paket- oder CI-Prüfungen beweisen nicht die Funktion auf Ihrem Mac. Prüfen Sie nach Installation jedes Gateway mit einer echten Datei, bevor Sie sich auf die kombinierte Bandbreite verlassen.

Prüfberichte: [CHECKS.ru.md](../CHECKS.ru.md), [TEST-REPORT.json](../TEST-REPORT.json). Drittanbieter-Komponenten und ihre Lizenzen: [THIRD-PARTY-NOTICES.txt](../THIRD-PARTY-NOTICES.txt).

## Systemtray und Beenden (3.5)

Beim Schließen des Hauptfensters laufen Anwendung und Downloads im Systemtray weiter. Das Symbol zeigt den gesamten Download-Durchsatz **aller Aufträge**, unabhängig von Auswahl und Tabellenfilter. Die Tooltip-Anzeige enthält den genauen Wert in Mbit/s oder Gbit/s (ab 1 Gbit/s) und Byte/s, auch bis 10 Gbit/s; das kleine Symbol rundet ihn. Im Tray-Menü **Anwendung öffnen** wählen, um das Fenster wiederherzustellen. Unter Windows/Linux öffnet auch ein Klick auf das Symbol das Fenster. **Anwendung beenden** im Tray-Menü oder Fenster speichert den Zustand und beendet das Programm. Vor einem Update diese Funktion verwenden.

Ohne Systemtray wird das Fenster in die Taskleiste minimiert; Downloads laufen weiter und Beenden bleibt verfügbar. Verschwindet das Tray, wird das verborgene Fenster wieder geöffnet. Windows kann das Symbol im ausgeblendeten Benachrichtigungsbereich ablegen.

Das Setup erstellt standardmäßig eine Desktopverknüpfung und unter Windows einen Startmenüeintrag. macOS erstellt für den angemeldeten Benutzer einen Desktoplink zur installierten `.app`; beim ersten grafischen Start wird es erneut versucht. Vorhandene Desktopobjekte werden erhalten. Bei Datenschutzbeschränkungen den angeforderten Zugriff erlauben oder die App aus Programme öffnen. Unsere Setuptexte sind immer Englisch; Systemdialoge für Sicherheit und Berechtigungen folgen der Betriebssystemsprache.

## Lizenz

Die Originalanwendung darf kostenlos installiert und privat sowie in Organisationen einschließlich geschäftlicher Tätigkeiten genutzt werden. **Verkauf der Anwendung, kostenpflichtige Lizenzen und entgeltliche Weitergabe sind untersagt**, soweit keine separate Erlaubnis vorliegt. Die eigene Lizenz gewährt keine allgemeinen Änderungs- oder Weitergaberechte und ist keine von OSI anerkannte Open-Source-Lizenz. Drittanbieter behalten ihre eigenen Lizenzbedingungen. Dies ist eine übersetzte Zusammenfassung; maßgeblich ist der englische Text in [LICENSE.txt](../LICENSE.txt).

In 3.5 hat das Geschwindigkeitssymbol einen transparenten Hintergrund mit schwarzen Ziffern und Einheiten, auch bei 10 Gbit/s. Gesamtrate und Tray-Befehle bleiben erhalten.

## Prüfung mit MD5, SHA-256 und SHA-512

Nach dem Zusammensetzen berechnet die Anwendung alle drei Prüfsummen der gesamten Datei in einem Lesedurchlauf. Fügen Sie eine Referenz mit Strg+V oder Cmd+V in das optionale Hauptfeld ein. Der Typ wird automatisch erkannt: 32 Hexadezimalzeichen bedeuten MD5, 64 SHA-256 und 128 SHA-512. Der aufklappbare Bereich für zusätzliche Prüfsummen erlaubt mehrere Referenzen.

Veröffentlichte Referenzen werden aus unterstützten Digest-Headern der gesamten Repräsentation und Begleitdateien ermittelt: `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` und `SHA512SUMS`. Die Prüfsumme einer partiellen HTTP-Antwort gilt nicht als Prüfsumme der ganzen Datei. Die bisherigen Prüfungen für Hugging Face LFS und Metalink bleiben erhalten.

Alle bekannten Referenzen müssen vor der Veröffentlichung der fertigen Datei übereinstimmen. Bei Abweichung schlägt der Auftrag fehl; Teildaten bleiben erhalten. Ohne Referenz ist die Anzeige eine lokal berechnete Summe und keine bestätigte Übereinstimmung mit der Quelle. MD5 erkennt zufällige Beschädigungen; SHA-256 oder SHA-512 bieten eine stärkere Integritätsprüfung. MD5 ersetzt weder sichere Dateiidentität noch Anforderungen an vertrauenswürdige Spiegel.

Torrent-Teile werden weiterhin zwingend geprüft. Die Summen der zusammengesetzten Torrent-Dateien sind davon getrennt. Eine manuelle Referenz für die ganze Datei erfordert genau eine ausgewählte Datei; bei Gruppen erhält jeder Auftrag seine eigene Referenz.

Die lokale API akzeptiert `expected_md5`, `expected_sha256` und `expected_sha512` in `POST /api/v1/jobs`. Ergebnisse enthalten `md5`, `sha256`, `sha512` und Referenzquellen in `checksum_sources`. Bestehende Anfragen mit `expected_sha256` bleiben kompatibel.

## Fortschritt einzelner Downloads (3.5)

Jeder Download hat einen eigenen Balken in der Spalte Fortschritt. Die Spalten füllen die Breite des Downloadbereichs. Bei vielen Aufgaben wird die Liste vertikal gescrollt; schmale Bereiche bieten auch horizontales Scrollen, ohne die Spalten unlesbar zu verkleinern. Beim Aktualisieren bleibt die erste sichtbare Aufgabe erhalten. Sortierung und Filter ordnen jeden Balken weiterhin seinem Download zu. Bei unbekannter Gesamtgröße werden empfangene Daten statt eines Prozentsatzes angezeigt; der Tooltip zeigt Größe, Status und Fehlerdetails. Die ausgewählte Zeile zeigt weiterhin ausführlichen Fortschritt, Zeiten und Gateway-Geschwindigkeiten.
