package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.config.ZombiesConfig.GunStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Sonderwirkungen der Wunderwaffen, die keine normalen Kugeln verschießen. */
final class WonderWeapons {
	/** DG-2: so viele Zombies trifft ein Schuss insgesamt (DG-3 JZ: mehr). */
	private static final int CHAIN_TARGETS = 10;
	private static final int CHAIN_TARGETS_UPGRADED = 15;
	/** Wie weit der Blitz von einem Zombie zum nächsten springt. */
	private static final double CHAIN_JUMP = 6.0;

	/** Thundergun: halber Öffnungswinkel des Kegels (Kosinus). */
	private static final double THUNDER_CONE_COS = Math.cos(Math.toRadians(35));

	private WonderWeapons() {
	}

	/**
	 * Wunderwaffe DG-2: ein Blitz trifft den ersten Zombie im Visier und springt von dort
	 * auf weitere Zombies in der Nähe, ohne Schaden zu verlieren.
	 */
	static void lightning(ServerLevel level, ServerPlayer player, GunStats stats, float damage, boolean upgraded) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = GunManager.blockLimitedEnd(level, player, eye, player.getLookAngle(), stats.range);
		List<GunManager.BulletHit> hits = GunManager.traceEntities(level, player, eye, end);
		if (hits.isEmpty()) {
			arc(level, eye, end);
			return;
		}
		int max = upgraded ? CHAIN_TARGETS_UPGRADED : CHAIN_TARGETS;
		List<LivingEntity> struck = new ArrayList<>();
		LivingEntity current = hits.get(0).target();
		Vec3 from = eye;
		while (current != null && struck.size() < max) {
			struck.add(current);
			Vec3 to = current.getBoundingBox().getCenter();
			arc(level, from, to);
			from = to;
			Vec3 center = to;
			current = level.getEntitiesOfClass(LivingEntity.class, current.getBoundingBox().inflate(CHAIN_JUMP),
							e -> GunManager.isTarget(e, player) && !struck.contains(e))
					.stream()
					.min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(center)))
					.orElse(null);
		}
		for (LivingEntity target : struck) {
			GunManager.hurt(level, player, target, damage, false);
		}
	}

	/** Thundergun: eine Druckwelle tötet alle Zombies im Kegel vor dem Spieler und schleudert sie weg. */
	static void thunder(ServerLevel level, ServerPlayer player, GunStats stats, float damage, boolean upgraded) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		double range = upgraded ? stats.range * 1.5 : stats.range;
		for (double d = 1.0; d < range; d += 1.5) {
			Vec3 p = eye.add(look.scale(d));
			level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 4, d * 0.08, d * 0.08, d * 0.08, 0.02);
		}
		AABB area = player.getBoundingBox().inflate(range);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> GunManager.isTarget(e, player))) {
			Vec3 offset = target.getBoundingBox().getCenter().subtract(eye);
			double distance = offset.length();
			if (distance > range || distance < 1.0E-3 || offset.normalize().dot(look) < THUNDER_CONE_COS) {
				continue;
			}
			GunManager.hurt(level, player, target, damage, false);
			Vec3 push = offset.normalize().scale(upgraded ? 3.0 : 2.0);
			target.push(push.x, 0.6, push.z);
		}
		level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 2.0F, 0.6F);
	}

	private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (length < 0.5) {
			return;
		}
		Vec3 dir = delta.scale(1.0 / length);
		for (double d = 0.5; d < length; d += 0.4) {
			Vec3 p = from.add(dir.scale(d));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 12, 0.3, 0.5, 0.3, 0.2);
	}
}
