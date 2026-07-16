package dev.alone.core.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns off vanilla's fast food-based health regen (proposal §1.5): a full hunger bar should not
 * snap your health back in seconds — that would make injuries meaningless. Vitality instead recovers
 * <em>slowly</em>, and only when you're fed, hydrated, and unwounded (see {@code SurvivalMeters}).
 *
 * <p>We no-op BOTH halves of the natural-regen branches in {@code FoodData.tick}: the {@code heal()}
 * calls (so no fast healing) AND the {@code addExhaustion()} calls that pay for that healing. The
 * exhaustion no-op is the important half and was the bug behind "starved in a day": vanilla spends up
 * to 6 exhaustion every 10 ticks to heal a hurt, well-fed player, and since we'd disabled only the
 * heal — not the cost — a wounded body burned its whole food bar trying to mend and mended nothing,
 * looping because it never got un-hurt. Killing the exhaustion too makes regen fully inert: a wound
 * costs food only through the slow, deliberate recovery in {@code SurvivalMeters}, not this frantic
 * vanilla loop. (Exhaustion from real exertion — sprinting, jumping — is added outside {@code tick}
 * and is untouched; starvation at empty is likewise untouched.)
 */
@Mixin(FoodData.class)
public class FoodDataRegenMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void alone$noFastRegen(ServerPlayer player, float amount) {
        // deliberately nothing — slow vitality recovery replaces it
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
    private void alone$noRegenExhaustion(FoodData food, float amount) {
        // deliberately nothing — don't burn food paying for a heal we've disabled (this was the
        // "starved in a day" leak: a wounded, fed body drained its whole bar healing nothing).
    }
}
