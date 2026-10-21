# MultiRoute Downloader 4.1.1 — Guide en français

[Accueil](../README.md) · [English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · **Français** · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

**Correctifs 4.1.1 :** après un délai UDP dépassé ou une réponse DNS tronquée, une tentative TCP utilise la même passerelle et la même IP virtuelle ; le budget DNS total est de 8 secondes. Les mises à jour automatiques ne ramènent plus les vues défilantes vers le haut. [Notes de version](releases/4.1.1.en.md).

Propriétaire du projet: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## À quoi sert l’application ?

MultiRoute Downloader est une application de bureau Java qui télécharge de gros fichiers au moyen de plusieurs passerelles Internet présentes sur le même réseau Ethernet local. Les passerelles choisies prennent des segments dans une file commune. Une passerelle plus rapide peut ainsi télécharger davantage de segments ; une passerelle lente ne bloque pas les autres. Une transmission interrompue peut reprendre lorsque le protocole et l’identité du fichier permettent une reprise sûre.

Elle est particulièrement utile lorsque plusieurs accès Internet indépendants sont disponibles, par exemple une ligne fixe filaire et un routeur mobile 4G/5G. Leurs passerelles doivent être accessibles depuis l’ordinateur sur le même réseau Ethernet local. L’application utilise simultanément toutes les passerelles sélectionnées et disponibles pour télécharger des segments de fichiers, ce qui peut augmenter le débit total. Le gain réel dépend de la capacité indépendante des connexions et des limites du serveur de téléchargement.

Pour les téléchargements segmentés, la panne d’une connexion n’empêche pas les autres passerelles de continuer. Les segments incomplets sont réattribués aux passerelles encore disponibles et les données déjà vérifiées sont conservées. L’application prend également en charge le téléchargement groupé de plusieurs fichiers : des listes d’URL et une file d’attente permettent de gérer des groupes de téléchargements, que d’autres logiciels peuvent aussi créer via l’API locale.

L’application possède une interface Swing, un sélecteur de dossier propre au système et une API HTTP locale utilisable par d’autres logiciels. Aucun navigateur n’est nécessaire pour la piloter. L’interface, les messages d’état enregistrés et les aides sont disponibles en **anglais, russe, allemand, espagnol, français, chinois, coréen, arabe, italien, japonais, hindi et vietnamien**. Un nouveau profil utilise l’anglais. Le choix de langue est conservé et appliqué immédiatement ; les noms de fichiers, les URL et les clés JSON restent identiques. Certains messages externes non enregistrés, provenant d’un serveur ou d’une bibliothèque, peuvent conserver leur langue d’origine.

## Captures d’écran

Interface de bureau réelle en anglais, rendue depuis un **profil de démonstration** isolé. Les tâches et compteurs sont des exemples, pas de vrais téléchargements ni des débits Internet mesurés.

![Téléchargements : file d’attente, progression et statistiques par passerelle](images/desktop-downloads-en.png)

<details>
<summary>Réglages réseau et importation en groupe</summary>

Quatre passerelles sélectionnées et une adresse virtuelle de l’application. La démonstration ne vérifie ni passerelles ni débits.

![Quatre passerelles sélectionnées et une adresse virtuelle de l’application. La démonstration ne vérifie ni passerelles ni débits.](images/desktop-network-en.png)

Importation en groupe : une URL par ligne, vérification avant ajout.

![Importation en groupe : une URL par ligne, vérification avant ajout.](images/desktop-bulk-import-en.png)

</details>

## Environnement nécessaire

Les nouveaux installateurs Windows/macOS décrits ci-dessous incluent Java et le composant compilé. Le choix de Java, son téléchargement privé et la compilation locale dans cette section concernent les **ZIP portables 3.0 et Linux**. Npcap reste nécessaire séparément sous Windows. Les nouveaux paquets macOS ont le minimum plus strict 26+/15+ indiqué dans leur section.

### Prérequis communs

| Élément | Prérequis |
| --- | --- |
| Java | Environnement Java complet à partir de la version 17, correspondant au processeur, avec `java.base`, `java.desktop`, `java.net.http`, `java.xml` et `jdk.httpserver`. Une JRE suffit à l’exécution ; compiler le code Java exige un JDK. |
| Processeur | Windows : x64. Linux : x86_64 ou ARM64/aarch64 avec glibc. macOS : Intel x86_64 ou Apple Silicon arm64. |
| Mémoire vive | Aucun minimum strict n’a été confirmé par les tests de publication. **4 Go ou davantage sont recommandés**, notamment pour plusieurs téléchargements volumineux ou BitTorrent. Il s’agit d’une recommandation, pas d’un minimum validé. |
| Affichage | Une session graphique est nécessaire pour la fenêtre. Une carte graphique spéciale, CUDA, Python ou un environnement d’IA ne sont pas nécessaires au téléchargement. `--headless` lance seulement l’API. |
| Disque | Sous Linux/macOS, au moins **512 Mio libres pour le déploiement** ; sous Windows, également 512 Mio pour l’installation privée de Java si elle est nécessaire. Ajoutez l’espace des fichiers téléchargés et des données temporaires. |
| Réseau | Un adaptateur Ethernet physique sur le même sous-réseau IPv4 que toutes les passerelles choisies ; chacune doit fournir un accès Internet réel. |
| Droits | Accès en écriture aux dossiers du programme, des données et des téléchargements, ainsi que les droits de capture/transmission décrits ci-dessous. |

Pour HTTP, le fichier de travail se trouve à côté du fichier final jusqu’à la vérification et à la finalisation. BitTorrent conserve également des pièces dans le dossier de données ; l’assemblage peut nécessiter plusieurs copies et des pièces débordant les limites des fichiers choisis. Prévoyez donc de l’espace **sur le disque de destination et sur celui du dossier de données**. Pour BitTorrent, un espace égal à une seule taille de fichier n’est pas garanti suffisant.

### Windows 11 x64

- **Java 17 ou ultérieur en 64 bits** est nécessaire. Les lanceurs vérifient version, architecture et modules. En l’absence d’un environnement approprié, une JRE officielle [Temurin 21](https://adoptium.net/installation/) à version fixée est téléchargée en HTTPS, vérifiée par SHA-256 et installée dans un dossier privé du programme. Le Java du système n’est pas remplacé.
- [Npcap](https://npcap.com/) est nécessaire. Le pilote et ses DLL **ne sont pas inclus dans le ZIP**. S’il manque, le lanceur télécharge son installateur officiel à version fixée et vérifie sa SHA-256 ainsi qu’une signature Authenticode valide de **Nmap Software LLC**. L’installation reste visible : vous confirmez UAC, lisez la licence Npcap et choisissez vous-même les options. Aucune installation silencieuse n’est effectuée. Une installation déjà appropriée est réutilisée.
- Npcap doit autoriser le processus concerné à accéder à l’adaptateur Ethernet. Avec l’option **AdminOnly**, l’accès et une éventuelle demande UAC dépendent des droits configurés. MultiRoute ne crée pas de règle permettant de contourner ces droits. Voir le [guide officiel de Npcap](https://npcap.com/guide/npcap-users-guide.html).
- L’architecture de Java, du composant réseau et de Npcap doit correspondre. Le composant Windows fourni est x64 ; cette version ne propose pas de paquet Windows ARM64 natif.
- Visual Studio et les SDK de développement sont inutiles pour exécuter le ZIP prêt à l’emploi. Ils servent uniquement à recompiler le composant Windows.

### Linux

- Linux avec **glibc**, sur x86_64 ou aarch64/ARM64. Aucun paquet Alpine/musl n’est fourni.
- Pour Swing : **X11 ou Wayland avec XWayland**, ainsi que les bibliothèques X11 et de polices appropriées. Sur un système sans bureau, utilisez `--headless`.
- `sudo` pour l’installation et le démarrage privilégié du composant réseau protégé. L’interface Java s’exécute sous votre compte habituel.
- La compilation locale du composant nécessite un compilateur, Binutils et les fichiers de développement de libpcap. Le script installe uniquement les paquets explicitement nécessaires via le gestionnaire de paquets ; il ne met pas à niveau toute la distribution.

**Prérequis supplémentaire pour BitTorrent/magnet :** les bibliothèques natives Linux fournies pour les deux architectures exigent **glibc 2.38 ou ultérieure**. Une installation standard d’Ubuntu 24.04 satisfait cette contrainte ABI ; Ubuntu 22.04 et Debian 12 ne la satisfont pas pour les bibliothèques torrent fournies. Le lanceur vérifie cette limite et avertit si glibc est trop ancienne ou inconnue ; le déploiement Java/HTTP peut continuer. Il ne tente pas de mettre à niveau glibc ou le système. Contrôlez aussi votre version avec `ldd --version` ; un déploiement réussi ne prouve pas à lui seul le chargement effectif de la bibliothèque torrent.

| Distribution | Paquets de compilation habituels |
| --- | --- |
| Debian/Ubuntu | `build-essential`, `binutils`, `libpcap-dev`, `pkg-config` |
| Fedora | `gcc`, `glibc-devel`, `make`, `binutils`, `libpcap-devel`, `pkgconf-pkg-config` |
| Arch | `gcc`, `make`, `binutils`, `pkgconf`, `libpcap` |

Selon l’environnement, `curl`, les certificats CA, `tar`, `gzip` et les bibliothèques graphiques Java sont également nécessaires. Sous Arch, les métadonnées des dépôts doivent être maintenues à jour avec les procédures normales de la distribution. Le script ne réalise pas de mise à niveau générale de tous les paquets.

Une version native appropriée de Java 17+ est réutilisée. Sinon, une JRE officielle Temurin 21 à version fixée, vérifiée par SHA-256, est installée **pour cette application uniquement**. Une connexion Internet est nécessaire si ces paquets ou cette JRE manquent.

### macOS — ZIP portable 3.0 uniquement

- Plateforme visée pour le déploiement et les fonctions Java/HTTP : **macOS 11 ou ultérieur**, Intel ou Apple Silicon. Java doit correspondre au processeur natif ; une JRE Intel sous Rosetta n’est pas automatiquement choisie sur Apple Silicon.
- Les **Command Line Tools d’Apple** et la libpcap du système sont nécessaires pour compiler localement le composant réseau. Xcode complet et Homebrew ne sont pas requis. Voir la [procédure officielle d’Apple](https://developer.apple.com/documentation/xcode/installing-the-command-line-tools/).
- Le composant utilise `sudo` pour ouvrir le périphérique BPF Ethernet, puis abandonne définitivement ses groupes supplémentaires, son identifiant de groupe et son identifiant d’utilisateur privilégiés. L’interface Java et les téléchargements s’exécutent sous le compte utilisateur habituel.
- Si macOS demande l’autorisation d’accéder au réseau local pour les programmes utilisés, vous devez accepter vous-même ces demandes.
- Si Java 17+ natif est absent, une JRE Temurin 21 à version fixée et vérifiée par SHA-256 est installée dans un dossier privé. Le Java du système n’est pas remplacé.

**Prérequis supplémentaire pour BitTorrent/magnet :** les bibliothèques torrent effectivement fournies ciblent des versions plus récentes : **macOS 26 ou ultérieur sur Apple Silicon, macOS 15 ou ultérieur sur Intel**. macOS 11+ ne signifie donc pas que tous les protocoles de cette publication y sont disponibles. Le lanceur détecte cette limite, avertit sur un système plus ancien et permet le déploiement Java/HTTP, sans mettre à niveau macOS. Une installation réussie ne prouve pas le chargement effectif de la bibliothèque torrent. Vérifiez version et architecture avec `sw_vers -productVersion` et `uname -m` ; l’exécution du paquet portable 3.0 sur un Mac utilisateur reste à vérifier.

Le `.app` produit dans cette version **n’est ni signé avec Developer ID ni notarié**. Le parcours portable 3.0 Finder/Gatekeeper sur un Mac utilisateur reste à vérifier. Les builds natifs 4.0 et le lancement/API ont été vérifiés sur de vrais runners Mac GitHub ; les dialogues de sécurité utilisateur et la capture BPF restent à vérifier. Les demandes de sécurité macOS et leurs confirmations sont expliquées séparément ci-dessous.

### Vérifications automatiques des lanceurs

Le déploiement vérifie le système, le processeur natif, les droits d’écriture, l’espace libre et un environnement Java complet approprié. Linux/macOS contrôlent également les limites de version BitTorrent décrites ci-dessus et examinent les liens réseau en lecture seule. Un lien au format Ethernet reste un candidat, pas une connexion physique aux passerelles démontrée. Le composant réseau vérifie l’adaptateur et le sous-réseau avant le transfert. Une VM disposant d’une connexion Ethernet correctement pontée n’est pas rejetée par le lanceur du seul fait de son matériel virtuel.

Les environnements Java manquants sont téléchargés depuis des sources officielles fixées et vérifiés avant utilisation. Sous Linux, les paquets compilateur/libpcap/runtime manquants sont demandés au gestionnaire de paquets ; sous macOS, les outils Apple manquants sont demandés via leur installateur visible. Un Java réduit inadéquat est écarté sous Linux/macOS ; l’environnement privé complet est installé si nécessaire. Vous devez confirmer vous-même les demandes d’administration, de licence et de confidentialité. Une erreur de téléchargement, d’empreinte ou de modules arrête l’étape concernée.

La langue choisie nécessite des polices installées couvrant ses caractères. Sous Linux minimal, installez les paquets adaptés de la distribution, par exemple Noto CJK pour le chinois/coréen et une famille Noto arabe. Java utilise les polices de repli de la plateforme ; l’application n’installe ni ne fournit de paquets de polices. La couverture dépend du poste.

## Réseau et adresse IP virtuelle

Un nouveau profil contient ces quatre passerelles, toutes sélectionnées par défaut :

```text
192.168.1.1
192.168.1.2
192.168.1.3
192.168.1.4
```

L’adresse virtuelle de l’application est **`192.168.1.70`** par défaut. Adaptez ces valeurs à votre réseau. L’application accepte **1 à 32 passerelles distinctes**, accessibles au moyen d’un adaptateur Ethernet physique approprié dans un sous-réseau IPv4 local commun. La passerelle utilisée par le système n’est pas ajoutée comme un cinquième chemin.

L’IP virtuelle appartient à la **pile réseau privée de l’application**. Elle permet des connexions indépendantes via les passerelles choisies, sans modifier les adresses des interfaces ni la table de routage du système, et donc sans changer les connexions des autres programmes. Vous n’avez pas à l’ajouter préalablement aux paramètres réseau du système. Elle doit toutefois être libre dans le LAN et appartenir au bon sous-réseau ; réservez-la si possible en dehors de la plage attribuée par DHCP. L’application recherche un conflit d’adresse avant son utilisation.

Ce transport Ethernet ne prend pas en charge le Wi-Fi, les adaptateurs exclusivement VPN ni les connexions IPv6. Les pairs BitTorrent locaux du LAN ne sont pas acheminés par ce transport. Si plusieurs adaptateurs Ethernet conviennent, l’application peut refuser une sélection ambiguë. Elle ne peut pas transformer une passerelle inaccessible en connexion Internet utilisable.

Les requêtes DNS utilisent la passerelle choisie : d’abord le routeur, puis si nécessaire `1.1.1.1` ou `8.8.8.8` via cette même passerelle. Il n’existe pas de repli vers le résolveur DNS du système. Après un délai UDP dépassé ou une réponse DNS tronquée, TCP est tenté via la même passerelle et la même IP virtuelle, dans un budget DNS total de 8 secondes. DNSSEC, DoH et les requêtes de suivi CNAME distinctes ne sont pas implémentés.

## Installateurs natifs 4.1.1 : Windows et macOS

Choisissez l’installateur de votre système dans les [GitHub Releases](https://github.com/2018-spec-fdsds/multiroute-downloader/releases), s’il est joint à la publication. Chaque paquet est **un seul fichier d’installation**, contenant application, lanceur natif, Java complet et composant réseau. Les sources et les ZIP portables 3.0 restent des distributions distinctes.

| Système | Installateur | Lancement après installation |
| --- | --- | --- |
| Windows 11 x64 | `MultiRoute-Downloader-4.1.1-Windows-x64-Setup.exe` | **MultiRoute Downloader** dans Démarrer ou via son raccourci de bureau |
| Apple Silicon, macOS 26+ | `MultiRoute-Downloader-4.1.1-macOS-arm64.pkg` | **MultiRoute Downloader** dans Applications, Spotlight ou le Dock |
| Intel, macOS 15+ | `MultiRoute-Downloader-4.1.1-macOS-x86_64.pkg` | **MultiRoute Downloader** dans Applications, Spotlight ou le Dock |

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

## ZIP portables 3.0 et Linux : installation et lancement

Téléchargez le ZIP correspondant à votre système dans les [publications GitHub](https://github.com/2018-spec-fdsds/multiroute-downloader/releases) et extrayez **tout son contenu**. Une publication prête à l’emploi et une copie des sources sont différentes : les sources seules doivent d’abord être compilées. Consultez le [guide de compilation](../native/source/BUILD-README.txt).

### Windows

1. Extrayez le ZIP dans un dossier permanent accessible en écriture.
2. Pour le premier lancement, double-cliquez sur **`Deploy-Windows.cmd`**. Les vérifications et, si nécessaire, le téléchargement vérifié Java/Npcap se font automatiquement.
3. Confirmez au besoin l’installation visible de Npcap, UAC et sa licence. Si elle demande un redémarrage, redémarrez Windows puis ouvrez à nouveau le lanceur.
4. Aux lancements suivants, utilisez **`Start-Windows.cmd`**, qui effectue également les vérifications. Ouvrez-le normalement, sans « Exécuter en tant qu’administrateur ».

Pour choisir Java explicitement, définissez `JAVA_EXE` vers un `java.exe` complet approprié, ou configurez `JAVA_HOME` ou `PATH`. Une valeur Windows explicite `JAVA_EXE` erronée doit être corrigée ou supprimée.

Exemple dans `cmd.exe` ; remplacez le chemin :

```bat
set "JAVA_EXE=C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe"
Start-Windows.cmd
```

Pour lancer seulement l’API ou changer son port :

```bat
Start-Windows.cmd --headless
Start-Windows.cmd --headless --port 8766
```

### Linux

1. Extrayez le ZIP dans un dossier permanent accessible en écriture sous votre compte utilisateur.
2. Ouvrez **`Deploy-Linux.desktop`** et autorisez le lanceur si le gestionnaire de fichiers le demande. Vous pouvez aussi ouvrir un terminal dans ce dossier et exécuter `bash ./Deploy-Linux.sh`.
3. Confirmez les opérations `sudo` demandées. Le script vérifie l’environnement, installe les dépendances manquantes, compile le composant et exécute son autotest hors ligne avant son installation protégée.
4. La fenêtre démarre sous le compte utilisateur normal. **Gardez ce terminal ouvert** : l’autorisation temporaire `sudo` est liée à cette session.
5. Aux lancements suivants, utilisez l’entrée **MultiRoute Downloader** du menu des applications ou `bash ./Start-Linux.sh`. Si Java ou le composant est absent, le lanceur poursuit le déploiement.

Sans interface graphique :

```sh
bash ./Deploy-Linux.sh --headless
bash ./Start-Linux.sh --headless
```

Ne lancez pas toute l’application avec `sudo`.

### macOS — ZIP portable 3.0 uniquement

1. Extrayez la totalité du ZIP macOS ; ne lancez pas le script depuis l’intérieur d’une archive.
2. Ouvrez **`Deploy-macOS.command`** avec Terminal. Le lanceur vérifie le système, le processeur natif, l’espace libre, les droits d’écriture, Java et le composant réseau.
3. Si les Command Line Tools manquent, l’installateur Apple s’ouvre. Confirmez vous-même ses demandes d’installation, de licence et d’administration. Le terminal attend **jusqu’à 30 minutes** et reprend automatiquement lorsque les outils sont prêts. `Ctrl+C` annule l’attente ; après une annulation ou une expiration, relancez le script une fois les outils installés.
4. Confirmez la demande d’administration pour le composant protégé. Celui-ci est vérifié avant validation de l’installation.
5. L’application est installée et ouverte dans **`~/Applications/MultiRoute Resume.app`**. **Gardez le terminal ouvert pendant son utilisation.**
6. Les lancements suivants se font depuis le `.app` installé ou **`Start-macOS.command`**. Si l’autorisation temporaire a expiré, fermez l’application et ouvrez à nouveau le lanceur.

Pour fournir votre propre Java natif :

```sh
JAVA_EXE="/absolute/path/to/java" ./Deploy-macOS.command
./Start-macOS.command --headless
```

Ne lancez pas Java ni tout le lanceur avec `sudo`. Le déploiement ne change pas globalement les droits BPF et ne crée pas d’exception permanente dans `sudoers`.

### macOS : demandes de sécurité au premier lancement

1. Vérifiez la provenance et la SHA-256 publiée du ZIP. Le paquet n’est pas signé/notarié ; son installation ne lui apporte pas une validation de confiance par Apple.
2. Tentez le lancement normal. Si seule l’identité du développeur ne peut pas être vérifiée, vous pouvez choisir **Réglages Système → Confidentialité et sécurité → Ouvrir quand même**, puis **Ouvrir**, si macOS le propose et si vous faites confiance au paquet vérifié. C’est également le parcours habituel sous macOS 15/26 ; voir [Apple](https://support.apple.com/en-us/102445). Une alerte de logiciel malveillant ou endommagé est différente et ne doit pas être contournée.
3. L’installation des outils Apple exige vos confirmations d’installation et de licence. Gardez son terminal d’attente ouvert.
4. Lors de la saisie du mot de passe `sudo` dans Terminal, aucun caractère ne s’affiche normalement. Il autorise le composant réseau protégé ; Java reste sous votre compte utilisateur.
5. Si **Réseau local** est demandé, autorisez le processus Terminal/Java/application concerné à communiquer avec vos passerelles. Après un refus, ce choix peut être modifié dans **Réglages Système → Confidentialité et sécurité → Réseau local**. Voir les [autorisations réseau Apple](https://support.apple.com/guide/mac-help/control-access-to-your-local-network-on-mac-mchla4f49138/mac).

Le lanceur ne désactive pas Gatekeeper/SIP ou l’antivirus et ne retire pas globalement les attributs de quarantaine des téléchargements. Les confirmations de sécurité et d’administration ne sont pas acceptées automatiquement.

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

### Installateurs natifs 4.1.1

| Système | Réglages, historique et journaux | Application/Java | Composant réseau |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App`, avec Java inclus | Composant x64 inclus ; Npcap installé séparément |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app`, avec Java | `/Library/PrivilegedHelperTools/MultiRouteResume/4.1.1/gateway-net` |

Fermez l’application avant une mise à jour avec un nouvel installateur. Sous Windows, supprimez-la par **Paramètres → Applications → Applications installées**. Sous macOS, supprimer la `.app` ne supprime pas automatiquement l’historique ni le composant protégé séparé. Les fichiers téléchargés restent dans le dossier choisi ; ne le supprimez pas seulement pour désinstaller. Le chemin des données diffère du ZIP Windows 3.0 ; conservez votre ancien profil avant migration.

### Paquets portables 3.0 et Linux

| Système | Données et historique | Java privé / composant réseau |
| --- | --- | --- |
| Windows | Par défaut, `data` à côté du programme ; `data-location.config` ou `--data-dir` peut désigner un autre profil. | Éventuellement `runtime` à côté du programme ; `native/windows-x64/gateway-net.exe`. |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/runtime` ; `/usr/local/libexec/MultiRouteResume/3.0/gateway-net`. |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | Dossier `runtime` voisin ; `/Library/PrivilegedHelperTools/MultiRouteResume/3.0/gateway-net`. |

Le dossier de téléchargement est indépendant du dossier de données. Les journaux UTF-8 se trouvent, relativement au dossier de données réellement utilisé, dans `logs/application.log` et `network/gateway-network.log`. Les messages enregistrés sont traduits à l’affichage ; les clés techniques internes restent identiques. IP, liens et chemins de fichiers sont conservés ; les adresses MAC et paramètres des liens signés sont masqués dans les journaux utilisateur.

| Problème | Vérification |
| --- | --- |
| Java ne démarre pas / mauvaise architecture | Vérifier `java -version`, `JAVA_EXE`, `JAVA_HOME`, l’architecture et tous les modules nécessaires. Relancer le déploiement ; corriger ou supprimer une valeur Windows explicite `JAVA_EXE` erronée. |
| Erreur Npcap | Vérifier l’installation et les droits d’accès, notamment AdminOnly. Ne pas créer automatiquement d’exception antivirus. |
| Aucun adaptateur approprié / plusieurs adaptateurs | Vérifier Ethernet physique, sous-réseau IPv4 commun et configuration des adaptateurs. Wi-Fi/VPN ne remplace pas le transport pris en charge. |
| IP virtuelle déjà occupée | Choisir une autre adresse libre du LAN et la vérifier. Ne pas modifier les routes du système. |
| Autorisation Linux/macOS expirée | macOS natif : rouvrir l’application et confirmer le dialogue d’administration si demandé. Portable 3.0/Linux : rouvrir le lanceur de terminal et y autoriser ; garder le terminal ouvert. Ne pas lancer Java en root. |
| Pas d’interface sous Linux | Vérifier X11 ou XWayland et les bibliothèques de polices, ou utiliser `--headless` pour l’API seule. |
| Téléchargement en attente malgré un test réussi | Vérifier URL, DNS, droits et erreurs réseau/serveur dans les détails. Tester un autre fichier ne prouve pas l’accès au serveur voulu. |
| HTTP 401/403 ou source Hugging Face privée | Configurer l’autorisation de la source et son accès lié à l’origine. Le test d’une passerelle ne remplace pas l’autorisation du serveur. |
| Clé d’hôte SFTP différente | Vérifier son empreinte par une voie indépendante et fiable ; ne pas désactiver la vérification. |
| Magnet sans métadonnées | Vérifier les participants/trackers et leur accessibilité ; une distribution inactive ne peut pas être forcée. |
| Espace insuffisant | Vérifier les disques des données et de destination ; arrêter le travail, modifier son dossier dans l’interface, puis reprendre. |
| Port API occupé | Choisir un port avec `--port 8766`. Un ancien processus MultiRoute est fermé proprement ; les autres applications ne sont pas arrêtées. |

Pour mettre à jour les paquets portables 3.0 Linux/macOS, exécutez le lanceur de déploiement correspondant depuis la nouvelle archive extraite. Après déplacement du dossier du programme, relancez-le pour actualiser le chemin du raccourci. Sauvegardez données et historique avant leur suppression volontaire ; ne supprimez pas récursivement le dossier des téléchargements pour désinstaller l’application.

## Portée des vérifications

Les tests Java, API, protocoles, interface et composants natifs hors ligne ainsi que des cas réseau synthétiques propres au projet ont été exécutés. Ils **ne démontrent pas le transfert réel par vos routeurs physiques**. Les paquets natifs 4.0 ont été compilés et vérifiés sur les runners GitHub Apple Silicon/macOS 26 et Intel/macOS 15 : lancement/API, torrent JNI, contenu et tests du composant réseau. Installation utilisateur, dialogues Finder/sécurité et capture BPF restent à vérifier. Seul le paquet portable 3.0 compile le composant localement chez l’utilisateur ; les paquets natifs 4.0 incluent des composants précompilés distincts Intel/Apple Silicon, sans signature Developer ID ni notarisation. Les vérifications de paquet ou CI ne démontrent pas leur fonctionnement sur votre Mac. Après installation, testez chaque passerelle avec un vrai fichier avant de compter sur leur bande passante combinée.

Rapports : [CHECKS.ru.md](../CHECKS.ru.md), [TEST-REPORT.json](../TEST-REPORT.json). Composants tiers et licences : [THIRD-PARTY-NOTICES.txt](../THIRD-PARTY-NOTICES.txt).

## Zone de notification et fermeture (4.0)

Fermer la fenêtre principale laisse l’application et les téléchargements actifs dans la zone de notification. L’icône indique le débit total de **tous les travaux**, indépendamment de la sélection et du filtre. Son infobulle affiche le débit exact en Mbit/s ou Gbit/s (à partir de 1 Gbit/s), ainsi qu’en octets/s, jusqu’à 10 Gbit/s ; la petite icône arrondit la valeur. Choisissez **Ouvrir l’application** dans le menu de l’icône pour restaurer la fenêtre. Sous Windows/Linux, un clic sur l’icône l’ouvre également. **Quitter l’application**, dans le menu de l’icône ou la fenêtre, enregistre l’état et termine le programme. Utilisez cette commande avant une mise à jour.

Sans zone de notification compatible, la fenêtre est réduite dans la barre des tâches et les téléchargements continuent ; Quitter reste disponible. Si la zone disparaît, la fenêtre cachée se rouvre. Windows peut ranger l’icône dans les notifications masquées.

Le setup crée un raccourci sur le bureau et, sous Windows, une entrée Démarrer. macOS crée un lien Desktop vers la `.app` installée pour l’utilisateur connecté et réessaie au premier lancement graphique. Les éléments existants sont conservés. Si la confidentialité empêche la création, autorisez l’accès demandé ou ouvrez depuis Applications. Nos textes du setup sont anglais ; les dialogues de sécurité et d’autorisation du système suivent la langue du système.

## Licence

Vous pouvez installer et utiliser gratuitement l’application originale à titre personnel et dans des organisations, y compris pour des activités professionnelles. **Vendre l’application, facturer sa licence ou la redistribuer contre paiement est interdit** sans autorisation distincte. La licence spécifique n’accorde pas de droit général de modification ou redistribution et n’est pas une licence open source approuvée par l’OSI. Les composants tiers conservent leurs licences. Ceci est un résumé traduit ; le texte anglais de [LICENSE.txt](../LICENSE.txt) fait foi.

En 4.0 l’icône de vitesse a un fond transparent avec des chiffres et unités noirs, y compris à 10 Gbit/s. La vitesse totale et les commandes de la zone de notification sont conservées.

## Vérification MD5, SHA-256 et SHA-512

Après assemblage, l’application calcule les trois sommes du fichier complet en une seule lecture. Collez une référence dans le champ principal facultatif avec Ctrl+V ou Cmd+V. Le type est reconnu automatiquement : 32 caractères hexadécimaux correspondent à MD5, 64 à SHA-256 et 128 à SHA-512. La section dépliable des sommes supplémentaires permet de saisir plusieurs références.

Les références publiées sont recherchées dans les en-têtes compatibles du condensat de la représentation complète et les fichiers annexes : `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` et `SHA512SUMS`. Le condensat d’une réponse HTTP partielle n’est pas considéré comme la somme du fichier entier. Les contrôles propres à Hugging Face LFS et Metalink sont conservés.

Toutes les références connues doivent correspondre avant de publier le fichier final. Une différence fait échouer la tâche tout en conservant les données partielles. Sans référence, la valeur est calculée localement et ne confirme pas une correspondance avec la source. MD5 détecte les altérations accidentelles ; SHA-256 ou SHA-512 offrent une vérification d’intégrité plus forte. MD5 ne remplace ni l’identité sûre du fichier ni les exigences relatives aux miroirs fiables.

La vérification des pièces torrent reste obligatoire et distincte des sommes des fichiers assemblés. Une référence manuelle du fichier entier exige de sélectionner exactement un fichier ; pour un groupe, donnez une référence propre à chaque tâche.

L’API locale accepte `expected_md5`, `expected_sha256` et `expected_sha512` dans `POST /api/v1/jobs`. Les résultats contiennent `md5`, `sha256`, `sha512` et les sources de référence dans `checksum_sources`. Les requêtes existantes avec `expected_sha256` restent compatibles.

## Progression de chaque téléchargement (4.0)

Chaque téléchargement dispose de sa propre barre dans la colonne Progression. Les colonnes occupent toute la largeur du panneau des téléchargements. La liste défile verticalement lorsque les tâches sont nombreuses ; les panneaux étroits permettent aussi un défilement horizontal sans rendre les colonnes illisibles. Lors de l’actualisation, la première tâche visible est conservée. Le tri et les filtres maintiennent chaque barre associée à son téléchargement. Si la taille totale est inconnue, le volume reçu est affiché à la place du pourcentage ; l’infobulle indique taille, état et erreurs. La ligne sélectionnée affiche toujours la progression détaillée, les temps et les vitesses des passerelles.

Le dossier de téléchargement par défaut est déterminé au démarrage pour l’utilisateur actuel : `%USERPROFILE%\Downloads` sous Windows et `~/Downloads` sous macOS/Linux. Le dossier par défaut enregistré suit le profil actuel ; un dossier choisi manuellement est conservé.

## Affichage du journal (3.6)

La fenêtre du journal conserve la position de lecture lors des actualisations automatiques et manuelles. En bas du journal, les nouvelles entrées restent visibles. Remonter suspend le suivi sans arrêter la journalisation. La sélection de texte à copier est conservée. Le changement de langue conserve l’événement visible ; la réouverture affiche les dernières entrées.

## Détails du téléchargement et rapport des sommes (3.7)

Le panneau inférieur des détails du téléchargement sélectionné conserve la position de lecture et la sélection de texte lors des actualisations automatiques répétées. Vous pouvez lire la fin du rapport des sommes sans que le panneau revienne au début. La sélection d’un autre téléchargement ouvre son rapport au début. La fenêtre distincte du journal conserve son comportement de défilement.

## Instructions et guide API (3.8)

Les boutons **Instructions** et **Guide API** se trouvent en haut à droite, sur une ligne distincte sous le choix de langue. Ils ouvrent le guide dans la langue sélectionnée avec le navigateur par défaut. **Guide API** mène directement à la section de l’API locale. Les liens correspondent à la version de l’application ; une connexion Internet est nécessaire pour ouvrir les pages GitHub.

## Gestion des commandes au démarrage (3.9)

La version 3.9 corrige un échec de démarrage lorsqu’une recherche de processus dépasse son délai et laisse son fichier temporaire de sortie verrouillé sous Windows. La sortie passe désormais par des canaux à tampon limité au lieu de fichiers temporaires. Les commandes interrompues ou ayant dépassé leur délai sont arrêtées, puis leur fin est attendue. Le remplacement d’une ancienne instance de MultiRoute exige toujours de vérifier l’identité de l’application. Les protocoles de téléchargement et le contrat de l’API locale restent inchangés.

## Interface de bureau et historique des téléchargements (4.0)

L’interface utilise FlatLaf avec les thèmes **Système**, **Clair** et **Sombre**. Le choix dans l’en-tête est enregistré. La file conserve les barres individuelles, les colonnes sur toute la largeur, le tri et les filtres. Le résumé montre tous les travaux, les actifs, les terminés et la vitesse globale de tous les travaux, indépendamment du filtre.

Survolez une ligne active ou terminée pour afficher le **nom complet du fichier et son chemin de destination absolu**, y compris après tri ou filtrage. Le titre du téléchargement sélectionné propose la même info-bulle. Le menu contextuel **Copier le chemin complet** copie la destination.

**Ctrl+L** sous Windows/Linux ou **Cmd+L** sous macOS place le curseur dans l’URL ; **Ctrl+F** ou **Cmd+F** dans la recherche ; **Échap** efface la recherche. Le collage Ctrl+V/Cmd+V est conservé.

Le téléchargement sélectionné comporte les onglets **Détails** et **Historique**. L’historique consigne les événements avec date et heure : premier démarrage, chaque reprise et fin réussie. Aucune date n’est inventée pour les anciens travaux sans données enregistrées. Les emplacements et champs de l’API sont décrits ci-dessous.

Les protocoles, passerelles, IP virtuelle, tests de vitesse, importation groupée, accès aux sources, MD5/SHA-256/SHA-512, zone de notification, API locale et défilement du journal et des détails sont conservés. La nouvelle interface ne modifie pas les routes du système.

### Stockage de l’historique et API

Le dossier des données contient `logs/downloads-history.jsonl` (UTF-8, un objet JSON par ligne) et `logs/downloads-history.log` (entrées lisibles datées). Ils sont complétés après les redémarrages et conservés lors de la suppression d’un travail ou de ses fichiers. Le journal texte conserve la langue au moment de l’écriture ; l’interface affiche les événements dans la langue actuelle. La fenêtre ne lit que les derniers événements, jusqu’à 1 000 / 2 Mio ; le journal complet reste sur disque et grandit avec l’utilisation. Archivez-le séparément au besoin. Il contient les noms et chemins de destination complets, sans URL source, mots de passe ni jetons. Vérifiez les chemins avant de le partager.

Windows natif : `%LOCALAPPDATA%\MultiRoute Downloader\data\logs\`. macOS natif : `~/Library/Application Support/MultiRoute Downloader/data/logs/`. Linux et les profils portables utilisent le dossier indiqué plus haut ; `--data-dir` permet d’en choisir un autre.

`GET /api/v1/jobs` et la réponse d’un travail exposent `lifecycle_history`. Chaque événement comporte `event_id`, `job_id`, `kind`, `time`, `filename`, `path` ; les torrents peuvent ajouter `paths`, les pauses `reason`. Types : `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Les dates utilisent ISO 8601 UTC, par exemple `2026-10-18T12:34:56.123Z` ; le journal lisible indique la date, l’heure locale et le décalage UTC. Les champs facultatifs `first_started_at`, `last_resumed_at`, `completed_at` résument les événements observés. La mise en file ne constitue pas encore un démarrage. La fin est enregistrée après vérification et publication. `recovered` signifie qu’un ancien fichier publié est vérifié maintenant, sans prétendre connaître son ancienne date de fin. Aucune date passée n’est inventée. Les opérations existantes restent disponibles ; aucun endpoint séparé n’est nécessaire.

La fenêtre générale **Journal** sépare **Événements de l’application** et **Historique des téléchargements**. L’historique affiche les événements datés des différents travaux, même supprimés ; l’historique du téléchargement sélectionné ne concerne que celui-ci.

## Nouveau nom et profils existants

MultiRoute Downloader s’appelait MultiRoute Resume. Les nouveaux profils utilisent les chemins ci-dessus. Si le nouveau dossier n’existe pas et qu’un ancien profil est disponible, il est réutilisé sans déplacer ni supprimer de fichiers. Les anciennes données peuvent rester dans `%LOCALAPPDATA%\MultiRoute Resume\data` ou `~/Library/Application Support/MultiRoute Resume/data`. Le dossier protégé `MultiRouteResume`, l’identifiant de mise à niveau Windows, l’identité interne de l’API et le port sont conservés. Après installation, utilisez le nouveau raccourci ou l’app MultiRoute Downloader. Les versions historiques gardent leurs noms de fichiers.

## Sites enregistrés, envois et aperçu des archives (4.0)

Ouvrez **Profils de sites…**, créez/modifiez un profil et choisissez-le dans **Téléchargements** ou **Envois**. Passerelles et IP virtuelle sont dans **Paramètres réseau**. Dans **Envois**, saisissez l’**URL du dossier distant**, puis **Choisir les fichiers et envoyer…** pour choisir un ou plusieurs fichiers locaux avec le sélecteur système. L’interface ajoute les noms; API `target_url` désigne au contraire un fichier distant complet. Pour l’aperçu, collez l’URL de l’archive dans le champ principal puis cliquez sur **Contenu de l’archive…**; aucun téléchargement ne démarre.

Un profil contient nom, URL de base, dossier local, dossier distant et passerelles. Il accepte accès anonyme, HTTP Basic/Bearer/Cookie/en-têtes personnalisés, FTP et SFTP avec empreinte SHA-256 du serveur préalablement vérifiée. Plusieurs comptes du même serveur restent isolés. Les dossiers/passerelles explicites remplacent les valeurs par défaut; les URL relatives utilisent la base. Les secrets sont chiffrés dans `site-profiles/vault.bin` avec la clé locale `site-profiles/key.bin` et des droits réservés au propriétaire. Cela ne protège pas contre quelqu’un contrôlant votre compte et sa clé. L’API ne renvoie pas les secrets. Des champs vides les conservent uniquement si origine et type restent identiques; `replace_credentials:true` les remplace. Une redirection HTTP vers une autre origine ne reçoit pas les identifiants. Supprimer le profil conserve tâches et fichiers locaux/distants; une tâche conserve sa liaison au profil initial et ne peut changer de compte; recréez-la avec un profil valide si le profil initial a été supprimé.

L’envoi et l’exploration distante prennent en charge **FTP, FTPS explicite et SFTP** via les passerelles choisies. Sélectionnez un fichier local ordinaire et une URL de fichier distant complète (`target_url`). Un fichier temporaire permet la reprise après comparaison du préfixe existant. Le succès exige une relecture distante complète et SHA-256: droits de **lecture, écriture et renommage** requis. Cette lecture consomme du trafic. Une somme différente ou une lecture refusée signifie un échec. Chaque fichier utilise une passerelle à la fois; les fichiers indépendants sont répartis et envoyés en parallèle. Une autre passerelle peut prendre le relais après le délai d’inactivité. Un même envoi n’est pas découpé entre passerelles. Pause/reprise et progression persistent. Supprimer la tâche conserve le fichier local et les fichiers distants définitifs/temporaires.

L’écrasement est désactivé par défaut. FTP/SFTP ne garantissent pas partout un renommage atomique uniquement si la cible est absente; un autre client peut modifier cette cible pendant la publication. Coordonnez les destinations partagées. Lister un dossier n’envoie et ne supprime rien.

L’aperçu liste noms, chemins, tailles, dossiers et chiffrement des **ZIP, TAR, 7Z, RAR4 et RAR5**, sans extraction ni exécution. Il exige une passerelle, des plages d’octets et un ETag ou Last-Modified stable. Limites: 16 MiB de métadonnées, 10 000 entrées, 60 secondes; décodage des en-têtes 7z limité à 64 MiB. La liste peut être partielle et ne vérifie pas l’intégrité du contenu. Choisissez le jeu de caractères original si les noms sont illisibles. Les en-têtes 7z chiffrés compatibles peuvent utiliser un mot de passe non conservé. Les en-têtes RAR chiffrés sont signalés non pris en charge/mot de passe nécessaire; les volumes multiples peuvent être incomplets.

L’API accepte `site_profile_id` pour téléchargements, envois, listes et aperçus. `GET /api/v1/site-profiles` renvoie `{profiles:[...]}`; `POST` ou `PUT` enregistre un objet plat et renvoie les métadonnées publiques; `DELETE /api/v1/site-profiles/{id}` supprime seulement le profil. `POST /api/v1/uploads` reçoit `local_path`, `target_url`, `routes`, éventuellement `site_profile_id`, `overwrite:false`; réponse HTTP 201. `GET /api/v1/uploads` renvoie `{uploads:[...]}`. GET/DELETE individuels et POST `/{id}/pause` / `/{id}/resume` sont disponibles. `POST /api/v1/remote/list`: `url` et `route`/`routes`; `POST /api/v1/archives/preview` accepte aussi `password` et `charset`. Voir [openapi.yaml](../openapi.yaml). Les manifestes UTF-8 de `data/uploads/` conservent l’ID du profil, pas le mot de passe. Un aperçu ne crée pas de téléchargement.

## Planification et priorité des téléchargements

Les contrôles de file/planification règlent `max_active_jobs` (1–32), limites globales/par passerelle et plage quotidienne (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm). Le fuseau local est exposé comme `schedule_timezone`; la plage peut franchir minuit et des heures égales signifient toute la journée. Hors plage, les téléchargements retournent en file; une pause manuelle reste une pause. `priority` de -100 à 100 classe les tâches en attente, valeur haute d’abord puis date de création; cela n’interrompt pas une tâche active. `start_after_ms` est le démarrage au plus tôt en millisecondes Unix, 0 supprime le délai. Les réglages persistent et concernent les téléchargements, pas la file d’envois indépendante. Gardez application et ordinateur actifs; aucun réveil du système n’est programmé. API: `GET`/`PUT`/`POST /api/v1/queue` et `PUT`/`POST /api/v1/jobs/{id}/options`.

## Intégration Chrome et Edge

Ouvrez **Chrome / Edge** dans l’application, autorisez les liens et ouvrez le dossier de l’extension. Une fois: `chrome://extensions` ou `edge://extensions`, mode développeur, **Charger l’extension non empaquetée**, puis `browser-extension` contenant `manifest.json`. Les paquets natifs l’incluent près du JAR; voir [browser-extension/](../browser-extension/). Gardez ce dossier à cet endroit. Décompressez d’abord `MultiRoute-Downloader-4.1.1-Chrome-Edge.zip` si utilisé. Cette version n’est pas publiée dans un magasin d’extensions.

MultiRoute doit fonctionner, y compris dans la zone de notification. Envoyez un lien par son menu contextuel ou la fenêtre de l’extension. Dossier, passerelles et profil courants sont utilisés; les choix de dépôt/torrent restent affichés. Le port est 8765; adaptez-le si l’API utilise un autre port. Seule l’URL est envoyée, sans cookies ni mots de passe; les autres téléchargements du navigateur ne sont pas interceptés. Configurez l’accès à la source dans l’application. Si le navigateur demande l’accès local, ne l’accordez qu’à cette extension choisie.

Le pont est désactivé par défaut et n’accepte que `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` sur `/api/v1/browser/status` et `/api/v1/browser/download`; les autres règles API restent inchangées. HTTP 202 confirme la remise au bureau, pas la fin d’un fichier. Un serveur headless seul ne peut ouvrir la sélection graphique. Supprimez l’extension ou désactivez l’intégration pour arrêter les envois de liens.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
