package dev.alone.core.mixin;

import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Kill the vanilla "everyone's asleep — blink to dawn" time-skip (see {@link dev.alone.core.GradualSleep}).
 * Sleeping shouldn't teleport the night away; it should run the clock <em>fast</em> until morning, so the
 * night still happens (and can be interrupted). The instant-skip is gated on {@code areEnoughSleeping}, so
 * forcing that false anywhere it's asked means the skip never fires — leaving GradualSleep to fast-forward
 * the tick rate instead. Players still lie down and recover as normal; only the teleport is suppressed.
 */
@Mixin(SleepStatus.class)
public class SleepStatusMixin {
    @Inject(method = "areEnoughSleeping", at = @At("HEAD"), cancellable = true)
    private void alone$noInstantSkip(int requiredPercent, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
