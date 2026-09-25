package de.knospenraucher.mczombies.mixin;

import de.knospenraucher.mczombies.game.GameManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verhindert während eines Spiels jedes Spawnen fremder Mobs (natürlich, Spawner, Schleim-Chunks,
 * Spawn-Eier ...). Alle diese Wege landen in {@link ServerLevel#addFreshEntity(Entity)}.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
	private void mczombies$blockForeignMobs(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		GameManager game = GameManager.get();
		if (game != null && game.blocksSpawn(entity)) {
			cir.setReturnValue(false);
		}
	}
}
