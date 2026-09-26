# MC Zombies: Untoten-Wellen

Eine Fabric-Mod für **Minecraft Java 26.3**: Spieler verteidigen sich in einer eigenen Map gegen
Zombie-Wellen, die jede Runde stärker werden. Treffer und Kills bringen Punkte.

> Aktueller Stand: **Phase 3 – Schusswaffen** (7 Waffen, Munition, Nachladen, Aufrüst-Maschine).
> Perks, Power-ups, Wiederbeleben und Spezialrunden folgen in Phase 4.

## Features (Phase 1)

- **Start mit der Faust:** Beim Spielstart wird das Inventar geleert (und bei Spielende
  zurückgegeben). In Runde 1 sterben Zombies mit einem Faustschlag, in Runde 2 und 3 mit zwei, danach werden sie stetig stärker.
- **Rundensystem:** Zombies spawnen an festgelegten Spawnpunkten. Anzahl, Leben, Tempo und Schaden
  steigen pro Runde. Zombies tragen einen Helm und verbrennen deshalb tagsüber nicht. Andere
  Mobs (Tiere, Monster) werden während des Spiels entfernt. Sind alle Zombies tot, beginnt nach einer kurzen Pause die nächste Runde.
- **Punkte:** 10 pro Treffer, 60 pro Kill, 100 für einen Kopftreffer-Kill (Projektil auf Kopfhöhe),
  130 für einen Nahkampf-Kill. Jeder Spieler startet mit 500 Punkten. Alles einstellbar.
- **HUD:** Rundenzahl groß links oben, darunter verbleibende Zombies bzw. Countdown, rechts oben die
  Punkteliste aller Spieler (eigener Name grün).
- **Down / Game Over:** Wer stirbt, geht „down“ und schaut bis zur nächsten Runde als Zuschauer zu.
  Zu Beginn jeder Runde kommen alle Gefallenen zurück. Sind alle Spieler gleichzeitig down: Game Over
  mit Statistik im Chat. (Wiederbeleben durch Mitspieler kommt in Phase 4.)
- **Mehrspieler:** Die gesamte Spiellogik läuft auf dem Server, Clients bekommen den Zustand per
  eigenem Netzwerkpaket und zeigen ihn nur an.

## Features (Phase 2)

- **Kaufbare Türen/Barrieren:** Ein beliebiger Blockbereich verschwindet, wenn ein Spieler ihn per
  Rechtsklick kauft. Eine Tür kann eine **Zone** freischalten: Spawnpunkte dieser Zone werden erst
  aktiv, wenn die Tür offen ist. Spawnpunkte ohne Zone gehören zur Zone `start`.
- **Wandwaffen:** Ein Block, an dem man per Rechtsklick eine bestimmte Waffe kauft. Hat man sie schon,
  kauft man Munition (32 Pfeile für Bogen/Armbrust). Gekaufte Waffen sind unzerstörbar.
  Das können eigene Schusswaffen (z. B. `mczombies:pistol`) oder Vanilla-Waffen sein.
- **Zufallskiste:** Mehrere mögliche Standorte, aktiv ist jeweils einer (Lichtstrahl-Partikel).
  Ein Dreh kostet 950 Punkte und gibt eine zufällige Waffe. Nach einigen Drehs kann die Kiste
  umziehen: Dann gibt es die Punkte zurück und sie steht woanders.
- **Fenster-Barrikaden:** Ein Blockbereich aus „Brettern“. Zombies am Fenster reißen nach und nach
  Bretter heraus. Wer am Fenster **schleicht**, setzt jede Sekunde ein Brett wieder ein und bekommt
  10 Punkte (höchstens 500 pro Runde).
- Schaut man auf ein kaufbares Element, steht in der Aktionsleiste, was es kostet.
- Bei Spielende werden alle Türen wieder geschlossen und alle Fenster repariert.

## Features (Phase 3)

- **Sieben Schusswaffen** (Kreativinventar, Tab „Kampf“):

  | Waffe | Item-ID | Besonderheit |
  |---|---|---|
  | Pistole | `mczombies:pistol` | Einzelschuss, 8 Schuss |
  | Maschinenpistole | `mczombies:smg` | Dauerfeuer, sehr schnell, wenig Schaden |
  | Sturmgewehr | `mczombies:assault_rifle` | Dauerfeuer, Allrounder |
  | Leichtes MG | `mczombies:lmg` | Dauerfeuer, 100 Schuss, langes Nachladen, durchschlägt 2 Zombies |
  | Schrotflinte | `mczombies:shotgun` | 8 Kugeln pro Schuss mit Streuung, kurze Reichweite |
  | Scharfschützengewehr | `mczombies:sniper` | Sehr hoher Schaden, durchschlägt 4 Zombies, ×3 bei Kopftreffer |
  | Raketenwerfer | `mczombies:rocket_launcher` | Explodiert beim Aufprall (Radius 4), Blöcke bleiben heil |

- **Schießen:** Rechtsklick. Automatikwaffen feuern, solange die Taste gehalten wird.
  Treffer sind sofort (Hitscan) und enden an Blöcken; Mitspieler werden nie getroffen.
- **Munition:** Magazin + Reserve. **R** lädt nach (Taste in den Steuerungs-Optionen änderbar);
  ein leeres Magazin wird automatisch nachgeladen. Wechselt man die Waffe, bricht das Nachladen ab.
  Rechts unten zeigt das HUD Waffenname und „Magazin / Reserve“, der Balken unter dem Item den
  Füllstand des Magazins.
- **Kopftreffer:** Treffer auf Augenhöhe machen mehr Schaden (Faktor je Waffe) und bringen
  beim Kill 100 statt 60 Punkte.
- **Wandwaffen und Zufallskiste** geben die Schusswaffen voll geladen. Hat man die Waffe schon,
  füllt ein Kauf an der Wand (Munitionspreis) bzw. ein Kisten-Treffer die Munition auf.
- **Aufrüst-Maschine:** Rechtsklick mit einer Schusswaffe in der Hand für 5000 Punkte:
  doppelter Schaden, 1,5× Magazin und Reserve, volle Munition, Glitzer und „(Verbessert)“ im Namen.

## Fertige Map: Der Riese (Nachbau von „The Giant“)

Nachbau der Black-Ops-III-Map. Der Grundriss folgt dem Bauplan vom Ladebildschirm, das Aussehen
Screenshots aus dem Spiel (Schnee, Backstein, rot glühende Fenster, Schornsteine, Kran mit dem Kopf
des Riesen). Die Maße sind geschätzt. `/zombies buildmap riese` baut sie um den Spieler herum
(am besten in einer flachen Welt, braucht etwa 130 × 120 Blöcke und 30 Blöcke Höhe). Der Spieler
steht danach im Start vor dem Hauptrechner.

| Bereich | Lage | Zugang | Inhalt |
|---|---|---|---|
| Start | Osten | – | Hauptrechner-Turm auf Plattform mit Aufrüst-Maschine, Teleporter-Ring, Balkon mit „Waffenfabrik“-Tafel, Sturmgewehr 500, Pistole 500 |
| Labor | Süden | Tür 750 | MP 1250, Pistole 750, Kiste, Tafel |
| Tierversuche | Süden, Mitte | offen vom Labor | Käfigzellen, Waschbecken, Fässer |
| Teleporter A | hinter dem Labor | Tür 1250 | Sturmgewehr 1500, Kiste, zwei glühende Gruben |
| Werkstatt/Hangar | Norden | Tür 750 | Schrotflinte 750, MP 1300, Kiste, Autos, Obergeschoss |
| Ofenraum | Norden, Mitte | offen von der Werkstatt | Hochöfen, Säulenreihe |
| Generatoren | auf dem Ofenraum | über das Obergeschoss der Werkstatt | Motor, Stromkasten, offenes Stahldach |
| Teleporter B | ganz im Norden | Tür 1250 aus dem Ofenraum | Leichtes MG 1500, Kiste |
| Innenhof | Mitte | offen von Werkstatt und Tierversuchen | MP 1250, Kiste, Strommast |
| Brücke | über dem Innenhof | Sperre 1000 an den Generatoren | Elektro-Spitzen (Falle kommt in Phase 4) |
| Hof links + Teleporter C | Westen | Tür 1250 aus dem Innenhof | Sturmgewehr 1400, Kiste, Schneehaufen |

Abweichungen vom Original: Teleporter, Falle und Stromschalter sind nur Deko (Strom kommt in
Phase 4, bis dahin ist die Brücke eine kaufbare Sperre), die Aufrüst-Maschine ist sofort nutzbar,
und die Wandwaffen sind auf die sieben Waffen der Mod abgebildet. Wandwaffen sind Goldblöcke.

## Voraussetzungen

- **JDK 25** (z.B. [Eclipse Temurin 25](https://adoptium.net/)). Prüfen mit `java -version`.
- Gradle musst du **nicht** installieren: Das Projekt bringt den Gradle-Wrapper (`gradlew`) mit,
  der beim ersten Start die passende Gradle-Version (9.7.1) selbst herunterlädt.
- Optional eine IDE: IntelliJ IDEA (Community reicht) oder VS Code mit Java-Erweiterung.

## Bauen

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

Der erste Build dauert einige Minuten (Minecraft und Fabric werden heruntergeladen). Die fertige Mod
liegt danach in `build/libs/mczombies-0.1.0.jar`. Diese Datei zusammen mit der
[Fabric API](https://modrinth.com/mod/fabric-api) in den `mods`-Ordner einer Fabric-Installation
für 26.3 legen.

Jeder Push auf GitHub wird außerdem automatisch gebaut (Reiter *Actions*); dort kann man die Jar
auch als Artefakt herunterladen.

## Testen mit `runClient`

```bash
./gradlew runClient
```

Das startet einen Minecraft-Client mit der Mod (Spielordner: `run/`).

1. **Neue Welt** anlegen: Spielmodus Kreativ, *Cheats erlauben* an. Am besten Welttyp *Flach*.
2. Ein paar **Zombie-Spawnpunkte** setzen: an eine Stelle gehen und `/zombies spawn add` eingeben
   (setzt den Punkt an deiner Position). Mehrere Punkte rund um dich verteilen, 10–20 Blöcke entfernt.
3. Optional den **Spieler-Startpunkt** setzen: `/zombies playerspawn set`.
4. `/zombies spawn show` zeigt alle Punkte 15 Sekunden lang mit Partikeln an.
5. `/zombies start` – nach 10 Sekunden beginnt Runde 1. Du wirst in den Abenteuermodus gesetzt.
6. Zombies töten und aufs HUD achten (Runde, Zombies, Punkte). Nach jeder Runde gibt es eine Pause.
7. Sterben lassen → Game Over mit Titel und Statistik im Chat. Nach 15 Sekunden wird automatisch
   zurückgesetzt, dein alter Spielmodus kommt zurück.
8. `/zombies reset` bricht ein laufendes Spiel jederzeit ab.

**Mehrspieler testen:** `./gradlew runServer` startet einen Dedicated Server (beim ersten Start in
`run/eula.txt` `eula=true` setzen). Zwei Clients kann man z.B. mit zwei `runClient`-Instanzen
im Offline-Modus verbinden (`online-mode=false` in `run/server.properties`).

## Befehle

Alle Befehle brauchen Operator-Rechte (in Einzelspieler: Cheats an).

| Befehl | Wirkung |
|---|---|
| `/zombies start` | Startet ein Spiel mit allen Nicht-Zuschauern in der aktuellen Welt |
| `/zombies reset` | Bricht das Spiel ab, entfernt Zombies, stellt Spielmodi wieder her |
| `/zombies status` | Zeigt Runde, verbleibende Zombies und aktive Spieler |
| `/zombies reload` | Liest `config/mczombies.json` neu ein |
| `/zombies spawn add [x y z]` | Zombie-Spawnpunkt an deiner Position bzw. den Koordinaten setzen |
| `/zombies spawn remove <nr>` | Spawnpunkt Nummer `<nr>` (siehe `list`) entfernen |
| `/zombies spawn list` | Alle Spawnpunkte und den Spieler-Startpunkt auflisten |
| `/zombies spawn clear` | Alle Zombie-Spawnpunkte löschen |
| `/zombies spawn show` | Spawnpunkte 15 s lang mit Partikeln anzeigen |
| `/zombies playerspawn set [x y z]` | Startpunkt der Spieler setzen (auch Wiedereinstieg nach Down) |
| `/zombies points <spieler> <anzahl>` | Punkte eines Teilnehmers setzen (zum Testen) |
| `/zombies spawn add <x y z> <zone>` | Spawnpunkt, der erst mit der Zone `<zone>` aktiv wird |
| `/zombies door add <name> <von> <bis> <preis> [zone]` | Blockbereich als kaufbare Tür anlegen (Blöcke müssen stehen) |
| `/zombies door remove <name>` / `door list` | Tür entfernen / alle Türen anzeigen |
| `/zombies window add <von> <bis>` | Blockbereich als Fenster-Barrikade anlegen (Bretter müssen stehen) |
| `/zombies window remove <nr>` / `window list` | Fenster entfernen / anzeigen |
| `/zombies wallweapon add <item> <preis> [munitionspreis]` | Den Block, auf den du schaust, zur Wandwaffe machen |
| `/zombies wallweapon remove <nr>` / `wallweapon list` | Wandwaffe entfernen / anzeigen |
| `/zombies box add [x y z]` | Kistenstandort (ohne Koordinaten: der Block, auf den du schaust) |
| `/zombies box remove <nr>` / `box list` | Kistenstandort entfernen / anzeigen |
| `/zombies upgrade add [x y z]` | Aufrüst-Maschine (ohne Koordinaten: der Block, auf den du schaust) |
| `/zombies upgrade remove <nr>` / `upgrade list` | Aufrüst-Maschine entfernen / anzeigen |
| `/zombies buildmap riese` | Baut den Nachbau von „The Giant“ um dich herum (ersetzt die Map-Einstellungen, alte Datei wird als `.bak` gesichert) |

Die Map kann nur bearbeitet werden, wenn kein Spiel läuft. `/zombies spawn show` zeigt auch Türen
(Flammen), Fenster (Funken), Wandwaffen (grün), Kistenstandorte (Lichtstrahl) und
Aufrüst-Maschinen (Zauberschrift).

Die Map-Einstellungen werden pro Welt in `<Weltordner>/mczombies_map.json` gespeichert. Wer eine
Map weitergibt, gibt die Spawnpunkte also automatisch mit.

## Konfiguration

Beim ersten Start entsteht `config/mczombies.json` (bei `runClient`: `run/config/mczombies.json`).
Die wichtigsten Werte:

| Wert | Standard | Bedeutung |
|---|---|---|
| `startingPoints` | 500 | Startpunkte je Spieler |
| `pointsPerHit` / `pointsPerKill` | 10 / 60 | Punkte für Treffer / Kill |
| `pointsPerMeleeKill` / `pointsPerHeadshotKill` | 130 / 100 | Kill-Punkte im Nahkampf / per Kopftreffer |
| `firstRoundDelaySeconds` / `intermissionSeconds` | 10 / 10 | Wartezeit vor Runde 1 / zwischen Runden |
| `zombiesBaseCount` / `zombiesPerRound` | 6 / 3 | Zombies in Runde 1 / zusätzlich pro Runde |
| `zombiesExtraPlayerFactor` | 0.5 | +50 % Zombies pro weiterem Spieler |
| `maxAliveZombies` | 24 | Maximal gleichzeitig lebende Zombies |
| `healthEarlyRounds` / `healthPerRound` | [1, 2, 2] / 3 | Leben in Runde 1–3 (1 = ein Faustschlag) / danach zusätzlich pro Runde |
| `healthLinearUntilRound` / `healthFactorAfterLinear` | 9 / 1.1 | Ab Runde 10: Leben ×1,1 pro Runde |
| `speedBase` / `speedPerRound` / `speedMax` | 0.20 / 0.008 / 0.33 | Laufgeschwindigkeit |
| `damageBase` / `damagePerRound` / `damageMax` | 2 / 0.25 / 10 | Schaden pro Schlag |
| `boxPrice` / `boxMoveChance` | 950 / 0.2 | Preis eines Kisten-Drehs / Umzugschance pro Dreh |
| `boxWeapons` | Liste | Waffen der Zufallskiste mit Gewichtung |
| `pointsPerBoardRepair` / `windowRepairPointsCapPerRound` | 10 / 500 | Punkte fürs Reparieren |
| `windowTearIntervalTicks` | 40 | So oft reißt ein Zombie ein Brett heraus (20 Ticks = 1 s) |
| `guns` | je Waffe | Schaden, Magazin, Reserve, Feuerrate, Nachladezeit, Reichweite, Streuung, Kugeln, Dauerfeuer, Durchschlag, Explosionsradius, Kopftreffer-Faktor |
| `upgradePrice` | 5000 | Preis an der Aufrüst-Maschine |
| `upgradeDamageMultiplier` / `upgradeAmmoMultiplier` | 2.0 / 1.5 | Wirkung der Aufrüstung |
| `startWithEmptyInventory` | true | Nur mit der Faust starten; Inventar kommt bei Spielende zurück |
| `adventureModeDuringGame` | true | Spieler können die Map während des Spiels nicht abbauen |
| `removeOtherMobsDuringGame` | true | Tiere und andere Monster werden während des Spiels entfernt |

Nach dem Ändern `/zombies reload` ausführen.

## Projektstruktur

```
src/main/java/de/knospenraucher/mczombies/     (Server + gemeinsamer Code)
├── MCZombies.java          Einstiegspunkt, registriert alles
├── command/                /zombies-Befehl
├── config/                 Balancing-Werte (JSON)
├── event/                  Treffer, Kills, Down statt Tod
├── game/                   GameManager (Rundenlogik), Skalierung, Spielerdaten
├── map/                    Map-Daten pro Welt (Spawns, Türen, Fenster, Kisten ...)
├── network/                Netzwerkpakete (HUD, Schießen/Nachladen)
└── weapon/                 Schusswaffen: Items, Schuss- und Nachladelogik
src/client/java/de/knospenraucher/mczombies/client/   (nur Client)
├── MCZombiesClient.java    Client-Einstiegspunkt
├── ClientGameState.java    Empfangener Spielzustand
├── GunInput.java           Rechtsklick/R an den Server schicken
└── ZombiesHud.java         HUD-Anzeige (inkl. Munition)
```
