package de.knospenraucher.mczombies.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.knospenraucher.mczombies.network.GunActionPayload;
import de.knospenraucher.mczombies.weapon.GunItem;
import de.knospenraucher.mczombies.weapon.KnifeMelee;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Waffen-Steuerung auf dem Client:
 * Linksklick schießt, Rechtsklick (halten) zielt über Kimme und Korn, R lädt nach, V sticht mit dem
 * Messer zu. Automatikwaffen schicken jeden Tick eine Schussanfrage, solange die Taste gehalten wird;
 * alle anderen nur beim Drücken. Feuerrate und Munition prüft der Server.
 */
public final class GunInput {
	private static KeyMapping reloadKey;
	private static KeyMapping meleeKey;
	private static boolean attackWasDown;
	private static int pendingShots;
	private static boolean aiming;
	private static int nextKnifeTick;

	private GunInput() {
	}

	public static void register() {
		reloadKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.mczombies.reload", InputConstants.KEY_R, KeyMapping.Category.GAMEPLAY));
		meleeKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.mczombies.melee", InputConstants.KEY_V, KeyMapping.Category.GAMEPLAY));
		ClientTickEvents.START_CLIENT_TICK.register(GunInput::swallowAttackClicks);
		ClientTickEvents.END_CLIENT_TICK.register(GunInput::tick);
	}

	/**
	 * Vor Minecrafts eigener Tastenverarbeitung: Linksklicks mit einer Waffe in der Hand gehören uns
	 * (Schießen), damit Vanilla nicht schlägt oder abbaut. Gezählt werden sie hier.
	 */
	public static boolean isAiming() {
		return aiming;
	}

	/** Sichtfeld-Faktor beim Zielen: Zielfernrohre zoomen stark, Kimme und Korn leicht. 1 = kein Zoom. */
	public static float zoomFactor() {
		Minecraft mc = Minecraft.getInstance();
		if (!aiming || mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem gun)) {
			return 1.0F;
		}
		return "sniper".equals(gun.category()) ? 0.35F : 0.75F;
	}

	private static void setAiming(boolean value) {
		if (value == aiming) {
			return;
		}
		aiming = value;
		if (Minecraft.getInstance().getConnection() != null) {
			ClientPlayNetworking.send(new GunActionPayload(value ? GunActionPayload.AIM_START : GunActionPayload.AIM_STOP));
		}
	}

	private static void swallowAttackClicks(Minecraft mc) {
		if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem) || mc.screen != null) {
			return;
		}
		while (mc.options.keyAttack.consumeClick()) {
			pendingShots++;
		}
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null) {
			attackWasDown = false;
			setAiming(false);
			pendingShots = 0;
			return;
		}
		ItemStack stack = mc.player.getMainHandItem();
		boolean holdingGun = stack.getItem() instanceof GunItem;

		while (reloadKey.consumeClick()) {
			if (holdingGun) {
				ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.RELOAD));
			}
		}

		// Messer: geht mit jeder Waffe (oder leerer Hand), wie in CoD.
		while (meleeKey.consumeClick()) {
			if (mc.player.tickCount >= nextKnifeTick) {
				nextKnifeTick = mc.player.tickCount + KnifeMelee.COOLDOWN_TICKS;
				boolean lunged = KnifeAnimation.start();
				ClientPlayNetworking.send(new GunActionPayload(lunged ? GunActionPayload.MELEE_LUNGE : GunActionPayload.MELEE));
			}
		}

		// Linksklick: schießen. Automatikwaffen feuern, solange die Taste gehalten wird.
		boolean attackDown = mc.options.keyAttack.isDown() && mc.screen == null;
		if (holdingGun) {
			GunItem gun = (GunItem) stack.getItem();
			if ((attackDown && gun.stats().automatic) || pendingShots > 0 || (attackDown && !attackWasDown)) {
				ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.SHOOT));
			}
		}
		pendingShots = 0;
		attackWasDown = attackDown;

		// Rechtsklick halten: über Kimme und Korn zielen.
		setAiming(holdingGun && mc.options.keyUse.isDown() && mc.screen == null);
	}
}
