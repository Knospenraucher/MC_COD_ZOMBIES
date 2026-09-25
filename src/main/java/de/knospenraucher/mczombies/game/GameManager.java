package de.knospenraucher.mczombies.game;

import de.knospenraucher.mczombies.MCZombies;
import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.map.MapData;
import de.knospenraucher.mczombies.network.HudSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Herzstück der Mod: verwaltet ein laufendes Spiel (Runden, Zombies, Spieler, Punkte).
 * <p>
 * Läuft ausschließlich auf dem Server. Clients bekommen den Zustand über
 * {@link HudSyncPayload} mitgeteilt und zeigen ihn nur an.
 */
public class GameManager {
	/** Entity-Tag, an dem wir unsere Rundenzombies erkennen. */
	public static final String ZOMBIE_TAG = "mczombies_round_zombie";

	private static final int HUD_SYNC_INTERVAL = 10;
	private static final int RETARGET_INTERVAL = 40;
	private static final int MOB_SWEEP_INTERVAL = 20;

	private static GameManager instance;

	private final MinecraftServer server;
	private final MapData map;
	private final MapMechanics mechanics;
	private final RandomSource random = RandomSource.create();

	private GameState state = GameState.IDLE;
	/** Welt, in der das Spiel läuft (dort, wo /zombies start ausgeführt wurde). */
	private ServerLevel level;
	private int round;
	/** Countdown in Ticks für INTERMISSION und GAME_OVER. */
	private int timer;
	/** Zombies, die in dieser Runde noch gespawnt werden müssen. */
	private int zombiesToSpawn;
	private int spawnCooldown;
	/** Aktuell lebende Rundenzombies. */
	private final Set<Zombie> aliveZombies = new HashSet<>();
	/** Teilnehmende Spieler in Beitrittsreihenfolge. */
	private final Map<UUID, PlayerData> players = new LinkedHashMap<>();
	private long ticks;
	private int showSpawnsTicks;

	private GameManager(MinecraftServer server) {
		this.server = server;
		this.map = MapData.load(server);
		this.mechanics = new MapMechanics(this, map);
	}

	// ================================================================ Lebenszyklus

	public static void create(MinecraftServer server) {
		instance = new GameManager(server);
	}

	public static void destroy() {
		if (instance != null) {
			try {
				instance.reset();
			} catch (Exception e) {
				MCZombies.LOGGER.warn("Fehler beim Aufräumen des Spiels", e);
			}
		}
		instance = null;
	}

	/** @return der GameManager des laufenden Servers oder null (z.B. im Hauptmenü) */
	public static GameManager get() {
		return instance;
	}

	public MapData getMap() {
		return map;
	}

	public GameState getState() {
		return state;
	}

	public int getRound() {
		return round;
	}

	public boolean isRunning() {
		return state == GameState.INTERMISSION || state == GameState.ACTIVE;
	}

	// ================================================================ Start / Reset

	/**
	 * Startet ein neues Spiel mit allen Nicht-Zuschauern in der angegebenen Welt.
	 *
	 * @return Fehlermeldung oder null bei Erfolg
	 */
	public String start(ServerLevel startLevel) {
		if (state != GameState.IDLE) {
			return "Es läuft bereits ein Spiel. Beende es mit /zombies reset.";
		}
		if (map.getZombieSpawns().isEmpty()) {
			return "Keine Zombie-Spawnpunkte gesetzt. Nutze /zombies spawn add.";
		}
		if (map.getZombieSpawns().stream().noneMatch(sp -> MapData.START_ZONE.equals(sp.zone()))) {
			return "Kein Spawnpunkt in der Zone „start“. Ohne ihn spawnen in Runde 1 keine Zombies.";
		}
		List<ServerPlayer> participants = new ArrayList<>();
		for (ServerPlayer player : startLevel.players()) {
			if (!player.isSpectator()) {
				participants.add(player);
			}
		}
		if (participants.isEmpty()) {
			return "Keine Spieler (außer Zuschauern) in dieser Welt.";
		}

		ZombiesConfig config = ZombiesConfig.get();
		level = startLevel;
		removeLeftoverZombies();
		removeOtherMobs();
		players.clear();
		aliveZombies.clear();
		round = 0;
		zombiesToSpawn = 0;

		mechanics.start(level);
		for (ServerPlayer player : participants) {
			players.put(player.getUUID(), new PlayerData(
					player.getName().getString(), config.startingPoints, currentGameType(player)));
			preparePlayer(player);
		}

		state = GameState.INTERMISSION;
		timer = Math.max(1, config.firstRoundDelaySeconds * 20);
		broadcast(Component.literal("Das Spiel beginnt in " + config.firstRoundDelaySeconds + " Sekunden!")
				.withStyle(ChatFormatting.GOLD));
		syncHud();
		return null;
	}

	/** Bricht ein laufendes Spiel ab und stellt alles wieder her. */
	public void reset() {
		removeAllZombies();
		// Türen/Fenster nur zurücksetzen, wenn wirklich ein Spiel lief,
		// damit Umbauten im Kreativmodus nicht überschrieben werden.
		mechanics.reset(level);
		level = null;
		restorePlayers();
		players.clear();
		round = 0;
		zombiesToSpawn = 0;
		state = GameState.IDLE;
		syncHud();
	}

	// ================================================================ Tick

	/** Wird jeden Server-Tick aufgerufen. */
	public void tick() {
		ticks++;
		if (showSpawnsTicks > 0) {
			showSpawnsTicks--;
			if (ticks % 10 == 0) {
				showSpawnParticles();
			}
		}

		switch (state) {
			case IDLE -> {
				return;
			}
			case INTERMISSION -> {
				if (--timer <= 0) {
					startRound(round + 1);
				}
			}
			case ACTIVE -> tickRound();
			case GAME_OVER -> {
				if (--timer <= 0) {
					reset();
					return;
				}
			}
		}

		if (isRunning()) {
			mechanics.tick(level, ticks, aliveZombies, alivePlayers());
		}
		if (isRunning() && ticks % MOB_SWEEP_INTERVAL == 0) {
			removeOtherMobs();
		}
		if (isRunning() && countAlivePlayers() == 0) {
			gameOver();
		}
		if (ticks % HUD_SYNC_INTERVAL == 0) {
			syncHud();
		}
	}

	private void startRound(int newRound) {
		round = newRound;
		reviveDownedPlayers();
		zombiesToSpawn = RoundScaling.zombieCount(round, countAlivePlayers());
		mechanics.onRoundStart();
		spawnCooldown = 0;
		state = GameState.ACTIVE;

		for (ServerPlayer player : onlineParticipants()) {
			sendTitle(player, Component.literal("Runde " + round).withStyle(ChatFormatting.DARK_RED),
					Component.literal(zombiesToSpawn + " Zombies").withStyle(ChatFormatting.GRAY));
		}
		syncHud();
	}

	private void tickRound() {
		// Entfernte Zombies (z.B. in ungeladenen Chunks) werden nachgespawnt,
		// damit die Runde nicht hängen bleibt. Getötete Zombies wurden bereits in
		// onZombieDeath entfernt.
		aliveZombies.removeIf(zombie -> {
			if (zombie.isRemoved()) {
				zombiesToSpawn++;
				return true;
			}
			return false;
		});

		ZombiesConfig config = ZombiesConfig.get();
		if (zombiesToSpawn > 0 && aliveZombies.size() < config.maxAliveZombies && --spawnCooldown <= 0) {
			if (spawnZombie()) {
				zombiesToSpawn--;
			}
			spawnCooldown = RoundScaling.spawnInterval(round);
		}

		if (ticks % RETARGET_INTERVAL == 0) {
			retargetZombies();
		}

		if (zombiesToSpawn <= 0 && aliveZombies.isEmpty()) {
			endRound();
		}
	}

	private void endRound() {
		ZombiesConfig config = ZombiesConfig.get();
		state = GameState.INTERMISSION;
		timer = Math.max(1, config.intermissionSeconds * 20);
		broadcast(Component.literal("Runde " + round + " überstanden! Nächste Runde in "
				+ config.intermissionSeconds + " Sekunden.").withStyle(ChatFormatting.GREEN));
		syncHud();
	}

	private void gameOver() {
		state = GameState.GAME_OVER;
		timer = Math.max(1, ZombiesConfig.get().gameOverDisplaySeconds * 20);
		removeAllZombies();

		Component subtitle = Component.literal("Ihr habt " + round + (round == 1 ? " Runde" : " Runden") + " erreicht")
				.withStyle(ChatFormatting.GRAY);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sendTitle(player, Component.literal("GAME OVER").withStyle(ChatFormatting.DARK_RED), subtitle);
		}

		broadcast(Component.literal("=== Game Over in Runde " + round + " ===").withStyle(ChatFormatting.DARK_RED));
		for (PlayerData data : sortedPlayers()) {
			broadcast(Component.literal(" " + data.name + ": " + data.points + " Punkte, "
					+ data.kills + " Kills, " + data.headshots + " Kopftreffer, " + data.downs + " Downs")
					.withStyle(ChatFormatting.GRAY));
		}
		syncHud();
	}

	// ================================================================ Zombies

	private boolean spawnZombie() {
		BlockPos spawn = pickSpawnPoint();
		if (spawn == null) {
			return false;
		}
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
		if (zombie == null) {
			return false;
		}
		zombie.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
		zombie.setYRot(random.nextFloat() * 360.0F);
		zombie.setBaby(false);
		zombie.setCanPickUpLoot(false);
		zombie.setPersistenceRequired();
		zombie.addTag(ZOMBIE_TAG);

		double health = RoundScaling.health(round);
		setAttribute(zombie, Attributes.MAX_HEALTH, health);
		setAttribute(zombie, Attributes.MOVEMENT_SPEED, RoundScaling.speed(round));
		setAttribute(zombie, Attributes.ATTACK_DAMAGE, RoundScaling.damage(round));
		setAttribute(zombie, Attributes.FOLLOW_RANGE, ZombiesConfig.get().followRange);
		// Keine Vanilla-Verstärkungen, sonst stimmt die Zombie-Anzahl nicht.
		setAttribute(zombie, Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
		zombie.setHealth((float) health);
		// Ein unzerstörbarer Helm verhindert, dass Zombies tagsüber verbrennen.
		ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
		helmet.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		zombie.setItemSlot(EquipmentSlot.HEAD, helmet);
		zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);

		if (!level.addFreshEntity(zombie)) {
			return false;
		}
		aliveZombies.add(zombie);
		ServerPlayer target = nearestAlivePlayer(zombie);
		if (target != null) {
			zombie.setTarget(target);
		}
		return true;
	}

	private static void setAttribute(LivingEntity entity, Holder<Attribute> attribute, double value) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	/**
	 * Wählt einen Spawnpunkt aus einer aktiven Zone. Punkte in der Nähe lebender
	 * Spieler werden bevorzugt.
	 */
	private BlockPos pickSpawnPoint() {
		List<BlockPos> all = new ArrayList<>();
		for (MapData.SpawnPoint spawn : map.getZombieSpawns()) {
			if (mechanics.isZoneActive(spawn.zone())) {
				all.add(spawn.toBlockPos());
			}
		}
		if (all.isEmpty()) {
			return null;
		}
		double radius = ZombiesConfig.get().spawnPointActivationRadius;
		double radiusSq = radius * radius;
		List<ServerPlayer> alive = alivePlayers();
		List<BlockPos> near = new ArrayList<>();
		for (BlockPos pos : all) {
			for (ServerPlayer player : alive) {
				if (player.level() == level && player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= radiusSq) {
					near.add(pos);
					break;
				}
			}
		}
		List<BlockPos> candidates = near.isEmpty() ? all : near;
		return candidates.get(random.nextInt(candidates.size()));
	}

	/** Lässt jeden Zombie den nächsten lebenden Spieler jagen. */
	private void retargetZombies() {
		for (Zombie zombie : aliveZombies) {
			ServerPlayer target = nearestAlivePlayer(zombie);
			if (target != null && zombie.getTarget() != target) {
				zombie.setTarget(target);
			}
		}
	}

	private ServerPlayer nearestAlivePlayer(Entity from) {
		ServerPlayer best = null;
		double bestDist = Double.MAX_VALUE;
		for (ServerPlayer player : alivePlayers()) {
			if (player.level() != from.level()) {
				continue;
			}
			double dist = player.distanceToSqr(from);
			if (dist < bestDist) {
				bestDist = dist;
				best = player;
			}
		}
		return best;
	}

	private void removeAllZombies() {
		for (Zombie zombie : aliveZombies) {
			zombie.discard();
		}
		aliveZombies.clear();
		zombiesToSpawn = 0;
		removeLeftoverZombies();
	}

	/** Entfernt markierte Zombies, die z.B. nach einem Serverneustart übrig geblieben sind. */
	private void removeLeftoverZombies() {
		if (level == null) {
			return;
		}
		for (Zombie zombie : level.getEntities(EntityTypes.ZOMBIE, z -> z.entityTags().contains(ZOMBIE_TAG))) {
			zombie.discard();
		}
	}

	/**
	 * Entfernt alle Mobs (Kühe, Skelette, Creeper ...) außer unseren Rundenzombies,
	 * damit während des Spiels nur Zombies auf der Map sind.
	 */
	private void removeOtherMobs() {
		if (level == null || !ZombiesConfig.get().removeOtherMobsDuringGame) {
			return;
		}
		for (Mob mob : level.getEntities(EntityTypeTest.forClass(Mob.class), mob -> !isRoundZombie(mob))) {
			mob.discard();
		}
	}

	public static boolean isRoundZombie(Entity entity) {
		return entity instanceof Zombie && entity.entityTags().contains(ZOMBIE_TAG);
	}

	/** Aufgerufen, wenn ein Rundenzombie stirbt (egal wodurch). */
	public void onZombieDeath(Entity zombie) {
		aliveZombies.remove(zombie);
	}

	// ================================================================ Spieler

	/** Heilt, setzt Spielmodus und teleportiert zum Spieler-Startpunkt. */
	private void preparePlayer(ServerPlayer player) {
		if (ZombiesConfig.get().adventureModeDuringGame) {
			player.setGameMode(GameType.ADVENTURE);
		} else if (player.isSpectator()) {
			player.setGameMode(GameType.SURVIVAL);
		}
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.clearFire();

		BlockPos spawn = map.getPlayerSpawn();
		if (spawn != null && player.level() == level) {
			player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
		} else {
			// Ohne Startpunkt: zu einem lebenden Mitspieler, falls vorhanden.
			for (ServerPlayer mate : alivePlayers()) {
				if (mate != player && mate.level() == player.level()) {
					player.teleportTo(mate.getX(), mate.getY(), mate.getZ());
					break;
				}
			}
		}
	}

	/**
	 * Wird aufgerufen, bevor ein Spieler stirbt.
	 *
	 * @return true, wenn der Tod abgefangen wurde (Spieler ist stattdessen "down")
	 */
	public boolean onPlayerLethalDamage(ServerPlayer player) {
		if (!isRunning()) {
			return false;
		}
		PlayerData data = players.get(player.getUUID());
		if (data == null) {
			return false;
		}
		if (!data.down) {
			data.down = true;
			data.downs++;
			broadcast(Component.literal(data.name + " ist gefallen!").withStyle(ChatFormatting.RED));
		}
		// Phase 1: Down = Zuschauer bis zur nächsten Runde.
		// (Wiederbelebung durch Mitspieler kommt mit dem Down-System in Phase 4.)
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.setGameMode(GameType.SPECTATOR);
		syncHud();
		return true;
	}

	private void reviveDownedPlayers() {
		for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
			PlayerData data = entry.getValue();
			if (!data.down) {
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				data.down = false;
				preparePlayer(player);
				player.sendSystemMessage(Component.literal("Du bist wieder im Spiel!").withStyle(ChatFormatting.GREEN));
			}
		}
	}

	/** Stellt nach Spielende den ursprünglichen Spielmodus wieder her. */
	private void restorePlayers() {
		for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null) {
				player.setGameMode(entry.getValue().previousGameType);
				player.setHealth(player.getMaxHealth());
			}
		}
	}

	/** Ermittelt den aktuellen Spielmodus ohne Zugriff auf interne Felder. */
	private static GameType currentGameType(ServerPlayer player) {
		if (player.isCreative()) {
			return GameType.CREATIVE;
		}
		if (player.isSpectator()) {
			return GameType.SPECTATOR;
		}
		return player.getAbilities().mayBuild ? GameType.SURVIVAL : GameType.ADVENTURE;
	}

	private List<ServerPlayer> onlineParticipants() {
		List<ServerPlayer> result = new ArrayList<>();
		for (UUID uuid : players.keySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			if (player != null) {
				result.add(player);
			}
		}
		return result;
	}

	/** Online-Teilnehmer, die nicht down sind. */
	private List<ServerPlayer> alivePlayers() {
		List<ServerPlayer> result = new ArrayList<>();
		for (ServerPlayer player : onlineParticipants()) {
			PlayerData data = players.get(player.getUUID());
			if (data != null && !data.down) {
				result.add(player);
			}
		}
		return result;
	}

	private int countAlivePlayers() {
		return alivePlayers().size();
	}

	// ================================================================ Punkte

	public PlayerData getPlayerData(ServerPlayer player) {
		return players.get(player.getUUID());
	}

	/** Schreibt einem Teilnehmer Punkte gut (negativ = abziehen, nie unter 0). */
	public void addPoints(ServerPlayer player, int amount) {
		PlayerData data = players.get(player.getUUID());
		if (data != null) {
			data.points = Math.max(0, data.points + amount);
		}
	}

	/**
	 * Zieht Punkte ab, wenn genug vorhanden sind.
	 *
	 * @return false, wenn der Spieler zu wenig Punkte hat oder nicht teilnimmt
	 */
	public boolean spendPoints(ServerPlayer player, int amount) {
		PlayerData data = players.get(player.getUUID());
		if (data == null || data.points < amount) {
			return false;
		}
		data.points -= amount;
		syncHud();
		return true;
	}

	/**
	 * Rechtsklick auf einen Block. Nur lebende Teilnehmer eines laufenden Spiels
	 * können Türen, Waffen und die Kiste benutzen.
	 *
	 * @return true, wenn der Klick verarbeitet wurde
	 */
	public boolean onUseBlock(ServerPlayer player, BlockPos pos) {
		if (!isRunning() || level == null || player.level() != level) {
			return false;
		}
		PlayerData data = players.get(player.getUUID());
		if (data == null || data.down) {
			return false;
		}
		return mechanics.onUse(level, player, pos);
	}

	/** @return false, wenn der Spieler nicht am Spiel teilnimmt */
	public boolean setPoints(ServerPlayer player, int amount) {
		PlayerData data = players.get(player.getUUID());
		if (data == null) {
			return false;
		}
		data.points = Math.max(0, amount);
		syncHud();
		return true;
	}

	private List<PlayerData> sortedPlayers() {
		List<PlayerData> list = new ArrayList<>(players.values());
		list.sort(Comparator.comparingInt((PlayerData d) -> d.points).reversed());
		return list;
	}

	// ================================================================ Anzeige

	/** Zeigt die Spawnpunkte für einige Sekunden mit Partikeln an. */
	public void showSpawns(int seconds) {
		showSpawnsTicks = seconds * 20;
	}

	private void showSpawnParticles() {
		ServerLevel overworld = level != null ? level : server.overworld();
		for (MapData.SpawnPoint pos : map.getZombieSpawns()) {
			overworld.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 6, 0.2, 0.5, 0.2, 0.01);
		}
		for (MapData.Door door : map.getDoors()) {
			for (MapData.BlockSnapshot block : door.blocks) {
				overworld.sendParticles(ParticleTypes.FLAME,
						block.pos.x + 0.5, block.pos.y + 0.5, block.pos.z + 0.5, 1, 0.2, 0.2, 0.2, 0.0);
			}
		}
		for (MapData.Window window : map.getWindows()) {
			for (MapData.BlockSnapshot board : window.boards) {
				overworld.sendParticles(ParticleTypes.CRIT,
						board.pos.x + 0.5, board.pos.y + 0.5, board.pos.z + 0.5, 2, 0.2, 0.2, 0.2, 0.0);
			}
		}
		for (MapData.WallWeapon weapon : map.getWallWeapons()) {
			overworld.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					weapon.pos.x + 0.5, weapon.pos.y + 0.5, weapon.pos.z + 0.5, 4, 0.3, 0.3, 0.3, 0.0);
		}
		for (BlockPos box : map.getBoxLocations()) {
			overworld.sendParticles(ParticleTypes.END_ROD,
					box.getX() + 0.5, box.getY() + 1.2, box.getZ() + 0.5, 4, 0.2, 0.4, 0.2, 0.0);
		}
		BlockPos playerSpawn = map.getPlayerSpawn();
		if (playerSpawn != null) {
			overworld.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					playerSpawn.getX() + 0.5, playerSpawn.getY() + 0.5, playerSpawn.getZ() + 0.5, 10, 0.3, 0.5, 0.3, 0.0);
		}
	}

	/** Schickt den aktuellen Spielzustand an alle Clients, die die Mod installiert haben. */
	public void syncHud() {
		List<HudSyncPayload.Entry> entries = new ArrayList<>();
		for (PlayerData data : sortedPlayers()) {
			entries.add(new HudSyncPayload.Entry(data.name, data.points, data.down));
		}
		int seconds = state == GameState.INTERMISSION ? (timer + 19) / 20 : 0;
		HudSyncPayload payload = new HudSyncPayload(state.ordinal(), round,
				Math.max(0, zombiesToSpawn) + aliveZombies.size(), seconds, entries);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, HudSyncPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	public String statusText() {
		return switch (state) {
			case IDLE -> "Kein Spiel aktiv.";
			case INTERMISSION -> "Pause vor Runde " + (round + 1) + " (" + (timer + 19) / 20 + " s), "
					+ countAlivePlayers() + " Spieler aktiv.";
			case ACTIVE -> "Runde " + round + ": " + (zombiesToSpawn + aliveZombies.size()) + " Zombies übrig, "
					+ countAlivePlayers() + " Spieler aktiv.";
			case GAME_OVER -> "Game Over in Runde " + round + ".";
		};
	}

	void broadcast(Component message) {
		server.getPlayerList().broadcastSystemMessage(message, false);
	}

	private static void sendTitle(ServerPlayer player, Component title, Component subtitle) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}
}
