package de.knospenraucher.mczombies.map.prefab;

import de.knospenraucher.mczombies.map.BlockSnapshots;
import de.knospenraucher.mczombies.map.MapData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nachbau der Black-Ops-III-Map „The Giant“ aus Minecraft-Blöcken.
 * Aufbau und Preise nach der Beschreibung im Call-of-Duty-Wiki, Maße frei geschätzt:
 * <pre>
 *                     [ Teleporter C ]  (Tür 1250, Laufsteg mit Kiste)
 *                            |
 * [ Teleporter A ]   [    Innenhof    ]   [ Teleporter B ] (oben, über Treppe)
 *   (Tür 1250)       (tiefer, Generator)
 *        |          ==== Brücke (oben) ====        |
 * [ Tierversuchslabor ] ------------- [ Autowerkstatt ]
 *   (2 Türen à 750)   [ Startbereich ]   (Tür 750)
 *                     (Hauptrechner = Aufrüst-Maschine)
 * </pre>
 * Norden ist -Z. Der Spieler, der den Befehl ausführt, steht danach im Startbereich.
 * Zombies kommen durch vernagelte Fenster (aus Käfigen) oder steigen im Innenhof und in
 * den Teleporterräumen aus dem Boden.
 */
public final class RieseMap {
	/** Abstand des Spieler-Startpunkts vom Nullpunkt der Map (nach Süden). */
	private static final int START_Z = 24;

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

	/** Baut die Map um die Füße des Spielers und trägt alle Map-Elemente ein. */
	public static void build(ServerLevel level, MapData map, BlockPos feet) {
		new RieseMap(level, map, feet).build();
	}

	private void build() {
		map.clearAll();

		// Platz schaffen, Boden legen.
		fill(-50, 1, -76, 50, 16, 34, Blocks.AIR);
		fill(-50, 0, -76, 50, 0, 34, Blocks.POLISHED_ANDESITE);

		// ---- Rohbau: zuerst die hohen Gebäude, dann die tieferen Bereiche, zuletzt der Start.
		room(-44, -14, -14, 24, 0, 11, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE, true);  // Tierversuchslabor
		room(14, -14, 44, 24, 0, 11, Blocks.BRICKS, Blocks.POLISHED_ANDESITE, true);     // Autowerkstatt
		room(-44, -40, -14, -14, 0, 7, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE, true);  // Teleporter A
		teleporterBRoom();
		lowerArea(-14, -40, 14, 10, 10, false, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);  // Innenhof
		lowerArea(-14, -70, 14, -40, 8, true, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_ANDESITE); // Teleporter C
		startArea();

		// ---- Einrichtung und Map-Elemente
		startDetails();
		labDetails();
		garageDetails();
		teleporterADetails();
		teleporterBDetails();
		courtyardDetails();
		teleporterCDetails();
		bridge();

		map.setPlayerSpawn(pos(0, 1, START_Z));
	}

	// ================================================================ Startbereich

	/** Platz vor der Fabrik, oben offen. In der Mitte der Hauptrechner. */
	private void startArea() {
		fill(-14, 0, 10, 14, 0, 28, Blocks.STONE_BRICKS);
		walls(-14, 10, 14, 28, 1, 6, Blocks.STONE_BRICKS);
		// Hauptrechner: Plattform mit vier Rechner-Säulen
		fill(-2, 0, 14, 2, 0, 18, Blocks.IRON_BLOCK);
		for (int[] c : new int[][] {{-3, 13}, {3, 13}, {-3, 19}, {3, 19}}) {
			fill(c[0], 1, c[1], c[0], 4, c[1], Blocks.OBSERVER);
			set(c[0], 5, c[1], Blocks.REDSTONE_BLOCK);
		}
		set(0, 0, 16, Blocks.SEA_LANTERN);
		lights(-12, 11, 12, 27, 0);
	}

	private void startDetails() {
		door("Labor1", -14, 1, 13, -14, 3, 15, 750, "labor,hof");
		door("Labor2", -14, 1, 20, -14, 3, 22, 750, "labor,hof");
		door("Werkstatt", 14, 1, 16, 14, 3, 18, 750, "garage,hof");

		// Aufrüst-Maschine auf dem Hauptrechner
		set(0, 1, 16, Blocks.ANVIL);
		map.addUpgradeMachine(pos(0, 1, 16));

		wallWeapon(4, 2, 10, "mczombies:assault_rifle", 500, 250);   // rechts vom Hauptrechner
		wallWeapon(14, 2, 25, "mczombies:pistol", 500, 250);

		window(-8, 1, 28, -7, 2, 28, 0, 1, null, 0);
		window(7, 1, 28, 8, 2, 28, 0, 1, null, 0);
	}

	// ================================================================ Tierversuchslabor (links)

	private void labDetails() {
		// Obergeschoss zur Brücke mit Treppe
		mezzanine(-43, -15, -13, -2);
		stairs(-42, -40, 3, -1);
		// Käfige für die Versuchstiere
		for (int z = 2; z <= 18; z += 4) {
			fill(-24, 1, z, -20, 3, z, Blocks.IRON_BARS);
			fill(-24, 1, z + 1, -24, 3, z + 2, Blocks.IRON_BARS);
		}
		wallWeapon(-44, 2, 8, "mczombies:smg", 1250, 625);           // Mitte, linke Seite
		wallWeapon(-18, 2, 24, "mczombies:pistol", 750, 375);        // rechts nach der ersten Tür
		box(-40, 1, -10);
		window(-44, 1, 0, -44, 2, 1, -1, 0, "labor", 0);
		window(-44, 1, 16, -44, 2, 17, -1, 0, "labor", 0);
		lights(-42, -12, -16, 22, 11);
	}

	// ================================================================ Autowerkstatt (rechts)

	private void garageDetails() {
		mezzanine(15, 43, -13, -2);
		stairs(40, 42, 3, -1);
		// Schmelzofen hinten in der Werkstatt
		for (int z = 6; z <= 14; z += 2) {
			set(43, 1, z, Blocks.BLAST_FURNACE);
		}
		fill(42, 1, 5, 43, 4, 5, Blocks.BRICKS);
		fill(42, 1, 15, 43, 4, 15, Blocks.BRICKS);
		// Autos (Platzhalter)
		car(22, 8);
		car(30, 16);
		wallWeapon(18, 2, 24, "mczombies:shotgun", 750, 375);        // links beim Reinkommen
		wallWeapon(44, 2, 12, "mczombies:smg", 1300, 650);           // vor dem Ofen
		box(40, 1, 20);
		window(44, 1, 0, 44, 2, 1, 1, 0, "garage", 0);
		lights(16, -12, 42, 22, 11);
	}

	private void car(int x, int z) {
		fill(x, 1, z, x + 4, 1, z + 2, Blocks.POLISHED_BLACKSTONE);
		fill(x + 1, 2, z, x + 3, 2, z + 2, Blocks.GLASS);
		set(x, 0, z, Blocks.COAL_BLOCK); // Ölflecken
		set(x + 4, 0, z + 2, Blocks.COAL_BLOCK);
	}

	// ================================================================ Teleporter A (hinter dem Labor)

	private void teleporterADetails() {
		door("TeleporterA", -30, 1, -14, -28, 3, -14, 1250, "teleporter_a");
		teleporterPad(-29, 0, -27);
		wallWeapon(-34, 2, -40, "mczombies:assault_rifle", 1500, 750); // links vom Teleporter
		box(-40, 1, -36);
		window(-44, 1, -28, -44, 2, -27, -1, 0, "teleporter_a", 0);
		lights(-42, -38, -16, -16, 7);
	}

	// ================================================================ Teleporter B (oben in der Werkstatt)

	private void teleporterBRoom() {
		room(14, -40, 44, -14, 5, 11, Blocks.BRICKS, Blocks.POLISHED_ANDESITE, true);
		fill(14, 1, -40, 44, 4, -14, Blocks.STONE);
	}

	private void teleporterBDetails() {
		// Durchgang vom Obergeschoss der Werkstatt
		fill(28, 6, -14, 30, 8, -14, Blocks.AIR);
		teleporterPad(30, 5, -27);
		map.addZombieSpawn(pos(38, 6, -34), "garage");
		map.addZombieSpawn(pos(20, 6, -34), "garage");
		lights(16, -38, 42, -16, 11);
	}

	// ================================================================ Innenhof (Mitte, tiefer gelegen)

	private void courtyardDetails() {
		// Von Labor und Werkstatt neben der Brücke herunterspringen; eine Stufe führt zurück.
		fill(-14, 1, -12, -14, 3, -10, Blocks.AIR);
		fill(-13, -1, -12, -13, -1, -10, Blocks.STONE_BRICKS);
		fill(14, 1, -12, 14, 3, -10, Blocks.AIR);
		fill(13, -1, -12, 13, -1, -10, Blocks.STONE_BRICKS);

		// Generator mit dem Stromschalter hinten im Hof (Strom kommt in Phase 4)
		fill(-10, -1, -38, -6, 1, -36, Blocks.IRON_BLOCK);
		set(-8, 2, -37, Blocks.REDSTONE_BLOCK);
		set(-8, 0, -35, Blocks.REDSTONE_LAMP);

		wallWeapon(14, 0, -24, "mczombies:smg", 1250, 625);          // Außengang, rechter Eingang
		box(-10, -1, -30);
		map.addZombieSpawn(pos(-8, -1, -20), "hof");
		map.addZombieSpawn(pos(8, -1, -30), "hof");
	}

	// ================================================================ Teleporter C (hinter dem Innenhof)

	private void teleporterCDetails() {
		door("TeleporterC", -1, -1, -40, 1, 1, -40, 1250, "teleporter_c");
		teleporterPad(0, -2, -58);
		// Laufsteg mit Kiste an der Ostseite
		fill(9, -1, -69, 13, 0, -60, Blocks.IRON_BLOCK);
		set(11, -1, -59, Blocks.IRON_BLOCK);
		fill(9, 1, -69, 9, 1, -61, Blocks.IRON_BARS);
		box(11, 1, -66);
		wallWeapon(-14, 0, -64, "mczombies:assault_rifle", 1400, 700); // Ecke links vom Teleporter
		window(-1, -1, -70, 0, 0, -70, 0, -1, "teleporter_c", -2);
		map.addZombieSpawn(pos(-8, -1, -50), "teleporter_c");
		lights(-12, -68, 12, -42, 8);
	}

	// ================================================================ Brücke (oben zwischen Labor und Werkstatt)

	private void bridge() {
		// Durchgänge in den Hofwänden auf Höhe der Obergeschosse
		fill(-14, 6, -8, -14, 8, -6, Blocks.AIR);
		fill(14, 6, -8, 14, 8, -6, Blocks.AIR);
		fill(-13, 5, -8, 13, 5, -6, Blocks.SPRUCE_PLANKS);
		fill(-13, 6, -9, 13, 6, -9, Blocks.IRON_BARS);
		fill(-13, 6, -5, 13, 6, -5, Blocks.IRON_BARS);
		// Im Original öffnet der Strom die Brücke; bis Phase 4 ist sie eine kaufbare Sperre.
		door("Bruecke", 0, 6, -8, 0, 7, -6, 1000, null);
	}

	// ================================================================ Bausteine

	/** Raum mit Boden, Wänden und optional Decke. Wände teilt er sich mit Nachbarn. */
	private void room(int x1, int z1, int x2, int z2, int floorY, int wallTop, Block wall, Block floor, boolean roof) {
		fill(x1, floorY, z1, x2, floorY, z2, floor);
		walls(x1, z1, x2, z2, floorY + 1, wallTop, wall);
		if (roof) {
			fill(x1, wallTop + 1, z1, x2, wallTop + 1, z2, wall);
		}
	}

	/** Tiefer gelegener Bereich (Boden auf y = -2). */
	private void lowerArea(int x1, int z1, int x2, int z2, int wallTop, boolean roof, Block wall, Block floor) {
		fill(x1 + 1, -1, z1 + 1, x2 - 1, 0, z2 - 1, Blocks.AIR);
		room(x1, z1, x2, z2, -2, wallTop, wall, floor, roof);
	}

	private void walls(int x1, int z1, int x2, int z2, int yFrom, int yTo, Block wall) {
		fill(x1, yFrom, z1, x2, yTo, z1, wall);
		fill(x1, yFrom, z2, x2, yTo, z2, wall);
		fill(x1, yFrom, z1, x1, yTo, z2, wall);
		fill(x2, yFrom, z1, x2, yTo, z2, wall);
	}

	/** Obergeschoss (Boden auf y = 5) über einem Teil eines Raums, mit Geländer zur Südseite. */
	private void mezzanine(int x1, int x2, int z1, int z2) {
		fill(x1, 5, z1, x2, 5, z2, Blocks.SPRUCE_PLANKS);
		fill(x1, 6, z2, x2, 6, z2, Blocks.OAK_FENCE);
	}

	/** Treppe aus Blöcken von Süden (zStart) nach Norden hoch auf das Obergeschoss. */
	private void stairs(int x1, int x2, int zStart, int zEnd) {
		int height = 1;
		for (int z = zStart; z >= zEnd && height <= 5; z--, height++) {
			fill(x1, 1, z, x2, height, z, Blocks.STONE_BRICKS);
		}
		// Lücke im Geländer oben an der Treppe
		fill(x1, 6, zEnd - 1, x2, 6, zEnd - 1, Blocks.AIR);
	}

	private void teleporterPad(int x, int floorY, int z) {
		fill(x - 1, floorY, z - 1, x + 1, floorY, z + 1, Blocks.IRON_BLOCK);
		set(x, floorY, z, Blocks.SEA_LANTERN);
		for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
			fill(x + c[0], floorY + 1, z + c[1], x + c[0], floorY + 3, z + c[1], Blocks.IRON_BARS);
			set(x + c[0], floorY + 4, z + c[1], Blocks.END_ROD);
		}
	}

	/** Lampen im Boden (wallTop = 0) oder in der Decke (y = wallTop + 1). */
	private void lights(int x1, int z1, int x2, int z2, int wallTop) {
		int y = wallTop == 0 ? 0 : wallTop + 1;
		for (int x = x1; x <= x2; x += 6) {
			for (int z = z1; z <= z2; z += 6) {
				set(x, y, z, Blocks.SEA_LANTERN);
			}
		}
	}

	/** Kaufbare Tür, die eine oder mehrere Zonen (Komma-getrennt) freischaltet. */
	private void door(String name, int x1, int y1, int z1, int x2, int y2, int z2, int price, String zone) {
		Block block = "Bruecke".equals(name) ? Blocks.IRON_BARS : Blocks.DARK_OAK_PLANKS;
		fill(x1, y1, z1, x2, y2, z2, block);
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
	 * @param dx     Richtung nach draußen in X (-1, 0, 1)
	 * @param dz     Richtung nach draußen in Z (-1, 0, 1)
	 * @param zone   Zone des Spawnpunkts (null = Start)
	 * @param floorY Bodenhöhe vor dem Fenster
	 */
	private void window(int x1, int y1, int z1, int x2, int y2, int z2, int dx, int dz, String zone, int floorY) {
		int sideX = dx == 0 ? 1 : 0;
		int sideZ = dz == 0 ? 1 : 0;
		fill(x1 - sideX + dx, floorY, z1 - sideZ + dz, x2 + sideX + dx * 4, floorY + 3, z2 + sideZ + dz * 4, Blocks.COBBLESTONE);
		fill(x1 + dx, floorY + 1, z1 + dz, x2 + dx * 3, floorY + 2, z2 + dz * 3, Blocks.AIR);

		fill(x1, y1, z1, x2, y2, z2, Blocks.SPRUCE_PLANKS);
		MapData.Window window = new MapData.Window();
		BlockSnapshots.setCorners(window, pos(x1, y1, z1), pos(x2, y2, z2));
		window.boards = BlockSnapshots.capture(level, window);
		map.addWindow(window);

		map.addZombieSpawn(pos(x1 + dx * 3, floorY + 1, z1 + dz * 3), zone);
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
