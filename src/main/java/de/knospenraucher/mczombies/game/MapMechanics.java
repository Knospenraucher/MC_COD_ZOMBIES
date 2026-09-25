package de.knospenraucher.mczombies.game;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.map.BlockSnapshots;
import de.knospenraucher.mczombies.map.MapData;
import de.knospenraucher.mczombies.map.MapData.BlockSnapshot;
import de.knospenraucher.mczombies.map.MapData.Door;
import de.knospenraucher.mczombies.map.MapData.Region;
import de.knospenraucher.mczombies.map.MapData.WallWeapon;
import de.knospenraucher.mczombies.map.MapData.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Laufzeit-Logik der Map-Elemente aus Phase 2:
 * kaufbare Türen (mit Zonen), Wandwaffen, Zufallskiste und Fenster-Barrikaden.
 * <p>
 * Die Definitionen stehen in {@link MapData}; diese Klasse kümmert sich darum,
 * was während eines Spiels mit ihnen passiert.
 */
public class MapMechanics {
	private static final int HINT_INTERVAL = 5;
	private static final double INTERACT_RANGE = 4.5;

	private final GameManager game;
	private final MapData map;
	private final RandomSource random = RandomSource.create();

	/** Namen der bereits gekauften Türen. */
	private final Set<String> openedDoors = new HashSet<>();
	/** Aktive Zonen (Start-Zone + Zonen geöffneter Türen). */
	private final Set<String> activeZones = new HashSet<>();

	/** Index des aktuellen Kistenstandorts in map.getBoxLocations(). */
	private int boxIndex = -1;
	private int boxUses;

	/** Reparatur-Punkte pro Spieler in dieser Runde (für die Obergrenze). */
	private final Map<UUID, Integer> repairPoints = new HashMap<>();
	/** Letzter Tick, in dem ein Spieler ein Brett repariert hat. */
	private final Map<UUID, Long> lastRepair = new HashMap<>();
	private long lastTear;

	MapMechanics(GameManager game, MapData map) {
		this.game = game;
		this.map = map;
	}

	// ================================================================ Lebenszyklus

	/** Neues Spiel: alles schließen/reparieren und Kiste an einen zufälligen Standort. */
	void start(ServerLevel level) {
		restoreAll(level);
		openedDoors.clear();
		activeZones.clear();
		activeZones.add(MapData.START_ZONE);
		repairPoints.clear();
		lastRepair.clear();
		List<BlockPos> boxes = map.getBoxLocations();
		boxIndex = boxes.isEmpty() ? -1 : random.nextInt(boxes.size());
		boxUses = 0;
	}

	/** Spielende: Türen wieder schließen, Fenster wieder vernageln. */
	void reset(ServerLevel level) {
		if (level != null) {
			restoreAll(level);
		}
		openedDoors.clear();
		activeZones.clear();
		boxIndex = -1;
	}

	void onRoundStart() {
		repairPoints.clear();
	}

	private void restoreAll(ServerLevel level) {
		for (Door door : map.getDoors()) {
			for (BlockSnapshot block : door.blocks) {
				BlockSnapshots.restore(level, block);
			}
		}
		for (Window window : map.getWindows()) {
			for (BlockSnapshot board : window.boards) {
				BlockSnapshots.restore(level, board);
			}
		}
	}

	public boolean isZoneActive(String zone) {
		return activeZones.contains(zone);
	}

	// ================================================================ Tick

	void tick(ServerLevel level, long ticks, Set<Zombie> zombies, List<ServerPlayer> alivePlayers) {
		ZombiesConfig config = ZombiesConfig.get();
		if (ticks - lastTear >= config.windowTearIntervalTicks) {
			lastTear = ticks;
			tearWindows(level, zombies);
		}
		for (ServerPlayer player : alivePlayers) {
			if (player.isShiftKeyDown()) {
				tryRepair(level, player, ticks);
			}
		}
		if (ticks % 10 == 0) {
			BlockPos box = activeBox();
			if (box != null) {
				level.sendParticles(ParticleTypes.END_ROD, box.getX() + 0.5, box.getY() + 1.5, box.getZ() + 0.5,
						3, 0.1, 1.0, 0.1, 0.0);
			}
		}
		if (ticks % HINT_INTERVAL == 0) {
			for (ServerPlayer player : alivePlayers) {
				showHint(player);
			}
		}
	}

	// ================================================================ Interaktion (Rechtsklick)

	/**
	 * Rechtsklick eines lebenden Teilnehmers auf einen Block.
	 *
	 * @return true, wenn der Klick zu einem Map-Element gehörte (Vanilla-Aktion wird dann unterdrückt)
	 */
	boolean onUse(ServerLevel level, ServerPlayer player, BlockPos pos) {
		Door door = closedDoorAt(pos);
		if (door != null) {
			buyDoor(level, player, door);
			return true;
		}
		WallWeapon weapon = map.getWallWeaponAt(pos);
		if (weapon != null) {
			buyWallWeapon(player, weapon);
			return true;
		}
		List<BlockPos> boxes = map.getBoxLocations();
		if (boxes.contains(pos)) {
			if (pos.equals(activeBox())) {
				useBox(player);
			} else {
				player.sendSystemMessage(Component.literal("Die Zufallskiste steht gerade woanders.").withStyle(ChatFormatting.GRAY), true);
			}
			return true;
		}
		return false;
	}

	/** Zeigt in der Aktionsleiste, was der Spieler gerade kaufen kann. */
	private void showHint(ServerPlayer player) {
		HitResult hit = player.pick(INTERACT_RANGE, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return;
		}
		BlockPos pos = blockHit.getBlockPos();
		Component hint = null;
		Door door = closedDoorAt(pos);
		WallWeapon weapon = map.getWallWeaponAt(pos);
		if (door != null) {
			hint = Component.literal("Rechtsklick: Tür öffnen [" + door.price + " Punkte]");
		} else if (weapon != null) {
			Item item = resolveItem(weapon.item);
			if (item != null) {
				String name = new ItemStack(item).getHoverName().getString();
				hint = hasItem(player, item) && isRanged(item)
						? Component.literal("Rechtsklick: Munition für " + name + " [" + weapon.ammoPrice + " Punkte]")
						: Component.literal("Rechtsklick: " + name + " kaufen [" + weapon.price + " Punkte]");
			}
		} else if (pos.equals(activeBox())) {
			hint = Component.literal("Rechtsklick: Zufallskiste [" + ZombiesConfig.get().boxPrice + " Punkte]");
		} else if (windowNear(player, 2.5) != null) {
			hint = Component.literal("Schleichen: Fenster reparieren");
		}
		if (hint != null) {
			player.sendSystemMessage(hint.copy().withStyle(ChatFormatting.YELLOW), true);
		}
	}

	// ================================================================ Türen

	private Door closedDoorAt(BlockPos pos) {
		for (Door door : map.getDoors()) {
			if (!openedDoors.contains(door.name) && door.contains(pos)) {
				return door;
			}
		}
		return null;
	}

	private void buyDoor(ServerLevel level, ServerPlayer player, Door door) {
		if (!game.spendPoints(player, door.price)) {
			notEnoughPoints(player, door.price);
			return;
		}
		openedDoors.add(door.name);
		if (door.zone != null) {
			activeZones.add(door.zone);
		}
		for (BlockSnapshot block : door.blocks) {
			level.destroyBlock(block.pos.toBlockPos(), false);
		}
		game.broadcast(Component.literal(player.getName().getString() + " hat „" + door.name + "“ geöffnet.")
				.withStyle(ChatFormatting.AQUA));
	}

	// ================================================================ Wandwaffen

	private void buyWallWeapon(ServerPlayer player, WallWeapon weapon) {
		Item item = resolveItem(weapon.item);
		if (item == null) {
			player.sendSystemMessage(Component.literal("Unbekanntes Item: " + weapon.item).withStyle(ChatFormatting.RED));
			return;
		}
		if (hasItem(player, item)) {
			if (!isRanged(item)) {
				player.sendSystemMessage(Component.literal("Du hast diese Waffe schon.").withStyle(ChatFormatting.GRAY), true);
				return;
			}
			if (!game.spendPoints(player, weapon.ammoPrice)) {
				notEnoughPoints(player, weapon.ammoPrice);
				return;
			}
			giveAmmo(player);
			player.sendSystemMessage(Component.literal("Munition gekauft.").withStyle(ChatFormatting.GREEN), true);
			return;
		}
		if (!game.spendPoints(player, weapon.price)) {
			notEnoughPoints(player, weapon.price);
			return;
		}
		giveWeapon(player, item);
	}

	// ================================================================ Zufallskiste

	private BlockPos activeBox() {
		List<BlockPos> boxes = map.getBoxLocations();
		if (boxIndex < 0 || boxIndex >= boxes.size()) {
			return null;
		}
		return boxes.get(boxIndex);
	}

	private void useBox(ServerPlayer player) {
		ZombiesConfig config = ZombiesConfig.get();
		if (!game.spendPoints(player, config.boxPrice)) {
			notEnoughPoints(player, config.boxPrice);
			return;
		}
		boxUses++;
		List<BlockPos> boxes = map.getBoxLocations();
		if (boxes.size() > 1 && boxUses >= config.boxMinUsesBeforeMove && random.nextDouble() < config.boxMoveChance) {
			// Kiste zieht um: Punkte zurück, neuer Standort.
			game.addPoints(player, config.boxPrice);
			int next = random.nextInt(boxes.size() - 1);
			boxIndex = next >= boxIndex ? next + 1 : next;
			boxUses = 0;
			game.broadcast(Component.literal("Die Zufallskiste ist weitergezogen!").withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}
		Item item = rollBoxWeapon();
		if (item == null) {
			game.addPoints(player, config.boxPrice);
			player.sendSystemMessage(Component.literal("Die Waffenliste der Kiste ist leer (Config prüfen).").withStyle(ChatFormatting.RED));
			return;
		}
		giveWeapon(player, item);
	}

	private Item rollBoxWeapon() {
		List<Item> items = new ArrayList<>();
		List<Integer> weights = new ArrayList<>();
		int total = 0;
		for (ZombiesConfig.BoxEntry entry : ZombiesConfig.get().boxWeapons) {
			Item item = resolveItem(entry.item);
			if (item != null && entry.weight > 0) {
				items.add(item);
				weights.add(entry.weight);
				total += entry.weight;
			}
		}
		if (total == 0) {
			return null;
		}
		int roll = random.nextInt(total);
		for (int i = 0; i < items.size(); i++) {
			roll -= weights.get(i);
			if (roll < 0) {
				return items.get(i);
			}
		}
		return items.get(items.size() - 1);
	}

	// ================================================================ Fenster

	/** Zombies direkt am Fenster reißen je ein Brett heraus. */
	private void tearWindows(ServerLevel level, Set<Zombie> zombies) {
		for (Window window : map.getWindows()) {
			AABB area = box(window).inflate(1.5);
			boolean zombieAtWindow = false;
			for (Zombie zombie : zombies) {
				if (!zombie.isRemoved() && zombie.getBoundingBox().intersects(area)) {
					zombieAtWindow = true;
					break;
				}
			}
			if (!zombieAtWindow) {
				continue;
			}
			List<BlockSnapshot> present = new ArrayList<>();
			for (BlockSnapshot board : window.boards) {
				if (BlockSnapshots.isPresent(level, board)) {
					present.add(board);
				}
			}
			if (!present.isEmpty()) {
				level.destroyBlock(present.get(random.nextInt(present.size())).pos.toBlockPos(), false);
			}
		}
	}

	/** Schleichende Spieler am Fenster setzen fehlende Bretter wieder ein. */
	private void tryRepair(ServerLevel level, ServerPlayer player, long ticks) {
		ZombiesConfig config = ZombiesConfig.get();
		Long last = lastRepair.get(player.getUUID());
		if (last != null && ticks - last < config.windowRepairIntervalTicks) {
			return;
		}
		Window window = windowNear(player, 2.5);
		if (window == null) {
			return;
		}
		for (BlockSnapshot board : window.boards) {
			if (BlockSnapshots.isPresent(level, board)) {
				continue;
			}
			BlockPos pos = board.pos.toBlockPos();
			AABB space = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
			if (!level.getEntities((Entity) null, space).isEmpty()) {
				continue; // Nicht in jemanden hineinbauen.
			}
			BlockSnapshots.restore(level, board);
			lastRepair.put(player.getUUID(), ticks);
			int earned = repairPoints.getOrDefault(player.getUUID(), 0);
			if (earned < config.windowRepairPointsCapPerRound) {
				int points = Math.min(config.pointsPerBoardRepair, config.windowRepairPointsCapPerRound - earned);
				repairPoints.put(player.getUUID(), earned + points);
				game.addPoints(player, points);
			}
			return;
		}
	}

	private Window windowNear(ServerPlayer player, double distance) {
		for (Window window : map.getWindows()) {
			if (player.getBoundingBox().intersects(box(window).inflate(distance))) {
				return window;
			}
		}
		return null;
	}

	private static AABB box(Region region) {
		return new AABB(region.min.x, region.min.y, region.min.z, region.max.x + 1, region.max.y + 1, region.max.z + 1);
	}

	// ================================================================ Items

	public static Item resolveItem(String id) {
		Identifier key = Identifier.tryParse(id);
		if (key == null) {
			return null;
		}
		return BuiltInRegistries.ITEM.getOptional(key).filter(item -> item != Items.AIR).orElse(null);
	}

	private static boolean isRanged(Item item) {
		return item == Items.BOW || item == Items.CROSSBOW;
	}

	private static boolean hasItem(ServerPlayer player, Item item) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(item)) {
				return true;
			}
		}
		return false;
	}

	/** Gibt eine unzerstörbare Waffe (bei Fernkampf mit Pfeilen). */
	private static void giveWeapon(ServerPlayer player, Item item) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		give(player, stack);
		if (isRanged(item)) {
			giveAmmo(player);
		}
		player.sendSystemMessage(Component.literal("Du hast " + stack.getHoverName().getString() + " bekommen!")
				.withStyle(ChatFormatting.GREEN), true);
	}

	private static void giveAmmo(ServerPlayer player) {
		give(player, new ItemStack(Items.ARROW, ZombiesConfig.get().arrowsPerAmmo));
	}

	private static void give(ServerPlayer player, ItemStack stack) {
		// Legt das Item ins Inventar oder lässt es fallen, wenn kein Platz ist.
		player.getInventory().placeItemBackInInventory(stack);
	}

	private static void notEnoughPoints(ServerPlayer player, int price) {
		player.sendSystemMessage(Component.literal("Nicht genug Punkte (" + price + " nötig).").withStyle(ChatFormatting.RED), true);
	}
}
