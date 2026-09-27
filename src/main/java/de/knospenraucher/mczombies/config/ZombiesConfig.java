package de.knospenraucher.mczombies.config;

import de.knospenraucher.mczombies.perk.Perk;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.knospenraucher.mczombies.MCZombies;
import de.knospenraucher.mczombies.weapon.GunCatalog;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Alle Balancing-Werte der Mod. Wird als {@code config/mczombies.json} gespeichert
 * und kann im Spiel mit {@code /zombies reload} neu eingelesen werden.
 * Fehlende Felder in der Datei bekommen automatisch die Standardwerte unten.
 */
public class ZombiesConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mczombies.json");

	/** Aktuelle Version der Config-Datei (für automatische Anpassung alter Dateien). */
	private static final int CURRENT_VERSION = 8;

	private static ZombiesConfig instance = new ZombiesConfig();

	/** Version der geladenen Datei; ältere Dateien werden in {@link #migrate()} angepasst. */
	public int configVersion = 0;

	// ---------------------------------------------------------------- Punkte
	/** Startpunkte jedes Spielers. */
	public int startingPoints = 500;
	/** Punkte für jeden Treffer, der einen Zombie nicht tötet (bei Durchschüssen je Zombie). */
	public int pointsPerHit = 10;
	/** Schrotflinten: jede Schrotkugel, die trifft, bringt pointsPerHit (false = einmal pro Schuss und Zombie). */
	public boolean pointsPerPellet = true;
	/** Punkte für einen Kill mit Treffer in den Oberkörper oder Bauch. */
	public int pointsPerKill = 60;
	/** Punkte für einen Nahkampf-Kill (Messer, Faust, Schwert). */
	public int pointsPerMeleeKill = 130;
	/** Punkte für einen Kopftreffer-Kill. */
	public int pointsPerHeadshotKill = 100;
	/** Punkte für einen Kill mit Treffer in den Hals. */
	public int pointsPerNeckKill = 70;
	/** Punkte für einen Kill mit Treffer in Arm oder Bein. */
	public int pointsPerLimbKill = 50;
	/** Punkte für einen Kill durch Explosion, Blitz, Druckwelle oder Electric Cherry. */
	public int pointsPerExplosiveKill = 50;
	/** Mehr Punkte kann ein Spieler nicht haben (Grenze in BO3). */
	public int pointsMax = 4194303;
	/** Wer down geht, verliert diesen Anteil seiner Punkte (auf 10 aufgerundet). */
	public double downPenaltyFraction = 0.05;
	/** Veraltet, wird nicht mehr benutzt: Kopftreffer bestimmt jetzt hitZones. */
	public double headshotTolerance = 0.35;

	/** Trefferzonen am Zombie (Höhen in Blöcken über den Füßen, für einen 1,95 hohen Zombie). */
	public HitZoneConfig hitZones = new HitZoneConfig();

	/** Höhen der Trefferzonen. Arme erkennt man am seitlichen Abstand von der Körpermitte. */
	public static class HitZoneConfig {
		/** Größe, für die die Höhen gelten; größere oder kleinere Wesen werden umgerechnet. */
		public double referenceHeight = 1.95;
		/** Ab hier Kopf. */
		public double headMinY = 1.50;
		/** Ab hier Hals (bis zum Kopf). */
		public double neckMinY = 1.40;
		/** Ab hier Oberkörper (bis zum Hals). */
		public double torsoUpperMinY = 1.125;
		/** Ab hier Bauch (bis zum Oberkörper); darunter Beine. */
		public double torsoLowerMinY = 0.75;
		/** Arme: zwischen dieser Höhe und dem Kopf ... */
		public double armMinY = 1.20;
		/** ... und mindestens so weit seitlich von der Körpermitte. */
		public double armLateral = 0.25;
		/** Deadshot: der Kopf reicht so viel weiter nach unten (der Hals zählt als Kopf). */
		public double deadshotHeadExtension = 0.10;
	}

	// ---------------------------------------------------------------- Runden
	/** Wartezeit nach /zombies start bis Runde 1 beginnt. */
	public int firstRoundDelaySeconds = 10;
	/** Pause zwischen zwei Runden. */
	public int intermissionSeconds = 10;
	/** Wie lange nach einem Game Over die Anzeige stehen bleibt, bevor das Spiel zurückgesetzt wird. */
	public int gameOverDisplaySeconds = 15;

	// ---------------------------------------------------------------- Zombie-Anzahl
	/** Zombies in Runde 1 (bei einem Spieler). */
	public int zombiesBaseCount = 6;
	/** Zusätzliche Zombies pro weiterer Runde. */
	public int zombiesPerRound = 3;
	/** Zusätzlicher Anteil pro weiterem Spieler (0.5 = +50 % pro Spieler). */
	public double zombiesExtraPlayerFactor = 0.5;
	/** Maximal gleichzeitig lebende Zombies; der Rest wird nachgespawnt. */
	public int maxAliveZombies = 24;
	/** Ticks zwischen zwei Spawns in Runde 1 (20 Ticks = 1 Sekunde). */
	public int spawnIntervalTicks = 40;
	/** Pro Runde wird das Spawn-Intervall um so viele Ticks kürzer ... */
	public int spawnIntervalDecreasePerRound = 3;
	/** ... aber nie kürzer als dieser Wert. */
	public int spawnIntervalMinTicks = 8;
	/** Spawnpunkte in diesem Radius um einen lebenden Spieler werden bevorzugt. */
	public double spawnPointActivationRadius = 48.0;

	// ---------------------------------------------------------------- Zombie-Werte
	/** Nur noch Rückfallwert, falls healthEarlyRounds leer ist. */
	public double healthBase = 150.0;
	/**
	 * Leben in den ersten Runden, in Black-Ops-III-Einheiten (wie der Waffenschaden).
	 * Standard wie im Original: Runde 1 hat 150.
	 */
	public List<Double> healthEarlyRounds = new ArrayList<>(List.of(150.0));
	/** Danach pro Runde so viel mehr Leben, bis healthLinearUntilRound. */
	public double healthPerRound = 100.0;
	/** Bis zu dieser Runde steigt das Leben linear ... */
	public int healthLinearUntilRound = 9;
	/** ... danach wird es pro Runde mit diesem Faktor multipliziert. */
	public double healthFactorAfterLinear = 1.1;
	/**
	 * Obergrenze des Zombie-Lebens. Wie in BO3 bleibt das Leben stehen, sobald die nächste Runde
	 * darüber läge (ab Runde 162: 2.035.642.980).
	 */
	public double healthMax = 2147483647.0;
	/**
	 * Schaden, der weder aus Schusswaffen noch aus dem Nahkampf kommt (Bogen, Armbrust), wird damit
	 * malgenommen. Faust und Schwert machen wie das Messer immer knifeDamage.
	 */
	public double meleeDamageScale = 150.0;
	/** Rückstoßresistenz der Rundenzombies (1 = Kugeln und Messer schieben sie nicht zurück, wie in BO3). */
	public double zombieKnockbackResistance = 1.0;
	/** Schaden eines Messerstichs (Taste V) in BO3-Einheiten. 150 = Runde-1-Zombies sterben mit einem Stich. */
	public double knifeDamage = 150.0;

	// ---- Perks (Black Ops III)
	/** So viele Perks darf ein Spieler gleichzeitig haben. */
	public int perkLimit = 4;
	/** So viele Schusswaffen darf man tragen (Mule Kick: eine mehr). Eine neue ersetzt die in der Hand. */
	public int weaponLimit = 2;
	/** Preis je Perk (IDs wie in /zombies perk add). */
	public Map<String, Integer> perkPrices = defaultPerkPrices();
	/** Quick Revive kostet allein weniger, weil es dann eine Selbstwiederbelebung ist. */
	public int quickReviveSoloPrice = 500;

	/** Nur noch Rückfallwerte; das Tempo hängt jetzt von der Zombie-Art ab (siehe walkerSpeed/runnerSpeed). */
	public double speedBase = 0.20;
	public double speedPerRound = 0.008;
	public double speedMax = 0.33;

	/** Laufgeschwindigkeit der Schlenderer (Vanilla-Zombie: 0.23). */
	public double walkerSpeed = 0.17;
	/** Laufgeschwindigkeit der Läufer (etwa wie Schnelligkeit II). */
	public double runnerSpeed = 0.30;
	/** Ab dieser Runde gibt es Läufer ... */
	public int runnersFromRound = 3;
	/** ... pro Runde steigt ihr Anteil um so viel (0.15 = 15 %) ... */
	public double runnerChancePerRound = 0.15;
	/** ... bis höchstens so viel. */
	public double runnerChanceMax = 0.9;
	/** Schlenderer: Ausholzeit bis zum Treffer und Pause bis zum nächsten Schlag (20 Ticks = 1 s). */
	public int walkerAttackWindupTicks = 16;
	public int walkerAttackCooldownTicks = 24;
	/** Läufer holen schneller aus und schlagen öfter zu. */
	public int runnerAttackWindupTicks = 8;
	public int runnerAttackCooldownTicks = 14;

	/**
	 * Schaden eines Zombie-Schlags in BO3-Einheiten. In BO3 macht ein Schlag 50 von 150 Leben, also
	 * ist man nach drei Schlägen down (mit Juggernog nach fünf), egal in welcher Runde.
	 */
	public double damageBase = 50.0;
	public double damagePerRound = 0.0;
	public double damageMax = 50.0;

	/** Wie weit Zombies Spieler wahrnehmen (Blöcke). */
	public double followRange = 64.0;

	// ---------------------------------------------------------------- Zufallskiste
	/** Preis für einen Dreh an der Zufallskiste. */
	public int boxPrice = 950;
	/** So oft wird die Kiste mindestens benutzt, bevor sie umziehen kann. */
	public int boxMinUsesBeforeMove = 4;
	/** Chance pro Dreh (danach), dass die Kiste umzieht. Der Preis wird dann erstattet. */
	public double boxMoveChance = 0.2;
	/** Mögliche Waffen mit Gewichtung (höher = häufiger). */
	public List<BoxEntry> boxWeapons = defaultBoxWeapons();

	private static List<BoxEntry> defaultBoxWeapons() {
		return new ArrayList<>(List.of(
				// Inhalt der Box auf The Giant
				new BoxEntry("mczombies:ray_gun", 3),
				new BoxEntry("mczombies:wunderwaffe_dg2", 2),
				new BoxEntry("mczombies:vmp", 8),
				new BoxEntry("mczombies:weevil", 8),
				new BoxEntry("mczombies:pharo", 8),
				new BoxEntry("mczombies:man_o_war", 7),
				new BoxEntry("mczombies:hvk_30", 8),
				new BoxEntry("mczombies:sheiva", 8),
				new BoxEntry("mczombies:icr_1", 8),
				new BoxEntry("mczombies:205_brecci", 8),
				new BoxEntry("mczombies:argus", 7),
				new BoxEntry("mczombies:haymaker_12", 7),
				new BoxEntry("mczombies:dingo", 7),
				new BoxEntry("mczombies:brm", 7),
				new BoxEntry("mczombies:48_dredge", 7),
				new BoxEntry("mczombies:gorgon", 7),
				new BoxEntry("mczombies:rpk", 7),
				new BoxEntry("mczombies:locus", 6),
				new BoxEntry("mczombies:drakon", 6),
				new BoxEntry("mczombies:svg_100", 6),
				new BoxEntry("mczombies:xm_53", 5),
				// Black-Market-/DLC-Waffen, seltener
				new BoxEntry("mczombies:bloodhound", 3),
				new BoxEntry("mczombies:marshal_16", 3),
				new BoxEntry("mczombies:rift_e9", 3),
				new BoxEntry("mczombies:hg_40", 3),
				new BoxEntry("mczombies:bootlegger", 3),
				new BoxEntry("mczombies:m1927", 3),
				new BoxEntry("mczombies:peacekeeper_mk2", 3),
				new BoxEntry("mczombies:thundergun", 1)));
	}

	/** Eintrag der Waffenliste der Zufallskiste. */
	public static class BoxEntry {
		public String item;
		public int weight;

		public BoxEntry(String item, int weight) {
			this.item = item;
			this.weight = weight;
		}
	}

	// ---------------------------------------------------------------- Waffen
	/** Pfeile, die man mit einem Bogen/einer Armbrust bzw. als Munition bekommt. */
	public int arrowsPerAmmo = 32;

	/**
	 * Werte der Schusswaffen, Schlüssel = Item-Name ohne Namespace (mr6, kn_44, ...).
	 * Schaden in BO3-Einheiten; Ticks: 20 = 1 Sekunde; Entfernungen in Blöcken.
	 */
	public Map<String, GunStats> guns = defaultGuns();

	/** Preis an der Aufrüst-Maschine. */
	public int upgradePrice = 5000;
	/** Nur Rückfallwert: Schaden aufgerüsteter Waffen ohne eigenen Wert = Schaden × dieser Faktor. */
	public double upgradeDamageMultiplier = 2.0;
	/** Nur Rückfallwert: Magazin und Reserve aufgerüsteter Waffen ohne eigenen Wert = Wert × dieser Faktor. */
	public double upgradeAmmoMultiplier = 1.5;

	/** Standardwerte aller Schusswaffen (siehe {@link GunCatalog}). */
	public static Map<String, GunStats> defaultGuns() {
		Map<String, GunStats> guns = new LinkedHashMap<>();
		for (GunCatalog.Def def : GunCatalog.all()) {
			guns.put(def.id(), def.stats().copy());
		}
		return guns;
	}

	/**
	 * Werte einer Schusswaffe. Felder mit 0 bedeuten "Standard" (steht jeweils dabei), damit ältere
	 * oder von Hand gekürzte Einträge weiter funktionieren. Die Berechnung steht in {@link de.knospenraucher.mczombies.weapon.GunItem}.
	 */
	public static class GunStats {
		/** Schaden pro Kugel auf kurze Entfernung (Raketen: im Zentrum der Explosion). */
		public double damage;
		/** Schaden ab minDamageRange (0 = wie damage, also kein Abfall). */
		public double damageMin;
		/** Bis zu dieser Entfernung voller Schaden ... */
		public double maxDamageRange;
		/** ... ab hier nur noch damageMin, dazwischen gleitend (0 = kein Abfall). */
		public double minDamageRange;
		/** Schuss pro Magazin. */
		public int magazine;
		/** Maximale Reservemunition. */
		public int reserve;
		/** Ticks zwischen zwei Schüssen (Kommazahl, 1200 / Schuss pro Minute); bei Feuerstößen von Stoß zu Stoß. */
		public double fireRateTicks;
		/** Dauer des Nachladens in Ticks. */
		public int reloadTicks;
		/** Reichweite in Blöcken. */
		public double range;
		/** Zufällige Streuung (0 = exakt, 0.1 = stark). */
		public double spread;
		/** Kugeln pro Schuss (Schrotflinte > 1). */
		public int pellets;
		/** true = Dauerfeuer bei gehaltener Maustaste, false = ein Schuss pro Klick. */
		public boolean automatic;
		/** Wie viele Zombies eine Kugel hintereinander treffen kann. */
		public int penetration;
		/** Anteil des Schadens, der nach jedem durchschossenen Zombie übrig bleibt (0 = 1, also kein Verlust). */
		public double penetrationDamageFactor;
		/** Explosionsradius in Blöcken (0 = keine Explosion). */
		public double explosionRadius;
		/** Schadensfaktor bei Kopftreffern. */
		public double headshotMultiplier;
		/** Schadensfaktor bei Treffern in Hals, Körper und Arme/Beine (0 = 1). */
		public double neckMultiplier;
		public double torsoMultiplier;
		public double limbMultiplier;
		/** Schüsse pro Abzug (Feuerstoß); 0 oder 1 = einzeln. */
		public int burst;
		/** Ticks zwischen den Schüssen eines Feuerstoßes (Kommazahl). */
		public double burstIntervalTicks;
		/** Werte nach Pack-a-Punch; 0 = Standardwert (Schaden × upgradeDamageMultiplier, Munition × upgradeAmmoMultiplier). */
		public double upgradedDamage;
		/** Mindestschaden nach Pack-a-Punch (0 = im selben Verhältnis wie vorher). */
		public double upgradedDamageMin;
		public int upgradedMagazine;
		public int upgradedReserve;
		/** Explosionsradius nach Pack-a-Punch (0 = wie vorher). */
		public double upgradedExplosionRadius;
		/** Kopftreffer-Faktor nach Pack-a-Punch (0 = wie vorher). */
		public double upgradedHeadshotMultiplier;
		/** Durchschlag nach Pack-a-Punch (0 = wie vorher) und Schadensanteil danach (0 = wie vorher). */
		public int upgradedPenetration;
		public double upgradedPenetrationDamageFactor;
		/** Feuerrate und Nachladezeit nach Pack-a-Punch (0 = wie vorher). */
		public double upgradedFireRateTicks;
		public int upgradedReloadTicks;
		/** Sonderwirkung: ray, lightning, thunder, annihilate (leer = keine). */
		public String special = "";

		public GunStats() {
		}

		public GunStats copy() {
			return GSON.fromJson(GSON.toJson(this), GunStats.class);
		}
	}

	/** Werte einer Waffe; fehlt sie in der Datei, gelten die Standardwerte. */
	public GunStats gun(String key) {
		GunStats stats = guns != null ? guns.get(key) : null;
		if (stats == null) {
			GunCatalog.Def def = GunCatalog.get(key);
			stats = def != null ? def.stats() : null;
		}
		return stats;
	}

	// ---------------------------------------------------------------- Fenster
	/** Alle so vielen Ticks reißt ein Zombie am Fenster ein Brett heraus. */
	public int windowTearIntervalTicks = 40;
	/** Alle so vielen Ticks repariert ein schleichender Spieler ein Brett. */
	public int windowRepairIntervalTicks = 20;
	/** Punkte pro repariertem Brett. */
	public int pointsPerBoardRepair = 10;
	/** Höchstens so viele Reparatur-Punkte pro Spieler und Runde. */
	public int windowRepairPointsCapPerRound = 500;

	// ---------------------------------------------------------------- Spieler
	/** Spieler während des Spiels in den Abenteuermodus setzen (verhindert Abbauen der Map). */
	public boolean adventureModeDuringGame = true;
	/** Spieler starten nur mit der Faust; ihr Inventar kommt bei Spielende zurück. */
	public boolean startWithEmptyInventory = true;
	/** Waffe, mit der jeder Spieler startet (leer = keine, nur Faust und Messer). */
	public String startingWeapon = "mczombies:mr6";
	/** Andere Mobs (Tiere, Monster) während des Spiels aus der Welt entfernen. */
	public boolean removeOtherMobsDuringGame = true;

	// ---- Leben der Spieler (Black Ops III)
	/** Leben eines Spielers in BO3-Einheiten (3 Zombie-Schläge). */
	public double playerHealth = 150.0;
	/** Leben mit Juggernog (5 Zombie-Schläge). */
	public double juggernogHealth = 250.0;
	/** So viele BO3-Lebenspunkte sind ein Minecraft-Lebenspunkt (150 → 30, also 15 Herzen). */
	public double bo3HealthPerMcHealth = 5.0;
	/** Nach so vielen Ticks ohne Treffer heilt man sich (48 = 2,4 Sekunden) ... */
	public int regenDelayTicks = 48;
	/** ... außer man hatte höchstens diesen Anteil Leben übrig (schwer verletzt) ... */
	public double veryHurtFraction = 0.2;
	/** ... dann dauert es so lange (100 = 5 Sekunden). */
	public int veryHurtRegenDelayTicks = 100;
	/** Danach füllt sich pro Tick dieser Anteil des Lebens auf. */
	public double regenPerTick = 0.1;
	/** Kein Hunger und keine Vanilla-Heilung durch Essen während des Spiels. */
	public boolean disableHunger = true;
	/** Herzen, Hunger und Rüstung im Spiel ausblenden (BO3 zeigt kein Leben an; rotes Blut am Rand statt dessen). */
	public boolean hideVanillaBars = true;
	/** Eigene Explosionen (XM-53, Ray Gun) verletzen einen selbst, wie in BO3. */
	public boolean explosiveSelfDamage = true;
	/** Mehr als so viel (BO3) zieht eine eigene Explosion nicht ab, solange man mehr Leben hat. */
	public double explosiveSelfDamageCap = 75.0;
	/** Dasselbe für die Ray Gun. */
	public double rayGunSelfDamageCap = 25.0;
	/** Explosionen bekommen wie in BO3 Zusatzschaden: Runde × Zufall 0 bis 99. */
	public boolean explosiveRoundBonus = true;

	private static Map<String, Integer> defaultPerkPrices() {
		Map<String, Integer> prices = new LinkedHashMap<>();
		for (Perk perk : Perk.values()) {
			prices.put(perk.id(), perk.defaultPrice());
		}
		return prices;
	}

	/** Preis eines Perks laut Config. */
	public int perkPrice(Perk perk) {
		return perkPrices.getOrDefault(perk.id(), perk.defaultPrice());
	}

	/** Passt Werte aus älteren Config-Dateien an geänderte Standards an. */
	private void migrate() {
		if (configVersion < 2) {
			// Runde 1: Zombies sterben mit einem Faustschlag.
			healthBase = 1.0;
		}
		if (configVersion < 3) {
			// Phase 3: Zufallskiste enthält die eigenen Schusswaffen.
			boxWeapons = defaultBoxWeapons();
		}
		if (configVersion < 5) {
			// Runde 2 und 3: zwei Faustschläge, danach langsamer Anstieg.
			healthEarlyRounds = new ArrayList<>(List.of(1.0, 2.0, 2.0));
			healthPerRound = 3.0;
		}
		if (configVersion < 7) {
			// Originalwerte aus Black Ops III: Zombie-Leben, Zombie-Schaden, Waffenschaden 1:1.
			healthBase = 150.0;
			healthEarlyRounds = new ArrayList<>(List.of(150.0));
			healthPerRound = 100.0;
			healthLinearUntilRound = 9;
			healthFactorAfterLinear = 1.1;
			healthMax = 1.0E9;
			damageBase = 6.7;
			damagePerRound = 0.0;
			damageMax = 6.7;
			guns = defaultGuns();
		}
		if (configVersion < 6) {
			// Black-Ops-III-Waffen ersetzen die sieben Platzhalterwaffen.
			boxWeapons = defaultBoxWeapons();
			if (guns != null) {
				guns.keySet().removeIf(key -> GunCatalog.get(key) == null);
			}
		}
		if (configVersion < 8) {
			// Lebens- und Schadenssystem aus BO3: Trefferzonen, Reichweitenabfall, neue Waffenwerte,
			// Spielerleben 150 (Juggernog 250), Zombie-Schlag 50, Zombie-Leben ohne Rundungsdrift.
			if (configVersion >= 6) {
				MCZombies.LOGGER.info("mczombies.json: Waffenwerte wurden auf die neuen BO3-Standards gesetzt (alte Datei als Sicherung daneben)");
			}
			guns = defaultGuns();
			healthMax = 2147483647.0;
			damageBase = 50.0;
			damagePerRound = 0.0;
			damageMax = 50.0;
			pointsPerKill = 60;
			hitZones = new HitZoneConfig();
		}
		if (hitZones == null) {
			hitZones = new HitZoneConfig();
		}
		if (perkPrices == null) {
			perkPrices = defaultPerkPrices();
		}
		defaultPerkPrices().forEach(perkPrices::putIfAbsent);
		// Neue Waffen in älteren Dateien ergänzen (vorhandene Werte bleiben).
		if (guns == null) {
			guns = defaultGuns();
		}
		defaultGuns().forEach(guns::putIfAbsent);
		configVersion = CURRENT_VERSION;
	}

	public static ZombiesConfig get() {
		return instance;
	}

	/** Lädt die Config von der Platte oder legt sie mit Standardwerten an. */
	public static void load() {
		try {
			if (Files.exists(FILE)) {
				ZombiesConfig loaded;
				try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
					loaded = GSON.fromJson(reader, ZombiesConfig.class);
				}
				if (loaded != null) {
					if (loaded.configVersion < CURRENT_VERSION) {
						// Vor dem Anpassen sichern, damit eigene Änderungen nicht verloren gehen.
						Path backup = FILE.resolveSibling("mczombies.json.v" + loaded.configVersion + ".bak");
						Files.copy(FILE, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
					loaded.migrate();
					instance = loaded;
				}
			} else {
				instance = new ZombiesConfig();
				instance.configVersion = CURRENT_VERSION;
			}
			// Immer zurückschreiben, damit neue Felder in der Datei auftauchen.
			save();
		} catch (Exception e) {
			MCZombies.LOGGER.error("Konnte {} nicht laden, verwende Standardwerte", FILE, e);
			instance = new ZombiesConfig();
		}
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			MCZombies.LOGGER.error("Konnte {} nicht speichern", FILE, e);
		}
	}
}
