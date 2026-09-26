package de.knospenraucher.mczombies.mixin.client;

import de.knospenraucher.mczombies.client.GunInput;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom beim Zielen über Kimme und Korn (wie beim Spannen eines Bogens, nur stärker). */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true, require = 0)
	private void mczombies$aimZoom(CallbackInfoReturnable<Float> cir) {
		float zoom = GunInput.zoomFactor();
		if (zoom < 1.0F) {
			cir.setReturnValue(cir.getReturnValueF() * zoom);
		}
	}
}
