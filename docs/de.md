# MultiRoute Downloader 4.3.9 — Anleitung auf Deutsch

[macOS: Installation, Start und Berechtigungen](macos/de.md)

<!-- product-overview:start -->
[**Produktübersicht**](overview/de.md)
<!-- product-overview:end -->

[Startseite](../README.md) · [English](en.md) · [Русский](ru.md) · **Deutsch** · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

<!-- current437:start -->

<a name="current-free-pro"></a>

## 4.3.9 — Aktueller Funktionsumfang, Free und Pro

Diese Anleitung beschreibt MultiRoute Downloader 4.3.9. Native Installationspakete und veröffentlichte Prüfsummen finden Sie im [Release 4.3.9](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.3.9). Die folgenden Abschnitte beschreiben den aktuellen Funktionsumfang und die Lizenzrechte.

### Lizenzgrenzen und Funktionen

Free enthält normale Einzeldatei-Downloads, unterstütztes Fortsetzen, die verfügbare volle Geschwindigkeit, Integritätsprüfung, geschütztes DNS und aktuelle Gateway-Raten/Status. Die Bandbreite wird nicht künstlich gedrosselt. Signierte Rechte sind unabhängig: Eine Teillizenz Pro schaltet nur die enthaltenen Rechte frei.

| Funktion | Free | Mit Lizenzberechtigung | ID |
| --- | --- | --- | --- |
| Gleichzeitig genutzte Gateways | ≤ 2 | ≤ 8 | `extra_gateways` |
| Gleichzeitige Downloads | ≤ 3 | ≤ 32 | `extra_concurrent_downloads` |
| Website-Profile | ≤ 3 | Keine Lizenzquote | `extra_site_profiles` |
| Download-Prioritäten | Nur normal | Wählbar | `download_priorities` |
| Gateway-Prioritäten | 0 | Wählbar | `gateway_priorities` |
| Gateway-Verlauf und Berichte | Nicht enthalten | Enthalten | `gateway_statistics` |
| Stapel-Downloads / -Uploads | Nicht enthalten | Enthalten | `batch_downloads` |
| API für externe Anwendungen | Nicht enthalten | Enthalten | `automation_api` |
| Zusätzliche Designs | Graphite / System | Alle Designs | `premium_appearance` |

Für einen Stapel **Neuer Download…** öffnen und **URL-Liste…** in der festen Aktionszeile unten im Fenster wählen.

Nur Rechte Ihrer signierten Lizenz werden freigeschaltet. Dateien und Profile bleiben erhalten. Weitere Dateien warten in der Warteschlange; aktuelle Gateway-Werte bleiben verfügbar. Technische Speichergrenzen gelten weiter.

Die Gateway-Grenze gilt für die gemeinsam von Downloads und Uploads verwendete Menge; ein zusätzlicher Systemweg zählt nicht dazu. Die Grenze von drei gleichzeitigen Aufgaben betrifft Downloads, nicht Uploads; weitere Downloads warten. Eine niedrigere Warteschlangeneinstellung bleibt wirksam. Vorhandene Gateways, Profile, Fortschritt und Prioritäten werden nicht gelöscht. Die Profilquote begrenzt nur das Erstellen neuer Profile. Verlauf/CSV benötigt sein Recht; Liveanzeigen bleiben Free. Zeitpläne bleiben verfügbar, abweichende Prioritäten benötigen ihr Recht. Technische Speicher- und Quellengrenzen bleiben bestehen.

Die signierte Lizenz ist an den Installationsschlüssel gebunden, nicht an MAC, IP oder Router. Sie hat kein Ablaufdatum; 4–4 umfasst 4.x. Änderungen an Gateways, DHCP oder Anwendungs-MAC erfordern keine Neuaktivierung. Ein anderer Rechner, Benutzer oder Datenordner kann eine neue Anfrage erfordern. Dies verspricht weder unveränderbaren Code noch universelle Hardwarebindung.

### Anwendungs-MAC

MultiRoute nutzt eine eigene feste MAC bei manueller IP und DHCP. Die MAC des OS-Adapters bleibt unverändert. Beobachtete Geräte und andere MultiRoute-Instanzen werden auf Konflikte geprüft; keine Antwort schließt ausgeschaltete Geräte nicht aus. Pool: lokal verwaltete Unicast-MACs mit 46 Zufallsbits. Kein DHCP-IP-Adresspool.

**MAC ändern…** — Die Schaltfläche ist nach dem Pausieren/Abschluss aktiver und wartender Übertragungen und dem Ende lokaler Prüfsummenprüfungen verfügbar. MultiRoute prüft beobachtete Quelladressen und kooperative Meldungen anderer Instanzen. „Kein Konflikt erkannt“ garantiert nicht, dass ein ausgeschaltetes oder stilles Gerät die Adresse nicht nutzt. Die Anwendungs-MAC muss sich von der physischen Adapter-MAC unterscheiden; der Adapter und die OS-Routen bleiben unverändert.

### Prüfsumme…

Der Referenzwert wird ohne Neustart oder Fortschrittsverlust gespeichert. MD5, SHA-256 oder SHA-512 wird anhand von 32, 64 oder 128 Hexzeichen erkannt. Nach dem Download wird die zusammengesetzte Datei geprüft. Referenz speichern und Jetzt prüfen wählen. Geprüft wird die fertige lokale Datei. Bei Abweichung erfolgt kein erneuter Download und keine Löschung.

Die abschließende Prüfung verwendet den zuletzt gespeicherten Referenzwert. Änderungen löschen keine Bytes und starten die Übertragung nicht neu. Eine lokale Nachprüfung behält die fertige Datei und meldet Übereinstimmung, Abweichung oder Fehler getrennt vom Downloadstatus. Herausgeberreferenzen bleiben unabhängig erhalten. Eine Gesamtprüfsumme gilt nur für eine Torrentdatei. Integrität ist kein Nachweis harmlosen Inhalts; bevorzugen Sie vertrauenswürdige SHA-256/SHA-512-Referenzen.

### API-Zugriff in 4.3.9

Die lokale JSON-API verwendet UTF-8 ohne gewöhnliches API-Token. Externe Steuerung benötigt automation_api; weitere Rechte werden zusätzlich geprüft. Dieses Recht allein erlaubt weder Stapelaufträge noch zusätzliche Gateways. Free behält eng begrenzte Lesediagnostik, den privat authentifizierten Lebenszyklus und die optionale Übergabe eines einzelnen Browserlinks. Siehe [API](../API.ru.txt) und [OpenAPI](../openapi.yaml); den Port nicht für LAN/Internet öffnen.


Die Originalanwendung unterliegt [Lizenz 1.2](../LICENSE.txt): Free darf privat und in Organisationen/Unternehmen verwendet werden; der Rechteinhaber kann Pro kostenpflichtig anbieten. Andere Empfänger dürfen sie ohne gesonderte Erlaubnis nicht verkaufen. OpenJDK GPLv2 mit Classpath Exception verlangt Hinweise/zugehörige Quellen; JNA nutzt Apache-2.0 mit Hinweisen; die separate JLayer LGPL-2.1 behält Austausch-/Debugging- und Quellrechte. Siehe [Lizenzübersicht](../LICENSING.md), [Komponentenkatalog](../THIRD-PARTY-NOTICES.txt) und Lizenzseite im Programm.

Der System-Stil hält Tabellentext, Felder und Bedienelemente auf dem gewählten Systemhintergrund lesbar; Graphite und System gehören zu Free.

Dateiname und Größe stehen in getrennten Spalten. Die Größe wird numerisch und unabhängig vom Namen sortiert; eine unbekannte Größe erscheint als Strich. Vollständiger Name und Pfad sind im Tooltip verfügbar.

Bewegen Sie den Zeiger über den Dateinamen: Der Tooltip zeigt vollständigen Namen, Speicherpfad, Übertragungsprotokoll und Quelladresse oder lokalen Quellpfad. Torrent-Aufgaben zeigen BitTorrent, auch wenn ihre Metadaten über HTTPS geöffnet wurden. Eine vorhandene Repository-Adresse folgt der tatsächlichen Datei-URL. Eingebettete Zugangsdaten, Abfragewerte und Fragmente werden ausgeblendet; ein erkannter öffentlicher Magnet-Infohash kann sichtbar bleiben. Gespeicherte URL und Übertragungszustand bleiben unverändert.


### Gateway-Prioritäten und Übertragungsdiagnose in 4.3.9

Legen Sie in den Netzwerkeinstellungen für jedes Gateway eine Priorität von **0 bis 1000** fest; der Wert steht auch an der Gateway-Anzeige. Ein höherer Wert bevorzugt das Gateway bei kleinen oder vollständigen Dateiübertragungen und teilt ihm bei großen Dateien mehr parallele Segmente zu. Andere ausgewählte, funktionierende Gateways helfen weiter. Gleiche Werte behalten die Auswahlreihenfolge bei; bei Ausfall oder Stillstand kann ein anderes ausgewähltes Gateway übernehmen. Dies garantiert kein bestimmtes Geschwindigkeitsverhältnis.

Erforderlich ist das eigenständige signierte Recht **`gateway_priorities`**. Das Recht für die Auftragspriorität **`download_priorities`** enthält es nicht. Ohne Gateway-Recht ist die wirksame Priorität überall 0; gespeicherte Werte bleiben erhalten. Änderungen während eines Downloads löschen keine geprüften Teile. Pro ausgewähltem Gateway gibt es ein bis vier parallele Segment-Worker und insgesamt höchstens 32 pro Auftrag.

In den Einstellungen stehen die Anzahl gleichzeitiger Downloads innerhalb der Lizenzgrenze und begrenzte oder unbegrenzte automatische Wiederholungen zur Wahl. Manuell pausierte Aufträge bleiben pausiert. Das Kontextmenü bietet Pause/Fortsetzen für geeignete ausgewählte Aufträge sowie das Kopieren von Dateinamen. Ein Doppelklick öffnet nur den Dateiordner; Eigenschaften, Statistik und individuelles Downloadprotokoll stehen im Kontextmenü.

Das individuelle Protokoll bleibt auch für abgeschlossene Dateien verfügbar. Einstellungen → Fehler und Lösungen öffnet eine durchsuchbare Offline-Hilfe mit 25 Themen in allen 12 Oberflächensprachen. Speicherort und Dateieigenschaften zeigen freien Platz in GB. Ein voller lokaler Datenträger wird ausdrücklich gemeldet, gespeicherte Teile bleiben erhalten und der Fehler wird nicht endlos als Netzwerkfehler wiederholt. Beim Warten nach dem Fortsetzen eines kleinen Segments bleibt der gespeicherte Präfix im Fortschritt sichtbar.

### Verbleibende Download-Daten · 4.3.9

Die Download-Tabelle enthält eine eigene Spalte **Restdaten**. Sie zeigt die noch zu ladende Datenmenge unabhängig von der verbleibenden Zeit und wird numerisch sortiert. Gespeicherter und laufender Fortschritt werden berücksichtigt; der Wert wird nie negativ. Bei unbekannter Gesamtgröße erscheint ein Gedankenstrich.

### Sichererer Mac-Start und Gateway-Einrichtung · 4.3.9

Der native Mac-Start erkennt den alten relativen Standardpfad, auch ohne führenden Schrägstrich. Er verwendet ein bestätigtes vorhandenes Profil oder einen leeren Standardordner des Benutzers. Mehrdeutige benutzerdefinierte Pfade erfordern weiterhin eine ausdrückliche Auswahl.

Nach fehlgeschlagener oder abgebrochener Gateway-Vorbereitung wiederholen weitere Gateways und Hintergrundprüfungen die Autorisierungsanfrage erst nach einer ausdrücklichen Wiederholung der Netzwerkeinrichtung. Beim ersten privilegierten Start kann macOS weiterhin die normale Administratorbestätigung für das Ethernet-BPF-Gerät verlangen.

Die Mac-Netzwerkkomponente 4.3.2 berücksichtigt eine von macOS ausgeblendete Adapter-MAC und behält Schutzprüfungen für den physischen Adapter bei. Eigene Anwendungs-MAC, unveränderte OS-Routen und unveränderte physische MAC bleiben erhalten. Mac-Symbol und App-Name entsprechen der MultiRoute-Identität unter Windows. Beenden Sie die App vor dem PKG-Update. Der Aufrufername im realen Systemdialog sowie LAN/DHCP-Übertragungen müssen noch auf Hardware geprüft werden; dieser Transport benötigt weiterhin kabelgebundenes Ethernet.

**macOS:** `/Applications/MultiRoute Downloader.app` · `/Library/PrivilegedHelperTools/MultiRouteResume/4.3.2/{gateway-net,gateway-launch}`.

### Mac-Gateway-Antworten mit manueller Adresse · 4.3.9

Die POSIX-Erfassung wird korrigiert: Manuelle Anwendungsadresse und DHCP verwenden beide einen Empfangsmodus für Antworten an die eigene Anwendungs-MAC, entsprechend dem bisherigen Windows-Verhalten. Ein erfolgreicher System-Ping prüft die Verbindung des Computers, nicht den Empfang an der virtuellen Anwendungsadresse.

DHCP-Diagnosen behalten ihre Formulierungen in zwölf Sprachen statt einer allgemeinen Fehlermeldung. Die bisherige Autorisierung bleibt unverändert: Normale Netzwerkprüfungen verwenden die vorbereitete Komponente erneut. API-Endpunkte, Bedienelemente, OS-Routen und physische Adapter-MAC bleiben unverändert. Der erste privilegierte Ethernet-BPF-Zugriff kann weiterhin die normale Administratorbestätigung verlangen.

Dies behebt einen Unterschied im Erfassungsmodus, nicht jede Ursache unerreichbarer Gateways. Adress-, Subnetz- und DHCP-Lease/Konfliktprüfungen bleiben aktiv. Manuelle Mac-Übertragungen müssen noch auf Hardware bestätigt werden; kabelgebundenes Ethernet bleibt erforderlich.

<!-- current437:end -->

<a name="appearance-and-sounds"></a>

## Darstellung und Abschlusssignale

Wählen Sie aus neun Darstellungen: Graphite, Nordic, Streams Studio, Neon, Community, Chat, Studio, Rose und System. Graphite ist die Standardeinstellung. Graphite und System sind in Free enthalten; die anderen Optionen erfordern das entsprechende Pro-Recht für die Darstellung. Öffnen Sie Einstellungen → Darstellung, um den Stil und abgerundete Bedienelemente anzupassen. Ihre Downloads laufen während der Auswahl weiter. **Abgerundete Fenster und Schaltflächen** ist eine eigene Option. Die Bedienelemente unterstützen Rundungen in allen Stilen; den äußeren Fensterrahmen steuert das Betriebssystem. Unter Windows 11 können maximierte oder angedockte Fenster rechteckig bleiben; macOS und Linux bestimmen ihre Fensterkonturen selbst. Die Oberfläche bietet eine Geschwindigkeitsgrafik; die Netzwerkeinstellungen verwenden kompakte Gateway-Karten.

Abschlusssignale sind **standardmäßig ausgeschaltet**. Aktivieren Sie das Hauptkästchen und wählen Sie getrennt Signale **für jede Datei** und **für alle Downloads**. Zur Auswahl stehen **Sanfte Glöckchen**, **Piano** und **Marimba**; beide Melodien lassen sich vorhören. Sind alle Downloads fertig und ist das Gesamtsignal aktiv, erklingt für die letzte Datei nur die Schlussmelodie, ohne überlagertes Einzelsignal. Pausierte oder fehlgeschlagene Aufträge sind nicht abgeschlossen. Alte Abschlüsse werden beim Start oder bei Einstellungsänderungen nicht erneut abgespielt. Systemlautstärke und Audioausgabe bestimmen ebenfalls die Hörbarkeit.

Für eigene Klänge wählen Sie **Eigene Melodie** und getrennte **MP3- oder WAV-Dateien** für Datei- und Gesamtabschluss. Jeder Ausschnitt darf **höchstens 10 Sekunden** lang sein. Die Anwendung prüft die decodierte Audiodauer und vertraut nicht nur Dateiendung oder Metadaten. Akzeptierte Dateien werden in das lokale Anwendungsprofil kopiert; die Originale können danach verschoben werden.

Der optionale Desktop-Kurier stellt den Downloadfortschritt als sichtbare Last dar. Wählen Sie in den Darstellungseinstellungen den Mann oder die Frau und stellen Sie die Größe von 40% bis 100% ein. Der Sack zeigt die Summe der bekannten Restdatenmengen aktiver Downloads und schrumpft, während Bytes ankommen. Die Bewegung folgt ihrer Gesamtgeschwindigkeit; bei null steht der Kurier still. Kleine Lasten werden auf der Schulter getragen, größere gezogen; bei sehr großen Lasten ist die Anstrengung deutlicher sichtbar.

Mit der Schaltfläche Avatar zeigen oder verbergen Sie den Kurier. Ziehen Sie ihn an die gewünschte Position, doppelklicken Sie mit der linken Maustaste, um die Anwendung anzuzeigen, oder klicken Sie mit der rechten Maustaste für seine Optionen. Der Kurier ist ein visueller Begleiter: Dateien bleiben in den gewählten Ordnern.

Nach Abschluss legt der Kurier die gelieferte Last ab und blendet sich aus. Laufen Downloads weiter, kehrt er mit den verbleibenden Daten zurück.

Darstellung, Rundungen und Töne sind lokale Desktop-Einstellungen. Sie ergänzen keine Download-API-Endpunkte und ändern weder Übertragungen noch Prüfsummen- oder DNS-Sicherheitseinstellungen.

<a name="startup-profile-location"></a>

## Datenordner der Anwendung

Die native Anwendung verwendet einen gespeicherten absoluten Datenordnerpfad, unabhängig vom Arbeitsverzeichnis bei der Anmeldung. Downloadverlauf, Einstellungen und gespeicherte Teile liegen im gewählten Profil. Pfade der portablen Befehlszeilenversion werden unterstützt; ein ausdrückliches `--data-dir` legt den Datenordner fest.

<a name="automatic-startup"></a>

## Automatischer Start

Aktivieren Sie neben Sprache und Darstellung **Mit dem Betriebssystem starten**. Die Option ist **standardmäßig ausgeschaltet**. Sie registriert MultiRoute für die nächste Anmeldung Ihres Benutzerkontos; Ausschalten entfernt den eigenen Autostarteintrag. Dies ist ein Start bei der Benutzeranmeldung, kein Systemdienst und kein Start vor der Anmeldung. Zum Anlegen des Eintrags sind keine Administratorrechte erforderlich.

Der automatische Start verwendet den Infobereich, falls verfügbar; andernfalls bleibt ein normales erreichbares Fenster sichtbar. Prioritäten, Pausen und Zeitpläne bleiben wirksam. Autostart setzt keine pausierten Downloads zwangsweise fort. Öffnen Sie die Anwendung zunächst manuell, um Installation und Netzwerkvoraussetzungen abzuschließen. Verschieben Sie die registrierte Anwendung sowie bei portabler Nutzung Java und Bibliotheken nicht. Nach einem Umzug schalten Sie Autostart am neuen Ort aus und wieder ein. Deaktivieren Sie ihn vor der Deinstallation.

Das Betriebssystem oder die Desktoprichtlinie kann einen registrierten Eintrag deaktivieren. Prüfen Sie unter macOS bei einer Rückfrage **Systemeinstellungen → Allgemein → Anmeldeobjekte & Erweiterungen**; die Netzwerkberechtigung ist unabhängig davon. Eine Registrierung beweist keinen erfolgreichen künftigen Anmeldestart. Ein echter Neustart bzw. eine Abmeldung ist nicht Teil der Prüfungen mit eigenen Testprofilen; die Ergebnisse stehen im jeweiligen Releasebericht.

Falls die Anwendung nicht geöffnet werden kann, entfernen Sie **nur ihren eigenen Eintrag**:

- **Windows:** die Anwendung speichert ihren eigenen `REG_SZ`-Wert **MultiRoute Downloader** unter `HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Run`. Schalten Sie Autostart vorzugsweise in der App aus. Lässt sich diese nicht öffnen, löschen Sie im Registrierungs-Editor **nur diesen Wert**; behalten Sie den Run-Schlüssel und andere Einträge. Bei einer OS-Sperre verwenden Sie **Einstellungen → Apps → Autostart**; die App hebt diese Entscheidung nicht auf. Windows begrenzt den gesamten Startbefehl auf 260 Zeichen. Verwenden Sie bei Überschreitung einen kürzeren Anwendungs-/Profilpfad und registrieren Sie erneut.
- **macOS:** `~/Library/LaunchAgents/io.github.2018-spec-fdsds.multiroute-downloader.plist` entfernen.
- **Linux:** `${XDG_CONFIG_HOME:-~/.config}/autostart/io.github.2018-spec-fdsds.multiroute-downloader.desktop` aus dem tatsächlichen XDG-Konfigurationsverzeichnis entfernen. Erforderlich ist ein grafischer Desktop mit XDG-Autostart; diese Option installiert keinen systemd-Dienst.

<a name="dns-settings"></a>

## DNS-Einstellungen

Die Prüfungen für Plattformen und Installationspakete stehen in [NATIVE-INSTALLER-TESTS.json](../NATIVE-INSTALLER-TESTS.json).

Öffnen Sie **Netzwerk → DNS-Auflösung**. Standard ist **DNS über HTTPS (DoH), Cloudflare**. Die Einstellung gilt nur für MultiRoute; DNS-Server, Routen und IP-Adressen des Betriebssystems werden nicht geändert.

DoH unterstützt begrenztes HTTP/2 bei Auswahl durch TLS ALPN sowie HTTP/1.1, wenn dieses Protokoll ausgehandelt wird. Anbieterkompatibilität und reale Gateway-Ergebnisse stehen im passenden abschließenden Releasebericht.

- **DoH:** verschlüsselte DNS-Anfragen über HTTPS durch jedes gewählte Gateway. Der Hostname der URL wird gegen das TLS-Zertifikat geprüft.
- **DoT:** verschlüsselte DNS-Anfragen über TLS durch jedes gewählte Gateway, standardmäßig Port 853. Der Servername dient zur Zertifikatsprüfung.
- **System-DNS (Computer):** ausdrücklich die DNS-Richtlinie von Computer, Betriebssystem oder VPN verwenden. DNS muss dabei nicht jedem gewählten Gateway folgen; Dateiverbindungen behalten ihr Gateway. Dieser Modus garantiert keine DNS-Verschlüsselung.

Wählen Sie Cloudflare, Quad9, Google oder **Benutzerdefiniert**. Voreinstellungen sind fest; benutzerdefiniert lassen sich URL/Servername, Port und öffentliche numerische IPv4-Startadressen ändern. Diese Adressen vermeiden eine vorläufige Namensauflösung des DNS-Anbieters durch das Betriebssystem. DoH/DoT prüfen Vertrauenskette, Hostname und SNI. Es gibt keinen stillen Rückfall auf unverschlüsseltes oder System-DNS. Ein sicherer DNS-Fehler beendet den Versuch; der Anbieter muss über das gewählte Gateway erreichbar sein.

Bearbeiten Sie den Entwurf, wählen Sie Gateways und klicken Sie auf **DNS testen**. Der Test speichert nichts und zeigt je Gateway Ergebnis, Zeit und öffentliche IPv4-Adressen oder einen Fehler. Teilfehler bleiben sichtbar. Im Systemmodus bestätigt Erfolg die Betriebssystemauflösung, nicht einen unabhängigen Internetzugang jedes Gateways. **DNS-Einstellungen übernehmen** prüft und speichert die Konfiguration. Die zurückgegebene Revision kennzeichnet den Stand. Statusaktualisierungen und Sprachwechsel erhalten Entwurf, Cursor und Leseposition.

Jede Anfrage einschließlich DNSSEC-Zusatzabfragen hat insgesamt 8 Sekunden. Validierte TTL wird auch durch den Signaturablauf begrenzt. Der native Cache ist auf 300 Sekunden begrenzt und nach Gateway/Revision getrennt. TTL null und DoH HTTP Age bleiben wirksam. System-DNS folgt dem OS/JVM-Cache. Öffentliche IPv4-Ziele werden unterstützt; IPv6-Auflösung wird nicht unterstützt.

API: `GET /api/v1/network/dns` liefert Konfiguration und schreibgeschützten Status; `PUT` oder `POST` übernimmt Konfiguration. `POST /api/v1/network/dns/test` testet einen Entwurf; `GET /api/v1/network/dns/providers` listet Vorgaben. Nur Konfigurationsfelder senden, keine Revisions-/Statusfelder aus GET. [API](../API.ru.txt) · [OpenAPI](../openapi.yaml).

<a name="dnssec-validation"></a>

## Lokale DNSSEC-Prüfung

Unter **Netzwerk → DNS-Auflösung → DNSSEC-Validierung** wählen Sie **Signierte Domains prüfen** (Standard für DoH/DoT), **Sichere DNSSEC-Antworten verlangen** oder **DNSSEC deaktiviert**. Java prüft lokal mit dnsjava und mitgelieferten Root-Vertrauensankern. Das AD-Bit des Anbieters allein ist kein Nachweis. DoH/DoT verschlüsselt die Übertragung; DNSSEC bestätigt unabhängig die DNS-Daten.

**Prüfen** akzeptiert sichere und ungesicherte Antworten; **Verlangen** nur sichere. Ungesichert kann eine bestätigte unsignierte Delegierung oder nicht unterstützte Algorithmen bedeuten; lesen Sie den Grund. Ungültige und unbestimmte Ergebnisse werden bei aktivierter Prüfung abgelehnt. System-DNS deaktiviert lokale Prüfung. Die Rückkehr zu DoH/DoT stellt die letzte Richtlinie wieder her; Anbieterwechsel erhalten sie. Übernehmen speichert, DNS testen prüft den ungespeicherten Entwurf.

Pro Anfrage werden `secure`, `insecure`, `bogus`, `indeterminate` und `not_checked` unterschieden. Fehlende Ergebnisse erscheinen als nicht verfügbar. Eine erfolgreiche Verbindung beweist kein sicheres DNSSEC-Ergebnis. Ablauf, fehlende Schlüssel/Nachweise, Abfragefehler und Zeitüberschreitung haben eigene Gründe. Die Systemuhr muss stimmen. Zusatzabfragen verwenden dasselbe Gateway und das gemeinsame Zeitbudget. DNSSEC verbirgt keine Namen und prüft keine Dateiinhalte.

API: `dnssec` akzeptiert `off`, `validate`, `require`; System-DNS nur `off`. `dnssec_validation`, `dnssec_trust_anchors` und die Anfragefelder `dnssec_status`, `dnssec_reason_code`, `dnssec_reason`, `dnssec_validated_ttl`, `dnssec_aux_queries` sind nur lesbar. Nicht beim Speichern zurücksenden. Siehe API/OpenAPI oben und [DNSSEC](#dnssec-validation).

**HTTP-Bereichsverarbeitung:** Bei CDNs wie Civitai wird nach HTTP 200 für einen bei null beginnenden Bytebereich ein Bereich ab einem anderen Offset geprüft. Das erste Segment bleibt begrenzt; passende gespeicherte Daten eines sequenziellen Downloads können als Segmente fortgesetzt werden. Ein vorübergehendes HTTP 200 für einen späteren Bereich führt nach 2–30 Sekunden zu einem neuen Versuch, ohne gespeicherte Daten zu verwerfen oder den Gateway-Worker dauerhaft auszuschließen. Ein dauerhaftes Transferprotokoll je Gateway unterscheidet empfangene Daten von gespeichertem Fortschritt. [Transferdiagnose](transfer-diagnostics.en.md).

[Installationspakete 4.3.9](../downloads/v4.3.9/README.md) enthalten eine private Laufzeit Java 21. Windows Setup ist auf Englisch und erstellt eine Desktopverknüpfung; Npcap wird separat benötigt. macOS verwendet die systemeigene libpcap. Die Installations- und Sicherheitsbestätigungen stehen unten.

Projektinhaber: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## Zweck des Programms

MultiRoute Downloader ist ein Java-Desktopprogramm zum Herunterladen großer Dateien über mehrere Internet-Gateways im selben lokalen Ethernet-Netz. Alle ausgewählten Gateways beziehen Teile aus einer gemeinsamen Warteschlange. Ein schnelleres Gateway kann dadurch mehr Teile herunterladen; ein langsames Gateway hält die anderen nicht auf. Unterbrochene Übertragungen können fortgesetzt werden, soweit das Quellprotokoll dies sicher erlaubt.

Das ist besonders nützlich, wenn mehrere unabhängige Internetanschlüsse vorhanden sind, zum Beispiel ein kabelgebundener Festnetzanschluss und ein Mobilfunkrouter mit 4G/5G. Ihre Gateways müssen vom Rechner im selben Ethernet-LAN erreichbar sein. Das Programm nutzt alle ausgewählten, verfügbaren Gateways gleichzeitig für Dateisegmente und kann so die gemeinsame Downloadgeschwindigkeit erhöhen. Der tatsächliche Gewinn hängt von der unabhängigen Bandbreite der Anschlüsse und den Grenzen des Downloadservers ab.

Bei segmentierten Downloads können die übrigen Gateways weiterarbeiten, wenn ein einzelner Anschluss ausfällt. Noch nicht vollständig geladene Teile werden anderen verfügbaren Gateways zugewiesen; bereits geprüfte Daten bleiben erhalten. Außerdem unterstützt das Programm den gemeinsamen Download mehrerer Dateien: URL-Listen und eine Warteschlange erlauben Gruppen von Downloads, die sich auch über die lokale API aus anderen Anwendungen anlegen lassen.

Das Programm besitzt eine eigene Swing-Oberfläche, einen systemüblichen Ordnerdialog und eine lokale HTTP-API für andere Programme. Ein Webbrowser ist für die Bedienung nicht erforderlich. Die Oberfläche, registrierte Statusmeldungen und Hilfetexte sind in **Englisch, Russisch, Deutsch, Spanisch, Französisch, Chinesisch, Koreanisch, Arabisch, Italienisch, Japanisch, Hindi und Vietnamesisch** verfügbar. Bei einem neuen Profil ist Englisch voreingestellt. Die Sprachwahl wird gespeichert und sofort angewendet; Dateinamen, URLs und JSON-Schlüssel bleiben unverändert. Nicht registrierte Meldungen externer Server oder Bibliotheken können in ihrer ursprünglichen Sprache erscheinen.

<a name="application-virtual-ip"></a>

### Warum die Anwendung eine virtuelle IP verwendet

**Wählen Sie Gateways für MultiRoute, ohne die Verbindung anderer Programme zu ändern.** Für seinen internen Netzwerktransport verwendet das Programm eine eigene, freie lokale IPv4-Adresse. Damit baut es Verbindungen über die ausgewählten Gateways unabhängig vom Standardgateway des Computers auf, ohne die IP-Adressen der Betriebssystemadapter, die Routingtabelle oder das Standardgateway des Systems zu ändern. Browser und andere Programme behalten ihre bisherigen Netzwerkeinstellungen.

Beispiel für ein LAN `192.168.1.0/24`: Der Computer behält seine normale Adresse `192.168.1.x`, MultiRoute kann die freie Adresse `192.168.1.70` verwenden, und Downloads können über `192.168.1.1`–`192.168.1.4` laufen. Computer, virtuelle Adresse und ausgewählte Gateways müssen im selben tatsächlichen IPv4-Subnetz liegen. Prüfen und übernehmen Sie eine freie Adresse in den Netzwerkeinstellungen der Anwendung; tragen Sie sie nicht manuell am Betriebssystemadapter ein. Die virtuelle IP ist kein weiterer Internetanschluss: Die Bandbreite liefern die Router und ihre Internetleitungen.

#### Adresse auswählen und prüfen

1. Prüfen Sie IPv4-Adresse und Subnetzmaske des Ethernet-Adapters am Computer. Wählen Sie eine nutzbare Geräteadresse im selben Subnetz. `192.168.1.70` ist nur ein Beispiel; verwenden Sie weder die eigene Computeradresse noch eine Gateway-, Netzwerk- oder Broadcastadresse.
2. Öffnen Sie die Routerverwaltung und prüfen Sie den tatsächlichen DHCP-Vergabebereich. Nutzen Sie eine Adresse außerhalb der automatischen Vergabe oder reservieren/sperren Sie sie auf dem DHCP-Server. Bedienen mehrere DHCP-Server dieses LAN, muss die Adresse bei allen vor einer anderen Vergabe geschützt sein.
3. Wählen Sie in MultiRoute für die virtuelle Adresse **Prüfen und übernehmen** und prüfen Sie anschließend die Gateways. Die Anwendung prüft das Subnetz und mögliche ARP-Adresskonflikte. Keine ARP-Antwort bedeutet nur, dass momentan kein Konflikt erkannt wurde; dies beweist weder den Ausschluss aus dem DHCP-Pool noch verhindert es eine spätere Vergabe.

## Screenshots

Unten sehen Sie die aktuellen Anwendungsfenster. Öffnen Sie das Bild in voller Größe, um einzelne Bedienelemente zu lesen. Die beispielhaften Übertragungswerte dienen der Veranschaulichung.

![MultiRoute Downloader — Downloadwarteschlange und Gateway-Geschwindigkeiten](images/v4.3.7/downloads-en-ae5b55131583.png)

*Aktuelle Windows-Oberfläche, aufgenommen mit einem isolierten Demoprofil. Beispieldateien, Fortschritt, Geschwindigkeiten und mit Pro freigeschaltete Ansichten veranschaulichen die Bedienelemente und sind keine Leistungsmessungen.*

<table>
<tr>
<td width="50%" valign="top"><strong>Anwendungseinstellungen</strong><br /><a href="images/v4.3.7/settings-8abcb12a782d.png"><img src="images/v4.3.7/settings-8abcb12a782d.png" width="420" alt="Anwendungseinstellungen" /></a></td>
<td width="50%" valign="top"><strong>Darstellung, Kurier und Klänge</strong><br /><a href="images/v4.3.7/appearance-cebf121d5de7.png"><img src="images/v4.3.7/appearance-cebf121d5de7.png" width="420" alt="Darstellung, Kurier und Klänge" /></a></td>
</tr>
<tr>
<td width="50%" valign="top"><strong>Anwendungsadresse und geschütztes DNS</strong><br /><a href="images/v4.3.7/network-dns-e29dfdba1007.png"><img src="images/v4.3.7/network-dns-e29dfdba1007.png" width="420" alt="Anwendungsadresse und geschütztes DNS" /></a></td>
<td width="50%" valign="top"><strong>Gateway-Auswahl und Geschwindigkeitstests</strong><br /><a href="images/v4.3.7/network-gateways-aee0a7897f48.png"><img src="images/v4.3.7/network-gateways-aee0a7897f48.png" width="420" alt="Gateway-Auswahl und Geschwindigkeitstests" /></a></td>
</tr>
</table>






## Erforderliche Umgebung

Die Windows/macOS-Installer enthalten eine eigene Java-21-Laufzeit und die kompilierte Netzwerkkomponente. Windows benötigt zusätzlich den Npcap-Treiber. Java muss nicht separat installiert werden.

| Plattform | Betriebssystem | Komponenten |
| --- | --- | --- |
| Windows x64 | Windows 11 | Java 21 |
| Apple Silicon | macOS 26+ | Java 21 |
| Intel Mac | macOS 15+ | Java 21 |

Benötigt werden ein kabelgebundener Ethernet-Adapter im selben IPv4-Subnetz wie alle gewählten Gateways, eine freie virtuelle IPv4-Adresse, beschreibbare Daten-/Downloadordner und Platz für Datei und temporäre Teile. Der Geschwindigkeitsgewinn hängt von unabhängigen Internetleitungen und dem Quellserver ab. Eine GPU ist nicht erforderlich.

Die Anwendung unterstützt Linux; in diesem Distributionsrepository ist jedoch kein Linux-Installer der Version 4.x veröffentlicht. Windows- und macOS-Pakete sind nicht für Linux geeignet.

Warten Sie vor einem Update, bis aktive Downloads, Uploads und die abschließende Dateiprüfung beendet sind. Beenden Sie die Anwendung anschließend über das Tray und starten Sie den vollständigen neuen Installer. Bewahren Sie Datenordner, heruntergeladene Dateien und gespeicherte Teile auf.

## Netzwerk und virtuelle IP-Adresse

Diese bearbeitbaren Gateway-Adressen dienen als Beispiele. Wählen Sie die Gateways für Ihr LAN manuell: Free erlaubt bis zu **2 gleichzeitig verwendete Gateways**, das entsprechende Pro-Recht bis zu **8**:

```text
192.168.1.1
192.168.1.2
192.168.1.3
192.168.1.4
```

Die virtuelle Adresse der Anwendung ist standardmäßig **`192.168.1.70`**. Passen Sie diese Adressen an Ihr eigenes Netz an. Das Programm unterstützt **1 bis 8 unterschiedliche Gateways**, die über einen geeigneten physischen Ethernet-Adapter in einem gemeinsamen lokalen IPv4-Subnetz erreichbar sein müssen. Das vom Betriebssystem benutzte Gateway wird nicht zusätzlich als fünfter Weg angelegt.

Die virtuelle IP gehört zum **privaten Netzwerkstack der Anwendung**. Sie ermöglicht unabhängige Verbindungen über die gewählten Gateways, ohne die Schnittstellenadressen oder Routingtabelle des Betriebssystems und damit die Verbindungen anderer Programme zu ändern. Es ist keine zusätzliche Adresse, die Sie zuvor im Betriebssystem eintragen müssen. Sie muss jedoch im LAN frei sein und zum Subnetz passen; reservieren Sie sie möglichst außerhalb des DHCP-Vergabebereichs. Die Anwendung prüft einen Adresskonflikt vor der Nutzung.

WLAN, reine VPN-Adapter und IPv6-Verbindungen werden von diesem Ethernet-Transport nicht unterstützt. Auch Verbindungen zu lokalen LAN-BitTorrent-Peers werden nicht über ihn geführt. Bei mehreren passenden Ethernet-Adaptern kann die Auswahl als mehrdeutig abgelehnt werden. Ein nicht erreichbares Gateway wird durch die Anwendung nicht zu einem nutzbaren Internetanschluss.

## Native Installationspakete 4.3.9: Windows und macOS

| System | Installationsdatei | Start nach der Installation |
| --- | --- | --- |
| Windows 11 x64 | [MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-Windows-x64-Setup.exe) | **MultiRoute Downloader** im Startmenü oder über die Desktopverknüpfung |
| Apple Silicon, macOS 26+ | [MultiRoute-Downloader-4.3.9-macOS-arm64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-arm64.pkg) | **MultiRoute Downloader** in Programme, Spotlight oder Dock |
| Intel, macOS 15+ | [MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/download/v4.3.9/MultiRoute-Downloader-4.3.9-macOS-x86_64.pkg) | **MultiRoute Downloader** in Programme, Spotlight oder Dock |

Die zwölf Sprach-Anleitungen und Screenshots sind auch offline enthalten. Windows: `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`. macOS: im Finder beim installierten Programm **Paketinhalt zeigen** wählen, dann `Contents/app/docs/` öffnen. `en.md`, `ru.md`, `de.md`, `es.md`, `fr.md`, `zh.md`, `ko.md`, `ar.md`, `it.md`, `ja.md`, `hi.md` oder `vi.md` in einem Text- oder Markdown-Programm öffnen.

### Windows: installieren und öffnen

1. Laden Sie die `.exe` herunter und vergleichen Sie deren SHA-256 mit dem Release. Öffnen Sie die Datei und folgen Sie dem Installationsassistenten. Die Installation gilt für Ihr Benutzerkonto; wählen Sie die angebotenen Verknüpfungsoptionen.
2. Öffnen Sie **MultiRoute Downloader** im Startmenü oder über die Verknüpfung. Ein normales Programmfenster erscheint, ohne Kommandozeilenfenster. Java ist enthalten und muss nicht separat installiert werden.
3. Das Programm prüft Npcap. Fehlt der Treiber, bestätigen Sie seinen Download im Programm. Es lädt den festgelegten offiziellen Installer, prüft SHA-256, Authenticode-Signatur und Herausgeber und öffnet die normale Npcap-Installation. Lesen Sie die Lizenz und bestätigen Sie Windows UAC. Nach einem erforderlichen Neustart öffnen Sie MultiRoute erneut. Npcap bleibt ein separater Treiber: **Er wird nicht mitgeliefert oder still installiert**. Wenn er fehlt, ist für diesen Schritt Internetzugang nötig.

Programm und Installer haben keine Herausgebersignatur. SmartScreen kann einen unbekannten Herausgeber melden: Prüfen Sie das offizielle Release und die Datei, bevor Sie gegebenenfalls **Weitere Informationen → Trotzdem ausführen** wählen. Eine Malware-Erkennung ist ein anderer Fall. Deaktivieren Sie keinen Virenschutz und legen Sie keine Ausnahme als Installationsschritt an. Eine AdminOnly-Npcap-Installation kann eine weitere Autorisierung für den Gerätezugriff erfordern. Starten Sie die Java-Anwendung als normalen Benutzer.

### macOS: installieren und öffnen

1. Wählen Sie die `.pkg` für die **native** CPU: arm64 für Apple Silicon, x86_64 für Intel. Die neuen Pakete benötigen **macOS 26+ auf Apple Silicon** beziehungsweise **15+ auf Intel**, einschließlich Torrent/magnet. Die ältere portable HTTP-Mindestversion gilt für diese Pakete nicht.
2. Laden Sie die `.pkg` herunter, vergleichen Sie die veröffentlichte SHA-256 und öffnen Sie Apples Installer. Bestätigen Sie Installation und Administratorfreigabe zum Einrichten des geschützten Netzwerkhelfers.
3. Öffnen Sie **`/Applications/MultiRoute Downloader.app`** in Finder oder Spotlight. Das Symbol kann ins Dock übernommen werden. Java und der fertig kompilierte Helfer sind enthalten: **Terminal, Command Line Tools, Homebrew und eine separate Java-Installation werden für dieses Paket nicht benötigt**.
4. Bestätigen Sie bei Bedarf den normalen macOS-Administratordialog, damit der geschützte Helfer das Ethernet-BPF-Gerät öffnen kann. Anschließend gibt er die erhöhten Rechte ab; das Java-Fenster läuft als normaler Benutzer. Während der Nutzung ist keine Terminalsitzung nötig.

### macOS: Sicherheitsbestätigungen des nativen Pakets

Das Paket ist **nicht mit Developer ID signiert und nicht notarisiert**. Eine technische Ad-hoc-Signatur bestätigt keine Apple-Entwickleridentität. Versuchen Sie zunächst, `.pkg` oder Anwendung normal zu öffnen. Falls macOS einen nicht verifizierten Entwickler meldet und eine Ausnahme anbietet, wählen Sie **Systemeinstellungen → Datenschutz & Sicherheit → Dennoch öffnen**, dann **Öffnen**, erst nach Prüfung von Release-Herkunft und Hash. Ein verwalteter Mac kann dies verbieten. Siehe [Apple](https://support.apple.com/de-de/102445).

Installationsfreigabe, spätere Helferfreigabe und Entwicklerausnahme sind getrennte Bestätigungen. Erlauben Sie bei Nachfrage **Lokales Netzwerk** für MultiRoute/Java, damit Ihre LAN-Gateways erreichbar sind. Einen Ordnerzugriff erlauben Sie für das gewählte Downloadziel. Eine abgelehnte Abfrage bedeutet nicht, dass die gesamte Java-Anwendung als root gestartet werden soll.

Der Installer deaktiviert weder Gatekeeper/SIP noch globale Quarantäneprüfungen, verändert keine Systemrouten und richtet keine globale `sudoers`-Ausnahme ein. Eine Warnung vor Malware oder einer beschädigten Datei ist keine normale Entwicklerbestätigung: Halten Sie an und prüfen Sie die Datei. Der Dialogtext kann je nach macOS und Organisationsrichtlinien abweichen.

## Erster Download

1. Wählen Sie oben im Fenster bei **Language** die Sprache **Deutsch**.
2. Öffnen Sie die Netzwerkeinstellungen. Bearbeiten oder ergänzen Sie die Gateways und wählen Sie eine freie virtuelle IP. Die sichtbare Hilfe **(?)** erklärt deren Zweck. Hinweise zu Feldern erscheinen beim Darüberfahren nach etwa 250 ms.
3. Prüfen und übernehmen Sie die virtuelle Adresse, dann prüfen Sie die Gateways. Wählen Sie mindestens eines aus.
4. Fügen Sie die Datei-URL mit **Ctrl+V**, auf macOS mit **Cmd+V**, oder über die Einfügeschaltfläche in das lange Linkfeld ein. Links sind nicht auf 128 Zeichen begrenzt.
5. Wählen Sie das Downloadziel über den systemüblichen Ordnerdialog. Bei einem neuen Profil ist dies der Ordner `Downloads` Ihres Benutzerkontos.
6. Starten Sie den Download mit der Schaltfläche direkt neben dem Linkfeld. Bei Quellen mit mehreren Dateien bestätigen Sie zuerst die gewünschte Auswahl.
7. Beobachten Sie Gesamtgeschwindigkeit, vergangene und geschätzte verbleibende Zeit sowie die Geschwindigkeit und Datenmenge jedes Gateways. Die optionale Grafik lässt sich einblenden.

**Der Geschwindigkeitstest lädt höchstens 16 MiB oder misst bis zu fünf Sekunden. Diese Grenzen gelten nur für den Test**, nicht für normale Downloads. Ohne direkte HTTP-Datei-URL, etwa bei magnet oder einer Repository-Seite, verwendet er eine Selectel-Testdatei. Er misst keine vollständige BitTorrent-Verfügbarkeit. Für die eigentlichen Downloads können Sie separate Warteschlangen- und Geschwindigkeitslimits festlegen.

HTTP-Segmente sind standardmäßig **20 MiB** groß, einstellbar von 1 bis 64 MiB. Alle Gateways holen neue Teile, sobald sie frei sind. Nach **60 Sekunden ohne neue Daten** wird eine festhängende Übertragung geschlossen und ihr Teil für andere ausgewählte Gateways freigegeben; zwei Gateways schreiben denselben Teil nicht gleichzeitig. Bei BitTorrent gibt die Quelle die Teilgröße vor.

Sie können Gateways während eines laufenden Downloads hinzufügen oder entfernen. Ein neu angelegtes Gateway wird einem vorhandenen Auftrag über dessen Gateway-Auswahl hinzugefügt, nicht automatisch allen Aufträgen. Bei jedem unfertigen Auftrag muss mindestens eines verbleiben. Bereits geprüfte Teile bleiben erhalten. Eine entfernte Profilzeile löscht keine heruntergeladenen Dateien.

Wählen Sie mehrere Download-Zeilen mit **Strg+Klick (Ctrl)** unter Windows/Linux oder **Cmd+Klick** unter macOS; **Umschalt+Klick (Shift)** wählt einen Bereich. Wenn die Tabelle den Fokus hat, wählt **Strg+A** oder **Cmd+A** alle gerade sichtbaren Zeilen unter Berücksichtigung von Suche und Statusfilter. Die Löschaktion gilt für die gesamte Auswahl mit einer gemeinsamen Bestätigung. Die Option zum zusätzlichen Löschen fertiger Dateien und geladener Teile ist **standardmäßig nicht angekreuzt**; so entfernen Sie nur die Listeneinträge und behalten die Daten. Aktivieren Sie sie nur, wenn die Dateien dieser Aufträge gelöscht werden sollen; andere Dateien und der Zielordner bleiben erhalten. Das Ergebnis zeigt die Anzahl entfernter Einträge. Fehlgeschlagene Aufträge bleiben mit einer Fehlerübersicht in der Liste, damit Sie sie prüfen und das Entfernen wiederholen können; erfolgreiche Entfernungen werden nicht wiederholt. Das dauerhafte Download-Verlaufsprotokoll bleibt beim Entfernen von Aufträgen oder Dateien erhalten.

## Warteschlange und mehrere Links

In den Warteschlangen- und Limit-Einstellungen legen Sie die Zahl gleichzeitig aktiver Aufträge fest; standardmäßig sind es **zwei**. Auftragsprioritäten, globale und einzelne Gateway-Limits sowie ein Zeitplan nach der lokalen Uhr des Computers sind einstellbar. Ein Geschwindigkeitslimit von `0` bedeutet keine Begrenzung. Gemessen werden Nutzdaten, ohne Netzwerkheader. Manuell pausierte Aufträge werden nicht einfach durch die Warteschlange neu gestartet.

Der Linkimport akzeptiert mehrere Zeilen, TXT-Dateien sowie hineingezogene URLs oder `.torrent`-Dateien. Eine Vorschau ermöglicht die bewusste Auswahl; bis zu 1000 Links pro Import werden in API-Paketen von höchstens 128 verarbeitet. Suche, Statusfilter und Sortierung erleichtern die Auswahl alter Aufträge. Die angezeigte Restzeit ist eine Schätzung anhand der aktuellen Geschwindigkeit und enthält nicht die abschließende Prüfung oder Zusammenstellung.

## Unterstützte Quellen und Integritätsprüfung

| Quelle | Verhalten |
| --- | --- |
| HTTP/HTTPS | Verteilung über Range-Anfragen mit Antwort 206, wenn die Quelle eine sichere Teilübertragung erlaubt. TLS-Zertifikate und Hostnamen werden geprüft. |
| HTTP ohne Range | Ein kompletter Datenstrom über **ein** gewähltes Gateway, auch ohne `Content-Length`. Bei Abbruch oder Wegwechsel beginnt dieser Strom bei Byte 0. |
| FTP/FTPS | Passives FTP beziehungsweise explizites FTPS mit TLS für Steuer- und Datenverbindung. Für sichere Segmentierung ist eine vertrauenswürdige SHA-256 oder SHA-512 erforderlich; sonst wird vollständig über ein Gateway übertragen. |
| SFTP | SSH/SFTP mit vorher unabhängig geprüftem, festgelegtem **SHA-256-Hostschlüssel-Fingerabdruck**. Ein abweichender Schlüssel wird abgelehnt. Eine vertrauenswürdige Datei-SHA-256 erlaubt sichere Segmentierung. |
| WebDAV | HTTPS-Auflistung und Dateiauswahl; Quelladresse `webdavs://…` gemäß API-Schema. |
| Metalink | `.meta4` mit Dateiauswahl, Spiegeln und veröffentlichten Datei-/Teilhashes. Spiegel werden nur bei vertrauenswürdiger SHA-256 als dieselbe Datei verwendet; gleiche Dateigröße allein reicht nicht. |
| Hugging Face | Repository-/`tree`-/`blob`-/`resolve`-Links, bewusste Dateiauswahl, GGUF-Filter, LFS-/Xet-Metadaten und HTTP-Download. Python oder `hf_xet` sind nicht nötig. Private Quellen benötigen den ausdrücklich eingetragenen Zugang und die entsprechende Serverberechtigung. |
| BitTorrent | Lokale `.torrent`, direkte Torrent-URL und magnet; Metadaten-/Teilhashprüfung, Datei-Auswahl, Peer-/Seed-/Trackerstatus und Datei-Prioritäten. Ausgewählte Gateways transportieren Peer-, Tracker-, DHT- und Webseed-Daten. |

BitTorrent-Seeding ist **standardmäßig ausgeschaltet**. Es kann mit Zeit- und Verhältnislimit aktiviert werden und verwendet die gewählten Gateways. Magnet-Metadaten benötigen erreichbare Teilnehmer; ein Link allein garantiert keinen Download. SSL-Torrents mit separatem Clientzertifikat sowie IPv6-/LAN-Peers werden von diesem Release nicht unterstützt.

Eine veröffentlichte SHA-256 sucht die Anwendung unter anderem in Hugging-Face-LFS-Metadaten, `Repr-Digest`, `filename.sha256` und `SHA256SUMS`. Eine Summe eines Teil-HTTP-Antwortkörpers wird nicht als Summe der ganzen Datei ausgegeben. Sie können eine vertrauenswürdige Gesamt-SHA-256 auch manuell eintragen.

Nach Zusammenstellung wird die SHA-256 des Ergebnisses berechnet. Bei Abweichung vom bekannten Referenzwert wird das Ergebnis nicht als fertige geprüfte Datei veröffentlicht. **Nicht jede Quelle veröffentlicht eine Referenzsumme**: ohne sie ist die angezeigte SHA-256 nur ein lokal berechneter Wert und keine bestätigte Übereinstimmung mit dem Quellserver. BitTorrent prüft die in seinen Metadaten festgelegten Teilhashes; eine lokale Gesamt-SHA-256 ist davon zu unterscheiden. Für eine sichere HTTP-Fortsetzung ist eine geeignete starke Quellidentität, etwa ein starker ETag oder eine vertrauenswürdige Gesamt-SHA-256, erforderlich; ungeeignete Quellen können abgelehnt werden.

Unter **Source access** lassen sich Basic/Bearer-Zugang, Cookies, erlaubte Header und FTP-/SFTP-Zugang ausdrücklich konfigurieren. Sie gelten für den genauen Quell-Origin und werden nicht einem anderen Server beim Weiterleiten mitgegeben. Das lokale AES-GCM-Zugangsarchiv ist benutzerbeschränkt. Browser-Cookies und fremde Schlüssel werden nicht automatisch gelesen. Diese optionalen Quellzugänge sind keine Gateway-Tokens.

<a name="multiroute-api"></a>

## Lokale API für andere Programme

Basisadresse: **`http://127.0.0.1:8765/api/v1`**. Die Anwendung muss laufen. Die API verwendet JSON/UTF-8 und benötigt keinen Gateway- oder API-Token. Sie ist auf denselben Computer beschränkt und bietet keine Remote-Authentifizierung. Anfragen aus fremden Browser-Origins werden abgelehnt; für eine Browserintegration ist ein lokaler serverseitiger Vermittler erforderlich.

JSON-Schlüssel und Statuswerte bleiben bei jedem Sprachwechsel stabil. Die im Fenster markierten Gateways werden in externen API-Anfragen **nicht automatisch übernommen**; geben Sie `routes` ausdrücklich an.

Speichern Sie dieses Beispiel als `job.json`, ersetzen Sie URL und Zielordner:

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

Unter Linux/macOS ist beispielsweise `"directory": "/home/your-user/Downloads"` beziehungsweise `"/Users/your-user/Downloads"` zu verwenden. Ein JSON-Pfad mit Windows-Backslashes muss diese verdoppeln. Bei Bedarf kann `expected_sha256` als vertrauenswürdiger 64-stelliger Hexwert ergänzt werden.

```sh
curl --request POST --header "Content-Type: application/json" --data-binary @job.json http://127.0.0.1:8765/api/v1/jobs
curl http://127.0.0.1:8765/api/v1/health
curl http://127.0.0.1:8765/api/v1/jobs
```

Verwenden Sie in Windows PowerShell ausdrücklich **`curl.exe`**, falls `curl` ein PowerShell-Alias ist. Ein `201` bestätigt die Auftragserstellung, nicht den abgeschlossenen Download. Speichern Sie die zurückgegebene `id` und verfolgen Sie `GET /api/v1/jobs/{id}`. Ein vorhandener kompatibler Auftrag kann mit `reused: true` zurückgegeben werden und muss gegebenenfalls ausdrücklich fortgesetzt werden.

| Anfrage | Zweck |
| --- | --- |
| `GET /health` | Verfügbarkeit und Version |
| `POST /jobs` | Einen Auftrag erstellen |
| `POST /jobs/batch` | Mehrere Aufträge, bis zu 128 Eingaben pro Paket |
| `POST /jobs/{id}/pause`, `POST /jobs/{id}/resume` | Pausieren beziehungsweise fortsetzen |
| `PUT /jobs/{id}/routes` | Vollständigen Gateway-Satz dieses Auftrags ändern |
| `DELETE /jobs/{id}` | Auftrag entfernen, Dateien behalten |
| `DELETE /jobs/{id}?delete_files=true` | Auch die Dateien und Teile dieses Auftrags entfernen |
| `GET /network`, `POST /network` | Virtuelle IP und Gateway-Profile lesen beziehungsweise konfigurieren |
| `GET /queue`, `PUT /queue` | Parallelität, Limits und lokalen Zeitplan |
| `POST /sources/inspect`, `POST /sources/download` | Quell-Dateien auflisten beziehungsweise gewählte Dateien laden |
| `GET /sources/auth`, `PUT /sources/auth` | Explizite, optionale Quellzugänge verwalten |
| `GET /events` | Ereignisstrom im SSE-Format |

Alle Pfade in der Tabelle beziehen sich auf `/api/v1`. Vollständige Felder und Fehler sind im [OpenAPI-Schema](../openapi.yaml) dokumentiert; weitere ausführliche Beispiele enthält die [API-Anleitung auf Russisch](../API.ru.txt).

## Daten, Aktualisierung und Fehlersuche

### Native Pakete 4.3.9

| System | Einstellungen, Verlauf und Protokolle | Programm/Laufzeit | Netzwerkhelfer |
| --- | --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App`, einschließlich Java | Enthaltener x64-Helfer; Npcap separat installiert |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app`, einschließlich Java | `/Library/PrivilegedHelperTools/MultiRouteResume/` |

<a name="systemtray-und-beenden-40"></a>

## Systemtray und Beenden

Beim Schließen des Hauptfensters laufen Anwendung und Downloads im Systemtray weiter. Das Symbol zeigt den gesamten Download-Durchsatz **aller Aufträge**, unabhängig von Auswahl und Tabellenfilter. Die Tooltip-Anzeige enthält den genauen Wert in Mbit/s oder Gbit/s (ab 1 Gbit/s) und Byte/s, auch bis 10 Gbit/s; das kleine Symbol rundet ihn. Im Tray-Menü **Anwendung öffnen** wählen, um das Fenster wiederherzustellen. Unter Windows/Linux öffnet auch ein Klick auf das Symbol das Fenster. **Anwendung beenden** im Tray-Menü oder Fenster speichert den Zustand und beendet das Programm. Vor einem Update diese Funktion verwenden.

Ohne Systemtray wird das Fenster in die Taskleiste minimiert; Downloads laufen weiter und Beenden bleibt verfügbar. Verschwindet das Tray, wird das verborgene Fenster wieder geöffnet. Windows kann das Symbol im ausgeblendeten Benachrichtigungsbereich ablegen.

Das Setup erstellt standardmäßig eine Desktopverknüpfung und unter Windows einen Startmenüeintrag. macOS erstellt für den angemeldeten Benutzer einen Desktoplink zur installierten `.app`; beim ersten grafischen Start wird es erneut versucht. Vorhandene Desktopobjekte werden erhalten. Bei Datenschutzbeschränkungen den angeforderten Zugriff erlauben oder die App aus Programme öffnen. Unsere Setuptexte sind immer Englisch; Systemdialoge für Sicherheit und Berechtigungen folgen der Betriebssystemsprache.

## Lizenz

Die Originalanwendung darf kostenlos installiert und privat sowie in Organisationen einschließlich geschäftlicher Tätigkeiten genutzt werden. **Verkauf der Anwendung, kostenpflichtige Lizenzen und entgeltliche Weitergabe sind untersagt**, soweit keine separate Erlaubnis vorliegt. Die eigene Lizenz gewährt keine allgemeinen Änderungs- oder Weitergaberechte und ist keine von OSI anerkannte Open-Source-Lizenz. Drittanbieter behalten ihre eigenen Lizenzbedingungen. Dies ist eine übersetzte Zusammenfassung; maßgeblich ist der englische Text in [LICENSE.txt](../LICENSE.txt).

Das Geschwindigkeitssymbol hat einen transparenten Hintergrund mit schwarzen Ziffern und Einheiten, auch bei 10 Gbit/s. Es zeigt die Gesamtrate und bietet die oben beschriebenen Tray-Befehle.

## Prüfung mit MD5, SHA-256 und SHA-512

Nach dem Zusammensetzen berechnet die Anwendung alle drei Prüfsummen der gesamten Datei in einem Lesedurchlauf. Fügen Sie eine Referenz mit Strg+V oder Cmd+V in das optionale Hauptfeld ein. Der Typ wird automatisch erkannt: 32 Hexadezimalzeichen bedeuten MD5, 64 SHA-256 und 128 SHA-512. Der aufklappbare Bereich für zusätzliche Prüfsummen erlaubt mehrere Referenzen.

Veröffentlichte Referenzen werden aus unterstützten Digest-Headern der gesamten Repräsentation und Begleitdateien ermittelt: `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512` und `SHA512SUMS`. Die Prüfsumme einer partiellen HTTP-Antwort gilt nicht als Prüfsumme der ganzen Datei. Die bisherigen Prüfungen für Hugging Face LFS und Metalink bleiben erhalten.

Alle bekannten Referenzen müssen vor der Veröffentlichung der fertigen Datei übereinstimmen. Bei Abweichung schlägt der Auftrag fehl; Teildaten bleiben erhalten. Ohne Referenz ist die Anzeige eine lokal berechnete Summe und keine bestätigte Übereinstimmung mit der Quelle. MD5 erkennt zufällige Beschädigungen; SHA-256 oder SHA-512 bieten eine stärkere Integritätsprüfung. MD5 ersetzt weder sichere Dateiidentität noch Anforderungen an vertrauenswürdige Spiegel.

Torrent-Teile werden weiterhin zwingend geprüft. Die Summen der zusammengesetzten Torrent-Dateien sind davon getrennt. Eine manuelle Referenz für die ganze Datei erfordert genau eine ausgewählte Datei; bei Gruppen erhält jeder Auftrag seine eigene Referenz.

Die lokale API akzeptiert `expected_md5`, `expected_sha256` und `expected_sha512` in `POST /api/v1/jobs`. Ergebnisse enthalten `md5`, `sha256`, `sha512` und Referenzquellen in `checksum_sources`. Bestehende Anfragen mit `expected_sha256` bleiben kompatibel.

<a name="fortschritt-einzelner-downloads-40"></a>

## Fortschritt einzelner Downloads

Jeder Download hat einen eigenen Balken in der Spalte Fortschritt. Die Spalten füllen die Breite des Downloadbereichs. Bei vielen Aufgaben wird die Liste vertikal gescrollt; schmale Bereiche bieten auch horizontales Scrollen, ohne die Spalten unlesbar zu verkleinern. Beim Aktualisieren bleibt die erste sichtbare Aufgabe erhalten. Sortierung und Filter ordnen jeden Balken weiterhin seinem Download zu. Bei unbekannter Gesamtgröße werden empfangene Daten statt eines Prozentsatzes angezeigt; der Tooltip zeigt Größe, Status und Fehlerdetails. Die ausgewählte Zeile zeigt weiterhin ausführlichen Fortschritt, Zeiten und Gateway-Geschwindigkeiten.

Der Standard-Downloadordner wird beim Start für den aktuellen Benutzer ermittelt: `%USERPROFILE%\Downloads` unter Windows und `~/Downloads` unter macOS/Linux. Ein gespeicherter Standardordner folgt dem aktuellen Profil; ein manuell gewählter Ordner bleibt erhalten.

<a name="protokollanzeige-36"></a>

## Protokollanzeige

Das Protokollfenster behält beim automatischen und manuellen Aktualisieren die Leseposition bei. Am unteren Ende bleiben neue Einträge sichtbar. Beim Hochscrollen wird nur das Folgen pausiert; die Protokollierung läuft weiter. Die Textauswahl zum Kopieren bleibt erhalten. Beim Sprachwechsel bleibt das sichtbare Ereignis erhalten; nach erneutem Öffnen werden die neuesten Einträge angezeigt.

<a name="downloaddetails-und-prüfsummenbericht-37"></a>

## Downloaddetails und Prüfsummenbericht

Der untere Detailbereich des ausgewählten Downloads behält beim wiederholten automatischen Aktualisieren die Leseposition und Textauswahl bei. Das Ende des Prüfsummenberichts lässt sich lesen, ohne dass der Bereich an den Anfang zurückspringt. Bei Auswahl eines anderen Downloads öffnet sich dessen Bericht am Anfang. Das separate Protokollfenster behält sein bisheriges Scrollverhalten.

<a name="anleitung-und-api-anleitung-38"></a>

## Anleitung und API-Anleitung

Die Schaltflächen **Anleitung** und **API-Anleitung** befinden sich oben rechts in einer eigenen Zeile unter der Sprachauswahl. Sie öffnen die Anleitung in der gewählten Sprache im Standardbrowser. **API-Anleitung** führt direkt zum Abschnitt über die lokale API. Die Links entsprechen der Anwendungsversion; zum Öffnen der GitHub-Seiten ist Internetzugang erforderlich.

<a name="desktopoberfläche-und-downloadverlauf-40"></a>

## Desktopoberfläche und Downloadverlauf

Die Desktopoberfläche verwendet FlatLaf. In den **Darstellungseinstellungen** stehen **Graphite** und **System** zur Verfügung; weitere Stile erfordern das entsprechende Pro-Recht. Die Darstellung wird lokal gespeichert. Die Warteschlange behält einzelne Fortschrittsbalken, Spalten über die gesamte Breite sowie Sortierung und Filter. Eine Übersicht zeigt alle, aktive und abgeschlossene Aufträge sowie die Gesamtgeschwindigkeit aller Aufträge, unabhängig vom Tabellenfilter.

Bewegen Sie den Zeiger über eine laufende oder abgeschlossene Downloadzeile: Der Tooltip zeigt den **vollständigen Dateinamen und absoluten Zielpfad**, auch nach Sortierung oder Filterung. Der Titel des ausgewählten Downloads hat denselben Tooltip. Im Kontextmenü kopiert **Vollständigen Pfad kopieren** das Ziel.

**Strg+L** unter Windows/Linux oder **Cmd+L** unter macOS fokussiert die Downloadadresse; **Strg+F** oder **Cmd+F** die Suche; **Escape** leert die Suche. Strg+V/Cmd+V zum Einfügen bleibt verfügbar.

Der ausgewählte Download hat die Register **Details** und **Verlauf**. Der Verlauf enthält Ereignisse mit Datum und Uhrzeit, darunter den ersten Start, jede Fortsetzung und den erfolgreichen Abschluss. Für alte Aufträge ohne aufgezeichnete Zeiten werden keine Daten erfunden. Speicherorte und API-Felder stehen unten.

Die Anwendung unterstützt Downloadprotokolle, wählbare Gateways, virtuelle IP, Geschwindigkeitstests, Stapelimport, Quellenzugang, MD5/SHA-256/SHA-512, Tray, lokale API und lesbares Scrollen in Protokoll und Details. Die Oberfläche ändert keine Systemrouten.

### Speicherung des Verlaufs und API

Im Datenordner stehen `logs/downloads-history.jsonl` (UTF-8, ein JSON-Objekt je Zeile) und `logs/downloads-history.log` (lesbare Einträge mit Datum). Beide werden über Neustarts hinweg ergänzt und beim Entfernen eines Auftrags oder seiner Dateien nicht gelöscht. Das Textprotokoll behält die Sprache zum Schreibzeitpunkt; die Oberfläche zeigt gespeicherte Ereignisse in der aktuell gewählten Sprache. Das Protokollfenster lädt nur die letzten bis zu 1.000 Ereignisse / 2 MiB; das vollständige Journal bleibt auf der Festplatte und wächst mit der Nutzung. Archivieren Sie es bei Bedarf separat. Namen und vollständige Zielpfade sind enthalten, Quell-URLs, Kennwörter und Zugangstokens jedoch nicht. Prüfen Sie die Pfade vor dem Weitergeben.

Native Windows: `%LOCALAPPDATA%\MultiRoute Downloader\data\logs\`. Natives macOS: `~/Library/Application Support/MultiRoute Downloader/data/logs/`. Linux und portable Profile verwenden den oben beschriebenen Datenordner; `--data-dir` wählt einen anderen.

`GET /api/v1/jobs` und einzelne Auftragsantworten enthalten `lifecycle_history`. Ein Ereignis hat `event_id`, `job_id`, `kind`, `time`, `filename` und `path`; Torrent-Einträge können `paths`, Pausen `reason` enthalten. Arten: `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`. Zeitstempel sind ISO 8601 UTC, etwa `2026-10-18T12:34:56.123Z`; das Textprotokoll zeigt lokales Datum, Uhrzeit und UTC-Offset. Optionale `first_started_at`, `last_resumed_at`, `completed_at` fassen beobachtete Ereignisse zusammen. Ein Auftrag in der Warteschlange hat noch keine Startzeit. Abschluss bedeutet erfolgreiche Prüfung und Veröffentlichung. `recovered` bezeichnet die heutige Prüfung einer früher veröffentlichten Datei, keine bekannte frühere Abschlusszeit. Für alte Aufträge werden keine Daten erfunden. Die bisherigen API-Operationen bleiben verfügbar; ein eigener Verlauf-Endpunkt ist nicht erforderlich.

Das allgemeine **Protokoll** trennt **Anwendungsereignisse** und **Downloadverlauf**. Der Verlauf zeigt datierte Ereignisse verschiedener, auch entfernter Aufträge; der Verlauf beim ausgewählten Download enthält nur dessen Ereignisse.

## Gespeicherte Anwendungsprofile

Profile verwenden die oben genannten Datenpfade. Im gewählten Ordner liegen Einstellungen, Downloadverlauf und gespeicherte Teile; bewahren Sie diesen Ordner und Ihre heruntergeladenen Dateien bei einer Aktualisierung auf. Die installierte Anwendung verwendet das geschützte Netzwerkkomponenten-Verzeichnis `MultiRouteResume` und interne Anwendungskennungen. Starten Sie sie über die MultiRoute-Downloader-Verknüpfung oder die `.app`.

<a name="gespeicherte-websites-uploads-und-archivvorschau-40"></a>

## Gespeicherte Websites, Uploads und Archivvorschau

Öffnen Sie **Website-Profile…**, erstellen/bearbeiten Sie ein Profil und wählen Sie es unter **Downloads** oder **Uploads**. Gateways und virtuelle IP finden Sie unter **Netzwerkeinstellungen**. Unter **Uploads** geben Sie die entfernte **Ordner-URL** an und wählen mit **Dateien auswählen und hochladen…** eine oder mehrere lokale Dateien im Systemdialog. Die Oberfläche ergänzt die Namen; API `target_url` bezeichnet dagegen eine vollständige Datei-URL. Für eine Archivvorschau URL oben einfügen und **Archivinhalt…** anklicken; dabei beginnt kein Download.

Ein Websiteprofil speichert Name, Basis-URL, lokalen Downloadordner, entfernten Ordner und Gateways. Möglich sind anonymer Zugriff, HTTP Basic/Bearer/Cookie/eigene Header sowie FTP- und SFTP-Zugangsdaten mit vorher geprüftem SHA-256-Hostschlüssel. Mehrere Konten desselben Servers bleiben getrennt. Explizite Ordner/Gateways überschreiben die Vorgaben; relative URLs beziehen sich auf die Basis-URL. Geheimnisse liegen verschlüsselt in `site-profiles/vault.bin`, der lokale Schlüssel in `site-profiles/key.bin`, jeweils nur für den Besitzer. Wer das Benutzerkonto und den Schlüssel kontrolliert, kann sie dennoch lesen. Öffentliche API-Antworten enthalten keine Zugangsdaten. Leere Felder behalten Geheimnisse nur bei gleichem Origin und Authentifizierungstyp; `replace_credentials:true` ersetzt sie ausdrücklich. Fremde HTTP-Redirect-Origins erhalten keine Profilzugangsdaten. Profil löschen entfernt keine Aufgaben oder lokalen/entfernten Dateien; abhängige Aufgaben behalten ihre ursprüngliche Profilbindung und können das Konto nicht wechseln; erstellen Sie die Aufgabe bei Bedarf mit einem gültigen Profil neu.

Uploads und entfernte Verzeichnisse unterstützen **FTP, explizites FTPS und SFTP** über ausgewählte Anwendungsgateways. Geben Sie eine reguläre lokale Datei und eine vollständige entfernte Datei-URL (`target_url`) an. Ein temporäres Ziel ermöglicht Fortsetzen nach Prüfung des vorhandenen Präfixes. Erfolg erfordert vollständiges Zurücklesen mit SHA-256; das Konto benötigt **Lese-, Schreib- und Umbenennungsrechte**. Dies verbraucht zusätzlichen Datenverkehr. Eine abweichende Prüfsumme oder verweigertes Lesen ist ein Fehler. Jede Datei nutzt jeweils ein Gateway; mehrere Dateien laufen parallel und werden auf Gateways verteilt. Nach dem Leerlauflimit kann ein anderes Gateway übernehmen. Eine einzelne Datei wird beim Upload nicht parallel aufgeteilt. Aufgaben bleiben über Neustarts pausierbar/fortsetzbar. Entfernen einer Aufgabe lässt lokale sowie entfernte fertige/temporäre Dateien bestehen.

Überschreiben ist standardmäßig aus. FTP/SFTP bieten nicht überall atomisches Umbenennen ausschließlich bei fehlendem Ziel; ein anderer Client kann dasselbe Ziel zeitgleich ändern. Gemeinsame Ziele müssen koordiniert werden. Verzeichnisse auflisten lädt nichts hoch und löscht nichts.

Die Vorschau zeigt Namen, Pfade, Größen, Verzeichnisse und Verschlüsselung von **ZIP, TAR, 7Z, RAR4 und RAR5**, ohne Entpacken oder Ausführen. Erforderlich sind ein Gateway, Bytebereiche und ein stabiler ETag oder Last-Modified. Grenzen: 16 MiB Metadaten, 10.000 Einträge, 60 Sekunden; 7z-Header maximal 64 MiB Dekodierspeicher. Ein unvollständiges Verzeichnis ist möglich und bestätigt keine Inhaltsintegrität. Bei falschen Namen den ursprünglichen Zeichensatz angeben. Unterstützte verschlüsselte 7z-Header können ein nicht gespeichertes Passwort nutzen. Verschlüsselte RAR-Header werden als nicht unterstützt/Passwort erforderlich gemeldet; mehrteilige Archive können unvollständig sein.

Die API nimmt `site_profile_id` für Downloads, Uploads, Verzeichnisse und Vorschauen an. `GET /api/v1/site-profiles` liefert `{profiles:[...]}`; `POST` oder `PUT` speichert ein flaches Profilobjekt und liefert öffentliche Metadaten. `DELETE /api/v1/site-profiles/{id}` löscht nur das Profil. `POST /api/v1/uploads` erhält `local_path`, `target_url`, `routes`, optional `site_profile_id`, `overwrite:false` und liefert HTTP 201. `GET /api/v1/uploads` liefert `{uploads:[...]}`; einzelne Aufgaben haben GET/DELETE und POST `/{id}/pause` / `/{id}/resume`. `POST /api/v1/remote/list` nutzt `url` und `route`/`routes`; `POST /api/v1/archives/preview` zusätzlich optional `password` und `charset`. Siehe [openapi.yaml](../openapi.yaml). UTF-8-Manifeste unter `data/uploads/` speichern Profil-IDs statt Passwörtern. Vorschauen erzeugen keine Downloadaufgabe.

## Downloadzeitplan und Priorität

Unter Warteschlange/Zeitplan stehen `max_active_jobs` (1–32), globale/Gateway-Geschwindigkeitslimits und ein tägliches Fenster (`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm) zur Verfügung. Es gilt die lokale Computerzeitzone, als `schedule_timezone` gemeldet. Fenster über Mitternacht sind möglich; gleiche Start-/Endzeit bedeutet ganztägig. Außerhalb des Fensters warten Downloads in der Warteschlange; manuell pausierte Aufgaben bleiben pausiert. `priority` -100 bis 100 ordnet wartende Downloads absteigend, bei Gleichstand nach Erstellungszeit. Aktive Downloads werden dadurch nicht unterbrochen. `start_after_ms` ist der früheste einmalige Unix-Zeitpunkt in Millisekunden, 0 entfernt die Verzögerung. Einstellungen bleiben gespeichert und gelten für Downloads, nicht die separate Uploadwarteschlange. Anwendung und Computer müssen laufen; das Aufwecken des Betriebssystems ist nicht enthalten. API: `GET`/`PUT`/`POST /api/v1/queue` und `PUT`/`POST /api/v1/jobs/{id}/options`.

## Integration mit Chrome und Edge

Öffnen Sie im Programm die Integration **Chrome / Edge**, aktivieren Sie die Linkübergabe und öffnen Sie den Erweiterungsordner aus dem nativen Installer. Öffnen Sie `chrome://extensions` oder `edge://extensions`, aktivieren Sie den Entwicklermodus und wählen Sie **Entpackte Erweiterung laden** mit diesem Ordner. Lassen Sie den ausgewählten Ordner an seinem Ort.

MultiRoute muss laufen, auch im Tray genügt. Über das Kontextmenü eines Links oder das Erweiterungsfenster die URL senden. Die Anwendung verwendet aktuelle Ordner, Gateways und Websiteprofil; Repository-/Torrent-Auswahl bleibt erhalten. Bei geändertem API-Port den Erweiterungsport anpassen (Standard 8765). Nur URLs werden übertragen, keine Browsercookies oder Passwörter; normale Browserdownloads werden nicht abgefangen. Quellenzugriff in der Anwendung konfigurieren. Eine eventuell verlangte lokale Verbindungsberechtigung nur für diese Erweiterung bestätigen.

Die standardmäßig deaktivierte Brücke akzeptiert nur `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl` an `/api/v1/browser/status` und `/api/v1/browser/download`; andere API-Ursprungsregeln bleiben bestehen. HTTP 202 bestätigt die Übergabe an die Oberfläche, keinen fertigen Download. Ein reiner Headless-Server kann den Desktopauswahldialog nicht öffnen. Zum Abschalten die Erweiterung entfernen oder die Integration deaktivieren.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
