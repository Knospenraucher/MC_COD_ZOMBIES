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

/**
 * Alle Balancing-Werte der Mod. Wird als {@code config/mczombies.json} gespeichert
 * und kann im Spiel mit {@code /zombies reload} neu eingelesen werden.
 * Fehlende Felder in der Datei bekommen automatisch die Standardwerte unten.
 */
public class ZombiesConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mczombies.json");

	private static ZombiesConfig instance = new ZombiesConfig();

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
	/** Leben in Runde 1 (2 = ein Herz). */
	public double healthBase = 8.0;
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

	// ---------------------------------------------------------------- Spieler
	/** Spieler während des Spiels in den Abenteuermodus setzen (verhindert Abbauen der Map). */
	public boolean adventureModeDuringGame = true;
	/** Andere Mobs (Tiere, Monster) während des Spiels aus der Welt entfernen. */
	public boolean removeOtherMobsDuringGame = true;

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
						instance = loaded;
					}
				}
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
