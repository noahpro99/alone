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
 * (dirt, grass, coarse dirt, podzol, mud…) now <b>fall like gravel</b> when nothing holds them up: you
 * can't tunnel a dugout shelter, because the earth caves in behind you. Clay is left alone (it's
 * cohesive, which is why you can build a bloomery from it), and sand/gravel already fall. This grafts
 * {@link FallingBlock}'s schedule-on-change / fall-on-tick behaviour onto ordinary soil via the shared
 * {@link BlockBehaviour} hooks (gated so only soil is affected; blocks that override these run their own).
 */
@Mixin(BlockBehaviour.class)
public class DirtFallingMixin {
    private static boolean alone$soil(BlockState state) {
        return state.is(BlockTags.DIRT);
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
            && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            FallingBlockEntity.fall(level, pos, state);
        }
    }
}
