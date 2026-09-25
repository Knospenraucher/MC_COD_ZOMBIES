package de.knospenraucher.mczombies.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.game.GameManager;
import de.knospenraucher.mczombies.map.MapData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Der Befehl {@code /zombies} (Operator-Rechte, Level 2).
 *
 * <pre>
 * /zombies start | reset | status | reload
 * /zombies spawn add [pos] | remove &lt;nr&gt; | list | clear | show
 * /zombies playerspawn set [pos]
 * /zombies points &lt;spieler&gt; &lt;anzahl&gt;
 * </pre>
 */
public final class ZombiesCommand {
	private ZombiesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("zombies")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("start").executes(ZombiesCommand::start))
				.then(Commands.literal("reset").executes(ZombiesCommand::reset))
				.then(Commands.literal("status").executes(ZombiesCommand::status))
				.then(Commands.literal("reload").executes(ZombiesCommand::reload))
				.then(Commands.literal("spawn")
						.then(Commands.literal("add")
								.executes(ctx -> addSpawn(ctx, BlockPos.containing(ctx.getSource().getPosition())))
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> addSpawn(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "pos")))))
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
										.executes(ZombiesCommand::setPoints)))));
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

	private static int addSpawn(CommandContext<CommandSourceStack> ctx, BlockPos pos) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		if (!map.addZombieSpawn(pos)) {
			ctx.getSource().sendFailure(Component.literal("Dort gibt es schon einen Spawnpunkt."));
			return 0;
		}
		int nr = map.getZombieSpawns().size();
		ctx.getSource().sendSuccess(() -> Component.literal("Zombie-Spawnpunkt #" + nr + " gesetzt: " + format(pos)), true);
		return 1;
	}

	private static int removeSpawn(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		int nr = IntegerArgumentType.getInteger(ctx, "nr");
		BlockPos removed = map.removeZombieSpawn(nr);
		if (removed == null) {
			ctx.getSource().sendFailure(Component.literal("Spawnpunkt #" + nr + " existiert nicht."));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal("Spawnpunkt #" + nr + " (" + format(removed) + ") entfernt."), true);
		return 1;
	}

	private static int listSpawns(CommandContext<CommandSourceStack> ctx) {
		MapData map = map(ctx);
		if (map == null) {
			return 0;
		}
		List<BlockPos> spawns = map.getZombieSpawns();
		BlockPos playerSpawn = map.getPlayerSpawn();
		ctx.getSource().sendSuccess(() -> Component.literal("Spieler-Startpunkt: "
				+ (playerSpawn == null ? "nicht gesetzt" : format(playerSpawn))).withStyle(ChatFormatting.GREEN), false);
		ctx.getSource().sendSuccess(() -> Component.literal(spawns.size() + " Zombie-Spawnpunkte:").withStyle(ChatFormatting.GOLD), false);
		for (int i = 0; i < spawns.size(); i++) {
			String line = " #" + (i + 1) + ": " + format(spawns.get(i));
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

	// ---------------------------------------------------------------- Hilfen

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
