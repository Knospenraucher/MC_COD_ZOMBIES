package de.knospenraucher.mczombies.client;

import de.knospenraucher.mczombies.MCZombies;
import de.knospenraucher.mczombies.network.HudSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

/** Client-Einstiegspunkt: empfängt den Spielzustand, zeichnet das HUD und steuert die Waffen. */
public class MCZombiesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HudSyncPayload.TYPE,
				(payload, context) -> ClientGameState.update(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientGameState.clear());

		HudElementRegistry.addLast(MCZombies.id("hud"), ZombiesHud::render);
		HudElementRegistry.addLast(MCZombies.id("knife"), KnifeAnimation::render);
		GunInput.register();
	}
}
