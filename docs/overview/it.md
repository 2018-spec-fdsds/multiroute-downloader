# MultiRoute Downloader
<!-- product-languages:start -->
<p align="center" dir="ltr">
<a href="../../README.md">English</a> · <a href="../../README.ru.md">Русский</a> · <a href="de.md">Deutsch</a> · <a href="es.md">Español</a> · <a href="fr.md">Français</a> · <a href="zh.md">中文</a><br />
<a href="ko.md">한국어</a> · <a href="ar.md">العربية</a> · <strong>Italiano</strong> · <a href="ja.md">日本語</a> · <a href="hi.md">हिन्दी</a> · <a href="vi.md">Tiếng Việt</a>
</p>
<!-- product-languages:end -->

<p align="center"><img src="../images/overview/it/hero.svg" width="1320" alt="MultiRoute Downloader — Più connessioni. Un solo download." /></p>

<p align="center"><strong>Scarica file di grandi dimensioni con le connessioni che hai già.</strong><br />Combina gateway indipendenti a banda larga e mobili per scaricare file di grandi dimensioni, conserva l’avanzamento dei trasferimenti riprendibili e verifica il file completato.</p>

<p align="center">
  <a href="https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9"><strong>Scarica 4.3.9</strong></a> ·
  <a href="../../downloads/README.md">Download attuali</a> ·
  <a href="../it.md">Guida utente</a> ·
  <a href="../../API.ru.txt">Riferimento API</a> ·
  <a href="../../README.ru.md">Русский</a>
</p>

## Perché esiste MultiRoute

L’idea è nata da un problema comune nelle reti domestiche: erano disponibili una connessione a banda larga cablata, un router mobile e una connessione Internet di riserva, ma un download di grandi dimensioni continuava a usare solo il gateway predefinito del computer. Quando quella connessione rallentava o si interrompeva, le altre non potevano essere d’aiuto.

MultiRoute riunisce queste connessioni indipendenti in un unico spazio di lavoro per i download. I gateway disponibili prendono segmenti di file da una coda condivisa; quelli più veloci svolgono più lavoro e i segmenti incompleti di una connessione bloccata possono essere riassegnati. L’applicazione conserva i progressi dove è supportata la ripresa, assembla il risultato e lo confronta con un checksum di riferimento disponibile.

- **Più banda per un file.** Connessioni Internet indipendenti scaricano parti diverse in parallelo. La loro capacità combinata può abbreviare un download di grandi dimensioni se la sorgente e la rete locale riescono a tenere il passo.
- **Progressi nonostante le interruzioni.** Le parti completate vengono salvate; i gateway funzionanti continuano a scaricare e i segmenti bloccati possono tornare nell’insieme delle attività condivise.
- **Controllo visibile.** Segui la velocità totale e quella di ogni gateway, cambia i gateway selezionati durante un trasferimento e osserva il contributo di ogni connessione.
- **Flussi di lavoro ripetibili.** Riutilizza i profili dei siti, metti in coda elenchi di link o affida i trasferimenti all’API locale da script e strumenti di sviluppo.

I vantaggi effettivi dipendono dall’indipendenza delle connessioni, dai limiti della sorgente e dalla rete locale. Router che condividono la stessa connessione a monte non ne moltiplicano la banda.

![Gateway condivisi, progressi affidabili, verifica, DNS protetto e controlli dei trasferimenti](../images/overview/it/features.svg)

### Utile anche con una sola connessione

Usa la ripresa dove supportata, il confronto con checksum pubblicati, il DNS protetto, le code in blocco, le pianificazioni, i profili dei siti, i caricamenti e l’API locale. Inizia con un gateway e aggiungine altri quando sono disponibili.

## Download e installazione

**Versione attuale: 4.3.9.** Gli installer nativi includono un ambiente Java 21 dedicato e il componente di rete della piattaforma. Apri l’applicazione dalla sua normale icona.

| Il tuo computer | Installer in un solo file |
| --- | --- |
| **Windows 11 · x64** | [Windows Setup EXE](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe) |
| **Mac · Apple Silicon · macOS 26+** | [Apple Silicon PKG](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg) |
| **Mac · Intel · macOS 15+** | [Intel PKG](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg) |

[Integrazione con Chrome / Edge](../it.md#chrome-and-edge-integration) · [Checksum e rapporto](../../downloads/v4.3.9/README.md) · [Dettagli dei download](../../downloads/README.md)

L’installer Windows è in inglese e crea un collegamento sul desktop. Npcap viene verificato separatamente e, se necessario, viene proposto il suo installer ufficiale; macOS usa libpcap di sistema. Consulta la guida nella tua lingua per l’installazione e le conferme di sicurezza. Gli installer nativi pubblici coprono Windows e macOS; qui non è incluso un installer Linux 4.x.

**Free / Pro nella versione 4.3.9:** Free permette di usare fino a **2 gateway**, **3 download simultanei** e di creare **3 profili di siti**, con aspetto Graphite/Sistema. Le corrispondenti funzioni Pro consentono fino a **8 gateway**, **32 download simultanei**, profili aggiuntivi, priorità, cronologia e rapporti dei gateway, operazioni in blocco, l’API esterna e stili aggiuntivi. Free non limita deliberatamente la velocità di download. [Dettagli completi delle funzioni](../licensing.en.md).


## Novità di 4.3.9

- **Risposte dei gateway Mac con indirizzo manuale.** La cattura POSIX viene corretta affinché indirizzo manuale dell’app e DHCP ricevano entrambi le risposte al MAC proprio dell’app, come già su Windows. Un ping di sistema riuscito verifica il collegamento del computer, non la ricezione sull’indirizzo virtuale dell’app.

- La diagnostica DHCP conserva il testo specifico nelle dodici lingue invece di un errore generico. L’autorizzazione esistente resta invariata: i normali controlli riutilizzano il componente preparato. Non cambiano endpoint API, controlli, route del sistema o MAC fisico. Il primo accesso privilegiato a Ethernet BPF può ancora richiedere la normale conferma amministratore.

Si corregge una differenza nel modo di cattura, non ogni causa di gateway irraggiungibile. Restano attivi i controlli di indirizzo libero, sottorete e lease/conflitti DHCP. I trasferimenti Mac con indirizzo manuale richiedono ancora conferma su hardware; Ethernet cablata resta necessaria.

### Controlli dei trasferimenti

- **Avvio Mac e configurazione dei gateway più affidabili.** L’avvio nativo su Mac riconosce il vecchio percorso predefinito relativo, anche senza barra iniziale, e usa un profilo esistente verificato o una cartella predefinita vuota dell’utente. I percorsi personalizzati ambigui richiedono una scelta esplicita.

- Dopo un errore o l’annullamento della preparazione, gli altri gateway e i controlli in background smettono di ripetere le richieste di autorizzazione fino a un nuovo tentativo esplicito di configurazione della rete. Il primo avvio privilegiato può ancora richiedere la normale conferma amministratore di macOS per aprire il dispositivo BPF Ethernet.

- Il componente di rete Mac 4.3.2 gestisce un MAC dell’adattatore nascosto da macOS mantenendo i controlli di protezione della scheda fisica. L’app conserva il proprio MAC senza cambiare le route del sistema o il MAC fisico. Icona e nome Mac adottano la stessa identità MultiRoute di Windows. Esci dall’app prima dell’aggiornamento PKG. Il nome del richiedente nel dialogo reale e i trasferimenti LAN/DHCP richiedono ancora verifica su hardware; questo trasporto richiede tuttora Ethernet cablata.



- **Dati ancora da scaricare.** La tabella include la colonna **Dati rimanenti**. Mostra i byte ancora da scaricare separatamente dal tempo stimato e si ordina numericamente. Tiene conto dell’avanzamento salvato e in corso, non scende sotto zero e mostra un trattino quando la dimensione totale è sconosciuta.


- **Dai una preferenza ai gateway.** Le priorità 0–1000 compaiono vicino agli indicatori: valori maggiori favoriscono i file piccoli e ricevono più segmenti paralleli dei file grandi. Gli altri gateway funzionanti contribuiscono comunque. Serve il diritto Pro separato `gateway_priorities`.

- **Comprendi le interruzioni.** Apri il registro individuale dal menu contestuale, anche dopo il completamento, oppure la guida ricercabile di 25 argomenti nelle impostazioni. Il disco pieno viene segnalato chiaramente e la destinazione mostra spazio libero in GB.

- **Gestisci la coda.** Imposta download simultanei entro la licenza e tentativi automatici. Copia i nomi dei file selezionati, sospendi/riprendi le attività compatibili e apri la cartella con un doppio clic. Il prefisso salvato resta nel progresso durante l’attesa di dati dopo la ripresa.


## I tuoi trasferimenti, ben visibili

Lo spazio di lavoro di 4.3.9 mette i tuoi file al centro: colonne separate per nome e dimensione, avanzamento individuale, velocità totale e per gateway, un grafico della velocità e dettagli dei file. Graphite e System sono inclusi in Free; stili aggiuntivi come Rose richiedono la corrispondente funzione Pro.

![MultiRoute Downloader 4.3.7 — coda di download e velocità dei gateway](../images/v4.3.7/downloads-en-ae5b55131583.png)

*Interfaccia Windows attuale, acquisita con un profilo dimostrativo isolato. File di esempio, avanzamento, velocità e viste abilitate con Pro illustrano i controlli e non sono misurazioni delle prestazioni.*

<details>
<summary><strong>Impostazioni di rete e cronologia della velocità</strong></summary>

![Indirizzo dell’applicazione e DNS protetto](../images/v4.3.7/network-dns-e29dfdba1007.png)

![Selezione dei gateway e test di velocità](../images/v4.3.7/network-gateways-aee0a7897f48.png)

![MultiRoute Downloader 4.3.7 — cronologia della velocità di download](../images/v4.3.7/speed-graph-a85815544945.png)

</details>


## Scegli il tuo aspetto

Scegli tra nove aspetti: Graphite, Nordic, Streams Studio, Neon, Community, Chat, Studio, Rose e System. Graphite è quello predefinito. Graphite e System sono inclusi in Free; le altre scelte richiedono la corrispondente funzione Pro per l’aspetto. Apri Impostazioni → Aspetto per cambiare lo stile e i controlli arrotondati. I download continuano mentre scegli.

<table>
<tr>
<td width="50%" valign="top"><strong>Graphite</strong><br /><a href="../images/v4.3.7/downloads-en-ae5b55131583.png"><img src="../images/v4.3.7/downloads-en-ae5b55131583.png" width="420" alt="Graphite" /></a></td>
<td width="50%" valign="top"><strong>Nordic</strong><br /><a href="../images/v4.3.7/skin-nordic-a4e8d79666e0.png"><img src="../images/v4.3.7/skin-nordic-a4e8d79666e0.png" width="420" alt="Nordic" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Streams Studio</strong><br /><a href="../images/v4.3.7/skin-streams-b2f3be388bda.png"><img src="../images/v4.3.7/skin-streams-b2f3be388bda.png" width="420" alt="Streams Studio" /></a></td>
<td width="50%" valign="top"><strong>Neon</strong><br /><a href="../images/v4.3.7/skin-neon-6b0c1d2da5f8.png"><img src="../images/v4.3.7/skin-neon-6b0c1d2da5f8.png" width="420" alt="Neon" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Community</strong><br /><a href="../images/v4.3.7/skin-community-9763964c8f12.png"><img src="../images/v4.3.7/skin-community-9763964c8f12.png" width="420" alt="Community" /></a></td>
<td width="50%" valign="top"><strong>Chat</strong><br /><a href="../images/v4.3.7/skin-chat-221a28cc76f4.png"><img src="../images/v4.3.7/skin-chat-221a28cc76f4.png" width="420" alt="Chat" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Studio</strong><br /><a href="../images/v4.3.7/skin-studio-094e14ba38ea.png"><img src="../images/v4.3.7/skin-studio-094e14ba38ea.png" width="420" alt="Studio" /></a></td>
<td width="50%" valign="top"><strong>Rose</strong><br /><a href="../images/v4.3.7/skin-rose-02646e7f35bf.png"><img src="../images/v4.3.7/skin-rose-02646e7f35bf.png" width="420" alt="Rose" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>System</strong><br /><a href="../images/v4.3.7/skin-system-c148099984c6.png"><img src="../images/v4.3.7/skin-system-c148099984c6.png" width="420" alt="System" /></a></td>
<td width="50%" valign="top"></td>
</tr>
</table>

## Un corriere che segue i tuoi download

Il corriere desktop facoltativo trasforma l’avanzamento dei download in un carico visibile. Scegli l’uomo o la donna nelle impostazioni dell’aspetto e regola le dimensioni dal 40% al 100%. Il sacco mostra il totale noto dei dati rimanenti dei download attivi e si riduce man mano che arrivano i byte. Il movimento segue la loro velocità di download complessiva; a velocità zero il corriere resta fermo. I carichi piccoli sono portati in spalla, quelli più grandi vengono trascinati e quelli molto grandi mostrano uno sforzo maggiore.

Usa il pulsante Avatar per mostrare o nascondere il corriere. Trascinalo per spostarlo, fai doppio clic con il pulsante sinistro del mouse per mostrare l’applicazione o fai clic con il destro per le sue opzioni. È un compagno visivo: i file rimangono nelle cartelle scelte.

Quando un download termina, il corriere posa il carico consegnato e scompare gradualmente. Se il lavoro continua, ritorna con i dati rimanenti.

### Carico residuo piccolo · 768 MB

<table>
<tr>
<td width="50%" valign="top"><strong>Corriere uomo</strong><br /><a href="../images/v4.3.7/avatar-man-shoulder-eeeb879d0240.png"><img src="../images/v4.3.7/avatar-man-shoulder-eeeb879d0240.png" width="420" alt="Corriere uomo" /></a></td>
<td width="50%" valign="top"><strong>Corriere donna</strong><br /><a href="../images/v4.3.7/avatar-woman-shoulder-5682615d5576.png"><img src="../images/v4.3.7/avatar-woman-shoulder-5682615d5576.png" width="420" alt="Corriere donna" /></a></td>
</tr>
</table>

### Carico residuo medio · 7 GB

<table>
<tr>
<td width="50%" valign="top"><strong>Corriere uomo</strong><br /><a href="../images/v4.3.7/avatar-man-drag-a3dc0a3ed22c.png"><img src="../images/v4.3.7/avatar-man-drag-a3dc0a3ed22c.png" width="420" alt="Corriere uomo" /></a></td>
<td width="50%" valign="top"><strong>Corriere donna</strong><br /><a href="../images/v4.3.7/avatar-woman-drag-48d8ee0fa4d1.png"><img src="../images/v4.3.7/avatar-woman-drag-48d8ee0fa4d1.png" width="420" alt="Corriere donna" /></a></td>
</tr>
</table>

### Carico residuo grande · 18 GB

<table>
<tr>
<td width="50%" valign="top"><strong>Corriere uomo</strong><br /><a href="../images/v4.3.7/avatar-man-heavy-649f17608748.png"><img src="../images/v4.3.7/avatar-man-heavy-649f17608748.png" width="420" alt="Corriere uomo" /></a></td>
<td width="50%" valign="top"><strong>Corriere donna</strong><br /><a href="../images/v4.3.7/avatar-woman-heavy-d445fdb37867.png"><img src="../images/v4.3.7/avatar-woman-heavy-d445fdb37867.png" width="420" alt="Corriere donna" /></a></td>
</tr>
</table>

## Le impostazioni a colpo d’occhio

Imposta la cartella di download, i profili dei siti e la lingua, poi regola l’aspetto, le dimensioni del corriere e i suoni di completamento. Le impostazioni di rete riuniscono indirizzo dell’applicazione, MAC, gateway e DNS. I suoni facoltativi permettono di distinguere il completamento di un file da quello dell’intera coda, con melodie integrate o brani MP3/WAV personali di massimo 10 secondi.

<table>
<tr>
<td width="50%" valign="top"><strong>Impostazioni dell’applicazione</strong><br /><a href="../images/v4.3.7/settings-8abcb12a782d.png"><img src="../images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Impostazioni dell’applicazione" /></a></td>
<td width="50%" valign="top"><strong>Aspetto, corriere e suoni</strong><br /><a href="../images/v4.3.7/appearance-cebf121d5de7.png"><img src="../images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Aspetto, corriere e suoni" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Indirizzo dell’applicazione e DNS protetto</strong><br /><a href="../images/v4.3.7/network-dns-e29dfdba1007.png"><img src="../images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Indirizzo dell’applicazione e DNS protetto" /></a></td>
<td width="50%" valign="top"><strong>Selezione dei gateway e test di velocità</strong><br /><a href="../images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="../images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Selezione dei gateway e test di velocità" /></a></td>
</tr>
</table>

<details>
<summary><strong>Spazi di lavoro per download e caricamenti</strong></summary>

<table>
<tr>
<td width="50%" valign="top"><strong>Crea un download</strong><br /><a href="../images/v4.3.7/new-download-dcb720c7769d.png"><img src="../images/v4.3.7/new-download-dcb720c7769d.png" width="420" alt="Crea un download" /></a></td>
<td width="50%" valign="top"><strong>Spazio di lavoro per i caricamenti</strong><br /><a href="../images/v4.3.7/uploads-31c63833de2e.png"><img src="../images/v4.3.7/uploads-31c63833de2e.png" width="420" alt="Spazio di lavoro per i caricamenti" /></a></td>
</tr>
</table>

</details>

## Dalle esigenze quotidiane al controllo pratico

| Vuoi… | MultiRoute offre |
| --- | --- |
| **Usare le connessioni disponibili** | Una coda condivisa di segmenti HTTP, da 1 a 8 profili gateway modificabili e selezione in tempo reale di uno, più o tutti i gateway. |
| **Conservare e verificare file di grandi dimensioni** | Ripresa dove supportata, parti salvate, assemblaggio automatico e controlli con riferimenti dell’editore tramite MD5, SHA-256 o SHA-512. |
| **Proteggere le richieste DNS** | DoH/DoT cifrato, fornitori predefiniti o personalizzati e DNSSEC locale, senza modificare impostazioni DNS o route del sistema operativo. |
| **Organizzare più download** | Elenchi di link, priorità, fasce orarie giornaliere, avvii ritardati, limiti di velocità e attività, statistiche per gateway, grafici e cronologia con date. |
| **Riutilizzare i siti abituali** | Profili salvati, esplorazione di cartelle remote, caricamenti FTP/FTPS/SFTP e anteprime degli archivi. |
| **Adattare l’applicazione al tuo modo di lavorare** | Invio di link dal browser, API locale, dodici lingue, temi chiaro/scuro/di sistema, incolla da tastiera, finestre di sistema per la scelta delle cartelle, indicatore di velocità nell’area di notifica e avvio facoltativo all’accesso. |

| Trasferimento | Opzioni supportate |
| --- | --- |
| **Download** | HTTP/HTTPS, FTP, FTPS esplicito, SFTP, WebDAV, Metalink, BitTorrent e link magnet; link Hugging Face e selezione dei file nei repository. |
| **Caricamento** | FTP, FTPS esplicito e SFTP, con profili dei siti e verifica SHA-256 tramite rilettura. |
| **Anteprima degli archivi** | Elenco dei contenuti di ZIP, TAR, 7Z e RAR prima del download completo, quando sono disponibili intervalli di byte e intestazioni supportate. |

### Sapere che cosa è stato verificato

I controlli dei file confrontano i dati assemblati con un riferimento attendibile, quando disponibile; un hash locale da solo non prova l’identità dell’editore. DoH/DoT cifra il trasporto DNS; DNSSEC convalida separatamente i dati DNS firmati. Questi controlli non dimostrano che i contenuti scaricati siano innocui.

## Un’API per le tue applicazioni e lo sviluppo assistito dall’IA

Script, applicazioni di terze parti e agenti di programmazione basati sull’IA possono chiedere a MultiRoute di recuperare pesi dei modelli, dataset, librerie, risorse e artefatti di progetto necessari allo sviluppo. Il chiamante riceve lo stato di avanzamento, il completamento e i risultati dei checksum, e può controllare la coda mentre MultiRoute gestisce gateway, nuovi tentativi e ripresa dove supportata.

L’API viene eseguita localmente su **`http://127.0.0.1:8765/api/v1`**. Per esempio, invia questo JSON a **`POST /api/v1/jobs`** con `Content-Type: application/json`:

```json
{
  "url": "https://example.org/model.bin",
  "directory": "C:/Users/YOUR_USER/Downloads",
  "routes": [
    {"type": "gateway", "gateway": "192.168.1.1", "enabled": true}
  ]
}
```

La gestione generale tramite l’API esterna richiede la corrispondente funzione Pro; la creazione in blocco e i limiti dei gateway vengono verificati separatamente. Sostituisci URL, cartella e gateway con i tuoi valori e configura prima la rete. Specifica esplicitamente le route: le caselle dell’interfaccia non vengono copiate nelle richieste API. `201` indica che l’attività è stata creata; conserva `id` e consulta `GET /jobs/{id}` per l’avanzamento, lo stato `complete`/`failed` e i dettagli dei checksum. L’esempio è illustrativo e non invia alcun download.

[Riferimento API](../../API.ru.txt) · [Schema OpenAPI](../../openapi.yaml) · [Guida all’integrazione](../it.md#multiroute-api)

## Inizia in quattro passaggi

1. **Installa** il pacchetto per la tua piattaforma e segui la guida.
2. **Scegli le impostazioni di rete:** Ethernet cablata, gateway nella stessa LAN IPv4 e un indirizzo virtuale inutilizzato per l’applicazione. Le route del sistema operativo usate dalle altre applicazioni rimangono invariate.
3. **Seleziona e prova i gateway.** I valori predefiniti sono `192.168.1.1`–`192.168.1.4`, con indirizzo virtuale `192.168.1.70`; adattali alla tua rete.
4. **Incolla un link o un elenco, scegli una cartella e avvia.** Segui il contributo di ogni gateway e la verifica finale.

<details>
<summary><strong>Requisiti e dettagli dei trasferimenti</strong></summary>

- Il trasporto con più gateway supporta IPv4 pubblico su Ethernet cablata; gli adattatori solo Wi-Fi/VPN, le sorgenti solo IPv6 e i peer torrent della LAN locale non rientrano in questa modalità.
- La ripresa per segmenti richiede il supporto degli intervalli e un’identità stabile della sorgente. I segmenti HTTP sono di 20 MiB per impostazione predefinita; il lavoro bloccato può essere riassegnato dopo 60 secondi senza dati, una volta chiusa la relativa risorsa di scrittura. I trasferimenti HTTP che non supportano la ripresa ricominciano dopo un’interruzione. I frammenti torrent usano gli hash dei metadati.
- Ogni file caricato usa un gateway; più file possono usare gateway diversi. La verifica tramite rilettura consuma banda e richiede permessi remoti di lettura, scrittura e ridenominazione. L’anteprima degli archivi ha limiti relativi a metadati e intestazioni.
- Le pianificazioni richiedono l’applicazione in esecuzione e il computer attivo; non lo riattivano e non pianificano la coda separata dei caricamenti.
- Il DNS di sistema segue i criteri del sistema operativo/VPN. Le ancore di fiducia radice DNSSEC vengono aggiornate con le versioni. Il DNS cifrato non cifra di per sé i contenuti dei file HTTP/FTP.
- Gli installer non sono firmati; i pacchetti macOS non sono firmati con Developer ID né autenticati tramite notarizzazione. Segui le normali conferme nelle guide senza disattivare le protezioni del sistema. Ogni versione include il rapporto di verifica e i limiti ancora presenti.

</details>

## Guide in dodici lingue

[English](../en.md) · [Русский](../ru.md) · [Deutsch](../de.md) · [Español](../es.md) · [Français](../fr.md) · [中文](../zh.md)<br />
[한국어](../ko.md) · [العربية](../ar.md) · [Italiano](../it.md) · [日本語](../ja.md) · [हिन्दी](../hi.md) · [Tiếng Việt](../vi.md)


<a name="application-virtual-ip"></a>

### Indirizzo dell’applicazione e pacchetti attuali

L’applicazione usa un proprio indirizzo IPv4 locale e un proprio indirizzo MAC applicativo, così i gateway selezionati possono funzionare indipendentemente dal gateway predefinito del sistema operativo, senza modificare le route del sistema né il MAC dell’adattatore fisico. Usa un indirizzo libero nella stessa sottorete effettivamente in uso. Mantieni un indirizzo manuale al di fuori dell’assegnazione DHCP automatica, oppure riservalo/escludilo sui server DHCP della LAN; quindi usa **Verifica e applica** nelle impostazioni di rete. [Scelta e verifica dell’indirizzo](../it.md#application-virtual-ip).

Gli attuali pacchetti Windows 4.3.9 hanno superato una verifica locale di aggiornamento e avvio; i pacchetti macOS hanno superato i controlli dei componenti nativi e dell’ambiente Java sui runner CI di destinazione. I trasferimenti tramite gateway su LAN/DHCP con Mac fisici richiedono ancora una convalida sull’hardware. [Rapporto di verifica](../../NATIVE-INSTALLER-TESTS.json).

## Autore e licenza

Creato da **[2018-spec-fdsds](https://github.com/2018-spec-fdsds)**. L’uso gratuito comprende l’uso personale e quello di organizzazioni e aziende. La vendita dell’applicazione originale, l’addebito di un costo per la licenza o la ridistribuzione a pagamento richiedono un’autorizzazione scritta separata. Non vengono concessi diritti generali di modifica o ridistribuzione; rimangono applicabili le condizioni delle terze parti.

[Licenza](../../LICENSE.txt) · [Sintesi della licenza](../../LICENSING.md) · [Avvisi sulle terze parti](../../THIRD-PARTY-NOTICES.txt)

Questa area di distribuzione contiene gli installer attuali della versione 4.3.9 e la documentazione utente.
