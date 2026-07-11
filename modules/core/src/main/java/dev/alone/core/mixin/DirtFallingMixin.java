package dev.alone.core.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Loose soil has no cohesion — undercut it and it collapses (proposal §5.3/§5.4). So dirt-family blocks
 * (dirt, grass, coarse dirt, podzol, mud…) now <b>fall like gravel</b> when nothing holds them up — <b>but,
 * like real mining, timber holds up the roof</b>: undercut soil within {@link #SUPPORT_SPAN} blocks of a
 * grounded structural support (a placed beam, post, plank, stone wall — anything but more loose soil) stays
 * put, so you can shore a cellar or tunnel and the span between supports holds. Clay is left alone (it's
 * cohesive), and sand/gravel already fall. This grafts {@link FallingBlock}'s schedule-on-change /
 * fall-on-tick behaviour onto soil via the shared {@link BlockBehaviour} hooks (gated to soil).
 */
@Mixin(BlockBehaviour.class)
public class DirtFallingMixin {
    /** How far undercut soil reaches from a support before it caves — a modest unreinforced span. */
    private static final int SUPPORT_SPAN = 4;

    private static boolean alone$soil(BlockState state) {
        return state.is(BlockTags.DIRT);
    }

    /** Is there a grounded structural support within {@link #SUPPORT_SPAN} blocks horizontally? */
    private static boolean alone$shored(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            p.set(pos);
            for (int i = 1; i <= SUPPORT_SPAN; i++) {
                p.move(d);
                if (alone$isSupport(level, p)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A support can bear earth: a solid, non-soil, non-falling block that is itself grounded (stands on
     *  something) — a beam/post/wall, or a block of <b>tamped rammed earth</b> ({@link dev.alone.core.Tamping}).
     *  Loose soil can't hold soil, so you shore with timber, stone, or earth you've packed firm. */
    private static boolean alone$isSupport(ServerLevel level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (alone$soil(s)) {
            // Rammed earth bears load like a post; loose soil can't hold soil.
            return dev.alone.core.Tamping.isTamped(level, p) && !level.getBlockState(p.below()).isAir();
        }
        if (s.isAir() || s.getBlock() instanceof FallingBlock) {
            return false;
        }
        if (s.getCollisionShape(level, p).isEmpty()) {
            return false; // no solid body (a plant, torch, rail…) — can't hold up earth
        }
        return !level.getBlockState(p.below()).isAir(); // grounded, part of a post/wall — not floating
    }

    @Inject(method = "onPlace", at = @At("TAIL"))
    private void alone$scheduleOnPlace(BlockState state, Level level, BlockPos pos, BlockState oldState,
                                       boolean movedByPiston, CallbackInfo ci) {
        if (alone$soil(state)) {
            level.scheduleTick(pos, state.getBlock(), 2);
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"))
    private void alone$scheduleOnUpdate(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                        BlockPos pos, Direction direction, BlockPos neighborPos,
                                        BlockState neighborState, RandomSource random,
                                        CallbackInfoReturnable<BlockState> cir) {
        if (alone$soil(state)) {
            tickAccess.scheduleTick(pos, state.getBlock(), 2);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void alone$fall(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                            CallbackInfo ci) {
        if (alone$soil(state) && pos.getY() >= level.getMinY()
            && FallingBlock.isFree(level.getBlockState(pos.below()))
            && !dev.alone.core.Tamping.isTamped(level, pos) // rammed earth holds itself up
            && !alone$shored(level, pos)) { // timber/stone/rammed earth within span holds the roof up
            FallingBlockEntity.fall(level, pos, state);
        }
    }
}
