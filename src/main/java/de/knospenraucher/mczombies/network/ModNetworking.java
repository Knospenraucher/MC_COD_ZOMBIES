package de.knospenraucher.mczombies.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Registriert alle eigenen Netzwerkpakete (muss auf Server und Client passieren). */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.playS2C().register(HudSyncPayload.TYPE, HudSyncPayload.CODEC);
	}
}
