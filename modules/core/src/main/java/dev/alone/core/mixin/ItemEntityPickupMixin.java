package dev.alone.core.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No vacuum pickup (proposal §5.1 / realism). Walking over a dropped item no longer sucks it in — you
 * pick things up deliberately: right-click a specific item ({@code Pickup}), or, more reliably,
 * <b>sneak-walk over items to scoop them up</b> (no fiddly aiming). Only the normal, non-sneaking
 * walkover is cancelled here; XP orbs and arrows (different entities) are unaffected.
 */
@Mixin(ItemEntity.class)
public class ItemEntityPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void alone$noAutoPickup(Player player, CallbackInfo ci) {
        if (!player.isShiftKeyDown()) {
            ci.cancel(); // walking over it doesn't grab it — crouch to scoop, or right-click it
        }
    }
}
