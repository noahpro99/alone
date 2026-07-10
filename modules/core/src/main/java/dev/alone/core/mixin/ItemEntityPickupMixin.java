package dev.alone.core.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No vacuum pickup (proposal §5.1 / realism). Walking over a dropped item no longer sucks it into your
 * inventory — you pick things up deliberately by right-clicking them (see {@code Pickup}). This just
 * cancels the auto-pickup on touch; XP orbs and arrows (different entities) are unaffected.
 */
@Mixin(ItemEntity.class)
public class ItemEntityPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void alone$noAutoPickup(Player player, CallbackInfo ci) {
        ci.cancel();
    }
}
