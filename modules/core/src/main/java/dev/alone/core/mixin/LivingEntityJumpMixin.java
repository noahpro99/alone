package dev.alone.core.mixin;

import dev.alone.core.SurvivalMeters;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A jump is a burst of effort — it costs stamina (proposal §1.4). Server-side only. */
@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void alone$jumpExertion(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && !player.isCreative()) {
            SurvivalMeters.exert(player, 3f);
        }
    }
}
