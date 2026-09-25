package de.knospenraucher.mczombies.map.prefab;

import de.knospenraucher.mczombies.map.BlockSnapshots;
import de.knospenraucher.mczombies.map.MapData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vorgefertigte Map „Koloss-Fabrik“, frei nach dem Aufbau von „The Giant“:
 * <pre>
 *                 [ Hauptrechner ]   (Aufrüst-Maschine, Zone "rechner")
 *                        |
 * [ Schmelzofen ] -- [ Innenhof ] -- [ Tierlabor ]
 *  (Zone "ofen")   (Start, Statue)   (Zone "labor")
 * </pre>
 * Der Innenhof ist offen, die drei Räume hinter kaufbaren Türen. Zombies kommen aus
 * geschlossenen Käfigen hinter den Fenstern. Norden ist immer -Z, der Ursprung ist die
 * Position des Spielers, der den Befehl ausführt (Mitte des Innenhofs).
 */
public final class KolossMap {
	/** Wandhöhe (Blöcke über dem Boden). */
	private static final int WALL = 5;

	private final ServerLevel level;
	private final MapData map;
	/** Ursprung: Bodenblock in der Mitte des Innenhofs. */
	private final int ox;
	private final int oy;
	private final int oz;

	private KolossMap(ServerLevel level, MapData map, BlockPos feet) {
		this.level = level;
		this.map = map;
		this.ox = feet.getX();
		this.oy = feet.getY() - 1;
		this.oz = feet.getZ();
	}

	/** Baut die Map um die Füße des Spielers und trägt alle Map-Elemente ein. */
	public static void build(ServerLevel level, MapData map, BlockPos feet) {
		new KolossMap(level, map, feet).build();
	}

	private void build() {
		map.clearAll();

		// Platz schaffen und durchgehenden Boden legen.
		fill(-34, 1, -34, 34, 12, 22, Blocks.AIR);
		fill(-34, 0, -34, 34, 0, 22, Blocks.POLISHED_ANDESITE);

		// Erst alle Räume (sie teilen sich Wände mit dem Innenhof), dann der Innenhof,
		// zuletzt Türen, Fenster und Waffen, damit keine Wand sie wieder überbaut.
		room(-26, -8, -10, 8, Blocks.NETHER_BRICKS, Blocks.BRICKS);            // Schmelzofen (Westen)
		room(10, -8, 26, 8, Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_STONE); // Tierlabor (Osten)
		room(-8, -26, 8, -10, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_ANDESITE);       // Hauptrechner (Norden)
		courtyard();

		furnaceDetails();
		labDetails();
		mainframeDetails();
		courtyardDetails();

		map.setPlayerSpawn(pos(0, 1, 6));
	}

	// ================================================================ Innenhof (Start)

	private void courtyard() {
		fill(-10, 0, -10, 10, 0, 10, Blocks.STONE_BRICKS);
		walls(-10, -10, 10, 10, Blocks.STONE_BRICKS);
		// Ein paar verwitterte Steine für die Optik.
		for (int x = -10; x <= 10; x++) {
			for (int z = -10; z <= 10; z++) {
				if ((x * 31 + z * 17 + 100) % 7 == 0) {
					set(x, 0, z, Blocks.MOSSY_STONE_BRICKS);
				}
			}
		}
		// Lichter in den Ecken.
		set(-9, 0, -9, Blocks.SEA_LANTERN);
		set(9, 0, -9, Blocks.SEA_LANTERN);
		set(-9, 0, 9, Blocks.SEA_LANTERN);
		set(9, 0, 9, Blocks.SEA_LANTERN);
		statue();
	}

	private void courtyardDetails() {
		// Zwei Fenster in der Südwand, dahinter Zombie-Käfige.
		window(-6, 1, 10, -5, 2, 10, 0, 1, null);
		window(5, 1, 10, 6, 2, 10, 0, 1, null);

		wallWeapon(-10, 2, -6, "mczombies:pistol", 500, 250);
		wallWeapon(-5, 2, -10, "mczombies:assault_rifle", 1200, 600);
		wallWeapon(5, 2, -10, "mczombies:shotgun", 750, 375);
		box(-8, 1, 8);
	}

	/** Die Statue des Riesen in der Mitte des Hofs. */
	private void statue() {
		fill(-2, 1, -2, 2, 1, 2, Blocks.POLISHED_ANDESITE);
		fill(-1, 2, 0, -1, 4, 0, Blocks.STONE);          // linkes Bein
		fill(1, 2, 0, 1, 4, 0, Blocks.STONE);            // rechtes Bein
		fill(-1, 5, 0, 1, 7, 0, Blocks.STONE);           // Körper
		fill(-2, 6, 0, -2, 7, 0, Blocks.STONE);          // Arme
		fill(2, 6, 0, 2, 7, 0, Blocks.STONE);
		set(-2, 5, 0, Blocks.IRON_BLOCK);                // Fäuste
		set(2, 5, 0, Blocks.IRON_BLOCK);
		set(0, 8, 0, Blocks.CHISELED_STONE_BRICKS);      // Kopf
		set(0, 9, 0, Blocks.CHISELED_STONE_BRICKS);
	}

	// ================================================================ Schmelzofen (Westen)

	private void furnaceDetails() {
		door("ofen_tuer", -10, 1, -1, -10, 3, 1, 750, "ofen");
		for (int x = -24; x <= -12; x += 3) {
			set(x, 1, -7, Blocks.BLAST_FURNACE);
		}
		teleporter(-18, 0);
		window(-26, 1, -1, -26, 2, 0, -1, 0, "ofen");
		wallWeapon(-18, 2, 8, "mczombies:smg", 1000, 500);
		box(-24, 1, -6);
	}

	// ================================================================ Tierlabor (Osten)

	private void labDetails() {
		door("labor_tuer", 10, 1, -1, 10, 3, 1, 750, "labor");
		// Käfige an der Südwand
		for (int x = 13; x <= 21; x += 4) {
			fill(x, 1, 5, x + 2, 3, 5, Blocks.IRON_BARS);
			fill(x, 1, 6, x, 3, 7, Blocks.IRON_BARS);
			fill(x + 2, 1, 6, x + 2, 3, 7, Blocks.IRON_BARS);
		}
		teleporter(18, 0);
		window(26, 1, 0, 26, 2, 1, 1, 0, "labor");
		wallWeapon(18, 2, -8, "mczombies:sniper", 1500, 750);
		box(24, 1, -6);
	}

	// ================================================================ Hauptrechner (Norden)

	private void mainframeDetails() {
		door("rechner_tuer", -1, 1, -10, 1, 3, -10, 1250, "rechner");
		// Rechnerwand an der Nordseite
		for (int x = -6; x <= 6; x++) {
			for (int y = 1; y <= 4; y++) {
				int v = Math.floorMod(x + y, 3);
				set(x, y, -25, v == 0 ? Blocks.REDSTONE_BLOCK : v == 1 ? Blocks.IRON_BLOCK : Blocks.OBSERVER);
			}
		}
		// Aufrüst-Maschine vor dem Rechner
		set(0, 1, -22, Blocks.ANVIL);
		map.addUpgradeMachine(pos(0, 1, -22));
		window(-8, 1, -19, -8, 2, -18, -1, 0, "rechner");
		box(6, 1, -12);
	}

	// ================================================================ Bausteine

	/** Raum mit Wänden, Boden und Decke samt Lampen. Wände teilt er sich mit Nachbarn. */
	private void room(int x1, int z1, int x2, int z2, Block wall, Block floor) {
		fill(x1, 0, z1, x2, 0, z2, floor);
		walls(x1, z1, x2, z2, wall);
		fill(x1, WALL + 1, z1, x2, WALL + 1, z2, wall);
		for (int x = x1 + 2; x < x2; x += 4) {
			for (int z = z1 + 2; z < z2; z += 4) {
				set(x, WALL + 1, z, Blocks.SEA_LANTERN);
			}
		}
	}

	private void walls(int x1, int z1, int x2, int z2, Block wall) {
		fill(x1, 1, z1, x2, WALL, z1, wall);
		fill(x1, 1, z2, x2, WALL, z2, wall);
		fill(x1, 1, z1, x1, WALL, z2, wall);
		fill(x2, 1, z1, x2, WALL, z2, wall);
	}

	/** Kaufbare Tür aus dunklen Brettern, die eine Zone freischaltet. */
	private void door(String name, int x1, int y1, int z1, int x2, int y2, int z2, int price, String zone) {
		fill(x1, y1, z1, x2, y2, z2, Blocks.DARK_OAK_PLANKS);
		MapData.Door door = new MapData.Door();
		door.name = name;
		door.price = price;
		door.zone = zone;
		BlockSnapshots.setCorners(door, pos(x1, y1, z1), pos(x2, y2, z2));
		door.blocks = BlockSnapshots.capture(level, door);
		map.addDoor(door);
	}

	/**
	 * Vernageltes Fenster in einer Außenwand, dahinter ein geschlossener Käfig mit Zombie-Spawnpunkt.
	 *
	 * @param dx,dz Richtung nach draußen (genau eine davon ist ±1)
	 * @param zone  Zone des Spawnpunkts (null = Start)
	 */
	private void window(int x1, int y1, int z1, int x2, int y2, int z2, int dx, int dz, String zone) {
		// Käfig: 3 Blöcke tief, 1 Block breiter als das Fenster auf jeder Seite, innen 2 hoch.
		int sideX = dx == 0 ? 1 : 0;
		int sideZ = dz == 0 ? 1 : 0;
		int ax = x1 - sideX + dx;
		int az = z1 - sideZ + dz;
		int bx = x2 + sideX + dx * 4;
		int bz = z2 + sideZ + dz * 4;
		fill(ax, 0, az, bx, 3, bz, Blocks.COBBLESTONE);
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

	private void box(int x, int y, int z) {
		set(x, y, z, Blocks.CHEST);
		map.addBoxLocation(pos(x, y, z));
	}

	/** Dekorativer Teleporter (noch ohne Funktion). */
	private void teleporter(int x, int z) {
		fill(x - 1, 0, z - 1, x + 1, 0, z + 1, Blocks.IRON_BLOCK);
		set(x, 0, z, Blocks.SEA_LANTERN);
		set(x - 1, 1, z - 1, Blocks.END_ROD);
		set(x + 1, 1, z - 1, Blocks.END_ROD);
		set(x - 1, 1, z + 1, Blocks.END_ROD);
		set(x + 1, 1, z + 1, Blocks.END_ROD);
	}

	private BlockPos pos(int x, int y, int z) {
		return new BlockPos(ox + x, oy + y, oz + z);
	}

	private void set(int x, int y, int z, Block block) {
		level.setBlock(pos(x, y, z), block.defaultBlockState(), 2);
	}

	private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		BlockState state = block.defaultBlockState();
		for (BlockPos p : BlockPos.betweenClosed(pos(x1, y1, z1), pos(x2, y2, z2))) {
			level.setBlock(p, state, 2);
		}
	}
}
