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
 * Rechtsklick (Taste "Benutzen") schießt, R lädt nach, V sticht mit dem Messer zu.
 * Automatikwaffen schicken jeden Tick eine Schussanfrage, solange die Taste gehalten wird;
 * alle anderen nur beim Drücken. Feuerrate und Munition prüft der Server.
 */
public final class GunInput {
	private static KeyMapping reloadKey;
	private static KeyMapping meleeKey;
	private static boolean useWasDown;
	private static int nextKnifeTick;

	private GunInput() {
	}

	public static void register() {
		reloadKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.mczombies.reload", InputConstants.KEY_R, KeyMapping.Category.GAMEPLAY));
		meleeKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.mczombies.melee", InputConstants.KEY_V, KeyMapping.Category.GAMEPLAY));
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
				ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.RELOAD));
			}
		}

		// Messer: geht mit jeder Waffe (oder leerer Hand), wie in CoD.
		while (meleeKey.consumeClick()) {
			if (mc.player.tickCount >= nextKnifeTick) {
				nextKnifeTick = mc.player.tickCount + KnifeMelee.COOLDOWN_TICKS;
				ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.MELEE));
				KnifeAnimation.start();
			}
		}

		boolean useDown = mc.options.keyUse.isDown();
		if (holdingGun && useDown) {
			GunItem gun = (GunItem) stack.getItem();
			if (gun.stats().automatic || !useWasDown) {
				ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.SHOOT));
			}
		}
		useWasDown = useDown;
	}
}
