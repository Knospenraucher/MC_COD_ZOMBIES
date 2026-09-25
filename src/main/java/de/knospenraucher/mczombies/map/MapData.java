package de.knospenraucher.mczombies.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.knospenraucher.mczombies.MCZombies;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Map-Konfiguration einer Welt: Spawnpunkte, Spieler-Startpunkt, Türen, Fenster,
 * Wandwaffen und Standorte der Zufallskiste.
 * <p>
 * Wird als {@code mczombies_map.json} im Weltordner gespeichert, damit jede Map ihre
 * eigenen Einstellungen mitbringt (Welt kopieren = Map inklusive Setup kopieren).
 */
public class MapData {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "mczombies_map.json";
	/** Zone, die von Anfang an aktiv ist. */
	public static final String START_ZONE = "start";

	// ================================================================ Datentypen

	/** Einfache, JSON-freundliche Blockposition. */
	public static class Pos {
		public int x;
		public int y;
		public int z;

		public Pos(BlockPos pos) {
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
		}

		public BlockPos toBlockPos() {
			return new BlockPos(x, y, z);
		}

		@Override
		public String toString() {
			return x + " " + y + " " + z;
		}
	}

	/** Zombie-Spawnpunkt, der zu einer Zone gehört. */
	public static class SpawnPoint extends Pos {
		/** Zone des Spawnpunkts; null bedeutet {@link #START_ZONE}. */
		public String zone;

		public SpawnPoint(BlockPos pos, String zone) {
			super(pos);
			this.zone = zone;
		}

		public String zone() {
			return zone == null ? START_ZONE : zone;
		}
	}

	/** Gespeicherter Block (Position + Blockzustand als JSON). */
	public static class BlockSnapshot {
		public Pos pos;
		public JsonElement state;
	}

	/** Quader aus zwei Ecken. */
	public static class Region {
		public Pos min;
		public Pos max;

		public boolean contains(BlockPos p) {
			return p.getX() >= min.x && p.getX() <= max.x
					&& p.getY() >= min.y && p.getY() <= max.y
					&& p.getZ() >= min.z && p.getZ() <= max.z;
		}

		public String describe() {
			return min + " bis " + max;
		}
	}

	/** Kaufbare Tür/Barriere: ein Blockbereich, der beim Kauf verschwindet. */
	public static class Door extends Region {
		public String name;
		public int price;
		/** Zone, die beim Öffnen freigeschaltet wird (optional). */
		public String zone;
		public List<BlockSnapshot> blocks = new ArrayList<>();
	}

	/** Fenster-Barrikade: jeder Block im Bereich ist ein Brett. */
	public static class Window extends Region {
		public List<BlockSnapshot> boards = new ArrayList<>();
	}

	/** Wandwaffe: Block, an dem man eine bestimmte Waffe und Munition kauft. */
	public static class WallWeapon {
		public Pos pos;
		/** Item-ID, z.B. {@code minecraft:bow}. */
		public String item;
		public int price;
		public int ammoPrice;
	}

	/** Gespeicherter Inhalt der Datei. */
	private static class Data {
		List<SpawnPoint> zombieSpawns = new ArrayList<>();
		Pos playerSpawn = null;
		List<Door> doors = new ArrayList<>();
		List<Window> windows = new ArrayList<>();
		List<WallWeapon> wallWeapons = new ArrayList<>();
		List<Pos> boxLocations = new ArrayList<>();

		/** Ältere Dateien kennen manche Listen noch nicht. */
		void fillMissing() {
			if (zombieSpawns == null) zombieSpawns = new ArrayList<>();
			if (doors == null) doors = new ArrayList<>();
			if (windows == null) windows = new ArrayList<>();
			if (wallWeapons == null) wallWeapons = new ArrayList<>();
			if (boxLocations == null) boxLocations = new ArrayList<>();
		}
	}

	private final Path file;
	private Data data = new Data();

	private MapData(Path file) {
		this.file = file;
	}

	public static MapData load(MinecraftServer server) {
		MapData map = new MapData(server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME));
		if (Files.exists(map.file)) {
			try (Reader reader = Files.newBufferedReader(map.file, StandardCharsets.UTF_8)) {
				Data loaded = GSON.fromJson(reader, Data.class);
				if (loaded != null) {
					loaded.fillMissing();
					map.data = loaded;
				}
			} catch (Exception e) {
				MCZombies.LOGGER.error("Konnte Map-Daten {} nicht laden", map.file, e);
			}
		}
		return map;
	}

	public void save() {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			GSON.toJson(data, writer);
		} catch (IOException e) {
			MCZombies.LOGGER.error("Konnte Map-Daten {} nicht speichern", file, e);
		}
	}

	/** Hilfe für Befehle: entfernt Element {@code nr} (1-basiert) aus einer Liste. */
	private <T> T removeAt(List<T> list, int nr) {
		if (nr < 1 || nr > list.size()) {
			return null;
		}
		T removed = list.remove(nr - 1);
		save();
		return removed;
	}

	// ================================================================ Zombie-Spawnpunkte

	public List<SpawnPoint> getZombieSpawns() {
		return List.copyOf(data.zombieSpawns);
	}

	/** @return false, wenn an dieser Position schon ein Spawnpunkt existiert */
	public boolean addZombieSpawn(BlockPos pos, String zone) {
		for (SpawnPoint s : data.zombieSpawns) {
			if (s.toBlockPos().equals(pos)) {
				return false;
			}
		}
		data.zombieSpawns.add(new SpawnPoint(pos, START_ZONE.equals(zone) ? null : zone));
		save();
		return true;
	}

	public SpawnPoint removeZombieSpawn(int nr) {
		return removeAt(data.zombieSpawns, nr);
	}

	public void clearZombieSpawns() {
		data.zombieSpawns.clear();
		save();
	}

	// ================================================================ Spieler-Startpunkt

	public BlockPos getPlayerSpawn() {
		return data.playerSpawn == null ? null : data.playerSpawn.toBlockPos();
	}

	public void setPlayerSpawn(BlockPos pos) {
		data.playerSpawn = new Pos(pos);
		save();
	}

	// ================================================================ Türen

	public List<Door> getDoors() {
		return List.copyOf(data.doors);
	}

	public Door getDoor(String name) {
		for (Door door : data.doors) {
			if (door.name.equalsIgnoreCase(name)) {
				return door;
			}
		}
		return null;
	}

	public void addDoor(Door door) {
		data.doors.add(door);
		save();
	}

	public boolean removeDoor(String name) {
		boolean removed = data.doors.removeIf(d -> d.name.equalsIgnoreCase(name));
		if (removed) {
			save();
		}
		return removed;
	}

	// ================================================================ Fenster

	public List<Window> getWindows() {
		return List.copyOf(data.windows);
	}

	public void addWindow(Window window) {
		data.windows.add(window);
		save();
	}

	public Window removeWindow(int nr) {
		return removeAt(data.windows, nr);
	}

	// ================================================================ Wandwaffen

	public List<WallWeapon> getWallWeapons() {
		return List.copyOf(data.wallWeapons);
	}

	public WallWeapon getWallWeaponAt(BlockPos pos) {
		for (WallWeapon w : data.wallWeapons) {
			if (w.pos.toBlockPos().equals(pos)) {
				return w;
			}
		}
		return null;
	}

	/** Ersetzt eine vorhandene Wandwaffe an derselben Position. */
	public void addWallWeapon(WallWeapon weapon) {
		data.wallWeapons.removeIf(w -> w.pos.toBlockPos().equals(weapon.pos.toBlockPos()));
		data.wallWeapons.add(weapon);
		save();
	}

	public WallWeapon removeWallWeapon(int nr) {
		return removeAt(data.wallWeapons, nr);
	}

	// ================================================================ Zufallskiste

	public List<BlockPos> getBoxLocations() {
		List<BlockPos> result = new ArrayList<>();
		for (Pos p : data.boxLocations) {
			result.add(p.toBlockPos());
		}
		return result;
	}

	/** @return false, wenn der Standort schon existiert */
	public boolean addBoxLocation(BlockPos pos) {
		if (getBoxLocations().contains(pos)) {
			return false;
		}
		data.boxLocations.add(new Pos(pos));
		save();
		return true;
	}

	public Pos removeBoxLocation(int nr) {
		return removeAt(data.boxLocations, nr);
	}
}
