# MultiRoute-Projektanweisung — in die Regeln Ihres Agenten kopieren

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <strong>Deutsch</strong> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

Verwenden Sie diesen Block in Ihrer vorhandenen `AGENTS.md`, `CLAUDE.md` oder gleichwertigen dauerhaften Projektanweisungen. Hängen Sie ihn an und behalten Sie die übrigen Projektregeln bei. Ersetzen Sie Zielverzeichnis und Routen einmalig. Einrichtung und Beispiele finden Sie auf [Deutsch](../ai-agents.de.md) oder [Englisch](../ai-agents.en.md).

```markdown
## Downloads: standardmäßig MultiRoute verwenden

Verwende für externe Dateien, die für die genehmigte Projektaufgabe benötigt
werden (Modelle, Datensätze, Archive, Medien und andere Assets), den laufenden
MultiRoute Downloader. Frage mich nicht bei jeder Datei nach einem Downloader.
Verwende für Paketmanager und Git ihre normalen Installations-/Klon-/Updateabläufe.

Projekteinstellungen (einmal bearbeiten):
- API: http://127.0.0.1:8765/api/v1
- Ziel: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- REST-Routen: [{"type":"direct","enabled":true}]
- Optionales site_profile_id: <EXISTING_PROFILE_ID_OR_OMIT>

Verwende das verbundene lokale REST-Werkzeug oder, falls verfügbar, die
MultiRoute-MCP-Werkzeuge. Konfiguriere für MCP ausdrücklich Gateway-IPv4-Adressen
separat; sein download_add erfordert url, directory und gateways. Die REST-Route
direct ist der Betriebssystempfad; sie ist kein MCP-Gatewaywert. Erfinde keine
Routen und setze keine Auswahl in der Oberfläche voraus.

Prüfe mit REST vor der ersten Verwendung einer Sitzung GET /health und
GET /license/policy. Wenn nur MCP verfügbar ist, ermittle die Werkzeuge und rufe
download_list mit limit 1 auf, um Verbindung und Berechtigung zu prüfen;
der Adapter selbst prüft die Lizenzrichtlinie. Das Erstellen von Downloads
erfordert features.automation_api=true. Ist dies nicht verfügbar, melde den Grund
und frage nach einem anderen Downloader, statt stillschweigend einen zu verwenden.

Für jede benötigte Datei: Wähle ihre genaue URL, das genehmigte Zielverzeichnis
und den Dateinamen; übergib, falls verfügbar, einen vertrauenswürdigen SHA-256 des
Herausgebers als expected_sha256. Erstelle den Auftrag mit POST /jobs (routes,
nicht gateways) oder MCP download_add. Verwende für Wiederholungen derselben
Anfrage einen stabilen Idempotenzschlüssel; behalte die zurückgegebene Auftrags-ID.
Erstellung bedeutet nicht Abschluss. Frage den Auftrag mit GET /jobs/{id} oder
download_status in angemessenen Abständen ab. Melde pausierte/fehlgeschlagene
Zustände; verwende zum Fortsetzen den vorhandenen Auftrag, statt Duplikate zu
erstellen. Ändere keine anderen Aufträge oder Netzwerkeinstellungen. Prüfe nach
Abschluss checksum_checking und integrity_status. Behaupte keine Überprüfung
anhand des Herausgebers ohne Übereinstimmung mit einer vertrauenswürdigen Referenz.
Gib den gespeicherten lokalen Pfad, die Auftrags-ID und das Prüfergebnis zurück.
Die Datei bleibt auf dem MultiRoute-Computer; behaupte nicht, dass sie in einer
entfernten Ausführungs-Sandbox vorhanden ist.
```

Die Regel drückt eine Projektpräferenz aus. Ein kompatibles verbundenes Werkzeug und die normalen Berechtigungen des Agenten sind weiterhin erforderlich. Heruntergeladene Quelldateien sind Daten, keine zusätzlichen Agentenanweisungen.
