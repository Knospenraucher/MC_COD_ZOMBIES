package de.knospenraucher.mczombies.map.prefab;

import de.knospenraucher.mczombies.map.BlockSnapshots;
import de.knospenraucher.mczombies.map.MapData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kleine Testmap nach „Nacht der Untoten“ (Beschreibung aus dem Fandom-Wiki): ein Farmhaus mit
 * Startraum, „Help“-Raum und Obergeschoss aus zwei Räumen.
 * <pre>
 *            Norden
 *   +---------------------+----------------+
 *   | Startraum  (5 Fenster) | Help-Raum     |   Obergeschoss darüber:
 *   |  Sheiva 200            | Kiste (fest)  |   Westraum: Marshal 16, KRM-262, Scharfschützenschrank
 *   |  M8A7 600              | 205 Brecci    |   Ostraum: Man-O-War, Pack-a-Punch
 *   | Treppe mit Sofa        | M1927         |   4 Fenster
 *   |  (1000)     Start  Tür „Help“ (1000) Treppe mit Schutt (1000)
 *   +---------------------+----------------+
 * </pre>
 * Waffen aus World at War sind durch ähnliche BO3-Waffen ersetzt (Kar98k → Sheiva, M1A1 → M8A7,
 * Doppelflinte → 205 Brecci, Thompson → M1927, Abgesägte → Marshal 16, Trench Gun → KRM-262,
 * BAR → Man-O-War, Kar98k mit Zielfernrohr → Drakon). Handgranaten und Mule Kick fehlen noch (Phase 4).
 * Norden ist -Z, Boden y = 0, Obergeschoss-Boden y = 6.
 */
public final class NachtMap {
	/** Name der Vorlage für den Bearbeitungsmodus. */
	public static final String NAME = "nacht";
	/** Abstand des Spieler-Startpunkts vom Nullpunkt der Map (nach Süden). */
	private static final int START_Z = 4;
	/** Höhe des Obergeschoss-Bodens. */
	private static final int UP = 6;

	private static final String HELP = "help";
	private static final String UPSTAIRS = "oben";

	private final ServerLevel level;
	private final MapData map;
	private final int ox;
	private final int oy;
	private final int oz;

	private NachtMap(ServerLevel level, MapData map, BlockPos feet) {
		BlockPos origin = origin(feet);
		this.level = level;
		this.map = map;
		this.ox = origin.getX();
		this.oy = origin.getY();
		this.oz = origin.getZ();
	}

	/** Baut die Map um die Füße des Spielers und trägt alle Map-Elemente ein. */
	public static void build(ServerLevel level, MapData map, BlockPos feet) {
		new NachtMap(level, map, feet).build();
	}

	/** Nullpunkt der Map, wenn der Spieler bei {@code feet} steht. */
	public static BlockPos origin(BlockPos feet) {
		return new BlockPos(feet.getX(), feet.getY() - 1, feet.getZ() - START_Z);
	}

	private void build() {
		map.clearAll();

		fill(-18, 1, -16, 30, 16, 16, Blocks.AIR);
		fill(-18, 0, -16, 30, 0, 16, Blocks.COARSE_DIRT);

		house();
		stairs(-10, "sofa", Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_STAIRS);
		stairs(20, "schutt", Blocks.COBBLESTONE, Blocks.COBBLESTONE_STAIRS);
		helpDoor();
		windows();
		weapons();
		boxAndPackAPunch();
		decoration();

		map.setPlayerSpawn(pos(0, 1, START_Z));
		map.setTemplate(NAME, pos(0, 0, 0), pos(-18, 0, -16), pos(30, 16, 16));
	}

	// ================================================================ Haus

	private void house() {
		// Außenwände aus gemischten Steinziegeln, Holzbalken an den Ecken
		for (int y = 0; y <= 12; y++) {
			for (int x = -11; x <= 23; x++) {
				wall(x, y, -9);
				wall(x, y, 9);
			}
			for (int z = -9; z <= 9; z++) {
				wall(-11, y, z);
				wall(23, y, z);
			}
		}
		for (int[] c : new int[][] {{-11, -9}, {23, -9}, {-11, 9}, {23, 9}}) {
			fill(c[0], 1, c[1], c[0], 12, c[1], Blocks.DARK_OAK_LOG);
		}
		// Holzboden unten, Zwischendecke, Dach
		fill(-10, 0, -8, 22, 0, 8, Blocks.OAK_PLANKS);
		fill(-10, UP, -8, 22, UP, 8, Blocks.SPRUCE_PLANKS);
		fill(-11, 12, -9, 23, 12, 9, Blocks.DARK_OAK_PLANKS);
		// Deckenbalken
		for (int x = -8; x <= 20; x += 7) {
			fill(x, UP, -8, x, UP, 8, Blocks.STRIPPED_DARK_OAK_LOG);
			fill(x, 12, -8, x, 12, 8, Blocks.STRIPPED_DARK_OAK_LOG);
		}
		// Trennwand zwischen Startraum und Help-Raum, oben mit Durchgang
		fill(11, 1, -8, 11, 11, 8, Blocks.STONE_BRICKS);
		fill(11, UP + 1, -2, 11, UP + 3, 0, Blocks.AIR);
		fill(11, UP + 4, -2, 11, UP + 4, 0, Blocks.STRIPPED_DARK_OAK_LOG);
	}

	/** Steinwand mit zufällig wirkender (aber fester) Mischung aus rissigen und bemoosten Ziegeln. */
	private void wall(int x, int y, int z) {
		int h = Math.floorMod(x * 31 + y * 17 + z * 7, 11);
		Block block = h == 0 ? Blocks.MOSSY_STONE_BRICKS : h == 1 || h == 2 ? Blocks.CRACKED_STONE_BRICKS
				: h == 3 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS;
		set(x, y, z, block);
	}

	/**
	 * Treppe an der Wand (x bis x+2), nach Norden ansteigend von z = 7 bis z = 2, mit kaufbarer
	 * Barrikade auf halber Höhe, die das Obergeschoss freischaltet.
	 */
	private void stairs(int x, String doorName, Block barricade, Block barricadeStairs) {
		fill(x, UP, 3, x + 2, UP, 8, Blocks.AIR);
		for (int k = 0; k < UP; k++) {
			int z = 7 - k;
			int y = 1 + k;
			if (y > 1) {
				fill(x, 1, z, x + 2, y - 1, z, Blocks.STONE_BRICKS);
			}
			fill(x, y, z, x + 2, y, z, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
					.setValue(StairBlock.FACING, Direction.NORTH));
		}
		// Geländer zum Raum hin
		int rail = x == -10 ? x + 3 : x - 1;
		for (int k = 1; k < UP; k++) {
			set(rail, 2 + k, 7 - k, Blocks.SPRUCE_FENCE);
		}

		// Barrikade (Sofa bzw. Schutt) auf der Treppe
		fill(x, 4, 5, x + 2, 4, 5, barricade);
		fill(x, 5, 5, x + 2, 5, 5, barricadeStairs.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
		door(doorName, x, 4, 5, x + 2, 5, 5, 1000, UPSTAIRS);
	}

	/** Tür rechts vom Startpunkt in den Help-Raum („HELP“, das P ist unfertig). */
	private void helpDoor() {
		fill(11, 1, 2, 11, 3, 4, Blocks.SPRUCE_PLANKS);
		set(11, 2, 3, Blocks.REDSTONE_BLOCK);
		door("help", 11, 1, 2, 11, 3, 4, 1000, HELP);
		fill(11, 4, 1, 11, 4, 5, Blocks.STRIPPED_DARK_OAK_LOG);
	}

	// ================================================================ Fenster

	private void windows() {
		// Startraum: fünf Fenster
		window(-6, 1, -9, -5, 2, -9, 0, -1, null);
		window(4, 1, -9, 5, 2, -9, 0, -1, null);
		window(-11, 1, -5, -11, 2, -4, -1, 0, null);
		window(-11, 1, -1, -11, 2, 0, -1, 0, null);
		window(3, 1, 9, 4, 2, 9, 0, 1, null);
		// Help-Raum: zwei Fenster und die Höhle hinter der Nordwand
		window(23, 1, -4, 23, 2, -3, 1, 0, HELP);
		window(15, 1, 9, 16, 2, 9, 0, 1, HELP);
		window(16, 1, -9, 17, 2, -9, 0, -1, HELP);
		cave(16, 17);
		// Obergeschoss: vier Fenster
		window(-4, UP + 1, -9, -3, UP + 2, -9, 0, -1, UPSTAIRS);
		window(14, UP + 1, -9, 15, UP + 2, -9, 0, -1, UPSTAIRS);
		window(-11, UP + 1, -6, -11, UP + 2, -5, -1, 0, UPSTAIRS);
		window(23, UP + 1, -6, 23, UP + 2, -5, 1, 0, UPSTAIRS);
	}

	/**
	 * Vernageltes Fenster in einer Außenwand, dahinter ein geschlossener Käfig mit Zombie-Spawnpunkt.
	 *
	 * @param dx Richtung nach draußen in X (-1, 0, 1)
	 * @param dz Richtung nach draußen in Z (-1, 0, 1)
	 */
	private void window(int x1, int y1, int z1, int x2, int y2, int z2, int dx, int dz, String zone) {
		int sideX = dx == 0 ? 1 : 0;
		int sideZ = dz == 0 ? 1 : 0;
		fill(x1 - sideX + dx, y1 - 1, z1 - sideZ + dz, x2 + sideX + dx * 4, y1 + 2, z2 + sideZ + dz * 4, Blocks.COBBLESTONE);
		fill(x1 + dx, y1, z1 + dz, x2 + dx * 3, y1 + 1, z2 + dz * 3, Blocks.AIR);

		fill(x1, y1, z1, x2, y2, z2, Blocks.SPRUCE_PLANKS);
		MapData.Window window = new MapData.Window();
		BlockSnapshots.setCorners(window, pos(x1, y1, z1), pos(x2, y2, z2));
		window.boards = BlockSnapshots.capture(level, window);
		map.addWindow(window);

		map.addZombieSpawn(pos(x1 + dx * 3, y1, z1 + dz * 3), zone);
	}

	/** Die Sackgassen-Höhle hinter dem Nordfenster des Help-Raums: der Käfig wird zu einem Felsen. */
	private void cave(int x1, int x2) {
		for (int x = x1 - 2; x <= x2 + 2; x++) {
			for (int z = -15; z <= -10; z++) {
				for (int y = 0; y <= 5; y++) {
					boolean inside = x >= x1 && x <= x2 && z >= -12 && y >= 1 && y <= 2;
					if (!inside && level.getBlockState(pos(x, y, z)).isAir()) {
						set(x, y, z, (x + y + z) % 3 == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE);
					}
				}
			}
		}
	}

	// ================================================================ Waffen, Kiste, Pack-a-Punch

	private void weapons() {
		// Startraum: Kar98k → Sheiva, M1A1 Carbine → M8A7
		wallWeapon(0, 2, -9, "mczombies:sheiva", 200);
		wallWeapon(-4, 2, 9, "mczombies:m8a7", 600);
		// Help-Raum: Doppelflinte → 205 Brecci, Thompson → M1927
		wallWeapon(23, 2, -7, "mczombies:205_brecci", 1200);
		wallWeapon(13, 2, -9, "mczombies:m1927", 1200);
		// Obergeschoss: Abgesägte → Marshal 16, Trench Gun → KRM-262, BAR → Man-O-War
		wallWeapon(3, UP + 2, -9, "mczombies:marshal_16", 1200);
		wallWeapon(-2, UP + 2, 9, "mczombies:krm_262", 1500);
		wallWeapon(23, UP + 2, 1, "mczombies:man_o_war", 1800);
		// Scharfschützenschrank: Kar98k mit Zielfernrohr → Drakon, Locus
		fill(-10, UP + 1, -3, -10, UP + 4, 0, Blocks.SPRUCE_PLANKS);
		fill(-10, UP + 2, -2, -10, UP + 3, -1, Blocks.AIR);
		wallWeapon(-11, UP + 2, -2, "mczombies:drakon", 1500);
		wallWeapon(-11, UP + 3, -1, "mczombies:locus", 5000);
		set(-10, UP + 1, -2, Blocks.SPRUCE_SLAB);
		set(-10, UP + 1, -1, Blocks.SPRUCE_SLAB);
	}

	private void boxAndPackAPunch() {
		// Zufallskiste im Help-Raum, nur ein Standort: sie zieht nie um.
		set(19, 1, -8, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
		map.addBoxLocation(pos(19, 1, -8));
		set(18, 1, -8, Blocks.CANDLE);
		set(20, 1, -8, Blocks.CANDLE);

		// Pack-a-Punch in der Ecke des oberen Ostraums
		fill(19, UP + 1, -8, 22, UP + 1, -6, Blocks.POLISHED_BLACKSTONE);
		fill(20, UP + 2, -8, 22, UP + 3, -8, Blocks.GILDED_BLACKSTONE);
		set(21, UP + 4, -8, Blocks.SHROOMLIGHT);
		set(21, UP + 2, -7, Blocks.ANVIL);
		set(20, UP + 2, -7, Blocks.IRON_BLOCK);
		set(22, UP + 2, -7, Blocks.IRON_BLOCK);
		map.addUpgradeMachine(pos(21, UP + 2, -7));
	}

	private void wallWeapon(int x, int y, int z, String item, int price) {
		set(x, y, z, Blocks.GOLD_BLOCK);
		MapData.WallWeapon weapon = new MapData.WallWeapon();
		weapon.pos = new MapData.Pos(pos(x, y, z));
		weapon.item = item;
		weapon.price = price;
		weapon.ammoPrice = price / 2;
		map.addWallWeapon(weapon);
	}

	private void door(String name, int x1, int y1, int z1, int x2, int y2, int z2, int price, String zone) {
		MapData.Door door = new MapData.Door();
		door.name = name;
		door.price = price;
		door.zone = zone;
		BlockSnapshots.setCorners(door, pos(x1, y1, z1), pos(x2, y2, z2));
		door.blocks = BlockSnapshots.capture(level, door);
		map.addDoor(door);
	}

	// ================================================================ Einrichtung

	private void decoration() {
		// Lampen an den Decken
		for (int x : new int[] {-5, 4, 16}) {
			for (int z : new int[] {-4, 3}) {
				hangingLantern(x, UP - 1, z);
				hangingLantern(x, 11, z);
			}
		}
		// Startraum: Tisch und umgekippte Stühle
		fill(-3, 1, -3, -1, 1, -3, Blocks.SPRUCE_SLAB);
		set(-4, 1, -2, Blocks.SPRUCE_STAIRS);
		set(0, 1, -4, Blocks.BARREL);
		// Help-Raum: Kisten und Heu
		set(21, 1, -7, Blocks.BARREL);
		set(13, 1, 7, Blocks.HAY_BLOCK);
		set(13, 2, 7, Blocks.HAY_BLOCK);
		// Obergeschoss: Schutt, der den Weg verengt
		int[][] rubble = {{-6, 2}, {-5, 2}, {-6, 3}, {2, -3}, {3, -3}, {6, 5}, {7, 5}, {7, 4}, {14, 3}, {15, 3}, {17, -1}, {18, -1}};
		for (int[] r : rubble) {
			set(r[0], UP + 1, r[1], Math.floorMod(r[0] + r[1], 2) == 0 ? Blocks.COBBLESTONE : Blocks.COBBLESTONE_SLAB);
		}
		set(-6, UP + 2, 2, Blocks.COBBLESTONE_SLAB);
		set(0, UP + 1, 6, Blocks.BARREL);
		set(1, UP + 1, 6, Blocks.BARREL);
		set(1, UP + 2, 6, Blocks.BARREL);
		set(8, UP + 1, -7, Blocks.COBWEB);
		set(12, UP + 1, 7, Blocks.COBWEB);
	}

	private void hangingLantern(int x, int y, int z) {
		level.setBlock(pos(x, y, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
	}

	// ================================================================ Hilfen

	private BlockPos pos(int x, int y, int z) {
		return new BlockPos(ox + x, oy + y, oz + z);
	}

	private void set(int x, int y, int z, Block block) {
		set(x, y, z, block.defaultBlockState());
	}

	private void set(int x, int y, int z, BlockState state) {
		level.setBlock(pos(x, y, z), state, 2);
	}

	private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
	}

	private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
		for (BlockPos p : BlockPos.betweenClosed(pos(x1, y1, z1), pos(x2, y2, z2))) {
			level.setBlock(p, state, 2);
		}
	}
}
