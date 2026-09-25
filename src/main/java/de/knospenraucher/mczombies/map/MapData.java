package de.knospenraucher.mczombies.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 * Map-Konfiguration einer Welt: Zombie-Spawnpunkte und Spieler-Startpunkt.
 * Wird als {@code mczombies_map.json} im Weltordner gespeichert, damit jede Map ihre
 * eigenen Punkte mitbringt (Welt kopieren = Map inklusive Setup kopieren).
 */
public class MapData {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "mczombies_map.json";

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

	/** Gespeicherter Inhalt der Datei. */
	private static class Data {
		List<Pos> zombieSpawns = new ArrayList<>();
		Pos playerSpawn = null;
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
					if (loaded.zombieSpawns == null) {
						loaded.zombieSpawns = new ArrayList<>();
					}
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

	// ---------------------------------------------------------------- Zombie-Spawnpunkte

	public List<BlockPos> getZombieSpawns() {
		List<BlockPos> result = new ArrayList<>();
		for (Pos p : data.zombieSpawns) {
			result.add(p.toBlockPos());
		}
		return result;
	}

	/** @return false, wenn an dieser Position schon ein Spawnpunkt existiert */
	public boolean addZombieSpawn(BlockPos pos) {
		if (getZombieSpawns().contains(pos)) {
			return false;
		}
		data.zombieSpawns.add(new Pos(pos));
		save();
		return true;
	}

	/** Entfernt Spawnpunkt Nummer {@code index} (1-basiert, wie in /zombies spawn list). */
	public BlockPos removeZombieSpawn(int index) {
		if (index < 1 || index > data.zombieSpawns.size()) {
			return null;
		}
		Pos removed = data.zombieSpawns.remove(index - 1);
		save();
		return removed.toBlockPos();
	}

	public void clearZombieSpawns() {
		data.zombieSpawns.clear();
		save();
	}

	// ---------------------------------------------------------------- Spieler-Startpunkt

	public BlockPos getPlayerSpawn() {
		return data.playerSpawn == null ? null : data.playerSpawn.toBlockPos();
	}

	public void setPlayerSpawn(BlockPos pos) {
		data.playerSpawn = new Pos(pos);
		save();
	}
}
