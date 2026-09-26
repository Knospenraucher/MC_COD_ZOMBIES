package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.config.ZombiesConfig.GunStats;
import de.knospenraucher.mczombies.network.GunActionPayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serverseitige Waffenlogik: Feuerrate, Munition, Nachladen und Treffer.
 * <p>
 * Kugeln sind "Hitscan": ein Strahl vom Auge des Spielers in Blickrichtung,
 * der an Blöcken endet und bis zu {@code penetration} Gegner trifft.
 * Raketen explodieren am ersten Treffer (Block oder Gegner) und schaden allen
 * Nicht-Spielern im Radius; Blöcke bleiben heil.
 */
public final class GunManager {
	/** Abstand der Leuchtspur-Partikel in Blöcken. */
	private static final double TRACER_STEP = 2.5;
	private static final int TRACER_MAX_POINTS = 12;

	/** Frühester Tick für den nächsten Schuss je Spieler. */
	private static final Map<UUID, Long> nextShot = new HashMap<>();
	/** Laufende Nachladevorgänge je Spieler. */
	private static final Map<UUID, Reload> reloads = new HashMap<>();
	/** Laufende Feuerstöße je Spieler. */
	private static final Map<UUID, Burst> bursts = new HashMap<>();
	private static long ticks;

	/** Gerade verarbeiteter Schuss; {@code CombatEvents} liest ihn für Kill-Punkte. */
	private static Shot currentShot;

	/** Ein Treffer durch eine Waffe: wer geschossen hat und ob es ein Kopftreffer war. */
	public record Shot(ServerPlayer shooter, boolean headshot) {
	}

	private record Reload(int slot, long endTick) {
	}

	/** Noch ausstehende Schüsse eines Feuerstoßes. */
	private record Burst(int slot, int remaining, long nextTick) {
	}

	record BulletHit(LivingEntity target, Vec3 point, double distance) {
	}

	private GunManager() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(GunActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				switch (payload.action()) {
					case GunActionPayload.RELOAD -> startReload(player, true);
					case GunActionPayload.MELEE -> KnifeMelee.stab(player);
					default -> shoot(player);
				}
			});
		});
		ServerTickEvents.END_SERVER_TICK.register(GunManager::tick);
	}

	/** Der Schuss, dessen Schaden gerade verarbeitet wird, sonst null. */
	public static Shot currentShot() {
		return currentShot;
	}

	// ================================================================ Schießen

	private static void shoot(ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof GunItem gun) || reloads.containsKey(player.getUUID())) {
			return;
		}
		if (ticks < nextShot.getOrDefault(player.getUUID(), 0L)) {
			return;
		}
		if (GunItem.isReloading(stack)) {
			// Übrig gebliebene Markierung (z. B. nach Server-Neustart) entfernen.
			GunItem.setReloading(stack, false);
		}
		GunStats stats = gun.stats();
		ServerLevel level = (ServerLevel) player.level();
		int mag = gun.getMag(stack);
		int reserve = gun.getReserve(stack);
		if (mag <= 0) {
			if (reserve > 0) {
				startReload(player, false);
			} else {
				// Leer: Klicken, und nicht in jedem Tick erneut.
				nextShot.put(player.getUUID(), ticks + 10);
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.6F);
				player.sendSystemMessage(Component.literal("Keine Munition mehr!").withStyle(ChatFormatting.RED), true);
			}
			return;
		}

		int burst = Math.max(1, stats.burst);
		int interval = Math.max(1, stats.burstIntervalTicks);
		nextShot.put(player.getUUID(), ticks + Math.max(1, stats.fireRateTicks) + (long) (burst - 1) * interval);
		fireOnce(level, player, stack, gun);
		if (burst > 1) {
			bursts.put(player.getUUID(), new Burst(player.getInventory().getSelectedSlot(), burst - 1, ticks + interval));
		}
	}

	/** Ein einzelner Schuss (auch innerhalb eines Feuerstoßes); verbraucht eine Kugel. */
	private static void fireOnce(ServerLevel level, ServerPlayer player, ItemStack stack, GunItem gun) {
		GunStats stats = gun.stats();
		int mag = gun.getMag(stack);
		int reserve = gun.getReserve(stack);
		if (mag <= 0) {
			return;
		}
		gun.setAmmo(stack, mag - 1, reserve);
		boolean upgraded = GunItem.isUpgraded(stack);
		playShotSound(level, player, gun, upgraded);

		float damage = (float) gun.damage(stack);
		String special = stats.special == null ? "" : stats.special;
		double radius = gun.explosionRadius(stack);
		switch (special) {
			case "lightning" -> WonderWeapons.lightning(level, player, stats, damage, upgraded);
			case "thunder" -> WonderWeapons.thunder(level, player, stats, damage, upgraded);
			default -> {
				if (radius > 0) {
					fireRocket(level, player, stats, damage, radius, special.equals("ray"), upgraded);
				} else {
					fireBullets(level, player, stats, damage, special.equals("annihilate"));
				}
			}
		}

		if (mag - 1 == 0 && reserve > 0) {
			bursts.remove(player.getUUID());
			startReload(player, false);
		}
	}

	private static void fireBullets(ServerLevel level, ServerPlayer player, GunStats stats, float damage, boolean annihilate) {
		Vec3 eye = player.getEyePosition();
		// Treffer aller Kugeln eines Schusses sammeln (Schrotflinte), damit jeder Gegner
		// den Schaden als einen einzigen Treffer bekommt.
		Map<LivingEntity, Float> damageByTarget = new LinkedHashMap<>();
		Map<LivingEntity, Boolean> headshotByTarget = new HashMap<>();
		int pellets = Math.max(1, stats.pellets);
		for (int i = 0; i < pellets; i++) {
			Vec3 dir = spread(player.getLookAngle(), stats.spread, player.getRandom());
			Vec3 end = blockLimitedEnd(level, player, eye, dir, stats.range);
			List<BulletHit> hits = traceEntities(level, player, eye, end);
			int penetration = Math.max(1, stats.penetration);
			Vec3 tracerEnd = end;
			for (int h = 0; h < hits.size() && h < penetration; h++) {
				BulletHit hit = hits.get(h);
				boolean headshot = isHeadshot(hit.target(), hit.point());
				float amount = headshot ? (float) (damage * stats.headshotMultiplier) : damage;
				damageByTarget.merge(hit.target(), amount, Float::sum);
				headshotByTarget.merge(hit.target(), headshot, Boolean::logicalOr);
				tracerEnd = hit.point();
				level.sendParticles(headshot ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
						hit.point().x, hit.point().y, hit.point().z, 4, 0.05, 0.05, 0.05, 0.1);
				if (annihilate) {
					// Annihilator: der Zombie zerplatzt.
					AABB box = hit.target().getBoundingBox();
					level.sendParticles(ParticleTypes.CRIMSON_SPORE, box.getCenter().x, box.getCenter().y, box.getCenter().z,
							30, 0.3, 0.5, 0.3, 0.1);
				}
			}
			if (hits.isEmpty()) {
				level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.02, 0.02, 0.02, 0.0);
			}
			tracer(level, eye, tracerEnd, pellets > 1);
		}
		damageByTarget.forEach((target, amount) ->
				hurt(level, player, target, amount, headshotByTarget.getOrDefault(target, false)));
	}

	private static void fireRocket(ServerLevel level, ServerPlayer player, GunStats stats, float damage, double radius,
			boolean ray, boolean upgraded) {
		Vec3 eye = player.getEyePosition();
		Vec3 dir = spread(player.getLookAngle(), stats.spread, player.getRandom());
		Vec3 end = blockLimitedEnd(level, player, eye, dir, stats.range);
		List<BulletHit> hits = traceEntities(level, player, eye, end);
		Vec3 impact = hits.isEmpty() ? end : hits.get(0).point();

		// Rauchspur (Ray Gun: grüner bzw. roter Strahl)
		double length = impact.distanceTo(eye);
		for (double d = 1.0; d < length; d += ray ? 0.5 : 1.0) {
			Vec3 p = eye.add(dir.scale(d));
			if (ray) {
				level.sendParticles(upgraded ? ParticleTypes.FLAME : ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			} else {
				level.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
			}
		}

		if (ray) {
			level.sendParticles(ParticleTypes.EXPLOSION, impact.x, impact.y, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
		} else {
			level.sendParticles(radius >= 3 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION,
					impact.x, impact.y, impact.z, 1, 0.0, 0.0, 0.0, 0.0);
			level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 1.0F);
		}

		AABB area = new AABB(impact.x - radius, impact.y - radius, impact.z - radius,
				impact.x + radius, impact.y + radius, impact.z + radius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> isTarget(e, player))) {
			double distance = target.getBoundingBox().getCenter().distanceTo(impact);
			if (distance > radius) {
				continue;
			}
			// Voller Schaden im Zentrum, 40 % am Rand.
			float amount = (float) (damage * (1.0 - 0.6 * distance / radius));
			hurt(level, player, target, amount, false);
		}
	}

	/** Endpunkt eines Strahls: Reichweite oder der erste getroffene Block. */
	static Vec3 blockLimitedEnd(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 dir, double range) {
		Vec3 end = eye.add(dir.scale(range));
		BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
	}

	/** Alle Gegner auf der Strecke eye → end, nach Entfernung sortiert. */
	static List<BulletHit> traceEntities(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 end) {
		AABB area = new AABB(eye.x, eye.y, eye.z, end.x, end.y, end.z).inflate(1.0);
		List<BulletHit> hits = new ArrayList<>();
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> isTarget(e, player))) {
			target.getBoundingBox().inflate(0.1).clip(eye, end)
					.ifPresent(point -> hits.add(new BulletHit(target, point, point.distanceTo(eye))));
		}
		hits.sort(Comparator.comparingDouble(BulletHit::distance));
		return hits;
	}

	/** Spieler (Mitspieler) werden nie getroffen. */
	static boolean isTarget(LivingEntity entity, ServerPlayer shooter) {
		return entity != shooter && entity.isAlive() && !(entity instanceof Player);
	}

	private static boolean isHeadshot(LivingEntity target, Vec3 point) {
		return point.y >= target.getEyeY() - ZombiesConfig.get().headshotTolerance;
	}

	private static Vec3 spread(Vec3 look, double spread, RandomSource random) {
		if (spread <= 0) {
			return look;
		}
		return look.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread)
				.normalize();
	}

	/** Schaden als Spielerangriff, damit Punkte und Kills dem Schützen gutgeschrieben werden. */
	static void hurt(ServerLevel level, ServerPlayer shooter, LivingEntity target, float amount, boolean headshot) {
		Shot previous = currentShot;
		currentShot = new Shot(shooter, headshot);
		try {
			// Keine Schadens-Abklingzeit: sonst würden schnelle Waffen viele Treffer verlieren.
			target.setInvulnerableTime(0);
			target.hurtServer(level, shooter.damageSources().playerAttack(shooter), amount);
		} finally {
			currentShot = previous;
		}
	}

	private static void tracer(ServerLevel level, Vec3 from, Vec3 to, boolean sparse) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (length < TRACER_STEP) {
			return;
		}
		Vec3 dir = delta.scale(1.0 / length);
		double step = sparse ? TRACER_STEP * 2 : TRACER_STEP;
		int points = 0;
		for (double d = 1.5; d < length && points < TRACER_MAX_POINTS; d += step, points++) {
			Vec3 p = from.add(dir.scale(d));
			level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static void playShotSound(ServerLevel level, ServerPlayer player, GunItem gun, boolean upgraded) {
		double x = player.getX();
		double y = player.getEyeY();
		double z = player.getZ();
		float pitchBonus = upgraded ? 0.15F : 0.0F;
		String special = gun.stats().special == null ? "" : gun.stats().special;
		switch (special) {
			case "ray" -> {
				level.playSound(null, x, y, z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2F, 1.8F + pitchBonus);
				return;
			}
			case "lightning" -> {
				level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.6F + pitchBonus);
				return;
			}
			case "thunder" -> {
				level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.5F + pitchBonus);
				return;
			}
			default -> {
			}
		}
		switch (gun.category()) {
			case "shotgun" -> {
				level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.5F, 0.6F + pitchBonus);
				level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F + pitchBonus);
			}
			case "sniper" ->
					level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 2.0F, 0.5F + pitchBonus);
			case "launcher" ->
					level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 2.0F, 0.6F + pitchBonus);
			case "lmg" ->
					level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.1F + pitchBonus);
			case "rifle" ->
					level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F + pitchBonus);
			case "smg" ->
					level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8F, 1.9F + pitchBonus);
			case "wonder" ->
					level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.5F, 1.2F + pitchBonus);
			default ->
					level.playSound(null, x, y, z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.9F, 1.6F + pitchBonus);
		}
	}

	// ================================================================ Nachladen

	/**
	 * Beginnt das Nachladen der Waffe in der Hand.
	 *
	 * @param manual true = per Taste ausgelöst (dann gibt es Rückmeldung, falls es nicht geht)
	 */
	private static void startReload(ServerPlayer player, boolean manual) {
		if (!player.isAlive() || player.isSpectator() || reloads.containsKey(player.getUUID())) {
			return;
		}
		ItemStack stack = player.getMainHandItem();
		if (!(stack.getItem() instanceof GunItem gun)) {
			return;
		}
		if (gun.getMag(stack) >= gun.magazineSize(stack)) {
			return;
		}
		if (gun.getReserve(stack) <= 0) {
			if (manual) {
				player.sendSystemMessage(Component.literal("Keine Reservemunition.").withStyle(ChatFormatting.RED), true);
			}
			return;
		}
		int slot = player.getInventory().getSelectedSlot();
		reloads.put(player.getUUID(), new Reload(slot, ticks + Math.max(1, gun.stats().reloadTicks)));
		GunItem.setReloading(stack, true);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8F, 1.3F);
	}

	private static void tick(MinecraftServer server) {
		ticks++;
		tickBursts(server);
		Iterator<Map.Entry<UUID, Reload>> it = reloads.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Reload> entry = it.next();
			Reload reload = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			ItemStack stack = player.getInventory().getItem(reload.slot());
			boolean stillHolding = player.getInventory().getSelectedSlot() == reload.slot()
					&& stack.getItem() instanceof GunItem && player.isAlive() && !player.isSpectator();
			if (!stillHolding) {
				// Waffe gewechselt: Nachladen abgebrochen.
				if (stack.getItem() instanceof GunItem) {
					GunItem.setReloading(stack, false);
				}
				it.remove();
				continue;
			}
			if (ticks >= reload.endTick()) {
				finishReload(player, stack, (GunItem) stack.getItem());
				it.remove();
			}
		}
	}

	private static void tickBursts(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Burst>> it = bursts.entrySet().iterator();
		List<Runnable> shots = new ArrayList<>();
		while (it.hasNext()) {
			Map.Entry<UUID, Burst> entry = it.next();
			Burst burst = entry.getValue();
			if (ticks < burst.nextTick()) {
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || !player.isAlive() || player.isSpectator()
					|| player.getInventory().getSelectedSlot() != burst.slot()
					|| !(player.getMainHandItem().getItem() instanceof GunItem gun)
					|| reloads.containsKey(player.getUUID())) {
				it.remove();
				continue;
			}
			ItemStack stack = player.getMainHandItem();
			if (burst.remaining() <= 1) {
				it.remove();
			} else {
				int interval = Math.max(1, gun.stats().burstIntervalTicks);
				entry.setValue(new Burst(burst.slot(), burst.remaining() - 1, ticks + interval));
			}
			// Erst nach dem Durchlauf schießen: fireOnce kann den Feuerstoß beenden (Magazin leer).
			shots.add(() -> fireOnce((ServerLevel) player.level(), player, stack, gun));
		}
		shots.forEach(Runnable::run);
	}

	private static void finishReload(ServerPlayer player, ItemStack stack, GunItem gun) {
		int mag = gun.getMag(stack);
		int reserve = gun.getReserve(stack);
		int take = Math.min(gun.magazineSize(stack) - mag, reserve);
		gun.setAmmo(stack, mag + Math.max(0, take), reserve - Math.max(0, take));
		GunItem.setReloading(stack, false);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8F, 1.8F);
	}
}
