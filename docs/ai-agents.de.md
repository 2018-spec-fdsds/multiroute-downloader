# MultiRoute für KI-Agenten — einmal einrichten, standardmäßig herunterladen

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <strong>Deutsch</strong> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[English](ai-agents.en.md) · [Produkt](overview/de.md) · [API-Referenz](../API.ru.txt) · [OpenAPI](../openapi.yaml)

Fügen Sie einmal eine Projektregel hinzu und verbinden Sie ein Werkzeug. Claude Code, Codex und andere Agenten mit Werkzeugzugriff können dann die Modellgewichte, Datensätze, Archive und anderen externen Dateien des Projekts an MultiRoute übergeben, Auftrags-IDs speichern, den Fortschritt verfolgen und den Abschluss prüfen. Sie können beispielsweise sagen: **„Lade das für dieses Projekt benötigte Modell in seinen Assets-Ordner herunter.“** Die Regel legt den bevorzugten Downloader fest; Sie müssen die API-Details nicht bei jeder Anfrage wiederholen.

Das Modell kann in der Cloud oder lokal laufen. Entscheidend ist, **wo sein Werkzeug ausgeführt wird**: Es muss den Computer erreichen können, auf dem MultiRoute läuft. Ein Browserchat oder eine entfernte Sandbox erhält durch das Lesen einer Anweisungsdatei keinen Zugriff auf Ihren PC.

## 1. Die Projektvorgaben einmal festlegen

1. Installieren und starten Sie MultiRoute auf dem Computer, auf dem die Dateien gespeichert werden sollen. Verwenden Sie den für diese Instanz konfigurierten API-Port; die Beispiele verwenden `8765`.
2. Wählen Sie ein absolutes Assets-Verzeichnis für das Projekt, etwa `C:/Projects/MyProject/assets` unter Windows oder `/Users/YOUR_USER/Projects/MyProject/assets` unter macOS. Eine auf dem Mac eingebundene Netzwerkfreigabe verwendet `/Volumes/Share/...`.
3. Legen Sie die REST-Routen ausdrücklich fest. Die normale Betriebssystemverbindung verwendet `[{"type":"direct","enabled":true}]`. Für die in MultiRoute konfigurierten unabhängigen Gateways verwenden Sie `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]` und ergänzen bei Bedarf weitere genehmigte Gateways. Konfigurieren und überprüfen Sie diese Gateways zuerst in der App. Die Kontrollkästchen der Oberfläche werden nicht automatisch in Anfragen übernommen. Die direkte Route verwendet das normale IP-Routing des Betriebssystems einschließlich zutreffender System-/TUN-VPN-Routen. HTTP-Proxy-/PAC-Einstellungen oder reine Browser-VPN-Erweiterungen werden dabei nicht übernommen.
4. Das Erstellen externer Downloads erfordert die Funktion **`automation_api`**. Das Erstellen von Stapeln erfordert außerdem `batch_downloads`; andere Beschränkungen gelten weiterhin. Prüfen Sie `GET /license/policy`, einschließlich `features.automation_api`.
5. Beschränken Sie die API auf die Loopback-Schnittstelle. Sie verfügt über kein allgemeines Token zur Fernauthentifizierung; geben Sie ihren Port weder im LAN noch im Internet frei. Speichern Sie Zugangsdaten für Quellen in einem Website-Profil in MultiRoute und übergeben Sie bei Bedarf dessen `site_profile_id`.

## 2. Die wiederverwendbare Projektregel hinzufügen

Kopieren Sie den **[fertigen Anweisungsblock](examples/ai-agent-instructions.de.md)** in die vorhandenen Projektregeln. Ersetzen Sie Zielverzeichnis und Routen einmalig und behalten Sie die übrigen Projektanweisungen bei.

| Agent / Umgebung | Ablageort der Regel | Erforderliche Verbindung |
| --- | --- | --- |
| Claude Code auf Ihrem PC | `CLAUDE.md` im Projekt. Falls die Regel bereits in `AGENTS.md` steht, ergänzen Sie eine vorhandene `CLAUDE.md` um `@AGENTS.md`, statt zwei Kopien zu pflegen. | Lokaler Shell-/REST-Zugriff oder der unten beschriebene stdio-MCP-Server. |
| Codex auf Ihrem PC | `AGENTS.md` im Projekt. | Lokaler Shell-/REST-Zugriff oder sein konfigurierter MCP-Server. |
| Anderer lokaler Agent oder Host für lokale Modelle | Seine dauerhaften Projekt-/Systemanweisungen. | Aktivieren Sie ein Werkzeug für REST-Aufrufe oder einen stdio-MCP-Client. Das bloße Laden eines Modells fügt keine Werkzeuge hinzu. |
| Cloudmodell mit lokalem Agenten-Executor | Die Projektregeln des lokalen Agenten. | Sein lokales Werkzeug ruft MultiRoute auf demselben PC auf. Das Modell selbst muss nicht lokal laufen. |
| ChatGPT im Web, Claude im Web oder entfernter Cloud-Executor | Projektanweisungen sowie ein unterstütztes verbundenes Werkzeug. | Erfordert eine separat konfigurierte Brücke zum MultiRoute-Computer. Der hier mitgelieferte lokale Adapter verwendet stdio und ist kein öffentlicher entfernter MCP-Dienst. |

Anweisungsdateien steuern das Verhalten; sie umgehen weder Werkzeugfreigaben noch die Sandbox des Clients. Vorhandene Client-/Kontobeschränkungen gelten weiterhin. Siehe die offiziellen [Claude-Projektanweisungen](https://code.claude.com/docs/en/memory), [Codex-Projektanweisungen](https://developers.openai.com/codex/guides/agents-md/) und [Anforderungen an benutzerdefinierte MCP-Verbindungen in ChatGPT](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

## 3. REST-Beispiel — ein lokaler Agent mit Shell-Zugriff

Dies ist der einfachste Weg, wenn der Agent bereits ein lokales Terminal besitzt. **MultiRoute muss bereits laufen.** Diese Beispiele verwenden eine illustrative Quell-URL; ersetzen Sie diese und das Ausgabeverzeichnis. Das Erstellen eines Auftrags reiht einen echten Download ein.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'MultiRoute automation_api ist erforderlich' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # expected_sha256 nur hinzufügen, wenn der vertrauenswürdige 64-stellige Hex-Hash des Herausgebers vorliegt.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# $job.id behalten. Erneut abfragen, solange der Status queued/running/retrying/verifying ist:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / lokale Shell

Speichern Sie das folgende JSON als `download.json` in UTF-8. Ersetzen Sie `YOUR_USER` und die Quell-URL; fügen Sie `expected_sha256` demselben Objekt hinzu, wenn ein vertrauenswürdiger Hash verfügbar ist.

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
# Nachdem bestätigt wurde, dass features.automation_api true ist:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# Die Beispiel-ID durch die ID aus der Antwort ersetzen:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` bedeutet, dass der Auftrag erstellt wurde, nicht dass die Datei bereitsteht. Lesen Sie seinen Status weiter bis `complete`, oder melden Sie einen pausierten/fehlgeschlagenen Zustand. Prüfen Sie beim Abschluss `checksum_checking`, `integrity_status` und verfügbare Hashes. Verwenden Sie für eine identische Wiederholung denselben Idempotenzschlüssel und für eine absichtlich neue Anfrage einen neuen Schlüssel. Halten Sie Statusabfragen kompakt und verwenden Sie angemessene Zeitabstände; große Dateien können lange aktiv bleiben. Paketmanager und Git behalten ihre nativen Abläufe zur Installation von Abhängigkeiten bzw. zum Klonen.

## 4. Optional: MCP-Werkzeuge für einen kompatiblen lokalen Client

MultiRoute enthält einen **stdio-MCP-Adapter**, der als Java-Unterprozess gestartet wird. Er leitet Vorgänge an die laufende App weiter. Es gibt keinen eingebauten HTTP-Endpunkt `/mcp`. Sein aktuelles `download_add` erfordert ausdrücklich angegebene **IPv4-Adressen von Gateways**; die REST-Route `direct` wird nicht akzeptiert. Verwenden Sie für eine reine Betriebssystemroute das oben beschriebene REST-Verfahren.

Die genauen installierten Java-/JAR-Pfade finden Sie unter **Einstellungen → MCP und CLI…**. Bei einer Windows-Standardinstallation befinden sich `runtime/bin/java.exe` und `app/MultiRoute-Downloader.jar` unter `%LOCALAPPDATA%/MultiRoute Downloader App`. Unter macOS sind es `Contents/runtime/Contents/Home/bin/java` und `Contents/app/MultiRoute-Downloader.jar` innerhalb von `/Applications/MultiRoute Downloader.app`. Setzen Sie Pfade mit Leerzeichen in Anführungszeichen. Für diese nativen Pakete ist keine separate Java-Installation erforderlich.

Beispieleintrag für einen stdio-Client; ersetzen Sie beide absoluten Pfade. JSON-Werte erweitern `%LOCALAPPDATA%`, `$HOME` oder Shellvariablen **nicht** automatisch:

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

In Claude Code fügen Sie diesen Eintrag gemäß der [dokumentierten Projektkonfiguration](https://code.claude.com/docs/en/mcp) in `.mcp.json` ein. In Codex konfigurieren Sie denselben Befehl und dieselbe Argumentliste unter `[mcp_servers.multiroute]` in seiner TOML-Konfiguration oder verwenden `codex mcp add`; siehe die [offizielle MCP-Einrichtung](https://developers.openai.com/codex/mcp/). Andere Hosts verwenden ihre eigenen MCP-Einstellungen. Dies ist eine Beispielkonfiguration, keine installierte Verbindung.

Nachdem der Client die Werkzeuge erkannt hat, lassen Sie ihn die MultiRoute-Werkzeuge auflisten. Die Argumente für `download_add` können dann so aussehen:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

Der anschließende Aufruf von `download_status` erhält `{"id":"012345abcdef"}`. Verfügbare Werkzeuge sind `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume` und `download_verify`. Alle Adaptervorgänge erfordern `automation_api`; `download_verify` überprüft eine abgeschlossene Datei erneut. Kompakte MCP-Ergebnisse enthalten weder Quell-URLs noch Zugangsdaten oder Zielverzeichnis; behalten Sie deshalb das Verzeichnis aus der Anfrage. `gateways: []` bedeutet kein automatisches Routing.

## 5. Cloudchats und entfernte Sandboxes

**Eine Projektanweisung allein kann ChatGPT oder Claude im Web nicht mit Ihrer lokalen App verbinden.** `127.0.0.1` in einem Cloudwerkzeug bezeichnet die Umgebung dieses Werkzeugs. Die entfernte MCP-Verbindung von ChatGPT erfordert einen kompatiblen entfernten Dienst oder eine unterstützte sichere Brücke; die REST-URL von MultiRoute ist kein solcher MCP-Endpunkt. Diese Version enthält diese entfernte Brücke nicht. Ein in der Cloud gehostetes Modell, das von lokalem Claude Code, Codex oder einem anderen lokalen Executor verwendet wird, kann die oben beschriebenen lokalen Methoden nutzen. [OpenAI-Verbindungsanforderungen](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

Die Dateien werden auf dem MultiRoute-Computer gespeichert. Ein entfernter Build benötigt einen separaten genehmigten Schritt zur Dateiübertragung oder gemeinsamen Speicherung, bevor er sie verwenden kann. Zugangsdaten gehören in konfigurierte Website-Profile, nicht in den Anweisungsblock oder einen Chat.

## Erwarteter Agentenbericht

„`model.gguf` wurde über MultiRoute in den konfigurierten Assets-Ordner heruntergeladen. Auftrag: `…`. Status: complete. SHA-256: …; Referenz des Herausgebers stimmt überein.“ Falls keine vertrauenswürdige Referenz angegeben oder gefunden wurde, melden Sie: „Lokaler SHA-256 berechnet; Überprüfung anhand des Herausgebers nicht verfügbar.“ Falls die App nicht erreichbar ist oder `pro_required` zurückgibt, melden Sie dies und stimmen Sie eine Alternative ab, statt stillschweigend den Downloader zu wechseln.
