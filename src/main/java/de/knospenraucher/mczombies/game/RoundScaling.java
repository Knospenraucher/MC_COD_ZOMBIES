package de.knospenraucher.mczombies.game;

import de.knospenraucher.mczombies.config.ZombiesConfig;

/**
 * Formeln, wie Zombies pro Runde stärker werden. Alle Stellschrauben kommen aus der Config.
 */
public final class RoundScaling {
	private RoundScaling() {
	}

	/** Gesamtzahl der Zombies in einer Runde, abhängig von der Spielerzahl. */
	public static int zombieCount(int round, int players) {
		ZombiesConfig c = ZombiesConfig.get();
		double base = c.zombiesBaseCount + (double) c.zombiesPerRound * (round - 1);
		double playerFactor = 1.0 + c.zombiesExtraPlayerFactor * Math.max(0, players - 1);
		return Math.max(1, (int) Math.round(base * playerFactor));
	}

	/** Leben: erst linear, ab {@code healthLinearUntilRound} exponentiell. */
	public static double health(int round) {
		ZombiesConfig c = ZombiesConfig.get();
		int linearRounds = Math.min(round, c.healthLinearUntilRound);
		double health = c.healthBase + c.healthPerRound * (linearRounds - 1);
		if (round > c.healthLinearUntilRound) {
			health *= Math.pow(c.healthFactorAfterLinear, round - c.healthLinearUntilRound);
		}
		return Math.min(Math.max(1.0, health), Math.min(c.healthMax, 1024.0));
	}

	public static double speed(int round) {
		ZombiesConfig c = ZombiesConfig.get();
		return Math.min(c.speedMax, c.speedBase + c.speedPerRound * (round - 1));
	}

	public static double damage(int round) {
		ZombiesConfig c = ZombiesConfig.get();
		return Math.min(c.damageMax, c.damageBase + c.damagePerRound * (round - 1));
	}

	/** Ticks zwischen zwei Zombie-Spawns. */
	public static int spawnInterval(int round) {
		ZombiesConfig c = ZombiesConfig.get();
		return Math.max(c.spawnIntervalMinTicks, c.spawnIntervalTicks - c.spawnIntervalDecreasePerRound * (round - 1));
	}
}
