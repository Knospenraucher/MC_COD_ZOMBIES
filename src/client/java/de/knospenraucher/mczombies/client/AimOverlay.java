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
		if (!GunInput.isAiming() || mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem gun)) {
			return;
		}
		int cx = g.guiWidth() / 2;
		int cy = g.guiHeight() / 2;
		if ("sniper".equals(gun.category())) {
			scope(g, cx, cy);
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
