package dev.alone.core.mixin;

import dev.alone.core.SurvivalMeters;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Drinking a liquid restores thirst (§1.2) — milk, honey, or a water bottle. */
@Mixin(ItemStack.class)
public class ItemStackDrinkMixin {

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void alone$drinkThirst(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        float restore = 0f;
        if (self.is(Items.MILK_BUCKET) || self.is(Items.HONEY_BOTTLE)) {
            restore = 30f;
        } else if (self.is(Items.POTION)) {
            restore = 40f; // water bottle / potion is mostly water
        }
        if (restore > 0f) {
            SurvivalMeters.drink(player, restore);
        }
    }
}
