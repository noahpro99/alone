package dev.alone.core.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Turns off vanilla's fast food-based health regen (proposal §1.5): a full hunger bar should not
 * snap your health back in seconds — that would make injuries meaningless. We no-op the two
 * {@code heal()} calls inside {@code FoodData.tick} (leaving exhaustion and starvation untouched);
 * vitality instead recovers <em>slowly</em>, and only when you're fed, hydrated, and unwounded
 * (see {@code SurvivalMeters}).
 */
@Mixin(FoodData.class)
public class FoodDataRegenMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void alone$noFastRegen(ServerPlayer player, float amount) {
        // deliberately nothing — slow vitality recovery replaces it
    }
}
