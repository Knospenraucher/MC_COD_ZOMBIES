package de.knospenraucher.mczombies.weapon;

import de.knospenraucher.mczombies.MCZombies;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Zielen über Kimme und Korn (Rechtsklick halten): viel weniger Streuung, dafür langsamer laufen.
 * Den Zoom und die Visier-Grafik macht der Client.
 */
public final class Aiming {
	/** Streuung beim Zielen: Anteil der Hüftfeuer-Streuung (Schrotflinten streuen weiter). */
	private static final double SPREAD_FACTOR = 0.2;
	private static final double SHOTGUN_SPREAD_FACTOR = 0.7;
	/** Beim Zielen läuft man so viel langsamer (-35 %). */
	private static final double SLOWDOWN = -0.35;
	private static final Identifier SLOW_ID = MCZombies.id("aiming_slowdown");

	private static final Set<UUID> AIMING = new HashSet<>();

	private Aiming() {
	}

	public static void set(ServerPlayer player, boolean aiming) {
		boolean holdsGun = player.getMainHandItem().getItem() instanceof GunItem;
		aiming = aiming && holdsGun && player.isAlive() && !player.isSpectator();
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (aiming) {
			AIMING.add(player.getUUID());
			if (speed != null && !speed.hasModifier(SLOW_ID)) {
				speed.addTransientModifier(new AttributeModifier(SLOW_ID, SLOWDOWN, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		} else {
			AIMING.remove(player.getUUID());
			if (speed != null) {
				speed.removeModifier(SLOW_ID);
			}
		}
	}

	public static boolean isAiming(ServerPlayer player) {
		return AIMING.contains(player.getUUID()) && player.getMainHandItem().getItem() instanceof GunItem;
	}

	static double spreadFactor(ServerPlayer player, int pellets) {
		if (!isAiming(player)) {
			return 1.0;
		}
		return pellets > 1 ? SHOTGUN_SPREAD_FACTOR : SPREAD_FACTOR;
	}
}
