package dev.alone.core.mixin;

import dev.alone.core.Campfires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Burns a lit campfire's fuel down each tick (proposal §3). When it's spent, the fire is <b>burnt out</b>
 * — the lay has gone to charcoal, so the campfire breaks and drops a piece of charcoal, and you must lay
 * a fresh one (you can't just relight the ash). {@code cookTick} is the lit-state ticker.
 */
@Mixin(CampfireBlockEntity.class)
public class CampfireCookTickMixin {

    @Inject(method = "cookTick", at = @At("HEAD"))
    @SuppressWarnings("rawtypes")
    private static void alone$burnFuel(ServerLevel level, BlockPos pos, BlockState state,
                                       CampfireBlockEntity entity, RecipeManager.CachedCheck cache, CallbackInfo ci) {
        int fuel = Campfires.getFuel(entity);
        if (fuel <= 0) {
            // Burnt out — the wood is spent. Break the campfire (no campfire drop) and leave charcoal.
            level.destroyBlock(pos, false);
            Block.popResource(level, pos, new ItemStack(Items.CHARCOAL));
            return;
        }
        Campfires.setFuel(entity, fuel - 1);

        // The fire's smoke reads its life: a well-fed blaze billows, a dying one barely wisps. Scaled by
        // how much fuel is banked (server-sent, so every nearby player sees the same living fire).
        float life = Math.min(1f, fuel / (float) Campfires.INITIAL_FUEL);
        if (life > 0.05f && level.getGameTime() % 8L == 0L) {
            int puffs = Math.round(life * 3f);
            if (puffs > 0) {
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    puffs, 0.12, 0.02 + 0.06 * life, 0.12, 0.006 + 0.02 * life);
            }
        }
    }
}
