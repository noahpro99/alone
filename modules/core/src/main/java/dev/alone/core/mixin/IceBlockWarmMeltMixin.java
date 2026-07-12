package dev.alone.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ice houses need restocking (proposal: Food, farming &amp; preservation; see {@link dev.alone.core.IceHouse}).
 * Vanilla ice only melts in bright block-light (torches/lava), so packed into a <b>dark</b> ice house it
 * would last forever — free, permanent cold. That's not how ice houses worked: the ice slowly melts from the
 * warmth leaking in, which is why you cut fresh blocks each winter and haul them home.
 *
 * <p>So natural ice also melts <b>slowly, in the dark</b>, but only where the surrounding <b>biome is above
 * freezing</b>. Ice hauled into a temperate or warm ice house steadily melts away (restock it), while ice in
 * a <b>below-freezing biome</b> — a frozen lake, an arctic ice cellar — stays frozen indefinitely, so a
 * cold-climate store keeps for free. Only plain {@link Blocks#ICE} is affected; packed/blue ice keep their
 * vanilla rules, and frosted ice runs its own staged melt.
 */
@Mixin(IceBlock.class)
public abstract class IceBlockWarmMeltMixin {

    /** MC's freeze threshold: a biome at or below this base temperature is cold enough to hold snow/ice. */
    private static final float FREEZING = 0.15f;
    /** Per random tick, in a warm biome — kept low so ice lasts a good while before it's gone (tunable). */
    private static final float WARM_MELT_CHANCE = 0.02f;

    @Shadow
    protected abstract void melt(BlockState state, Level level, BlockPos pos);

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void alone$warmMelt(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // Vanilla may have already melted it this tick (bright enough) — if it's no longer plain ice, done.
        if (!level.getBlockState(pos).is(Blocks.ICE)) {
            return;
        }
        if (level.getBiome(pos).value().getBaseTemperature() > FREEZING && random.nextFloat() < WARM_MELT_CHANCE) {
            this.melt(state, level, pos); // reuse vanilla melt (→ water, or evaporates in dry dimensions)
        }
    }
}
