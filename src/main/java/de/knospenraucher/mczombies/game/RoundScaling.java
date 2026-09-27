package de.knospenraucher.mczombies.game;

import de.knospenraucher.mczombies.config.ZombiesConfig;

import java.util.List;

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

	/**
	 * Leben in BO3-Einheiten, gerechnet wie im Original ({@code ai_calculate_health}): Runde 1 hat 150,
	 * bis Runde 9 kommen je 100 dazu (950), danach je 10 %, bei jedem Schritt abgerundet.
	 * Würde die nächste Runde {@code healthMax} überschreiten, bleibt das Leben stehen (BO3: ab
	 * Runde 162 immer 2.035.642.980). Die ersten Runden kann {@code healthEarlyRounds} festlegen.
	 */
	public static double health(int round) {
		ZombiesConfig c = ZombiesConfig.get();
		List<Double> early = c.healthEarlyRounds != null && !c.healthEarlyRounds.isEmpty()
				? c.healthEarlyRounds : List.of(c.healthBase);
		long health = (long) Math.floor(early.get(0));
		for (int r = 2; r <= round; r++) {
			long next;
			if (r <= early.size()) {
				next = (long) Math.floor(early.get(r - 1));
			} else if (r <= c.healthLinearUntilRound) {
				next = health + (long) Math.floor(c.healthPerRound);
			} else {
				next = health + (long) Math.floor(health * (c.healthFactorAfterLinear - 1.0) + 1.0E-7);
			}
			if (next > c.healthMax) {
				break;
			}
			health = next;
		}
		return Math.max(1L, health);
	}

	public static double speed(int round) {
		ZombiesConfig c = ZombiesConfig.get();
		return Math.min(c.speedMax, c.speedBase + c.speedPerRound * (round - 1));
	}

	/** Schaden eines Zombie-Schlags in BO3-Einheiten (Standard: immer 50). */
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
