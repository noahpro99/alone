package dev.alone.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Grass colonises bare ground over <b>days</b>, not minutes (realism / §5.4 de-turfing). Vanilla grass and
 * mycelium spread to adjacent dirt on random ticks, greening a cleared patch nearly as fast as you strip it
 * — which makes clearing turf pointless. We throttle only the SPREAD branch: most random ticks where the
 * grass would colonise a neighbour are skipped, so it creeps back over in-game days. Grass <b>dying</b> under
 * cover is untouched (that branch runs when the block can't stay alive), so shading it out still works at once.
 */
@Mixin(SpreadingSnowyBlock.class)
public class SpreadingGrassSlowMixin {
    @Shadow
    private static boolean canStayAlive(BlockState state, LevelReader level, BlockPos pos) {
        throw new AssertionError();
    }

    private static final int SPREAD_SLOWDOWN = 15; // ~1/15 of vanilla's spread rate — days to reclaim bare soil

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void alone$slowSpread(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                  CallbackInfo ci) {
        // Only throttle LIVE grass that would spread to a neighbour; leave the die-under-cover branch to run.
        if (canStayAlive(state, level, pos) && random.nextInt(SPREAD_SLOWDOWN) != 0) {
            ci.cancel();
        }
    }
}
