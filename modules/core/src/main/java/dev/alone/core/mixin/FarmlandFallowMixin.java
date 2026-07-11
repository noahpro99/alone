package dev.alone.core.mixin;

import dev.alone.core.SoilFertility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fallow recovery (proposal §4.1). Tilled ground with nothing growing on it slowly regains fertility —
 * nutrients return when no crop is drawing them out. So letting a plot rest (or rotating away from it) is
 * how you heal soil you've worn down, exactly as fallowing did before fertiliser. Full recovery of dead
 * soil takes roughly a season of rest. See {@link SoilFertility}.
 */
@Mixin(FarmlandBlock.class)
public class FarmlandFallowMixin {
    private static final float RECOVERY_CHANCE = 0.2f; // paced so worn-out soil heals over ~a 28-day season

    @Inject(method = "randomTick", at = @At("TAIL"))
    private void alone$fallowRecovery(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                      CallbackInfo ci) {
        // Only fallow ground heals — if a crop is growing above, it's drawing nutrients out, not returning them.
        if (!(level.getBlockState(pos.above()).getBlock() instanceof CropBlock)
            && random.nextFloat() < RECOVERY_CHANCE) {
            SoilFertility.recover(level, pos, 1);
        }
    }
}
