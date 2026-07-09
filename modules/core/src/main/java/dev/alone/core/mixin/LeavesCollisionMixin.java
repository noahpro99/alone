package dev.alone.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Leaves are soft — a player can push through a canopy (and climb up through it, see
 * {@link dev.alone.core.Climbing}) instead of standing on it like solid ground. We drop the
 * collision box for <b>players only</b> when the block is leaves; mobs and everything else still
 * treat foliage as solid, so this is a deliberately narrow change.
 */
@Mixin(BlockBehaviour.class)
public class LeavesCollisionMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void alone$leavesPassable(BlockState state, BlockGetter level, BlockPos pos,
                                      CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        // Check the entity/player context FIRST: block-collision caching runs at bootstrap with an
        // empty context, before tags are bound — touching BlockTags.LEAVES there throws "Tags not
        // bound". The player context only exists at runtime, when tags are safely available.
        if (context instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Player
            && state.is(BlockTags.LEAVES)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
