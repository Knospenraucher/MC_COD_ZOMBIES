package de.knospenraucher.mczombies.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.game.GameManager;
import de.knospenraucher.mczombies.map.BlockSnapshots;
import de.knospenraucher.mczombies.map.MapData;
import de.knospenraucher.mczombies.map.prefab.KolossMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Der Befehl {@code /zombies} (Operator-Rechte, Level 2).
 *
 * <pre>
 * /zombies start | reset | status | reload
 * /zombies spawn add [pos] [zone] | remove &lt;nr&gt; | list | clear | show
 * /zombies playerspawn set [pos]
 * /zombies points &lt;spieler&gt; &lt;anzahl&gt;
 * /zombies door add &lt;name&gt; &lt;von&gt; &lt;bis&gt; &lt;preis&gt; [zone] | remove &lt;name&gt; | list
 * /zombies window add &lt;von&gt; &lt;bis&gt; | remove &lt;nr&gt; | list
 * /zombies wallweapon add &lt;item&gt; &lt;preis&gt; [munitionspreis] | remove &lt;nr&gt; | list
 * /zombies box add [pos] | remove &lt;nr&gt; | list
 * /zombies upgrade add [pos] | remove &lt;nr&gt; | list
 * /zombies buildmap koloss
 * </pre>
 */
public final class ZombiesCommand {
	private ZombiesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
		dispatcher.register(Commands.literal("zombies")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("start").executes(ZombiesCommand::start))
				.then(Commands.literal("reset").executes(ZombiesCommand::reset))
				.then(Commands.literal("status").executes(ZombiesCommand::status))
				.then(Commands.literal("reload").executes(ZombiesCommand::reload))
				.then(Commands.literal("spawn")
						.then(Commands.literal("add")
								.executes(ctx -> addSpawn(ctx, BlockPos.containing(ctx.getSource().getPosition()), MapData.START_ZONE))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> addSpawn(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos"), MapData.START_ZONE))
										.then(Commands.argument("zone", StringArgumentType.word())
												.executes(ctx -> addSpawn(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
														StringArgumentType.getString(ctx, "zone"))))))
						.then(Commands.literal("remove")
								.then(Commands.argument("nr", IntegerArgumentType.integer(1))
										.executes(ZombiesCommand::removeSpawn)))
						.then(Commands.literal("list").executes(ZombiesCommand::listSpawns))
						.then(Commands.literal("clear").executes(ZombiesCommand::clearSpawns))
						.then(Commands.literal("show").executes(ZombiesCommand::showSpawns)))
				.then(Commands.literal("playerspawn")
						.then(Commands.literal("set")
								.executes(ctx -> setPlayerSpawn(ctx, BlockPos.containing(ctx.getSource().getPosition())))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> setPlayerSpawn(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos"))))))
				.then(Commands.literal("points")
						.then(Commands.argument("spieler", EntityArgument.player())
								.then(Commands.argument("anzahl", IntegerArgumentType.integer(0))
										.executes(ZombiesCommand::setPoints))))
				.then(Commands.literal("door")
						.then(Commands.literal("add")
								.then(Commands.argument("name", StringArgumentType.word())
										.then(Commands.argument("von", BlockPosArgument.blockPos())
												.then(Commands.argument("bis", BlockPosArgument.blockPos())
														.then(Commands.argument("preis", IntegerArgumentType.integer(0))
																.executes(ctx -> addDoor(ctx, null))
																.then(Commands.argument("zone", StringArgumentType.word())
																		.executes(ctx -> addDoor(ctx, StringArgumentType.getString(ctx, "zone")))))))))
						.then(Commands.literal("remove")
								.then(Commands.argument("name", StringArgumentType.word()).executes(ZombiesCommand::removeDoor)))
						.then(Commands.literal("list").executes(ZombiesCommand::listDoors)))
				.then(Commands.literal("window")
						.then(Commands.literal("add")
								.then(Commands.argument("von", BlockPosArgument.blockPos())
										.then(Commands.argument("bis", BlockPosArgument.blockPos())
												.executes(ZombiesCommand::addWindow))))
						.then(Commands.literal("remove")
								.then(Commands.argument("nr", IntegerArgumentType.integer(1)).executes(ZombiesCommand::removeWindow)))
						.then(Commands.literal("list").executes(ZombiesCommand::listWindows)))
				.then(Commands.literal("wallweapon")
						.then(Commands.literal("add")
								.then(Commands.argument("item", ItemArgument.item(buildContext))
										.then(Commands.argument("preis", IntegerArgumentType.integer(0))
												.executes(ctx -> addWallWeapon(ctx, -1))
												.then(Commands.argument("munitionspreis", IntegerArgumentType.integer(0))
														.executes(ctx -> addWallWeapon(ctx, IntegerArgumentType.getInteger(ctx, "munitionspreis")))))))
						.then(Commands.literal("remove")
								.then(Commands.argument("nr", IntegerArgumentType.integer(1)).executes(ZombiesCommand::removeWallWeapon)))
						.then(Commands.literal("list").executes(ZombiesCommand::listWallWeapons)))
				.then(Commands.literal("box")
						.then(Commands.literal("add")
								.executes(ctx -> addBox(ctx, lookedAtBlock(ctx)))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> addBox(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
						.then(Commands.literal("remove")
								.then(Commands.argument("nr", IntegerArgumentType.integer(1)).executes(ZombiesCommand::removeBox)))
						.then(Commands.literal("list").executes(ZombiesCommand::listBoxes)))
				.then(Commands.literal("upgrade")
						.then(Commands.literal("add")
								.executes(ctx -> addUpgrade(ctx, lookedAtBlock(ctx)))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> addUpgrade(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
						.then(Commands.literal("remove")
								.then(Commands.argument("nr", IntegerArgumentType.integer(1)).executes(ZombiesCommand::removeUpgrade)))
						.then(Commands.literal("list").executes(ZombiesCommand::listUpgrades)))
				.then(Commands.literal("buildmap")
						.then(Commands.literal("koloss").executes(ZombiesCommand::buildKoloss))));
	}

	// ---------------------------------------------------------------- Spielsteuerung

	private static int start(CommandContext<CommandSourceStack> ctx) {
		GameManager game = game(ctx);
		if (game == null) {
			return 0;
		}
		String error = game.start(ctx.getSource().getLevel());
		if (error != null) {
			ctx.getSource().sendFailure(Component.literal(error));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Spiel gestartet."), true);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) {
		GameManager game = game(ctx);
		if (game == null) {
			return 0;
		}
		game.reset();
		ctx.getSource().sendSuccess(() -> Component.literal("Spiel zurückgesetzt."), true);
		return 1;
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		GameManager game = game(ctx);
		if (game == null) {
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal(game.statusText()), false);
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		ZombiesConfig.load();
		ctx.getSource().sendSuccess(() -> Component.literal("Config neu geladen (gilt ab dem nächsten Spawn)."), true);
		return 1;
	}

	private static int setPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		GameManager game = game(ctx);
		if (game == null) {
			return 0;
		}
		ServerPlayer player = EntityArgument.getPlayer(ctx, "spieler");
		int amount = IntegerArgumentType.getInteger(ctx, "anzahl");
		if (!game.setPoints(player, amount)) {
			ctx.getSource().sendFailure(Component.literal(player.getName().getString() + " nimmt nicht am Spiel teil."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal(
				player.getName().getString() + " hat jetzt " + amount + " Punkte."), true);
		return 1;
	}

	// ---------------------------------------------------------------- Map-Setup

	private static int addSpawn(CommandContext<CommandSourceStack> ctx, BlockPos pos, String zone) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		if (!map.addZombieSpawn(pos, zone)) {
			ctx.getSource().sendFailure(Component.literal("Dort gibt es schon einen Spawnpunkt."));
			return 0;
		}
		int nr = map.getZombieSpawns().size();
		ctx.getSource().sendSuccess(() -> Component.literal("Zombie-Spawnpunkt #" + nr + " gesetzt: " + format(pos)
				+ " (Zone " + zone + ")"), true);
		return 1;
	}

	private static int removeSpawn(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		int nr = IntegerArgumentType.getInteger(ctx, "nr");
		MapData.SpawnPoint removed = map.removeZombieSpawn(nr);
		if (removed == null) {
			ctx.getSource().sendFailure(Component.literal("Spawnpunkt #" + nr + " existiert nicht."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Spawnpunkt #" + nr + " (" + removed + ") entfernt."), true);
		return 1;
	}

	private static int listSpawns(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		List<MapData.SpawnPoint> spawns = map.getZombieSpawns();
		BlockPos playerSpawn = map.getPlayerSpawn();
		ctx.getSource().sendSuccess(() -> Component.literal("Spieler-Startpunkt: "
				+ (playerSpawn == null ? "nicht gesetzt" : format(playerSpawn))).withStyle(ChatFormatting.GREEN), false);
		ctx.getSource().sendSuccess(() -> Component.literal(spawns.size() + " Zombie-Spawnpunkte:").withStyle(ChatFormatting.GOLD), false);
		for (int i = 0; i < spawns.size(); i++) {
			String line = " #" + (i + 1) + ": " + spawns.get(i) + " (Zone " + spawns.get(i).zone() + ")";
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return spawns.size();
	}

	private static int clearSpawns(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		map.clearZombieSpawns();
		ctx.getSource().sendSuccess(() -> Component.literal("Alle Zombie-Spawnpunkte gelöscht."), true);
		return 1;
	}

	private static int showSpawns(CommandContext<CommandSourceStack> ctx) {
		GameManager game = game(ctx);
		if (game == null) {
			return 0;
		}
		game.showSpawns(15);
		ctx.getSource().sendSuccess(() -> Component.literal(
				"Spawnpunkte werden 15 Sekunden lang angezeigt (blau = Zombies, grün = Spieler)."), false);
		return 1;
	}

	private static int setPlayerSpawn(CommandContext<CommandSourceStack> ctx, BlockPos pos) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		map.setPlayerSpawn(pos);
		ctx.getSource().sendSuccess(() -> Component.literal("Spieler-Startpunkt gesetzt: " + format(pos)), true);
		return 1;
	}

	// ---------------------------------------------------------------- Türen

	private static int addDoor(CommandContext<CommandSourceStack> ctx, String zone) throws CommandSyntaxException {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		String name = StringArgumentType.getString(ctx, "name");
		BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "von");
		BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "bis");
		int price = IntegerArgumentType.getInteger(ctx, "preis");
		if (map.getDoor(name) != null) {
			ctx.getSource().sendFailure(Component.literal("Es gibt schon eine Tür namens „" + name + "“."));
			return 0;
		}
		if (BlockSnapshots.volume(from, to) > BlockSnapshots.MAX_BLOCKS) {
			ctx.getSource().sendFailure(Component.literal("Bereich zu groß (max. " + BlockSnapshots.MAX_BLOCKS + " Blöcke)."));
			return 0;
		}
		MapData.Door door = new MapData.Door();
		door.name = name;
		door.price = price;
		door.zone = zone;
		BlockSnapshots.setCorners(door, from, to);
		door.blocks = BlockSnapshots.capture(ctx.getSource().getLevel(), door);
		if (door.blocks.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Im Bereich sind keine Blöcke. Baue die Tür zuerst."));
			return 0;
		}
		map.addDoor(door);
		ctx.getSource().sendSuccess(() -> Component.literal("Tür „" + name + "“ angelegt: " + door.blocks.size()
				+ " Blöcke, " + price + " Punkte" + (zone != null ? ", schaltet Zone " + zone + " frei" : "") + "."), true);
		return 1;
	}

	private static int removeDoor(CommandContext<CommandSourceStack> ctx) {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		String name = StringArgumentType.getString(ctx, "name");
		if (!map.removeDoor(name)) {
			ctx.getSource().sendFailure(Component.literal("Keine Tür namens „" + name + "“."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Tür „" + name + "“ entfernt (die Blöcke bleiben stehen)."), true);
		return 1;
	}

	private static int listDoors(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		List<MapData.Door> doors = map.getDoors();
		ctx.getSource().sendSuccess(() -> Component.literal(doors.size() + " Türen:").withStyle(ChatFormatting.GOLD), false);
		for (MapData.Door door : doors) {
			String line = " " + door.name + ": " + door.price + " Punkte, " + door.describe()
					+ (door.zone != null ? ", Zone " + door.zone : "");
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return doors.size();
	}

	// ---------------------------------------------------------------- Fenster

	private static int addWindow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, "von");
		BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, "bis");
		if (BlockSnapshots.volume(from, to) > 64) {
			ctx.getSource().sendFailure(Component.literal("Ein Fenster darf höchstens 64 Blöcke groß sein."));
			return 0;
		}
		MapData.Window window = new MapData.Window();
		BlockSnapshots.setCorners(window, from, to);
		window.boards = BlockSnapshots.capture(ctx.getSource().getLevel(), window);
		if (window.boards.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Im Bereich sind keine Blöcke. Setze zuerst die Bretter."));
			return 0;
		}
		map.addWindow(window);
		int nr = map.getWindows().size();
		ctx.getSource().sendSuccess(() -> Component.literal("Fenster #" + nr + " mit " + window.boards.size() + " Brettern angelegt."), true);
		return 1;
	}

	private static int removeWindow(CommandContext<CommandSourceStack> ctx) {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		int nr = IntegerArgumentType.getInteger(ctx, "nr");
		if (map.removeWindow(nr) == null) {
			ctx.getSource().sendFailure(Component.literal("Fenster #" + nr + " existiert nicht."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Fenster #" + nr + " entfernt."), true);
		return 1;
	}

	private static int listWindows(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		List<MapData.Window> windows = map.getWindows();
		ctx.getSource().sendSuccess(() -> Component.literal(windows.size() + " Fenster:").withStyle(ChatFormatting.GOLD), false);
		for (int i = 0; i < windows.size(); i++) {
			String line = " #" + (i + 1) + ": " + windows.get(i).describe() + ", " + windows.get(i).boards.size() + " Bretter";
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return windows.size();
	}

	// ---------------------------------------------------------------- Wandwaffen

	private static int addWallWeapon(CommandContext<CommandSourceStack> ctx, int ammoPrice) throws CommandSyntaxException {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		BlockPos pos = lookedAtBlock(ctx);
		Item item = ItemArgument.getItem(ctx, "item").createItemStack(1).getItem();
		int price = IntegerArgumentType.getInteger(ctx, "preis");
		MapData.WallWeapon weapon = new MapData.WallWeapon();
		weapon.pos = new MapData.Pos(pos);
		weapon.item = BuiltInRegistries.ITEM.getKey(item).toString();
		weapon.price = price;
		weapon.ammoPrice = ammoPrice >= 0 ? ammoPrice : price / 2;
		map.addWallWeapon(weapon);
		ctx.getSource().sendSuccess(() -> Component.literal("Wandwaffe " + weapon.item + " an " + format(pos) + ": "
				+ weapon.price + " Punkte, Munition " + weapon.ammoPrice + " Punkte."), true);
		return 1;
	}

	private static int removeWallWeapon(CommandContext<CommandSourceStack> ctx) {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		int nr = IntegerArgumentType.getInteger(ctx, "nr");
		if (map.removeWallWeapon(nr) == null) {
			ctx.getSource().sendFailure(Component.literal("Wandwaffe #" + nr + " existiert nicht."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Wandwaffe #" + nr + " entfernt."), true);
		return 1;
	}

	private static int listWallWeapons(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		List<MapData.WallWeapon> weapons = map.getWallWeapons();
		ctx.getSource().sendSuccess(() -> Component.literal(weapons.size() + " Wandwaffen:").withStyle(ChatFormatting.GOLD), false);
		for (int i = 0; i < weapons.size(); i++) {
			MapData.WallWeapon w = weapons.get(i);
			String line = " #" + (i + 1) + ": " + w.item + " an " + w.pos + ", " + w.price + " / Munition " + w.ammoPrice;
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return weapons.size();
	}

	// ---------------------------------------------------------------- Zufallskiste

	private static int addBox(CommandContext<CommandSourceStack> ctx, BlockPos pos) {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		if (!map.addBoxLocation(pos)) {
			ctx.getSource().sendFailure(Component.literal("Dort steht schon ein Kistenstandort."));
			return 0;
		}
		int nr = map.getBoxLocations().size();
		ctx.getSource().sendSuccess(() -> Component.literal("Kistenstandort #" + nr + " gesetzt: " + format(pos)), true);
		return 1;
	}

	private static int removeBox(CommandContext<CommandSourceStack> ctx) {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		int nr = IntegerArgumentType.getInteger(ctx, "nr");
		if (map.removeBoxLocation(nr) == null) {
			ctx.getSource().sendFailure(Component.literal("Kistenstandort #" + nr + " existiert nicht."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Kistenstandort #" + nr + " entfernt."), true);
		return 1;
	}

	private static int listBoxes(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		List<BlockPos> boxes = map.getBoxLocations();
		ctx.getSource().sendSuccess(() -> Component.literal(boxes.size() + " Kistenstandorte:").withStyle(ChatFormatting.GOLD), false);
		for (int i = 0; i < boxes.size(); i++) {
			String line = " #" + (i + 1) + ": " + format(boxes.get(i));
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return boxes.size();
	}

	// ---------------------------------------------------------------- Aufrüst-Maschine

	private static int addUpgrade(CommandContext<CommandSourceStack> ctx, BlockPos pos) {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		if (!map.addUpgradeMachine(pos)) {
			ctx.getSource().sendFailure(Component.literal("Dort steht schon eine Aufrüst-Maschine."));
			return 0;
		}
		int nr = map.getUpgradeMachines().size();
		ctx.getSource().sendSuccess(() -> Component.literal("Aufrüst-Maschine #" + nr + " gesetzt: " + format(pos)), true);
		return 1;
	}

	private static int removeUpgrade(CommandContext<CommandSourceStack> ctx) {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		int nr = IntegerArgumentType.getInteger(ctx, "nr");
		if (map.removeUpgradeMachine(nr) == null) {
			ctx.getSource().sendFailure(Component.literal("Aufrüst-Maschine #" + nr + " existiert nicht."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Aufrüst-Maschine #" + nr + " entfernt."), true);
		return 1;
	}

	private static int listUpgrades(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		List<BlockPos> machines = map.getUpgradeMachines();
		ctx.getSource().sendSuccess(() -> Component.literal(machines.size() + " Aufrüst-Maschinen:").withStyle(ChatFormatting.GOLD), false);
		for (int i = 0; i < machines.size(); i++) {
			String line = " #" + (i + 1) + ": " + format(machines.get(i));
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return machines.size();
	}

	// ---------------------------------------------------------------- Vorgefertigte Maps

	private static int buildKoloss(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		MapData map = editableMap(ctx);
		if (map == null) {
			return 0;
		}
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		KolossMap.build(ctx.getSource().getLevel(), map, player.blockPosition());
		ctx.getSource().sendSuccess(() -> Component.literal("Map „Koloss-Fabrik“ gebaut. Die alten Map-Einstellungen liegen in "
				+ "mczombies_map.json.bak. Start mit /zombies start.").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	// ---------------------------------------------------------------- Hilfen

	/** Der Block, auf den der ausführende Spieler schaut (max. 6 Blöcke entfernt). */
	private static BlockPos lookedAtBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		HitResult hit = player.pick(6.0, 1.0F, false);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			return blockHit.getBlockPos();
		}
		throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(
				Component.literal("Schau dabei auf einen Block (max. 6 Blöcke entfernt).")).create();
	}

	/** Map-Daten, aber nur wenn gerade kein Spiel läuft (sonst würden offene Türen gespeichert). */
	private static MapData editableMap(CommandContext<CommandSourceStack> ctx) {
		GameManager game = game(ctx);
		if (game == null) {
			return null;
		}
		if (game.getState() != de.knospenraucher.mczombies.game.GameState.IDLE) {
			ctx.getSource().sendFailure(Component.literal("Während eines Spiels kann die Map nicht bearbeitet werden. Erst /zombies reset."));
			return null;
		}
		return game.getMap();
	}

	private static GameManager game(CommandContext<CommandSourceStack> ctx) {
		GameManager game = GameManager.get();
		if (game == null) {
			ctx.getSource().sendFailure(Component.literal("Spiel ist noch nicht bereit."));
		}
		return game;
	}

	private static MapData map(CommandContext<CommandSourceStack> ctx) {
		GameManager game = game(ctx);
		return game == null ? null : game.getMap();
	}

	private static String format(BlockPos pos) {
		return pos.getX() + " " + pos.getY() + " " + pos.getZ();
	}
}
