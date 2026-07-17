package dev.alone.core.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arrows are reusable — you recover them from a kill (hunting realism). Vanilla only drops an arrow when it
 * sticks in a <em>block</em>; one that hits an <b>entity</b> is consumed and gone. Here a player-fired arrow
 * that strikes an animal (or anything) drops its arrow item at the spot, so you pick it back up — arrows don't
 * evaporate on a hit. Only real, player-fired arrows ({@code pickup == ALLOWED}) and only non-piercing shots
 * (a piercing arrow already drops itself once spent); mob-fired and creative arrows are untouched.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowRecoverMixin {
    @Shadow
    protected abstract ItemStack getPickupItem();

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void alone$recoverOnHit(EntityHitResult hitResult, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.level() instanceof ServerLevel level
                && self.pickup == AbstractArrow.Pickup.ALLOWED
                && self.getPierceLevel() == 0) {
            self.spawnAtLocation(level, getPickupItem(), 0.1F); // drop the shaft to be retrieved from the kill
        }
    }
}
