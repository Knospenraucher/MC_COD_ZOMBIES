package de.knospenraucher.mczombies.client;

import de.knospenraucher.mczombies.game.GameState;
import de.knospenraucher.mczombies.network.HudSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Zeichnet das Spiel-HUD:
 * links oben die Runde (groß, rot) und darunter Zombies/Countdown,
 * rechts oben die Punkteliste aller Spieler.
 */
public final class ZombiesHud {
	// Farben im ARGB-Format (Alpha muss gesetzt sein, sonst ist der Text unsichtbar).
	private static final int RED = 0xFFB01010;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFAAAAAA;
	private static final int GOLD = 0xFFFFD24A;
	private static final int OWN = 0xFF7CFC7C;
	private static final int BACKGROUND = 0x80000000;

	private ZombiesHud() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		GameState state = ClientGameState.state();
		if (state == GameState.IDLE) {
			return;
		}
		Font font = mc.font;

		// ---- Runde (doppelt so groß)
		String roundText = ClientGameState.round() > 0 ? String.valueOf(ClientGameState.round()) : "-";
		graphics.pose().pushMatrix();
		graphics.pose().scale(3.0F, 3.0F);
		graphics.text(font, roundText, 3, 3, RED, true);
		graphics.pose().popMatrix();

		// ---- Zeile darunter: Zombies übrig / Countdown / Game Over
		String info = switch (state) {
			case ACTIVE -> "Zombies: " + ClientGameState.zombiesLeft();
			case INTERMISSION -> "Nächste Runde in " + ClientGameState.countdown() + " s";
			case GAME_OVER -> "GAME OVER";
			default -> "";
		};
		graphics.text(font, info, 10, 40, state == GameState.GAME_OVER ? RED : WHITE, true);

		// ---- Punkteliste rechts oben
		String ownName = mc.player != null ? mc.player.getName().getString() : "";
		int y = 8;
		int right = graphics.guiWidth() - 8;
		for (HudSyncPayload.Entry entry : ClientGameState.players()) {
			String name = entry.down() ? entry.name() + " (down)" : entry.name();
			String points = String.valueOf(entry.points());
			int nameWidth = font.width(name);
			int pointsWidth = font.width(points);
			int left = right - nameWidth - 10 - pointsWidth;

			graphics.fill(left - 3, y - 2, right + 3, y + font.lineHeight, BACKGROUND);
			int nameColor = entry.down() ? GRAY : entry.name().equals(ownName) ? OWN : WHITE;
			graphics.text(font, name, left, y, nameColor, true);
			graphics.text(font, points, right - pointsWidth, y, GOLD, true);
			y += font.lineHeight + 3;
		}
	}
}
