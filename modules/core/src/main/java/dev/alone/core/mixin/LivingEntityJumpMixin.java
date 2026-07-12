package dev.alone.core.mixin;

import dev.alone.core.SurvivalMeters;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A jump is a burst of effort — it costs stamina, more the heavier you're loaded (proposal §1.4/§5.1),
 *  so hopping up a hill under a pack is what tires you, not walking a slope. Also removes the vanilla
 *  sprint-jump speed boost for players, so bunny-hopping is no faster than plain sprinting (both sides,
 *  since movement is client-authoritative). */
@Mixin(LivingEntity.class)
public class LivingEntityJumpMixin {

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void alone$jumpExertion(CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && !player.isCreative()) {
            SurvivalMeters.exert(player, SurvivalMeters.jumpCost(player));
        }
    }

    /** Vanilla adds a horizontal shove when you jump while sprinting — the "bunny hop" that makes
     *  sprint-jumping outrun plain sprinting. Realistically a hop doesn't speed you up, so players don't
     *  get it (sprinting is your top ground speed). Mobs keep vanilla behaviour. */
    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void alone$noBunnyHop(LivingEntity self, Vec3 boost) {
        if (!(self instanceof Player)) {
            self.addDeltaMovement(boost);
        }
    }

    /** Even with the sprint-jump shove gone, launching at sprint speed and coasting through the air (less
     *  drag than ground friction) still lets a player bunny-hop faster than they can run — and slip past
     *  slow effects, which only bite ground movement. So bleed off some horizontal momentum on a sprinting
     *  player's jump: chained hops then decay below sprint speed instead of building past it, while a single
     *  running jump still carries you across a gap. Factor is deliberately gentle and playtest-tunable. */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void alone$dampSprintHop(CallbackInfo ci) {
        if ((Object) this instanceof Player player && !player.getAbilities().flying && player.isSprinting()) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.75, v.y, v.z * 0.75);
        }
    }
}
