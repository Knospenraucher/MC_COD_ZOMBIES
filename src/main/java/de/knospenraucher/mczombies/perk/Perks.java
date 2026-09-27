package de.knospenraucher.mczombies.perk;

import de.knospenraucher.mczombies.MCZombies;
import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.weapon.GunItem;
import de.knospenraucher.mczombies.weapon.GunManager;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Welche Perks jeder Spieler hat und was sie bewirken (Werte nach Black Ops III).
 * <p>
 * Perks gelten für ein Spiel; wer down geht, verliert alle. Die Waffen-Perks (Speed Cola,
 * Double Tap, Deadshot, Electric Cherry) fragt {@link GunManager} hier ab.
 */
public final class Perks {
	private static final Identifier JUGGERNOG_ID = MCZombies.id("perk_juggernog");
	private static final Identifier STAMIN_UP_ID = MCZombies.id("perk_stamin_up");
	/** Juggernog: 250 statt 100 Leben in BO3, also das 2,5-Fache (20 → 50 Minecraft-Leben). */
	private static final double JUGGERNOG_BONUS_HEALTH = 30.0;
	/** Stamin-Up: 7 % schneller. */
	private static final double STAMIN_UP_SPEED = 0.07;
	/** Speed Cola: Nachladezeit halbiert. */
	private static final double SPEED_COLA_RELOAD = 0.5;
	/** Double Tap 2.0: 33 % schneller feuern, jede Kugel zählt doppelt. */
	private static final double DOUBLE_TAP_FIRE_INTERVAL = 0.75;
	private static final double DOUBLE_TAP_DAMAGE = 2.0;
	/** Deadshot: Hüftfeuer streut nur halb so stark, Kopftreffer zählen etwas weiter unten schon. */
	private static final double DEADSHOT_HIP_SPREAD = 0.5;
	private static final double DEADSHOT_HEAD_TOLERANCE = 0.35;
	/** Quick Revive allein: so oft kann man sich selbst wiederbeleben. */
	public static final int SOLO_REVIVES = 3;
	/** Nach der Selbstwiederbelebung so lange unverwundbar. */
	private static final int REVIVE_PROTECTION_TICKS = 60;
	/** Electric Cherry: Schock-Reichweite und Schaden bei leerem Magazin, Abklingzeit. */
	private static final double CHERRY_MAX_RADIUS = 4.0;
	private static final double CHERRY_MAX_DAMAGE = 1000.0;
	private static final int CHERRY_COOLDOWN_TICKS = 40;
	/** Widow's Wine: Netz um den Spieler, wenn ein Zombie trifft. */
	private static final double WIDOW_RADIUS = 4.0;
	private static final int WIDOW_SLOW_TICKS = 120;
	private static final int WIDOW_COOLDOWN_TICKS = 300;
	private static final int WIDOW_KNIFE_SLOW_TICKS = 80;

	private static final Map<UUID, Set<Perk>> OWNED = new HashMap<>();
	private static final Map<UUID, Integer> SOLO_REVIVES_USED = new HashMap<>();
	private static final Map<UUID, Long> PROTECTED_UNTIL = new HashMap<>();
	private static final Map<UUID, Long> CHERRY_READY = new HashMap<>();
	private static final Map<UUID, Long> WIDOW_READY = new HashMap<>();

	private Perks() {
	}

	// ================================================================ Besitz

	public static boolean has(ServerPlayer player, Perk perk) {
		Set<Perk> perks = OWNED.get(player.getUUID());
		return perks != null && perks.contains(perk);
	}

	/** Perks des Spielers in Kaufreihenfolge. */
	public static List<Perk> list(ServerPlayer player) {
		Set<Perk> perks = OWNED.get(player.getUUID());
		return perks == null ? List.of() : new ArrayList<>(perks);
	}

	public static int count(ServerPlayer player) {
		Set<Perk> perks = OWNED.get(player.getUUID());
		return perks == null ? 0 : perks.size();
	}

	/** Gibt den Perk und wendet dauerhafte Wirkungen (Leben, Tempo) an. */
	public static void add(ServerPlayer player, Perk perk) {
		OWNED.computeIfAbsent(player.getUUID(), id -> new LinkedHashSet<>()).add(perk);
		switch (perk) {
			case JUGGERNOG -> {
				modifier(player, Attributes.MAX_HEALTH, JUGGERNOG_ID, JUGGERNOG_BONUS_HEALTH, AttributeModifier.Operation.ADD_VALUE);
				player.setHealth(player.getMaxHealth());
			}
			case STAMIN_UP -> modifier(player, Attributes.MOVEMENT_SPEED, STAMIN_UP_ID, STAMIN_UP_SPEED,
					AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
			default -> {
			}
		}
	}

	/** Nimmt alle Perks weg (beim Down und am Spielende). Mit Mule Kick geht die dritte Waffe verloren. */
	public static void clear(ServerPlayer player) {
		boolean hadMuleKick = has(player, Perk.MULE_KICK);
		OWNED.remove(player.getUUID());
		removeModifier(player, Attributes.MAX_HEALTH, JUGGERNOG_ID);
		removeModifier(player, Attributes.MOVEMENT_SPEED, STAMIN_UP_ID);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		if (hadMuleKick) {
			dropExtraWeapons(player);
		}
	}

	/** Neues Spiel oder Spielende: alles vergessen (die Attribute räumt {@link #clear} pro Spieler auf). */
	public static void reset() {
		OWNED.clear();
		SOLO_REVIVES_USED.clear();
		PROTECTED_UNTIL.clear();
		CHERRY_READY.clear();
		WIDOW_READY.clear();
	}

	// ================================================================ Waffen

	/** Wie viele Schusswaffen man tragen darf (2, mit Mule Kick 3). */
	public static int weaponLimit(ServerPlayer player) {
		return ZombiesConfig.get().weaponLimit + (has(player, Perk.MULE_KICK) ? 1 : 0);
	}

	public static double reloadFactor(ServerPlayer player) {
		return has(player, Perk.SPEED_COLA) ? SPEED_COLA_RELOAD : 1.0;
	}

	public static double fireIntervalFactor(ServerPlayer player) {
		return has(player, Perk.DOUBLE_TAP) ? DOUBLE_TAP_FIRE_INTERVAL : 1.0;
	}

	public static double bulletDamageFactor(ServerPlayer player) {
		return has(player, Perk.DOUBLE_TAP) ? DOUBLE_TAP_DAMAGE : 1.0;
	}

	public static double hipSpreadFactor(ServerPlayer player) {
		return has(player, Perk.DEADSHOT) ? DEADSHOT_HIP_SPREAD : 1.0;
	}

	public static double extraHeadTolerance(ServerPlayer player) {
		return has(player, Perk.DEADSHOT) ? DEADSHOT_HEAD_TOLERANCE : 0.0;
	}

	/** Mule Kick verloren: Schusswaffen über dem Limit (die zuletzt einsortierten) verschwinden. */
	private static void dropExtraWeapons(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		int limit = weaponLimit(player);
		int guns = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).getItem() instanceof GunItem && ++guns > limit) {
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
	}

	// ================================================================ Electric Cherry

	/**
	 * Nachladen mit Electric Cherry: elektrischer Schlag um den Spieler. Je leerer das Magazin,
	 * desto größer die Reichweite und der Schaden (wie in BO3).
	 */
	public static void onReloadStart(ServerPlayer player, int mag, int magazineSize) {
		if (!has(player, Perk.ELECTRIC_CHERRY) || magazineSize <= 0) {
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		if (now < CHERRY_READY.getOrDefault(player.getUUID(), 0L)) {
			return;
		}
		CHERRY_READY.put(player.getUUID(), now + CHERRY_COOLDOWN_TICKS);
		double empty = 1.0 - Math.max(0, Math.min(mag, magazineSize)) / (double) magazineSize;
		double radius = 1.5 + (CHERRY_MAX_RADIUS - 1.5) * empty;
		float damage = (float) Math.max(100.0, CHERRY_MAX_DAMAGE * empty);
		Vec3 center = player.position().add(0, 1.0, 0);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, (int) (20 + radius * 15),
				radius * 0.5, 0.6, radius * 0.5, 0.3);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4F, 1.8F);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
				e -> GunManager.isTarget(e, player) && e.distanceToSqr(center) <= radius * radius)) {
			Vec3 at = target.getBoundingBox().getCenter();
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 10, 0.3, 0.5, 0.3, 0.2);
			GunManager.hurt(level, player, target, damage, false);
		}
	}

	// ================================================================ Widow's Wine

	/** Ein Zombie hat getroffen: mit Widow's Wine bleiben alle Zombies in der Nähe in Netzen hängen. */
	public static void onHitByZombie(ServerLevel level, ServerPlayer player) {
		if (!has(player, Perk.WIDOWS_WINE)) {
			return;
		}
		long now = level.getGameTime();
		if (now < WIDOW_READY.getOrDefault(player.getUUID(), 0L)) {
			return;
		}
		WIDOW_READY.put(player.getUUID(), now + WIDOW_COOLDOWN_TICKS);
		Vec3 center = player.position();
		level.sendParticles(ParticleTypes.ITEM_COBWEB, center.x, center.y + 1.0, center.z, 60,
				WIDOW_RADIUS * 0.4, 0.8, WIDOW_RADIUS * 0.4, 0.05);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.7F);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(WIDOW_RADIUS),
				e -> GunManager.isTarget(e, player))) {
			web(level, target, WIDOW_SLOW_TICKS);
		}
	}

	/** Messerstich mit Widow's Wine: der getroffene Zombie wird eingesponnen. */
	public static void onKnifeHit(ServerPlayer player, LivingEntity target) {
		if (has(player, Perk.WIDOWS_WINE) && target.isAlive()) {
			web((ServerLevel) player.level(), target, WIDOW_KNIFE_SLOW_TICKS);
		}
	}

	private static void web(ServerLevel level, LivingEntity target, int ticks) {
		target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4));
		Vec3 at = target.getBoundingBox().getCenter();
		level.sendParticles(ParticleTypes.ITEM_COBWEB, at.x, at.y, at.z, 12, 0.3, 0.6, 0.3, 0.02);
	}

	// ================================================================ Quick Revive (allein)

	/**
	 * Allein mit Quick Revive: statt down zu gehen, steht man sofort wieder auf (höchstens
	 * {@link #SOLO_REVIVES}-mal pro Spiel). Alle Perks gehen dabei verloren, wie in BO3.
	 *
	 * @return true, wenn der Spieler sich selbst wiederbelebt hat
	 */
	public static boolean trySoloRevive(ServerPlayer player) {
		if (!has(player, Perk.QUICK_REVIVE)) {
			return false;
		}
		SOLO_REVIVES_USED.merge(player.getUUID(), 1, Integer::sum);
		clear(player);
		player.setHealth(player.getMaxHealth());
		player.clearFire();
		ServerLevel level = (ServerLevel) player.level();
		PROTECTED_UNTIL.put(player.getUUID(), level.getGameTime() + REVIVE_PROTECTION_TICKS);
		// Zombies direkt daneben zurückstoßen, damit man wieder Luft hat.
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3.0),
				e -> GunManager.isTarget(e, player))) {
			Vec3 push = target.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(1.2);
			target.push(push.x, 0.3, push.z);
		}
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(),
				40, 0.5, 0.8, 0.5, 0.3);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
		return true;
	}

	public static int soloRevivesUsed(ServerPlayer player) {
		return SOLO_REVIVES_USED.getOrDefault(player.getUUID(), 0);
	}

	/** Kurz nach der Selbstwiederbelebung greifen Zombies ins Leere. */
	public static boolean isProtected(ServerPlayer player) {
		return player.level().getGameTime() < PROTECTED_UNTIL.getOrDefault(player.getUUID(), 0L);
	}

	// ================================================================ Hilfen

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute,
			Identifier id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null && !instance.hasModifier(id)) {
			instance.addTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}

	private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute,
			Identifier id) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}
}
