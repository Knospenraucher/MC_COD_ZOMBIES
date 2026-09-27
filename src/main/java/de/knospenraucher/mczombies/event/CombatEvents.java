package de.knospenraucher.mczombies.event;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.game.GameManager;
import de.knospenraucher.mczombies.game.PlayerData;
import de.knospenraucher.mczombies.game.PlayerHealth;
import de.knospenraucher.mczombies.game.ZombieAttacks;
import de.knospenraucher.mczombies.perk.Perks;
import de.knospenraucher.mczombies.weapon.GunManager;
import de.knospenraucher.mczombies.weapon.HitZone;
import de.knospenraucher.mczombies.weapon.HitZones;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Verbindet Minecraft-Kampfereignisse mit dem Spiel:
 * Punkte für Treffer/Kills (nach Trefferzone wie in BO3), Heilung der Spieler und "down" statt Tod.
 */
public final class CombatEvents {
	private CombatEvents() {
	}

	public static void register() {
		// Rundenzombies schlagen nicht per Vanilla-Nahkampf zu (der trifft sofort bei Berührung),
		// sondern mit Ausholzeit über ZombieAttacks.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				!(entity instanceof ServerPlayer) || !GameManager.isRoundZombie(source.getEntity())
						|| ZombieAttacks.isOwnAttack());

		// Spieler sterben während eines Spiels nicht, sondern gehen "down".
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			GameManager game = GameManager.get();
			if (game != null && entity instanceof ServerPlayer player && game.onPlayerLethalDamage(player)) {
				return false;
			}
			return true;
		});

		// Punkte für Treffer, die einen Zombie nicht töten; verletzte Spieler heilen erst später.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (blocked || damageTaken <= 0) {
				return;
			}
			if (entity instanceof ServerPlayer player) {
				GameManager game = GameManager.get();
				if (game != null && game.isRunning() && game.getPlayerData(player) != null) {
					PlayerHealth.onDamaged(player);
				}
				return;
			}
			if (entity.getHealth() <= 0) {
				return;
			}
			ServerPlayer attacker = zombieAttacker(entity, source);
			GunManager.Shot shot = GunManager.currentShot();
			if (attacker != null && (shot == null || shot.hitPoints())) {
				GameManager.get().addPoints(attacker, ZombiesConfig.get().pointsPerHit);
			}
		});

		// Kill-Punkte (mit Bonus für Nahkampf und Kopftreffer).
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			GameManager game = GameManager.get();
			if (game == null || !GameManager.isRoundZombie(entity)) {
				return;
			}
			game.onZombieDeath(entity);

			ServerPlayer killer = zombieAttacker(entity, source);
			if (killer == null) {
				return;
			}
			ZombiesConfig config = ZombiesConfig.get();
			PlayerData data = game.getPlayerData(killer);
			int points;
			if (isMelee(killer, source)) {
				points = config.pointsPerMeleeKill;
			} else {
				HitZone zone = killZone(entity, source, killer);
				points = switch (zone) {
					case HEAD -> config.pointsPerHeadshotKill;
					case NECK -> config.pointsPerNeckKill;
					case TORSO_UPPER, TORSO_LOWER -> config.pointsPerKill;
					case ARM, LEG -> config.pointsPerLimbKill;
					case NONE -> config.pointsPerExplosiveKill;
				};
				if (zone == HitZone.HEAD && data != null) {
					data.headshots++;
				}
			}
			if (data != null) {
				data.kills++;
			}
			game.addPoints(killer, points);
		});
	}

	/**
	 * @return der Spieler, der einen Rundenzombie getroffen hat, oder null
	 * (kein Spiel aktiv, kein Rundenzombie oder kein Spieler als Verursacher)
	 */
	private static ServerPlayer zombieAttacker(LivingEntity entity, DamageSource source) {
		GameManager game = GameManager.get();
		if (game == null || !game.isRunning() || !GameManager.isRoundZombie(entity)) {
			return null;
		}
		return source.getEntity() instanceof ServerPlayer player ? player : null;
	}

	/** Nahkampf: der Spieler hat direkt zugeschlagen (kein Projektil, kein Schuss). */
	private static boolean isMelee(ServerPlayer player, DamageSource source) {
		return GunManager.currentShot() == null && source.getDirectEntity() == player;
	}

	/** Trefferzone des tödlichen Treffers: vom Schuss, bei Pfeilen nach deren Höhe, sonst keine. */
	private static HitZone killZone(LivingEntity target, DamageSource source, ServerPlayer killer) {
		GunManager.Shot shot = GunManager.currentShot();
		if (shot != null) {
			return shot.zone();
		}
		Entity direct = source.getDirectEntity();
		if (direct instanceof Projectile projectile) {
			return HitZones.classifyHeight(target, projectile.getY(), Perks.hasDeadshot(killer));
		}
		return HitZone.NONE;
	}
}
