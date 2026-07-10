package dev.alone.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rope handling (proposal §5.7). Breaking the <b>top</b> of a hung line reels it in one length at a
 * time from the free bottom end — each break peels off the lowest length and returns one coil, so
 * holding the break walks the whole line back into your pack a metre a second. A lower length can't be
 * broken at all. Nothing litters the ground. (Placement/extension lives in {@link RopeItem}.)
 */
public final class Ropes {
    private Ropes() {
    }

    private static final int MAX_COLLECT = 512; // a runaway backstop; a real line is far shorter

    public static void init() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || !state.is(AloneBlocks.ROPE)) {
                return true; // not our rope (or client) — let vanilla handle it
            }
            if (player.isCreative()) {
                return true; // creative: just break the one block, no roll-up
            }
            // BFS the line from the broken block: its highest Y (only the top reels in) and the block
            // furthest along the rope — the true free end, following every sideways jog to the very tip.
            // BFS dequeues in distance order, so the last one out is the farthest.
            Set<BlockPos> seen = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            BlockPos origin = pos.immutable();
            seen.add(origin);
            queue.add(origin);
            int topY = origin.getY();
            BlockPos end = origin;
            while (!queue.isEmpty() && seen.size() < MAX_COLLECT) {
                BlockPos current = queue.poll();
                end = current;
                topY = Math.max(topY, current.getY());
                for (Direction dir : Direction.values()) {
                    BlockPos next = current.relative(dir).immutable();
                    if (!seen.contains(next) && level.getBlockState(next).is(AloneBlocks.ROPE)) {
                        seen.add(next);
                        queue.add(next);
                    }
                }
            }
            // You reel in from the anchor: only the top length works. A lower one won't break (its destroy
            // speed is zero), but guard here too so nothing can pull a rope from the middle.
            if (pos.getY() < topY) {
                return false; // not the top — leave the whole line hanging
            }
            // Peel off just the free end and return one coil; the top stays so another break takes the
            // next one up. Holding the break walks the whole line in a length at a time, from the tip up.
            level.removeBlock(end, false);
            giveOrDrop(player, 1);
            return false; // we handled the break
        });
    }

    private static void giveOrDrop(Player player, int count) {
        ItemStack coil = new ItemStack(AloneItems.ROPE, count);
        if (!player.getInventory().add(coil) && !coil.isEmpty()) {
            player.drop(coil, false); // pack full — the remainder falls at your feet
        }
    }
}
