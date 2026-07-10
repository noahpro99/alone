package dev.alone.core.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Make dropped items <b>interactable</b> so you can right-click them to pick them up (proposal §5.1).
 * By default item entities aren't targeted by the crosshair; flipping {@code isPickable} lets the
 * interaction ray hit them. Pickup itself (and blocking a left-click from destroying them) lives in
 * {@code Pickup}.
 */
@Mixin(Entity.class)
public class EntityPickableMixin {
    @Inject(method = "isPickable", at = @At("RETURN"), cancellable = true)
    private void alone$itemsPickable(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && (Object) this instanceof ItemEntity) {
            cir.setReturnValue(true);
        }
    }
}
