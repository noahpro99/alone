package dev.alone.core.mixin;

import dev.alone.core.Climbing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Free-climbing (proposal §5.4 / realism): players cling to soft foliage and can scrabble up short
 * flat walls, on top of vanilla ladders/vines. We only ever <em>add</em> climbable surfaces — if
 * vanilla already says you're on a ladder, we leave it be.
 */
@Mixin(LivingEntity.class)
public class LivingEntityClimbMixin {
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void alone$freeClimb(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return; // already a ladder / vine / climbable tag
        }
        if ((Object) this instanceof Player player && Climbing.canClimb(player)) {
            cir.setReturnValue(true);
        }
    }
}
