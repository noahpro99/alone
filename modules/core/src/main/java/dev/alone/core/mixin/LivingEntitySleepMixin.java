package dev.alone.core.mixin;

import dev.alone.core.Sleeping;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Let a ground-rester stay down. Vanilla wakes any sleeper whose sleeping spot isn't a bed (via
 * {@code checkBedExists} in the sleep tick), which would eject someone resting on bare ground the instant
 * they lay down. When {@link Sleeping#GROUND_RESTING} is set, we answer "yes, a bed exists" so the engine
 * leaves them lying there — {@link dev.alone.core.GradualSleep} runs the night and wakes them at dawn.
 */
@Mixin(LivingEntity.class)
public class LivingEntitySleepMixin {
    @Inject(method = "checkBedExists", at = @At("HEAD"), cancellable = true)
    private void alone$groundRestNeedsNoBed(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && player.getAttachedOrElse(Sleeping.GROUND_RESTING, false)) {
            cir.setReturnValue(true);
        }
    }
}
