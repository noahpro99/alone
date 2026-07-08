package dev.alone.food.mixin;

import dev.alone.core.Conditions;
import dev.alone.core.Hygiene;
import dev.alone.core.Nutrition;
import dev.alone.food.AloneFood;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Raw-food sickness risk (proposal §4.2). {@code ItemStack.finishUsingItem} is the moment an
 * item's use completes — i.e. the bite lands — so it's where we roll the gamble.
 */
@Mixin(ItemStack.class)
public class ItemStackFinishUsingMixin {

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void alone$rawFoodRisk(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide()) {
            return;
        }
        ItemStack self = (ItemStack) (Object) this;
        if (!(entity instanceof Player player) || !self.has(DataComponents.FOOD)) {
            return;
        }
        // Diet variety (§1.1): a monotonous diet shrinks usable hunger.
        Nutrition.onEat(player, self);
        // Per-tier odds (§4.2): raw chicken is a real gamble, fish much safer.
        float chance = 0f;
        int illnessTicks = Conditions.FOODBORNE_ILLNESS_TICKS;
        if (self.is(AloneFood.RAW_HIGH_RISK)) {
            chance = 0.8f;
        } else if (self.is(AloneFood.RAW_MEDIUM_RISK)) {
            chance = 0.45f;
        } else if (self.is(AloneFood.RAW_LOW_RISK)) {
            chance = 0.2f;
            illnessTicks = Conditions.FOODBORNE_ILLNESS_TICKS / 2;
        }
        // Eating with dirty hands contaminates even safe food (§5.6).
        if (Hygiene.handsDirty(player)) {
            chance += 0.25f;
        }
        // On a bad roll you don't just get a few seconds of potion — you contract a lingering
        // foodborne illness (§1.5) that keeps you weak for minutes and persists through relog.
        if (chance > 0f && player.getRandom().nextFloat() < chance) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0)); // acute onset
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            Conditions.addSickness(player, illnessTicks);
        }
    }
}
