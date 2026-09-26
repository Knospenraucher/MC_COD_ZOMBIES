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
 * Grundriss nach dem Bauplan vom Ladebildschirm, Aussehen nach Screenshots, Maße geschätzt:
 * <pre>
 *                  [ Teleporter B ]            (Kran, Schornsteine)
 *                  [  Ofenraum    ][ Werkstatt/Hangar ]
 *                  [ Generatoren (oben) ][ Obergeschoss ]
 * [Tele C][ Hof links ]|[ Innenhof  ~Brücke~ ]|[ Start: Hauptrechner ]
 *                      [ Tierversuche ][ Labor ]
 *                                     [ Teleporter A ]
 * </pre>
 * Norden ist -Z. Der Spieler, der den Befehl ausführt, steht danach im Start vor dem Hauptrechner.
 * Zombies kommen durch vernagelte Fenster (aus Käfigen hinter der Wand) oder steigen in den
 * Höfen und Teleporterräumen aus dem Boden.
 */
public final class RieseMap {
	/** Abstand des Spieler-Startpunkts vom Nullpunkt der Map (nach Osten). */
	private static final int START_X = 8;

	/** Bodenblock der offenen Flächen; nur darauf fällt Schnee. */
	private static final Block OUTDOOR_FLOOR = Blocks.POLISHED_ANDESITE;

	private final ServerLevel level;
	private final MapData map;
	private final int ox;
	private final int oy;
	private final int oz;

	private RieseMap(ServerLevel level, MapData map, BlockPos feet) {
		this.level = level;
		this.map = map;
		this.ox = feet.getX() - START_X;
		this.oy = feet.getY() - 1;
		this.oz = feet.getZ();
	}

	/** Baut die Map um die Füße des Spielers und trägt alle Map-Elemente ein. */
	public static void build(ServerLevel level, MapData map, BlockPos feet) {
		new RieseMap(level, map, feet).build();
	}

	private void build() {
		map.clearAll();

		// Platz schaffen, Boden legen.
		fill(-88, 1, -66, 42, 30, 50, Blocks.AIR);
		fill(-88, 0, -66, 42, 0, 50, OUTDOOR_FLOOR);

		// ---- Rohbau: erst die niedrigen Hofmauern, dann die Gebäude (gemeinsame Wände werden überschrieben).
		walls(-40, -12, 0, 12, 1, 5, Blocks.BRICKS);                                            // Innenhof
		walls(-80, -12, -40, 12, 1, 5, Blocks.BRICKS);                                          // Hof links
		room(-40, -40, -10, -12, 4, Blocks.BRICKS, Blocks.STONE_BRICKS, true);                  // Ofenraum
		room(-10, -40, 20, -12, 10, Blocks.BRICKS, Blocks.SMOOTH_STONE, true);                  // Werkstatt
		glassRoom(-40, -56, -10, -40, 8);                                                       // Teleporter B
		room(-40, 12, 0, 30, 6, Blocks.BRICKS, Blocks.SMOOTH_STONE, true);                      // Tierversuche
		room(0, 12, 32, 30, 6, Blocks.BRICKS, Blocks.SMOOTH_STONE, true);                       // Labor
		glassRoom(0, 30, 32, 46, 8);                                                            // Teleporter A
		glassRoom(-80, -10, -62, 10, 7);                                                        // Teleporter C
		walls(0, -12, 32, 12, 1, 6, Blocks.BRICKS);                                             // Start

		// ---- Einrichtung und Map-Elemente
		startArea();
		labDetails();
		animalTestingDetails();
		teleporterADetails();
		garageDetails();
		furnaceDetails();
		generatorDetails();
		teleporterBDetails();
		courtyardDetails();
		bridge();
		leftCourtyardDetails();
		teleporterCDetails();
		skyline();

		// ---- Schnee auf offenen Flächen und Dächern
		snow(1, -11, 31, 11, 1, OUTDOOR_FLOOR);        // Start
		snow(-39, -11, -1, 11, 1, OUTDOOR_FLOOR);      // Innenhof
		snow(-61, -11, -41, 11, 1, OUTDOOR_FLOOR);     // Hof links
		snow(-39, -22, -11, -13, 6, Blocks.BRICKS);    // Generatoren
		snow(-40, -40, -10, -23, 6, Blocks.BRICKS);    // Ofenraum-Dach
		snow(-10, -40, 20, -12, 12, Blocks.BRICKS);    // Werkstatt-Dach
		snow(-40, 12, 32, 30, 8, Blocks.BRICKS);       // Labor- und Tierversuchs-Dach

		map.setPlayerSpawn(pos(START_X, 1, 0));
	}

	// ================================================================ Start: Hof vor dem Hauptrechner

	private void startArea() {
		mainframe(25, 0);
		roundPad(12, 0, 0);

		// „Waffenfabrik der Riese“: Tafel über einem Balkon, Treppe von Osten
		fill(2, 4, -11, 13, 4, -9, Blocks.SPRUCE_PLANKS);
		fill(2, 5, -9, 13, 5, -9, Blocks.OAK_FENCE);
		stairs(17, -11, -1, 0, 3, 4, Blocks.STONE_BRICKS);
		fill(5, 7, -12, 10, 9, -12, Blocks.GILDED_BLACKSTONE);
		set(2, 5, -11, Blocks.LANTERN);
		set(11, 5, -11, Blocks.LANTERN);
		// Rot glühende Gitterfenster
		fill(12, 7, -12, 19, 8, -12, Blocks.SHROOMLIGHT);
		fill(18, 4, 12, 30, 5, 12, Blocks.SHROOMLIGHT);

		door("Labor", 6, 1, 12, 8, 3, 12, 750, "labor,hof");
		door("Werkstatt", 20, 1, -12, 22, 3, -12, 750, "garage,hof");

		wallWeapon(0, 2, -6, "mczombies:assault_rifle", 500, 250);
		wallWeapon(26, 2, 12, "mczombies:pistol", 500, 250);

		window(32, 1, -10, 32, 2, -9, 1, 0, null);
		window(32, 1, 9, 32, 2, 10, 1, 0, null);

		lampPost(2, 10);
		lampPost(30, -10);
	}

	/**
	 * Hauptrechner: Turm auf einer erhöhten Plattform mit Stufen, oben Glaskuppel und Leuchtfeuer
	 * (der Strahl in den Himmel). Unten im Turm steckt die Aufrüst-Maschine.
	 */
	private void mainframe(int cx, int cz) {
		// Plattform (Oberkante y = 2) mit Stufen aus Halbstufen von Westen
		fill(cx - 5, 1, cz - 6, cx + 5, 2, cz + 6, Blocks.STONE_BRICKS);
		fill(cx - 5, 2, cz - 6, cx + 5, 2, cz + 6, Blocks.SMOOTH_STONE);
		fill(cx - 8, 1, cz - 2, cx - 8, 1, cz + 2, Blocks.SMOOTH_STONE_SLAB);
		fill(cx - 7, 1, cz - 2, cx - 6, 1, cz + 2, Blocks.STONE_BRICKS);
		fill(cx - 6, 2, cz - 2, cx - 6, 2, cz + 2, Blocks.SMOOTH_STONE_SLAB);
		// Geländer
		fill(cx - 5, 3, cz - 6, cx + 5, 3, cz - 6, Blocks.IRON_BARS);
		fill(cx - 5, 3, cz + 6, cx + 5, 3, cz + 6, Blocks.IRON_BARS);
		fill(cx + 5, 3, cz - 6, cx + 5, 3, cz + 6, Blocks.IRON_BARS);
		fill(cx - 5, 3, cz - 6, cx - 5, 3, cz - 3, Blocks.IRON_BARS);
		fill(cx - 5, 3, cz + 3, cx - 5, 3, cz + 6, Blocks.IRON_BARS);

		// Gerippter Turm, innen eine Leuchtsäule
		for (int y = 3; y <= 14; y++) {
			Block ring = y % 3 == 0 ? Blocks.IRON_BLOCK : Blocks.POLISHED_DEEPSLATE;
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
		// Kuppel, Leuchtfeuer und Kugeln an Auslegern
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				if (dx * dx + dz * dz <= 10) {
					set(cx + dx, 15, cz + dz, Blocks.GLASS);
				}
			}
		}
		fill(cx - 1, 16, cz - 1, cx + 1, 16, cz + 1, Blocks.IRON_BLOCK);
		set(cx, 17, cz, Blocks.BEACON);
		for (int[] c : new int[][] {{-4, 0}, {4, 0}, {0, -4}, {0, 4}}) {
			set(cx + c[0], 13, cz + c[1], Blocks.END_ROD);
		}

		// Aufrüst-Maschine im runden Fenster unten am Turm, darüber ein Glasfenster
		set(cx - 3, 3, cz, Blocks.ANVIL);
		set(cx - 3, 4, cz, Blocks.GLASS);
		map.addUpgradeMachine(pos(cx - 3, 3, cz));
	}

	// ================================================================ Labor (Süden)

	private void labDetails() {
		// Tafel, Tisch mit Präparategläsern, Pinnwand
		fill(4, 2, 29, 9, 3, 29, Blocks.POLISHED_BLACKSTONE);
		fill(4, 1, 25, 9, 1, 26, Blocks.SMOOTH_STONE_SLAB);
		set(5, 2, 25, Blocks.GLASS);
		set(8, 2, 25, Blocks.GLASS);
		fill(20, 2, 29, 22, 3, 29, Blocks.SPRUCE_PLANKS);
		// Offener Durchgang zu den Tierversuchen
		fill(0, 1, 16, 0, 3, 24, Blocks.AIR);

		wallWeapon(32, 2, 18, "mczombies:smg", 1250, 625);
		wallWeapon(24, 2, 30, "mczombies:pistol", 750, 375);
		box(28, 1, 26);
		window(32, 1, 24, 32, 2, 25, 1, 0, "labor");
		door("TeleporterA", 14, 1, 30, 16, 3, 30, 1250, "teleporter_a");
		lights(2, 14, 30, 28, 6);
	}

	// ================================================================ Tierversuche (unten Mitte)

	private void animalTestingDetails() {
		// Käfigzellen an der Südwand
		for (int x = -38; x <= -20; x += 6) {
			fill(x, 1, 26, x + 3, 3, 26, Blocks.IRON_BARS);
			fill(x, 1, 27, x, 3, 29, Blocks.IRON_BARS);
			fill(x + 3, 1, 27, x + 3, 3, 29, Blocks.IRON_BARS);
			set(x + 1, 1, 28, Blocks.HAY_BLOCK);
		}
		// Waschbecken, Fässer, Maschendraht-Absperrung
		set(-4, 1, 13, Blocks.CAULDRON);
		set(-12, 1, 13, Blocks.BARREL);
		set(-11, 1, 13, Blocks.BARREL);
		fill(-14, 1, 18, -14, 3, 22, Blocks.IRON_BARS);
		// Durchgang in den Innenhof
		fill(-20, 1, 12, -16, 3, 12, Blocks.AIR);

		window(-40, 1, 20, -40, 2, 21, -1, 0, "labor");
		window(-8, 1, 30, -7, 2, 30, 0, 1, "labor");
		map.addZombieSpawn(pos(-30, 1, 18), "labor");
		lights(-38, 14, -2, 28, 6);
	}

	// ================================================================ Teleporter A (hinter dem Labor)

	private void teleporterADetails() {
		teleporterRoom(16, 40, 6, 26);
		wallWeapon(32, 2, 38, "mczombies:assault_rifle", 1500, 750);
		box(28, 1, 44);
		window(0, 1, 41, 0, 2, 42, -1, 0, "teleporter_a");
		map.addZombieSpawn(pos(16, 1, 34), "teleporter_a");
		lights(2, 32, 30, 44, 8);
	}

	// ================================================================ Werkstatt / Hangar (Norden)

	private void garageDetails() {
		car(0, -36);
		car(8, -24);
		// Obergeschoss an der Westseite mit Treppe, von dort zu den Generatoren
		fill(-9, 5, -25, -2, 5, -13, Blocks.SPRUCE_PLANKS);
		fill(-1, 6, -25, -1, 6, -13, Blocks.OAK_FENCE);
		fill(-6, 6, -26, -2, 6, -26, Blocks.OAK_FENCE);
		stairs(-9, -30, 0, 1, 3, 5, Blocks.STONE_BRICKS);
		fill(-10, 6, -20, -10, 8, -16, Blocks.AIR);
		// Durchgänge in den Innenhof und in den Ofenraum
		fill(-8, 1, -12, -4, 3, -12, Blocks.AIR);
		fill(-10, 1, -38, -10, 3, -35, Blocks.AIR);
		// Rot glühende Fenster an der Außenseite
		fill(20, 6, -30, 20, 8, -20, Blocks.SHROOMLIGHT);

		wallWeapon(20, 2, -28, "mczombies:shotgun", 750, 375);
		wallWeapon(20, 2, -18, "mczombies:smg", 1300, 650);
		box(16, 1, -36);
		window(20, 1, -34, 20, 2, -33, 1, 0, "garage");
		lights(-8, -38, 18, -14, 10);
	}

	private void car(int x, int z) {
		fill(x, 1, z, x + 4, 1, z + 2, Blocks.POLISHED_BLACKSTONE);
		fill(x + 1, 2, z, x + 3, 2, z + 2, Blocks.GLASS);
		set(x, 0, z, Blocks.COAL_BLOCK); // Ölflecken
		set(x + 4, 0, z + 2, Blocks.COAL_BLOCK);
	}

	// ================================================================ Ofenraum (1. Stock, unter Teleporter B)

	private void furnaceDetails() {
		for (int x = -38; x <= -12; x += 2) {
			set(x, 1, -39, Blocks.BLAST_FURNACE);
		}
		// Gotische Bögen: Säulenreihe quer durch den Raum
		for (int x = -38; x <= -14; x += 4) {
			fill(x, 1, -24, x, 4, -24, Blocks.STONE_BRICKS);
		}
		// Glutrotes Licht aus der Decke
		for (int x = -37; x <= -13; x += 6) {
			for (int z = -38; z <= -14; z += 6) {
				set(x, 5, z, Blocks.SHROOMLIGHT);
			}
		}
		door("TeleporterB", -26, 1, -40, -24, 3, -40, 1250, "teleporter_b");
		window(-40, 1, -34, -40, 2, -33, -1, 0, "garage");
		map.addZombieSpawn(pos(-30, 1, -30), "garage");
	}

	// ================================================================ Generatoren (2. Stock, auf dem Ofenraum)

	private void generatorDetails() {
		// Offenes Stahldach
		for (int x = -39; x <= -11; x += 14) {
			fill(x, 6, -22, x, 9, -22, Blocks.IRON_BARS);
			fill(x, 6, -13, x, 9, -13, Blocks.IRON_BARS);
		}
		for (int z = -22; z <= -13; z += 3) {
			fill(-39, 10, z, -11, 10, z, Blocks.IRON_BARS);
		}
		// Großer Motor und Stromkasten (Strom kommt in Phase 4)
		fill(-32, 6, -19, -28, 8, -16, Blocks.IRON_BLOCK);
		set(-32, 8, -19, Blocks.AIR);
		set(-32, 8, -16, Blocks.AIR);
		fill(-14, 6, -21, -14, 7, -21, Blocks.IRON_BLOCK);
		set(-14, 8, -21, Blocks.REDSTONE_LAMP);
		// Geländer (Lücke Richtung Brücke)
		fill(-40, 6, -23, -11, 6, -23, Blocks.IRON_BARS);
		fill(-40, 6, -22, -40, 6, -13, Blocks.IRON_BARS);
		fill(-40, 6, -12, -25, 6, -12, Blocks.IRON_BARS);
		fill(-21, 6, -12, -11, 6, -12, Blocks.IRON_BARS);
	}

	// ================================================================ Teleporter B (ganz im Norden)

	private void teleporterBDetails() {
		teleporterRoom(-25, -48, -34, -16);
		wallWeapon(-10, 2, -48, "mczombies:lmg", 1500, 750);
		box(-14, 1, -53);
		window(-26, 1, -56, -25, 2, -56, 0, -1, "teleporter_b");
		map.addZombieSpawn(pos(-34, 1, -44), "teleporter_b");
		lights(-38, -54, -12, -42, 8);
	}

	// ================================================================ Innenhof (Mitte, unter der Brücke)

	private void courtyardDetails() {
		wallWeapon(-34, 2, -12, "mczombies:smg", 1250, 625);
		box(-36, 1, 8);
		set(-38, 1, -10, Blocks.CAMPFIRE);   // Feuertonne
		set(-2, 1, 10, Blocks.CAMPFIRE);
		powerPole(-12, 0);
		lampPost(-30, 10);
		map.addZombieSpawn(pos(-30, 1, -2), "hof");
		map.addZombieSpawn(pos(-8, 1, 4), "hof");
	}

	// ================================================================ Brücke (von den Generatoren über den Innenhof)

	private void bridge() {
		fill(-24, 5, -11, -22, 5, 4, Blocks.IRON_BLOCK);
		fill(-25, 6, -11, -25, 6, 4, Blocks.IRON_BARS);
		fill(-21, 6, -11, -21, 6, 4, Blocks.IRON_BARS);
		stairs(-24, 9, 0, -1, 3, 5, Blocks.STONE_BRICKS);
		// Elektro-Spitzen unter der Brücke (die Falle kommt in Phase 4)
		for (int z = -9; z <= 3; z += 4) {
			set(-23, 4, z, Blocks.END_ROD);
		}
		// Im Original öffnet der Strom die Brücke; bis Phase 4 ist sie eine kaufbare Sperre.
		door("Bruecke", -24, 6, -12, -22, 7, -12, 1000, null);
	}

	// ================================================================ Hof links und Teleporter C (Westen)

	private void leftCourtyardDetails() {
		door("TeleporterC", -40, 1, -1, -40, 3, 1, 1250, "teleporter_c");
		wallWeapon(-45, 2, -12, "mczombies:assault_rifle", 1400, 700);
		box(-50, 1, -8);
		set(-44, 1, 10, Blocks.CAMPFIRE);
		powerPole(-50, 4);
		// Schneehaufen in der Ecke
		fill(-60, 1, 8, -57, 1, 11, Blocks.SNOW_BLOCK);
		fill(-60, 2, 10, -58, 2, 11, Blocks.SNOW_BLOCK);
		// Stacheldraht auf den Mauern
		for (int x = -61; x <= -41; x += 2) {
			set(x, 6, -12, Blocks.COBWEB);
			set(x, 6, 12, Blocks.COBWEB);
		}
		window(-52, 1, -12, -51, 2, -12, 0, -1, "teleporter_c");
		map.addZombieSpawn(pos(-48, 1, 6), "teleporter_c");
	}

	private void teleporterCDetails() {
		fill(-62, 1, -2, -62, 3, 2, Blocks.AIR);
		teleporterRoom(-71, 0, -66, -76);
		window(-80, 1, 5, -80, 2, 6, -1, 0, "teleporter_c");
		map.addZombieSpawn(pos(-72, 1, -7), "teleporter_c");
		lights(-78, -8, -64, 8, 7);
	}

	// ================================================================ Kulisse: Schornsteine und Kran

	private void skyline() {
		for (int[] c : new int[][] {{-46, -63}, {-22, -63}, {-2, -63}, {36, -60}, {38, -42}}) {
			fill(c[0], 1, c[1], c[0] + 1, 26, c[1] + 1, Blocks.BRICKS);
			fill(c[0], 27, c[1], c[0] + 1, 27, c[1] + 1, Blocks.CAMPFIRE);
		}
		// Kran mit dem hängenden Kopf des Riesen
		fill(30, 1, -60, 30, 24, -60, Blocks.POLISHED_BLACKSTONE);
		fill(12, 24, -60, 29, 24, -60, Blocks.POLISHED_BLACKSTONE);
		fill(16, 17, -60, 16, 23, -60, Blocks.IRON_BARS);
		fill(15, 13, -61, 17, 16, -59, Blocks.POLISHED_BLACKSTONE);
		set(15, 15, -59, Blocks.SHROOMLIGHT);
		set(17, 15, -59, Blocks.SHROOMLIGHT);
	}

	// ================================================================ Bausteine

	/** Raum mit Boden (y = 0), Wänden und optional Decke. Wände teilt er sich mit Nachbarn. */
	private void room(int x1, int z1, int x2, int z2, int wallTop, Block wall, Block floor, boolean roof) {
		fill(x1, 0, z1, x2, 0, z2, floor);
		walls(x1, z1, x2, z2, 1, wallTop, wall);
		if (roof) {
			fill(x1, wallTop + 1, z1, x2, wallTop + 1, z2, wall);
		}
	}

	/** Teleporterraum: Wände aus Fliesen und ein Glasdach. */
	private void glassRoom(int x1, int z1, int x2, int z2, int wallTop) {
		room(x1, z1, x2, z2, wallTop, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, false);
		fill(x1 + 1, wallTop + 1, z1 + 1, x2 - 1, wallTop + 1, z2 - 1, Blocks.GLASS);
	}

	/** Einrichtung eines Teleporterraums: runde Plattform in der Mitte, zwei glühende Gruben. */
	private void teleporterRoom(int padX, int padZ, int pitX1, int pitX2) {
		roundPad(padX, 0, padZ);
		for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
			fill(padX + c[0], 1, padZ + c[1], padX + c[0], 3, padZ + c[1], Blocks.IRON_BARS);
			set(padX + c[0], 4, padZ + c[1], Blocks.END_ROD);
		}
		glowPit(pitX1, padZ);
		glowPit(pitX2, padZ);
	}

	/** Runde Teleporter-Plattform: Eisenkern mit Leuchtring. */
	private void roundPad(int x, int floorY, int z) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 <= 5) {
					set(x + dx, floorY, z + dz, d2 <= 1 ? Blocks.IRON_BLOCK : Blocks.SEA_LANTERN);
				}
			}
		}
	}

	/** Orange glühende Grube im Boden mit Geländer. */
	private void glowPit(int x, int z) {
		fill(x - 1, 0, z - 1, x + 1, 0, z + 1, Blocks.SHROOMLIGHT);
		walls(x - 2, z - 2, x + 2, z + 2, 1, 1, Blocks.IRON_BARS);
	}

	private void walls(int x1, int z1, int x2, int z2, int yFrom, int yTo, Block wall) {
		fill(x1, yFrom, z1, x2, yTo, z1, wall);
		fill(x1, yFrom, z2, x2, yTo, z2, wall);
		fill(x1, yFrom, z1, x1, yTo, z2, wall);
		fill(x2, yFrom, z1, x2, yTo, z2, wall);
	}

	/**
	 * Treppe aus vollen Blöcken, die in Richtung (dx, dz) um je einen Block ansteigt.
	 *
	 * @param width Breite quer zur Laufrichtung
	 */
	private void stairs(int x, int z, int dx, int dz, int width, int height, Block block) {
		for (int h = 1; h <= height; h++) {
			int bx = x + dx * (h - 1);
			int bz = z + dz * (h - 1);
			if (dx != 0) {
				fill(bx, 1, bz, bx, h, bz + width - 1, block);
			} else {
				fill(bx, 1, bz, bx + width - 1, h, bz, block);
			}
		}
	}

	/** Strommast mit Querbalken. */
	private void powerPole(int x, int z) {
		fill(x, 1, z, x, 9, z, Blocks.SPRUCE_FENCE);
		fill(x - 1, 9, z, x + 1, 9, z, Blocks.SPRUCE_PLANKS);
	}

	/** Laterne auf einem Pfosten. */
	private void lampPost(int x, int z) {
		fill(x, 1, z, x, 3, z, Blocks.SPRUCE_FENCE);
		set(x, 4, z, Blocks.LANTERN);
	}

	/** Lampen in der Decke (y = wallTop + 1). */
	private void lights(int x1, int z1, int x2, int z2, int wallTop) {
		for (int x = x1; x <= x2; x += 6) {
			for (int z = z1; z <= z2; z += 6) {
				set(x, wallTop + 1, z, Blocks.SEA_LANTERN);
			}
		}
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
