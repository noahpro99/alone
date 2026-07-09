package dev.alone.core.mixin;

import dev.alone.core.Forging;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Live durability for forgeable metal gear (proposal §8.2). An unforged blank is brittle; a forged
 * piece's durability rides on its forge quality (and drops with each re-temper). We scale the value
 * the stack reports so it applies everywhere — the damage bar, wear, and breaking — without baking a
 * fixed MAX_DAMAGE component onto the stack (which would freeze the base and break the scaling).
 */
@Mixin(ItemStack.class)
public class ItemStackForgeDurabilityMixin {
    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void alone$forgeDurability(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!Forging.isForgeable(self)) {
            return;
        }
        cir.setReturnValue(Forging.effectiveMaxDamage(self, cir.getReturnValueI()));
    }
}
