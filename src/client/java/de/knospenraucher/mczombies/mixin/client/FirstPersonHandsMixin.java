package de.knospenraucher.mczombies.mixin.client;

import de.knospenraucher.mczombies.client.GunInput;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Beim Zielen zeigt das Visier-Overlay die Waffe, Hand und Waffe in der Ich-Ansicht werden ausgeblendet. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void mczombies$hideWhileAiming(CallbackInfo ci) {
		if (GunInput.hideHeldItem()) {
			ci.cancel();
		}
	}
}
