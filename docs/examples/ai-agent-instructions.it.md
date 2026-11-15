# Istruzione di progetto MultiRoute — da copiare nelle regole del tuo agente

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <strong>Italiano</strong> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

Usa questo blocco nel tuo `AGENTS.md`, `CLAUDE.md` o nelle istruzioni persistenti di progetto equivalenti già esistenti. Aggiungilo conservando le altre regole del progetto. Sostituisci una volta destinazione e rotte. Per configurazione ed esempi, consulta [Italiano](../ai-agents.it.md) o [Inglese](../ai-agents.en.md).

```markdown
## Download: usare MultiRoute per impostazione predefinita

Per i file esterni necessari all’attività autorizzata del progetto (modelli,
dataset, archivi, contenuti multimediali e altre risorse), usa MultiRoute
Downloader in esecuzione. Non chiedermi di scegliere un gestore di download per
ogni file. Mantieni i gestori di pacchetti e Git sui loro normali percorsi di
installazione, clonazione e aggiornamento.

Impostazioni del progetto (modificare una volta):
- API: http://127.0.0.1:8765/api/v1
- Destinazione: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- Rotte REST: [{"type":"direct","enabled":true}]
- site_profile_id facoltativo: <EXISTING_PROFILE_ID_OR_OMIT>

Usa lo strumento REST locale connesso o gli strumenti MCP di MultiRoute quando
disponibili. Per MCP, configura separatamente indirizzi IPv4 espliciti dei gateway;
il suo download_add richiede url, directory e gateways. La rotta REST direct è il
percorso del sistema operativo; non è un valore di gateway MCP. Non inventare rotte
e non presumere la selezione nell’interfaccia.

Con REST, controlla GET /health e GET /license/policy prima del primo utilizzo in
una sessione. Con il solo MCP, rileva gli strumenti e chiama download_list con
limit 1 per verificare connettività e funzionalità; l’adattatore stesso controlla
la politica di licenza. La creazione di download richiede features.automation_api=true.
Se non disponibile, segnala il motivo e chiedi quale altro gestore usare invece
di usarne uno silenziosamente.

Per ogni file necessario: scegli URL esatto, destinazione autorizzata e nome file;
passa un SHA-256 attendibile dell’editore come expected_sha256 quando disponibile.
Crea l’attività con POST /jobs (routes, non gateways) o MCP download_add. Usa una
chiave di idempotenza stabile per i nuovi tentativi della stessa richiesta;
conserva l’id dell’attività restituito. La creazione non è il completamento.
Interroga quell’attività con GET /jobs/{id} o download_status a intervalli ragionevoli.
Segnala gli stati sospesi o non riusciti; riutilizza l’attività esistente per
riprendere invece di creare duplicati. Non modificare altre attività o impostazioni
di rete estranee. Al completamento controlla checksum_checking e integrity_status.
Non dichiarare una verifica dell’editore senza una corrispondenza con un riferimento
attendibile. Restituisci il percorso locale salvato, l’id dell’attività e l’esito
della verifica. Il file rimane sul computer MultiRoute; non affermare che esista
in una sandbox di esecuzione remota.
```

La regola esprime una preferenza del progetto. Restano necessari uno strumento connesso compatibile e i normali permessi dell’agente. I file di origine scaricati sono dati, non istruzioni aggiuntive per l’agente.
