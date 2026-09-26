package de.knospenraucher.mczombies.client;

import de.knospenraucher.mczombies.weapon.GunItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Visier beim Zielen: Kimme und Korn in der Bildmitte, bei Scharfschützengewehren ein Zielfernrohr
 * (schwarzer Rand mit Fadenkreuz).
 */
public final class AimOverlay {
	private static final int BLACK = 0xFF000000;
	private static final int SIGHT = 0xFF1C1C1C;
	private static final int DOT = 0xFFFF3030;

	private AimOverlay() {
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem gun)) {
			return;
		}
		float progress = GunInput.aimProgress(deltaTracker.getGameTimeDeltaPartialTick(false));
		if (progress <= 0.0F) {
			return;
		}
		if ("sniper".equals(gun.category())) {
			// Zielfernrohr erst, wenn die Waffe fast oben ist
			if (progress > 0.85F) {
				scope(g, g.guiWidth() / 2, g.guiHeight() / 2);
			}
			return;
		}
		// Rein ins Visier: die Waffe kommt von unten rechts (Hüfte) in die Bildmitte, raus umgekehrt.
		float away = 1.0F - progress;
		int cx = g.guiWidth() / 2 + Math.round(away * g.guiWidth() * 0.22F);
		int cy = g.guiHeight() / 2 + Math.round(away * g.guiHeight() * 0.6F);
		if (gun.key().endsWith("mr6")) {
			mr6(g, cx, cy);
		} else {
			ironSights(g, cx, cy);
		}
	}

	/** Korn (Pfosten mit rotem Punkt) mittig, Kimme (zwei Blöcke mit Lücke) darunter. */
	private static void ironSights(GuiGraphicsExtractor g, int cx, int cy) {
		// Korn
		g.fill(cx - 1, cy + 1, cx + 2, cy + 22, SIGHT);
		g.fill(cx - 1, cy - 1, cx + 2, cy + 1, DOT);
		g.fill(cx - 6, cy + 20, cx + 7, cy + 24, SIGHT);
		// Kimme
		g.fill(cx - 40, cy + 10, cx - 5, cy + 40, SIGHT);
		g.fill(cx + 6, cy + 10, cx + 41, cy + 40, SIGHT);
		g.fill(cx - 40, cy + 40, cx + 41, cy + 46, SIGHT);
	}

	/**
	 * MR6 über Kimme und Korn wie in BO3: kantiges Schlittenende mit Kimme, gelbem Korn und grüner
	 * Munitionsanzeige, darunter Griffstück und Griff bis zum Bildrand. Die Einheit u wächst mit der
	 * Bildschirmhöhe.
	 */
	private static void mr6(GuiGraphicsExtractor g, int cx, int cy) {
		float u = g.guiHeight() / 100.0F;
		float bottom = cy + 90 * u;
		int body = 0xFF2A2A2E;
		int edge = 0xFF48484E;
		int shadow = 0xFF18181A;
		int grip = 0xFF232326;

		// Korn: dunkler Pfosten mit gelbem Strich, sitzt in der Lücke der Kimme
		rect(g, cx - u, cy - u, cx + u, cy + 5 * u, shadow);
		g.fill(cx, cy - (int) u, cx + Math.max(1, (int) (0.5F * u)), cy + (int) (2 * u), 0xFFEFF230);

		// Kimme: zwei Ohren, dazwischen die Lücke
		quad(g, cx - 7 * u, cx - 2 * u, cy, cx - 8 * u, cx - 2 * u, cy + 4 * u, body);
		quad(g, cx + 2 * u, cx + 7 * u, cy, cx + 2 * u, cx + 8 * u, cy + 4 * u, body);
		rect(g, cx - 7 * u, cy, cx - 2 * u, cy + 0.6F * u, edge);
		rect(g, cx + 2 * u, cy, cx + 7 * u, cy + 0.6F * u, edge);
		rect(g, cx - 8 * u, cy + 4 * u, cx + 8 * u, cy + 9 * u, body);
		rect(g, cx - 5 * u, cy + 6 * u, cx + 5 * u, cy + 7 * u, shadow);

		// Schlittenende mit Munitionsanzeige (grüne LEDs)
		quad(g, cx - 9 * u, cx + 9 * u, cy + 9 * u, cx - 10 * u, cx + 10 * u, cy + 30 * u, body);
		rect(g, cx - 9 * u, cy + 9 * u, cx + 9 * u, cy + 10 * u, edge);
		rect(g, cx - 6 * u, cy + 13 * u, cx + 2 * u, cy + 27 * u, shadow);
		rect(g, cx - 5 * u, cy + 14 * u, cx + 1 * u, cy + 26 * u, 0xFF222226);
		for (int i = 0; i < 5; i++) {
			float y = cy + (14 + i * 2.2F) * u;
			rect(g, cx + 4 * u, y, cx + 6 * u, y + 1.4F * u, 0xFF46E068);
		}
		// Griffstück, etwas breiter
		rect(g, cx - 12 * u, cy + 30 * u, cx + 11 * u, cy + 40 * u, 0xFF36363B);
		rect(g, cx - 12 * u, cy + 30 * u, cx + 11 * u, cy + 31 * u, edge);
		rect(g, cx - 12 * u, cy + 39 * u, cx + 11 * u, cy + 40 * u, shadow);

		// Griff mit geriffelter Rückseite, läuft nach unten aus dem Bild
		quad(g, cx - 8 * u, cx + 8 * u, cy + 40 * u, cx - 9 * u, cx + 9 * u, bottom, grip);
		for (float y = cy + 43 * u; y < bottom; y += 2.5F * u) {
			rect(g, cx - 5 * u, y, cx + 5 * u, y + u, shadow);
		}
	}

	private static void rect(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int color) {
		g.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), color);
	}

	/** Trapez: oben von xTopL bis xTopR, unten von xBotL bis xBotR, zeilenweise gefüllt. */
	private static void quad(GuiGraphicsExtractor g, float xTopL, float xTopR, float yTop,
			float xBotL, float xBotR, float yBot, int color) {
		int y0 = Math.round(yTop);
		int y1 = Math.round(yBot);
		for (int y = y0; y < y1; y++) {
			float t = (y - y0 + 0.5F) / Math.max(1, y1 - y0);
			int l = Math.round(xTopL + (xBotL - xTopL) * t);
			int r = Math.round(xTopR + (xBotR - xTopR) * t);
			g.fill(l, y, r, y + 1, color);
		}
	}

	/** Zielfernrohr: runder Ausschnitt (aus Streifen angenähert), Rest schwarz, Fadenkreuz. */
	private static void scope(GuiGraphicsExtractor g, int cx, int cy) {
		int w = g.guiWidth();
		int h = g.guiHeight();
		int r = (int) (h * 0.42);
		g.fill(0, 0, w, cy - r, BLACK);
		g.fill(0, cy + r, w, h, BLACK);
		for (int dy = -r; dy < r; dy += 2) {
			int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
			g.fill(0, cy + dy, cx - half, cy + dy + 2, BLACK);
			g.fill(cx + half, cy + dy, w, cy + dy + 2, BLACK);
		}
		g.fill(cx - r, cy, cx + r, cy + 1, BLACK);
		g.fill(cx, cy - r, cx + 1, cy + r, BLACK);
		g.fill(cx - r, cy - 1, cx - r / 3, cy + 2, BLACK);
		g.fill(cx + r / 3, cy - 1, cx + r, cy + 2, BLACK);
		g.fill(cx - 1, cy + r / 3, cx + 2, cy + r, BLACK);
	}
}
