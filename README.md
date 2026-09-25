# MC Zombies: Untoten-Wellen

Eine Fabric-Mod für **Minecraft Java 26.3**: Spieler verteidigen sich in einer eigenen Map gegen
Zombie-Wellen, die jede Runde stärker werden. Treffer und Kills bringen Punkte.

> Aktueller Stand: **Phase 1 – Grundgerüst** (Rundensystem, Punkte, HUD, Game Over).
> Türen, Wandwaffen, Zufallskiste, Waffen, Perks usw. folgen in späteren Phasen.

## Features (Phase 1)

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
| `healthBase` / `healthPerRound` | 8 / 4 | Leben in Runde 1 / zusätzlich pro Runde |
| `healthLinearUntilRound` / `healthFactorAfterLinear` | 9 / 1.1 | Ab Runde 10: Leben ×1,1 pro Runde |
| `speedBase` / `speedPerRound` / `speedMax` | 0.20 / 0.008 / 0.33 | Laufgeschwindigkeit |
| `damageBase` / `damagePerRound` / `damageMax` | 2 / 0.25 / 10 | Schaden pro Schlag |
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
├── map/                    Spawnpunkte pro Welt
└── network/                Paket für die HUD-Synchronisation
src/client/java/de/knospenraucher/mczombies/client/   (nur Client)
├── MCZombiesClient.java    Client-Einstiegspunkt
├── ClientGameState.java    Empfangener Spielzustand
└── ZombiesHud.java         HUD-Anzeige
```
