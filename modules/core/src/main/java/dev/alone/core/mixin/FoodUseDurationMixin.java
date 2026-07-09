package dev.alone.core.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Eating takes real time (proposal §1.1). Vanilla wolfs a whole meal in 1.6s; that's not a meal, it's
 * a magic trick. We triple the eat time for anything with a food component (a snack becomes ~5s),
 * keeping the relative differences between foods — so you can't scarf a steak mid-scramble, which
 * pairs with the "no eating while sprinting" rule. Drinks/potions (no food component) are untouched.
 */
@Mixin(ItemStack.class)
public class FoodUseDurationMixin {
    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void alone$slowEating(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.has(DataComponents.FOOD)) {
            cir.setReturnValue(cir.getReturnValueI() * 3);
        }
    }
}
