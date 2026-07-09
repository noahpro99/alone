package dev.alone.core.mixin;

import dev.alone.core.Climbing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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

    /**
     * Slow a bare-wall climb to rock-climbing pace. Vanilla clamps climb motion to ladder speed; we
     * scale the vertical component down for our wall climbs (leaves and real ladders keep vanilla speed).
     */
    @Inject(method = "handleOnClimbable", at = @At("RETURN"), cancellable = true)
    private void alone$slowWallClimb(Vec3 motion, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof Player player && Climbing.isWallClimbing(player)) {
            Vec3 m = cir.getReturnValue();
            cir.setReturnValue(new Vec3(m.x, m.y * Climbing.WALL_CLIMB_SPEED, m.z));
        }
    }
}
