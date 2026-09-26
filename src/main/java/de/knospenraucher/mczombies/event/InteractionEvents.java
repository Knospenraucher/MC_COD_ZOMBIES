package de.knospenraucher.mczombies.event;

import de.knospenraucher.mczombies.game.GameManager;
import de.knospenraucher.mczombies.weapon.GunItem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** Rechtsklicks auf Map-Elemente (Türen, Wandwaffen, Zufallskiste) und Linksklick mit Waffen. */
public final class InteractionEvents {
	private InteractionEvents() {
	}

	public static void register() {
		// Mit einer Schusswaffe in der Hand schießt der Linksklick: nicht abbauen, nicht schlagen.
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
				player.getItemInHand(hand).getItem() instanceof GunItem ? InteractionResult.FAIL : InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
				player.getItemInHand(hand).getItem() instanceof GunItem ? InteractionResult.FAIL : InteractionResult.PASS);

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			// Nur auf dem Server und nur für die Haupthand, sonst zählt ein Klick doppelt.
			if (!(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			GameManager game = GameManager.get();
			if (game != null && game.onUseBlock(serverPlayer, hitResult.getBlockPos())) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
	}
}
