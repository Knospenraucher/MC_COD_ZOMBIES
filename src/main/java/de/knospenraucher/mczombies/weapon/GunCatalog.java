package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.config.ZombiesConfig.GunStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Alle Schusswaffen der Mod: die Waffen aus Black Ops III Zombies.
 * <p>
 * Schaden, Magazin, Reserve, Feuerrate und Pack-a-Punch-Werte folgen dem Original (Fandom-Wiki,
 * NamuWiki); wo keine Quelle etwas hergab, sind die Werte geschätzt (mit "geschätzt" markiert).
 * Der Schaden ist 1:1 der BO3-Schaden, denn auch die Zombies haben ihr BO3-Leben (siehe ZombieHealth).
 * Nachladezeiten stehen im Original kaum irgendwo und sind je Klasse geschätzt.
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
		gun("mr6", "MR6", "Death & Taxes", "pistol").dmg(50, 150).ammo(8, 80, 12, 96).rate(4, false); // Schaden geschätzt
		gun("rk5", "RK5", "Rex-Kalibur 115", "pistol").dmg(100, 200).ammo(15, 120, 30, 180).rate(4, false);
		gun("l_car_9", "L-CAR 9", "Flux Collider 935", "pistol").dmg(150, 225).ammo(20, 160, 40, 200).rate(2, true);
		gun("bloodhound", "Bloodhound", "Meat Wagon", "pistol").dmg(250, 1200).ammo(8, 72, 8, 50).rate(7, false)
				.head(3.0).upgradedExplosion(1.5);
		gun("marshal_16", "Marshal 16", "Perun & Veles", "pistol").dmg(700, 775).ammo(2, 152, 6, 230).rate(8, false)
				.pellets(4, 0.08).range(20);
		gun("rift_e9", "Rift E9", "Elder Invader", "pistol").dmg(120, 220).ammo(12, 144, 68, 272).rate(6, false)
				.burst(2, 2); // Schaden geschätzt

		// ------------------------------------------------------------ Maschinenpistolen
		gun("kuda", "Kuda", "Crocuta", "smg").dmg(110, 150).ammo(30, 210, 50, 350).rate(2, true);
		gun("vmp", "VMP", "The Impaler", "smg").dmg(95, 140).ammo(40, 240, 40, 400).rate(1, true);
		gun("weevil", "Weevil", "Barrage", "smg").dmg(95, 175).ammo(50, 350, 100, 500).rate(2, true);
		gun("vesper", "Vesper", "Infernus", "smg").dmg(100, 140).ammo(30, 210, 60, 360).rate(1, true).spread(0.045);
		gun("pharo", "Pharo", "Whispering Regurgitator", "smg").dmg(80, 150).ammo(40, 160, 60, 300).rate(7, false)
				.burst(4, 1).head(2.5);
		gun("razorback", "Razorback", "Gullinbursti", "smg").dmg(150, 225).ammo(30, 210, 60, 300).rate(3, true)
				.spread(0.02).reload(35);
		gun("hg_40", "HG 40", "Afterburner 2.0", "smg").dmg(120, 210).ammo(32, 384, 64, 448).rate(2, true);
		gun("bootlegger", "Bootlegger", "Ein Sten", "smg").dmg(150, 190).ammo(32, 192, 64, 320).rate(2, true);
		gun("m1927", "M1927", "Untouchable", "smg").dmg(90, 180).ammo(50, 350, 100, 500).rate(1, true);

		// ------------------------------------------------------------ Sturmgewehre
		gun("kn_44", "KN-44", "Anointed Avenger", "rifle").dmg(120, 200).ammo(30, 210, 50, 350).rate(2, true);
		gun("hvk_30", "HVK-30", "High Velocity Kicker", "rifle").dmg(110, 175).ammo(30, 300, 60, 480).rate(2, true);
		gun("icr_1", "ICR-1", "Illuminated Deanimator", "rifle").dmg(150, 200).ammo(30, 210, 45, 360).rate(2, true)
				.spread(0.012);
		gun("man_o_war", "Man-O-War", "Dread Armada", "rifle").dmg(210, 300).ammo(30, 360, 40, 440).rate(2, true)
				.pen(2);
		gun("sheiva", "Sheiva", "Cumulus Struggle", "rifle").dmg(100, 180).ammo(10, 100, 30, 270).rate(3, false);
		gun("m8a7", "M8A7", "The Unspeakable", "rifle").dmg(120, 220).ammo(32, 256, 48, 384).rate(6, false)
				.burst(4, 1).head(2.5);
		gun("peacekeeper_mk2", "Peacekeeper MK2", "Writ of Shamash", "rifle").dmg(120, 170).ammo(32, 352, 64, 512)
				.rate(2, true);

		// ------------------------------------------------------------ Schrotflinten (Schaden pro Schrotkugel)
		gun("krm_262", "KRM-262", "Dagon's Glare", "shotgun").dmg(225, 775).ammo(8, 48, 16, 64).rate(20, false);
		gun("205_brecci", "205 Brecci", "Stellar Screech", "shotgun").dmg(300, 550).ammo(12, 108, 24, 120).rate(6, false);
		gun("haymaker_12", "Haymaker 12", "Shoeshining 100", "shotgun").dmg(280, 500).ammo(16, 64, 32, 160)
				.rate(6, true);
		gun("argus", "Argus", "Ancient Messenger", "shotgun").dmg(800, 2000).ammo(10, 60, 32, 96).rate(19, false)
				.pellets(1, 0.005).range(48).pen(3); // Flintenlaufgeschosse statt Schrot

		// ------------------------------------------------------------ Leichte MGs
		gun("brm", "BRM", "Blight Oblivion", "lmg").dmg(200, 300).ammo(75, 375, 125, 500).rate(2, true);
		gun("dingo", "Dingo", "Dire Wolf", "lmg").dmg(250, 380).ammo(80, 480, 120, 600).rate(2, true);
		gun("gorgon", "Gorgon", "Athena's Spear", "lmg").dmg(225, 350).ammo(50, 250, 75, 375).rate(3, true)
				.pen(3);
		gun("48_dredge", "48 Dredge", "Trapezohedron Shard", "lmg").dmg(200, 250).ammo(90, 450, 120, 720)
				.rate(10, false).burst(6, 1);
		gun("rpk", "RPK", "R115 Resonator", "lmg").dmg(180, 300).ammo(100, 400, 125, 500).rate(2, true); // geschätzt

		// ------------------------------------------------------------ Scharfschützengewehre
		gun("drakon", "Drakon", "Bahamut", "sniper").dmg(500, 800).ammo(20, 100, 20, 140).rate(5, false);
		gun("locus", "Locus", "Arrhythmic Dirge", "sniper").dmg(500, 900).ammo(10, 60, 25, 100).rate(21, false)
				.pen(6);
		gun("svg_100", "SVG-100", "Ikken Hissatsu", "sniper").dmg(800, 1025).ammo(6, 60, 10, 100).rate(28, false);

		// ------------------------------------------------------------ Werfer (Werte geschätzt)
		gun("xm_53", "XM-53", "Heliacal Incandescence", "launcher").dmg(1800, 3600).ammo(3, 15, 6, 30)
				.rate(20, false).explosion(4.0);

		// ------------------------------------------------------------ Wunderwaffen
		gun("ray_gun", "Ray Gun", "Porter's X2 Ray Gun", "wonder").dmg(1000, 2000).ammo(20, 160, 40, 200)
				.rate(5, false).explosion(2.0).special("ray");
		gun("ray_gun_mk2", "Ray Gun Mark II", "Porter's Mark II Ray Gun", "wonder").dmg(2300, 4600)
				.ammo(21, 162, 42, 201).rate(7, false).burst(3, 1).reload(60).pen(2).special("ray");
		gun("wunderwaffe_dg2", "Wunderwaffe DG-2", "Wunderwaffe DG-3 JZ", "wonder").raw(INSTAKILL, INSTAKILL)
				.ammo(3, 15, 6, 30).rate(20, false).reload(124).special("lightning");
		gun("thundergun", "Thundergun", "Zeus Cannon", "wonder").raw(INSTAKILL, INSTAKILL).ammo(2, 12, 4, 24)
				.rate(12, false).reload(40).range(12).special("thunder");
		// Annihilator: tötet laut Wiki bis Runde 24 mit einem Treffer (Runde 24: gut 3900 Leben)
		gun("annihilator", "Annihilator", "Annihilator (Pack-a-Punch)", "wonder").raw(4000, 8000)
				.ammo(6, 12, 6, 24).rate(10, false).reload(50).pen(5).special("annihilate");
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

	private static Builder gun(String id, String name, String papName, String category) {
		GunStats s = new GunStats(0, 1, 0, 1, 50, 64, 0.02, 1, false, 1, 0, 2.0);
		switch (category) {
			case "pistol" -> { s.range = 48; s.spread = 0.015; s.reloadTicks = 30; }
			case "smg" -> { s.range = 40; s.spread = 0.03; s.reloadTicks = 45; s.headshotMultiplier = 1.5; }
			case "shotgun" -> { s.range = 18; s.spread = 0.1; s.reloadTicks = 60; s.pellets = 6; s.headshotMultiplier = 1.5; }
			case "lmg" -> { s.range = 64; s.spread = 0.035; s.reloadTicks = 110; s.penetration = 2; s.headshotMultiplier = 1.5; }
			case "sniper" -> { s.range = 128; s.spread = 0.0; s.reloadTicks = 60; s.penetration = 4; s.headshotMultiplier = 3.0; }
			case "launcher" -> { s.range = 96; s.spread = 0.0; s.reloadTicks = 70; s.headshotMultiplier = 1.0; }
			case "wonder" -> { s.range = 64; s.spread = 0.0; s.reloadTicks = 60; s.headshotMultiplier = 1.0; }
			default -> { }
		}
		Builder builder = new Builder(s);
		ALL.add(new Def(id, name, papName, category, s));
		return builder;
	}

	/** Setzt Werte direkt im GunStats-Objekt, das schon in {@link #ALL} steht. */
	private static final class Builder {
		private final GunStats s;

		private Builder(GunStats s) {
			this.s = s;
		}

		/** BO3-Schaden pro Kugel (Schrotflinten: pro Schrotkugel), ohne und mit Pack-a-Punch. */
		Builder dmg(double bo3, double bo3Upgraded) {
			return raw(bo3, bo3Upgraded);
		}

		/** Schaden, der nicht aus einer Wiki-Tabelle stammt (Wunderwaffen). */
		Builder raw(double damage, double upgraded) {
			s.damage = damage;
			s.upgradedDamage = upgraded;
			return this;
		}

		Builder ammo(int magazine, int reserve, int upgradedMagazine, int upgradedReserve) {
			s.magazine = magazine;
			s.reserve = reserve;
			s.upgradedMagazine = upgradedMagazine;
			s.upgradedReserve = upgradedReserve;
			return this;
		}

		Builder rate(int ticks, boolean automatic) {
			s.fireRateTicks = ticks;
			s.automatic = automatic;
			return this;
		}

		Builder burst(int shots, int intervalTicks) {
			s.burst = shots;
			s.burstIntervalTicks = intervalTicks;
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

		Builder pen(int penetration) {
			s.penetration = penetration;
			return this;
		}

		Builder head(double multiplier) {
			s.headshotMultiplier = multiplier;
			return this;
		}

		Builder reload(int ticks) {
			s.reloadTicks = ticks;
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
