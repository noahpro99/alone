package dev.alone.core;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * The rope block (proposal §5.7) — a hanging climb line that can share its space with water, exactly
 * like seagrass or kelp. Placed next to other ropes, it dynamically connects to them horizontally and
 * vertically using UP, DOWN, NORTH, SOUTH, EAST, and WEST properties.
 */
public class RopeBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    public RopeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(WATERLOGGED, Boolean.FALSE)
            .setValue(UP, Boolean.FALSE)
            .setValue(DOWN, Boolean.FALSE)
            .setValue(NORTH, Boolean.FALSE)
            .setValue(SOUTH, Boolean.FALSE)
            .setValue(EAST, Boolean.FALSE)
            .setValue(WEST, Boolean.FALSE));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        state = state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        return getConnectionState(level, pos, state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource randomSource) {
        if (state.getValue(WATERLOGGED)) {
            scheduledTickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return getConnectionState(level, pos, state);
    }

    private BlockState getConnectionState(LevelReader level, BlockPos pos, BlockState state) {
        return state
            .setValue(UP, level.getBlockState(pos.above()).is(this))
            .setValue(DOWN, level.getBlockState(pos.below()).is(this))
            .setValue(NORTH, level.getBlockState(pos.north()).is(this))
            .setValue(SOUTH, level.getBlockState(pos.south()).is(this))
            .setValue(EAST, level.getBlockState(pos.east()).is(this))
            .setValue(WEST, level.getBlockState(pos.west()).is(this));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, UP, DOWN, NORTH, SOUTH, EAST, WEST);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /** Time to hand-reel one 1 m length. Coiling a metre is ~a second in real life, but a real second is
     *  a big slice of Minecraft's compressed day, so at the game's scale it's near-instant — a quick flick
     *  per length. Each break peels off the lowest length, so a long line just takes more (fast) breaks. */
    private static final int TICKS_PER_SEGMENT = 5;
    private static final int MAX_LINE = 512;

    /**
     * Breaking a rope <em>is</em> reeling it in (see {@link Ropes}). You can only do it from the <b>top</b>
     * of a line — a lower length won't break at all — and each break takes the same short time, peeling a
     * single length off the free bottom end. Hold the break on the top rope and the whole line walks back
     * into your pack a metre at a time.
     */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return pos.getY() < lineTopY(level, pos) ? 0f : 1.0f / TICKS_PER_SEGMENT;
    }

    /** The highest Y among the rope blocks connected (6-connected) to this one. */
    private int lineTopY(BlockGetter level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        seen.add(origin);
        queue.add(origin);
        int topY = origin.getY();
        while (!queue.isEmpty() && seen.size() < MAX_LINE) {
            BlockPos p = queue.poll();
            topY = Math.max(topY, p.getY());
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d).immutable();
                if (!seen.contains(n) && level.getBlockState(n).is(this)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return topY;
    }
}
