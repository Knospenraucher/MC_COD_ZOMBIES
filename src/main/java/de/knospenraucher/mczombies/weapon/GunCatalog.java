package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.config.ZombiesConfig.GunStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Alle Schusswaffen der Mod: die Waffen aus Black Ops III Zombies.
 * <p>
 * Schaden (nah und fern), Kopftreffer-Faktor, Magazin, Reserve, Feuerrate und Pack-a-Punch-Werte
 * folgen dem Original (Fandom-Wiki, NamuWiki, Fan-Blogs). Wo keine Quelle etwas hergab, sind die
 * Werte geschätzt und mit "geschätzt" markiert; die Klassenstandards in {@link #gun} (Reichweiten
 * des Schadensabfalls, Durchschlag, Kopftreffer ×4 bzw. Pistole ×3) sind ebenfalls Schätzungen.
 * Der Schaden ist 1:1 der BO3-Schaden, denn auch die Zombies haben ihr BO3-Leben (siehe ZombieHealth).
 * Feuerrate: {@code rpm(...)} rechnet Schuss pro Minute in Ticks um (1200 / RPM).
 * Die Werte sind nur Standards; die Config kann jede Waffe überschreiben.
 */
public final class GunCatalog {
	/** Schaden, der jeden Zombie sofort tötet (Wunderwaffen mit "unendlich" Schaden im Original). */
	private static final double INSTAKILL = 1.0E9;

	/** Eine Waffe: Item-Name, Anzeigename, Name nach Pack-a-Punch, Klasse und Standardwerte. */
	public record Def(String id, String name, String papName, String category, GunStats stats) {
	}

	private static final List<Def> ALL = new ArrayList<>();

	static {
		// ------------------------------------------------------------ Pistolen
		// MR6: 20 Schaden, Kopf ×3 = 60, Runde 1 (150 Leben) also 3 Kopfschüsse wie im Original.
		// Die Aufteilung 20 × 3 ist geschätzt (nur der Schaden 20 steht im NamuWiki).
		gun("mr6", "MR6", "Death & Taxes", "pistol").dmg(20, 0, 50, 0).noFalloff().head(3.0)
				.ammo(8, 80, 12, 96).ticks(4.0, false).reload(30); // PaP geschätzt
		gun("rk5", "RK5", "Rex-Kalibur 115", "pistol").dmg(100, 25, 200, 50).ammo(15, 120, 30, 180)
				.burst(3, 1.5, 9.0).reload(32); // Feuerstoß-Takt und PaP geschätzt
		gun("l_car_9", "L-CAR 9", "Flux Collider 935", "pistol").dmg(150, 30, 225, 45).head(6.0)
				.ammo(20, 160, 40, 200).rpm(722, true).reload(34);
		gun("bloodhound", "Bloodhound", "Meat Wagon", "pistol").dmg(50, 35, 1200, 1200).ammo(8, 80, 16, 50)
				.ticks(7.0, false).reload(50).upgradedExplosion(1.5); // Mindestschaden und Takt geschätzt
		gun("marshal_16", "Marshal 16", "Perun & Veles", "pistol").dmg(700, 210, 1050, 315).pellets(4, 0.08)
				.falloff(4, 12).head(1.5).pen(1, 1.0).range(20).ammo(4, 152, 12, 230).ticks(8.0, false)
				.reload(40); // zwei Pistolen (2 + 2 Schuss); Abfall und PaP-Schaden geschätzt
		gun("rift_e9", "Rift E9", "Elder Invader", "pistol").dmg(120, 60, 220, 110).ammo(12, 144, 68, 272)
				.burst(2, 2.344, 12.903).reload(34); // Schaden geschätzt

		// ------------------------------------------------------------ Maschinenpistolen
		gun("kuda", "Kuda", "Crocuta", "smg").dmg(110, 60, 150, 0).ammo(30, 210, 50, 350).rpm(722, true).reload(40);
		gun("vmp", "VMP", "The Impaler", "smg").dmg(95, 50, 140, 0).ammo(40, 240, 40, 400).rpm(909, true).reload(42);
		gun("weevil", "Weevil", "Barrage", "smg").dmg(95, 45, 175, 0).head(5.0).ammo(50, 350, 100, 500)
				.rpm(722, true).reload(52);
		gun("vesper", "Vesper", "Infernus", "smg").dmg(100, 60, 140, 0).ammo(30, 210, 60, 360).rpm(1200, true)
				.spread(0.045).reload(44);
		// Pharo: Kopf ×2,75 abgeleitet (tötet mit einem Feuerstoß bis Runde 8, mit PaP bis Runde 14).
		gun("pharo", "Pharo", "Whispering Regurgitator", "smg").dmg(80, 60, 150, 0).head(2.75)
				.ammo(40, 160, 60, 300).burst(4, 1.320, 7.284).reload(50);
		gun("razorback", "Razorback", "Gullinbursti", "smg").dmg(150, 70, 225, 0).ammo(30, 210, 60, 300)
				.rpm(600, true).spread(0.02).reload(35); // Feuerrate und PaP geschätzt
		gun("hg_40", "HG 40", "Afterburner 2.0", "smg").dmg(120, 70, 210, 0).ammo(32, 384, 64, 448).rpm(517, true)
				.reload(46);
		gun("bootlegger", "Bootlegger", "Ein Sten", "smg").dmg(150, 0, 190, 0).noFalloff().ammo(32, 192, 64, 320)
				.rpm(722, true).reload(44); // Feuerrate geschätzt
		gun("m1927", "M1927", "Untouchable", "smg").dmg(90, 60, 180, 0).ammo(50, 350, 100, 500).rpm(937, true)
				.reload(60);

		// ------------------------------------------------------------ Sturmgewehre
		gun("kn_44", "KN-44", "Anointed Avenger", "rifle").dmg(120, 70, 200, 0).head(4.0, 5.0)
				.ammo(30, 210, 50, 350).rpm(625, true).reload(48);
		gun("hvk_30", "HVK-30", "High Velocity Kicker", "rifle").dmg(110, 60, 175, 0).ammo(30, 300, 60, 480)
				.rpm(722, true).reload(44).papPen(5, 0.9); // PaP: Vollmantelgeschosse
		gun("icr_1", "ICR-1", "Illuminated Deanimator", "rifle").dmg(150, 100, 200, 0).ammo(30, 210, 45, 360)
				.rpm(600, true).spread(0.012).reload(48);
		gun("man_o_war", "Man-O-War", "Dread Armada", "rifle").dmg(210, 100, 300, 0).ammo(30, 360, 40, 440)
				.rpm(517, true).reload(50);
		gun("sheiva", "Sheiva", "Cumulus Struggle", "rifle").dmg(100, 80, 180, 0).ammo(10, 100, 30, 270)
				.ticks(3.0, false).reload(46);
		// M8A7: Kopf ×5 abgeleitet (ein PaP-Feuerstoß tötet bis Runde 25).
		gun("m8a7", "M8A7", "The Unspeakable", "rifle").dmg(120, 80, 220, 0).head(5.0).ammo(32, 256, 48, 384)
				.burst(4, 1.0, 9.0).reload(42);
		gun("peacekeeper_mk2", "Peacekeeper MK2", "Writ of Shamash", "rifle").dmg(120, 70, 170, 0)
				.ammo(32, 352, 64, 512).rpm(652, true).reload(44);

		// ------------------------------------------------------------ Schrotflinten (Schaden pro Schrotkugel)
		gun("krm_262", "KRM-262", "Dagon's Glare", "shotgun").dmg(225, 68, 775, 0).ammo(8, 48, 16, 64)
				.rpm(60, false).papRate(10.0); // Mindestschaden und PaP geschätzt
		gun("205_brecci", "205 Brecci", "Stellar Screech", "shotgun").dmg(300, 90, 550, 0).ammo(12, 108, 24, 120)
				.rpm(187, false);
		gun("haymaker_12", "Haymaker 12", "Shoeshining 100", "shotgun").dmg(280, 15, 500, 0).ammo(16, 64, 32, 160)
				.rpm(240, true); // Feuerrate und PaP geschätzt
		gun("argus", "Argus", "Ancient Messenger", "shotgun").dmg(800, 500, 2000, 0).pellets(1, 0.005)
				.falloff(12, 30).range(48).pen(3, 0.75).ammo(10, 60, 32, 96).rpm(63, false); // Flintenlaufgeschosse

		// ------------------------------------------------------------ Leichte MGs
		gun("brm", "BRM", "Blight Oblivion", "lmg").dmg(200, 80, 300, 0).ammo(75, 375, 125, 500).rpm(600, true)
				.reload(100); // Feuerrate und PaP geschätzt
		gun("dingo", "Dingo", "Dire Wolf", "lmg").dmg(250, 50, 380, 0).ammo(80, 480, 120, 600).rpm(722, true);
		gun("gorgon", "Gorgon", "Athena's Spear", "lmg").dmg(225, 130, 350, 0).ammo(50, 250, 75, 375)
				.rpm(400, true).reload(100); // Feuerrate und PaP geschätzt
		gun("48_dredge", "48 Dredge", "Trapezohedron Shard", "lmg").dmg(200, 125, 250, 0).head(4.0)
				.ammo(90, 450, 120, 720).burst(6, 1.041, 9.562).reload(66).papPen(5, 0.9); // PaP: Vollmantel
		gun("rpk", "RPK", "R115 Resonator", "lmg").dmg(180, 100, 300, 0).ammo(100, 400, 125, 500).rpm(600, true)
				.reload(80); // alle Werte geschätzt

		// ------------------------------------------------------------ Scharfschützengewehre
		// Drakon: Kopf ×4 abgeleitet (tötet mit PaP und Kopfschuss bis Runde 16).
		gun("drakon", "Drakon", "Bahamut", "sniper").dmg(500, 400, 800, 0).ammo(20, 100, 20, 140).rpm(240, false);
		gun("locus", "Locus", "Arrhythmic Dirge", "sniper").dmg(500, 0, 900, 0).noFalloff().pen(6, 0.9)
				.ammo(10, 60, 25, 100).rpm(57, false);
		gun("svg_100", "SVG-100", "Ikken Hissatsu", "sniper").dmg(800, 550, 1025, 0).ammo(6, 60, 10, 100)
				.rpm(42, false).reload(70);

		// ------------------------------------------------------------ Werfer (Werte geschätzt)
		gun("xm_53", "XM-53", "Heliacal Incandescence", "launcher").dmg(1800, 0, 3600, 0).ammo(3, 15, 6, 30)
				.ticks(20.0, false).explosion(4.0);

		// ------------------------------------------------------------ Wunderwaffen
		gun("ray_gun", "Ray Gun", "Porter's X2 Ray Gun", "wonder").dmg(1000, 0, 2000, 0).ammo(20, 160, 40, 200)
				.rpm(181, false).explosion(2.0).special("ray");
		// Ray Gun Mark II: Kopftreffer 50.000 (PaP 100.000), also Kopf ×21,74.
		gun("ray_gun_mk2", "Ray Gun Mark II", "Porter's Mark II Ray Gun", "wonder").dmg(2300, 0, 4600, 0)
				.head(21.74).ammo(21, 162, 42, 201).burst(3, 1.0, 7.2).reload(60).pen(2, 1.0).special("ray");
		gun("wunderwaffe_dg2", "Wunderwaffe DG-2", "Wunderwaffe DG-3 JZ", "wonder").dmg(INSTAKILL, 0, INSTAKILL, 0)
				.ammo(3, 15, 6, 30).rpm(60, false).reload(124).papReload(96).special("lightning");
		gun("thundergun", "Thundergun", "Zeus Cannon", "wonder").dmg(INSTAKILL, 0, INSTAKILL, 0).ammo(2, 12, 4, 24)
				.rpm(100, false).reload(40).range(12).special("thunder");
		// Annihilator: tötet laut Wiki bis Runde 24 mit einem Treffer (Runde 24: gut 3900 Leben)
		gun("annihilator", "Annihilator", "Annihilator (Pack-a-Punch)", "wonder").dmg(4000, 0, 8000, 0)
				.ammo(6, 12, 6, 24).ticks(10.0, false).reload(50).pen(5, 1.0).special("annihilate");
	}

	private GunCatalog() {
	}

	public static List<Def> all() {
		return Collections.unmodifiableList(ALL);
	}

	public static Def get(String id) {
		for (Def def : ALL) {
			if (def.id().equals(id)) {
				return def;
			}
		}
		return null;
	}

	// ---------------------------------------------------------------- Aufbau

	/** Legt eine Waffe mit den Standardwerten ihrer Klasse an (alle geschätzt). */
	private static Builder gun(String id, String name, String papName, String category) {
		GunStats s = new GunStats();
		s.magazine = 1;
		s.fireRateTicks = 1.0;
		s.reloadTicks = 50;
		s.range = 64;
		s.spread = 0.02;
		s.pellets = 1;
		s.penetration = 1;
		s.headshotMultiplier = 4.0;
		switch (category) {
			case "pistol" -> {
				s.range = 48; s.spread = 0.015; s.reloadTicks = 30; s.headshotMultiplier = 3.0;
				falloff(s, 8, 20); pen(s, 2, 0.5);
			}
			case "smg" -> {
				s.range = 40; s.spread = 0.03; s.reloadTicks = 45;
				falloff(s, 10, 25); pen(s, 2, 0.5);
			}
			case "rifle" -> {
				s.range = 64; s.spread = 0.02; s.reloadTicks = 48;
				falloff(s, 20, 45); pen(s, 3, 0.6);
			}
			case "shotgun" -> {
				s.range = 18; s.spread = 0.1; s.reloadTicks = 60; s.pellets = 4; s.headshotMultiplier = 1.5;
				falloff(s, 4, 12);
			}
			case "lmg" -> {
				s.range = 64; s.spread = 0.035; s.reloadTicks = 110;
				falloff(s, 25, 50); pen(s, 3, 0.75);
			}
			case "sniper" -> {
				s.range = 128; s.spread = 0.0; s.reloadTicks = 60;
				falloff(s, 40, 80); pen(s, 5, 0.85);
			}
			case "launcher" -> { s.range = 96; s.spread = 0.0; s.reloadTicks = 70; s.headshotMultiplier = 1.0; }
			case "wonder" -> { s.range = 64; s.spread = 0.0; s.reloadTicks = 60; s.headshotMultiplier = 1.0; }
			default -> { }
		}
		Builder builder = new Builder(s);
		ALL.add(new Def(id, name, papName, category, s));
		return builder;
	}

	private static void falloff(GunStats s, double full, double min) {
		s.maxDamageRange = full;
		s.minDamageRange = min;
	}

	private static void pen(GunStats s, int targets, double keep) {
		s.penetration = targets;
		s.penetrationDamageFactor = keep;
	}

	/** Setzt Werte direkt im GunStats-Objekt, das schon in {@link #ALL} steht. */
	private static final class Builder {
		private final GunStats s;

		private Builder(GunStats s) {
			this.s = s;
		}

		/**
		 * BO3-Schaden pro Kugel (Schrotflinten: pro Schrotkugel): nah und fern, ohne und mit Pack-a-Punch.
		 * Fern 0 = kein Abfall; PaP fern 0 = im selben Verhältnis wie ohne PaP.
		 */
		Builder dmg(double max, double min, double upgradedMax, double upgradedMin) {
			s.damage = max;
			s.damageMin = min;
			s.upgradedDamage = upgradedMax;
			s.upgradedDamageMin = upgradedMin;
			if (min <= 0) {
				noFalloff();
			}
			return this;
		}

		/** Bis {@code full} Blöcke voller Schaden, ab {@code min} Blöcken der Mindestschaden. */
		Builder falloff(double full, double min) {
			GunCatalog.falloff(s, full, min);
			return this;
		}

		Builder noFalloff() {
			GunCatalog.falloff(s, 0, 0);
			return this;
		}

		Builder ammo(int magazine, int reserve, int upgradedMagazine, int upgradedReserve) {
			s.magazine = magazine;
			s.reserve = reserve;
			s.upgradedMagazine = upgradedMagazine;
			s.upgradedReserve = upgradedReserve;
			return this;
		}

		/** Feuerrate in Schuss pro Minute. */
		Builder rpm(double rpm, boolean automatic) {
			return ticks(1200.0 / rpm, automatic);
		}

		/** Feuerrate als Ticks von Schuss zu Schuss. */
		Builder ticks(double ticks, boolean automatic) {
			s.fireRateTicks = ticks;
			s.automatic = automatic;
			return this;
		}

		/** Feuerstoß: Schüsse, Ticks zwischen den Schüssen, Ticks von Stoß zu Stoß. */
		Builder burst(int shots, double intervalTicks, double cycleTicks) {
			s.burst = shots;
			s.burstIntervalTicks = intervalTicks;
			s.fireRateTicks = cycleTicks;
			s.automatic = false;
			return this;
		}

		Builder pellets(int pellets, double spread) {
			s.pellets = pellets;
			s.spread = spread;
			return this;
		}

		Builder spread(double spread) {
			s.spread = spread;
			return this;
		}

		Builder range(double range) {
			s.range = range;
			return this;
		}

		/** Durchschlag: so viele Zombies, und so viel Schaden bleibt nach jedem. */
		Builder pen(int targets, double keep) {
			GunCatalog.pen(s, targets, keep);
			return this;
		}

		/** Durchschlag nach Pack-a-Punch. */
		Builder papPen(int targets, double keep) {
			s.upgradedPenetration = targets;
			s.upgradedPenetrationDamageFactor = keep;
			return this;
		}

		Builder head(double multiplier) {
			s.headshotMultiplier = multiplier;
			return this;
		}

		Builder head(double multiplier, double upgraded) {
			s.headshotMultiplier = multiplier;
			s.upgradedHeadshotMultiplier = upgraded;
			return this;
		}

		Builder reload(int ticks) {
			s.reloadTicks = ticks;
			return this;
		}

		Builder papRate(double ticks) {
			s.upgradedFireRateTicks = ticks;
			return this;
		}

		Builder papReload(int ticks) {
			s.upgradedReloadTicks = ticks;
			return this;
		}

		Builder explosion(double radius) {
			s.explosionRadius = radius;
			return this;
		}

		Builder upgradedExplosion(double radius) {
			s.upgradedExplosionRadius = radius;
			return this;
		}

		Builder special(String special) {
			s.special = special;
			return this;
		}
	}
}
