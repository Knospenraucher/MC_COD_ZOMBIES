package de.knospenraucher.mczombies.client;

import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.game.GameState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * Schadensanzeige wie in Black Ops III statt Herzen: Bei jedem Treffer blitzt der Bildschirmrand rot
 * auf, je weniger Leben, desto röter bleibt der Rand. Ab 20 % Leben pulsiert er und man hört den
 * Herzschlag, bis man wieder ganz geheilt ist. Liest nur das eigene Leben, braucht kein Paket.
 */
public final class DamageOverlay {
	/** Rot des Randes (ohne Alpha). */
	private static final int RED = 0x8A0000;
	/** So viele Ringe bilden den weichen Rand. */
	private static final int BANDS = 14;
	/** So viel stärker wird der Rand pro Treffer, und so schnell verblasst das (Ticks). */
	private static final float FLASH_PER_HIT = 0.6F;
	private static final float FLASH_FADE_TICKS = 30.0F;
	private static final float HURT_ALPHA = 0.45F;
	private static final int HEARTBEAT_TICKS = 20;

	private static float lastHealth = -1.0F;
	private static float lastMax = -1.0F;
	private static float flash;
	private static boolean veryHurt;
	private static int heartbeatTimer;

	private DamageOverlay() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(DamageOverlay::tick);
	}

	/** Im Spiel keine Herzen, kein Hunger und keine Rüstung anzeigen (BO3 zeigt kein Leben). */
	public static boolean hideVanillaBars() {
		return ZombiesConfig.get().hideVanillaBars && participating(Minecraft.getInstance());
	}

	/** Nur Teilnehmer eines laufenden Spiels (wer zuschaut oder nicht mitspielt, behält seine Herzen). */
	private static boolean participating(Minecraft mc) {
		if (mc.player == null || ClientGameState.state() == GameState.IDLE) {
			return false;
		}
		String name = mc.player.getName().getString();
		return ClientGameState.players().stream().anyMatch(entry -> entry.name().equals(name));
	}

	private static boolean active(Minecraft mc) {
		return participating(mc) && !mc.player.isSpectator();
	}

	private static void tick(Minecraft mc) {
		if (!active(mc)) {
			lastHealth = lastMax = -1.0F;
			flash = 0.0F;
			veryHurt = false;
			heartbeatTimer = 0;
			return;
		}
		if (mc.isPaused()) {
			// Pausenmenü: kein Herzschlag, nichts verblasst.
			return;
		}
		LocalPlayer player = mc.player;
		float health = player.getHealth();
		float max = player.getMaxHealth();
		if (lastHealth >= 0 && health < lastHealth - 0.01F && Math.abs(max - lastMax) < 0.01F) {
			flash = Math.min(1.0F, flash + FLASH_PER_HIT);
		}
		lastHealth = health;
		lastMax = max;
		flash = Math.max(0.0F, flash - 1.0F / FLASH_FADE_TICKS);

		if (health >= max) {
			veryHurt = false;
		} else if (health > 0 && health <= max * ZombiesConfig.get().veryHurtFraction + 1.0E-3F) {
			veryHurt = true;
		}
		if (veryHurt) {
			if (--heartbeatTimer <= 0) {
				player.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
				heartbeatTimer = HEARTBEAT_TICKS;
			}
		} else {
			heartbeatTimer = 0;
		}
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (!active(mc)) {
			return;
		}
		LocalPlayer player = mc.player;
		float max = Math.max(1.0F, player.getMaxHealth());
		float ratio = Mth.clamp(player.getHealth() / max, 0.0F, 1.0F);
		float alpha = HURT_ALPHA * (1.0F - ratio) + flash * 0.5F;
		if (veryHurt) {
			float time = (player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(false)) / 20.0F;
			float pulse = 0.35F + 0.25F * (0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI));
			alpha = Math.max(alpha, pulse);
		}
		alpha = Math.min(0.9F, alpha);
		if (alpha < 0.01F) {
			return;
		}
		vignette(graphics, graphics.guiWidth(), graphics.guiHeight(), alpha);
	}

	/** Roter Rand aus ineinanderliegenden Ringen, außen kräftig, nach innen durchsichtig. */
	private static void vignette(GuiGraphicsExtractor g, int w, int h, float alpha) {
		int t = Math.max(1, Math.round(Math.min(w, h) * 0.3F / BANDS));
		for (int i = 0; i < BANDS; i++) {
			float fade = 1.0F - (float) i / BANDS;
			int a = Math.round(255 * alpha * fade * fade);
			if (a <= 0) {
				continue;
			}
			int color = (a << 24) | RED;
			int in = i * t;
			int out = (i + 1) * t;
			if (out * 2 >= Math.min(w, h)) {
				break;
			}
			g.fill(in, in, w - in, out, color);
			g.fill(in, h - out, w - in, h - in, color);
			g.fill(in, out, out, h - out, color);
			g.fill(w - out, out, w - in, h - out, color);
		}
	}
}
