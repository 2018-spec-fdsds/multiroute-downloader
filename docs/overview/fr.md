# MultiRoute Downloader
<!-- product-languages:start -->
<p align="center" dir="ltr">
<a href="../../README.md">English</a> · <a href="../../README.ru.md">Русский</a> · <a href="de.md">Deutsch</a> · <a href="es.md">Español</a> · <strong>Français</strong> · <a href="zh.md">中文</a><br />
<a href="ko.md">한국어</a> · <a href="ar.md">العربية</a> · <a href="it.md">Italiano</a> · <a href="ja.md">日本語</a> · <a href="hi.md">हिन्दी</a> · <a href="vi.md">Tiếng Việt</a>
</p>
<!-- product-languages:end -->

<p align="center"><img src="../images/overview/fr/hero.svg" width="1320" alt="MultiRoute Downloader — Plus de connexions. Un seul téléchargement." /></p>

<p align="center"><strong>Téléchargez de gros fichiers avec les connexions dont vous disposez déjà.</strong><br />Combinez des passerelles haut débit et mobiles indépendantes pour les gros téléchargements, conservez la progression des transferts pouvant être repris et vérifiez le fichier terminé.</p>

<p align="center">
  <a href="https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9"><strong>Télécharger 4.3.9</strong></a> ·
  <a href="../../downloads/README.md">Téléchargements actuels</a> ·
  <a href="../fr.md">Guide d’utilisation</a> ·
  <a href="../../API.ru.txt">Référence de l’API</a> ·
  <a href="../../README.ru.md">Русский</a>
</p>

## Pourquoi MultiRoute existe

L’idée est née d’un problème familier des réseaux domestiques : une connexion haut débit filaire, un routeur mobile et une connexion Internet de secours étaient disponibles, mais un gros téléchargement n’utilisait toujours que la passerelle par défaut de l’ordinateur. Lorsque cette connexion ralentissait ou tombait en panne, les autres accès ne pouvaient pas aider.

MultiRoute réunit ces accès indépendants dans un espace de téléchargement unique. Les passerelles disponibles prennent des segments de fichiers dans une file partagée ; les plus rapides prennent davantage de travail, et les segments inachevés d’une connexion bloquée peuvent être réaffectés. L’application conserve la progression lorsque la reprise est prise en charge, assemble le résultat et le compare à une somme de contrôle de référence disponible.

- **Plus de bande passante pour un fichier.** Des accès Internet indépendants récupèrent différentes parties en parallèle. Leur capacité cumulée peut raccourcir un gros téléchargement si la source et le réseau local peuvent suivre.
- **Une progression malgré les interruptions.** Les parties terminées sont sauvegardées ; les passerelles fonctionnelles continuent de télécharger et les segments bloqués peuvent rejoindre à nouveau le lot de travail commun.
- **Un contrôle visible.** Suivez le débit total et celui de chaque passerelle, changez les passerelles sélectionnées pendant un transfert et observez la contribution de chaque connexion.
- **Des flux de travail reproductibles.** Réutilisez les profils de sites, mettez des listes de liens en file d’attente ou confiez les transferts à l’API locale depuis des scripts et des outils de développement.

Les gains réels dépendent de l’indépendance des accès, des limites de la source et du réseau local. Des routeurs partageant une même connexion en amont ne multiplient pas sa bande passante.

![Passerelles partagées, progression fiable, vérification, DNS protégé et commandes de transfert](../images/overview/fr/features.svg)

### Utile aussi avec une seule connexion

Profitez de la reprise lorsqu’elle est prise en charge, de la comparaison avec des sommes de contrôle publiées, du DNS protégé, des files par lots, des horaires, des profils de sites, des envois et de l’API locale. Commencez avec une passerelle, puis ajoutez-en d’autres lorsqu’elles sont disponibles.

## Téléchargement et installation

**Version actuelle : 4.3.9.** Les installateurs natifs incluent leur propre environnement Java 21 et le composant réseau de la plateforme. Ouvrez l’application depuis son icône habituelle.

| Votre ordinateur | Installateur en un seul fichier |
| --- | --- |
| **Windows 11 · x64** | [Windows Setup EXE](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe) |
| **Mac · Apple Silicon · macOS 26+** | [Apple Silicon PKG](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg) |
| **Mac · Intel · macOS 15+** | [Intel PKG](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg) |

[Intégration Chrome / Edge](../fr.md#chrome-and-edge-integration) · [Sommes de contrôle et rapport](../../downloads/v4.3.9/README.md) · [Détails des téléchargements](../../downloads/README.md)

L’installateur Windows est en anglais et crée un raccourci sur le bureau. Npcap est vérifié séparément et son installateur officiel est proposé si nécessaire ; macOS utilise libpcap du système. Consultez le guide dans votre langue pour l’installation et les confirmations de sécurité. Les installateurs natifs publics couvrent Windows et macOS ; aucun installateur Linux 4.x n’est fourni ici.

**Free / Pro dans 4.3.9 :** Free permet d’utiliser jusqu’à **2 passerelles**, **3 téléchargements simultanés** et de créer **3 profils de sites**, avec les apparences Graphite/Système. Les fonctions Pro correspondantes autorisent jusqu’à **8 passerelles**, **32 téléchargements simultanés**, des profils supplémentaires, les priorités, l’historique et les rapports des passerelles, les opérations par lots, l’API externe et des styles supplémentaires. Free ne bride pas volontairement la vitesse de téléchargement. [Détail complet des fonctions](../licensing.en.md).




## Nouveautés de 4.3.9

- **Réponses des passerelles Mac avec une adresse manuelle.** La capture POSIX est corrigée pour que l’adresse manuelle de l’application et DHCP utilisent tous deux un mode recevant les réponses à sa propre MAC, comme sous Windows. Un ping système réussi vérifie la connexion de l’ordinateur, pas la réception à l’adresse virtuelle de l’application.

- Les diagnostics DHCP conservent leur texte en douze langues au lieu d’une erreur générique. L’autorisation existante reste inchangée : les contrôles normaux réutilisent le composant préparé. Les points d’accès API, commandes, routes du système et MAC physique ne changent pas. Le premier accès privilégié à Ethernet BPF peut encore demander la confirmation normale de l’administrateur.

Cela corrige une différence de mode de capture, pas toutes les causes de passerelle inaccessible. Les vérifications d’adresse libre, de sous-réseau et de bail/conflit DHCP restent actives. Les transferts Mac avec adresse manuelle restent à confirmer sur matériel ; Ethernet filaire demeure nécessaire.

### Contrôler les transferts

- **Démarrage Mac et passerelles plus fiables.** Le démarrage natif sur Mac reconnaît l’ancien chemin relatif par défaut, y compris sans barre initiale. Il utilise un profil existant vérifié ou un dossier utilisateur par défaut vide. Un emplacement personnalisé ambigu exige toujours un choix explicite.

- Après un échec ou une annulation de préparation, les autres passerelles et contrôles en arrière-plan cessent de répéter les demandes d’autorisation jusqu’à une nouvelle tentative explicite de configuration réseau. Le premier démarrage privilégié peut encore demander la confirmation normale de l’administrateur macOS pour ouvrir le périphérique BPF Ethernet.

- Le composant réseau Mac 4.3.2 tient compte d’une adresse MAC masquée par macOS tout en conservant les contrôles de protection de la carte physique. La MAC propre à l’application est conservée, sans changer les routes du système ni la MAC physique. L’icône et le nom Mac reprennent l’identité MultiRoute de Windows. Quittez l’application avant la mise à jour PKG. Le nom de l’appelant dans le dialogue réel et les transferts LAN/DHCP restent à confirmer sur matériel ; ce transport nécessite toujours Ethernet filaire.



- **Données restant à télécharger.** Le tableau comporte une colonne **Données restantes**. Elle indique les octets restant à télécharger, séparément du temps estimé, avec un tri numérique. La valeur tient compte de la progression enregistrée et en cours, ne devient jamais négative et affiche un tiret si la taille totale est inconnue.


- **Donnez une préférence aux passerelles.** Les priorités 0–1000 apparaissent près des indicateurs : les valeurs élevées favorisent les petits fichiers et reçoivent plus de segments parallèles des gros fichiers. Les autres passerelles disponibles contribuent toujours. Le droit Pro distinct `gateway_priorities` est nécessaire.

- **Comprenez les interruptions.** Ouvrez le journal individuel dans le menu contextuel, même après la fin du fichier, ou le guide d’erreurs recherchable de 25 sujets dans les paramètres. Le disque plein est signalé clairement et la destination indique l’espace libre en Go.

- **Maîtrisez la file.** Réglez les téléchargements simultanés dans la limite de la licence et les nouvelles tentatives automatiques. Copiez les noms, mettez en pause/reprenez les tâches sélectionnées compatibles et ouvrez le dossier par double-clic. Le préfixe enregistré reste visible dans la progression pendant l’attente de données après reprise.


## Vos transferts en un coup d’œil

L’espace de travail de 4.3.9 place vos fichiers au centre : colonnes distinctes pour le nom et la taille, progression individuelle, débit total et par passerelle, graphique de débit et détails des fichiers. Graphite et System sont inclus dans Free ; les styles supplémentaires, comme Rose, nécessitent la fonction Pro correspondante.

![MultiRoute Downloader 4.3.7 — file de téléchargements et débits des passerelles](../images/v4.3.7/downloads-en-ae5b55131583.png)

*Interface Windows actuelle, capturée dans un profil de démonstration isolé. Les fichiers d’exemple, la progression, les débits et les vues activées avec Pro illustrent les commandes et ne constituent pas des mesures de performance.*

<details>
<summary><strong>Paramètres réseau et historique des débits</strong></summary>

![Adresse de l’application et DNS protégé](../images/v4.3.7/network-dns-e29dfdba1007.png)

![Sélection des passerelles et tests de débit](../images/v4.3.7/network-gateways-aee0a7897f48.png)

![MultiRoute Downloader 4.3.7 — historique du débit de téléchargement](../images/v4.3.7/speed-graph-a85815544945.png)

</details>


## Choisissez votre apparence

Choisissez parmi neuf apparences : Graphite, Nordic, Streams Studio, Neon, Community, Chat, Studio, Rose et System. Graphite est l’apparence par défaut. Graphite et System sont inclus dans Free ; les autres choix nécessitent le droit Pro d’apparence correspondant. Ouvrez Paramètres → Apparence pour changer le style et les commandes arrondies. Vos téléchargements continuent pendant votre choix.

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

## Un coursier qui suit vos téléchargements

Le coursier de bureau facultatif transforme la progression des téléchargements en une charge visible. Choisissez l’homme ou la femme dans les paramètres d’apparence et réglez la taille de 40% à 100%. Le sac indique le total des volumes restants connus des téléchargements actifs et rétrécit à mesure que les octets arrivent. Le mouvement suit leur débit de téléchargement cumulé ; lorsque le débit est nul, le coursier reste immobile. Les petites charges sont portées sur l’épaule, les plus grosses sont traînées et les très grosses demandent davantage d’effort.

Utilisez le bouton Avatar pour afficher ou masquer le coursier. Faites-le glisser pour le déplacer, double-cliquez avec le bouton gauche de la souris pour afficher l’application ou faites un clic droit pour ses options. C’est un compagnon visuel : les fichiers restent dans les dossiers choisis.

À la fin d’un téléchargement, le coursier dépose la charge livrée et disparaît progressivement. Si des téléchargements continuent, il revient avec les données restantes.

### Petit volume restant · 768 MB

<table>
<tr>
<td width="50%" valign="top"><strong>Coursier</strong><br /><a href="../images/v4.3.7/avatar-man-shoulder-eeeb879d0240.png"><img src="../images/v4.3.7/avatar-man-shoulder-eeeb879d0240.png" width="420" alt="Coursier" /></a></td>
<td width="50%" valign="top"><strong>Coursière</strong><br /><a href="../images/v4.3.7/avatar-woman-shoulder-5682615d5576.png"><img src="../images/v4.3.7/avatar-woman-shoulder-5682615d5576.png" width="420" alt="Coursière" /></a></td>
</tr>
</table>

### Volume restant moyen · 7 GB

<table>
<tr>
<td width="50%" valign="top"><strong>Coursier</strong><br /><a href="../images/v4.3.7/avatar-man-drag-a3dc0a3ed22c.png"><img src="../images/v4.3.7/avatar-man-drag-a3dc0a3ed22c.png" width="420" alt="Coursier" /></a></td>
<td width="50%" valign="top"><strong>Coursière</strong><br /><a href="../images/v4.3.7/avatar-woman-drag-48d8ee0fa4d1.png"><img src="../images/v4.3.7/avatar-woman-drag-48d8ee0fa4d1.png" width="420" alt="Coursière" /></a></td>
</tr>
</table>

### Grand volume restant · 18 GB

<table>
<tr>
<td width="50%" valign="top"><strong>Coursier</strong><br /><a href="../images/v4.3.7/avatar-man-heavy-649f17608748.png"><img src="../images/v4.3.7/avatar-man-heavy-649f17608748.png" width="420" alt="Coursier" /></a></td>
<td width="50%" valign="top"><strong>Coursière</strong><br /><a href="../images/v4.3.7/avatar-woman-heavy-d445fdb37867.png"><img src="../images/v4.3.7/avatar-woman-heavy-d445fdb37867.png" width="420" alt="Coursière" /></a></td>
</tr>
</table>

## Les paramètres en un coup d’œil

Définissez le dossier de téléchargement, les profils de sites et la langue, puis réglez l’apparence, la taille du coursier et les sons de fin. Les paramètres réseau regroupent l’adresse de l’application, la MAC, les passerelles et le DNS. Les sons facultatifs permettent de distinguer la fin d’un fichier de celle de toute la file, avec des mélodies intégrées ou vos propres extraits MP3/WAV de 10 secondes au maximum.

<table>
<tr>
<td width="50%" valign="top"><strong>Paramètres de l’application</strong><br /><a href="../images/v4.3.7/settings-8abcb12a782d.png"><img src="../images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Paramètres de l’application" /></a></td>
<td width="50%" valign="top"><strong>Apparence, coursier et sons</strong><br /><a href="../images/v4.3.7/appearance-cebf121d5de7.png"><img src="../images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Apparence, coursier et sons" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Adresse de l’application et DNS protégé</strong><br /><a href="../images/v4.3.7/network-dns-e29dfdba1007.png"><img src="../images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Adresse de l’application et DNS protégé" /></a></td>
<td width="50%" valign="top"><strong>Sélection des passerelles et tests de débit</strong><br /><a href="../images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="../images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Sélection des passerelles et tests de débit" /></a></td>
</tr>
</table>

<details>
<summary><strong>Espaces de téléchargement et d’envoi</strong></summary>

<table>
<tr>
<td width="50%" valign="top"><strong>Créer un téléchargement</strong><br /><a href="../images/v4.3.7/new-download-dcb720c7769d.png"><img src="../images/v4.3.7/new-download-dcb720c7769d.png" width="420" alt="Créer un téléchargement" /></a></td>
<td width="50%" valign="top"><strong>Espace d’envoi</strong><br /><a href="../images/v4.3.7/uploads-31c63833de2e.png"><img src="../images/v4.3.7/uploads-31c63833de2e.png" width="420" alt="Espace d’envoi" /></a></td>
</tr>
</table>

</details>

## Des besoins quotidiens à un contrôle concret

| Vous souhaitez… | MultiRoute propose |
| --- | --- |
| **Utiliser les connexions disponibles** | Une file partagée de segments HTTP, 1 à 8 profils de passerelles modifiables et la sélection en direct d’une, de plusieurs ou de toutes les passerelles. |
| **Conserver et vérifier de gros fichiers** | Reprise lorsqu’elle est prise en charge, parties sauvegardées, assemblage automatique et comparaison avec les références de l’éditeur au moyen de MD5, SHA-256 ou SHA-512. |
| **Protéger les requêtes DNS** | DoH/DoT chiffré, fournisseurs prédéfinis ou personnalisés et DNSSEC local, sans modifier les paramètres DNS ni les routes du système d’exploitation. |
| **Organiser plusieurs téléchargements** | Listes de liens, priorités, plages horaires quotidiennes, démarrages différés, limites de débit et de tâches, statistiques par passerelle, graphiques et historique daté. |
| **Réutiliser vos sites habituels** | Profils enregistrés, navigation dans les dossiers distants, envois FTP/FTPS/SFTP et aperçu des archives. |
| **Adapter l’application à votre façon de travailler** | Transmission de liens depuis le navigateur, API locale, douze langues, thèmes clair/sombre/système, collage au clavier, boîtes de dialogue système pour les dossiers, indicateur de débit dans la zone de notification et lancement facultatif à l’ouverture de session. |

| Transfert | Options prises en charge |
| --- | --- |
| **Téléchargement** | HTTP/HTTPS, FTP, FTPS explicite, SFTP, WebDAV, Metalink, BitTorrent et liens magnet ; liens Hugging Face et sélection de fichiers dans les dépôts. |
| **Envoi** | FTP, FTPS explicite et SFTP, avec profils de sites et vérification SHA-256 par relecture. |
| **Aperçu des archives** | Liste du contenu des archives ZIP, TAR, 7Z et RAR avant le téléchargement complet, lorsque les plages d’octets et les en-têtes pris en charge sont disponibles. |

### Comprendre ce qui a été vérifié

Les contrôles de fichiers comparent les données assemblées à une référence fiable lorsqu’elle est disponible ; un hachage local ne suffit pas à établir l’identité de l’éditeur. DoH/DoT chiffre le transport DNS ; DNSSEC valide séparément les données DNS signées. Ces contrôles ne prouvent pas que le contenu téléchargé est inoffensif.

## Une API pour vos applications et votre développement assisté par IA

Les scripts, applications tierces et agents de programmation par IA peuvent demander à MultiRoute de récupérer les poids de modèles, jeux de données, bibliothèques, ressources et artefacts de projet nécessaires au développement. L’appelant reçoit la progression, l’état de fin et les résultats des sommes de contrôle, et peut piloter la file pendant que MultiRoute gère les passerelles, les nouvelles tentatives et la reprise lorsqu’elle est prise en charge.

L’API fonctionne localement à l’adresse **`http://127.0.0.1:8765/api/v1`**. Par exemple, envoyez ce JSON à **`POST /api/v1/jobs`** avec `Content-Type: application/json` :

```json
{
  "url": "https://example.org/model.bin",
  "directory": "C:/Users/YOUR_USER/Downloads",
  "routes": [
    {"type": "gateway", "gateway": "192.168.1.1", "enabled": true}
  ]
}
```

Le pilotage général par l’API externe nécessite la fonction Pro correspondante ; la création par lots et les limites de passerelles sont contrôlées séparément. Remplacez l’URL, le dossier et la passerelle par vos valeurs ; configurez d’abord le réseau. Indiquez explicitement les routes : les cases cochées dans l’interface ne sont pas recopiées dans les requêtes API. `201` signifie que la tâche a été créée ; conservez `id` et consultez `GET /jobs/{id}` pour la progression, l’état `complete`/`failed` et le détail des sommes de contrôle. Cet exemple est illustratif et ne soumet aucun téléchargement.

[Référence de l’API](../../API.ru.txt) · [Schéma OpenAPI](../../openapi.yaml) · [Guide d’intégration](../fr.md#multiroute-api)

## Démarrer en quatre étapes

1. **Installez** le paquet de votre plateforme et suivez son guide.
2. **Choisissez les paramètres réseau :** Ethernet filaire, passerelles sur le même réseau local IPv4 et adresse virtuelle inutilisée pour l’application. Les routes du système d’exploitation utilisées par les autres applications restent inchangées.
3. **Sélectionnez et testez les passerelles.** Les valeurs par défaut sont `192.168.1.1`–`192.168.1.4`, avec l’adresse virtuelle `192.168.1.70` ; adaptez-les à votre réseau.
4. **Collez un lien ou une liste, choisissez un dossier et démarrez.** Suivez la contribution de chaque passerelle et la vérification finale.

<details>
<summary><strong>Prérequis et détails des transferts</strong></summary>

- Le transport par plusieurs passerelles prend en charge l’IPv4 public sur Ethernet filaire ; les adaptateurs uniquement Wi-Fi/VPN, les sources uniquement IPv6 et les pairs torrent du réseau local sont hors du périmètre de ce mode.
- La reprise par segments nécessite la prise en charge des plages et une identité stable de la source. Les segments HTTP font 20 MiB par défaut ; un travail bloqué peut être réaffecté après 60 secondes sans données, une fois son dispositif d’écriture fermé. Un transfert HTTP non reprenable recommence après une interruption. Les morceaux torrent utilisent les hachages des métadonnées.
- Un fichier envoyé utilise une passerelle ; plusieurs fichiers peuvent utiliser différentes passerelles. La vérification par relecture consomme de la bande passante et nécessite des droits distants de lecture, d’écriture et de renommage. L’aperçu des archives comporte des limites de métadonnées et d’en-têtes.
- Les horaires nécessitent que l’application soit ouverte et l’ordinateur éveillé ; ils ne le réveillent pas et ne planifient pas la file d’envoi distincte.
- Le DNS système suit la politique du système d’exploitation/VPN. Les ancres de confiance racine DNSSEC sont mises à jour avec les versions. Le DNS chiffré ne chiffre pas lui-même le contenu des fichiers HTTP/FTP.
- Les installateurs ne sont pas signés ; les paquets macOS ne sont ni signés avec Developer ID ni notariés. Suivez les confirmations habituelles des guides sans désactiver les protections du système. Chaque version comprend son rapport de vérification et ses limites restantes.

</details>

## Guides en douze langues

[English](../en.md) · [Русский](../ru.md) · [Deutsch](../de.md) · [Español](../es.md) · [Français](../fr.md) · [中文](../zh.md)<br />
[한국어](../ko.md) · [العربية](../ar.md) · [Italiano](../it.md) · [日本語](../ja.md) · [हिन्दी](../hi.md) · [Tiếng Việt](../vi.md)


<a name="application-virtual-ip"></a>

### Adresse de l’application et paquets actuels

L’application utilise sa propre adresse IPv4 locale et sa propre adresse MAC applicative afin que les passerelles choisies fonctionnent indépendamment de la passerelle par défaut du système d’exploitation, sans modifier les routes système ni l’adresse MAC de l’adaptateur physique. Utilisez une adresse libre dans le sous-réseau réellement utilisé. Gardez toute adresse manuelle en dehors de l’attribution automatique DHCP, ou réservez-la/excluez-la sur les serveurs DHCP du réseau local ; utilisez ensuite **Vérifier et appliquer** dans les paramètres réseau. [Choix de l’adresse et vérifications](../fr.md#application-virtual-ip).

Les paquets Windows 4.3.9 actuels ont passé un contrôle local de mise à niveau et de démarrage ; les paquets macOS ont passé les contrôles des composants natifs et de l’environnement Java sur les exécuteurs CI ciblés. Les transferts par passerelles sur un réseau LAN/DHCP avec un Mac physique doivent encore être validés sur le matériel. [Rapport de vérification](../../NATIVE-INSTALLER-TESTS.json).

## Auteur et licence

Créé par **[2018-spec-fdsds](https://github.com/2018-spec-fdsds)**. L’utilisation gratuite couvre les usages personnels ainsi que ceux des organisations et des entreprises. La vente de l’application d’origine, la facturation de sa licence ou sa redistribution payante nécessitent une autorisation écrite distincte. Aucun droit général de modification ou de redistribution n’est accordé ; les conditions des tiers restent applicables.

[Licence](../../LICENSE.txt) · [Résumé de la licence](../../LICENSING.md) · [Mentions relatives aux tiers](../../THIRD-PARTY-NOTICES.txt)

Cet espace de distribution contient les installateurs actuels de la version 4.3.9 et la documentation utilisateur.
