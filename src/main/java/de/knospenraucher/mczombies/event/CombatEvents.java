package de.knospenraucher.mczombies.event;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.game.GameManager;
import de.knospenraucher.mczombies.game.PlayerData;
import de.knospenraucher.mczombies.weapon.GunManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Verbindet Minecraft-Kampfereignisse mit dem Spiel:
 * Punkte für Treffer/Kills und "down" statt Tod für Spieler.
 */
public final class CombatEvents {
	private CombatEvents() {
	}

	public static void register() {
		// Spieler sterben während eines Spiels nicht, sondern gehen "down".
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			GameManager game = GameManager.get();
			if (game != null && entity instanceof ServerPlayer player && game.onPlayerLethalDamage(player)) {
				return false;
			}
			return true;
		});

		// Punkte für Treffer, die einen Zombie nicht töten.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (blocked || damageTaken <= 0 || entity.getHealth() <= 0) {
				return;
			}
			ServerPlayer attacker = zombieAttacker(entity, source);
			if (attacker != null) {
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
			} else if (isHeadshot(entity, source)) {
				points = config.pointsPerHeadshotKill;
				if (data != null) {
					data.headshots++;
				}
			} else {
				points = config.pointsPerKill;
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

	/** Kopftreffer: Schuss bzw. Projektil traf auf Höhe der Augen oder darüber. */
	private static boolean isHeadshot(LivingEntity target, DamageSource source) {
		GunManager.Shot shot = GunManager.currentShot();
		if (shot != null) {
			return shot.headshot();
		}
		Entity direct = source.getDirectEntity();
		if (!(direct instanceof Projectile projectile)) {
			return false;
		}
		return projectile.getY() >= target.getEyeY() - ZombiesConfig.get().headshotTolerance;
	}
}
