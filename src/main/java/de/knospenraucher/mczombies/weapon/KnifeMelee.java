package de.knospenraucher.mczombies.weapon;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Messer-Nahkampf auf Taste V, mit jeder Waffe in der Hand (wie in CoD).
 * <p>
 * Trifft den Zombie im Visier (bis {@link #REACH} Blöcke). Läuft der Spieler auf einen Zombie zu,
 * macht der Client einen Ausfallschritt wie in CoD; dann trifft das Messer den nächsten Zombie
 * in einem Kegel bis {@link #LUNGE_REACH} Blöcke. Schaden: Minecraft-Schaden 1 × {@code meleeDamageScale} = 150 wie das BO3-Messer.
 * Kills zählen als Nahkampf-Kills (130 Punkte).
 */
public final class KnifeMelee {
	private static final double REACH = 2.5;
	private static final double LUNGE_REACH = 4.0;
	/** Kosinus des halben Öffnungswinkels für den Ausfallschritt (etwa 35°). */
	private static final double LUNGE_CONE_COS = 0.82;
	/** Ticks zwischen zwei Messerstichen. */
	public static final int COOLDOWN_TICKS = 14;

	private static final Map<UUID, Long> NEXT_STAB = new HashMap<>();

	private KnifeMelee() {
	}

	/** @param lunge der Client hat einen Ausfallschritt gemacht (dann reicht das Messer weiter) */
	public static void stab(ServerPlayer player, boolean lunge) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		if (now < NEXT_STAB.getOrDefault(player.getUUID(), 0L)) {
			return;
		}
		NEXT_STAB.put(player.getUUID(), now + COOLDOWN_TICKS);

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8F, 1.4F);

		LivingEntity target = findTarget(level, player, eye, look, lunge);
		if (target == null) {
			return;
		}
		Vec3 hit = target.getBoundingBox().getCenter();
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.0, 0.0, 0.0, 0.0);
		level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.1F);
		// Keine Vanilla-Unverwundbarkeit nach einem Schuss: sonst ginge der Stich verloren.
		target.setInvulnerableTime(0);
		target.hurtServer(level, player.damageSources().playerAttack(player), 1.0F);
	}

	private static LivingEntity findTarget(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 look, boolean lunge) {
		Vec3 end = GunManager.blockLimitedEnd(level, player, eye, look, REACH);
		List<GunManager.BulletHit> direct = GunManager.traceEntities(level, player, eye, end);
		if (!direct.isEmpty()) {
			return direct.get(0).target();
		}
		if (!lunge) {
			return null;
		}
		AABB area = player.getBoundingBox().inflate(LUNGE_REACH);
		return level.getEntitiesOfClass(LivingEntity.class, area, e -> GunManager.isTarget(e, player))
				.stream()
				.filter(e -> {
					Vec3 offset = e.getBoundingBox().getCenter().subtract(eye);
					double distance = offset.length();
					return distance <= LUNGE_REACH + 0.5 && distance > 1.0E-3
							&& offset.normalize().dot(look) >= LUNGE_CONE_COS
							&& player.hasLineOfSight(e);
				})
				.min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
				.orElse(null);
	}
}
