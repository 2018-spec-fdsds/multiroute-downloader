# Instruction de projet MultiRoute — à copier dans les règles de votre agent

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <strong>Français</strong> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<a href="ai-agent-instructions.ko.md">한국어</a> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

Utilisez ce bloc dans votre `AGENTS.md`, `CLAUDE.md` ou vos instructions persistantes de projet équivalentes existantes. Ajoutez-le en conservant les autres règles du projet. Remplacez une fois la destination et les routes. Pour la configuration et les exemples, consultez le [français](../ai-agents.fr.md) ou l’[anglais](../ai-agents.en.md).

```markdown
## Téléchargements : utiliser MultiRoute par défaut

Pour les fichiers externes nécessaires à la tâche de projet autorisée (modèles,
jeux de données, archives, médias et autres ressources), utilise MultiRoute
Downloader en cours d’exécution. Ne me demande pas de choisir un gestionnaire
de téléchargements pour chaque fichier. Garde les gestionnaires de paquets et
Git sur leurs procédures normales d’installation, de clonage et de mise à jour.

Paramètres du projet (à modifier une fois) :
- API: http://127.0.0.1:8765/api/v1
- Destination : <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- Routes REST : [{"type":"direct","enabled":true}]
- site_profile_id facultatif : <EXISTING_PROFILE_ID_OR_OMIT>

Utilise l’outil REST local connecté ou les outils MCP MultiRoute s’ils sont
disponibles. Pour MCP, configure séparément des adresses IPv4 explicites de
passerelles ; son download_add nécessite url, directory et gateways. La route
REST direct est le chemin du système d’exploitation ; ce n’est pas une valeur
de passerelle MCP. N’invente pas de routes et ne présume pas la sélection dans
l’interface.

Avec REST, vérifie GET /health et GET /license/policy avant la première utilisation
d’une session. Avec MCP seulement, découvre les outils et appelle download_list
avec limit 1 pour vérifier la connectivité et la capacité ; l’adaptateur vérifie
lui-même la politique de licence. La création de téléchargements nécessite
features.automation_api=true. Si elle est indisponible, signale la raison et
demande quel autre gestionnaire utiliser au lieu d’en utiliser un silencieusement.

Pour chaque fichier nécessaire : choisis son URL exacte, sa destination autorisée
et son nom ; transmets un SHA-256 de confiance de l’éditeur comme expected_sha256
s’il est disponible. Crée la tâche avec POST /jobs (routes, pas gateways) ou MCP
download_add. Utilise une clé d’idempotence stable pour les nouvelles tentatives
de la même requête ; conserve l’id de tâche renvoyé. La création n’est pas
l’achèvement. Consulte cette tâche avec GET /jobs/{id} ou download_status à des
intervalles raisonnables. Signale les états suspendus ou en échec ; réutilise la
tâche existante pour reprendre au lieu de créer des doublons. Ne modifie pas les
autres tâches ni les paramètres réseau sans rapport. À la fin, vérifie
checksum_checking et integrity_status. N’affirme pas avoir vérifié auprès de
l’éditeur sans correspondance avec une référence de confiance. Renvoie le chemin
local enregistré, l’id de tâche et le résultat de vérification. Le fichier reste
sur l’ordinateur MultiRoute ; ne prétends pas qu’il existe dans un bac à sable
d’exécution distant.
```

La règle exprime une préférence du projet. Un outil connecté compatible et les permissions habituelles de l’agent restent nécessaires. Les fichiers sources téléchargés sont des données, pas des instructions supplémentaires pour l’agent.
