package dev.alone.core.mixin;

import dev.alone.core.Sleeping;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Let a deliberate <b>daytime rest</b> stick (§5.2). In 26.2 {@code Player.tick} wakes any sleeper the moment
 * {@code BedRule.canSleep(level)} is false — which by day is every tick, so lying down to pass the daylight
 * hours was ejected instantly (a night's sleep survives only because {@code canSleep} is true at night). When
 * the player is mid-rest ({@link Sleeping#GROUND_RESTING} — set for both a ground-rest and a daytime bed
 * rest), we answer that they <em>can</em> sleep, so the engine leaves them lying there; {@link
 * dev.alone.core.GradualSleep} then runs the clock fast and rouses them at the phase boundary (nightfall for
 * a day rest, dawn for a night's sleep). Normal sleepers are untouched.
 */
@Mixin(Player.class)
public class PlayerDayRestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/attribute/BedRule;canSleep(Lnet/minecraft/world/level/Level;)Z"))
    private boolean alone$restStaysDown(BedRule bedRule, Level level) {
        boolean vanilla = bedRule.canSleep(level);
        if (!vanilla && (Object) this instanceof Player self
            && self.getAttachedOrElse(Sleeping.GROUND_RESTING, false)) {
            return true; // a deliberate rest — GradualSleep ends it at the boundary, not the day/bed rule
        }
        return vanilla;
    }
}
