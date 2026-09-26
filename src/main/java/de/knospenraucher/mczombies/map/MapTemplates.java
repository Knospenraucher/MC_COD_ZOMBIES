package de.knospenraucher.mczombies.map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import de.knospenraucher.mczombies.MCZombies;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Eigene Map-Vorlagen: Blöcke eines Bereichs plus alle Map-Elemente (Türen, Fenster, Wandwaffen …),
 * gespeichert unter {@code config/mczombies/maps/<name>.json.gz}.
 * <p>
 * Liegt dort eine Vorlage, baut {@code /zombies buildmap <name>} sie statt der eingebauten Map.
 * So gilt ein Umbau aus dem Bearbeitungsmodus für alle Welten. Gespeichert werden nur Blockzustände,
 * keine Inhalte von Truhen oder Schildern.
 */
public final class MapTemplates {
	private static final Gson GSON = new Gson();
	private static final int VERSION = 1;

	private MapTemplates() {
	}

	private static Path file(String name) {
		return FabricLoader.getInstance().getConfigDir().resolve("mczombies").resolve("maps").resolve(name + ".json.gz");
	}

	public static boolean exists(String name) {
		return Files.exists(file(name));
	}

	/** @return true, wenn eine Vorlage gelöscht wurde */
	public static boolean delete(String name) throws IOException {
		return Files.deleteIfExists(file(name));
	}

	public static Path location(String name) {
		return file(name);
	}

	/**
	 * Speichert den Bereich der aktuellen Map als Vorlage.
	 *
	 * @return Anzahl der gespeicherten Nicht-Luft-Blöcke
	 */
	public static int save(ServerLevel level, MapData map, String name) throws IOException {
		MapData.TemplateInfo info = map.getTemplate();
		BlockPos min = info.min.toBlockPos();
		BlockPos max = info.max.toBlockPos();
		BlockPos origin = info.origin.toBlockPos();

		// Palette: jeder vorkommende Blockzustand einmal, die Blöcke verweisen per Index darauf.
		Map<BlockState, Integer> index = new HashMap<>();
		JsonArray palette = new JsonArray();
		JsonArray blocks = new JsonArray();
		int solid = 0;
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = level.getBlockState(pos);
			Integer i = index.get(state);
			if (i == null) {
				JsonElement json = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).result().orElse(null);
				if (json == null) {
					state = Blocks.AIR.defaultBlockState();
					i = index.get(state);
				}
				if (i == null) {
					i = palette.size();
					index.put(state, i);
					palette.add(json != null ? json : BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).result().orElseThrow());
				}
			}
			blocks.add(i);
			if (!state.isAir()) {
				solid++;
			}
		}

		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		root.add("size", ints(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1));
		root.add("originOffset", ints(origin.getX() - min.getX(), origin.getY() - min.getY(), origin.getZ() - min.getZ()));
		root.add("palette", palette);
		root.add("blocks", blocks);
		root.add("map", map.exportRelative(origin));

		Path path = file(name);
		Files.createDirectories(path.getParent());
		try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8)) {
			GSON.toJson(root, writer);
		}
		return solid;
	}

	/**
	 * Setzt eine gespeicherte Vorlage so in die Welt, dass ihr Bezugspunkt auf {@code origin} liegt,
	 * und übernimmt ihre Map-Elemente.
	 */
	public static void paste(ServerLevel level, MapData map, String name, BlockPos origin) throws IOException {
		JsonObject root;
		try (Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(file(name))), StandardCharsets.UTF_8)) {
			root = JsonParser.parseReader(reader).getAsJsonObject();
		}
		int[] size = toInts(root.getAsJsonArray("size"));
		int[] offset = toInts(root.getAsJsonArray("originOffset"));
		BlockPos min = origin.offset(-offset[0], -offset[1], -offset[2]);
		BlockPos max = min.offset(size[0] - 1, size[1] - 1, size[2] - 1);

		List<BlockState> palette = new ArrayList<>();
		for (JsonElement json : root.getAsJsonArray("palette")) {
			BlockState state = BlockState.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
			if (state == null) {
				MCZombies.LOGGER.warn("Unbekannter Block in Vorlage {}: {}", name, json);
				state = Blocks.AIR.defaultBlockState();
			}
			palette.add(state);
		}

		// Gleiche Reihenfolge wie beim Speichern (BlockPos.betweenClosed).
		JsonArray blocks = root.getAsJsonArray("blocks");
		int i = 0;
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			level.setBlock(pos, palette.get(blocks.get(i++).getAsInt()), 2);
		}

		map.clearAll();
		map.importRelative(root.get("map"), origin);
		map.setTemplate(name, origin, min, max);
	}

	private static JsonArray ints(int... values) {
		JsonArray array = new JsonArray();
		for (int v : values) {
			array.add(v);
		}
		return array;
	}

	private static int[] toInts(JsonArray array) {
		int[] result = new int[array.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = array.get(i).getAsInt();
		}
		return result;
	}
}
