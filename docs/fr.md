# MultiRoute Downloader 4.3.9 — Guide en français

[macOS : installation, démarrage et autorisations](macos/fr.md)

<!-- product-overview:start -->
[**Présentation du produit**](overview/fr.md)
<!-- product-overview:end -->

[Accueil](../README.md) · [English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · **Français** · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

<!-- current437:start -->

<a name="current-free-pro"></a>

## 4.3.9 — Fonctions actuelles, Free et Pro

Ce guide décrit MultiRoute Downloader 4.3.9. Les installateurs natifs et les sommes de contrôle publiées sont disponibles dans la [version 4.3.9](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9). Les sections suivantes présentent ses fonctions actuelles et les droits de licence.

### Limites et fonctions de la licence

Free comprend les téléchargements unitaires, la reprise prise en charge, tout le débit disponible, la vérification d’intégrité, le DNS protégé et les débits/états actuels des passerelles. Aucun bridage artificiel du débit. Les droits signés sont indépendants : une licence Pro partielle n’active que ceux qu’elle contient.

| Fonction | Free | Avec le droit dans la licence | ID |
| --- | --- | --- | --- |
| Passerelles utilisées ensemble | ≤ 2 | ≤ 8 | `extra_gateways` |
| Téléchargements simultanés | ≤ 3 | ≤ 32 | `extra_concurrent_downloads` |
| Profils de sites | ≤ 3 | Sans quota de licence | `extra_site_profiles` |
| Priorités de téléchargement | Normal uniquement | Au choix | `download_priorities` |
| Priorités des passerelles | 0 | Au choix | `gateway_priorities` |
| Historique et rapports des passerelles | Non inclus | Inclus | `gateway_statistics` |
| Téléchargements / envois groupés | Non inclus | Inclus | `batch_downloads` |
| API pour applications externes | Non inclus | Inclus | `automation_api` |
| Thèmes supplémentaires | Graphite / System | Tous les thèmes | `premium_appearance` |

Pour un lot, ouvrez **Nouveau téléchargement…** et choisissez **Liste de liens…** dans la ligne d’actions fixe en bas de cette fenêtre.

Seuls les droits de votre licence signée sont activés. Fichiers et profils sont conservés. Les fichiers supplémentaires attendent en file ; les vitesses et états actuels restent accessibles. Les limites techniques de stockage s’appliquent.

La limite des passerelles porte sur l’ensemble partagé par téléchargements et envois, sans voie système supplémentaire. La limite de trois tâches simultanées concerne les téléchargements, pas les envois ; les autres attendent. Une limite de file configurée plus basse reste applicable. Les passerelles, profils, progrès et priorités enregistrés ne sont pas supprimés. Le quota limite la création de nouveaux profils ; les anciens restent utilisables. L’historique/CSV exige son droit ; les indicateurs instantanés restent Free. La planification reste disponible ; les priorités personnalisées exigent leur droit. Les limites techniques du stockage et des sources restent applicables.

La licence signée est liée à la clé d’installation, pas à une MAC, une IP ou un routeur. Elle n’expire pas ; 4–4 couvre 4.x. Changer de passerelle, DHCP ou MAC applicative ne demande pas de réactivation. Un autre ordinateur, compte ou profil peut nécessiter une nouvelle demande. Cela ne promet ni code impossible à modifier ni liaison matérielle universelle.

### MAC de l’application

MultiRoute utilise son propre MAC stable en IP manuelle et DHCP. Le MAC de la carte système reste inchangé. Les conflits avec les appareils observés et autres instances sont vérifiés ; une absence de réponse n’exclut pas des appareils éteints. Plage : MAC unicast administrés localement, 46 bits aléatoires. Ce n’est pas une plage IP DHCP.

**Modifier le MAC…** — Utilisez le bouton après la pause/fin des transferts actifs ou en attente et la fin des vérifications locales. Les adresses observées et les annonces coopératives d’autres instances sont contrôlées. « Aucun conflit détecté » ne garantit pas qu’un appareil éteint ou silencieux n’utilise pas cette adresse. La MAC applicative doit différer de la MAC physique ; l’adaptateur et les routes système ne changent pas.

### Somme de contrôle…

La référence est enregistrée sans redémarrage ni remise à zéro. MD5, SHA-256 ou SHA-512 est reconnu par 32, 64 ou 128 chiffres hexadécimaux. Le fichier assemblé sera vérifié à la fin. Enregistrez une référence puis choisissez Vérifier maintenant. Le fichier local terminé est lu. Un écart n’entraîne ni retéléchargement ni suppression.

La vérification finale utilise la dernière référence enregistrée. La modifier n’efface pas les octets et ne relance pas le transfert. La revérification locale conserve le fichier terminé et indique correspondance, différence ou erreur séparément. Les références de l’éditeur sont conservées indépendamment. Une somme globale ne concerne qu’un fichier torrent. L’intégrité ne prouve pas l’innocuité du contenu ; préférez une référence SHA-256/SHA-512 fiable.

### Accès à l’API dans 4.3.9

L’API JSON locale utilise UTF-8 sans jeton API ordinaire. La gestion externe exige automation_api ; les autres droits sont contrôlés aussi. Ce droit seul n’autorise ni lots ni passerelles supplémentaires. Free conserve un diagnostic de lecture limité, le cycle de vie avec authentification privée et le pont optionnel pour un seul lien du navigateur. Voir [API](../API.ru.txt) et [OpenAPI](../openapi.yaml) ; ne pas exposer le port au LAN/Internet.


L’application originale utilise la [Licence 1.2](../LICENSE.txt) : Free autorise l’usage personnel et professionnel ; le titulaire peut proposer Pro payant. Les autres destinataires ne peuvent la vendre sans autorisation distincte. OpenJDK GPLv2 avec Classpath Exception exige notices/sources correspondantes ; JNA utilise Apache-2.0 avec notices ; JLayer LGPL-2.1 séparée conserve les droits de remplacement/débogage et d’accès aux sources. Voir [conditions](../LICENSING.md), [catalogue complet](../THIRD-PARTY-NOTICES.txt) et la page des licences.

Le style System garde le texte des tableaux, champs et commandes lisible sur le fond système sélectionné ; Graphite et System sont inclus dans Free.

Le nom et la taille du fichier ont des colonnes séparées. Le tri de taille est numérique et indépendant du nom ; une taille inconnue est indiquée par un tiret. Le nom et le chemin complets sont disponibles dans l’infobulle.

Survolez le nom du fichier pour voir le nom complet, le chemin de destination, le protocole et l’adresse ou le chemin local de la source. Une tâche torrent indique BitTorrent même si ses métadonnées ont été ouvertes en HTTPS. L’adresse du dépôt, si disponible, suit l’URL réelle du fichier. Les identifiants intégrés, valeurs de requête et fragments sont masqués ; un infohash magnet public reconnu peut rester visible. L’URL enregistrée et l’état du transfert ne changent pas.


### Priorités des passerelles et diagnostic des transferts dans 4.3.9

Dans les paramètres réseau, attribuez à chaque passerelle une priorité de **0 à 1000** ; sa valeur apparaît aussi près de son indicateur. Une valeur élevée favorise cette passerelle pour les petits fichiers et les transferts complets, et lui attribue davantage de segments parallèles pour les gros fichiers. Les autres passerelles sélectionnées et disponibles continuent à contribuer. Les valeurs égales conservent l’ordre de sélection ; une autre passerelle peut reprendre le travail si la préférée échoue ou se bloque. Aucun rapport précis de vitesse n’est garanti.

Le droit signé indépendant **`gateway_priorities`** est nécessaire. Le droit de priorité des tâches **`download_priorities`** ne l’inclut pas. Sans ce droit, toutes les priorités effectives sont 0 ; les valeurs enregistrées sont conservées. Leur modification pendant un téléchargement n’efface pas les parties vérifiées. Chaque passerelle sélectionnée utilise un à quatre traitements de segments parallèles, avec au plus 32 par tâche.

Les paramètres permettent de choisir le nombre de téléchargements simultanés dans la limite de la licence et des tentatives automatiques limitées ou illimitées. Les pauses manuelles restent actives. Le menu contextuel permet de mettre en pause/reprendre les tâches sélectionnées compatibles et de copier les noms de fichiers. Le double-clic ouvre uniquement le dossier ; propriétés, statistiques et journal individuel se trouvent dans le menu contextuel.

Le journal individuel reste disponible pour les fichiers terminés. Paramètres → Erreurs et solutions ouvre un guide hors ligne avec recherche : 25 sujets dans les 12 langues de l’interface. Le dossier de destination et les propriétés indiquent l’espace libre en Go. Un disque local plein est signalé explicitement, les parties sont conservées et ce défaut ne devient pas une boucle de tentatives réseau. Le préfixe enregistré reste visible dans la progression lorsqu’un petit segment repris attend des données.

### Données restant à télécharger · 4.3.9

Le tableau comporte une colonne **Données restantes**. Elle indique les octets restant à télécharger, séparément du temps estimé, avec un tri numérique. La valeur tient compte de la progression enregistrée et en cours, ne devient jamais négative et affiche un tiret si la taille totale est inconnue.

### Démarrage Mac et passerelles plus fiables · 4.3.9

Le démarrage natif sur Mac reconnaît l’ancien chemin relatif par défaut, y compris sans barre initiale. Il utilise un profil existant vérifié ou un dossier utilisateur par défaut vide. Un emplacement personnalisé ambigu exige toujours un choix explicite.

Après un échec ou une annulation de préparation, les autres passerelles et contrôles en arrière-plan cessent de répéter les demandes d’autorisation jusqu’à une nouvelle tentative explicite de configuration réseau. Le premier démarrage privilégié peut encore demander la confirmation normale de l’administrateur macOS pour ouvrir le périphérique BPF Ethernet.

Le composant réseau Mac 4.3.2 tient compte d’une adresse MAC masquée par macOS tout en conservant les contrôles de protection de la carte physique. La MAC propre à l’application est conservée, sans changer les routes du système ni la MAC physique. L’icône et le nom Mac reprennent l’identité MultiRoute de Windows. Quittez l’application avant la mise à jour PKG. Le nom de l’appelant dans le dialogue réel et les transferts LAN/DHCP restent à confirmer sur matériel ; ce transport nécessite toujours Ethernet filaire.

**macOS:** `/Applications/MultiRoute Downloader.app` · `/Library/PrivilegedHelperTools/MultiRouteResume/4.3.2/{gateway-net,gateway-launch}`.

### Réponses des passerelles Mac avec une adresse manuelle · 4.3.9

La capture POSIX est corrigée pour que l’adresse manuelle de l’application et DHCP utilisent tous deux un mode recevant les réponses à sa propre MAC, comme sous Windows. Un ping système réussi vérifie la connexion de l’ordinateur, pas la réception à l’adresse virtuelle de l’application.

Les diagnostics DHCP conservent leur texte en douze langues au lieu d’une erreur générique. L’autorisation existante reste inchangée : les contrôles normaux réutilisent le composant préparé. Les points d’accès API, commandes, routes du système et MAC physique ne changent pas. Le premier accès privilégié à Ethernet BPF peut encore demander la confirmation normale de l’administrateur.

Cela corrige une différence de mode de capture, pas toutes les causes de passerelle inaccessible. Les vérifications d’adresse libre, de sous-réseau et de bail/conflit DHCP restent actives. Les transferts Mac avec adresse manuelle restent à confirmer sur matériel ; Ethernet filaire demeure nécessaire.

<!-- current437:end -->

<a name="appearance-and-sounds"></a>

## Apparence et sons de fin

Choisissez parmi neuf apparences : Graphite, Nordic, Streams Studio, Neon, Community, Chat, Studio, Rose et System. Graphite est l’apparence par défaut. Graphite et System sont inclus dans Free ; les autres choix nécessitent le droit Pro d’apparence correspondant. Ouvrez Paramètres → Apparence pour changer le style et les commandes arrondies. Vos téléchargements continuent pendant votre choix. **Fenêtres et boutons arrondis** est une option distincte. Les commandes peuvent être arrondies dans tous les styles ; le cadre extérieur dépend du système. Windows 11 peut conserver des angles droits pour les fenêtres maximisées ou ancrées ; macOS et Linux gèrent eux-mêmes leur contour. L’interface propose un graphique de vitesse et les paramètres réseau utilisent des fiches compactes pour les passerelles.

Les sons sont **désactivés par défaut**. Cochez l’activation générale, puis choisissez séparément les notifications **pour chaque fichier** et **pour tous les téléchargements**. Sélectionnez **Cloches douces**, **Piano** ou **Marimba**, puis écoutez un aperçu de chaque mélodie. Lorsque tous les téléchargements sont terminés et que l’alerte générale est activée, seul l’air final accompagne le dernier fichier, sans second son superposé. Les tâches en pause ou en échec ne sont pas terminées. L’ancien historique reste silencieux au démarrage et lors des changements de préférences. Vérifiez aussi le volume et la sortie audio du système.

Pour vos propres sons, choisissez **Mélodie personnalisée** et sélectionnez des fichiers **MP3 ou WAV** distincts pour un fichier et pour tous les téléchargements. Chaque extrait doit durer **10 secondes au maximum**. L’application vérifie la durée de l’audio décodé plutôt que de se fier à l’extension ou aux métadonnées. Les fichiers acceptés sont copiés dans le profil local de l’application ; vous pouvez ensuite déplacer les originaux.

Le coursier de bureau facultatif transforme la progression des téléchargements en une charge visible. Choisissez l’homme ou la femme dans les paramètres d’apparence et réglez la taille de 40% à 100%. Le sac indique le total des volumes restants connus des téléchargements actifs et rétrécit à mesure que les octets arrivent. Le mouvement suit leur débit de téléchargement cumulé ; lorsque le débit est nul, le coursier reste immobile. Les petites charges sont portées sur l’épaule, les plus grosses sont traînées et les très grosses demandent davantage d’effort.

Utilisez le bouton Avatar pour afficher ou masquer le coursier. Faites-le glisser pour le déplacer, double-cliquez avec le bouton gauche de la souris pour afficher l’application ou faites un clic droit pour ses options. C’est un compagnon visuel : les fichiers restent dans les dossiers choisis.

À la fin d’un téléchargement, le coursier dépose la charge livrée et disparaît progressivement. Si des téléchargements continuent, il revient avec les données restantes.

L’apparence, les arrondis et les sons sont des préférences locales du bureau. Ils n’ajoutent aucun point d’accès à l’API de téléchargement et ne modifient ni les transferts, ni les sommes de contrôle, ni la sécurité DNS.

<a name="startup-profile-location"></a>

## Dossier de données de l’application

L’application native utilise un chemin absolu enregistré vers le dossier de données, indépendant du répertoire de travail à l’ouverture de session. L’historique des téléchargements, les paramètres et les parties enregistrées sont stockés dans le profil sélectionné. Les chemins de la version portable en ligne de commande sont pris en charge ; un `--data-dir` explicite choisit le dossier de données.

<a name="automatic-startup"></a>

## Démarrage automatique

Activez **Démarrer avec le système d’exploitation** près des commandes de langue et d’apparence. L’option est **désactivée par défaut**. Elle inscrit MultiRoute pour la prochaine ouverture de session de votre compte ; la désactiver supprime sa propre entrée. Il s’agit d’un lancement à l’ouverture de session, pas d’un service système ni d’un lancement avant connexion. Créer l’entrée ne nécessite pas les droits administrateur.

Le démarrage automatique utilise la zone de notification si elle est disponible ; sinon une fenêtre normale reste accessible. Les priorités, pauses et horaires de la file restent applicables ; les téléchargements en pause ne sont pas forcés à reprendre. Ouvrez d’abord manuellement l’application pour terminer l’installation et les prérequis réseau. Conservez l’application et, en usage portable, Java et les bibliothèques à leurs chemins enregistrés. Après un déplacement, désactivez puis réactivez le démarrage depuis le nouvel emplacement. Désactivez-le avant la désinstallation.

Le système ou la politique du bureau peut bloquer une entrée enregistrée. Sur macOS, consultez **Réglages Système → Général → Ouverture et extensions** si une autorisation est demandée ; l’autorisation réseau est distincte. L’inscription ne prouve pas un futur démarrage réussi. Les vérifications sur profils de test n’incluent pas un véritable redémarrage ou une fermeture de session ; consultez le rapport de la version.

Si l’application ne s’ouvre pas, supprimez **uniquement son entrée** :

- **Windows :** l’application stocke sa propre valeur `REG_SZ` **MultiRoute Downloader** dans `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`. Désactivez de préférence le démarrage depuis l’app. Si elle ne s’ouvre pas, supprimez **uniquement cette valeur** dans l’Éditeur du Registre, en conservant la clé Run et les autres entrées. Si le système l’a désactivée, utilisez **Paramètres → Applications → Démarrage** ; l’app respecte cette décision. Windows limite la commande complète à 260 caractères ; si elle est trop longue, choisissez un chemin d’application/profil plus court et réinscrivez-la.
- **macOS :** supprimez `~/Library/LaunchAgents/io.github.2018-spec-fdsds.multiroute-downloader.plist`.
- **Linux :** supprimez `${XDG_CONFIG_HOME:-~/.config}/autostart/io.github.2018-spec-fdsds.multiroute-downloader.desktop` du répertoire XDG réel. Un bureau graphique compatible avec le démarrage XDG est nécessaire ; aucun service systemd n’est installé.

<a name="dns-settings"></a>

## Paramètres DNS

Les résultats de vérification des plateformes et des installateurs figurent dans [NATIVE-INSTALLER-TESTS.json](../NATIVE-INSTALLER-TESTS.json).

Ouvrez **Réseau → Résolution DNS**. Le réglage par défaut est **DNS sur HTTPS (DoH), Cloudflare**. Il concerne uniquement MultiRoute et ne change ni DNS, ni routes, ni adresses IP du système.

DoH prend en charge HTTP/2 borné sélectionné par TLS ALPN et HTTP/1.1 lorsque ce protocole est négocié. Compatibilité des fournisseurs et résultats réels figurent dans le rapport final correspondant.

- **DoH :** DNS chiffré sur HTTPS via chaque passerelle sélectionnée. Le certificat TLS est vérifié avec le nom d’hôte de l’URL.
- **DoT :** DNS chiffré sur TLS via chaque passerelle, port 853 par défaut ; le nom du serveur sert à vérifier le certificat.
- **DNS système (ordinateur) :** utiliser explicitement la politique DNS de l’ordinateur, du système ou du VPN. Le DNS ne suit pas nécessairement chaque passerelle ; les connexions de fichiers gardent leur passerelle. Ce mode ne garantit pas le chiffrement DNS.

Choisissez Cloudflare, Quad9, Google ou **Personnalisé**. Les préréglages sont fixes ; Personnalisé permet de modifier URL/nom, port et IPv4 publiques numériques d’amorçage. Elles évitent une résolution préalable du fournisseur par le système. DoH/DoT vérifient chaîne de confiance, nom et SNI, sans repli silencieux vers DNS non chiffré ou système. Un échec sécurisé termine cette tentative. Le fournisseur doit être accessible via la passerelle choisie.

Modifiez un brouillon, sélectionnez les passerelles et cliquez sur **Tester DNS**. Le test ne l’enregistre pas et affiche résultat, durée et IPv4 publiques ou erreur par passerelle. Un échec partiel reste visible. En mode Système, une réussite vérifie la résolution du système, pas la liaison Internet indépendante de chaque routeur. **Appliquer les paramètres DNS** valide et enregistre. La révision retournée identifie la configuration appliquée ; actualisations et changements de langue préservent brouillon, curseur et défilement.

Chaque requête dispose de 8 secondes au total, requêtes DNSSEC auxiliaires incluses. Le TTL validé respecte aussi l’expiration des signatures. Le cache natif est limité à 300 secondes et séparé par passerelle/révision. TTL nul et HTTP Age de DoH restent applicables. Le mode Système suit le cache OS/JVM. Les destinations IPv4 publiques sont prises en charge ; la résolution IPv6 n’est pas prise en charge.

API : `GET /api/v1/network/dns` lit configuration et état en lecture seule ; `PUT` ou `POST` applique la configuration. `POST /api/v1/network/dns/test` teste un brouillon ; `GET /api/v1/network/dns/providers` liste les préréglages. Envoyez uniquement les champs de configuration, sans révision/état de GET. [API](../API.ru.txt) · [OpenAPI](../openapi.yaml).

**Gestion des plages HTTP :** lorsqu’un CDN comme Civitai renvoie HTTP 200 pour une plage commençant à zéro, une plage commençant après zéro est aussi vérifiée. Le premier segment reste limité à sa taille ; les données séquentielles enregistrées compatibles peuvent être reprises en segments. Un HTTP 200 temporaire pour une plage suivante entraîne une nouvelle tentative après 2–30 secondes, en conservant les données et le processus de la passerelle. Le journal persistant par passerelle distingue le trafic reçu de la progression enregistrée. [Diagnostic des transferts](transfer-diagnostics.en.md).

[Les installateurs 4.3.9](../downloads/v4.3.9/README.md) incluent un environnement Java 21 privé. L’assistant Windows est en anglais et crée un raccourci de bureau ; Npcap reste requis séparément. macOS utilise libpcap du système. Suivez les étapes d’installation et de sécurité ci-dessous.

Propriétaire du projet: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## À quoi sert l’application ?

MultiRoute Downloader est une application de bureau Java qui télécharge de gros fichiers au moyen de plusieurs passerelles Internet présentes sur le même réseau Ethernet local. Les passerelles choisies prennent des segments dans une file commune. Une passerelle plus rapide peut ainsi télécharger davantage de segments ; une passerelle lente ne bloque pas les autres. Une transmission interrompue peut reprendre lorsque le protocole et l’identité du fichier permettent une reprise sûre.

Elle est particulièrement utile lorsque plusieurs accès Internet indépendants sont disponibles, par exemple une ligne fixe filaire et un routeur mobile 4G/5G. Leurs passerelles doivent être accessibles depuis l’ordinateur sur le même réseau Ethernet local. L’application utilise simultanément toutes les passerelles sélectionnées et disponibles pour télécharger des segments de fichiers, ce qui peut augmenter le débit total. Le gain réel dépend de la capacité indépendante des connexions et des limites du serveur de téléchargement.

Pour les téléchargements segmentés, la panne d’une connexion n’empêche pas les autres passerelles de continuer. Les segments incomplets sont réattribués aux passerelles encore disponibles et les données déjà vérifiées sont conservées. L’application prend également en charge le téléchargement groupé de plusieurs fichiers : des listes d’URL et une file d’attente permettent de gérer des groupes de téléchargements, que d’autres logiciels peuvent aussi créer via l’API locale.

L’application possède une interface Swing, un sélecteur de dossier propre au système et une API HTTP locale utilisable par d’autres logiciels. Aucun navigateur n’est nécessaire pour la piloter. L’interface, les messages d’état enregistrés et les aides sont disponibles en **anglais, russe, allemand, espagnol, français, chinois, coréen, arabe, italien, japonais, hindi et vietnamien**. Un nouveau profil utilise l’anglais. Le choix de langue est conservé et appliqué immédiatement ; les noms de fichiers, les URL et les clés JSON restent identiques. Certains messages externes non enregistrés, provenant d’un serveur ou d’une bibliothèque, peuvent conserver leur langue d’origine.

<a name="application-virtual-ip"></a>

### Pourquoi l’application utilise une IP virtuelle

**Choisissez les passerelles de MultiRoute sans changer la connexion des autres applications.** Le programme utilise sa propre adresse IPv4 locale libre pour son transport réseau interne. Il peut ainsi établir des connexions via les passerelles sélectionnées indépendamment de la passerelle par défaut de l’ordinateur, sans modifier les adresses des interfaces système, la table de routage ni la passerelle par défaut du système d’exploitation. Le navigateur et les autres programmes conservent leur configuration réseau habituelle.

Par exemple, dans un LAN `192.168.1.0/24`, l’ordinateur conserve son adresse normale `192.168.1.x`, MultiRoute peut utiliser l’adresse libre `192.168.1.70` et les téléchargements peuvent passer par `192.168.1.1`–`192.168.1.4`. L’ordinateur, l’adresse virtuelle et les passerelles choisies doivent appartenir au même sous-réseau IPv4 réel. Vérifiez et appliquez une adresse libre dans les paramètres réseau de l’application ; ne l’ajoutez pas manuellement à l’interface du système. L’IP virtuelle ne constitue pas un accès Internet supplémentaire : les routeurs et leurs liaisons externes fournissent la bande passante.

#### Choisir et vérifier l’adresse

1. Vérifiez l’IPv4 et le masque du port Ethernet de l’ordinateur. Choisissez une adresse d’hôte utilisable dans ce même sous-réseau. `192.168.1.70` n’est qu’un exemple : n’utilisez ni l’IP de l’ordinateur, ni celle d’une passerelle, ni l’adresse réseau ou de diffusion.
2. Ouvrez l’administration du routeur et vérifiez la plage DHCP réelle. Utilisez une adresse hors attribution automatique ou réservez/excluez-la sur le serveur DHCP. Si plusieurs serveurs DHCP desservent le LAN, chacun doit empêcher l’attribution de cette adresse à un autre appareil.
3. Dans MultiRoute, choisissez **Vérifier et appliquer** pour l’adresse virtuelle, puis vérifiez les passerelles. L’application valide le sous-réseau et recherche les conflits par ARP. L’absence de réponse ARP signifie seulement qu’aucun conflit n’a été détecté à cet instant ; elle ne prouve pas l’exclusion du pool DHCP et n’empêche pas une attribution ultérieure.

## Captures d’écran

Les fenêtres actuelles de l’application sont présentées ci-dessous. Ouvrez l’image en taille réelle pour lire chaque commande. Les valeurs de transfert d’exemple sont illustratives.

![MultiRoute Downloader — file de téléchargements et débits des passerelles](images/v4.3.7/downloads-en-ae5b55131583.png)

*Interface Windows actuelle, capturée dans un profil de démonstration isolé. Les fichiers d’exemple, la progression, les débits et les vues activées avec Pro illustrent les commandes et ne constituent pas des mesures de performance.*

<table>
<tr>
<td width="50%" valign="top"><strong>Paramètres de l’application</strong><br /><a href="images/v4.3.7/settings-8abcb12a782d.png"><img src="images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Paramètres de l’application" /></a></td>
<td width="50%" valign="top"><strong>Apparence, coursier et sons</strong><br /><a href="images/v4.3.7/appearance-cebf121d5de7.png"><img src="images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Apparence, coursier et sons" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Adresse de l’application et DNS protégé</strong><br /><a href="images/v4.3.7/network-dns-e29dfdba1007.png"><img src="images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Adresse de l’application et DNS protégé" /></a></td>
<td width="50%" valign="top"><strong>Sélection des passerelles et tests de débit</strong><br /><a href="images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Sélection des passerelles et tests de débit" /></a></td>
</tr>
</table>






## Environnement nécessaire

Les installateurs Windows/macOS incluent Java 21 privé et le composant réseau compilé. Windows nécessite séparément le pilote Npcap. Une installation Java distincte n’est pas nécessaire.

| Plateforme | Système d’exploitation | Composants |
| --- | --- | --- |
| Windows x64 | Windows 11 | Java 21 |
| Apple Silicon | macOS 26+ | Java 21 |
| Intel Mac | macOS 15+ | Java 21 |

Il faut un adaptateur Ethernet filaire dans le même sous-réseau IPv4 que toutes les passerelles choisies, une IPv4 virtuelle libre, des dossiers de données et de téléchargement accessibles en écriture et de l’espace pour le fichier et ses parties temporaires. Le gain dépend des accès WAN indépendants et du serveur source. Aucun GPU n’est nécessaire.

L’application prend en charge Linux, mais aucun installateur Linux 4.x n’est publié dans ce dépôt de distributions. N’utilisez pas les paquets Windows ou macOS sous Linux.

Avant une mise à jour, attendez la fin des téléchargements, envois et vérifications finales en cours. Quittez ensuite depuis la zone de notification et lancez le nouvel installateur complet. Conservez le dossier de données, les fichiers téléchargés et les parties enregistrées.

## Réseau et adresse IP virtuelle

Ces adresses de passerelles modifiables sont des exemples. Sélectionnez manuellement les passerelles de votre LAN : Free permet d’en utiliser jusqu’à **2 simultanément**, le droit Pro correspondant jusqu’à **8** :

```text
192.168.1.1
192.168.1.2
192.168.1.3
192.168.1.4
```

L’adresse virtuelle de l’application est **`192.168.1.70`** par défaut. Adaptez ces valeurs à votre réseau. L’application accepte **1 à 8 passerelles distinctes**, accessibles au moyen d’un adaptateur Ethernet physique approprié dans un sous-réseau IPv4 local commun. La passerelle utilisée par le système n’est pas ajoutée comme un cinquième chemin.

L’IP virtuelle appartient à la **pile réseau privée de l’application**. Elle permet des connexions indépendantes via les passerelles choisies, sans modifier les adresses des interfaces ni la table de routage du système, et donc sans changer les connexions des autres programmes. Vous n’avez pas à l’ajouter préalablement aux paramètres réseau du système. Elle doit toutefois être libre dans le LAN et appartenir au bon sous-réseau ; réservez-la si possible en dehors de la plage attribuée par DHCP. L’application recherche un conflit d’adresse avant son utilisation.

Ce transport Ethernet ne prend pas en charge le Wi-Fi, les adaptateurs exclusivement VPN ni les connexions IPv6. Les pairs BitTorrent locaux du LAN ne sont pas acheminés par ce transport. Si plusieurs adaptateurs Ethernet conviennent, l’application peut refuser une sélection ambiguë. Elle ne peut pas transformer une passerelle inaccessible en connexion Internet utilisable.

## Installateurs natifs 4.3.9 : Windows et macOS

| Système | Installateur | Lancement après installation |
| --- | --- | --- |
| Windows 11 x64 | [MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe) | **MultiRoute Downloader** dans Démarrer ou via son raccourci de bureau |
| Apple Silicon, macOS 26+ | [MultiRoute-Downloader-4.3.9-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg) | **MultiRoute Downloader** dans Applications, Spotlight ou le Dock |
| Intel, macOS 15+ | [MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg) | **MultiRoute Downloader** dans Applications, Spotlight ou le Dock |

Les douze guides et les captures sont inclus pour une consultation hors ligne. Windows : `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`. macOS : dans le Finder, choisissez **Afficher le contenu du paquet** sur l’application installée, puis ouvrez `Contents/app/docs/`. Ouvrez `en.md`, `ru.md`, `de.md`, `es.md`, `fr.md`, `zh.md`, `ko.md`, `ar.md`, `it.md`, `ja.md`, `hi.md` ou `vi.md` avec un éditeur de texte ou un lecteur Markdown.

### Windows : installer et ouvrir

1. Téléchargez le `.exe` et comparez son SHA-256 à celui publié. Ouvrez le fichier et suivez l’assistant. L’installation concerne votre compte ; choisissez les options de raccourcis proposées.
2. Ouvrez **MultiRoute Downloader** depuis Démarrer ou le raccourci. Une fenêtre d’application normale s’ouvre, sans fenêtre de commandes. Java est inclus et n’a pas besoin d’une installation séparée.
3. L’application vérifie Npcap. Si le pilote manque, confirmez son téléchargement dans l’application. Elle obtient l’installateur officiel fixé, vérifie SHA-256, signature Authenticode et éditeur, puis ouvre l’installation normale de Npcap. Lisez la licence et confirmez UAC de Windows. Si un redémarrage est demandé, redémarrez Windows puis ouvrez MultiRoute. Npcap reste un pilote séparé : **il n’est pas inclus ni installé silencieusement**. Internet est nécessaire à cette étape lorsqu’il manque.

L’application et l’installateur n’ont pas de signature d’éditeur. SmartScreen peut signaler un éditeur inconnu : vérifiez la publication officielle et le fichier avant de choisir **Informations complémentaires → Exécuter quand même**, si cette option est proposée. Une détection de logiciel malveillant est un autre cas. Ne désactivez pas l’antivirus et n’ajoutez pas d’exception comme étape d’installation. Une installation Npcap AdminOnly peut demander une autre autorisation de capture. Ouvrez Java sous votre compte normal, sans lancer toute l’application en administrateur.

### macOS : installer et ouvrir

1. Utilisez le `.pkg` du processeur **natif** : arm64 pour Apple Silicon, x86_64 pour Intel. Les nouveaux paquets exigent **macOS 26+ sur Apple Silicon** ou **15+ sur Intel**, y compris pour torrent/magnet. L’ancien minimum HTTP du ZIP portable ne s’applique pas à ces paquets.
2. Téléchargez le `.pkg`, vérifiez le SHA-256 publié et ouvrez-le avec l’Installateur Apple. Confirmez l’installation et l’autorisation d’administration pour placer le composant réseau protégé.
3. Ouvrez **`/Applications/MultiRoute Downloader.app`** dans Finder ou Spotlight. Son icône peut être ajoutée au Dock. Java et le composant déjà compilé sont inclus : **Terminal, Command Line Tools, Homebrew et une installation Java séparée ne sont pas nécessaires** pour ce paquet.
4. À la demande, confirmez le dialogue d’administration macOS normal pour permettre au composant protégé d’ouvrir le périphérique Ethernet BPF. Il réduit ensuite ses privilèges ; la fenêtre Java fonctionne sous votre compte normal. Aucune session Terminal n’est nécessaire pendant l’utilisation.

### macOS : confirmations de sécurité du paquet natif

Le paquet **n’est ni signé avec Developer ID ni notarié**. Une signature technique ad hoc n’atteste pas d’une identité de développeur Apple. Essayez d’abord d’ouvrir normalement le `.pkg` ou l’application. Si macOS signale un développeur non vérifié et propose une exception, choisissez **Réglages Système → Confidentialité et sécurité → Ouvrir quand même**, puis **Ouvrir**, uniquement après vérification de l’origine et de l’empreinte du fichier. Un Mac administré peut l’interdire. Voir [Apple](https://support.apple.com/fr-fr/102445).

Autorisation d’installation, autorisation ultérieure du composant et exception pour le développeur sont des confirmations distinctes. Si l’accès au **Réseau local** est demandé pour MultiRoute/Java, autorisez-le pour atteindre vos passerelles LAN. Accordez l’accès au dossier de téléchargement choisi. Un refus ne signifie pas qu’il faut exécuter toute l’application Java en root.

L’installateur ne désactive pas Gatekeeper/SIP, ne modifie pas les routes système, ne crée pas d’exception globale `sudoers` et ne supprime pas globalement la quarantaine. Une alerte de logiciel malveillant ou de fichier endommagé n’est pas le dialogue normal de développeur inconnu : arrêtez-vous et vérifiez le fichier. Le texte dépend de macOS et des règles de l’organisation.

## Premier téléchargement

1. Choisissez **Français** dans le sélecteur **Language**, en haut de la fenêtre.
2. Ouvrez les paramètres réseau. Modifiez ou ajoutez les passerelles et choisissez une IP virtuelle libre. L’aide visible **(?)** explique son utilité. Les infobulles apparaissent environ 250 ms après le survol d’un champ ou d’un bouton.
3. Vérifiez et appliquez l’adresse virtuelle, puis vérifiez les passerelles. Sélectionnez-en au moins une.
4. Collez l’URL dans le long champ de lien avec **Ctrl+V**, **Cmd+V** sur macOS, ou le bouton de collage. Les liens ne sont pas limités à 128 caractères.
5. Choisissez le dossier de destination via le dialogue du système. Un nouveau profil utilise le dossier `Downloads` de votre compte.
6. Démarrez avec le bouton situé juste à côté du lien. Pour une source contenant plusieurs fichiers, confirmez d’abord votre sélection.
7. Consultez la vitesse totale, le temps écoulé et restant estimé, ainsi que la vitesse et le volume de chaque passerelle. Le graphique facultatif peut être affiché.

**Le test de vitesse transfère au maximum 16 Mio ou mesure pendant cinq secondes. Ces limites concernent uniquement le test**, pas les téléchargements normaux. Sans URL directe vers un fichier HTTP, par exemple avec un magnet ou une page de dépôt, il utilise un fichier de test Selectel. Il ne mesure pas la disponibilité de toute une distribution BitTorrent. Vous pouvez définir séparément des limites de file et de vitesse pour les téléchargements réels.

Les segments HTTP font **20 Mio** par défaut, réglables de 1 à 64 Mio. Une passerelle prend de nouveaux segments dès qu’elle est libre. Après **60 secondes sans nouvelles données**, la transmission bloquée est fermée et son segment redevient disponible pour une autre passerelle choisie ; deux passerelles n’écrivent pas simultanément le même segment. Avec BitTorrent, la taille des pièces vient des métadonnées de la source.

Vous pouvez ajouter ou retirer des passerelles pendant un téléchargement. Une nouvelle passerelle doit être incluse dans un téléchargement existant via sa sélection de passerelles ; elle n’est pas ajoutée automatiquement à tous les travaux. Chaque travail inachevé doit conserver au moins une passerelle. Les segments déjà vérifiés sont conservés. Retirer un profil ne supprime pas les fichiers téléchargés.

Sélectionnez plusieurs lignes de téléchargement avec **Ctrl+clic** sous Windows/Linux ou **Cmd+clic** sous macOS ; **Maj+clic (Shift)** sélectionne une plage. Lorsque le tableau a le focus, **Ctrl+A** ou **Cmd+A** sélectionne toutes les lignes visibles selon la recherche et le filtre d’état. L’action de suppression s’applique à toute la sélection avec une seule confirmation. L’option supprimant aussi les fichiers terminés et les parties téléchargées est **décochée par défaut** : vous pouvez retirer uniquement les entrées de la liste et conserver les données. Cochez-la seulement pour supprimer les fichiers de ces travaux ; les autres fichiers et le dossier de destination sont conservés. Le résultat indique le nombre d’entrées supprimées. Les travaux dont la suppression a échoué restent dans la liste avec un récapitulatif des erreurs afin de les examiner et de réessayer ; les suppressions réussies ne sont pas répétées. Supprimer des travaux ou des fichiers n’efface pas l’historique permanent des téléchargements.

## File d’attente et plusieurs liens

Les paramètres de file et de limites définissent le nombre de travaux actifs simultanément : **deux par défaut**. Vous pouvez régler les priorités, une limite globale, les limites de chaque passerelle et un horaire suivant l’horloge locale de l’ordinateur. Une limite de vitesse de `0` signifie aucune limitation. Les limites comptent les données utiles, sans les en-têtes réseau. Un travail mis en pause manuellement n’est pas simplement redémarré par la file.

L’import de liens accepte plusieurs lignes, des fichiers TXT et des URL ou `.torrent` déposés dans l’interface. L’aperçu permet un choix explicite ; jusqu’à 1000 liens par import sont traités par lots API de 128 au maximum. Recherche, filtre d’état et tri facilitent le choix d’anciens travaux. Le temps restant est une estimation basée sur la vitesse actuelle et n’inclut pas la vérification ou l’assemblage final.

## Sources prises en charge et intégrité

| Source | Comportement |
| --- | --- |
| HTTP/HTTPS | Répartition via des requêtes Range avec réponse 206, lorsque la source permet un transfert segmenté sûr. Les certificats TLS et les noms d’hôte sont vérifiés. |
| HTTP sans Range | Un flux complet par **une seule** passerelle choisie, y compris sans `Content-Length`. En cas d’interruption ou de changement de chemin, le flux reprend depuis l’octet 0. |
| FTP/FTPS | FTP passif ou FTPS explicite, avec TLS sur les connexions de commande et de données. La segmentation sûre nécessite une SHA-256 ou SHA-512 fiable ; sinon, le fichier est transféré entièrement par une seule passerelle. |
| SFTP | SSH/SFTP avec **empreinte SHA-256 de la clé d’hôte**, préalablement vérifiée par un moyen indépendant. Une clé différente est rejetée. Une SHA-256 ou SHA-512 de fichier fiable permet la segmentation sûre. |
| WebDAV | Liste et sélection de fichiers en HTTPS ; adresse source `webdavs://…` suivant le schéma API. |
| Metalink | `.meta4` avec sélection de fichiers, miroirs et empreintes publiées des fichiers/pièces. Les miroirs ne sont utilisés comme le même fichier qu’avec une SHA-256 fiable ; une taille identique ne suffit pas. |
| Hugging Face | Liens de dépôt, `tree`, `blob` et `resolve`, choix explicite des fichiers, filtre GGUF, métadonnées LFS/Xet et transfert HTTP. Python et `hf_xet` ne sont pas nécessaires. Les sources privées exigent un accès explicitement configuré et l’autorisation du serveur. |
| BitTorrent | `.torrent` local, URL directe de torrent et magnet ; vérification des métadonnées/pièces, choix des fichiers, état des pairs/sources/trackers et priorités des fichiers. Les échanges des pairs, trackers, DHT et webseeds passent par les passerelles choisies. |

Le partage BitTorrent après téléchargement est **désactivé par défaut**. Vous pouvez l’activer avec des limites de durée et de ratio ; il utilise les passerelles sélectionnées. Un magnet nécessite des participants joignables pour obtenir ses métadonnées ; le lien seul ne garantit pas le téléchargement. Les torrents SSL exigeant un certificat client distinct et les pairs IPv6/LAN ne sont pas pris en charge par cette version.

L’application recherche notamment une SHA-256 publiée dans les métadonnées LFS de Hugging Face, `Repr-Digest`, `filename.sha256` et `SHA256SUMS`. L’empreinte d’un corps de réponse HTTP partiel n’est pas présentée comme celle du fichier complet. Vous pouvez aussi saisir manuellement une SHA-256 complète obtenue d’une source fiable.

Après assemblage, la SHA-256 du résultat est calculée. Si elle ne correspond pas à la référence connue, le résultat n’est pas publié comme fichier final vérifié. **Toutes les sources ne publient pas de référence** : en son absence, la SHA-256 affichée est une valeur calculée localement et ne signifie pas une correspondance confirmée avec le serveur. BitTorrent vérifie les empreintes des pièces contenues dans ses métadonnées ; une SHA-256 locale du fichier entier est une vérification distincte. La reprise HTTP sûre exige une identité de source appropriée, par exemple un ETag fort ou une SHA-256 complète fiable ; les sources inadéquates peuvent être rejetées.

Sous **Source access**, vous pouvez saisir explicitement des accès Basic/Bearer, cookies, en-têtes autorisés et identifiants FTP/SFTP. Ils sont liés à l’origine exacte et ne sont pas transmis à un autre serveur lors d’une redirection. Le magasin local AES-GCM est limité au compte utilisateur. Les cookies du navigateur et les clés d’autres applications ne sont pas lus automatiquement. Ces accès facultatifs concernent les sources ; ce ne sont pas des jetons de passerelle.

<a name="multiroute-api"></a>

## API locale pour d’autres applications

Adresse de base : **`http://127.0.0.1:8765/api/v1`**. L’application doit être lancée. L’API utilise JSON/UTF-8 et ne demande pas de jeton de passerelle ou d’API. Elle est limitée au même ordinateur et ne fournit pas d’authentification distante. Les requêtes provenant d’une autre origine de navigateur sont rejetées ; une intégration dans un navigateur nécessite un intermédiaire serveur local.

Les clés JSON et les états restent identiques quelle que soit la langue. Les passerelles cochées dans la fenêtre **ne sont pas automatiquement reprises** dans une requête externe : indiquez `routes` explicitement.

Enregistrez cet exemple dans `job.json`, puis remplacez l’URL et le dossier :

```json
{
  "url": "https://example.org/large-file.bin",
  "directory": "D:/Downloads",
  "chunk_mb": 20,
  "routes": [
    {"type": "gateway", "gateway": "192.168.1.1", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.2", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.3", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.4", "enabled": true}
  ]
}
```

Sous Linux/macOS, utilisez par exemple `"directory": "/home/your-user/Downloads"` ou `"/Users/your-user/Downloads"`. Dans un chemin JSON Windows avec des barres inverses, celles-ci doivent être doublées. Vous pouvez ajouter `expected_sha256`, une référence fiable de 64 caractères hexadécimaux.

```sh
curl --request POST --header "Content-Type: application/json" --data-binary @job.json http://127.0.0.1:8765/api/v1/jobs
curl http://127.0.0.1:8765/api/v1/health
curl http://127.0.0.1:8765/api/v1/jobs
```

Dans Windows PowerShell, utilisez explicitement **`curl.exe`** si `curl` est un alias PowerShell. Un code `201` confirme la création du travail, pas la fin du téléchargement. Conservez l’`id` renvoyé et consultez `GET /api/v1/jobs/{id}`. Un travail compatible existant peut être retourné avec `reused: true` ; il faut éventuellement le reprendre explicitement.

| Requête | Utilité |
| --- | --- |
| `GET /health` | Disponibilité et version |
| `POST /jobs` | Créer un travail |
| `POST /jobs/batch` | Plusieurs travaux, jusqu’à 128 entrées par lot |
| `POST /jobs/{id}/pause`, `POST /jobs/{id}/resume` | Mettre en pause ou reprendre |
| `PUT /jobs/{id}/routes` | Remplacer l’ensemble des passerelles de ce travail |
| `DELETE /jobs/{id}` | Supprimer le travail en gardant les fichiers |
| `DELETE /jobs/{id}?delete_files=true` | Supprimer également ses fichiers et pièces |
| `GET /network`, `POST /network` | Lire ou configurer l’IP virtuelle et les passerelles |
| `GET /queue`, `PUT /queue` | Parallélisme, limites et horaires locaux |
| `POST /sources/inspect`, `POST /sources/download` | Lister les fichiers d’une source ou télécharger ceux choisis |
| `GET /sources/auth`, `PUT /sources/auth` | Gérer les accès aux sources, explicites et facultatifs |
| `GET /events` | Flux d’événements SSE |

Tous les chemins de ce tableau sont relatifs à `/api/v1`. Les champs et erreurs complets sont décrits dans le [schéma OpenAPI](../openapi.yaml) ; le [guide API en russe](../API.ru.txt) contient d’autres exemples détaillés.

## Données, mise à jour et dépannage

### Installateurs natifs 4.3.9

| Système | Réglages, historique et journaux | Application/Java | Composant réseau |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App`, avec Java inclus | Composant x64 inclus ; Npcap installé séparément |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app`, avec Java | `/Library/PrivilegedHelperTools/MultiRouteResume/` |

<a name="zone-de-notification-et-fermeture-40"></a>

## Zone de notification et fermeture

Fermer la fenêtre principale laisse l’application et les téléchargements actifs dans la zone de notification. L’icône indique le débit total de **tous les travaux**, indépendamment de la sélection et du filtre. Son infobulle affiche le débit exact en Mbit/s ou Gbit/s (à partir de 1 Gbit/s), ainsi qu’en octets/s, jusqu’à 10 Gbit/s ; la petite icône arrondit la valeur. Choisissez **Ouvrir l’application** dans le menu de l’icône pour restaurer la fenêtre. Sous Windows/Linux, un clic sur l’icône l’ouvre également. **Quitter l’application**, dans le menu de l’icône ou la fenêtre, enregistre l’état et termine le programme. Utilisez cette commande avant une mise à jour.

Sans zone de notification compatible, la fenêtre est réduite dans la barre des tâches et les téléchargements continuent ; Quitter reste disponible. Si la zone disparaît, la fenêtre cachée se rouvre. Windows peut ranger l’icône dans les notifications masquées.

Le setup crée un raccourci sur le bureau et, sous Windows, une entrée Démarrer. macOS crée un lien Desktop vers la `.app` installée pour l’utilisateur connecté et réessaie au premier lancement graphique. Les éléments existants sont conservés. Si la confidentialité empêche la création, autorisez l’accès demandé ou ouvrez depuis Applications. Nos textes du setup sont anglais ; les dialogues de sécurité et d’autorisation du système suivent la langue du système.

## Licence

Vous pouvez installer et utiliser gratuitement l’application originale à titre personnel et dans des organisations, y compris pour des activités professionnelles. **Vendre l’application, facturer sa licence ou la redistribuer contre paiement est interdit** sans autorisation distincte. La licence spécifique n’accorde pas de droit général de modification ou redistribution et n’est pas une licence open source approuvée par l’OSI. Les composants tiers conservent leurs licences. Ceci est un résumé traduit ; le texte anglais de [LICENSE.txt](../LICENSE.txt) fait foi.

L’icône de vitesse a un fond transparent avec des chiffres et unités noirs, y compris à 10 Gbit/s. Elle indique le débit global et donne accès aux commandes de la zone de notification décrites ci-dessus.

## Vérification MD5, SHA-256 et SHA-512

Après assemblage, l’application calcule les trois sommes du fichier complet en une seule lecture. Collez une référence dans le champ principal facultatif avec Ctrl+V ou Cmd+V. Le type est reconnu automatiquement : 32 caractères hexadécimaux correspondent à MD5, 64 à SHA-256 et 128 à SHA-512. La section dépliable des sommes supplémentaires permet de saisir plusieurs références.

Les références publiées sont recherchées dans les en-têtes compatibles du condensat de la représentation complète et les fichiers annexes : `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` et `SHA512SUMS`. Le condensat d’une réponse HTTP partielle n’est pas considéré comme la somme du fichier entier. Les contrôles propres à Hugging Face LFS et Metalink sont conservés.

Toutes les références connues doivent correspondre avant de publier le fichier final. Une différence fait échouer la tâche tout en conservant les données partielles. Sans référence, la valeur est calculée localement et ne confirme pas une correspondance avec la source. MD5 détecte les altérations accidentelles ; SHA-256 ou SHA-512 offrent une vérification d’intégrité plus forte. MD5 ne remplace ni l’identité sûre du fichier ni les exigences relatives aux miroirs fiables.

La vérification des pièces torrent reste obligatoire et distincte des sommes des fichiers assemblés. Une référence manuelle du fichier entier exige de sélectionner exactement un fichier ; pour un groupe, donnez une référence propre à chaque tâche.

L’API locale accepte `expected_md5`, `expected_sha256` et `expected_sha512` dans `POST /api/v1/jobs`. Les résultats contiennent `md5`, `sha256`, `sha512` et les sources de référence dans `checksum_sources`. Les requêtes existantes avec `expected_sha256` restent compatibles.

<a name="progression-de-chaque-téléchargement-40"></a>

## Progression de chaque téléchargement

Chaque téléchargement dispose de sa propre barre dans la colonne Progression. Les colonnes occupent toute la largeur du panneau des téléchargements. La liste défile verticalement lorsque les tâches sont nombreuses ; les panneaux étroits permettent aussi un défilement horizontal sans rendre les colonnes illisibles. Lors de l’actualisation, la première tâche visible est conservée. Le tri et les filtres maintiennent chaque barre associée à son téléchargement. Si la taille totale est inconnue, le volume reçu est affiché à la place du pourcentage ; l’infobulle indique taille, état et erreurs. La ligne sélectionnée affiche toujours la progression détaillée, les temps et les vitesses des passerelles.

Le dossier de téléchargement par défaut est déterminé au démarrage pour l’utilisateur actuel : `%USERPROFILE%\Downloads` sous Windows et `~/Downloads` sous macOS/Linux. Le dossier par défaut enregistré suit le profil actuel ; un dossier choisi manuellement est conservé.

<a name="affichage-du-journal-36"></a>

## Affichage du journal

La fenêtre du journal conserve la position de lecture lors des actualisations automatiques et manuelles. En bas du journal, les nouvelles entrées restent visibles. Remonter suspend le suivi sans arrêter la journalisation. La sélection de texte à copier est conservée. Le changement de langue conserve l’événement visible ; la réouverture affiche les dernières entrées.

<a name="détails-du-téléchargement-et-rapport-des-sommes-37"></a>

## Détails du téléchargement et rapport des sommes

Le panneau inférieur des détails du téléchargement sélectionné conserve la position de lecture et la sélection de texte lors des actualisations automatiques répétées. Vous pouvez lire la fin du rapport des sommes sans que le panneau revienne au début. La sélection d’un autre téléchargement ouvre son rapport au début. La fenêtre distincte du journal conserve son comportement de défilement.

<a name="instructions-et-guide-api-38"></a>

## Instructions et guide API

Les boutons **Instructions** et **Guide API** se trouvent en haut à droite, sur une ligne distincte sous le choix de langue. Ils ouvrent le guide dans la langue sélectionnée avec le navigateur par défaut. **Guide API** mène directement à la section de l’API locale. Les liens correspondent à la version de l’application ; une connexion Internet est nécessaire pour ouvrir les pages GitHub.

<a name="interface-de-bureau-et-historique-des-téléchargements-40"></a>

## Interface de bureau et historique des téléchargements

L’interface utilise FlatLaf. Choisissez **Graphite** ou **System** dans les **Paramètres d’apparence** ; les styles supplémentaires nécessitent le droit Pro correspondant. Le choix est enregistré localement. La file conserve les barres individuelles, les colonnes sur toute la largeur, le tri et les filtres. Le résumé montre tous les travaux, les actifs, les terminés et la vitesse globale de tous les travaux, indépendamment du filtre.

Survolez une ligne active ou terminée pour afficher le **nom complet du fichier et son chemin de destination absolu**, y compris après tri ou filtrage. Le titre du téléchargement sélectionné propose la même info-bulle. Le menu contextuel **Copier le chemin complet** copie la destination.

**Ctrl+L** sous Windows/Linux ou **Cmd+L** sous macOS place le curseur dans l’URL ; **Ctrl+F** ou **Cmd+F** dans la recherche ; **Échap** efface la recherche. Le collage Ctrl+V/Cmd+V est conservé.

Le téléchargement sélectionné comporte les onglets **Détails** et **Historique**. L’historique consigne les événements avec date et heure : premier démarrage, chaque reprise et fin réussie. Aucune date n’est inventée pour les anciens travaux sans données enregistrées. Les emplacements et champs de l’API sont décrits ci-dessous.

L’application prend en charge les protocoles de téléchargement, les passerelles sélectionnables, l’IP virtuelle, les tests de vitesse, l’importation groupée, l’accès aux sources, MD5/SHA-256/SHA-512, la zone de notification, l’API locale et le défilement lisible du journal et des détails. L’interface ne modifie pas les routes du système.

### Stockage de l’historique et API

Le dossier des données contient `logs/downloads-history.jsonl` (UTF-8, un objet JSON par ligne) et `logs/downloads-history.log` (entrées lisibles datées). Ils sont complétés après les redémarrages et conservés lors de la suppression d’un travail ou de ses fichiers. Le journal texte conserve la langue au moment de l’écriture ; l’interface affiche les événements dans la langue actuelle. La fenêtre ne lit que les derniers événements, jusqu’à 1 000 / 2 Mio ; le journal complet reste sur disque et grandit avec l’utilisation. Archivez-le séparément au besoin. Il contient les noms et chemins de destination complets, sans URL source, mots de passe ni jetons. Vérifiez les chemins avant de le partager.

Windows natif : `%LOCALAPPDATA%\MultiRoute Downloader\data\logs\`. macOS natif : `~/Library/Application Support/MultiRoute Downloader/data/logs/`. Linux et les profils portables utilisent le dossier indiqué plus haut ; `--data-dir` permet d’en choisir un autre.

`GET /api/v1/jobs` et la réponse d’un travail exposent `lifecycle_history`. Chaque événement comporte `event_id`, `job_id`, `kind`, `time`, `filename`, `path` ; les torrents peuvent ajouter `paths`, les pauses `reason`. Types : `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Les dates utilisent ISO 8601 UTC, par exemple `2026-10-18T12:34:56.123Z` ; le journal lisible indique la date, l’heure locale et le décalage UTC. Les champs facultatifs `first_started_at`, `last_resumed_at`, `completed_at` résument les événements observés. La mise en file ne constitue pas encore un démarrage. La fin est enregistrée après vérification et publication. `recovered` signifie qu’un ancien fichier publié est vérifié maintenant, sans prétendre connaître son ancienne date de fin. Aucune date passée n’est inventée. Les opérations existantes restent disponibles ; aucun endpoint séparé n’est nécessaire.

La fenêtre générale **Journal** sépare **Événements de l’application** et **Historique des téléchargements**. L’historique affiche les événements datés des différents travaux, même supprimés ; l’historique du téléchargement sélectionné ne concerne que celui-ci.

## Profils enregistrés de l’application

Les profils utilisent les chemins de données indiqués ci-dessus. Le dossier choisi contient les paramètres, l’historique des téléchargements et les parties enregistrées ; conservez ce dossier et les fichiers téléchargés lors d’une mise à jour. L’application installée utilise le dossier protégé du composant réseau `MultiRouteResume` et des identifiants internes de l’application. Ouvrez-la depuis le raccourci ou la `.app` MultiRoute Downloader.

<a name="sites-enregistrés-envois-et-aperçu-des-archives-40"></a>

## Sites enregistrés, envois et aperçu des archives

Ouvrez **Profils de sites…**, créez/modifiez un profil et choisissez-le dans **Téléchargements** ou **Envois**. Passerelles et IP virtuelle sont dans **Paramètres réseau**. Dans **Envois**, saisissez l’**URL du dossier distant**, puis **Choisir les fichiers et envoyer…** pour choisir un ou plusieurs fichiers locaux avec le sélecteur système. L’interface ajoute les noms; API `target_url` désigne au contraire un fichier distant complet. Pour l’aperçu, collez l’URL de l’archive dans le champ principal puis cliquez sur **Contenu de l’archive…**; aucun téléchargement ne démarre.

Un profil contient nom, URL de base, dossier local, dossier distant et passerelles. Il accepte accès anonyme, HTTP Basic/Bearer/Cookie/en-têtes personnalisés, FTP et SFTP avec empreinte SHA-256 du serveur préalablement vérifiée. Plusieurs comptes du même serveur restent isolés. Les dossiers/passerelles explicites remplacent les valeurs par défaut; les URL relatives utilisent la base. Les secrets sont chiffrés dans `site-profiles/vault.bin` avec la clé locale `site-profiles/key.bin` et des droits réservés au propriétaire. Cela ne protège pas contre quelqu’un contrôlant votre compte et sa clé. L’API ne renvoie pas les secrets. Des champs vides les conservent uniquement si origine et type restent identiques; `replace_credentials:true` les remplace. Une redirection HTTP vers une autre origine ne reçoit pas les identifiants. Supprimer le profil conserve tâches et fichiers locaux/distants; une tâche conserve sa liaison au profil initial et ne peut changer de compte; recréez-la avec un profil valide si le profil initial a été supprimé.

L’envoi et l’exploration distante prennent en charge **FTP, FTPS explicite et SFTP** via les passerelles choisies. Sélectionnez un fichier local ordinaire et une URL de fichier distant complète (`target_url`). Un fichier temporaire permet la reprise après comparaison du préfixe existant. Le succès exige une relecture distante complète et SHA-256: droits de **lecture, écriture et renommage** requis. Cette lecture consomme du trafic. Une somme différente ou une lecture refusée signifie un échec. Chaque fichier utilise une passerelle à la fois; les fichiers indépendants sont répartis et envoyés en parallèle. Une autre passerelle peut prendre le relais après le délai d’inactivité. Un même envoi n’est pas découpé entre passerelles. Pause/reprise et progression persistent. Supprimer la tâche conserve le fichier local et les fichiers distants définitifs/temporaires.

L’écrasement est désactivé par défaut. FTP/SFTP ne garantissent pas partout un renommage atomique uniquement si la cible est absente; un autre client peut modifier cette cible pendant la publication. Coordonnez les destinations partagées. Lister un dossier n’envoie et ne supprime rien.

L’aperçu liste noms, chemins, tailles, dossiers et chiffrement des **ZIP, TAR, 7Z, RAR4 et RAR5**, sans extraction ni exécution. Il exige une passerelle, des plages d’octets et un ETag ou Last-Modified stable. Limites: 16 MiB de métadonnées, 10 000 entrées, 60 secondes; décodage des en-têtes 7z limité à 64 MiB. La liste peut être partielle et ne vérifie pas l’intégrité du contenu. Choisissez le jeu de caractères original si les noms sont illisibles. Les en-têtes 7z chiffrés compatibles peuvent utiliser un mot de passe non conservé. Les en-têtes RAR chiffrés sont signalés non pris en charge/mot de passe nécessaire; les volumes multiples peuvent être incomplets.

L’API accepte `site_profile_id` pour téléchargements, envois, listes et aperçus. `GET /api/v1/site-profiles` renvoie `{profiles:[...]}`; `POST` ou `PUT` enregistre un objet plat et renvoie les métadonnées publiques; `DELETE /api/v1/site-profiles/{id}` supprime seulement le profil. `POST /api/v1/uploads` reçoit `local_path`, `target_url`, `routes`, éventuellement `site_profile_id`, `overwrite:false`; réponse HTTP 201. `GET /api/v1/uploads` renvoie `{uploads:[...]}`. GET/DELETE individuels et POST `/{id}/pause` / `/{id}/resume` sont disponibles. `POST /api/v1/remote/list`: `url` et `route`/`routes`; `POST /api/v1/archives/preview` accepte aussi `password` et `charset`. Voir [openapi.yaml](../openapi.yaml). Les manifestes UTF-8 de `data/uploads/` conservent l’ID du profil, pas le mot de passe. Un aperçu ne crée pas de téléchargement.

## Planification et priorité des téléchargements

Les contrôles de file/planification règlent `max_active_jobs` (1–32), limites globales/par passerelle et plage quotidienne (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm). Le fuseau local est exposé comme `schedule_timezone`; la plage peut franchir minuit et des heures égales signifient toute la journée. Hors plage, les téléchargements retournent en file; une pause manuelle reste une pause. `priority` de -100 à 100 classe les tâches en attente, valeur haute d’abord puis date de création; cela n’interrompt pas une tâche active. `start_after_ms` est le démarrage au plus tôt en millisecondes Unix, 0 supprime le délai. Les réglages persistent et concernent les téléchargements, pas la file d’envois indépendante. Gardez application et ordinateur actifs; aucun réveil du système n’est programmé. API: `GET`/`PUT`/`POST /api/v1/queue` et `PUT`/`POST /api/v1/jobs/{id}/options`.

<a name="dnssec-validation"></a>

## Validation DNSSEC locale

Dans **Réseau → Résolution DNS → Validation DNSSEC**, choisissez **Valider les domaines signés** (défaut DoH/DoT), **Exiger des réponses DNSSEC sécurisées** ou **DNSSEC désactivé**. Java valide localement avec dnsjava et les ancres racines fournies. Le seul indicateur AD du fournisseur ne constitue pas une preuve. DoH/DoT chiffre le transport ; DNSSEC authentifie séparément les données DNS.

**Valider** accepte les réponses sécurisées et non sécurisées ; **Exiger** uniquement les sécurisées. Une réponse non sécurisée peut indiquer une délégation non signée authentifiée ou des algorithmes non pris en charge : lisez le motif. Les résultats invalides et indéterminés sont rejetés lorsque la validation est active. Le DNS système désactive la validation locale. Revenir à DoH/DoT restaure la dernière politique ; changer de fournisseur la conserve. Appliquer enregistre ; Tester DNS utilise le brouillon.

Chaque requête distingue `secure`, `insecure`, `bogus`, `indeterminate` et `not_checked`. Un résultat absent est indisponible. Une connexion réussie ne prouve pas un verdict DNSSEC sécurisé. Expiration, clés/preuves absentes, échecs et délais ont des motifs distincts. L’horloge doit être exacte. Les requêtes auxiliaires utilisent la même passerelle et le délai global. DNSSEC ne masque pas les noms et ne vérifie pas le contenu téléchargé.

API : `dnssec` accepte `off`, `validate`, `require` ; le mode Système seulement `off`. Champs en lecture seule : `dnssec_validation`, `dnssec_trust_anchors` et, par requête, `dnssec_status`, `dnssec_reason_code`, `dnssec_reason`, `dnssec_validated_ttl`, `dnssec_aux_queries`. Ne les renvoyez pas à l’enregistrement. Consultez API/OpenAPI ci-dessus et les [DNSSEC](#dnssec-validation).

## Intégration Chrome et Edge

Ouvrez l’intégration **Chrome / Edge** dans l’application, activez l’envoi de liens et ouvrez le dossier d’extension fourni avec l’installateur natif. Allez sur `chrome://extensions` ou `edge://extensions`, activez le mode développeur puis **Charger l’extension non empaquetée** avec ce dossier. Conservez le dossier choisi à son emplacement.

MultiRoute doit fonctionner, y compris dans la zone de notification. Envoyez un lien par son menu contextuel ou la fenêtre de l’extension. Dossier, passerelles et profil courants sont utilisés; les choix de dépôt/torrent restent affichés. Le port est 8765; adaptez-le si l’API utilise un autre port. Seule l’URL est envoyée, sans cookies ni mots de passe; les autres téléchargements du navigateur ne sont pas interceptés. Configurez l’accès à la source dans l’application. Si le navigateur demande l’accès local, ne l’accordez qu’à cette extension choisie.

Le pont est désactivé par défaut et n’accepte que `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` sur `/api/v1/browser/status` et `/api/v1/browser/download`; les autres règles API restent inchangées. HTTP 202 confirme la remise au bureau, pas la fin d’un fichier. Un serveur headless seul ne peut ouvrir la sélection graphique. Supprimez l’extension ou désactivez l’intégration pour arrêter les envois de liens.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
