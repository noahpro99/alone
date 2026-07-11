package dev.alone.core.mixin;

import dev.alone.core.PlacedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A loose (player-placed) block is quick to pull back up — it's just resting, not rooted (proposal
 * §5.1/§5.4). This overrides the slow "rooted" break time (from {@code PlayerDestroySpeedMixin}) for
 * anything a player set down, so it pops in a few ticks. Naturally-generated terrain keeps its slow time.
 */
@Mixin(BlockBehaviour.class)
public class BlockBehaviourLooseBreakMixin {
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void alone$looseBreak(BlockState state, Player player, BlockGetter level, BlockPos pos,
                                  CallbackInfoReturnable<Float> cir) {
        float original = cir.getReturnValueF();
        if (PlacedBlocks.isPlaced(level, pos)) {
            // Your own set-down block comes up fast, no tool needed — even for logs/stone whose rooted
            // rules zero out the by-hand speed. It's just resting there; you're picking it up, not uprooting.
            cir.setReturnValue(Math.max(original, 0.34f)); // loose — comes up in ~3 ticks
        }
    }
}
