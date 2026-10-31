# macOS: installazione, avvio e autorizzazioni

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · **Italiano** · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## Installare e aprire

Prima di aggiornare, attendi la fine dei trasferimenti e scegli **Quit/Exit** dal menu MultiRoute nella barra dei menu. Il pulsante rosso può soltanto nascondere la finestra. Scarica il PKG Apple Silicon o Intel dai [download](../../downloads/README.md), verifica SHA-256 e aprilo. Conferma l’installazione nel dialogo amministratore di macOS. Avvia `/Applications/MultiRoute Downloader.app` con Finder o Spotlight: non servono Java separata o Terminale.

## Confermare l’apertura

Se macOS non verifica lo sviluppatore, prova prima ad aprire PKG o app. Solo per un file controllato e attendibile: **Impostazioni di Sistema → Privacy e sicurezza → Open Anyway → Open**; le diciture possono variare. Con avvisi di malware o file danneggiato fermati e verifica il file. [Istruzioni Apple](https://support.apple.com/en-us/102445).

## Rete e password

Il primo avvio dell’app o l’applicazione di IP virtuale, DHCP o MAC può richiedere l’autorizzazione amministratore per aprire Ethernet BPF. Una riconfigurazione o un nuovo processo può richiederla ancora. Verifiche e test di velocità normali riutilizzano il componente pronto, senza password per ogni file o gateway. Route del sistema e MAC fisico non cambiano.

Se compare la richiesta **Rete locale**, consenti la voce MultiRoute riconosciuta in Privacy e sicurezza → Rete locale. Non tutte le installazioni mostrano la richiesta. [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

La password amministratore Mac va solo nel dialogo di sistema, non nei campi licenza/Hugging Face. Il richiedente può apparire come `DesktopLauncher`/Java: conferma solo la tua operazione MultiRoute attesa. Dopo annullamento riprova esplicitamente e consulta il registro. Non disattivare globalmente Gatekeeper/SIP.

[MultiRoute Downloader — Italiano](../it.md) · [macOS — English / 12 languages](README.md)
