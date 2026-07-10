package dev.alone.core.mixin;

import dev.alone.core.Climbing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On a rope, crouching should just <b>lower you down smoothly</b> (proposal §5.7), like sinking through
 * leaves — not trigger the vanilla "cling to the ladder" behaviour that stops you dead when you sneak.
 * We suppress that clinging on rope so the crouch-to-descend from {@code LivingEntityClimbMixin} takes
 * effect. Real ladders/vines still cling as normal.
 */
@Mixin(LivingEntity.class)
public class PlayerRopeMixin {
    @Inject(method = "isSuppressingSlidingDownLadder", at = @At("RETURN"), cancellable = true)
    private void alone$ropeDescendsSmoothly(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && (Object) this instanceof Player player && Climbing.onRope(player)) {
            cir.setReturnValue(false);
        }
    }
}
