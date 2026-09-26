package de.knospenraucher.mczombies.game;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.weapon.GunManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Leben der Rundenzombies in Black-Ops-III-Einheiten (Runde 1: 150, Runde 10: 1045 ...).
 * <p>
 * Minecraft erlaubt höchstens 1024 Leben, deshalb haben Rundenzombies in Minecraft immer 20 Leben
 * und das echte Leben steht hier. Jeder Treffer wird in {@code LivingEntityMixin} abgefangen:
 * Schusswaffen ziehen ihren BO3-Schaden direkt ab, alles andere (Faust, Schwert, Bogen) wird mit
 * {@link ZombiesConfig#meleeDamageScale} umgerechnet (Faust = 150 wie das Messer).
 * Erst wenn das BO3-Leben aufgebraucht ist, bekommt der Zombie tödlichen Minecraft-Schaden.
 */
public final class ZombieHealth {
	/** Minecraft-Leben eines Rundenzombies (nur für Anzeige und Trefferanimation). */
	public static final float MINECRAFT_HEALTH = 20.0F;
	/** Nicht tödliche Treffer: winziger Schaden, damit Trefferanimation und Punkte bleiben. */
	private static final float TOKEN_DAMAGE = 0.001F;
	/** So lange schützt ein Nahkampftreffer vor dem nächsten (wie die Vanilla-Unverwundbarkeit). */
	private static final long MELEE_COOLDOWN_TICKS = 10;

	private static final Map<UUID, Pool> POOLS = new HashMap<>();

	private static final class Pool {
		double health;
		long lastMeleeTick = Long.MIN_VALUE;

		Pool(double health) {
			this.health = health;
		}
	}

	private ZombieHealth() {
	}

	public static void init(LivingEntity zombie, double health) {
		POOLS.put(zombie.getUUID(), new Pool(health));
	}

	public static void remove(LivingEntity zombie) {
		POOLS.remove(zombie.getUUID());
	}

	public static void clear() {
		POOLS.clear();
	}

	/** Restliches BO3-Leben, oder -1 wenn der Zombie nicht verwaltet wird. */
	public static double get(LivingEntity zombie) {
		Pool pool = POOLS.get(zombie.getUUID());
		return pool == null ? -1 : pool.health;
	}

	/**
	 * Rechnet einen Treffer um und gibt den Minecraft-Schaden zurück, der tatsächlich ankommen soll.
	 * Für Wesen ohne BO3-Leben bleibt der Schaden unverändert.
	 */
	public static float onHurt(LivingEntity target, DamageSource source, float amount) {
		Pool pool = POOLS.get(target.getUUID());
		if (pool == null || amount <= 0) {
			return amount;
		}
		double damage;
		if (GunManager.currentShot() != null) {
			damage = amount;
		} else {
			long now = target.level().getGameTime();
			if (now - pool.lastMeleeTick < MELEE_COOLDOWN_TICKS) {
				return 0.0F;
			}
			pool.lastMeleeTick = now;
			damage = amount * ZombiesConfig.get().meleeDamageScale;
		}
		pool.health -= damage;
		if (pool.health <= 0) {
			return 1.0E6F;
		}
		return TOKEN_DAMAGE;
	}
}
