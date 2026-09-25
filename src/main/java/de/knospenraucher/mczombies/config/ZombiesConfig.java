package de.knospenraucher.mczombies.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.knospenraucher.mczombies.MCZombies;
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
	private static final int CURRENT_VERSION = 4;

	private static ZombiesConfig instance = new ZombiesConfig();

	/** Version der geladenen Datei; ältere Dateien werden in {@link #migrate()} angepasst. */
	public int configVersion = 0;

	// ---------------------------------------------------------------- Punkte
	/** Startpunkte jedes Spielers. */
	public int startingPoints = 500;
	/** Punkte für jeden Treffer, der einen Zombie nicht tötet. */
	public int pointsPerHit = 10;
	/** Punkte für einen normalen Kill. */
	public int pointsPerKill = 60;
	/** Punkte für einen Nahkampf-Kill (statt pointsPerKill). */
	public int pointsPerMeleeKill = 130;
	/** Punkte für einen Kopftreffer-Kill mit Projektil (statt pointsPerKill). */
	public int pointsPerHeadshotKill = 100;
	/** Toleranz in Blöcken unterhalb der Augenhöhe, ab der ein Projektiltreffer als Kopftreffer zählt. */
	public double headshotTolerance = 0.35;

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
	/** Leben in Runde 1 (2 = ein Herz). 1 = ein Faustschlag. */
	public double healthBase = 1.0;
	/** Zusätzliches Leben pro Runde, bis healthLinearUntilRound. */
	public double healthPerRound = 4.0;
	/** Bis zu dieser Runde steigt das Leben linear ... */
	public int healthLinearUntilRound = 9;
	/** ... danach wird es pro Runde mit diesem Faktor multipliziert. */
	public double healthFactorAfterLinear = 1.1;
	/** Obergrenze (Minecraft erlaubt maximal 1024). */
	public double healthMax = 1024.0;

	/** Laufgeschwindigkeit in Runde 1 (Vanilla-Zombie: 0.23). */
	public double speedBase = 0.20;
	public double speedPerRound = 0.008;
	public double speedMax = 0.33;

	/** Angriffsschaden in Runde 1. */
	public double damageBase = 2.0;
	public double damagePerRound = 0.25;
	public double damageMax = 10.0;

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
				new BoxEntry("mczombies:pistol", 6),
				new BoxEntry("mczombies:smg", 10),
				new BoxEntry("mczombies:shotgun", 10),
				new BoxEntry("mczombies:assault_rifle", 10),
				new BoxEntry("mczombies:lmg", 7),
				new BoxEntry("mczombies:sniper", 6),
				new BoxEntry("mczombies:rocket_launcher", 3),
				new BoxEntry("minecraft:diamond_sword", 4)));
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
	 * Werte der Schusswaffen, Schlüssel = Item-Name ohne Namespace (pistol, smg, ...).
	 * Schaden in halben Herzen; Ticks: 20 = 1 Sekunde.
	 */
	public Map<String, GunStats> guns = defaultGuns();

	/** Preis an der Aufrüst-Maschine. */
	public int upgradePrice = 5000;
	/** Schaden aufgerüsteter Waffen = Schaden × dieser Faktor. */
	public double upgradeDamageMultiplier = 2.0;
	/** Magazin und Reserve aufgerüsteter Waffen = Wert × dieser Faktor. */
	public double upgradeAmmoMultiplier = 1.5;

	/** Standardwerte aller Schusswaffen. */
	public static Map<String, GunStats> defaultGuns() {
		Map<String, GunStats> guns = new LinkedHashMap<>();
		//                         Schaden Magazin Reserve Feuerrate Nachladen Reichweite Streuung Kugeln Auto Durchschlag Explosion Kopf
		guns.put("pistol", new GunStats(6, 8, 80, 5, 30, 48, 0.01, 1, false, 1, 0, 2.0));
		guns.put("smg", new GunStats(4, 32, 192, 2, 40, 40, 0.035, 1, true, 1, 0, 1.5));
		guns.put("assault_rifle", new GunStats(7, 30, 180, 3, 45, 64, 0.02, 1, true, 1, 0, 2.0));
		guns.put("lmg", new GunStats(8, 100, 300, 3, 100, 64, 0.04, 1, true, 2, 0, 1.5));
		guns.put("shotgun", new GunStats(5, 6, 48, 16, 60, 16, 0.12, 8, false, 1, 0, 1.5));
		guns.put("sniper", new GunStats(40, 5, 40, 30, 60, 128, 0.0, 1, false, 4, 0, 3.0));
		guns.put("rocket_launcher", new GunStats(40, 3, 15, 20, 70, 96, 0.0, 1, false, 1, 4.0, 1.0));
		return guns;
	}

	/** Werte einer Schusswaffe. */
	public static class GunStats {
		/** Schaden pro Kugel (bei Raketen: Schaden im Zentrum der Explosion). */
		public double damage;
		/** Schuss pro Magazin. */
		public int magazine;
		/** Maximale Reservemunition. */
		public int reserve;
		/** Ticks zwischen zwei Schüssen. */
		public int fireRateTicks;
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
		/** Explosionsradius in Blöcken (0 = keine Explosion). */
		public double explosionRadius;
		/** Schadensfaktor bei Kopftreffern. */
		public double headshotMultiplier;

		public GunStats(double damage, int magazine, int reserve, int fireRateTicks, int reloadTicks, double range,
				double spread, int pellets, boolean automatic, int penetration, double explosionRadius, double headshotMultiplier) {
			this.damage = damage;
			this.magazine = magazine;
			this.reserve = reserve;
			this.fireRateTicks = fireRateTicks;
			this.reloadTicks = reloadTicks;
			this.range = range;
			this.spread = spread;
			this.pellets = pellets;
			this.automatic = automatic;
			this.penetration = penetration;
			this.explosionRadius = explosionRadius;
			this.headshotMultiplier = headshotMultiplier;
		}
	}

	/** Werte einer Waffe; fehlt sie in der Datei, gelten die Standardwerte. */
	public GunStats gun(String key) {
		GunStats stats = guns != null ? guns.get(key) : null;
		return stats != null ? stats : defaultGuns().get(key);
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
	/** Andere Mobs (Tiere, Monster) während des Spiels aus der Welt entfernen. */
	public boolean removeOtherMobsDuringGame = true;

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
				try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
					ZombiesConfig loaded = GSON.fromJson(reader, ZombiesConfig.class);
					if (loaded != null) {
						loaded.migrate();
						instance = loaded;
					}
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
