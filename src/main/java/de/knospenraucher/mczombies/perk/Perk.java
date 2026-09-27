package de.knospenraucher.mczombies.perk;

/**
 * Die Perks aus Black Ops III mit Originalpreisen. Wirkungen stehen in {@link Perks}.
 * Die Farbe (ARGB) und das Kürzel braucht das HUD, der Block den Automaten in der Map.
 */
public enum Perk {
	JUGGERNOG("juggernog", "Juggernog", 2500, 0xFFD02828, "JUG", "Mehr Leben: 250 statt 100 (5 statt 2 Treffer)"),
	QUICK_REVIVE("quick_revive", "Quick Revive", 1500, 0xFF40A8E0, "QR", "Allein: 3 Selbstwiederbelebungen (500 Punkte)"),
	SPEED_COLA("speed_cola", "Speed Cola", 3000, 0xFF30C040, "SC", "Doppelt so schnell nachladen"),
	DOUBLE_TAP("double_tap", "Double Tap Root Beer", 2000, 0xFFE0A020, "DT", "Schneller feuern, jede Kugel trifft doppelt"),
	STAMIN_UP("stamin_up", "Stamin-Up", 2000, 0xFFF0D040, "SU", "Schneller laufen"),
	MULE_KICK("mule_kick", "Mule Kick", 4000, 0xFF508030, "MK", "Dritte Waffe tragen"),
	DEADSHOT("deadshot", "Deadshot Daiquiri", 1500, 0xFF505058, "DS", "Weniger Streuung aus der Hüfte, Kopftreffer leichter"),
	WIDOWS_WINE("widows_wine", "Widow's Wine", 4000, 0xFF9040C0, "WW", "Wird man getroffen, bleiben Zombies in Netzen hängen"),
	ELECTRIC_CHERRY("electric_cherry", "Electric Cherry", 2000, 0xFF60D0FF, "EC", "Nachladen schockt Zombies in der Nähe");

	private final String id;
	private final String displayName;
	private final int defaultPrice;
	private final int color;
	private final String shortName;
	private final String description;

	Perk(String id, String displayName, int defaultPrice, int color, String shortName, String description) {
		this.id = id;
		this.displayName = displayName;
		this.defaultPrice = defaultPrice;
		this.color = color;
		this.shortName = shortName;
		this.description = description;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return displayName;
	}

	public int defaultPrice() {
		return defaultPrice;
	}

	public int color() {
		return color;
	}

	public String shortName() {
		return shortName;
	}

	public String description() {
		return description;
	}

	/** @return der Perk mit dieser ID, oder null */
	public static Perk byId(String id) {
		for (Perk perk : values()) {
			if (perk.id.equalsIgnoreCase(id)) {
				return perk;
			}
		}
		return null;
	}
}
