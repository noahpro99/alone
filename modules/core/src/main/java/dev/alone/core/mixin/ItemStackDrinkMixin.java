package dev.alone.core.mixin;

import dev.alone.core.AnimalProducts;
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

/**
 * Drinking a liquid restores thirst (§1.2) — milk, honey, or a water bottle. Milk also <b>nourishes</b>
 * (§1.1/§7.2): a bucket of whole milk is a genuine, if modest, meal, so it feeds hunger on top of the
 * thirst.
 *
 * <p><b>Condition-clearing note.</b> Vanilla {@code MilkBucketItem.finishUsingItem} strips <em>all</em>
 * mob effects. In this pack that is <b>not</b> a way to cheese survival illnesses: every condition
 * ({@link dev.alone.core.Conditions} sickness/bleeding/sprain/infection/dysentery, {@link
 * dev.alone.core.Nutrition} scurvy) is a persistent <em>attachment</em> that vanilla milk cannot touch —
 * it only clears the transient debuff effects those conditions re-stamp on the player every ~second, so
 * anything milk wipes is right back within a tick. No neutralising mixin is needed; milk keeps its
 * vanilla "clears a random potion effect" behaviour and simply gains food value here.
 */
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
        // Milk is food, not just water — a modest meal's worth of calories in the bucket.
        if (self.is(Items.MILK_BUCKET)) {
            player.getFoodData().eat(AnimalProducts.MILK_NUTRITION, AnimalProducts.MILK_SATURATION);
        }
    }
}
