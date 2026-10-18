# MultiRoute Downloader 4.0 — Guida in italiano

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [Home](../README.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

Proprietario del progetto: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## 1. Scopo e funzionamento

MultiRoute Downloader è un’applicazione desktop Java per scaricare file usando contemporaneamente più **gateway Internet** nella stessa rete Ethernet locale. Per esempio, un router può usare una linea fissa e un altro una connessione mobile 4G/5G. I gateway selezionati prelevano segmenti da una coda condivisa: quelli più veloci possono scaricarne di più. Se una connessione si interrompe, gli altri continuano; i segmenti incompleti tornano alla coda e i dati già verificati rimangono disponibili. È possibile importare gruppi di link o creare lavori tramite l’API locale.

L’aumento di velocità dipende dalla capacità indipendente delle linee, dai limiti del server e dal supporto alla ripresa sicura. Non è garantito un moltiplicatore pari al numero di router. L’interfaccia è un’app desktop, non una pagina web. Il nuovo profilo parte in inglese; sono disponibili russo, tedesco, spagnolo, francese, cinese, coreano, arabo, italiano, giapponese, hindi e vietnamita. La scelta viene salvata e applicata a messaggi registrati, log e suggerimenti. Un errore grezzo di un server o di una libreria esterna può restare nella lingua originale. Nomi, percorsi, URL e chiavi JSON non vengono tradotti.

## 2. Ambiente necessario

| Piattaforma | Sistema e processore | Componenti |
| --- | --- | --- |
| Windows | Windows 11 x64; nessun pacchetto ARM64 | L’installer nativo include Java 21 e il componente di rete x64; Npcap si installa separatamente |
| Mac Apple Silicon | M1/M2/M3/M4 o successivo, macOS 26+ | PKG arm64 con Java e componente di rete già compilato |
| Mac Intel | x86_64, macOS 15+ | PKG x86_64 con Java e componente compilato |
| Linux | Distribuzione glibc, x86_64 o ARM64; torrent/magnet richiedono glibc 2.38+ | Java 17+ completo, sudo, compilatore C, binutils, pkg-config e file di sviluppo libpcap; per la GUI X11 oppure Wayland con XWayland e librerie dei caratteri |

Java deve comprendere `java.desktop`, `java.net.http`, `jdk.httpserver` e `java.xml`, anche per `--headless`. I pacchetti nativi Windows/macOS non richiedono Java separato. Gli ZIP portable 3.0 e il deploy Linux possono ottenere un runtime privato da fonti ufficiali. Usare pacchetti completi, senza mescolare singoli file di versioni diverse.

Occorre una scheda **Ethernet cablata** nella stessa sottorete IPv4 di tutti i gateway scelti. Ogni router deve permettere al virtual IP dell’app di raggiungere Internet. Una configurazione solo Wi-Fi, solo VPN o solo IPv6 non è supportata da questo trasporto. Si consigliano almeno 4 GB di RAM per comodità, senza dichiararlo un minimo misurato o imposto. Non servono GPU, CUDA o modelli AI. Programma, dati e destinazione devono essere scrivibili. Il deploy controlla almeno 512 MiB liberi oltre allo spazio dei download. Per torrent riservare spazio aggiuntivo per pezzi interi, confini dei file, destinazione e cache.

La lingua scelta richiede font installati che coprano i suoi caratteri. Su Linux minimale installare i pacchetti adatti della distribuzione, per esempio Noto CJK per cinese/coreano e una famiglia Noto araba. Java usa i font di ripiego della piattaforma; l’app non installa né include pacchetti di font. La copertura può variare tra computer.

## 3. Installazione e conferme di sicurezza

Scegliere un solo installer dalla [release 4.0](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.0), se allegato, e confrontarlo con lo SHA-256 pubblicato:

- Windows: `MultiRoute-Downloader-4.0-Windows-x64-Setup.exe`.
- Apple Silicon, macOS 26+: `MultiRoute-Downloader-4.0-macOS-arm64.pkg`.
- Intel, macOS 15+: `MultiRoute-Downloader-4.0-macOS-x86_64.pkg`.

### Windows

Aprire l’EXE, completare l’installazione per il proprio utente e avviare dal menu Start o dal collegamento sul desktop. L’app non si avvia in una finestra di script. Le nostre schermate di setup sono in inglese; le finestre di sicurezza seguono la lingua di Windows.

Al primo avvio viene controllato Npcap. Se manca, autorizzare il download nell’app: il file ufficiale fissato viene verificato tramite SHA-256 e identità Authenticode prima di aprire il normale installer Npcap. Leggere la licenza e approvare la richiesta UAC. Riavviare Windows se richiesto. Npcap non è incorporato e non è installato silenziosamente; se manca, serve Internet per questo passaggio. Un’installazione AdminOnly può richiedere autorizzazione alla cattura, mantenendo Java sotto l’utente normale.

L’app e l’installer non hanno firma del produttore. Se SmartScreen indica autore sconosciuto, verificare origine e hash prima di decidere se usare “Ulteriori informazioni → Esegui comunque”, quando disponibile. Un rilevamento malware è un caso diverso: non disabilitare l’antivirus e non creare esclusioni per installare.

### macOS

Aprire il PKG per il processore nativo in Apple Installer e autorizzare il componente protetto con la normale richiesta amministrativa. Avviare `/Applications/MultiRoute Downloader.app` da Finder, Spotlight o Dock. Questo pacchetto non richiede Terminale, Command Line Tools, Homebrew o Java separato.

Quando il componente deve aprire Ethernet BPF, compare la normale autorizzazione macOS. Dopo l’apertura dei dispositivi di cattura riduce i privilegi; Java rimane nell’account ordinario. Pacchetto e app **non sono firmati Developer ID né autenticati tramite notarizzazione**. Per un avviso di sviluppatore non verificabile con eccezione disponibile, controllare prima origine e SHA-256, poi seguire [Apple](https://support.apple.com/en-us/102445): Impostazioni di Sistema → Privacy e Sicurezza → Apri comunque, quindi Apri. Un Mac gestito può vietare l’eccezione.

L’autorizzazione all’installazione, quella al componente di rete e l’eccezione per autore sconosciuto sono conferme distinte. Quando richiesto, consentire **Rete locale** e il solo percorso di download scelto. Non disabilitare Gatekeeper/SIP, non rimuovere globalmente la quarantena e non eseguire tutta Java come root. Un avviso malware/file danneggiato non è il normale avviso sull’autore: fermarsi e verificare il file.

### Linux e ZIP portable 3.0

Estrarre uno ZIP completo. In Linux eseguire prima `bash ./Deploy-Linux.sh`, poi `bash ./Start-Linux.sh`. Nel portable Windows usare `Deploy-Windows.cmd` e poi `Start-Windows.cmd`; nel portable macOS `Deploy-macOS.command` e `Start-macOS.command`. Queste distribuzioni precedenti conservano gli script e sono separate dagli installer nativi 4.0. Linux può installare le dipendenze compilatore/libpcap dai repository; il portable macOS può richiedere Apple Command Line Tools e sudo. Conservare la sessione Terminale necessaria, senza avviare tutta Java come root. Alpine/musl non è incluso. Il deploy non aggiorna il sistema né sostituisce glibc.

Le dodici guide sono disponibili offline. Windows: `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`. macOS: Mostra contenuto pacchetto sull’app, poi `Contents/app/docs/it.md`. I collegamenti di aiuto nel browser richiedono Internet.

## 4. Gateway e indirizzo virtuale

I quattro gateway iniziali sono `192.168.1.1`, `192.168.1.2`, `192.168.1.3`, `192.168.1.4`, tutti selezionati. Si possono modificare ed estendere a **1–32** indirizzi distinti. Il fatto che il computer usi già uno di essi come gateway predefinito non crea una quinta connessione.

Il virtual IPv4 dell’app parte da `192.168.1.70`. Controllarlo e applicarlo nelle impostazioni di rete, verificando che nessun altro dispositivo lo usi; riservarlo fuori dall’assegnazione DHCP automatica. È dedicato al trasporto interno: **non viene aggiunto alla scheda del sistema e non cambia la tabella di routing del sistema operativo**. Gli altri programmi mantengono la connessione abituale. Non aggiungere manualmente questo IP alla configurazione di Windows/macOS/Linux. Il punto interrogativo e i suggerimenti ne spiegano lo scopo.

Controllare i gateway e selezionarne uno, diversi o tutti. Il test di velocità trasferisce al massimo **16 MiB o 5 secondi**, oltre all’eventuale preparazione. Sono limiti del test, non dei download. Senza un URL HTTP diretto adatto viene usato un file di prova Selectel.

Si possono aggiungere/rimuovere profili e cambiare i gateway del lavoro già avviato, purché ogni lavoro incompleto conservi almeno un gateway. Un nuovo profilo non viene automaticamente selezionato nei lavori esistenti. Cambiare il virtual IP richiede di mettere in pausa i lavori attivi. Gli indirizzi MAC restano nascosti nell’interfaccia.

## 5. Download e interfaccia

1. Incollare nel campo URL esteso con Ctrl+V o Cmd+V su Mac, oppure con il pulsante. Sono accettati magnet e file `.torrent`.
2. Scegliere la cartella con il selettore del sistema. Il valore iniziale è `%USERPROFILE%\Downloads` in Windows e `~/Downloads` in macOS/Linux; una cartella scelta manualmente viene conservata.
3. Scegliere i gateway e premere Avvia download accanto all’URL. Repository Hugging Face, elenchi WebDAV e Metalink permettono anteprima e selezione dei file. L’importazione multipla accetta un URL per riga e file di testo.
4. Osservare progresso individuale, tempo trascorso/rimanente, velocità totale e di ciascun gateway, volume e grafico opzionale. I segmenti HTTP sono **20 MiB** di default, regolabili da 1 a 64 MiB. Un segmento senza dati per **60 secondi** viene annullato e restituito al gruppo per un altro gateway. La dimensione dei pezzi torrent deriva dai metadati.
5. Sono disponibili pausa/ripresa, priorità, limiti, pianificazioni e cambio cartella per un lavoro fermo. La coda consente due lavori attivi per default. Dopo un riavvio i precedenti lavori attivi tornano in pausa. Il seeding torrent è disattivato per default e può avere limiti di tempo e rapporto.

FlatLaf offre temi chiaro, scuro e di sistema salvati. Il riepilogo mostra tutti i lavori, quelli attivi/completati e la velocità totale indipendentemente dal filtro. Ogni lavoro conserva la propria barra; colonne a tutta larghezza, scorrimento, ricerca, ordinamento e filtri rimangono disponibili.

Passando sopra una riga attiva o completata si leggono **nome completo del file e percorso assoluto di destinazione**, anche dopo ordinamento o filtro. Lo stesso suggerimento appare nel titolo selezionato; il menu contestuale copia il percorso. Ctrl+L/Cmd+L porta all’URL, Ctrl+F/Cmd+F alla ricerca, Escape azzera la ricerca.

Il lavoro selezionato ha schede Dettagli e Cronologia. Il rapporto mantiene posizione e selezione durante aggiornamenti e cambio lingua; un altro lavoro si apre dall’inizio. Il registro globale distingue eventi dell’applicazione e cronologia download. In fondo segue i nuovi eventi, mentre lascia leggere gli eventi precedenti senza salti.

Chiudere la finestra lascia i download attivi nell’area di notifica. L’icona mostra la somma di tutti i lavori fino a 10 Gbit/s, con dettaglio esatto nel suggerimento. Apri applicazione ripristina la finestra; Esci dall’applicazione salva e termina. Senza supporto tray, riduce alla barra delle applicazioni. **Uscire dal tray prima di aggiornare**.

Nella rimozione di un lavoro, l’opzione di eliminazione file è **deselezionata per default**. Si può rimuovere solo dalla lista lasciando file completo/parziale; eliminare i file solo intenzionalmente. La rimozione non cancella il registro storico permanente.

## 6. Protocolli, credenziali e verifica

| Fonte | Comportamento e requisiti |
| --- | --- |
| HTTP/HTTPS | Segmenti/ripresa con identità sicura e byte ranges; altrimenti flusso completo su un gateway, da riavviare da zero dopo interruzione |
| Hugging Face | File/repository, selezione e risoluzione Hub/Xet; le fonti private o limitate richiedono autorizzazione |
| FTP | EPSV/PASV passivo; FTP semplice non cifra credenziali né dati |
| FTPS | TLS esplicito su controllo e dati; FTPS implicito non supportato |
| SFTP | SSH con impronta SHA256 del server verificata indipendentemente e fissata; chiavi sconosciute non accettate automaticamente |
| WebDAV | Elenco HTTPS e selezione file |
| Metalink | `.meta4`, SHA-256 attendibile, mirror e hash dei pezzi quando presenti |
| Torrent/magnet | Trasporto sui gateway scelti, verifica obbligatoria dei pezzi dai metadati e priorità/selezione dei file |

Nell’accesso alle fonti si configurano utente/password, Bearer token, cookie o intestazioni consentite per l’origin esatto, con cifratura locale. Non sono token per i router; i gateway non ne richiedono. Non inserire password negli URL. FTP/FTPS/SFTP senza un SHA-256 o SHA-512 attendibile pubblicato o fornito manualmente usano il flusso completo.

Dopo l’assemblaggio l’app calcola **MD5, SHA-256 e SHA-512** in una lettura. Il campo principale riconosce automaticamente 32/64/128 cifre esadecimali; si possono aggiungere riferimenti. Cerca digest dell’intera rappresentazione e `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512`, `SHA512SUMS`, mantenendo Hugging Face LFS e Metalink. Un digest di una risposta HTTP parziale non rappresenta l’intero file.

Tutti i riferimenti noti devono coincidere prima della pubblicazione. Un errore conserva i dati parziali. Senza riferimento si vede solo un calcolo locale, non un confronto confermato con l’autore. MD5 rileva corruzione accidentale e non sostituisce SHA-256/SHA-512 o l’identità forte del file. Verifica dei pezzi torrent e hash completi sono distinti; un riferimento manuale completo richiede un solo file selezionato.

<a name="multiroute-api"></a>

## 7. API per altre applicazioni

A programma aperto l’API si trova su `http://127.0.0.1:8765/api/v1`. Richieste e risposte sono JSON UTF-8; usare `Content-Type: application/json` per il corpo. Non serve un API token per creare/gestire lavori locali; le credenziali della fonte sono separate. Accetta soltanto il computer locale: non esporre la porta in LAN o Internet. `--headless` esegue soltanto l’API. Usare il launcher nativo; gli esempi `.cmd` riguardano portable.

Controllo: `GET /api/v1/health`, risposta normale `{"ok":true,"version":"4.0"}`. Prima configurare la rete nella finestra o inviare a `POST /api/v1/network`:

```json
{
  "virtual_ip": "192.168.1.70",
  "gateways": ["192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"]
}
```

Inviare il seguente JSON a `POST /api/v1/jobs`, sostituendo URL e cartella reale sul computer dell’app. Per Linux: `/home/your-user/Downloads`; macOS: `/Users/your-user/Downloads`. Specificare esplicitamente `routes`: l’API non copia automaticamente le caselle della GUI.

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

`HTTP 201` significa creazione, non completamento. Conservare l’`id` e interrogare `GET /api/v1/jobs/{id}` fino a `status: complete`, oppure leggere `error`. `speed_bps` indica **byte al secondo**: moltiplicare per 8 e dividere per 1.000.000 per Mbit/s.

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

Pausa/ripresa accettano `{}`. Per corpo di eliminazione, conservazione file predefinita, `Idempotency-Key`, coda, batch, SSE, accesso e limiti, consultare [OpenAPI](../openapi.yaml). Si possono inviare `expected_md5`, `expected_sha256`, `expected_sha512`; i risultati espongono `md5`, `sha256`, `sha512`, `checksum_sources` e avvisi. Chiavi e stati JSON non cambiano con la lingua. [Altri esempi API](../API.ru.txt) sono in russo.

Esempio Python 3: sostituire l’URL di esempio, configurare prima la rete e lasciare aperta l’app. UTF-8 conserva anche percorsi Unicode.

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

## 8. Cronologia, dati e aggiornamento

| Sistema | Dati | Applicazione/runtime |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

Il componente nativo macOS è `/Library/PrivilegedHelperTools/MultiRouteResume/4.0/gateway-net`. Windows portable 3.0 usa `data/` presso il launcher, modificabile con `data-location.config` o `--data-dir`. Il runtime Linux privato è `~/.local/share/MultiRouteResume/runtime` e il componente protetto `/usr/local/libexec/MultiRouteResume/3.0/gateway-net`. I file scaricati sono nella destinazione scelta e possono essere altrove.

`logs/downloads-history.jsonl` è JSON per riga UTF-8; `logs/downloads-history.log` è testo leggibile datato. Vengono aggiunti eventi tra i riavvii senza riscrivere il passato al cambio lingua o alla rimozione del lavoro. Il testo mantiene la lingua di scrittura; la GUI rende gli eventi nella lingua attuale. La finestra legge al massimo la coda di 1.000 eventi/2 MiB, lasciando l’archivio completo sul disco. Cresce con l’uso: archiviarlo separatamente se necessario. Contiene nomi e percorsi assoluti, senza URL sorgenti, password o token; controllare i percorsi prima di condividerlo.

Gli eventi `lifecycle_history` hanno `event_id`, `job_id`, `kind`, `time`, `filename`, `path`; per torrent può comparire `paths` e per pausa `reason` (`pause`, `shutdown`, `maintenance`). Tipi: `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Date ISO 8601 UTC, ad esempio `2026-10-18T12:34:56.123Z`; il testo mostra data/ora locale completa e offset UTC. Gli opzionali `first_started_at`, `last_resumed_at`, `completed_at` riassumono inizio, ultima ripresa e completamento verificato/pubblicato realmente osservati. Un lavoro in coda non è ancora iniziato; non si inventano date passate. `recovered` è la verifica attuale di un vecchio file pubblicato, non la conoscenza della sua data di fine.

Prima dell’aggiornamento uscire dal tray, installare il pacchetto completo e conservare dati/parti. In Windows rimuovere da Impostazioni → App → App installate. Su Mac cancellare `.app` non rimuove automaticamente cronologia o componente protetto. Non eliminare la cartella dei download per disinstallare. Se si sposta la cartella Linux, ripetere il deploy.

## 9. Problemi e limiti delle verifiche

- Avvio: verificare versione, CPU, pacchetto completo e moduli; correggere `JAVA_EXE`/`JAVA_HOME` errati. Restano la correzione 3.9 dei file temporanei bloccati e l’identificazione della precedente istanza.
- Velocità zero: verificare Ethernet, sottorete, virtual IP libero, accesso Internet della fonte e del router. Un test di velocità riuscito non prova ogni download reale.
- Errore torrent glibc/macOS: usare un sistema compatibile, senza sostituire manualmente le librerie di sistema.
- Host-key SFTP diversa: verificare con l’amministratore senza disabilitare il controllo.
- Porta 8765 di un altro programma: usare `--port 8766`; non terminare processi estranei. Solo un’istanza precedente identificata come MultiRoute può essere sostituita.
- Spazio/permessi: controllare file, pezzi e cache; mettere in pausa prima di cambiare cartella. Riprendere il lavoro esistente per un bersaglio già presente.

Segnalare versione, OS/CPU, fase ed errore esatto, eliminando password, token, link privati e dati personali. Test controllati Windows/POSIX, Mac CI e controlli del pacchetto non dimostrano installazione reale, dialoghi Finder/Gatekeeper/BPF o velocità/failover dei router fisici. I pacchetti senza firma e notarizzazione sono una **prerelease**. Dopo l’installazione provare un file piccolo noto con ogni gateway, poi con la combinazione.

## 10. Licenza

L’applicazione originale può essere usata gratuitamente da privati e organizzazioni, anche in attività commerciali. Sono vietati vendita, addebito della licenza e distribuzione a pagamento senza permesso separato. La licenza personalizzata non concede diritti generali di modifica/ridistribuzione e non è una licenza open source approvata OSI. Fa fede l’inglese [LICENSE.txt](../LICENSE.txt). FlatLaf (Apache-2.0) e gli altri componenti mantengono le proprie licenze. Proprietario: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Nome e profili precedenti

MultiRoute Downloader si chiamava MultiRoute Resume. I nuovi profili usano i percorsi sopra; se la cartella nuova manca e un vecchio profilo esiste, viene riutilizzato senza spostare o cancellare file. I dati precedenti possono restare in `%LOCALAPPDATA%\MultiRoute Resume\data` o `~/Library/Application Support/MultiRoute Resume/data`. Restano compatibili directory protetta `MultiRouteResume`, ID di aggiornamento Windows, identità interna API e porta. Dopo l’installazione usare il nuovo collegamento o l’app MultiRoute Downloader. Le versioni storiche mantengono i vecchi nomi dei file.

## Siti salvati, caricamenti e anteprima archivi (4.0)

Aprire **Profili dei siti…**, creare/modificare il profilo e sceglierlo in **Download** o **Caricamenti**. Gateway e IP virtuale sono in **Impostazioni di rete**. In **Caricamenti** inserire l’**URL della cartella remota** e usare **Scegli file e carica…** per scegliere uno o più file locali con il selettore di sistema. L’interfaccia aggiunge i nomi; API `target_url` indica invece un file remoto completo. Incollare l’URL dell’archivio nel campo principale e usare **Contenuto dell’archivio…** per l’anteprima, senza avviare il download.

Un profilo salva nome, URL di base, cartella locale, cartella remota e gateway. Supporta accesso anonimo, HTTP Basic/Bearer/Cookie/intestazioni proprie, FTP e SFTP con impronta SHA-256 del server verificata prima. Più account dello stesso server restano separati. Cartelle/gateway espliciti prevalgono; gli URL relativi usano la base. I segreti sono cifrati in `site-profiles/vault.bin` con la chiave locale `site-profiles/key.bin` e permessi del proprietario. Chi controlla account e chiave può leggerli. L’API pubblica non restituisce segreti. Campi vuoti conservano le credenziali solo a origine e tipo invariati; `replace_credentials:true` le sostituisce. Una redirezione HTTP verso un’altra origine non riceve le credenziali. Eliminare il profilo conserva attività e file locali/remoti; l’attività conserva il legame al profilo originale e non cambia account; se è stato eliminato, ricreare l’attività con un profilo valido quando necessario.

Caricamento ed esplorazione remota supportano **FTP, FTPS esplicito e SFTP** tramite gateway scelti. Selezionare un file locale normale e un URL completo del file remoto (`target_url`), non una sola cartella. Un file temporaneo permette di riprendere dopo il confronto del prefisso. Il successo richiede rilettura remota completa e SHA-256: servono permessi di **lettura, scrittura e rinomina**, con traffico aggiuntivo. Una somma diversa o lettura negata è un errore. Ogni file usa un gateway per volta; più file vengono distribuiti e caricati in parallelo. Dopo il limite di inattività può subentrare un altro gateway; un singolo invio non viene suddiviso in parallelo. Pausa/ripresa e avanzamento persistono. Rimuovere l’attività conserva il file locale e i file remoti finali/temporanei.

La sovrascrittura è disattivata. FTP/SFTP non garantiscono ovunque una rinomina atomica soltanto se il destinatario non esiste; un altro client può modificarlo durante la pubblicazione. Coordinare i percorsi condivisi. Elencare cartelle non invia né elimina file.

L’anteprima di **ZIP, TAR, 7Z, RAR4 e RAR5** mostra nomi, percorsi, dimensioni, cartelle e cifratura senza estrarre o eseguire. Richiede un gateway, intervalli di byte ed ETag o Last-Modified stabile. Limiti: 16 MiB di metadati, 10.000 elementi, 60 secondi; decodifica intestazioni 7z fino a 64 MiB di memoria. L’elenco può essere parziale e non verifica l’integrità dei contenuti. Specificare la codifica originale se i nomi sono errati. Le intestazioni 7z cifrate supportate accettano una password non salvata. Le intestazioni RAR cifrate sono segnalate non supportate/password necessaria; archivi multivolume possono essere incompleti.

L’API accetta `site_profile_id` per scaricare, caricare, elencare e visualizzare. `GET /api/v1/site-profiles` restituisce `{profiles:[...]}`; `POST`/`PUT` salva un oggetto piatto e restituisce metadati pubblici; `DELETE /api/v1/site-profiles/{id}` elimina solo il profilo. `POST /api/v1/uploads` riceve `local_path`, `target_url`, `routes`, `site_profile_id` facoltativo e `overwrite:false`, con HTTP 201. GET restituisce `{uploads:[...]}`; sono disponibili GET/DELETE singoli e POST `/{id}/pause` / `/{id}/resume`. `POST /api/v1/remote/list` usa `url` e `route`/`routes`; `POST /api/v1/archives/preview` accetta anche `password` e `charset`. Vedere [openapi.yaml](../openapi.yaml). I manifesti UTF-8 in `data/uploads/` usano ID del profilo, non password. L’anteprima non crea un download.

## Pianificazione e priorità dei download

Nei controlli coda/orario impostare `max_active_jobs` (1–32), limiti globali/per gateway e finestra giornaliera (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm). Si usa il fuso locale, restituito come `schedule_timezone`; può attraversare mezzanotte e orari uguali significano tutto il giorno. Fuori finestra i download tornano in coda; le pause manuali restano. `priority` da -100 a 100 ordina gli elementi in attesa, valore alto prima poi creazione, senza interrompere quelli attivi. `start_after_ms` è l’istante minimo Unix in millisecondi, 0 rimuove il ritardo. Persiste ai riavvii e riguarda i download, non la coda upload indipendente. Applicazione e computer devono restare attivi; non viene programmato il risveglio OS. API: `GET`/`PUT`/`POST /api/v1/queue`, `PUT`/`POST /api/v1/jobs/{id}/options`.

## Integrazione Chrome ed Edge

Aprire **Chrome / Edge** nell’app, consentire l’invio dei link e aprire la cartella estensione. Una volta: `chrome://extensions` o `edge://extensions`, modalità sviluppatore, **Carica estensione non pacchettizzata**, quindi scegliere `browser-extension` con `manifest.json`. Il pacchetto nativo la include accanto al JAR; nel sorgente: [browser-extension/](../browser-extension/). Lasciarla al suo posto. Estrarre prima l’eventuale `MultiRoute-Downloader-4.0-Chrome-Edge.zip`. Nessuna pubblicazione negli store.

MultiRoute deve funzionare, anche nella tray. Inviare l’URL dal menu del link o dal popup. Usa cartella, gateway e profilo attuali; restano le selezioni repository/torrent. La porta predefinita è 8765, da adattare se l’API cambia. Viene inviato solo l’URL, senza cookie/password e senza intercettare altri download. Configurare l’accesso alla fonte nell’app. Concedere l’eventuale permesso di connessione locale soltanto a questa estensione scelta.

Il ponte è disattivato e accetta solo `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` su `/api/v1/browser/status` e `/api/v1/browser/download`; le altre regole API non cambiano. HTTP 202 indica consegna all’interfaccia, non file completato. Un server headless da solo non apre la selezione desktop. Rimuovere l’estensione o disabilitare l’integrazione per interrompere l’invio.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
