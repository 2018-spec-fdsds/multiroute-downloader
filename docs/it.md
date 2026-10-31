# MultiRoute Downloader 4.3.9 — Guida in italiano

[macOS: installazione, avvio e autorizzazioni](macos/it.md)

<!-- product-overview:start -->
[**Panoramica del prodotto**](overview/it.md)
<!-- product-overview:end -->

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [Home](../README.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

<!-- current437:start -->

<a name="current-free-pro"></a>

## 4.3.9 — Funzioni attuali, Free e Pro

Questa guida descrive MultiRoute Downloader 4.3.9 e le impostazioni attuali. Scaricare il pacchetto per la propria piattaforma dai [download pubblicati](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/latest), confrontare lo SHA-256 e seguire le istruzioni di installazione e sicurezza riportate sotto. Per 4.3.9 valgono i limiti Free e le funzionalità Pro seguenti.

### Limiti e funzioni della licenza

Free include download singoli normali, ripresa supportata, tutta la velocità disponibile, verifiche d’integrità, DNS protetto e velocità/stato attuali dei gateway. Non limita artificialmente la banda. I diritti firmati sono indipendenti: una licenza Pro parziale attiva solo quelli inclusi.

| Funzione | Free | Con diritto nella licenza | ID |
| --- | --- | --- | --- |
| Gateway usati insieme | ≤ 2 | ≤ 8 | `extra_gateways` |
| Download simultanei | ≤ 3 | ≤ 32 | `extra_concurrent_downloads` |
| Profili dei siti | ≤ 3 | Senza quota di licenza | `extra_site_profiles` |
| Priorità di download | Solo normale | Selezionabili | `download_priorities` |
| Priorità dei gateway | 0 | Selezionabili | `gateway_priorities` |
| Cronologia e report dei gateway | Non incluso | Incluso | `gateway_statistics` |
| Download / caricamenti in gruppo | Non incluso | Incluso | `batch_downloads` |
| API per applicazioni esterne | Non incluso | Incluso | `automation_api` |
| Temi aggiuntivi | Graphite / System | Tutti i temi | `premium_appearance` |

Per un gruppo, aprire **Nuovo download…** e scegliere **Elenco URL…** nella riga fissa delle azioni in fondo alla finestra.

Si attivano solo i diritti della licenza firmata. File e profili esistenti restano intatti. Gli altri file attendono in coda; velocità e stato attuali dei gateway restano disponibili. Restano i limiti tecnici di archiviazione.

Il limite dei gateway riguarda l’insieme condiviso da download e upload, senza un percorso di sistema aggiuntivo. Il limite di tre attività simultanee riguarda i download, non gli upload; gli altri download attendono. Un limite di coda configurato inferiore resta valido. Gateway, profili, avanzamento e priorità salvati non vengono eliminati. La quota limita la creazione di nuovi profili; quelli precedenti restano utilizzabili. Cronologia/CSV richiedono il relativo diritto; gli indicatori dal vivo restano Free. La pianificazione resta disponibile; le priorità personalizzate richiedono il loro diritto. Restano i limiti tecnici di spazio e sorgente.

La licenza firmata è legata alla chiave d’installazione, non a MAC, IP o router. Non scade; 4–4 copre 4.x. Cambiare gateway, DHCP o MAC applicativo non richiede riattivazione. Un altro computer, account o profilo può richiedere una nuova richiesta. Non si promette codice impossibile da modificare né vincolo hardware universale.

### MAC dell’applicazione

MultiRoute usa un proprio MAC stabile con IP manuale e DHCP. Il MAC della scheda del sistema non cambia. Vengono verificati conflitti con dispositivi osservati e altre istanze; nessuna risposta non esclude dispositivi spenti. Pool: MAC unicast locali con 46 bit casuali. Non è un pool di indirizzi IP DHCP.

**Cambia MAC…** — Il pulsante si usa dopo la pausa/fine dei trasferimenti attivi e in coda e la conclusione delle verifiche locali. Si controllano indirizzi osservati e annunci cooperativi di altre istanze. «Nessun conflitto rilevato» non garantisce che un dispositivo spento o silenzioso non usi l’indirizzo. Il MAC applicativo deve differire da quello fisico; adattatore e rotte del sistema restano invariati.

### Somma di controllo…

Il riferimento viene salvato senza riavviare o azzerare l’avanzamento. MD5, SHA-256 o SHA-512 è riconosciuto da 32, 64 o 128 cifre esadecimali. Il file assemblato sarà verificato al termine. Salva il riferimento e scegli Verifica ora. Viene letto il file locale completo. Una differenza non causa un nuovo download o l’eliminazione.

La verifica finale usa l’ultimo riferimento salvato. Modificarlo non cancella byte né riavvia il trasferimento. La verifica locale conserva il file completato e segnala corrispondenza, differenza o errore separatamente. I riferimenti dell’editore restano indipendenti. Una somma globale riguarda un solo file torrent. L’integrità non prova l’innocuità del contenuto; preferire riferimenti SHA-256/SHA-512 attendibili.

### Accesso API in 4.3.9

L’API JSON locale usa UTF-8 senza normale token API. Il controllo esterno richiede automation_api; vengono verificati anche gli altri diritti. Questo diritto da solo non abilita gruppi o gateway aggiuntivi. Free conserva diagnostica limitata in lettura, ciclo di vita con autenticazione privata e ponte opzionale per un singolo link dal browser. Vedere [API](../API.ru.txt) e [OpenAPI](../openapi.yaml); non esporre la porta a LAN/Internet.


L’applicazione originale usa la [Licenza 1.2](../LICENSE.txt): Free consente uso personale e aziendale; il titolare può offrire Pro a pagamento. Gli altri destinatari non possono venderla senza permesso separato. OpenJDK GPLv2 con Classpath Exception richiede avvisi/sorgenti corrispondenti; JNA usa Apache-2.0 con avvisi; JLayer LGPL-2.1 separata conserva diritti di sostituzione/debug e sorgenti. Vedere [condizioni](../LICENSING.md), [catalogo completo](../THIRD-PARTY-NOTICES.txt) e pagina delle licenze.

Lo stile System mantiene leggibili il testo delle tabelle, i campi e i controlli sullo sfondo di sistema; Graphite e System sono inclusi in Free.

Nome e dimensione del file hanno colonne separate. La dimensione viene ordinata numericamente, indipendentemente dal nome; una dimensione sconosciuta è indicata da un trattino. Nome e percorso completi rimangono disponibili nel suggerimento.

Passare il puntatore sul nome per vedere nome completo, percorso di salvataggio, protocollo e indirizzo o percorso locale della sorgente. Le attività torrent indicano BitTorrent anche se i metadati sono stati aperti tramite HTTPS. L’indirizzo del repository, se disponibile, segue l’URL effettivo del file. Credenziali incorporate, valori di query e frammenti sono nascosti; un infohash magnet pubblico riconosciuto può restare visibile. URL salvato e stato del trasferimento non cambiano.


### Priorità dei gateway e diagnostica dei trasferimenti in 4.3.9

Nelle impostazioni di rete assegna a ogni gateway una priorità da **0 a 1000**; il valore appare anche accanto al suo indicatore principale. Un numero maggiore favorisce il gateway per file piccoli o trasferimenti completi e gli assegna più segmenti paralleli per file grandi. Gli altri gateway selezionati e funzionanti continuano a contribuire. Valori uguali conservano l’ordine di selezione; un altro gateway selezionato può subentrare se quello preferito non funziona o si blocca. Non è garantito un rapporto preciso tra le velocità.

Serve il diritto firmato indipendente **`gateway_priorities`**. Il diritto di priorità delle attività **`download_priorities`** non lo comprende. Senza il diritto per i gateway, tutte le priorità effettive sono 0 e i valori salvati restano disponibili. È possibile modificarli durante il download senza cancellare le parti verificate. Ogni gateway selezionato utilizza da uno a quattro processi di segmenti paralleli, fino a 32 per attività.

Le impostazioni consentono di scegliere il numero di download simultanei entro il limite della licenza e tentativi automatici limitati o illimitati. Le pause manuali restano attive. Il menu contestuale permette di sospendere/riprendere le attività selezionate compatibili e copiare i nomi dei file. Il doppio clic apre solo la cartella; proprietà, statistiche e registro individuale sono nel menu contestuale.

Il registro individuale resta disponibile anche per i file completati. Impostazioni → Errori e soluzioni apre una guida locale ricercabile con 25 argomenti in tutte le 12 lingue dell’interfaccia. Destinazione e proprietà mostrano lo spazio libero in GB. Un disco locale pieno viene segnalato esplicitamente: le parti restano salvate e l’errore non genera tentativi di rete infiniti. Il prefisso salvato rimane visibile nell’avanzamento mentre un piccolo segmento ripreso attende nuovi dati.

### Dati ancora da scaricare · 4.3.9

La tabella include la colonna **Dati rimanenti**. Mostra i byte ancora da scaricare separatamente dal tempo stimato e si ordina numericamente. Tiene conto dell’avanzamento salvato e in corso, non scende sotto zero e mostra un trattino quando la dimensione totale è sconosciuta.

### Avvio Mac e configurazione dei gateway più affidabili · 4.3.9

L’avvio nativo su Mac riconosce il vecchio percorso predefinito relativo, anche senza barra iniziale, e usa un profilo esistente verificato o una cartella predefinita vuota dell’utente. I percorsi personalizzati ambigui richiedono una scelta esplicita.

Dopo un errore o l’annullamento della preparazione, gli altri gateway e i controlli in background smettono di ripetere le richieste di autorizzazione fino a un nuovo tentativo esplicito di configurazione della rete. Il primo avvio privilegiato può ancora richiedere la normale conferma amministratore di macOS per aprire il dispositivo BPF Ethernet.

Il componente di rete Mac 4.3.2 gestisce un MAC dell’adattatore nascosto da macOS mantenendo i controlli di protezione della scheda fisica. L’app conserva il proprio MAC senza cambiare le route del sistema o il MAC fisico. Icona e nome Mac adottano la stessa identità MultiRoute di Windows. Esci dall’app prima dell’aggiornamento PKG. Il nome del richiedente nel dialogo reale e i trasferimenti LAN/DHCP richiedono ancora verifica su hardware; questo trasporto richiede tuttora Ethernet cablata.

**macOS:** `/Applications/MultiRoute Downloader.app` · `/Library/PrivilegedHelperTools/MultiRouteResume/4.3.2/{gateway-net,gateway-launch}`.

### Risposte dei gateway Mac con indirizzo manuale · 4.3.9

La cattura POSIX viene corretta affinché indirizzo manuale dell’app e DHCP ricevano entrambi le risposte al MAC proprio dell’app, come già su Windows. Un ping di sistema riuscito verifica il collegamento del computer, non la ricezione sull’indirizzo virtuale dell’app.

La diagnostica DHCP conserva il testo specifico nelle dodici lingue invece di un errore generico. L’autorizzazione esistente resta invariata: i normali controlli riutilizzano il componente preparato. Non cambiano endpoint API, controlli, route del sistema o MAC fisico. Il primo accesso privilegiato a Ethernet BPF può ancora richiedere la normale conferma amministratore.

Si corregge una differenza nel modo di cattura, non ogni causa di gateway irraggiungibile. Restano attivi i controlli di indirizzo libero, sottorete e lease/conflitti DHCP. I trasferimenti Mac con indirizzo manuale richiedono ancora conferma su hardware; Ethernet cablata resta necessaria.

<!-- current437:end -->

<a name="appearance-and-sounds"></a>

<a name="aspetto-e-suoni-di-completamento-in-424"></a>

## Aspetto e suoni di completamento

Aprire le **Impostazioni aspetto** e scegliere fra i nove stili: **Graphite** (predefinito), **Nordic**, **StreamsStudio**, **Neon**, **Community**, **Chat**, **Studio**, **Rose**, **System**. Free include Graphite e System; gli altri stili richiedono la funzionalità Pro corrispondente `premium_appearance`. La scelta viene salvata localmente senza riavviare i download. **Finestre e pulsanti arrotondati** è un’opzione separata. I controlli possono essere arrotondati in tutti gli stili; il bordo esterno dipende dal sistema operativo. Windows 11 può mantenere rettangolari le finestre ingrandite o affiancate; macOS e Linux gestiscono autonomamente i propri bordi. Il grafico della velocità mostra l’attività dei trasferimenti e le schede dei gateway raccolgono le rispettive impostazioni nella finestra di rete.

I suoni sono **disattivati per impostazione predefinita**. Attivare la casella principale, poi scegliere separatamente gli avvisi **per ogni file** e **per tutti i download**. Sono disponibili **Campanelli delicati**, **Piano** e **Marimba**; entrambe le melodie si possono ascoltare in anteprima. Quando tutti i download sono completi e l’avviso generale è attivo, per l’ultimo file suona soltanto la melodia finale, senza un secondo segnale sovrapposto. Le attività in pausa o non riuscite non sono complete. La cronologia precedente resta silenziosa all’avvio e quando si cambiano le preferenze. Controllare anche volume e uscita audio del sistema.

Per usare suoni propri, scegliere **Melodia personalizzata** e selezionare file **MP3 o WAV** distinti per il completamento di un file e di tutti i download. Ogni brano deve durare **al massimo 10 secondi**. L’app verifica la durata dell’audio decodificato, non soltanto estensione o metadati. I file accettati vengono copiati nel profilo locale dell’applicazione; in seguito gli originali possono essere spostati.

Nelle **Impostazioni aspetto** si può mostrare o nascondere l’**avatar MultiRoute**: un widget opzionale separato sul desktop, con un personaggio uomo o donna. Regolare la scala fra **40–100%** e fare doppio clic con il pulsante sinistro sull’avatar per ripristinare la finestra dell’applicazione. Il movimento al passo o di corsa segue la **velocità totale effettiva dei download**; a riposo il personaggio si ferma. Il sacco e la sua etichetta riflettono la **somma dei byte rimanenti noti dei download attivi**: ogni file conta una sola volta indipendentemente dai gateway e non viene inventata una quantità residua per i file di dimensione sconosciuta. La visibilità viene salvata e l’animazione si ferma se nascosta o inattiva. È soltanto una rappresentazione visiva: i file restano nelle cartelle scelte e l’avatar è indipendente dai suoni di completamento. I personaggi sono immagini originali generate, senza affiliazione a Codex o OpenAI.

Aspetto, angoli, suoni e avatar sono impostazioni locali dell’interfaccia desktop. Non aggiungono endpoint all’API di download e non modificano trasferimenti, checksum o sicurezza DNS.

<a name="startup-profile-location"></a>

<a name="correzione-del-percorso-del-profilo-allavvio-in-423"></a>

## Percorso del profilo all’avvio

L’applicazione installata salva un percorso assoluto della cartella dati, indipendente dalla directory di lavoro all’accesso. Un percorso relativo salvato viene convertito in assoluto solo quando un profilo esistente è identificabile senza ambiguità. Cronologia, impostazioni e parti salvate restano al loro posto. I percorsi relativi della versione portabile e `--data-dir` esplicito sono supportati secondo la rispettiva modalità di avvio.

<a name="automatic-startup"></a>

<a name="avvio-automatico-in-421"></a>

## Avvio automatico

Attiva **Avvia con il sistema operativo** vicino ai controlli di lingua e aspetto. È **disattivato per impostazione predefinita**. Registra MultiRoute per il prossimo accesso al tuo account; disattivarlo rimuove la voce propria dell’app. È un avvio all’accesso dell’utente, non un servizio di sistema né un avvio prima dell’accesso. La registrazione non richiede privilegi di amministratore.

L’avvio automatico usa l’area di notifica se disponibile; altrimenti mostra una normale finestra accessibile. Restano in vigore priorità, pause e pianificazioni della coda; i download sospesi non vengono forzati a riprendere. Apri prima manualmente l’app per completare installazione e prerequisiti di rete. Mantieni l’app e, nell’uso portatile, Java e le librerie nei percorsi registrati. Dopo uno spostamento, disattiva e riattiva l’avvio dalla nuova posizione. Disattivalo prima della disinstallazione.

Il sistema o i criteri del desktop possono disabilitare una voce registrata. Su macOS, controlla **Impostazioni di Sistema → Generali → Elementi login ed estensioni** se viene richiesta l’approvazione; l’autorizzazione di rete è separata. La registrazione non dimostra un futuro avvio riuscito. I test con profili propri non includono un vero riavvio o logout; consulta il rapporto della versione.

Se non riesci ad aprire l’app, elimina **solo la sua voce**:

- **Windows:** l’app salva il proprio valore `REG_SZ` **MultiRoute Downloader** in `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`. È preferibile disattivare l’avvio dall’app. Se non si apre, elimina **solo quel valore** con l’Editor del Registro, mantenendo la chiave Run e le altre voci. Se il sistema lo ha disabilitato, usa **Impostazioni → App → Avvio**; l’app rispetta tale decisione. Windows limita il comando completo a 260 caratteri; se è troppo lungo, usa un percorso dell’app/profilo più breve e registralo di nuovo.
- **macOS:** rimuovi `~/Library/LaunchAgents/io.github.2018-spec-fdsds.multiroute-downloader.plist`.
- **Linux:** rimuovi `${XDG_CONFIG_HOME:-~/.config}/autostart/io.github.2018-spec-fdsds.multiroute-downloader.desktop` dalla directory XDG effettiva. Serve un desktop grafico che supporti l’avvio XDG; non viene installato un servizio systemd.

<a name="dns-settings"></a>

<a name="impostazioni-dns-in-422"></a>

## Impostazioni DNS

**Verificare la versione dell’installer e lo SHA-256 pubblicato; consultare il NATIVE-INSTALLER-TESTS.json corrispondente per i risultati di piattaforme e pacchetti.**

Aprire **Rete → Risoluzione DNS**. Il valore predefinito è **DNS su HTTPS (DoH), Cloudflare**. L’impostazione riguarda solo MultiRoute e non cambia DNS, rotte o indirizzi IP del sistema.

DoH supporta HTTP/2 con limiti, selezionato tramite TLS ALPN, e HTTP/1.1 quando negoziato. Compatibilità dei fornitori e risultati reali dei gateway sono nel rapporto finale della relativa release.

- **DoH:** DNS cifrato su HTTPS attraverso ogni gateway selezionato. Il certificato TLS è verificato rispetto al nome nell’URL.
- **DoT:** DNS cifrato su TLS attraverso ogni gateway, porta 853 predefinita; verifica del nome del server.
- **DNS di sistema (computer):** usare esplicitamente la politica DNS di computer, SO o VPN. Il DNS può non seguire ciascun gateway selezionato; le connessioni dei file mantengono il proprio gateway. Questa modalità non garantisce la cifratura DNS.

Scegliere Cloudflare, Quad9, Google o **Personalizzato**. I preset sono fissi; Personalizzato abilita URL/nome, porta e indirizzi IPv4 pubblici numerici di bootstrap. Permettono la connessione al fornitore senza risolverne prima il nome tramite il SO. DoH/DoT verificano catena di fiducia, nome e SNI, senza ripiego silenzioso su DNS in chiaro o di sistema. Un errore sicuro termina quel tentativo; il fornitore deve essere raggiungibile dal gateway selezionato.

Modificare una bozza, scegliere i gateway e premere **Prova DNS**. Il test non salva e mostra risultato, tempo e IPv4 pubblici o errore per gateway. I guasti parziali restano visibili. In modalità Sistema il successo verifica la risoluzione del SO, non la WAN indipendente di ogni router. **Applica impostazioni DNS** convalida e salva; la revisione restituita identifica la configurazione applicata. Aggiornamenti e cambi di lingua conservano bozza, cursore e scorrimento.

Ogni query ha un limite totale di 8 secondi, comprese le query DNSSEC ausiliarie. Il TTL convalidato rispetta anche la scadenza della firma; la cache nativa è limitata a 300 secondi e separata per gateway/revisione. TTL zero e HTTP Age DoH restano significativi. Sistema segue la cache OS/JVM. Sono supportate destinazioni IPv4 pubbliche; questa versione non aggiunge risoluzione IPv6.

API: `GET /api/v1/network/dns` legge configurazione e stato in sola lettura; `PUT` o `POST` applica la configurazione. `POST /api/v1/network/dns/test` prova una bozza; `GET /api/v1/network/dns/providers` elenca i preset. Inviare solo i campi di configurazione, senza revisione/stato di GET. [API](../API.ru.txt) · [OpenAPI](../openapi.yaml).

<a name="dnssec-validation"></a>

<a name="convalida-dnssec-locale-in-422"></a>

## Convalida DNSSEC locale

In **Rete → Risoluzione DNS → Convalida DNSSEC**, scegli **Verifica domini firmati** (predefinito DoH/DoT), **Richiedi risposte DNSSEC sicure** o **DNSSEC disattivato**. Java convalida localmente con dnsjava e ancore radice incluse. Il solo flag AD del fornitore non è una prova. DoH/DoT cifra il trasporto; DNSSEC autentica separatamente i dati DNS.

**Verifica** accetta risposte sicure e non protette; **Richiedi** solo quelle sicure. Non protetta può indicare una delega non firmata autenticata o algoritmi non supportati: leggi il motivo. Risultati non validi e indeterminati vengono rifiutati con la verifica attiva. Il DNS di sistema disattiva la convalida locale; tornando a DoH/DoT viene ripristinato il criterio precedente. Cambiare fornitore lo conserva. Applica salva; Test DNS verifica la bozza non salvata.

Ogni query distingue `secure`, `insecure`, `bogus`, `indeterminate`, `not_checked`; un risultato assente è indicato come non disponibile. Una connessione riuscita non dimostra DNSSEC sicuro. Scadenza, chiavi/prove mancanti, errori e timeout hanno motivi distinti. L’orologio deve essere corretto. Le query ausiliarie usano lo stesso gateway e tempo totale. DNSSEC non nasconde nomi e non controlla il contenuto scaricato.

API: `dnssec` accetta `off`, `validate`, `require`; Sistema solo `off`. Sono di sola lettura `dnssec_validation`, `dnssec_trust_anchors` e, per query, `dnssec_status`, `dnssec_reason_code`, `dnssec_reason`, `dnssec_validated_ttl`, `dnssec_aux_queries`. Non reinviarli nel salvataggio. Vedi API/OpenAPI sopra e [DNSSEC](#convalida-dnssec-locale-in-422).

**Gestione degli intervalli HTTP:** se una CDN come Civitai risponde HTTP 200 a un intervallo che parte da zero, viene controllato anche un intervallo con inizio diverso da zero. Il primo segmento resta limitato alla sua dimensione; i dati sequenziali salvati compatibili possono proseguire come segmenti. Un HTTP 200 temporaneo per un intervallo successivo viene ritentato dopo 2–30 secondi, conservando i dati e il worker del gateway. Il registro persistente per gateway distingue il traffico ricevuto dall’avanzamento salvato. [Diagnostica trasferimenti](transfer-diagnostics.en.md).

[Gli installer 4.3.9](../downloads/v4.3.9/README.md) includono Java 21 privato. Il programma di installazione Windows è in inglese e crea un collegamento sul desktop; Npcap resta necessario separatamente. macOS usa libpcap di sistema. Seguire le istruzioni di installazione e sicurezza sotto.

Proprietario del progetto: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

<a name="1-scopo-e-funzionamento"></a>

## Scopo e funzionamento

MultiRoute Downloader è un’applicazione desktop Java per scaricare file usando contemporaneamente più **gateway Internet** nella stessa rete Ethernet locale. Per esempio, un router può usare una linea fissa e un altro una connessione mobile 4G/5G. I gateway selezionati prelevano segmenti da una coda condivisa: quelli più veloci possono scaricarne di più. Se una connessione si interrompe, gli altri continuano; i segmenti incompleti tornano alla coda e i dati già verificati rimangono disponibili. È possibile importare gruppi di link o creare lavori tramite l’API locale.

L’aumento di velocità dipende dalla capacità indipendente delle linee, dai limiti del server e dal supporto alla ripresa sicura. Non è garantito un moltiplicatore pari al numero di router. L’interfaccia è un’app desktop, non una pagina web. Il nuovo profilo parte in inglese; sono disponibili russo, tedesco, spagnolo, francese, cinese, coreano, arabo, italiano, giapponese, hindi e vietnamita. La scelta viene salvata e applicata a messaggi registrati, log e suggerimenti. Un errore grezzo di un server o di una libreria esterna può restare nella lingua originale. Nomi, percorsi, URL e chiavi JSON non vengono tradotti.

<a name="application-virtual-ip"></a>

### Perché l’applicazione usa un IP virtuale

**Scegli i gateway di MultiRoute senza cambiare la connessione degli altri programmi.** L’applicazione usa un proprio indirizzo IPv4 locale libero per il trasporto di rete interno. Può così aprire connessioni attraverso i gateway selezionati indipendentemente dal gateway predefinito del computer, senza modificare gli indirizzi delle schede di sistema, la tabella di routing o il gateway predefinito del sistema operativo. Il browser e gli altri programmi mantengono le proprie impostazioni di rete.

Per esempio, in una LAN `192.168.1.0/24`, il computer conserva il suo normale indirizzo `192.168.1.x`, MultiRoute può usare l’indirizzo libero `192.168.1.70` e i download possono passare da `192.168.1.1`–`192.168.1.4`. Computer, indirizzo virtuale e gateway selezionati devono appartenere alla stessa sottorete IPv4 reale. Verifica e applica un indirizzo libero nelle impostazioni di rete dell’applicazione; non aggiungerlo manualmente alla scheda del sistema operativo. L’IP virtuale non crea un ulteriore accesso a Internet: la banda proviene dai router e dalle loro connessioni esterne.

#### Scegliere e controllare l’indirizzo

1. Controlla IPv4 e maschera della scheda Ethernet del computer. Scegli un indirizzo host utilizzabile nella stessa sottorete. `192.168.1.70` è solo un esempio: non usare l’IP del computer, quello di un gateway, l’indirizzo di rete o quello di broadcast.
2. Apri l’amministrazione del router e controlla l’intervallo DHCP effettivo. Usa un indirizzo fuori dall’assegnazione automatica oppure riservalo/escludilo sul server DHCP. Se più server DHCP servono la LAN, tutti devono impedire l’assegnazione di quell’indirizzo a un altro dispositivo.
3. In MultiRoute scegli **Verifica e applica** per l’indirizzo virtuale, poi controlla i gateway. L’app valida la sottorete e cerca conflitti mediante ARP. Nessuna risposta ARP significa soltanto che non è stato rilevato un conflitto in quel momento: non dimostra che l’indirizzo sia fuori dal pool DHCP e non impedisce un’assegnazione successiva.

## Ambiente necessario

Gli installer Windows/macOS includono Java 21 privato e il componente di rete compilato. Windows richiede separatamente il driver Npcap. Non occorre installare Java a parte.

| Piattaforma | Sistema operativo | Componenti |
| --- | --- | --- |
| Windows x64 | Windows 11 | Java 21 |
| Apple Silicon | macOS 26+ | Java 21 |
| Intel Mac | macOS 15+ | Java 21 |

Servono Ethernet cablata nella stessa sottorete IPv4 di tutti i gateway scelti, un IPv4 virtuale libero, cartelle dati/download scrivibili e spazio per il file e le parti temporanee. Il guadagno dipende da connessioni WAN indipendenti e dalla capacità del server. Non serve una GPU.

L’applicazione supporta Linux, ma questo repository di distribuzioni non pubblica un installer Linux 4.x. Non usare su Linux i pacchetti Windows o macOS.

Prima di aggiornare attendere il completamento di download, caricamenti e verifiche finali attivi. Uscire quindi dal menu della tray ed eseguire il nuovo installer completo. Conservare la cartella dati, i file scaricati e le parti salvate.

<a name="3-installazione-e-conferme-di-sicurezza"></a>

## Installazione e conferme di sicurezza

Scegliere un solo installer dalla [release 4.3.9](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9), se allegato, e confrontarlo con lo SHA-256 pubblicato:

- Windows: [MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe).
- Apple Silicon, macOS 26+: [MultiRoute-Downloader-4.3.9-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg).
- Intel, macOS 15+: [MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg).

### Windows

Aprire l’EXE, completare l’installazione per il proprio utente e avviare dal menu Start o dal collegamento sul desktop. L’app non si avvia in una finestra di script. Le nostre schermate di setup sono in inglese; le finestre di sicurezza seguono la lingua di Windows.

Al primo avvio viene controllato Npcap. Se manca, autorizzare il download nell’app: il file ufficiale fissato viene verificato tramite SHA-256 e identità Authenticode prima di aprire il normale installer Npcap. Leggere la licenza e approvare la richiesta UAC. Riavviare Windows se richiesto. Npcap non è incorporato e non è installato silenziosamente; se manca, serve Internet per questo passaggio. Un’installazione AdminOnly può richiedere autorizzazione alla cattura, mantenendo Java sotto l’utente normale.

L’app e l’installer non hanno firma del produttore. Se SmartScreen indica autore sconosciuto, verificare origine e hash prima di decidere se usare “Ulteriori informazioni → Esegui comunque”, quando disponibile. Un rilevamento malware è un caso diverso: non disabilitare l’antivirus e non creare esclusioni per installare.

### macOS

Aprire il PKG per il processore nativo in Apple Installer e autorizzare il componente protetto con la normale richiesta amministrativa. Avviare `/Applications/MultiRoute Downloader.app` da Finder, Spotlight o Dock. Questo pacchetto non richiede Terminale, Command Line Tools, Homebrew o Java separato.

Quando il componente deve aprire Ethernet BPF, compare la normale autorizzazione macOS. Dopo l’apertura dei dispositivi di cattura riduce i privilegi; Java rimane nell’account ordinario. Pacchetto e app **non sono firmati Developer ID né autenticati tramite notarizzazione**. Per un avviso di sviluppatore non verificabile con eccezione disponibile, controllare prima origine e SHA-256, poi seguire [Apple](https://support.apple.com/en-us/102445): Impostazioni di Sistema → Privacy e Sicurezza → Apri comunque, quindi Apri. Un Mac gestito può vietare l’eccezione.

L’autorizzazione all’installazione, quella al componente di rete e l’eccezione per autore sconosciuto sono conferme distinte. Quando richiesto, consentire **Rete locale** e il solo percorso di download scelto. Non disabilitare Gatekeeper/SIP, non rimuovere globalmente la quarantena e non eseguire tutta Java come root. Un avviso malware/file danneggiato non è il normale avviso sull’autore: fermarsi e verificare il file.

<a name="4-gateway-e-indirizzo-virtuale"></a>

## Gateway e indirizzo virtuale

I profili di esempio dei gateway usano `192.168.1.1`, `192.168.1.2`, `192.168.1.3` e `192.168.1.4`. Si possono modificare i profili e configurare **1–8** indirizzi distinti. Modificarli per la propria rete, controllare i gateway e selezionare manualmente quelli da usare: la presenza di quattro profili non significa che tutti e quattro i gateway siano attivi. Free permette fino a **2 gateway** contemporanei; la funzionalità Pro corrispondente `extra_gateways` ne permette fino a **8**. Il limite riguarda l’insieme condiviso da download e upload. L’uso di uno di questi gateway come predefinito del computer non aggiunge un’altra connessione Internet.

Il virtual IPv4 dell’app parte da `192.168.1.70`. Controllarlo e applicarlo nelle impostazioni di rete, verificando che nessun altro dispositivo lo usi; riservarlo fuori dall’assegnazione DHCP automatica. È dedicato al trasporto interno: **non viene aggiunto alla scheda del sistema e non cambia la tabella di routing del sistema operativo**. Gli altri programmi mantengono la connessione abituale. Non aggiungere manualmente questo IP alla configurazione di Windows/macOS/Linux. Il punto interrogativo e i suggerimenti ne spiegano lo scopo.

Controllare e selezionare uno o più gateway entro il limite della licenza. Il test trasferisce al massimo **16 MiB o 5 secondi**, oltre all’eventuale preparazione. È solo un limite del test, non dei download. Senza un URL HTTP diretto adatto viene usato un file di prova Selectel.

Si possono aggiungere o rimuovere profili e cambiare i gateway di un lavoro in corso entro il limite della licenza, lasciandone almeno uno per ogni lavoro incompleto. Un profilo aggiunto non viene selezionato automaticamente nei lavori esistenti. Cambiare l’IP virtuale richiede la pausa dei lavori attivi. Il MAC applicativo si gestisce nelle impostazioni di rete; il MAC della scheda fisica resta invariato e valgono le condizioni di pausa e controllo indicate nella sezione MAC sopra.

<a name="5-download-e-interfaccia"></a>

## Download e interfaccia

1. Incollare nel campo URL esteso con Ctrl+V o Cmd+V su Mac, oppure con il pulsante. Sono accettati magnet e file `.torrent`.
2. Scegliere la cartella con il selettore del sistema. Il valore iniziale è `%USERPROFILE%\Downloads` in Windows e `~/Downloads` in macOS/Linux; una cartella scelta manualmente viene conservata.
3. Scegliere i gateway e premere Avvia download accanto all’URL. Repository Hugging Face, elenchi WebDAV e Metalink permettono anteprima e selezione dei file. L’importazione multipla accetta un URL per riga e file di testo.
4. Osservare progresso individuale, tempo trascorso/rimanente, velocità totale e di ciascun gateway, volume e grafico opzionale. I segmenti HTTP sono **20 MiB** di default, regolabili da 1 a 64 MiB. Un segmento senza dati per **60 secondi** viene annullato e restituito al gruppo per un altro gateway. La dimensione dei pezzi torrent deriva dai metadati.
5. Sono disponibili pausa/ripresa, priorità, limiti, pianificazioni e cambio cartella per un lavoro fermo. La coda consente due lavori attivi per default. Dopo un riavvio i precedenti lavori attivi tornano in pausa. Il seeding torrent è disattivato per default e può avere limiti di tempo e rapporto.

FlatLaf offre gli stili descritti sopra e conserva la scelta localmente. Il riepilogo conta tutte le attività, quelle attive, quelle complete e la velocità totale indipendentemente dai filtri. Restano disponibili barre individuali, colonne a tutta larghezza, ordinamento, ricerca, filtro di stato e scorrimento con posizione di lettura conservata.

Passando sopra una riga attiva o completata si leggono **nome completo del file e percorso assoluto di destinazione**, anche dopo ordinamento o filtro. Lo stesso suggerimento appare nel titolo selezionato; il menu contestuale copia il percorso. Ctrl+L/Cmd+L porta all’URL, Ctrl+F/Cmd+F alla ricerca, Escape azzera la ricerca.

Il lavoro selezionato ha schede Dettagli e Cronologia. Il rapporto mantiene posizione e selezione durante aggiornamenti e cambio lingua; un altro lavoro si apre dall’inizio. Il registro globale distingue eventi dell’applicazione e cronologia download. In fondo segue i nuovi eventi, mentre lascia leggere gli eventi precedenti senza salti.

Chiudere la finestra lascia i download attivi nell’area di notifica. L’icona mostra la somma di tutti i lavori fino a 10 Gbit/s, con dettaglio esatto nel suggerimento. Apri applicazione ripristina la finestra; Esci dall’applicazione salva e termina. Senza supporto tray, riduce alla barra delle applicazioni. **Uscire dal tray prima di aggiornare**.

Selezionare più righe di download con **Ctrl+clic** su Windows/Linux o **Cmd+clic** su macOS; **Maiusc+clic (Shift)** seleziona un intervallo. Con il focus nella tabella, **Ctrl+A** o **Cmd+A** seleziona tutte le righe attualmente visibili secondo la ricerca e il filtro di stato. La rimozione si applica all’intera selezione con una sola conferma. L’opzione per eliminare anche i file completi e le parti scaricate è **deselezionata per impostazione predefinita**: si possono rimuovere soltanto le voci conservando i dati. Selezionarla solo per cancellare i file di quei lavori; gli altri file e la cartella di destinazione restano. Il risultato mostra il numero di voci rimosse. I lavori che non è stato possibile rimuovere rimangono nell’elenco con un riepilogo degli errori, per controllarli e riprovare; le rimozioni riuscite non vengono ripetute. Rimuovere lavori o file non cancella la cronologia permanente dei download.

<a name="6-protocolli-credenziali-e-verifica"></a>

## Protocolli, credenziali e verifica

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

<a name="7-api-per-altre-applicazioni"></a>

## API per altre applicazioni

A programma aperto l’API si trova su `http://127.0.0.1:8765/api/v1`. Richieste e risposte sono JSON UTF-8; usare `Content-Type: application/json` per il corpo. Non serve un API token per creare/gestire lavori locali; le credenziali della fonte sono separate. Accetta soltanto il computer locale: non esporre la porta in LAN o Internet. `--headless` esegue soltanto l’API. Usare il launcher nativo; gli esempi `.cmd` riguardano portable.

Gli esempi API seguenti richiedono la funzionalità `automation_api`. L’esempio abilita quattro percorsi e richiede anche la funzionalità `extra_gateways` appropriata; non rappresenta la selezione predefinita dell’interfaccia. Specificare manualmente i percorsi secondo i gateway disponibili e il limite della licenza: fino a 2 in Free o fino a 8 con la funzionalità Pro corrispondente.

Controllo: `GET /api/v1/health`, risposta normale `{"ok":true,"version":"4.3.9"}`. Prima configurare la rete nella finestra o inviare a `POST /api/v1/network`:

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

<a name="8-cronologia-dati-e-aggiornamento"></a>

## Cronologia, dati e aggiornamento

| Sistema | Dati | Applicazione/runtime |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

`logs/downloads-history.jsonl` è JSON per riga UTF-8; `logs/downloads-history.log` è testo leggibile datato. Vengono aggiunti eventi tra i riavvii senza riscrivere il passato al cambio lingua o alla rimozione del lavoro. Il testo mantiene la lingua di scrittura; la GUI rende gli eventi nella lingua attuale. La finestra legge al massimo la coda di 1.000 eventi/2 MiB, lasciando l’archivio completo sul disco. Cresce con l’uso: archiviarlo separatamente se necessario. Contiene nomi e percorsi assoluti, senza URL sorgenti, password o token; controllare i percorsi prima di condividerlo.

Gli eventi `lifecycle_history` hanno `event_id`, `job_id`, `kind`, `time`, `filename`, `path`; per torrent può comparire `paths` e per pausa `reason` (`pause`, `shutdown`, `maintenance`). Tipi: `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Date ISO 8601 UTC, ad esempio `2026-10-18T12:34:56.123Z`; il testo mostra data/ora locale completa e offset UTC. Gli opzionali `first_started_at`, `last_resumed_at`, `completed_at` riassumono inizio, ultima ripresa e completamento verificato/pubblicato realmente osservati. Un lavoro in coda non è ancora iniziato; non si inventano istanti non osservati. `recovered` indica la verifica attuale di un file pubblicato presente e non prova quando il download sia stato completato.

Prima dell’aggiornamento uscire dal tray, installare il pacchetto completo e conservare dati/parti. In Windows rimuovere da Impostazioni → App → App installate. Su Mac cancellare `.app` non rimuove automaticamente cronologia o componente protetto. Non eliminare la cartella dei download per disinstallare. Se si sposta la cartella Linux, ripetere il deploy.

<a name="10-licenza"></a>

## Licenza

L’applicazione originale può essere usata gratuitamente da privati e organizzazioni, anche in attività commerciali. Sono vietati vendita, addebito della licenza e distribuzione a pagamento senza permesso separato. La licenza personalizzata non concede diritti generali di modifica/ridistribuzione e non è una licenza open source approvata OSI. Fa fede l’inglese [LICENSE.txt](../LICENSE.txt). FlatLaf (Apache-2.0) e gli altri componenti mantengono le proprie licenze. Proprietario: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

<a name="nome-e-profili-precedenti"></a>

## Cartelle dei profili e componente locale

Le cartelle dei dati e i percorsi dell’applicazione sono elencati nella tabella sopra. Un profilo può trovarsi anche in `%LOCALAPPDATA%\MultiRoute Resume\data` o `~/Library/Application Support/MultiRoute Resume/data`. Se la cartella dati predefinita non esiste e l’applicazione identifica senza ambiguità un profilo presente in una di queste posizioni, lo usa senza spostare o cancellare file. Non rinominare queste cartelle mentre l’applicazione è in esecuzione. Il componente protetto usa la cartella `MultiRouteResume`; l’identità d’installazione Windows, l’identità API interna e la porta sono associate al componente installato. Aprire l’applicazione dal collegamento o dall’app MultiRoute Downloader installati.

<a name="siti-salvati-caricamenti-e-anteprima-archivi-40"></a>

## Siti salvati, caricamenti e anteprima archivi

Aprire **Profili dei siti…**, creare/modificare il profilo e sceglierlo in **Download** o **Caricamenti**. Gateway e IP virtuale sono in **Impostazioni di rete**. In **Caricamenti** inserire l’**URL della cartella remota** e usare **Scegli file e carica…** per scegliere uno o più file locali con il selettore di sistema. L’interfaccia aggiunge i nomi; API `target_url` indica invece un file remoto completo. Incollare l’URL dell’archivio nel campo principale e usare **Contenuto dell’archivio…** per l’anteprima, senza avviare il download.

Un profilo salva nome, URL di base, cartella locale, cartella remota e gateway. Supporta accesso anonimo, HTTP Basic/Bearer/Cookie/intestazioni proprie, FTP e SFTP con impronta SHA-256 del server verificata prima. Più account dello stesso server restano separati. Cartelle/gateway espliciti prevalgono; gli URL relativi usano la base. I segreti sono cifrati in `site-profiles/vault.bin` con la chiave locale `site-profiles/key.bin` e permessi del proprietario. Chi controlla account e chiave può leggerli. L’API pubblica non restituisce segreti. Campi vuoti conservano le credenziali solo a origine e tipo invariati; `replace_credentials:true` le sostituisce. Una redirezione HTTP verso un’altra origine non riceve le credenziali. Eliminare il profilo conserva attività e file locali/remoti; l’attività conserva il legame al profilo originale e non cambia account; se è stato eliminato, ricreare l’attività con un profilo valido quando necessario.

Caricamento ed esplorazione remota supportano **FTP, FTPS esplicito e SFTP** tramite gateway scelti. Selezionare un file locale normale e un URL completo del file remoto (`target_url`), non una sola cartella. Un file temporaneo permette di riprendere dopo il confronto del prefisso. Il successo richiede rilettura remota completa e SHA-256: servono permessi di **lettura, scrittura e rinomina**, con traffico aggiuntivo. Una somma diversa o lettura negata è un errore. Ogni file usa un gateway per volta; più file vengono distribuiti e caricati in parallelo. Dopo il limite di inattività può subentrare un altro gateway; un singolo invio non viene suddiviso in parallelo. Pausa/ripresa e avanzamento persistono. Rimuovere l’attività conserva il file locale e i file remoti finali/temporanei.

La sovrascrittura è disattivata. FTP/SFTP non garantiscono ovunque una rinomina atomica soltanto se il destinatario non esiste; un altro client può modificarlo durante la pubblicazione. Coordinare i percorsi condivisi. Elencare cartelle non invia né elimina file.

L’anteprima di **ZIP, TAR, 7Z, RAR4 e RAR5** mostra nomi, percorsi, dimensioni, cartelle e cifratura senza estrarre o eseguire. Richiede un gateway, intervalli di byte ed ETag o Last-Modified stabile. Limiti: 16 MiB di metadati, 10.000 elementi, 60 secondi; decodifica intestazioni 7z fino a 64 MiB di memoria. L’elenco può essere parziale e non verifica l’integrità dei contenuti. Specificare la codifica originale se i nomi sono errati. Le intestazioni 7z cifrate supportate accettano una password non salvata. Le intestazioni RAR cifrate sono segnalate non supportate/password necessaria; archivi multivolume possono essere incompleti.

L’API accetta `site_profile_id` per scaricare, caricare, elencare e visualizzare. `GET /api/v1/site-profiles` restituisce `{profiles:[...]}`; `POST`/`PUT` salva un oggetto piatto e restituisce metadati pubblici; `DELETE /api/v1/site-profiles/{id}` elimina solo il profilo. `POST /api/v1/uploads` riceve `local_path`, `target_url`, `routes`, `site_profile_id` facoltativo e `overwrite:false`, con HTTP 201. GET restituisce `{uploads:[...]}`; sono disponibili GET/DELETE singoli e POST `/{id}/pause` / `/{id}/resume`. `POST /api/v1/remote/list` usa `url` e `route`/`routes`; `POST /api/v1/archives/preview` accetta anche `password` e `charset`. Vedere [openapi.yaml](../openapi.yaml). I manifesti UTF-8 in `data/uploads/` usano ID del profilo, non password. L’anteprima non crea un download.

## Pianificazione e priorità dei download

Nei controlli coda/orario impostare `max_active_jobs` (1–32), limiti globali/per gateway e finestra giornaliera (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm). Si usa il fuso locale, restituito come `schedule_timezone`; può attraversare mezzanotte e orari uguali significano tutto il giorno. Fuori finestra i download tornano in coda; le pause manuali restano. `priority` da -100 a 100 ordina gli elementi in attesa, valore alto prima poi creazione, senza interrompere quelli attivi. `start_after_ms` è l’istante minimo Unix in millisecondi, 0 rimuove il ritardo. Persiste ai riavvii e riguarda i download, non la coda upload indipendente. Applicazione e computer devono restare attivi; non viene programmato il risveglio OS. API: `GET`/`PUT`/`POST /api/v1/queue`, `PUT`/`POST /api/v1/jobs/{id}/options`.

## Integrazione Chrome ed Edge

Aprire l’integrazione **Chrome / Edge** nell’app, abilitare l’invio dei link e aprire la cartella dell’estensione inclusa nell’installer. Aprire `chrome://extensions` o `edge://extensions`, attivare la modalità sviluppatore e scegliere **Carica estensione non pacchettizzata** con quella cartella. Non spostare la cartella scelta.

MultiRoute deve funzionare, anche nella tray. Inviare l’URL dal menu del link o dal popup. Usa cartella, gateway e profilo attuali; restano le selezioni repository/torrent. La porta predefinita è 8765, da adattare se l’API cambia. Viene inviato solo l’URL, senza cookie/password e senza intercettare altri download. Configurare l’accesso alla fonte nell’app. Concedere l’eventuale permesso di connessione locale soltanto a questa estensione scelta.

Il ponte è disattivato e accetta solo `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` su `/api/v1/browser/status` e `/api/v1/browser/download`; le altre regole API non cambiano. HTTP 202 indica consegna all’interfaccia, non file completato. Un server headless da solo non apre la selezione desktop. Rimuovere l’estensione o disabilitare l’integrazione per interrompere l’invio.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).

## Schermate

Di seguito sono mostrate le finestre attuali dell’applicazione. Apri l’immagine a dimensione intera per leggere i singoli controlli. I valori di trasferimento di esempio sono illustrativi.

![MultiRoute Downloader — coda di download e velocità dei gateway](images/v4.3.7/downloads-en-ae5b55131583.png)

*Interfaccia Windows attuale, acquisita con un profilo dimostrativo isolato. File di esempio, avanzamento, velocità e viste abilitate con Pro illustrano i controlli e non sono misurazioni delle prestazioni.*

<table>
<tr>
<td width="50%" valign="top"><strong>Impostazioni dell’applicazione</strong><br /><a href="images/v4.3.7/settings-8abcb12a782d.png"><img src="images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Impostazioni dell’applicazione" /></a></td>
<td width="50%" valign="top"><strong>Aspetto, corriere e suoni</strong><br /><a href="images/v4.3.7/appearance-cebf121d5de7.png"><img src="images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Aspetto, corriere e suoni" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Indirizzo dell’applicazione e DNS protetto</strong><br /><a href="images/v4.3.7/network-dns-e29dfdba1007.png"><img src="images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Indirizzo dell’applicazione e DNS protetto" /></a></td>
<td width="50%" valign="top"><strong>Selezione dei gateway e test di velocità</strong><br /><a href="images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Selezione dei gateway e test di velocità" /></a></td>
</tr>
</table>
