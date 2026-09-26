package de.knospenraucher.mczombies.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * Kleine Messer-Animation: ein Kampfmesser schwingt von rechts unten durchs Bild.
 * Gezeichnet aus Rechtecken, damit keine Textur nötig ist. Dazu der Ausfallschritt:
 * steht ein Zombie knapp außer Reichweite vor dir, macht der Spieler einen Satz nach vorne.
 */
public final class KnifeAnimation {
	/** Dauer der Animation in Millisekunden. */
	private static final long DURATION_MS = 320;
	private static final double LUNGE_MIN = 2.0;
	private static final double LUNGE_MAX = 4.0;

	private static long startMs = -1;

	private KnifeAnimation() {
	}

	public static boolean isPlaying() {
		return startMs >= 0 && System.currentTimeMillis() - startMs < DURATION_MS;
	}

	public static void start() {
		startMs = System.currentTimeMillis();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.swing(InteractionHand.MAIN_HAND);
			lunge(mc.player);
		}
	}

	/** Satz nach vorne zum nächsten Zombie im Blickfeld (wie der Ausfallschritt in CoD). */
	private static void lunge(Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		AABB area = player.getBoundingBox().inflate(LUNGE_MAX);
		player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && !(e instanceof Player))
				.stream()
				.filter(e -> {
					Vec3 offset = e.getBoundingBox().getCenter().subtract(eye);
					double distance = offset.length();
					return distance >= LUNGE_MIN && distance <= LUNGE_MAX && offset.normalize().dot(look) >= 0.82;
				})
				.min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
				.ifPresent(target -> {
					Vec3 dir = target.position().subtract(player.position());
					Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize().scale(0.7);
					player.setDeltaMovement(flat.x, Math.max(player.getDeltaMovement().y, 0.1), flat.z);
				});
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!isPlaying()) {
			return;
		}
		float t = (System.currentTimeMillis() - startMs) / (float) DURATION_MS;
		// schnell rein, kurz halten, wieder raus
		float swing = t < 0.4F ? t / 0.4F : 1.0F;
		float out = t > 0.7F ? (t - 0.7F) / 0.3F : 0.0F;

		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		float angle = (float) Math.toRadians(35 - 95 * easeOut(swing));
		float x = w * 0.72F - w * 0.18F * easeOut(swing);
		float y = h + 30 - h * 0.35F * easeOut(swing) + h * 0.5F * out;

		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().rotate(angle);
		float scale = Math.max(1.0F, h / 240.0F);
		graphics.pose().scale(scale, scale);
		drawKnife(graphics);
		graphics.pose().popMatrix();
	}

	private static float easeOut(float v) {
		return 1 - (1 - v) * (1 - v);
	}

	/** Messer senkrecht, Spitze nach oben, Griffende bei (0, 0). */
	private static void drawKnife(GuiGraphicsExtractor g) {
		int handle = 0xFF3A2A1C;
		int handleDark = 0xFF24190F;
		int guard = 0xFF55575C;
		int blade = 0xFFB9BEC6;
		int bladeEdge = 0xFFE6EAF0;
		int bladeShade = 0xFF8C9199;
		// Griff mit Rillen
		g.fill(-6, -34, 6, 0, handle);
		for (int i = -30; i < -2; i += 6) {
			g.fill(-6, i, 6, i + 2, handleDark);
		}
		g.fill(-7, -2, 7, 0, guard);
		// Parierstange
		g.fill(-12, -40, 12, -34, guard);
		// Klinge
		g.fill(-5, -96, 5, -40, blade);
		g.fill(3, -96, 5, -40, bladeEdge);
		g.fill(-5, -96, -3, -40, bladeShade);
		// Spitze
		g.fill(-3, -102, 5, -96, blade);
		g.fill(-1, -107, 5, -102, blade);
		g.fill(2, -111, 5, -107, bladeEdge);
	}
}
