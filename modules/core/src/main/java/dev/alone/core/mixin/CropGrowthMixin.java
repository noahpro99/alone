package dev.alone.core.mixin;

import dev.alone.core.Seasons;
import dev.alone.core.SoilFertility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Seasonal + realistic farming (proposal §4.1). A field is a whole season's investment, and never a sure
 * thing — it can fail four ways, so farming is real husbandry:
 * <ul>
 *   <li><b>Blight</b> — a small chance any crop just dies (pests, rot). Low, but a field is never guaranteed.</li>
 *   <li><b>Winter frost</b> — crops die within days once winter sets in, and don't grow at all.</li>
 *   <li><b>Weeds</b> — grass encroaching from neglected soil chokes crops; each nearby weed raises the
 *       death chance, and crops sow more weeds onto bare soil over time, so an untended plot rots itself.
 *       Weed by breaking the grass.</li>
 *   <li><b>Drought</b> — a crop whose farmland has no water reaching it (moisture 0) <b>dries out and
 *       dies</b>, twice as fast in summer heat. So a field must be irrigated — kept within reach of water.</li>
 * </ul>
 * All numbers are a chance per random-tick (~17/day per crop) and tunable. Bonemeal (a deliberate act)
 * still forces growth past all of this.
 */
@Mixin(CropBlock.class)
public class CropGrowthMixin {
    // All hazards are a chance PER RANDOM-TICK (~17/day). With crops now taking ~a season, the rates are set
    // low so a well-tended, watered field MOSTLY succeeds — weeds, drought, and winter are the real killers,
    // and those rightly compound over the long season. (Tune against playtest — the vanilla growth baseline
    // these are set against is only estimated.)
    private static final float BASE_DEATH = 0.0003f;          // ~0.5%/day blight — the field is never a sure thing
    private static final float WINTER_DEATH = 0.01f;          // frost — an exposed winter crop dies within days
    private static final float PER_WEED_DEATH = 0.0009f;      // each adjacent weed chokes the crop (~1.5%/day)
    private static final float DROUGHT_DEATH = 0.0025f;       // farmland gone dry — the crop wilts (~4%/day)
    private static final float DROUGHT_DEATH_SUMMER = 0.005f; // summer heat dries it out twice as fast
    private static final float EXHAUSTION_DEATH = 0.004f;     // fully worn-out soil starves the crop (~7%/day)
    private static final float WEED_SPREAD = 0.02f;           // chance a crop lets a weed sprout on nearby soil
    // Growth is paced to ~a season: only a fraction of would-be growth ticks advance the plant, so a crop is
    // a plant-in-spring, harvest-by-summer investment — not a couple of days. You pass the wait by living
    // your days (resting fast-forwards them). Tune to mature in ~one 28-day season.
    private static final float GROWTH_TICK_CHANCE = 0.07f;    // ~1 in 14 growth ticks advances (~a season to mature)

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void alone$cropHazards(BlockState state, ServerLevel level, BlockPos pos,
                                   RandomSource random, CallbackInfo ci) {
        boolean winter = Seasons.isWinter(level);
        int weeds = alone$countWeeds(level, pos);
        // Drought: farmland with no water reaching it (moisture 0) dries the crop out — worse in summer heat.
        BlockState soil = level.getBlockState(pos.below());
        boolean parched = soil.is(Blocks.FARMLAND) && soil.getValue(FarmlandBlock.MOISTURE) == 0;
        float drought = parched ? (Seasons.index(level) == 1 ? DROUGHT_DEATH_SUMMER : DROUGHT_DEATH) : 0f;
        // Exhausted soil (over-farmed, un-rotated) starves the crop — worse under monoculture.
        float exhaust = SoilFertility.exhaustion(level, pos.below());
        float exhaustion = exhaust * EXHAUSTION_DEATH
            * (exhaust > 0f && SoilFertility.isMonoculture(level, pos.below(), state.getBlock()) ? 1.5f : 1f);
        float death = BASE_DEATH + (winter ? WINTER_DEATH : 0f) + weeds * PER_WEED_DEATH + drought + exhaustion;

        if (random.nextFloat() < death) {
            level.removeBlock(pos, false); // the crop fails and dies — no harvest
            ci.cancel();
            return;
        }

        if (random.nextFloat() < WEED_SPREAD) {
            alone$trySpreadWeed(level, pos, random);
        }

        // Slow it to a season: most random ticks are NOT a growth tick, so the crop just waits. (Winter
        // stops growth entirely below; this only paces the growing months.) Worn-out soil grows it slower
        // still — up to ~70% slower on fully-exhausted ground.
        if (!winter && random.nextFloat() > GROWTH_TICK_CHANCE * (1f - exhaust * 0.7f)) {
            ci.cancel();
            return;
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
