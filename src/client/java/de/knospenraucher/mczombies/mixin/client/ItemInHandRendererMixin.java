package de.knospenraucher.mczombies.mixin.client;

import de.knospenraucher.mczombies.client.GunInput;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Beim Zielen zeigt das Visier-Overlay die Waffe, die normale Waffe in der Hand wird ausgeblendet. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true, require = 0)
	private void mczombies$hideWhileAiming(CallbackInfo ci) {
		if (GunInput.hideHeldItem()) {
			ci.cancel();
		}
	}
}
