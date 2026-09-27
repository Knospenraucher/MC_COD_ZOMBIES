package de.knospenraucher.mczombies.game;

import de.knospenraucher.mczombies.MCZombies;
import de.knospenraucher.mczombies.config.ZombiesConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Leben der Spieler wie in Black Ops III.
 * <p>
 * Ein Spieler hat 150 BO3-Leben (Juggernog: 250), ein Zombie-Schlag nimmt 50: drei Schläge ohne,
 * fünf mit Juggernog. Umgerechnet 1 Minecraft-Leben = 5 BO3-Leben, also 30 bzw. 50 Minecraft-Leben
 * und 10 pro Schlag. Wer 2,4 Sekunden nicht getroffen wird, heilt sich schnell komplett; wer auf
 * höchstens 20 % war, muss 5 Sekunden warten. Hunger und Vanilla-Heilung sind im Spiel aus.
 */
public final class PlayerHealth {
	private static final Identifier BASE_HEALTH_ID = MCZombies.id("bo3_base_health");
	private static final ResourceKey<DamageType> ZOMBIE_SWIPE =
			ResourceKey.create(Registries.DAMAGE_TYPE, MCZombies.id("zombie_swipe"));
	private static final ResourceKey<DamageType> OWN_EXPLOSIVE =
			ResourceKey.create(Registries.DAMAGE_TYPE, MCZombies.id("own_explosive"));
	/** Nahrung knapp unter der Grenze, ab der Minecraft von selbst heilt (18); Sprinten geht ab 7. */
	private static final int FOOD_WITHOUT_REGEN = 17;

	private static final Map<UUID, State> STATES = new HashMap<>();

	private static final class State {
		long lastHurtTick = Long.MIN_VALUE / 2;
		boolean veryHurt;
	}

	private PlayerHealth() {
	}

	// ================================================================ Umrechnung

	/** BO3-Leben in Minecraft-Leben. */
	public static double toMinecraft(double bo3) {
		double scale = ZombiesConfig.get().bo3HealthPerMcHealth;
		return scale > 0 ? bo3 / scale : bo3;
	}

	/** Minecraft-Leben in BO3-Leben. */
	public static double toBo3(double minecraft) {
		double scale = ZombiesConfig.get().bo3HealthPerMcHealth;
		return scale > 0 ? minecraft * scale : minecraft;
	}

	// ================================================================ Spielbeginn und -ende

	/** Setzt das Grundleben (150 BO3) für das Spiel. */
	public static void applyBaseHealth(ServerPlayer player) {
		AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
		if (instance == null) {
			return;
		}
		instance.removeModifier(BASE_HEALTH_ID);
		double bonus = toMinecraft(ZombiesConfig.get().playerHealth) - instance.getBaseValue();
		if (Math.abs(bonus) > 1.0E-6) {
			instance.addTransientModifier(new AttributeModifier(BASE_HEALTH_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
		}
		player.setHealth(player.getMaxHealth());
		STATES.remove(player.getUUID());
	}

	/** Spielende: normales Minecraft-Leben und Hunger zurück. */
	public static void restore(ServerPlayer player) {
		AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
		if (instance != null) {
			instance.removeModifier(BASE_HEALTH_ID);
		}
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		STATES.remove(player.getUUID());
	}

	public static void reset() {
		STATES.clear();
	}

	// ================================================================ Treffer und Heilung

	/** Ein Teilnehmer wurde verletzt: die Heilung beginnt von vorn. */
	public static void onDamaged(ServerPlayer player) {
		State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
		state.lastHurtTick = player.level().getGameTime();
		if (player.getHealth() <= player.getMaxHealth() * ZombiesConfig.get().veryHurtFraction) {
			state.veryHurt = true;
		}
	}

	/** Jeden Tick für alle lebenden Teilnehmer: Heilung nach BO3 und kein Hunger. */
	public static void tick(Collection<ServerPlayer> players) {
		ZombiesConfig c = ZombiesConfig.get();
		for (ServerPlayer player : players) {
			if (!player.isAlive() || player.isSpectator()) {
				continue;
			}
			if (c.disableHunger && player.getFoodData().getFoodLevel() != FOOD_WITHOUT_REGEN) {
				player.getFoodData().setFoodLevel(FOOD_WITHOUT_REGEN);
			}
			float max = player.getMaxHealth();
			State state = STATES.computeIfAbsent(player.getUUID(), id -> new State());
			if (player.getHealth() >= max) {
				state.veryHurt = false;
				continue;
			}
			long delay = state.veryHurt ? c.veryHurtRegenDelayTicks : c.regenDelayTicks;
			if (player.level().getGameTime() - state.lastHurtTick < delay) {
				continue;
			}
			float step = (float) (max * Math.max(0.01, c.regenPerTick));
			player.setHealth(Math.min(max, player.getHealth() + step));
		}
	}

	// ================================================================ Schadensquellen

	/** Schlag eines Rundenzombies: wird nicht von der Schwierigkeit verändert, schiebt nicht und kennt keine Abklingzeit. */
	public static DamageSource zombieSwipe(ServerLevel level, Entity zombie) {
		return type(level, ZOMBIE_SWIPE).map(type -> new DamageSource(type, zombie))
				.orElseGet(() -> zombie.damageSources().generic());
	}

	/**
	 * Eigene Explosion (XM-53, Ray Gun) trifft den Schützen, wie in BO3: Hat man mehr als 75 Leben
	 * (Ray Gun: 25), zieht sie genau so viel ab, sonst den vollen Schaden.
	 *
	 * @param bo3Damage Explosionsschaden an dieser Stelle in BO3-Einheiten
	 */
	public static void explosiveSelfDamage(ServerLevel level, ServerPlayer player, double bo3Damage, boolean rayGun) {
		GameManager game = GameManager.get();
		if (game == null || !game.isRunning() || game.getPlayerData(player) == null
				|| !player.isAlive() || player.isSpectator() || bo3Damage <= 0) {
			return;
		}
		ZombiesConfig c = ZombiesConfig.get();
		double cap = rayGun ? c.rayGunSelfDamageCap : c.explosiveSelfDamageCap;
		double health = toBo3(player.getHealth());
		double damage = health > cap ? cap : bo3Damage;
		Optional<Holder.Reference<DamageType>> type = type(level, OWN_EXPLOSIVE);
		DamageSource source = type.<DamageSource>map(DamageSource::new).orElseGet(() -> player.damageSources().generic());
		player.hurtServer(level, source, (float) toMinecraft(damage));
	}

	private static Optional<Holder.Reference<DamageType>> type(ServerLevel level, ResourceKey<DamageType> key) {
		return level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(key);
	}
}
