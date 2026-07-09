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
 * Leaves behave like <b>scaffolding</b> for players: you can stand on top of a canopy, but you pass
 * (and climb, see {@link dev.alone.core.Climbing}) up through it from inside, and drop through it by
 * crouching. Standing on top gives a solid box; being inside gives none. This is narrowed to
 * <b>players</b> — mobs and everything else still treat foliage as solid.
 */
@Mixin(BlockBehaviour.class)
public class LeavesCollisionMixin {
    @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
    private void alone$leavesScaffold(BlockState state, BlockGetter level, BlockPos pos,
                                      CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        // Check the entity/player context FIRST: block-collision caching runs at bootstrap with an
        // empty context, before tags are bound — touching BlockTags.LEAVES there throws "Tags not
        // bound". The player context only exists at runtime, when tags are safely available.
        if (!(context instanceof EntityCollisionContext ecc) || !(ecc.getEntity() instanceof Player)
            || !state.is(BlockTags.LEAVES)) {
            return;
        }
        // Standing on top (and not crouch-descending) → solid, so you can walk across a treetop.
        // Otherwise you're inside/below it → no collision, so you push and climb up through the
        // canopy (or sink down through it while sneaking).
        if (context.isAbove(Shapes.block(), pos, true) && !context.isDescending()) {
            cir.setReturnValue(Shapes.block());
        } else {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
