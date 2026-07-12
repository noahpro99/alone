package dev.alone.core.mixin;

import dev.alone.core.Nutrition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wires {@link Nutrition#onEat} to the moment a food is actually eaten (§1.1). Finishing a food resets the
 * scurvy clock if it's fresh (fruit/vegetable) and adds diet-variety fatigue for its group. Without this
 * hook {@code onEat} is never called — so scurvy could never be cured by eating fresh food (it only ever
 * onsets), and the "you can't live on bread" variety penalty never triggered at all. Same {@code
 * ItemStack.finishUsingItem} hook the drink mixin uses; gated to actual foods so drinks fall through.
 */
@Mixin(ItemStack.class)
public class ItemStackEatMixin {
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void alone$eatNutrition(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (self.has(DataComponents.FOOD)) {
            Nutrition.onEat(player, self);
        }
    }
}
