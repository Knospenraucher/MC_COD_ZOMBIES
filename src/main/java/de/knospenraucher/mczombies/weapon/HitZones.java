package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.config.ZombiesConfig.HitZoneConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bestimmt die Trefferzone eines Schusses am Zombie.
 * <p>
 * Gemessen wird nicht dort, wo die Kugel die Hitbox betritt (von oben geschossen wäre das immer
 * die Oberseite, also "Kopf"), sondern an der Stelle, an der die Kugel der senkrechten Körperachse
 * am nächsten kommt. Die Höhe dort ergibt Kopf, Hals, Oberkörper, Bauch oder Beine; der seitliche
 * Abstand von der Achse erkennt die nach vorne gestreckten Arme.
 */
public final class HitZones {
	private HitZones() {
	}

	/**
	 * @param eye      Startpunkt der Kugel
	 * @param dir      normierte Flugrichtung
	 * @param box      Hitbox, gegen die getestet wurde
	 * @param tIn      Entfernung, bei der die Kugel die Hitbox betritt
	 * @param end      Endpunkt der Kugel (Reichweite oder Block)
	 * @param deadshot der Schütze hat Deadshot Daiquiri
	 */
	public static HitZone classify(LivingEntity target, Vec3 eye, Vec3 dir, AABB box, double tIn, Vec3 end, boolean deadshot) {
		double length = end.distanceTo(eye);
		double tOut = box.contains(end) ? length
				: box.clip(end, eye).map(p -> p.distanceTo(eye)).orElse(tIn);
		double horizontal = dir.x * dir.x + dir.z * dir.z;
		double tc = horizontal < 1.0E-6 ? tIn
				: ((target.getX() - eye.x) * dir.x + (target.getZ() - eye.z) * dir.z) / horizontal;
		tc = Math.max(tIn, Math.min(tOut, tc));
		Vec3 p = eye.add(dir.scale(tc));
		double scale = scale(target);
		double y = (p.y - target.getY()) / scale;
		double lateral = Math.hypot(p.x - target.getX(), p.z - target.getZ()) / scale;
		return zone(y, lateral, deadshot);
	}

	/** Nur nach Höhe, z. B. für Pfeile. */
	public static HitZone classifyHeight(LivingEntity target, double worldY, boolean deadshot) {
		return zone((worldY - target.getY()) / scale(target), 0.0, deadshot);
	}

	private static double scale(LivingEntity target) {
		HitZoneConfig c = ZombiesConfig.get().hitZones;
		double reference = c.referenceHeight > 0 ? c.referenceHeight : 1.95;
		return target.getBbHeight() > 0 ? target.getBbHeight() / reference : 1.0;
	}

	static HitZone zone(double y, double lateral, boolean deadshot) {
		HitZoneConfig c = ZombiesConfig.get().hitZones;
		boolean center = lateral < c.armLateral;
		if (y >= c.headMinY || (deadshot && center && y >= c.headMinY - c.deadshotHeadExtension)) {
			return HitZone.HEAD;
		}
		if (y >= c.armMinY && !center) {
			return HitZone.ARM;
		}
		if (y >= c.neckMinY) {
			return HitZone.NECK;
		}
		if (y >= c.torsoUpperMinY) {
			return HitZone.TORSO_UPPER;
		}
		if (y >= c.torsoLowerMinY) {
			return HitZone.TORSO_LOWER;
		}
		return HitZone.LEG;
	}
}
