package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A coil of rope (proposal §5.7). Aim at a block face near the top of a cliff and use it: the rope
 * <b>unrolls straight down</b> from that spot, filling the open air with climbable {@link AloneBlocks#ROPE}
 * — one length of rope per block, as far down as the coil reaches (or until it hits something). Then
 * climb it up and down for free, no stamina, unlike free-climbing a bare face. Break the rope to
 * recover it. The natural gesture is to click the <em>vertical face</em> of the cliff's top block.
 */
public class RopeItem extends Item {
    /** A single coil won't drop forever — a sane maximum length. */
    private static final int MAX_ROPE = 32;

    public RopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        // Anchor just off the face you clicked, then drop straight down.
        BlockPos start = face == Direction.DOWN
            ? context.getClickedPos().below()
            : context.getClickedPos().relative(face);
        if (!level.getBlockState(start).canBeReplaced()) {
            return InteractionResult.PASS; // no open air to hang the rope in
        }

        Player player = context.getPlayer();
        ItemStack coil = context.getItemInHand();
        int available = (player != null && player.isCreative()) ? MAX_ROPE : coil.getCount();

        if (!level.isClientSide()) {
            BlockPos.MutableBlockPos cursor = start.mutable();
            int placed = 0;
            while (placed < available && placed < MAX_ROPE
                && level.getBlockState(cursor).canBeReplaced()) {
                level.setBlockAndUpdate(cursor.immutable(), AloneBlocks.ROPE.defaultBlockState());
                cursor.move(Direction.DOWN);
                placed++;
            }
            if (placed == 0) {
                return InteractionResult.PASS;
            }
            if (player != null && !player.isCreative()) {
                coil.shrink(placed);
            }
            level.playSound(null, start, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7f, 0.9f);
            if (player != null && placed >= available && placed < heightBelow(level, start)) {
                player.sendSystemMessage(Component.literal("The rope runs out before the bottom."));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** How far it is to the first solid block below the anchor (for the "ran out" hint). */
    private static int heightBelow(Level level, BlockPos start) {
        int h = 0;
        BlockPos.MutableBlockPos cursor = start.mutable();
        while (h < MAX_ROPE && level.getBlockState(cursor).canBeReplaced()) {
            cursor.move(Direction.DOWN);
            h++;
        }
        return h;
    }
}
