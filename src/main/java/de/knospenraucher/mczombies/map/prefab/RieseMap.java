package de.knospenraucher.mczombies.map.prefab;

import de.knospenraucher.mczombies.map.BlockSnapshots;
import de.knospenraucher.mczombies.map.MapData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nachbau der Black-Ops-III-Map „The Giant“ aus Minecraft-Blöcken, Schritt für Schritt.
 * Bisher steht der Spawn: der verschneite Hof vor dem Hauptrechner (im Bauplan „Courtyard“
 * links neben „Mainframe“), gebaut nach Screenshots aus dem Spiel.
 * <pre>
 *        Norden: Fabrikwand mit rot glühenden Fenstern, zwei glühende Bögen (Fenster)
 *   [Waffenfabrik ]   [ Plattform mit Hauptrechner-Turm ]        Zaun mit
 *   [Balkon, Tafel]   [ Teleporter-Ring, Aufrüst-Maschine]       Stacheldraht
 *   [Terrasse, Treppe hoch]      [ Treppe ]                      (2 Fenster)
 *                    Spieler-Start
 *        Süden: Uhrturm mit goldener Uhr und rot beleuchtetem Tor
 * </pre>
 * Norden ist -Z. Die Tore auf der Terrasse und unter dem Uhrturm führen später in die
 * nächsten Bereiche; bis die gebaut sind, bleiben sie geschlossen.
 */
public final class RieseMap {
	/** Abstand des Spieler-Startpunkts vom Nullpunkt der Map (nach Süden). */
	private static final int START_Z = 4;

	/** Bodenblock des Hofs; nur darauf fällt Schnee. */
	private static final Block GROUND = Blocks.POLISHED_ANDESITE;

	private final ServerLevel level;
	private final MapData map;
	private final int ox;
	private final int oy;
	private final int oz;

	private RieseMap(ServerLevel level, MapData map, BlockPos feet) {
		this.level = level;
		this.map = map;
		this.ox = feet.getX();
		this.oy = feet.getY() - 1;
		this.oz = feet.getZ() - START_Z;
	}

	/** Name der Vorlage, unter der Umbauten aus dem Bearbeitungsmodus gespeichert werden. */
	public static final String NAME = "riese";

	/** Baut die Map um die Füße des Spielers und trägt alle Map-Elemente ein. */
	public static void build(ServerLevel level, MapData map, BlockPos feet) {
		new RieseMap(level, map, feet).build();
	}

	/** Nullpunkt der Map, wenn der Spieler bei {@code feet} steht (gilt auch für gespeicherte Vorlagen). */
	public static BlockPos origin(BlockPos feet) {
		return new BlockPos(feet.getX(), feet.getY() - 1, feet.getZ() - START_Z);
	}

	private void build() {
		map.clearAll();

		fill(-30, 1, -34, 30, 32, 22, Blocks.AIR);
		fill(-30, 0, -34, 30, 0, 22, GROUND);

		northFactory();
		eastFence();
		clockTower();
		waffenfabrik();
		mainframePlatform();
		mainframeTower(0, -16);
		courtyardProps();

		// Schnee auf dem Hof, der Terrasse und den Mauerkronen
		snow(-21, -25, 21, 13, 1, GROUND);
		snow(-21, -14, -14, 2, 4, Blocks.STONE_BRICKS);
		snow(-22, -26, 22, -26, 13, Blocks.BRICKS);
		snow(-22, -26, -22, 14, 15, Blocks.BRICKS);
		snow(-22, 14, 22, 14, 13, Blocks.BRICKS);
		snow(-5, 14, 5, 18, 21, Blocks.BRICKS);

		map.setPlayerSpawn(pos(0, 1, START_Z));
		map.setTemplate(NAME, pos(0, 0, 0), pos(-30, 0, -34), pos(30, 32, 22));
	}

	// ================================================================ Norden: Fabrikwand

	/** Hohe Backsteinwand hinter dem Hauptrechner, mit rot glühenden Gitterfenstern und zwei Bögen. */
	private void northFactory() {
		fill(-22, 1, -26, 22, 12, -26, Blocks.BRICKS);
		// Rot glühende Fensterreihe, davor ein Gitter
		fill(-11, 7, -26, 11, 9, -26, Blocks.SHROOMLIGHT);
		fill(-11, 7, -25, 11, 9, -25, Blocks.IRON_BARS);

		// Zwei glühende Bögen mit Sandsäcken, dahinter kommen Zombies durch
		for (int x : new int[] {-15, 14}) {
			window(x, 1, -26, x + 1, 2, -26, 0, -1, null);
			fill(x - 1, 3, -26, x + 2, 3, -26, Blocks.SHROOMLIGHT);
			fill(x - 1, 1, -26, x - 1, 2, -26, Blocks.SHROOMLIGHT);
			fill(x + 2, 1, -26, x + 2, 2, -26, Blocks.SHROOMLIGHT);
			fill(x - 3, 1, -25, x - 2, 1, -25, Blocks.SMOOTH_SANDSTONE_SLAB);
			fill(x + 3, 1, -25, x + 4, 1, -25, Blocks.SMOOTH_SANDSTONE_SLAB);
		}

		// Schornsteine hinter der Fabrik
		for (int x : new int[] {-12, 10}) {
			fill(x, 1, -32, x + 1, 30, -31, Blocks.BRICKS);
			fill(x, 31, -32, x + 1, 31, -31, Blocks.CAMPFIRE);
		}
	}

	// ================================================================ Osten: Zaun mit Stacheldraht

	private void eastFence() {
		fill(22, 1, -26, 22, 2, 14, Blocks.BRICKS);
		fill(22, 3, -26, 22, 5, 14, Blocks.IRON_BARS);
		for (int z = -25; z <= 13; z += 2) {
			set(22, 6, z, Blocks.COBWEB);
		}
		window(22, 1, -10, 22, 2, -9, 1, 0, null);
		window(22, 1, 4, 22, 2, 5, 1, 0, null);
	}

	// ================================================================ Süden: Uhrturm

	private void clockTower() {
		fill(-22, 1, 14, 22, 12, 14, Blocks.BRICKS);
		fill(-5, 1, 14, 5, 20, 18, Blocks.BRICKS);
		fill(-6, 13, 14, 6, 13, 14, Blocks.STONE_BRICKS);   // Gesims

		// Goldene Uhr mit Zeigern, darüber eine Lampe
		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				if (dx * dx + dy * dy <= 5) {
					set(dx, 16 + dy, 13, Blocks.RAW_GOLD_BLOCK);
				}
			}
		}
		set(0, 16, 12, Blocks.POLISHED_BLACKSTONE);
		set(0, 17, 12, Blocks.POLISHED_BLACKSTONE);
		set(1, 16, 12, Blocks.POLISHED_BLACKSTONE);
		set(0, 20, 13, Blocks.STONE_BRICKS);
		hangingLantern(0, 19, 13);

		// Rot beleuchtetes Tor (führt später in den nächsten Bereich)
		fill(-1, 1, 14, 1, 3, 14, Blocks.DARK_OAK_PLANKS);
		fill(-2, 1, 14, -2, 3, 14, Blocks.SHROOMLIGHT);
		fill(2, 1, 14, 2, 3, 14, Blocks.SHROOMLIGHT);
		fill(-2, 4, 14, 2, 4, 14, Blocks.SHROOMLIGHT);

		wallWeapon(7, 2, 14, "mczombies:sheiva", 500, 250);

		// Niedrige Mauer mit Stacheldraht vor dem Uhrturm, am Ende die Pistole
		fill(-17, 1, 9, -7, 2, 9, Blocks.BRICKS);
		for (int x = -17; x <= -8; x += 2) {
			set(x, 3, 9, Blocks.COBWEB);
		}
		wallWeapon(-7, 2, 9, "mczombies:rk5", 500, 250);
	}

	// ================================================================ Westen: „Waffenfabrik der Riese“

	/** Fabrikgebäude mit Terrasse, Treppe, Stahlbalkon, Tafel und Kaugummiautomat. */
	private void waffenfabrik() {
		fill(-22, 1, -26, -22, 14, 14, Blocks.BRICKS);
		// Rot glühende Fenster im Obergeschoss
		fill(-22, 9, 0, -22, 11, 10, Blocks.SHROOMLIGHT);
		fill(-21, 9, 0, -21, 11, 10, Blocks.IRON_BARS);

		// Terrasse (Oberkante y = 3) mit niedriger Mauer und Stacheldraht
		fill(-21, 1, -14, -13, 3, 2, Blocks.STONE_BRICKS);
		fill(-13, 4, -14, -13, 4, 2, Blocks.SMOOTH_STONE);
		fill(-21, 4, -14, -14, 4, -14, Blocks.SMOOTH_STONE);
		fill(-21, 4, 2, -14, 4, 2, Blocks.SMOOTH_STONE);
		for (int z = -14; z <= 2; z += 2) {
			set(-13, 5, z, Blocks.COBWEB);
		}

		// Treppe von Osten hinauf, mit Handlauf
		fill(-13, 4, -2, -13, 5, 0, Blocks.AIR);
		BlockState step = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
		for (int h = 1; h <= 3; h++) {
			int x = -9 - h;
			if (h > 1) {
				fill(x, 1, -2, x, h - 1, 0, Blocks.STONE_BRICKS);
			}
			fill(x, h, -2, x, h, 0, step);
			set(x, h + 1, 1, Blocks.IRON_BARS);
		}

		// Stahlbalkon (Gitterrost) über der Terrasse, darüber die Tafel
		fill(-21, 8, -12, -16, 8, -4, Blocks.IRON_TRAPDOOR);
		fill(-16, 9, -12, -16, 9, -4, Blocks.IRON_BARS);
		fill(-21, 9, -12, -17, 9, -12, Blocks.IRON_BARS);
		fill(-21, 9, -4, -17, 9, -4, Blocks.IRON_BARS);
		fill(-16, 4, -12, -16, 7, -12, Blocks.IRON_BARS);
		fill(-16, 4, -4, -16, 7, -4, Blocks.IRON_BARS);
		fill(-22, 10, -10, -22, 12, -6, Blocks.GILDED_BLACKSTONE);
		set(-21, 14, -8, Blocks.IRON_BLOCK);
		hangingLantern(-21, 13, -8);

		// Kaugummiautomat in der Ecke unter dem Balkon (funktioniert ab Phase 4)
		set(-21, 4, -11, Blocks.REDSTONE_BLOCK);
		set(-21, 5, -11, Blocks.GLASS);
		hangingLantern(-21, 7, -11);

		// Tor zum nächsten Bereich (später kaufbar)
		fill(-22, 4, -1, -22, 6, 1, Blocks.DARK_OAK_PLANKS);
	}

	// ================================================================ Mitte: Plattform des Hauptrechners

	/** Erhöhte Plattform (Oberkante y = 4) mit breiter Treppe von Süden und Geländer. */
	private void mainframePlatform() {
		fill(-8, 1, -22, 8, 4, -6, Blocks.STONE_BRICKS);
		fill(-8, 4, -22, 8, 4, -6, Blocks.SMOOTH_STONE);
		roundPad(0, 4, -9);

		// Treppe: vier Stufen, von Süden nach Norden ansteigend
		BlockState step = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
		for (int h = 1; h <= 4; h++) {
			int z = -1 - h;
			if (h > 1) {
				fill(-2, 1, z, 2, h - 1, z, Blocks.STONE_BRICKS);
			}
			fill(-2, h, z, 2, h, z, step);
			set(-3, h + 1, z, Blocks.IRON_BARS);
			set(3, h + 1, z, Blocks.IRON_BARS);
		}

		// Geländer rundherum, vorne mit Lücke für die Treppe
		fill(-8, 5, -22, 8, 5, -22, Blocks.IRON_BARS);
		fill(-8, 5, -22, -8, 5, -6, Blocks.IRON_BARS);
		fill(8, 5, -22, 8, 5, -6, Blocks.IRON_BARS);
		fill(-8, 5, -6, -3, 5, -6, Blocks.IRON_BARS);
		fill(3, 5, -6, 8, 5, -6, Blocks.IRON_BARS);
	}

	/**
	 * Hauptrechner-Turm: gerippter Sockel, blau leuchtende Kuppel, oben drei glühende Kugeln und
	 * ein Leuchtfeuer (der Strahl in den Himmel). Vorne im Sockel sitzt die Aufrüst-Maschine.
	 */
	private void mainframeTower(int cx, int cz) {
		for (int y = 5; y <= 16; y++) {
			Block ring;
			if (y <= 10) {
				ring = y % 2 == 0 ? Blocks.IRON_BLOCK : Blocks.POLISHED_DEEPSLATE;
			} else {
				ring = Blocks.BLUE_ICE;
			}
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					int d2 = dx * dx + dz * dz;
					if (d2 >= 5 && d2 <= 10) {
						set(cx + dx, y, cz + dz, ring);
					}
				}
			}
			set(cx, y, cz, Blocks.SEA_LANTERN);
		}
		// Deckel, Leuchtfeuer, Kugeln an Auslegern (links, rechts, vorne)
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (dx * dx + dz * dz <= 5) {
					set(cx + dx, 17, cz + dz, Blocks.IRON_BLOCK);
				}
			}
		}
		fill(cx - 1, 18, cz - 1, cx + 1, 18, cz + 1, Blocks.IRON_BLOCK);
		set(cx, 19, cz, Blocks.BEACON);
		for (int[] arm : new int[][] {{-1, 0}, {1, 0}, {0, 1}}) {
			set(cx + arm[0] * 2, 19, cz + arm[1] * 2, Blocks.IRON_BARS);
			set(cx + arm[0] * 3, 19, cz + arm[1] * 3, Blocks.SHROOMLIGHT);
		}

		// Aufrüst-Maschine vorne im Sockel, darüber ein Sichtfenster
		set(cx, 5, cz + 3, Blocks.ANVIL);
		fill(cx - 1, 6, cz + 3, cx + 1, 7, cz + 3, Blocks.GLASS);
		map.addUpgradeMachine(pos(cx, 5, cz + 3));
	}

	// ================================================================ Kleinkram im Hof

	private void courtyardProps() {
		powerPole(12, 8);
		powerPole(-14, -19);
		lampPost(18, -20);
		lampPost(-18, 11);
		lampPost(16, 11);
		set(19, 1, 0, Blocks.CAMPFIRE);     // Feuertonne am Zaun
	}

	// ================================================================ Bausteine

	/** Runde Teleporter-Plattform: Eisenkern mit Leuchtring. */
	private void roundPad(int x, int y, int z) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 <= 5) {
					set(x + dx, y, z + dz, d2 <= 1 ? Blocks.IRON_BLOCK : Blocks.SEA_LANTERN);
				}
			}
		}
	}

	/** Strommast mit Querbalken und Isolatoren. */
	private void powerPole(int x, int z) {
		fill(x, 1, z, x, 13, z, Blocks.SPRUCE_FENCE);
		fill(x - 2, 13, z, x + 2, 13, z, Blocks.SPRUCE_PLANKS);
		set(x - 2, 14, z, Blocks.END_ROD);
		set(x + 2, 14, z, Blocks.END_ROD);
	}

	/** Laterne auf einem Pfosten. */
	private void lampPost(int x, int z) {
		fill(x, 1, z, x, 3, z, Blocks.SPRUCE_FENCE);
		set(x, 4, z, Blocks.LANTERN);
	}

	/** Hängende Laterne; darüber muss ein Block sein. */
	private void hangingLantern(int x, int y, int z) {
		level.setBlock(pos(x, y, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 2);
	}

	/** Schneedecke: auf jeden freien Platz in Höhe y, unter dem der angegebene Block liegt. */
	private void snow(int x1, int z1, int x2, int z2, int y, Block ground) {
		BlockState snow = Blocks.SNOW.defaultBlockState();
		for (int x = x1; x <= x2; x++) {
			for (int z = z1; z <= z2; z++) {
				// Ein paar Stellen bleiben frei, damit es nicht wie ein Teppich aussieht.
				if (Math.floorMod(x * 7 + z * 13, 11) < 2) {
					continue;
				}
				BlockPos p = pos(x, y, z);
				if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(ground)) {
					level.setBlock(p, snow, 2);
				}
			}
		}
	}

	/**
	 * Vernageltes Fenster in einer Außenwand (Boden y = 0), dahinter ein geschlossener Käfig
	 * mit Zombie-Spawnpunkt.
	 *
	 * @param dx   Richtung nach draußen in X (-1, 0, 1)
	 * @param dz   Richtung nach draußen in Z (-1, 0, 1)
	 * @param zone Zone des Spawnpunkts (null = Start)
	 */
	private void window(int x1, int y1, int z1, int x2, int y2, int z2, int dx, int dz, String zone) {
		int sideX = dx == 0 ? 1 : 0;
		int sideZ = dz == 0 ? 1 : 0;
		fill(x1 - sideX + dx, 0, z1 - sideZ + dz, x2 + sideX + dx * 4, 3, z2 + sideZ + dz * 4, Blocks.COBBLESTONE);
		fill(x1 + dx, 1, z1 + dz, x2 + dx * 3, 2, z2 + dz * 3, Blocks.AIR);

		fill(x1, y1, z1, x2, y2, z2, Blocks.SPRUCE_PLANKS);
		MapData.Window window = new MapData.Window();
		BlockSnapshots.setCorners(window, pos(x1, y1, z1), pos(x2, y2, z2));
		window.boards = BlockSnapshots.capture(level, window);
		map.addWindow(window);

		map.addZombieSpawn(pos(x1 + dx * 3, 1, z1 + dz * 3), zone);
	}

	/** Wandwaffe: ein Goldblock in der Wand. */
	private void wallWeapon(int x, int y, int z, String item, int price, int ammoPrice) {
		set(x, y, z, Blocks.GOLD_BLOCK);
		MapData.WallWeapon weapon = new MapData.WallWeapon();
		weapon.pos = new MapData.Pos(pos(x, y, z));
		weapon.item = item;
		weapon.price = price;
		weapon.ammoPrice = ammoPrice;
		map.addWallWeapon(weapon);
	}

	private BlockPos pos(int x, int y, int z) {
		return new BlockPos(ox + x, oy + y, oz + z);
	}

	private void set(int x, int y, int z, Block block) {
		level.setBlock(pos(x, y, z), block.defaultBlockState(), 2);
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
