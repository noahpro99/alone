package dev.alone.core;

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

/**
 * A coil of rope (proposal §5.7). Aim at a block face near the top of a cliff and use it: the rope
 * <b>unrolls straight down</b> from that spot, filling the open air with climbable {@link AloneBlocks#ROPE}
 * — one length of rope per block, as far down as the coil reaches. Extending an existing line is
 * natural too: click the <b>top</b> of a rope to <b>lengthen it further down</b>, or a rope's <b>side</b>
 * to <b>grow it upward</b>. Then climb it up and down for free, no stamina. Break any part of a line to
 * roll the whole thing back into your pack (see {@link Ropes}).
 */
public class RopeItem extends Item {
    /** A single pass won't run forever — a sane maximum length per use. */
    private static final int MAX_ROPE = 32;

    public RopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState clickedState = level.getBlockState(clicked);

        BlockPos start;
        Direction run;
        if (clickedState.is(AloneBlocks.ROPE)) {
            // Extending a line: a side grows it upward, the top (or bottom) lengthens it downward.
            if (face.getAxis().isHorizontal()) {
                start = columnEnd(level, clicked, Direction.UP).above();
                run = Direction.UP;
            } else {
                start = columnEnd(level, clicked, Direction.DOWN).below();
                run = Direction.DOWN;
            }
        } else {
            // A fresh anchor on a solid block face — unroll straight down.
            start = face == Direction.DOWN ? clicked.below() : clicked.relative(face);
            run = Direction.DOWN;
        }

        if (!level.getBlockState(start).canBeReplaced()) {
            return InteractionResult.PASS; // no open air to run the rope into
        }

        Player player = context.getPlayer();
        ItemStack coil = context.getItemInHand();
        if (!level.isClientSide()) {
            int available = (player != null && player.isCreative()) ? MAX_ROPE : coil.getCount();
            int placed = 0;
            BlockPos.MutableBlockPos cursor = start.mutable();
            while (placed < available && placed < MAX_ROPE && level.getBlockState(cursor).canBeReplaced()) {
                level.setBlockAndUpdate(cursor.immutable(), AloneBlocks.ROPE.defaultBlockState());
                cursor.move(run);
                placed++;
            }
            if (placed == 0) {
                return InteractionResult.PASS;
            }
            if (player != null && !player.isCreative()) {
                coil.shrink(placed);
            }
            level.playSound(null, start, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7f, 0.9f);
        }
        return InteractionResult.SUCCESS;
    }

    /** Walk from a rope block to the far end of its contiguous vertical run in the given direction. */
    private static BlockPos columnEnd(Level level, BlockPos from, Direction dir) {
        BlockPos pos = from;
        while (level.getBlockState(pos.relative(dir)).is(AloneBlocks.ROPE)) {
            pos = pos.relative(dir);
        }
        return pos;
    }
}
