# MultiRoute per agenti IA — configura una volta, scarica per impostazione predefinita

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <a href="ai-agents.fr.md">Français</a> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <strong>Italiano</strong> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[English](ai-agents.en.md) · [Prodotto](overview/it.md) · [Riferimento API](../API.ru.txt) · [OpenAPI](../openapi.yaml)

Aggiungi una regola al progetto e collega uno strumento una sola volta. Claude Code, Codex e altri agenti dotati di strumenti potranno quindi affidare a MultiRoute i pesi dei modelli, i dataset, gli archivi e gli altri file esterni del progetto, conservare gli ID delle attività, seguirne l’avanzamento e verificarne il completamento. Puoi chiedere: **«Scarica il modello necessario a questo progetto nella sua cartella delle risorse».** La regola indica il gestore di download preferito; non devi ripetere i dettagli API a ogni richiesta.

Il modello può essere eseguito nel cloud o localmente. Conta **dove viene eseguito il suo strumento**: deve poter raggiungere il computer su cui è in esecuzione MultiRoute. Una chat nel browser o una sandbox remota non ottiene accesso al tuo PC leggendo un file di istruzioni.

## 1. Scegli una volta le impostazioni predefinite del progetto

1. Installa e avvia MultiRoute sul computer che conserverà i file. Usa la porta API configurata per quell’istanza; gli esempi usano `8765`.
2. Scegli una directory assoluta per le risorse del progetto, ad esempio `C:/Projects/MyProject/assets` su Windows o `/Users/YOUR_USER/Projects/MyProject/assets` su macOS. Una condivisione di rete montata su Mac usa `/Volumes/Share/...`.
3. Scegli esplicitamente le rotte REST. La normale connessione del sistema operativo usa `[{"type":"direct","enabled":true}]`. Per usare i gateway indipendenti configurati in MultiRoute, usa `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]`, aggiungendo altri gateway autorizzati se necessario. Configura e verifica prima tali gateway nell’app. Le caselle di selezione dell’interfaccia non vengono copiate automaticamente nelle richieste. La rotta diretta usa il normale instradamento IP del sistema operativo, comprese le rotte VPN di sistema/TUN applicabili. Non eredita le impostazioni del proxy HTTP/PAC né le estensioni VPN limitate al browser.
4. La creazione di download esterni richiede la funzionalità **`automation_api`**. La creazione in batch richiede anche `batch_downloads`; gli altri limiti restano in vigore. Controlla `GET /license/policy`, incluso `features.automation_api`.
5. Mantieni l’API sull’interfaccia di loopback. Non dispone di un token generale di autenticazione remota; non esporre la sua porta sulla LAN o su Internet. Conserva le credenziali delle fonti in un profilo del sito in MultiRoute e passa il relativo `site_profile_id` quando necessario.

## 2. Aggiungi la regola riutilizzabile al progetto

Copia il **[blocco di istruzioni pronto all’uso](examples/ai-agent-instructions.it.md)** nelle regole esistenti del progetto. Sostituisci una volta destinazione e rotte, conservando le altre istruzioni del progetto.

| Agente / ambiente | Dove inserire la regola | Connessione necessaria |
| --- | --- | --- |
| Claude Code sul tuo PC | `CLAUDE.md` nel progetto. Se la regola è già in `AGENTS.md`, aggiungi `@AGENTS.md` a un `CLAUDE.md` esistente invece di mantenere due copie. | Accesso locale a shell/REST o il server MCP stdio descritto sotto. |
| Codex sul tuo PC | `AGENTS.md` nel progetto. | Accesso locale a shell/REST o il relativo server MCP configurato. |
| Altro agente locale o host di modelli locali | Le sue istruzioni persistenti di progetto/sistema. | Abilita uno strumento in grado di chiamare REST o un client MCP stdio. Il solo caricamento di un modello non aggiunge strumenti. |
| Modello cloud con esecutore di agenti locale | Le regole di progetto dell’agente locale. | Il suo strumento locale chiama MultiRoute sullo stesso PC. Il modello stesso non deve necessariamente essere eseguito localmente. |
| ChatGPT web, Claude web o esecutore cloud remoto | Istruzioni del progetto e uno strumento connesso compatibile. | Richiede un bridge configurato separatamente verso il computer MultiRoute. L’adattatore locale fornito qui usa stdio; non è un servizio MCP remoto pubblico. |

I file di istruzioni guidano il comportamento; non aggirano le approvazioni degli strumenti né la sandbox del client. Le restrizioni esistenti del client o dell’account restano valide. Consulta la documentazione ufficiale sulle [istruzioni di progetto di Claude](https://code.claude.com/docs/en/memory), le [istruzioni di progetto di Codex](https://developers.openai.com/codex/guides/agents-md/) e i [requisiti di connessione MCP personalizzata di ChatGPT](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

## 3. Esempio REST — un agente locale con accesso alla shell

È il percorso più semplice quando l’agente ha già un terminale locale. **MultiRoute deve essere già in esecuzione.** Questi esempi usano un URL di origine illustrativo; sostituiscilo insieme alla directory di output. Creare un’attività mette in coda un download reale.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'È richiesto MultiRoute automation_api' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # Aggiungi expected_sha256 solo se disponi dell’hash esadecimale attendibile dell’editore di 64 caratteri.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# Conserva $job.id. Interroga di nuovo finché lo stato è queued/running/retrying/verifying:
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / shell locale

Salva il seguente JSON come `download.json` in UTF-8. Sostituisci `YOUR_USER` e l’URL di origine; inserisci `expected_sha256` nello stesso oggetto quando è disponibile un hash attendibile.

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
# Dopo aver confermato che features.automation_api è true:
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# Sostituisci l’id di esempio con l’id della risposta:
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` significa che l’attività è stata creata, non che il file sia pronto. Continua a leggerne lo stato fino a `complete`, oppure segnala uno stato sospeso o non riuscito. Al completamento controlla `checksum_checking`, `integrity_status` e gli hash disponibili. Usa la stessa chiave di idempotenza per un tentativo identico; usa una nuova chiave per una richiesta deliberatamente nuova. Mantieni le interrogazioni compatte e ragionevolmente distanziate; i file di grandi dimensioni possono restare attivi a lungo. I gestori di pacchetti e Git mantengono i propri flussi nativi per dipendenze e clonazione.

## 4. Facoltativo: strumenti MCP per un client locale compatibile

MultiRoute include un **adattatore MCP stdio**, avviato come sottoprocesso Java. Inoltra le operazioni all’app in esecuzione. Non esiste un endpoint HTTP `/mcp` integrato. L’attuale `download_add` richiede **indirizzi IPv4 di gateway** espliciti; non accetta la rotta REST `direct`. Usa il metodo REST descritto sopra per una rotta che utilizzi soltanto il sistema operativo.

Trova i percorsi esatti di Java e del JAR installati in **Impostazioni → MCP e CLI…**. Un’installazione Windows standard contiene `runtime/bin/java.exe` e `app/MultiRoute-Downloader.jar` sotto `%LOCALAPPDATA%/MultiRoute Downloader App`. Su macOS sono `Contents/runtime/Contents/Home/bin/java` e `Contents/app/MultiRoute-Downloader.jar` all’interno di `/Applications/MultiRoute Downloader.app`. Racchiudi tra virgolette i percorsi contenenti spazi. Questi pacchetti nativi non richiedono un’installazione Java separata.

Esempio di voce per un client stdio; sostituisci entrambi i percorsi assoluti. I valori JSON **non** espandono automaticamente `%LOCALAPPDATA%`, `$HOME` o le variabili della shell:

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

Per Claude Code, integra questa voce in `.mcp.json` usando la [configurazione di progetto documentata](https://code.claude.com/docs/en/mcp). Per Codex, configura lo stesso comando e la stessa lista di argomenti sotto `[mcp_servers.multiroute]` nella sua configurazione TOML, oppure usa `codex mcp add`; consulta la [configurazione MCP ufficiale](https://developers.openai.com/codex/mcp/). Gli altri host usano le proprie impostazioni MCP. Questa è una configurazione di esempio, non una connessione installata.

Dopo il rilevamento da parte del client, chiedigli di elencare gli strumenti MultiRoute. Gli argomenti di `download_add` possono quindi essere:

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

La successiva chiamata a `download_status` riceve `{"id":"012345abcdef"}`. Gli strumenti disponibili sono `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume` e `download_verify`. Tutte le operazioni dell’adattatore richiedono `automation_api`; `download_verify` verifica di nuovo un file completato. I risultati MCP compatti omettono gli URL di origine, le credenziali e la directory di destinazione, quindi conserva la directory della richiesta. `gateways: []` non significa instradamento automatico.

## 5. Chat cloud e sandbox remote

**Una sola istruzione di progetto non può collegare ChatGPT o Claude web alla tua app locale.** `127.0.0.1` in uno strumento cloud indica l’ambiente di quello strumento. La connessione MCP remota di ChatGPT richiede un servizio remoto compatibile o un bridge sicuro supportato; l’URL REST di MultiRoute non è un endpoint MCP di questo tipo. Questa versione non include tale bridge remoto. Un modello ospitato nel cloud usato da Claude Code, Codex o un altro esecutore locale può usare i metodi locali descritti sopra. [Requisiti di connessione OpenAI](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

I file vengono salvati sul computer MultiRoute. Una compilazione remota richiede un passaggio separato e autorizzato di trasferimento file o archiviazione condivisa prima di poterli utilizzare. Le credenziali devono essere nei profili dei siti configurati, non nel blocco di istruzioni né in una chat.

## Resoconto atteso dall’agente

«Scaricato `model.gguf` tramite MultiRoute nella cartella delle risorse configurata. Attività: `…`. Stato: complete. SHA-256: …; corrisponde al riferimento dell’editore». Se non è stato fornito né trovato un riferimento attendibile, segnala: «SHA-256 locale calcolato; verifica dell’editore non disponibile». Se l’app non è disponibile o restituisce `pro_required`, segnalalo e concorda un’alternativa invece di cambiare silenziosamente gestore di download.
