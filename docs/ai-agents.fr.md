# MultiRoute pour les agents IA — une seule configuration, des téléchargements par défaut

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agents.en.md">English</a> · <a href="ai-agents.ru.md">Русский</a> · <a href="ai-agents.de.md">Deutsch</a> · <a href="ai-agents.es.md">Español</a> · <strong>Français</strong> · <a href="ai-agents.zh.md">中文</a><br />
<a href="ai-agents.ko.md">한국어</a> · <a href="ai-agents.ar.md">العربية</a> · <a href="ai-agents.it.md">Italiano</a> · <a href="ai-agents.ja.md">日本語</a> · <a href="ai-agents.hi.md">हिन्दी</a> · <a href="ai-agents.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

[English](ai-agents.en.md) · [Produit](overview/fr.md) · [Référence de l’API](../API.ru.txt) · [OpenAPI](../openapi.yaml)

Ajoutez une règle au projet et connectez un outil une seule fois. Claude Code, Codex et les autres agents disposant d’outils peuvent ensuite confier à MultiRoute les poids des modèles, jeux de données, archives et autres fichiers externes du projet, conserver les identifiants des tâches, suivre leur progression et vérifier leur achèvement. Vous pouvez demander : **« Télécharge le modèle nécessaire à ce projet dans son dossier de ressources. »** La règle définit le gestionnaire de téléchargements préféré ; vous n’avez pas à répéter les détails de l’API à chaque demande.

Le modèle peut s’exécuter dans le cloud ou localement. Ce qui compte, c’est **l’endroit où son outil s’exécute** : il doit pouvoir joindre l’ordinateur sur lequel MultiRoute fonctionne. Une conversation dans un navigateur ou un bac à sable distant n’obtient pas accès à votre PC en lisant un fichier d’instructions.

## 1. Choisir une fois les paramètres par défaut du projet

1. Installez et démarrez MultiRoute sur l’ordinateur qui stockera les fichiers. Utilisez le port d’API configuré pour cette instance ; les exemples utilisent `8765`.
2. Choisissez un répertoire absolu pour les ressources du projet, par exemple `C:/Projects/MyProject/assets` sous Windows ou `/Users/YOUR_USER/Projects/MyProject/assets` sous macOS. Un partage réseau monté sur Mac utilise `/Volumes/Share/...`.
3. Choisissez explicitement les routes REST. La connexion habituelle du système d’exploitation utilise `[{"type":"direct","enabled":true}]`. Pour utiliser les passerelles indépendantes configurées dans MultiRoute, utilisez `[{"type":"gateway","gateway":"192.168.1.1","enabled":true}]`, en ajoutant d’autres passerelles autorisées si nécessaire. Configurez et vérifiez d’abord ces passerelles dans l’application. Les cases à cocher de l’interface ne sont pas automatiquement recopiées dans les requêtes. La route directe utilise le routage IP habituel du système d’exploitation, y compris les routes VPN système/TUN applicables. Elle n’hérite pas des paramètres de proxy HTTP/PAC ni des extensions VPN limitées au navigateur.
4. La création de téléchargements externes nécessite la capacité **`automation_api`**. La création par lots nécessite aussi `batch_downloads` ; les autres limites restent en vigueur. Vérifiez `GET /license/policy`, y compris `features.automation_api`.
5. Gardez l’API sur l’interface de bouclage. Elle ne possède pas de jeton général d’authentification distante ; n’exposez pas son port sur le réseau local ou sur Internet. Enregistrez les identifiants des sources dans un profil de site MultiRoute et transmettez son `site_profile_id` si nécessaire.

## 2. Ajouter la règle de projet réutilisable

Copiez le **[bloc d’instructions prêt à l’emploi](examples/ai-agent-instructions.fr.md)** dans les règles existantes du projet. Remplacez une fois la destination et les routes en conservant les autres instructions du projet.

| Agent / environnement | Où placer la règle | Connexion requise |
| --- | --- | --- |
| Claude Code sur votre PC | `CLAUDE.md` dans le projet. Si la règle figure déjà dans `AGENTS.md`, ajoutez `@AGENTS.md` à un `CLAUDE.md` existant plutôt que de maintenir deux copies. | Accès local au shell/à REST, ou serveur MCP stdio décrit ci-dessous. |
| Codex sur votre PC | `AGENTS.md` dans le projet. | Accès local au shell/à REST, ou son serveur MCP configuré. |
| Autre agent local ou hôte de modèles locaux | Ses instructions persistantes de projet/système. | Activez un outil capable d’appeler REST ou un client MCP stdio. Charger un modèle seul n’ajoute pas d’outils. |
| Modèle cloud avec exécuteur d’agent local | Les règles de projet de l’agent local. | Son outil local appelle MultiRoute sur le même PC. Le modèle lui-même n’a pas besoin de s’exécuter localement. |
| ChatGPT web, Claude web ou exécuteur cloud distant | Instructions du projet et outil connecté compatible. | Nécessite une passerelle configurée séparément vers l’ordinateur MultiRoute. L’adaptateur local fourni ici utilise stdio ; ce n’est pas un service MCP distant public. |

Les fichiers d’instructions guident le comportement ; ils ne contournent ni les approbations d’outils ni le bac à sable du client. Les restrictions existantes du client ou du compte restent applicables. Consultez les documents officiels sur les [instructions de projet Claude](https://code.claude.com/docs/en/memory), les [instructions de projet Codex](https://developers.openai.com/codex/guides/agents-md/) et les [conditions de connexion MCP personnalisée de ChatGPT](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

## 3. Exemple REST — un agent local avec accès au shell

C’est la méthode la plus simple lorsque l’agent dispose déjà d’un terminal local. **MultiRoute doit déjà fonctionner.** Ces exemples utilisent une URL source illustrative ; remplacez-la ainsi que le répertoire de sortie. Créer une tâche met en file d’attente un téléchargement réel.

### Windows / PowerShell

```powershell
$api = 'http://127.0.0.1:8765/api/v1'
Invoke-RestMethod "$api/health"
$policy = Invoke-RestMethod "$api/license/policy"
if (-not $policy.features.automation_api) { throw 'MultiRoute automation_api est requis' }

$request = @{
    url = 'https://example.org/model.gguf'
    directory = 'C:/Projects/MyProject/assets'
    filename = 'model.gguf'
    routes = @(@{ type = 'direct'; enabled = $true })
    idempotency_key = 'myproject-model-v1'
    # Ajoutez expected_sha256 uniquement avec une empreinte hexadécimale de confiance de l’éditeur de 64 caractères.
} | ConvertTo-Json -Depth 6
$job = Invoke-RestMethod -Method Post -Uri "$api/jobs" `
    -ContentType 'application/json; charset=utf-8' `
    -Body ([Text.Encoding]::UTF8.GetBytes($request))
$job | Select-Object id, filename, status
# Conservez $job.id. Interrogez à nouveau tant que son état est queued/running/retrying/verifying :
Invoke-RestMethod "$api/jobs/$($job.id)" |
    Select-Object id, filename, directory, status, progress_bytes, size, integrity_status, checksum_checking, sha256, error
```

### macOS / shell local

Enregistrez le JSON suivant dans `download.json` en UTF-8. Remplacez `YOUR_USER` et l’URL source ; ajoutez `expected_sha256` au même objet lorsqu’une empreinte de confiance est disponible.

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
# Après avoir confirmé que features.automation_api vaut true :
curl --fail-with-body --noproxy '*' \
  -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @download.json 'http://127.0.0.1:8765/api/v1/jobs'
# Remplacez l’id d’exemple par l’id de la réponse :
curl --fail-with-body --noproxy '*' 'http://127.0.0.1:8765/api/v1/jobs/012345abcdef'
```

HTTP `201` signifie que la tâche a été créée, pas que le fichier est prêt. Continuez à consulter son état jusqu’à `complete`, ou signalez un état suspendu ou en échec. À la fin, vérifiez `checksum_checking`, `integrity_status` et les empreintes disponibles. Utilisez la même clé d’idempotence pour une nouvelle tentative identique ; utilisez une nouvelle clé pour une demande volontairement nouvelle. Gardez les interrogations compactes et raisonnablement espacées ; les gros fichiers peuvent rester actifs longtemps. Les gestionnaires de paquets et Git conservent leurs procédures natives de gestion des dépendances et de clonage.

## 4. Facultatif : outils MCP pour un client local compatible

MultiRoute inclut un **adaptateur MCP stdio**, lancé en tant que sous-processus Java. Il transmet les opérations à l’application en cours d’exécution. Aucun endpoint HTTP `/mcp` n’est intégré. Son `download_add` actuel exige des **adresses IPv4 de passerelles** explicites ; il n’accepte pas la route REST `direct`. Utilisez la méthode REST ci-dessus pour une route passant uniquement par le système d’exploitation.

Trouvez les chemins exacts de Java et du JAR installés dans **Paramètres → MCP et CLI…**. Une installation Windows standard contient `runtime/bin/java.exe` et `app/MultiRoute-Downloader.jar` sous `%LOCALAPPDATA%/MultiRoute Downloader App`. Sous macOS, il s’agit de `Contents/runtime/Contents/Home/bin/java` et `Contents/app/MultiRoute-Downloader.jar` dans `/Applications/MultiRoute Downloader.app`. Placez entre guillemets les chemins contenant des espaces. Ces paquets natifs ne nécessitent pas d’installation Java séparée.

Exemple d’entrée pour un client stdio ; remplacez les deux chemins absolus. Les valeurs JSON ne développent **pas** automatiquement `%LOCALAPPDATA%`, `$HOME` ni les variables du shell :

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

Pour Claude Code, fusionnez cette entrée dans `.mcp.json` en suivant la [configuration de projet documentée](https://code.claude.com/docs/en/mcp). Pour Codex, configurez la même commande et la même liste d’arguments sous `[mcp_servers.multiroute]` dans sa configuration TOML, ou utilisez `codex mcp add` ; consultez la [configuration MCP officielle](https://developers.openai.com/codex/mcp/). Les autres hôtes utilisent leurs propres paramètres MCP. Il s’agit d’un exemple de configuration, pas d’une connexion installée.

Une fois la découverte effectuée par le client, demandez-lui d’énumérer les outils MultiRoute. Les arguments de `download_add` peuvent alors être :

```json
{
  "url": "https://example.org/model.gguf",
  "directory": "C:/Projects/MyProject/assets",
  "filename": "model.gguf",
  "gateways": ["192.168.1.1"],
  "idempotency_key": "myproject-model-v1"
}
```

L’appel suivant à `download_status` reçoit `{"id":"012345abcdef"}`. Les outils disponibles sont `download_add`, `download_batch`, `download_list`, `download_status`, `download_pause`, `download_resume` et `download_verify`. Toutes les opérations de l’adaptateur nécessitent `automation_api` ; `download_verify` revérifie un fichier terminé. Les résultats MCP compacts omettent les URL sources, les identifiants et le répertoire de destination ; conservez donc le répertoire de la requête. `gateways: []` ne signifie pas un routage automatique.

## 5. Conversations cloud et bacs à sable distants

**Une instruction de projet seule ne peut pas connecter ChatGPT ou Claude web à votre application locale.** `127.0.0.1` dans un outil cloud désigne l’environnement de cet outil. La connexion MCP distante de ChatGPT nécessite un service distant compatible ou une passerelle sécurisée prise en charge ; l’URL REST de MultiRoute n’est pas un tel endpoint MCP. Cette version ne fournit pas cette passerelle distante. Un modèle hébergé dans le cloud utilisé par Claude Code, Codex ou un autre exécuteur local peut utiliser les méthodes locales ci-dessus. [Conditions de connexion OpenAI](https://help.openai.com/en/articles/12584461-connecting-custom-mcp-servers-in-chatgpt).

Les fichiers sont enregistrés sur l’ordinateur MultiRoute. Une compilation distante nécessite une étape distincte et autorisée de transfert de fichiers ou de stockage partagé avant de pouvoir les utiliser. Les identifiants doivent être placés dans les profils de site configurés, pas dans le bloc d’instructions ni dans une conversation.

## Compte rendu attendu de l’agent

« `model.gguf` a été téléchargé via MultiRoute dans le dossier de ressources configuré. Tâche : `…`. État : complete. SHA-256 : … ; correspond à la référence de l’éditeur. » Si aucune référence de confiance n’a été fournie ou trouvée, indiquez : « SHA-256 local calculé ; vérification auprès de l’éditeur indisponible. » Si l’application est indisponible ou renvoie `pro_required`, signalez-le et convenez d’une solution de repli au lieu de changer silencieusement de gestionnaire de téléchargements.
