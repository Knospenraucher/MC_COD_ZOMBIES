# MC Zombies: Untoten-Wellen

Eine Fabric-Mod für **Minecraft Java 26.3**: Spieler verteidigen sich in einer eigenen Map gegen
Zombie-Wellen, die jede Runde stärker werden. Treffer und Kills bringen Punkte.

> Aktueller Stand: **Phase 3 – Schusswaffen** (40 BO3-Waffen, Perks, Leben und Schaden wie in Black Ops III).
> Power-ups, Wiederbeleben, Granaten und Spezialrunden folgen noch.

## Features (Phase 1)

- **Start mit der MR6:** Beim Spielstart wird das Inventar geleert (und bei Spielende
  zurückgegeben); jeder bekommt die MR6 voll geladen (Config `startingWeapon`).
  Zombies haben ihr Leben aus Black Ops III (Runde 1: 150, dann +100 pro Runde, ab Runde 10 ×1,1),
  Spieler ebenfalls (siehe [Leben und Schaden](#leben-und-schaden-black-ops-iii)).
- **Messer auf V:** mit jeder Waffe in der Hand, 150 Schaden wie in BO3, Nahkampf-Kill 130 Punkte.
  Faust und Schwert machen genauso 150.
  Läufst du auf einen Zombie knapp außer Reichweite (bis 4 Blöcke) zu, machst du einen Ausfallschritt;
  sonst bleibst du stehen.
  Ein gezeichnetes Kampfmesser schwingt dabei durchs Bild. Taste in den Steuerungs-Optionen änderbar.
- **Zombie-Arten:** Schlenderer und Läufer. Ab Runde 3 gibt es Läufer, ihr Anteil steigt pro Runde.
  Zombies schlagen nicht bei Berührung zu, sondern holen aus und treffen erst nach der Ausholzeit
  (Läufer schneller); wer vorher wegläuft, wird nicht getroffen.
- **Rundensystem:** Zombies spawnen an festgelegten Spawnpunkten. Anzahl, Leben, Tempo und Schaden
  steigen pro Runde. Zombies tragen einen Helm und verbrennen deshalb tagsüber nicht. Andere
  Mobs (Tiere, Monster) werden während des Spiels entfernt. Sind alle Zombies tot, beginnt nach einer kurzen Pause die nächste Runde.
- **Punkte wie in BO3:** 10 pro Treffer, beim Kill je nach Trefferzone 100 (Kopf), 70 (Hals),
  60 (Körper), 50 (Arm, Bein, Explosion), 130 im Nahkampf. Jeder Spieler startet mit 500 Punkten.
  Wer down geht, verliert 5 % seiner Punkte. Alles einstellbar.
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
  Das können eigene Schusswaffen (z. B. `mczombies:kn_44`) oder Vanilla-Waffen sein.
- **Zufallskiste:** Mehrere mögliche Standorte, aktiv ist jeweils einer (Lichtstrahl-Partikel).
  Ein Dreh kostet 950 Punkte und gibt eine zufällige Waffe. Nach einigen Drehs kann die Kiste
  umziehen: Dann gibt es die Punkte zurück und sie steht woanders.
- **Fenster-Barrikaden:** Ein Blockbereich aus „Brettern“. Zombies am Fenster reißen nach und nach
  Bretter heraus. Wer am Fenster **schleicht**, setzt jede Sekunde ein Brett wieder ein und bekommt
  10 Punkte (höchstens 500 pro Runde).
- Schaut man auf ein kaufbares Element, steht in der Aktionsleiste, was es kostet.
- Bei Spielende werden alle Türen wieder geschlossen und alle Fenster repariert.

## Features (Phase 3)

- **Alle Waffen aus Black Ops III Zombies** (40 Stück, Kreativinventar Tab „Kampf“, Item-ID
  `mczombies:<name>`, z. B. `mczombies:kn_44`). Werte nach dem Original, auch der Schaden 1:1, weil
  die Zombies ihr BO3-Leben haben. Wo keine Quelle etwas hergab (v. a. Nachladezeiten,
  MR6, RPK, XM-53), sind die Werte geschätzt; alles ist in der Config änderbar.

  | Klasse | Waffen |
  |---|---|
  | Pistolen | `mr6`, `rk5`, `l_car_9`, `bloodhound`, `marshal_16`, `rift_e9` |
  | Maschinenpistolen | `kuda`, `vmp`, `weevil`, `vesper`, `pharo`, `razorback`, `hg_40`, `bootlegger`, `m1927` |
  | Sturmgewehre | `kn_44`, `hvk_30`, `icr_1`, `man_o_war`, `sheiva`, `m8a7`, `peacekeeper_mk2` |
  | Schrotflinten | `krm_262`, `205_brecci`, `haymaker_12`, `argus` |
  | Leichte MGs | `brm`, `dingo`, `gorgon`, `48_dredge`, `rpk` |
  | Scharfschützengewehre | `drakon`, `locus`, `svg_100` |
  | Werfer | `xm_53` |
  | Wunderwaffen | `ray_gun`, `ray_gun_mk2`, `wunderwaffe_dg2`, `thundergun`, `annihilator` |

  Feuerstoß-Waffen (Pharo, M8A7, 48 Dredge, Rift E9, Ray Gun Mark II) feuern pro Klick mehrere
  Schüsse. Die Ray Gun schießt einen explodierenden Strahl, die DG-2 einen Kettenblitz über bis zu
  10 Zombies, die Thundergun eine Druckwelle, die alle Zombies vor dir tötet und wegschleudert.
  Noch nicht dabei: Apothicon Servant, Wrath of the Ancients, KT-4, GKZ-45 Mk3 (Quest-Waffen anderer Maps).
  Texturen, Modelle und Namen erzeugt `tools/gen_gun_textures.py`.
- **Schießen:** Linksklick (mit Waffe in der Hand wird nicht geschlagen oder abgebaut).
  Automatikwaffen feuern, solange die Taste gehalten wird.
- **Zielen:** Rechtsklick halten (die Waffe gleitet in etwa 0,2 s ins Visier, Hand und Waffe der Ich-Ansicht werden dabei ausgeblendet; die MR6 hat eine eigene BO3-Visieransicht): Zoom, Kimme und Korn (Scharfschützengewehre: Zielfernrohr mit
  starkem Zoom), nur noch 20 % der Streuung (Schrotflinten 70 %), dafür 35 % langsamer.
  Treffer sind sofort (Hitscan) und enden an Blöcken; Mitspieler werden nie getroffen.
- **Munition:** Magazin + Reserve. **R** lädt nach (Taste in den Steuerungs-Optionen änderbar);
  ein leeres Magazin wird automatisch nachgeladen. Wechselt man die Waffe, bricht das Nachladen ab.
  Rechts unten zeigt das HUD Waffenname und „Magazin / Reserve“, der Balken unter dem Item den
  Füllstand des Magazins.
- **Trefferzonen und Reichweite:** Kopftreffer machen je Waffe mehr Schaden, und auf Entfernung
  verlieren die meisten Waffen Schaden (siehe [Leben und Schaden](#leben-und-schaden-black-ops-iii)).
- **Wandwaffen und Zufallskiste** geben die Schusswaffen voll geladen. Hat man die Waffe schon,
  füllt ein Kauf an der Wand (Munitionspreis) bzw. ein Kisten-Treffer die Munition auf.
- **Aufrüst-Maschine (Pack-a-Punch):** Rechtsklick mit einer Schusswaffe in der Hand für 5000 Punkte:
  Schaden, Magazin und Reserve wie nach Pack-a-Punch im Original, volle Munition, Glitzer und der
  Pack-a-Punch-Name (z. B. KN-44 → „Anointed Avenger“).

## Leben und Schaden (Black Ops III)

Leben und Schaden rechnen in BO3-Einheiten, damit alle Zahlen wie im Original sind.

**Zombies.** Runde 1 hat 150 Leben, bis Runde 9 kommen je 100 dazu (950), danach je 10 %, bei jedem
Schritt abgerundet wie in BO3 (Runde 10: 1045, Runde 20: 2701, Runde 30: 7000, Runde 50: 47073).
Ab Runde 162 steigt das Leben nicht mehr (2.035.642.980). Kugeln und Messer schieben Zombies nicht zurück.

**Trefferzonen.** Gemessen wird dort, wo die Kugel der Körperachse des Zombies am nächsten kommt
(so zählt ein Schuss von oben nicht automatisch als Kopftreffer):

| Zone | Höhe über den Füßen | Schaden | Kill-Punkte |
|---|---|---|---|
| Kopf | ab 1,50 Blöcke | × Kopftreffer-Faktor der Waffe | 100 |
| Hals | 1,40 bis 1,50 | × 1 | 70 |
| Arme | 1,20 bis 1,50, seitlich der Körpermitte | × 1 | 50 |
| Oberkörper / Bauch | 0,75 bis 1,40 | × 1 | 60 |
| Beine | unter 0,75 | × 1 | 50 |
| Explosion, Blitz, Druckwelle | – | – | 50 |

Kopftreffer-Faktoren: MR6 ×3, L-CAR 9 ×6, Weevil ×5, KN-44 ×4 (nach Pack-a-Punch ×5), M8A7 ×5,
Pharo ×2,75, Ray Gun Mark II ×21,74 (50.000 pro Kopftreffer), Schrotflinten ×1,5, sonst ×4
(Pistolen ×3). Deadshot Daiquiri zählt den Hals als Kopf.

**Schaden pro Treffer** = Schaden auf diese Entfernung × Faktor der Zone × Restanteil nach
durchschossenen Zombies, abgerundet. Double Tap verdoppelt ihn.

- **Reichweitenabfall:** bis zur ersten Entfernung voller Schaden, ab der zweiten der Mindestschaden,
  dazwischen gleitend. Pistolen 8→20 Blöcke, Maschinenpistolen 10→25, Sturmgewehre 20→45,
  leichte MGs 25→50, Scharfschützen 40→80, Schrot 4→12. MR6, Bootlegger und Locus haben keinen Abfall.
- **Durchschlag:** Pistolen und Maschinenpistolen treffen 2 Zombies (der zweite bekommt 50 %),
  Sturmgewehre 3 (je 60 %), MGs 3 (je 75 %), Scharfschützen 5 (je 85 %), Locus 6 (je 90 %),
  Schrot 1. HVK-30 und 48 Dredge bekommen nach Pack-a-Punch Vollmantel (5 Zombies, je 90 %).
- **Schrotflinten:** jede Schrotkugel ist ein eigener Treffer (und bringt 10 Punkte).
- **Explosionen** (XM-53, Ray Gun, Meat Wagon): 100 % im Zentrum, 40 % am Rand, der direkt getroffene
  Zombie bekommt alles. Dazu wie in BO3 Runde × Zufall 0 bis 99. Die eigene Explosion trifft auch dich:
  hast du mehr als 75 Leben (Ray Gun: 25), zieht sie genau so viel ab.
- **Feuerrate** in Schuss pro Minute wie im Original (z. B. 722 Schuss/min = 6 Schuss in einer halben
  Sekunde), auch schneller als ein Schuss pro Tick.

Beispiele: Die MR6 braucht in Runde 1 drei Kopfschüsse (3 × 60) oder acht Körpertreffer (8 × 20),
in Runde 2 fünf Kopfschüsse. Die L-CAR 9 tötet mit einem Kopfschuss aus der Nähe bis Runde 8, die
KN-44 bis Runde 4 (nach Pack-a-Punch bis Runde 9). Das Messer braucht in Runde 1 einen Stich,
in Runde 2 zwei, in Runde 3 und 4 drei.

**Spieler.** Du hast 150 Leben (Juggernog: 250), ein Zombie-Schlag nimmt 50: nach drei Schlägen bist
du down, mit Juggernog nach fünf. Die Schwierigkeit spielt keine Rolle, und zwei Zombies können
gleichzeitig treffen. In Minecraft sind das 30 bzw. 50 Leben (1 Minecraft-Leben = 5 BO3-Leben).
Wirst du 2,4 Sekunden nicht getroffen, heilst du dich in einer halben Sekunde ganz. Warst du auf
höchstens 20 %, dauert es 5 Sekunden. Hunger gibt es im Spiel nicht, und Herzen, Hunger und Rüstung
sind ausgeblendet: Wie in BO3 färbt sich stattdessen der Bildschirmrand rot, bei jedem Treffer
kurz stärker, und unter 20 % pulsiert er mit Herzschlag.

**Geschätzt** (keine oder nur eine schwache Quelle, alles in der Config änderbar): die Aufteilung
der MR6 in 20 Schaden × 3, das Spielerleben 150 und der Schlag 50, alle Kopftreffer-Faktoren außer
L-CAR 9, Weevil, KN-44 und 48 Dredge, Hals-, Körper- und Arm/Bein-Faktor 1, die Zonenhöhen, alle
Reichweiten des Abfalls, der Durchschlag, die meisten Nachladezeiten, die Pack-a-Punch-Werte vieler
Waffen, RPK und XM-53 komplett, die Explosions-Formel und der Eigenschaden.

## Perks (Black Ops III)

Perk-Automaten stehen in der Map (zwei Blöcke hoch). Rechtsklick kauft den Perk, höchstens 4 gleichzeitig.
Wer down geht, verliert alle Perks. Die Perk-Leiste steht links unten im HUD.

| Perk | Preis | Wirkung |
|------|-------|---------|
| Juggernog | 2500 | 250 statt 150 Leben: fünf statt drei Zombie-Schläge |
| Quick Revive | 500 allein / 1500 | Allein: sofort wieder aufstehen statt down, höchstens 3-mal pro Spiel, kurz unverwundbar. Zu mehreren noch ohne Wirkung (Wiederbeleben kommt noch) |
| Speed Cola | 3000 | Halbe Nachladezeit |
| Double Tap Root Beer 2.0 | 2000 | 25 % kürzere Pause zwischen Schüssen, jede Kugel zählt doppelt |
| Stamin-Up | 2000 | 7 % schneller laufen |
| Mule Kick | 4000 | Drei statt zwei Schusswaffen; beim Down geht die dritte verloren |
| Deadshot Daiquiri | 1500 | Halbe Hüftfeuer-Streuung, Halstreffer zählen als Kopftreffer |
| Widow's Wine | 4000 | Trifft dich ein Zombie, bleiben alle Zombies im Umkreis von 4 Blöcken 6 s in Netzen hängen (alle 15 s); Messerstiche spinnen ein |
| Electric Cherry | 2000 | Nachladen löst einen Schock aus: je leerer das Magazin, desto weiter (bis 4 Blöcke) und stärker (bis 1000) |

**Waffenlimit:** Man trägt höchstens 2 Schusswaffen (Mule Kick: 3). Eine neue Waffe von der Wand oder aus
der Kiste ersetzt die Waffe in der Hand.

## Testmap: Nacht der Untoten

`/zombies buildmap nacht` baut ein kleines Farmhaus nach der Beschreibung von „Nacht der Untoten“
im Fandom-Wiki (etwa 35 × 19 Blöcke, zwei Stockwerke). WaW-Waffen sind durch BO3-Waffen ersetzt.

| Bereich | Inhalt |
|---|---|
| Startraum | 5 Fenster, Sheiva 200 (statt Kar98k), M8A7 600 (statt M1A1 Carbine) |
| Help-Raum (Tür rechts vom Start, 1000) | 2 Fenster und die Höhle, Zufallskiste mit festem Platz, 205 Brecci 1200, M1927 1200 |
| Obergeschoss (Sofa-Treppe im Startraum oder Schutt-Treppe im Help-Raum, je 1000) | 4 Fenster, Marshal 16 1200, KRM-262 1500, Man-O-War 1800, Scharfschützenschrank mit Drakon 1500 und Locus 5000, Pack-a-Punch |

Zum Testen stehen alle neun Perk-Automaten im Haus (das Original hat keine): im Startraum Juggernog
(rot), Quick Revive (Blaueis), Speed Cola (Smaragd), Double Tap (Rohgold); im Help-Raum Stamin-Up (Gold),
Mule Kick (Moos), Deadshot (Obsidian); oben Widow's Wine (Amethyst) und Electric Cherry (Diamant).
Handgranaten fehlen noch. Der Bearbeitungsmodus funktioniert
wie bei „Der Riese“, die Vorlage heißt `nacht`.

## Map: Der Riese (Nachbau von „The Giant“, im Aufbau)

Die Map entsteht Bereich für Bereich nach Screenshots aus dem Spiel. Bisher steht der **Spawn**:
der verschneite Hof vor dem Hauptrechner. `/zombies buildmap riese` baut ihn um den Spieler herum
(etwa 60 × 60 Blöcke, 32 hoch).

- **Hauptrechner:** Turm auf einer erhöhten Plattform mit breiter Treppe, blau leuchtende Kuppel,
  glühende Kugeln und Leuchtstrahl. Vorne im Turm die Aufrüst-Maschine, davor der Teleporter-Ring.
- **Westen:** „Waffenfabrik der Riese“ mit Terrasse, Treppe, Stahlbalkon, Tafel und
  Kaugummiautomat (funktioniert ab Phase 4).
- **Süden:** Uhrturm mit goldener Uhr und rot beleuchtetem Tor, niedrige Mauer mit Stacheldraht.
- **Norden/Osten:** Fabrikwand mit glühenden Bögen, Zaun mit Stacheldraht, Schornsteine.
- **Wandwaffen:** Sheiva 500 am Uhrturm, RK5 500 an der niedrigen Mauer (wie im Startraum des Originals).
- **Fenster:** zwei in den Bögen im Norden, zwei im Zaun im Osten.

Die Tore auf der Terrasse und unter dem Uhrturm führen später in die nächsten Bereiche.

### Bearbeitungsmodus

Die gebaute Map lässt sich im Spiel umbauen und als eigene Vorlage speichern:

1. `/zombies buildmap riese`, dann `/zombies edit start` (schaltet in den Kreativmodus).
2. Blöcke setzen und abbauen; Türen, Fenster, Wandwaffen usw. mit den üblichen Befehlen ändern.
3. `/zombies edit save` speichert den Bereich samt Map-Elementen nach
   `config/mczombies/maps/riese.json.gz`. Ab dann baut `/zombies buildmap riese` diese Version,
   auch in anderen Welten.

`/zombies edit cancel` beendet ohne Speichern, `/zombies buildmap riese original` baut die
eingebaute Version, `/zombies edit reset` löscht die eigene Vorlage. Gespeichert werden nur
Blöcke, keine Inhalte von Truhen oder Schildern. Solange der Bearbeitungsmodus an ist, startet kein Spiel.

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
| `/zombies perk add <perk> [x y z]` | Perk-Automat (unterer Block; der Block darüber gehört dazu). Perks: juggernog, quick_revive, speed_cola, double_tap, stamin_up, mule_kick, deadshot, widows_wine, electric_cherry |
| `/zombies perk remove <nr>` / `list` | Perk-Automat entfernen / auflisten |
| `/zombies upgrade remove <nr>` / `upgrade list` | Aufrüst-Maschine entfernen / anzeigen |
| `/zombies buildmap riese` | Baut den Nachbau von „The Giant“ um dich herum (deine gespeicherte Version, falls vorhanden; ersetzt die Map-Einstellungen, alte Datei wird als `.bak` gesichert) |
| `/zombies buildmap riese original` | Baut immer die eingebaute Version |
| `/zombies buildmap nacht [original]` | Baut die Testmap „Nacht der Untoten“ um dich herum |
| `/zombies edit start` / `edit save` | Bearbeitungsmodus an / Umbau als eigene Vorlage speichern |
| `/zombies edit cancel` / `edit reset` | Ohne Speichern beenden / eigene Vorlage löschen |

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
| `pointsPerHit` / `pointsPerPellet` | 10 / true | Punkte für einen Treffer, der nicht tötet / auch für jede Schrotkugel |
| `pointsPerHeadshotKill` / `pointsPerNeckKill` / `pointsPerKill` / `pointsPerLimbKill` | 100 / 70 / 60 / 50 | Kill-Punkte je Trefferzone (Kopf / Hals / Körper / Arm, Bein) |
| `pointsPerMeleeKill` / `pointsPerExplosiveKill` | 130 / 50 | Kill-Punkte im Nahkampf / durch Explosion, Blitz, Druckwelle |
| `downPenaltyFraction` / `pointsMax` | 0.05 / 4194303 | Punktabzug beim Down / Höchststand |
| `hitZones` | siehe oben | Höhen der Trefferzonen, seitlicher Abstand der Arme, Deadshot-Erweiterung |
| `playerHealth` / `juggernogHealth` | 150 / 250 | Spielerleben in BO3-Einheiten ohne / mit Juggernog |
| `bo3HealthPerMcHealth` | 5 | So viele BO3-Leben sind ein Minecraft-Leben |
| `regenDelayTicks` / `veryHurtRegenDelayTicks` / `veryHurtFraction` / `regenPerTick` | 48 / 100 / 0.2 / 0.1 | Heilung: Wartezeit / Wartezeit bei höchstens 20 % / Grenze / Anteil pro Tick |
| `disableHunger` / `hideVanillaBars` | true / true | Kein Hunger im Spiel / Herzen, Hunger, Rüstung im Spiel ausblenden |
| `explosiveSelfDamage` / `explosiveSelfDamageCap` / `rayGunSelfDamageCap` | true / 75 / 25 | Eigene Explosionen treffen dich |
| `explosiveRoundBonus` | true | Explosionen: zusätzlich Runde × Zufall 0 bis 99 |
| `zombieKnockbackResistance` | 1.0 | 1 = Zombies werden nicht zurückgestoßen |
| `firstRoundDelaySeconds` / `intermissionSeconds` | 10 / 10 | Wartezeit vor Runde 1 / zwischen Runden |
| `zombiesBaseCount` / `zombiesPerRound` | 6 / 3 | Zombies in Runde 1 / zusätzlich pro Runde |
| `zombiesExtraPlayerFactor` | 0.5 | +50 % Zombies pro weiterem Spieler |
| `maxAliveZombies` | 24 | Maximal gleichzeitig lebende Zombies |
| `healthEarlyRounds` / `healthPerRound` | [150] / 100 | Zombie-Leben in BO3-Einheiten: Runde 1 / danach zusätzlich pro Runde |
| `healthMax` | 2147483647 | Würde das Leben darüber steigen, bleibt es stehen (wie in BO3 ab Runde 162) |
| `perkLimit` | 4 | So viele Perks gleichzeitig |
| `weaponLimit` | 2 | So viele Schusswaffen (Mule Kick: eine mehr) |
| `perkPrices` | BO3-Preise | Preis je Perk-ID |
| `quickReviveSoloPrice` | 500 | Quick Revive allein |
| `knifeDamage` | 150 | Schaden eines Messerstichs (V); 150 = Runde-1-Zombies sterben mit einem Stich |
| `meleeDamageScale` | 150 | Bogen, Armbrust: Minecraft-Schaden × Faktor (Faust und Schwert machen `knifeDamage`) |
| `healthLinearUntilRound` / `healthFactorAfterLinear` | 9 / 1.1 | Ab Runde 10: Leben ×1,1 pro Runde |
| `walkerSpeed` / `runnerSpeed` | 0.17 / 0.30 | Tempo der Schlenderer / Läufer |
| `runnersFromRound` / `runnerChancePerRound` / `runnerChanceMax` | 3 / 0.15 / 0.9 | Ab Runde 3 Läufer, Anteil +15 % pro Runde, höchstens 90 % |
| `walkerAttackWindupTicks` / `walkerAttackCooldownTicks` | 16 / 24 | Schlenderer: Ausholzeit / Pause zwischen Schlägen (20 Ticks = 1 s) |
| `runnerAttackWindupTicks` / `runnerAttackCooldownTicks` | 8 / 14 | Läufer: Ausholzeit / Pause zwischen Schlägen |
| `damageBase` / `damagePerRound` / `damageMax` | 50 / 0 / 50 | Zombie-Schlag in BO3-Einheiten (drei Schläge bis down, mit Juggernog fünf) |
| `boxPrice` / `boxMoveChance` | 950 / 0.2 | Preis eines Kisten-Drehs / Umzugschance pro Dreh |
| `boxWeapons` | Liste | Waffen der Zufallskiste mit Gewichtung |
| `pointsPerBoardRepair` / `windowRepairPointsCapPerRound` | 10 / 500 | Punkte fürs Reparieren |
| `windowTearIntervalTicks` | 40 | So oft reißt ein Zombie ein Brett heraus (20 Ticks = 1 s) |
| `guns` | je Waffe | Schaden nah/fern (`damage`, `damageMin`) und Abfall-Entfernungen (`maxDamageRange`, `minDamageRange`), Faktoren je Zone, Magazin, Reserve, Feuerrate in Ticks (Kommazahl), Feuerstoß, Nachladezeit, Reichweite, Streuung, Kugeln, Dauerfeuer, Durchschlag und Restanteil, Explosionsradius, alle Pack-a-Punch-Werte (`upgraded…`; 0 = Standard) |
| `upgradePrice` | 5000 | Preis an der Aufrüst-Maschine |
| `upgradeDamageMultiplier` / `upgradeAmmoMultiplier` | 2.0 / 1.5 | Wirkung der Aufrüstung, nur für Waffen ohne eigene Pack-a-Punch-Werte |
| `startWithEmptyInventory` | true | Mit leerem Inventar starten; Inventar kommt bei Spielende zurück |
| `startingWeapon` | `mczombies:mr6` | Startwaffe (leer = keine) |
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
├── GunInput.java           Linksklick/Rechtsklick/R/V an den Server schicken
└── ZombiesHud.java         HUD-Anzeige (inkl. Munition)
```
