package de.knospenraucher.mczombies.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.knospenraucher.mczombies.network.GunActionPayload;
import de.knospenraucher.mczombies.weapon.GunItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Waffen-Steuerung auf dem Client:
 * Rechtsklick (Taste "Benutzen") schießt, R lädt nach.
 * Automatikwaffen schicken jeden Tick eine Schussanfrage, solange die Taste gehalten wird;
 * alle anderen nur beim Drücken. Feuerrate und Munition prüft der Server.
 */
public final class GunInput {
	private static KeyMapping reloadKey;
	private static boolean useWasDown;

	private GunInput() {
	}

	public static void register() {
		reloadKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.mczombies.reload", InputConstants.KEY_R, KeyMapping.Category.GAMEPLAY));
		ClientTickEvents.END_CLIENT_TICK.register(GunInput::tick);
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null) {
			useWasDown = false;
			return;
		}
		ItemStack stack = mc.player.getMainHandItem();
		boolean holdingGun = stack.getItem() instanceof GunItem;

		while (reloadKey.consumeClick()) {
			if (holdingGun) {
				ClientPlayNetworking.send(new GunActionPayload(true));
			}
		}

		boolean useDown = mc.options.keyUse.isDown();
		if (holdingGun && useDown) {
			GunItem gun = (GunItem) stack.getItem();
			if (gun.stats().automatic || !useWasDown) {
				ClientPlayNetworking.send(new GunActionPayload(false));
			}
		}
		useWasDown = useDown;
	}
}
