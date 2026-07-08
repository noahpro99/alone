package dev.alone.core.mixin;

import dev.alone.core.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Seasonal + realistic farming (proposal §4.1). A field is never a sure thing:
 * <ul>
 *   <li><b>Baseline failure</b> — a small per-tick chance any crop just dies (blight, pests, rot).</li>
 *   <li><b>Winter frost</b> — a much higher death chance while it's winter, and no growth at all.</li>
 *   <li><b>Weeds</b> — grass encroaching from neglected soil chokes crops; each nearby weed raises the
 *       death chance. Crops also sow weeds onto adjacent bare soil over time, so an untended plot rots
 *       itself. Weed by breaking the grass.</li>
 * </ul>
 * All numbers are per random-tick (~17/day per crop) and tunable. Bonemeal (a deliberate act) bypasses.
 */
@Mixin(CropBlock.class)
public class CropGrowthMixin {
    private static final float BASE_DEATH = 0.002f;     // ~3%/day — the field is never guaranteed
    private static final float WINTER_DEATH = 0.02f;    // frost — an unattended winter field mostly dies
    private static final float PER_WEED_DEATH = 0.006f; // each adjacent weed chokes the crop a little more
    private static final float WEED_SPREAD = 0.02f;     // chance a crop lets a weed sprout on nearby soil

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void alone$cropHazards(BlockState state, ServerLevel level, BlockPos pos,
                                   RandomSource random, CallbackInfo ci) {
        boolean winter = Seasons.isWinter(level);
        int weeds = alone$countWeeds(level, pos);
        float death = BASE_DEATH + (winter ? WINTER_DEATH : 0f) + weeds * PER_WEED_DEATH;

        if (random.nextFloat() < death) {
            level.removeBlock(pos, false); // the crop fails and dies — no harvest
            ci.cancel();
            return;
        }

        if (random.nextFloat() < WEED_SPREAD) {
            alone$trySpreadWeed(level, pos, random);
        }

        if (winter) {
            ci.cancel(); // frozen ground — no growth this tick
        }
    }

    private static int alone$countWeeds(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (alone$isWeed(level.getBlockState(pos.offset(dx, 0, dz)))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean alone$isWeed(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.TALL_GRASS);
    }

    private static void alone$trySpreadWeed(ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos target = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
        if (target.equals(pos) || !level.getBlockState(target).isAir()) {
            return;
        }
        BlockState below = level.getBlockState(target.below());
        if (below.is(Blocks.FARMLAND) || below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK)) {
            BlockState weed = Blocks.SHORT_GRASS.defaultBlockState();
            if (weed.canSurvive(level, target)) {
                level.setBlockAndUpdate(target, weed);
            }
        }
    }
}
