package dev.alone.food.mixin;

import dev.alone.core.Conditions;
import dev.alone.core.Hygiene;
import dev.alone.core.Nutrition;
import dev.alone.core.SurvivalMeters;
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
        // A hot meal warms you (§1.3).
        if (self.is(AloneFood.HOT_MEALS)) {
            SurvivalMeters.warm(player, SurvivalMeters.HOT_MEAL_WARMTH);
        }
        // Food and thirst (§1.2): salt-preserved food dehydrates most, dry food some; juicy food helps.
        if (self.getOrDefault(dev.alone.food.Spoilage.PRESERVED, false)) {
            SurvivalMeters.drink(player, -12f);
        } else if (self.is(AloneFood.DRY_FOODS)) {
            SurvivalMeters.drink(player, -6f);
        } else if (self.is(AloneFood.JUICY_FOODS)) {
            SurvivalMeters.drink(player, 6f);
        }

        // Energy from food (§1.4): quick-carb / sugary foods give a little stamina straight back.
        if (self.is(AloneFood.ENERGY_FOODS)) {
            SurvivalMeters.restoreStamina(player, 15f);
        }
        // Golden apples stay fantasy — a restorative "second wind" retuned to the body's systems rather
        // than raw hearts: refill stamina, shed fatigue, quench thirst, and a window of vigor (fast
        // recovery, no soreness). The enchanted one lasts far longer.
        if (self.is(net.minecraft.world.item.Items.GOLDEN_APPLE)
            || self.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE)) {
            SurvivalMeters.rest(player, 100f, SurvivalMeters.MAX_STAMINA);
            SurvivalMeters.drink(player, 40f);
            SurvivalMeters.grantVigor(player,
                self.is(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE) ? 2400 : 900);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "A golden warmth floods through you — a second wind."));
        }
        // Per-tier odds (§4.2): raw chicken is a real gamble, fish much safer. But DRYING/SMOKING is what
        // makes raw meat safe to eat in the first place — jerky is dried raw meat, not cooked — so a piece
        // that's been dried (or smoked) carries no raw-meat gamble at all. (Old jerky can still turn: the
        // freshness "going off" risk below is separate and still applies.)
        float chance = 0f;
        int illnessTicks = Conditions.FOODBORNE_ILLNESS_TICKS;
        boolean dried = self.getOrDefault(dev.alone.food.Spoilage.DRIED, false);
        if (!dried) {
            if (self.is(AloneFood.RAW_HIGH_RISK)) {
                chance = 0.8f;
            } else if (self.is(AloneFood.RAW_MEDIUM_RISK)) {
                chance = 0.45f;
            } else if (self.is(AloneFood.RAW_LOW_RISK)) {
                chance = 0.2f;
                illnessTicks = Conditions.FOODBORNE_ILLNESS_TICKS / 2;
            }
        }
        // Eating with dirty hands contaminates even safe food (§5.6).
        if (Hygiene.handsDirty(player)) {
            chance += 0.25f;
        }
        // Food that's "going off" is a gamble even cooked (§4.2): once a perishable is into the last third
        // of its freshness — the "beginning to turn / going off" the tooltip warns of — eating it carries a
        // rising sickness chance, climbing toward the brink of rotting. Ties the freshness system to the bite.
        Long freshness = self.get(dev.alone.food.Spoilage.FRESHNESS);
        if (freshness != null && freshness > 0L) {
            // Freshness is stamped, not churned: the stored value only re-writes on a temperature-band
            // change, so we must drain the elapsed time here exactly as the tooltip (AloneFoodClient) and
            // the writer (Spoilage.tickStack) do — otherwise, at a stable temperature, this reads ~full and
            // the "going off" risk never fires even as the tooltip warns the food is nearly rotten.
            long now = level.getGameTime();
            long elapsed = Math.max(0L, now - self.getOrDefault(dev.alone.food.Spoilage.FRESHNESS_SEEN, now));
            long current = freshness - Math.round(
                elapsed * dev.alone.food.Spoilage.rateForBand(self.getOrDefault(dev.alone.food.Spoilage.RATE_BAND, 0)));
            long budget = self.getOrDefault(dev.alone.food.Spoilage.PRESERVED, false)
                ? dev.alone.food.Spoilage.PRESERVED_SHELF_TICKS : dev.alone.food.Spoilage.SPOIL_TICKS;
            float fraction = Math.max(0f, Math.min(1f, (float) current / budget));
            if (fraction < 0.33f) {
                chance = Math.min(0.95f, chance + (0.33f - fraction) / 0.33f * 0.4f); // up to +0.4 near rotting
            }
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
