package dev.alone.core;

import net.minecraft.core.BlockPos;
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
 * A coil of rope (proposal §5.7). Aim at a block face and use it to <b>anchor a rope that hangs down</b>
 * one block; click the rope again (or hold) to <b>add one more length below it</b>, paying it out down
 * the face a block at a time — rope only ever goes down. Climb it up and down for free (crouch to
 * descend). Break any part of a line to roll the whole thing back into your pack (see {@link Ropes}).
 */
public class RopeItem extends Item {
    public RopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        // On an existing rope: pay out one more length below the bottom of that line. On a solid block:
        // anchor a single length hanging off the face you clicked (a wall face hangs it down the wall).
        BlockPos target = clickedState.is(AloneBlocks.ROPE)
            ? columnBottom(level, clicked).below()
            : clicked.relative(context.getClickedFace());

        if (!level.getBlockState(target).canBeReplaced()) {
            return InteractionResult.PASS; // no open air there to run the rope into
        }

        Player player = context.getPlayer();
        ItemStack coil = context.getItemInHand();
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(target, AloneBlocks.ROPE.defaultBlockState());
            if (player != null && !player.isCreative()) {
                coil.shrink(1);
            }
            level.playSound(null, target, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7f, 0.9f);
        }
        return InteractionResult.SUCCESS;
    }

    /** The lowest rope block of the contiguous vertical line through this one. */
    private static BlockPos columnBottom(Level level, BlockPos from) {
        BlockPos pos = from;
        while (level.getBlockState(pos.below()).is(AloneBlocks.ROPE)) {
            pos = pos.below();
        }
        return pos;
    }
}
