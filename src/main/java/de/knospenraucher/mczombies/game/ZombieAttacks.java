package de.knospenraucher.mczombies.game;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Zombie-Arten und Angriffe wie in CoD.
 * <p>
 * Es gibt Schlenderer und Läufer; der Anteil der Läufer steigt pro Runde. Den Vanilla-Nahkampf
 * blockiert {@code CombatEvents}: Stattdessen holt ein Zombie aus, sobald er nah genug ist, und
 * trifft erst nach der Ausholzeit. Wer rechtzeitig wegläuft, wird nicht getroffen. Läufer holen
 * schneller aus und schlagen öfter zu.
 */
public final class ZombieAttacks {
	/** Reichweite eines Schlags (Abstand der Mittelpunkte, in Blöcken). */
	private static final double REACH = 2.0;

	private static final Map<UUID, State> STATES = new HashMap<>();
	/** true, während ein Zombie-Schlag von uns ausgeführt wird (darf durch den Schadensfilter). */
	private static boolean attacking;

	private static final class State {
		final boolean runner;
		int windup;
		int cooldown;

		State(boolean runner) {
			this.runner = runner;
		}
	}

	private ZombieAttacks() {
	}

	/** Würfelt die Art eines neuen Zombies aus. @return true = Läufer */
	public static boolean init(Zombie zombie, int round) {
		ZombiesConfig c = ZombiesConfig.get();
		double chance = Math.min(c.runnerChanceMax, Math.max(0.0, c.runnerChancePerRound * (round - c.runnersFromRound + 1)));
		boolean runner = round >= c.runnersFromRound && zombie.getRandom().nextDouble() < chance;
		STATES.put(zombie.getUUID(), new State(runner));
		return runner;
	}

	public static double speed(boolean runner) {
		ZombiesConfig c = ZombiesConfig.get();
		return runner ? c.runnerSpeed : c.walkerSpeed;
	}

	public static void remove(LivingEntity zombie) {
		STATES.remove(zombie.getUUID());
	}

	public static void clear() {
		STATES.clear();
	}

	/** Darf dieser Schaden durch? Vanilla-Schläge von Rundenzombies werden geblockt. */
	public static boolean isOwnAttack() {
		return attacking;
	}

	/** Jeden Tick: Ausholen, Treffen, Abklingzeit. */
	public static void tick(ServerLevel level, Collection<Zombie> zombies) {
		ZombiesConfig c = ZombiesConfig.get();
		for (Zombie zombie : zombies) {
			State state = STATES.get(zombie.getUUID());
			if (state == null || !zombie.isAlive()) {
				continue;
			}
			if (state.cooldown > 0) {
				state.cooldown--;
			}
			if (!(zombie.getTarget() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
				state.windup = 0;
				continue;
			}
			boolean inReach = zombie.distanceToSqr(player) <= REACH * REACH && zombie.hasLineOfSight(player);
			if (!inReach) {
				// Weggelaufen: der angefangene Schlag geht ins Leere.
				state.windup = 0;
				continue;
			}
			if (state.cooldown > 0) {
				continue;
			}
			int windupTicks = state.runner ? c.runnerAttackWindupTicks : c.walkerAttackWindupTicks;
			if (state.windup == 0) {
				level.playSound(null, zombie.getX(), zombie.getY(), zombie.getZ(),
						SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.0F, state.runner ? 1.3F : 0.9F);
			}
			if (++state.windup < windupTicks) {
				continue;
			}
			state.windup = 0;
			state.cooldown = state.runner ? c.runnerAttackCooldownTicks : c.walkerAttackCooldownTicks;
			hit(level, zombie, player);
		}
	}

	private static void hit(ServerLevel level, Zombie zombie, ServerPlayer player) {
		attacking = true;
		try {
			player.hurtServer(level, zombie.damageSources().mobAttack(zombie), (float) ZombiesConfig.get().damageBase);
		} finally {
			attacking = false;
		}
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1.0, player.getZ(),
				3, 0.2, 0.2, 0.2, 0.1);
	}
}
