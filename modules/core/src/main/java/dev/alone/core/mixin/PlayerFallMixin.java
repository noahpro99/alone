package dev.alone.core.mixin;

import dev.alone.core.Conditions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Realistic falls (proposal §1.5) run through the <b>injury system</b>, not the vanilla health curve.
 * We take over {@code Player.causeFallDamage} on the server and hand off to
 * {@link Conditions#applyFall} — which rolls for death like real life (LD50 ≈ 15 m) and, on a survived
 * fall, applies a sprain/fracture/bleeding plus a modest hit. Vanilla's soft {@code fallDistance − 3}
 * damage is skipped. Client-side and creative/flying falls fall through to vanilla.
 */
@Mixin(Player.class)
public class PlayerFallMixin {
    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void alone$injuryFall(double fallDistance, float damageMultiplier, DamageSource source,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean harmed = Conditions.applyFall(player, fallDistance, damageMultiplier, source);
        cir.setReturnValue(harmed);
    }
}
