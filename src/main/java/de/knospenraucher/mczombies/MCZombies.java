package de.knospenraucher.mczombies;

import de.knospenraucher.mczombies.command.ZombiesCommand;
import de.knospenraucher.mczombies.config.ZombiesConfig;
import de.knospenraucher.mczombies.event.CombatEvents;
import de.knospenraucher.mczombies.game.GameManager;
import de.knospenraucher.mczombies.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt der Mod (läuft auf Server und Client).
 * Registriert Config, Netzwerk-Pakete, Befehle und Server-Events.
 */
public class MCZombies implements ModInitializer {
	public static final String MOD_ID = "mczombies";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ZombiesConfig.load();
		ModNetworking.registerPayloads();

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> ZombiesCommand.register(dispatcher));

		// Pro Server-Instanz genau ein GameManager (Singleplayer startet intern auch einen Server).
		ServerLifecycleEvents.SERVER_STARTED.register(GameManager::create);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> GameManager.destroy());
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			GameManager game = GameManager.get();
			if (game != null) {
				game.tick();
			}
		});

		CombatEvents.register();
		LOGGER.info("MC Zombies geladen");
	}

	/** Erzeugt eine ID im Namespace der Mod, z.B. {@code mczombies:hud_sync}. */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
