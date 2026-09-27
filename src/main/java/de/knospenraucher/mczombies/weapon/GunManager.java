package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.config.ZombiesConfig.GunStats;
import de.knospenraucher.mczombies.game.GameManager;
import de.knospenraucher.mczombies.game.PlayerHealth;
import de.knospenraucher.mczombies.network.GunActionPayload;
import de.knospenraucher.mczombies.perk.Perks;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serverseitige Waffenlogik: Feuerrate, Munition, Nachladen und Treffer.
 * <p>
 * Kugeln sind "Hitscan": ein Strahl vom Auge des Spielers in Blickrichtung,
 * der an Blöcken endet und bis zu {@code penetration} Gegner trifft.
 * Schaden wie in Black Ops III: Grundschaden je nach Entfernung (Abfall), mal Faktor der
 * Trefferzone ({@link HitZones}), mal Restanteil nach jedem durchschossenen Zombie, abgerundet.
 * Jede Schrotkugel ist ein eigener Treffer.
 * Raketen explodieren am ersten Treffer (Block oder Gegner) und schaden allen
 * Nicht-Spielern im Radius; Blöcke bleiben heil.
 */
public final class GunManager {
	/** Abstand der Leuchtspur-Partikel in Blöcken. */
	private static final double TRACER_STEP = 2.5;
	private static final int TRACER_MAX_POINTS = 12;

	/** Toleranz beim Vergleich von Kommazahl-Ticks. */
	private static final double EPSILON = 1.0E-6;
	/** Höchstens so viele Schüsse pro Tick (sehr schnelle Waffen mit Double Tap). */
	private static final int MAX_SHOTS_PER_TICK = 3;
	/** Explosionen: am Rand des Radius bleibt dieser Anteil des Schadens. */
	private static final double EXPLOSION_EDGE_FACTOR = 0.4;

	/** Frühester Tick (mit Nachkommastellen) für den nächsten Schuss je Spieler. */
	private static final Map<UUID, Double> nextShot = new HashMap<>();
	/** Laufende Nachladevorgänge je Spieler. */
	private static final Map<UUID, Reload> reloads = new HashMap<>();
	/** Laufende Feuerstöße je Spieler. */
	private static final Map<UUID, Burst> bursts = new HashMap<>();
	private static long ticks;

	/** Gerade verarbeiteter Schuss; {@code CombatEvents} liest ihn für Kill-Punkte. */
	private static Shot currentShot;

	/**
	 * Ein Treffer durch eine Waffe: wer geschossen hat, wo der Zombie getroffen wurde und ob der
	 * Treffer Punkte bringt (bei Schrot ohne pointsPerPellet nur die erste Kugel pro Zombie).
	 */
	public record Shot(ServerPlayer shooter, HitZone zone, boolean hitPoints) {
		public boolean headshot() {
			return zone == HitZone.HEAD;
		}
	}

	private record Reload(int slot, long endTick) {
	}

	/** Noch ausstehende Schüsse eines Feuerstoßes. */
	private record Burst(int slot, int remaining, double nextTick) {
	}

	/** Ein Gegner auf der Schusslinie: wo die Kugel die (etwas vergrößerte) Hitbox betritt. */
	record BulletHit(LivingEntity target, Vec3 point, double distance, AABB box) {
	}

	private GunManager() {
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(GunActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			context.server().execute(() -> {
				switch (payload.action()) {
					case GunActionPayload.RELOAD -> startReload(player, true);
					case GunActionPayload.MELEE -> KnifeMelee.stab(player, false);
					case GunActionPayload.MELEE_LUNGE -> KnifeMelee.stab(player, true);
					case GunActionPayload.AIM_START -> Aiming.set(player, true);
					case GunActionPayload.AIM_STOP -> Aiming.set(player, false);
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
		double next = nextShot.getOrDefault(player.getUUID(), Double.NEGATIVE_INFINITY);
		if (ticks + EPSILON < next) {
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
				nextShot.put(player.getUUID(), ticks + 10.0);
				level.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.6F);
				player.sendSystemMessage(Component.literal("Keine Munition mehr!").withStyle(ChatFormatting.RED), true);
			}
			return;
		}

		int burst = Math.max(1, stats.burst);
		// Double Tap: kürzere Pause zwischen den Schüssen
		double factor = Perks.fireIntervalFactor(player);
		double interval = gun.burstInterval() * factor;
		double cycle = gun.fireInterval(stack) * factor;
		if (burst > 1) {
			// Der nächste Feuerstoß beginnt frühestens nach dem letzten Schuss des vorigen.
			cycle = Math.max(cycle, (burst - 1) * interval + 1.0);
		}
		// Kommazahl-Takt: wer durchgehend feuert, behält den Rest (722 Schuss/min = 6 Schuss in 10 Ticks).
		if (ticks - next >= 1.0) {
			next = ticks;
		}
		if (burst > 1) {
			nextShot.put(player.getUUID(), next + cycle);
			fireOnce(level, player, stack, gun);
			bursts.put(player.getUUID(), new Burst(player.getInventory().getSelectedSlot(), burst - 1, next + interval));
			return;
		}
		int shots = 0;
		do {
			next += cycle;
			fireOnce(level, player, stack, gun);
			shots++;
		} while (stats.automatic && shots < MAX_SHOTS_PER_TICK && ticks + EPSILON >= next
				&& gun.getMag(stack) > 0 && !reloads.containsKey(player.getUUID()));
		nextShot.put(player.getUUID(), next);
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
					// Den Rundenbonus bekommen in BO3 nur Geschosse (Werfer, Ray Gun), nicht der Meat Wagon.
					boolean roundBonus = special.equals("ray") || "launcher".equals(gun.category());
					fireRocket(level, player, stats, damage, radius, special.equals("ray"), upgraded, roundBonus);
				} else {
					fireBullets(level, player, stack, gun, special.equals("annihilate"));
				}
			}
		}

		if (mag - 1 == 0 && reserve > 0) {
			bursts.remove(player.getUUID());
			startReload(player, false);
		}
	}

	private static void fireBullets(ServerLevel level, ServerPlayer player, ItemStack stack, GunItem gun, boolean annihilate) {
		GunStats stats = gun.stats();
		Vec3 eye = player.getEyePosition();
		// Double Tap 2.0: jede Kugel zählt doppelt
		double perkFactor = Perks.bulletDamageFactor(player);
		boolean deadshot = Perks.hasDeadshot(player);
		boolean pointsPerPellet = ZombiesConfig.get().pointsPerPellet;
		// Deadshot: weniger Streuung aus der Hüfte
		double spreadFactor = Aiming.isAiming(player) ? 1.0 : Perks.hipSpreadFactor(player);
		int pellets = Math.max(1, stats.pellets);
		int penetration = gun.penetration(stack);
		double keep = gun.penetrationFactor(stack);
		// Ohne pointsPerPellet gibt es die Treffer-Punkte nur für die erste Kugel pro Zombie.
		Set<LivingEntity> awarded = new HashSet<>();
		for (int i = 0; i < pellets; i++) {
			Vec3 dir = spread(player.getLookAngle(), stats.spread * Aiming.spreadFactor(player, pellets) * spreadFactor, player.getRandom());
			Vec3 end = blockLimitedEnd(level, player, eye, dir, stats.range);
			List<BulletHit> hits = traceEntities(level, player, eye, end);
			Vec3 tracerEnd = end;
			int passed = 0;
			for (BulletHit hit : hits) {
				if (passed >= penetration) {
					break;
				}
				LivingEntity target = hit.target();
				if (!target.isAlive()) {
					// Schon von einer früheren Kugel dieses Schusses getötet: fliegt weiter.
					continue;
				}
				HitZone zone = HitZones.classify(target, eye, dir, hit.box(), hit.distance(), end, deadshot);
				double amount = gun.damageAt(stack, hit.distance()) * perkFactor
						* gun.zoneMultiplier(stack, zone) * Math.pow(keep, passed);
				// BO3 rundet den Schaden ab.
				float finalDamage = (float) Math.floor(amount);
				passed++;
				tracerEnd = hit.point();
				level.sendParticles(zone == HitZone.HEAD ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
						hit.point().x, hit.point().y, hit.point().z, 4, 0.05, 0.05, 0.05, 0.1);
				if (annihilate) {
					// Annihilator: der Zombie zerplatzt.
					AABB box = target.getBoundingBox();
					level.sendParticles(ParticleTypes.CRIMSON_SPORE, box.getCenter().x, box.getCenter().y, box.getCenter().z,
							30, 0.3, 0.5, 0.3, 0.1);
				}
				if (finalDamage > 0) {
					hurt(level, player, target, finalDamage, zone, pointsPerPellet || awarded.add(target));
				}
			}
			if (hits.isEmpty()) {
				level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.02, 0.02, 0.02, 0.0);
			}
			tracer(level, eye, tracerEnd, pellets > 1);
		}
	}

	private static void fireRocket(ServerLevel level, ServerPlayer player, GunStats stats, float damage, double radius,
			boolean ray, boolean upgraded, boolean roundBonus) {
		Vec3 eye = player.getEyePosition();
		Vec3 dir = spread(player.getLookAngle(), stats.spread * Aiming.spreadFactor(player, 1), player.getRandom());
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

		LivingEntity direct = hits.isEmpty() ? null : hits.get(0).target();
		ZombiesConfig config = ZombiesConfig.get();
		GameManager game = GameManager.get();
		int round = game != null && game.isRunning() ? Math.max(1, game.getRound()) : 1;
		AABB area = new AABB(impact.x - radius, impact.y - radius, impact.z - radius,
				impact.x + radius, impact.y + radius, impact.z + radius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> isTarget(e, player))) {
			// Gemessen bis zum nächsten Punkt der Hitbox, nicht bis zur Mitte.
			double distance = target == direct ? 0.0 : distanceToBox(impact, target.getBoundingBox());
			if (distance > radius) {
				continue;
			}
			// Voller Schaden im Zentrum, 40 % am Rand; der direkt getroffene Zombie bekommt alles.
			double amount = explosionDamage(damage, distance, radius);
			if (config.explosiveRoundBonus && roundBonus) {
				// BO3: Geschosse mit Explosion machen zusätzlich Runde × Zufall 0 bis 99.
				amount += round * player.getRandom().nextInt(100);
			}
			hurt(level, player, target, (float) Math.floor(amount), HitZone.NONE, true);
		}
		if (config.explosiveSelfDamage) {
			double self = distanceToBox(impact, player.getBoundingBox());
			if (self <= radius) {
				PlayerHealth.explosiveSelfDamage(level, player, explosionDamage(damage, self, radius), ray);
			}
		}
	}

	/** Explosionsschaden auf diese Entfernung: 100 % im Zentrum, 40 % am Rand. */
	private static double explosionDamage(double damage, double distance, double radius) {
		return damage * (1.0 - (1.0 - EXPLOSION_EDGE_FACTOR) * Math.min(1.0, distance / radius));
	}

	/** Abstand eines Punkts zum nächsten Punkt einer Box (0, wenn er drin liegt). */
	private static double distanceToBox(Vec3 point, AABB box) {
		double x = Math.max(box.minX, Math.min(point.x, box.maxX));
		double y = Math.max(box.minY, Math.min(point.y, box.maxY));
		double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
		return point.distanceTo(new Vec3(x, y, z));
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
			AABB box = target.getBoundingBox().inflate(0.1);
			if (box.contains(eye)) {
				// Der Zombie steht direkt im Spieler: Treffer aus nächster Nähe, aber nur, wenn er
				// vor und nicht hinter der Schussrichtung steht.
				Vec3 dir = end.subtract(eye);
				if ((target.getX() - eye.x) * dir.x + (target.getZ() - eye.z) * dir.z >= 0) {
					hits.add(new BulletHit(target, eye, 0.0, box));
				}
				continue;
			}
			box.clip(eye, end).ifPresent(point -> hits.add(new BulletHit(target, point, point.distanceTo(eye), box)));
		}
		hits.sort(Comparator.comparingDouble(BulletHit::distance));
		return hits;
	}

	/** Spieler (Mitspieler) werden nie getroffen. */
	public static boolean isTarget(LivingEntity entity, ServerPlayer shooter) {
		return entity != shooter && entity.isAlive() && !(entity instanceof Player);
	}

	private static Vec3 spread(Vec3 look, double spread, RandomSource random) {
		if (spread <= 0) {
			return look;
		}
		return look.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread)
				.normalize();
	}

	/**
	 * Schaden als Spielerangriff, damit Punkte und Kills dem Schützen gutgeschrieben werden.
	 *
	 * @param amount    BO3-Schaden (schon abgerundet)
	 * @param zone      Trefferzone (NONE bei Explosionen und Sonderwirkungen)
	 * @param hitPoints ob ein nicht tödlicher Treffer Punkte bringt
	 */
	public static void hurt(ServerLevel level, ServerPlayer shooter, LivingEntity target, float amount, HitZone zone,
			boolean hitPoints) {
		Shot previous = currentShot;
		currentShot = new Shot(shooter, zone, hitPoints);
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
		// Speed Cola: halbe Nachladezeit
		long reloadTicks = Math.max(1, Math.round(gun.reloadTicks(stack) * Perks.reloadFactor(player)));
		reloads.put(player.getUUID(), new Reload(slot, ticks + reloadTicks));
		Perks.onReloadStart(player, gun.getMag(stack), gun.magazineSize(stack));
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
			if (ticks + EPSILON < burst.nextTick()) {
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
			double interval = gun.burstInterval() * Perks.fireIntervalFactor(player);
			// Mehrere Schüsse in einem Tick, falls der Takt kürzer als ein Tick ist.
			int remaining = burst.remaining();
			double next = burst.nextTick();
			int now = 0;
			while (remaining > 0 && now < MAX_SHOTS_PER_TICK && ticks + EPSILON >= next) {
				remaining--;
				next += interval;
				now++;
			}
			if (remaining <= 0) {
				it.remove();
			} else {
				entry.setValue(new Burst(burst.slot(), remaining, next));
			}
			// Erst nach dem Durchlauf schießen: fireOnce kann den Feuerstoß beenden (Magazin leer).
			int count = now;
			shots.add(() -> {
				for (int i = 0; i < count; i++) {
					fireOnce((ServerLevel) player.level(), player, stack, gun);
				}
			});
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
