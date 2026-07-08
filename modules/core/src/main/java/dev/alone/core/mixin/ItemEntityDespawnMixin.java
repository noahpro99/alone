package dev.alone.core.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dropped items never despawn (proposal: realism — a stash on the ground stays put). Vanilla discards
 * an item once its age hits 6000 ticks; we skip that discard.
 *
 * <p><b>Exception:</b> "fake" items — the throwaway pickup-animation entities from {@code /give} and
 * the like — call {@code setNeverPickUp()} (pickupDelay = 32767) and set age to 5999 so they vanish on
 * the very next tick. Those we must still let despawn, or they'd hang around forever as un-pickable
 * ghost items. So we only suppress the despawn of genuine, pickup-able drops.
 */
@Mixin(ItemEntity.class)
public class ItemEntityDespawnMixin {
    @Shadow private int pickupDelay;

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1))
    private void alone$keepUnlessFake(ItemEntity self) {
        if (this.pickupDelay == 32767) {
            self.discard(); // never-pickup (fake) item — let it go
        }
        // otherwise: a real dropped item — do not despawn
    }
}
