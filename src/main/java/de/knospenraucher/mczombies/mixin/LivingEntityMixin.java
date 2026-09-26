package de.knospenraucher.mczombies.mixin;

import de.knospenraucher.mczombies.game.ZombieHealth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Leitet Schaden an Rundenzombies über deren BO3-Leben um (siehe {@link ZombieHealth}). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float mczombies$codHealth(float amount, ServerLevel level, DamageSource source, float originalAmount) {
		return ZombieHealth.onHurt((LivingEntity) (Object) this, source, amount);
	}
}
