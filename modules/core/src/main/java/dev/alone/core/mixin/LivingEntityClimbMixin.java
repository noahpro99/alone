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
     * Shape the vertical feel of our free-climbs. Vanilla clamps climb motion to ladder speed; we:
     * <ul>
     *   <li>let you <b>crouch to lower yourself down through a canopy</b> at a controlled speed, like
     *       sneaking down scaffolding;</li>
     *   <li><b>slow the ascent</b> of bare-wall and tree climbs (hauling up is the hard part) while
     *       leaving the <b>descent at vanilla ladder speed</b> so climbing down stays smooth.</li>
     * </ul>
     * Real ladders/vines are untouched.
     */
    @Inject(method = "handleOnClimbable", at = @At("RETURN"), cancellable = true)
    private void alone$freeClimbFeel(Vec3 motion, CallbackInfoReturnable<Vec3> cir) {
        if (!((Object) this instanceof Player player)) {
            return;
        }
        Vec3 m = cir.getReturnValue();
        // Rope behaves like scaffolding: you hold your vertical spot, crouch to lower yourself down, and
        // jump/press up to climb up — no automatic sliding.
        if (Climbing.onRope(player)) {
            double vy = player.isShiftKeyDown() ? Climbing.ROPE_DESCEND_SPEED : Math.max(0.0, m.y);
            cir.setReturnValue(new Vec3(m.x, vy, m.z));
            return;
        }
        if (Climbing.isDescendingLeaves(player)) {
            cir.setReturnValue(new Vec3(m.x, Climbing.LEAF_DESCEND_SPEED, m.z));
            return;
        }
        // At the top of a wall, mantle up and over the lip instead of crawling and sliding back.
        if (Climbing.isToppingOut(player)) {
            cir.setReturnValue(Climbing.mantleMotion(player));
            return;
        }
        double factor = Climbing.climbSpeedFactor(player);
        if (factor < 1.0 && m.y > 0.0) { // slow the climb up; leave the climb down ladder-smooth
            cir.setReturnValue(new Vec3(m.x, m.y * factor, m.z));
        }
    }
}
