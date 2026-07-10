package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Loose rocks (proposal §8.1) — a scatter of stones lying on the ground, generated across the surface
 * like grass tufts (see {@link WorldGen}). No collision, breaks instantly by hand, and drops a
 * {@link AloneItems#ROCK} to knap with — the day-one way to gather stone without a tool. It sits on any
 * solid top and pops off the moment its floor goes.
 */
public class LooseRockBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 1.5, 14.0);

    public LooseRockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
