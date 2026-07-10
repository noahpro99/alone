package dev.alone.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * A coil of rope (proposal §5.7). Aim at a block face and use it to <b>anchor a rope that hangs down</b>
 * one block; click the <b>top</b> of that line to <b>pay out one more length</b>. Rope only ever heads
 * down — but if the space straight below is blocked (a ledge, an overhang), it <b>routes sideways</b>
 * into an open neighbour so the line can work its way around the obstruction and keep descending. Climb
 * it up and down for free (crouch to descend). Break any part of a line to roll the whole thing back
 * into your pack (see {@link Ropes}).
 */
public class RopeItem extends Item {
    /** Safety bound on how big a connected rope line we'll trace. */
    private static final int MAX_LINE = 512;

    public RopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        BlockPos target;
        if (clickedState.is(AloneBlocks.ROPE)) {
            // You pay rope out from the anchor at the top: only the highest length of a line accepts more.
            // Clicking a lower length (say, while climbing) does nothing — go back to the top to feed out.
            LineScan scan = scanFrom(level, clicked);
            if (clicked.getY() != scan.topY()) {
                return InteractionResult.PASS; // not the top of the line
            }
            target = payOut(level, scan.end());
        } else {
            BlockPos hung = clicked.relative(context.getClickedFace());
            target = level.getBlockState(hung).canBeReplaced() ? hung : null;
        }
        if (target == null) {
            return InteractionResult.PASS; // nowhere open to run the rope into
        }

        Player player = context.getPlayer();
        ItemStack coil = context.getItemInHand();
        if (!level.isClientSide()) {
            // If the rope runs into water (a flooded cave, a lake off a dock), keep the water it hangs
            // in rather than carving a dry pocket — waterlogged, just like seagrass or kelp.
            boolean inWater = level.getFluidState(target).getType() == Fluids.WATER;
            level.setBlockAndUpdate(target,
                AloneBlocks.ROPE.defaultBlockState().setValue(RopeBlock.WATERLOGGED, inWater));
            if (player != null && !player.isCreative()) {
                coil.shrink(1);
            }
            level.playSound(null, target, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7f, 0.9f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Where the next length should go: straight down if that's open, else sideways into an open
     *  neighbour so the line can route around a ledge. Null if it's boxed in on all sides. The sideways
     *  pick is deterministic from position so client and server agree on which way it jogs. */
    private static BlockPos payOut(Level level, BlockPos end) {
        BlockPos down = end.below();
        if (level.getBlockState(down).canBeReplaced()) {
            return down;
        }
        List<BlockPos> open = new ArrayList<>();
        for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            BlockPos side = end.relative(d);
            if (level.getBlockState(side).canBeReplaced() && !foldsBack(level, side, end)) {
                open.add(side);
            }
        }
        if (open.isEmpty()) {
            return null; // boxed in — don't force a length onto the top; the line just stops here
        }
        return open.get(Math.floorMod((int) (end.asLong() >> 2), open.size()));
    }

    /** A sideways cell that touches any rope other than the end we're growing from would double the line
     *  back on itself — skip it, so a jog always heads out into clear space, never back toward the line. */
    private static boolean foldsBack(Level level, BlockPos candidate, BlockPos end) {
        for (Direction d : Direction.values()) {
            BlockPos n = candidate.relative(d);
            if (!n.equals(end) && level.getBlockState(n).is(AloneBlocks.ROPE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * BFS the connected line from where you clicked. Returns the highest Y in the line (to enforce "add
     * from the top") and the block <b>furthest along the rope</b> from here — the true growing end. Because
     * BFS dequeues in order of increasing distance, the last block out is the farthest, so a line that
     * jogs sideways at the bottom grows from the far end of the jog, not the nearest low block.
     */
    private static LineScan scanFrom(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        seen.add(origin);
        queue.add(origin);
        int topY = origin.getY();
        BlockPos end = origin;
        while (!queue.isEmpty() && seen.size() < MAX_LINE) {
            BlockPos pos = queue.poll();
            end = pos; // last dequeued = greatest BFS distance = furthest end (sideways included)
            topY = Math.max(topY, pos.getY());
            for (Direction d : Direction.values()) {
                BlockPos n = pos.relative(d).immutable();
                if (!seen.contains(n) && level.getBlockState(n).is(AloneBlocks.ROPE)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return new LineScan(topY, end);
    }

    private record LineScan(int topY, BlockPos end) {
    }
}
