package de.knospenraucher.mczombies.client;

import de.knospenraucher.mczombies.MCZombies;
import de.knospenraucher.mczombies.network.HudSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Client-Einstiegspunkt: empfängt den Spielzustand, zeichnet das HUD und steuert die Waffen. */
public class MCZombiesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HudSyncPayload.TYPE,
				(payload, context) -> ClientGameState.update(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientGameState.clear());

		// Blutiger Bildschirmrand statt Herzen (unter Hotbar, Chat und dem restlichen HUD).
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, MCZombies.id("damage"), DamageOverlay::render);
		for (Identifier bar : List.of(VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.ARMOR_BAR)) {
			HudElementRegistry.replaceElement(bar, original -> (graphics, deltaTracker) -> {
				if (!DamageOverlay.hideVanillaBars()) {
					original.extractRenderState(graphics, deltaTracker);
				}
			});
		}
		DamageOverlay.register();
		HudElementRegistry.addLast(MCZombies.id("hud"), ZombiesHud::render);
		HudElementRegistry.addLast(MCZombies.id("aim"), AimOverlay::render);
		HudElementRegistry.addLast(MCZombies.id("knife"), KnifeAnimation::render);
		GunInput.register();
	}
}
