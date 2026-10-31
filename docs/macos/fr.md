# macOS : installation, démarrage et autorisations

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · **Français** · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## Installer et ouvrir

Avant une mise à jour, terminez les transferts et choisissez **Quit/Exit** dans le menu MultiRoute de la barre des menus. Le bouton rouge peut seulement masquer la fenêtre. Téléchargez le PKG Apple Silicon ou Intel depuis les [téléchargements](../../downloads/README.md), vérifiez SHA-256 puis ouvrez-le. Confirmez l’installation dans le dialogue administrateur macOS. Lancez ensuite `/Applications/MultiRoute Downloader.app` avec Finder ou Spotlight ; Java séparé et Terminal ne sont pas nécessaires.

## Confirmer l’ouverture

Si macOS ne peut pas vérifier le développeur, essayez d’abord d’ouvrir le PKG ou l’app. Pour un fichier vérifié et de confiance uniquement : **Réglages Système → Confidentialité et sécurité → Open Anyway → Open** ; les libellés varient. Une alerte de logiciel malveillant ou de fichier endommagé exige d’arrêter et vérifier. [Instructions Apple](https://support.apple.com/en-us/102445).

## Réseau et mot de passe

Le premier démarrage de l’app ou l’application d’une IP virtuelle, de DHCP ou de sa MAC peut demander une autorisation administrateur pour Ethernet BPF. Une reconfiguration ou un nouveau processus peut la redemander. Les contrôles et tests de vitesse normaux réutilisent le composant prêt, sans mot de passe par fichier ou passerelle. Routes système et MAC physique ne changent pas.

Si une demande **Réseau local** apparaît, autorisez l’entrée MultiRoute reconnue dans Confidentialité et sécurité → Réseau local. Cette demande n’apparaît pas partout. [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

Le mot de passe administrateur se saisit seulement dans le dialogue système, jamais dans les champs licence/Hugging Face. Le demandeur peut s’appeler `DesktopLauncher`/Java : confirmez uniquement votre action MultiRoute attendue. Après annulation, relancez explicitement le contrôle et consultez le journal. Ne désactivez pas globalement Gatekeeper/SIP.

[MultiRoute Downloader — Français](../fr.md) · [macOS — English / 12 languages](README.md)
