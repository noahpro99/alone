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
 * Rope handling (proposal §5.7). Breaking any part of a hung line <b>rolls the whole connected run
 * back into your pack</b> — you recover every length at once instead of picking off one block at a
 * time, and nothing litters the ground. (Placement/extension lives in {@link RopeItem}.)
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
            Set<BlockPos> line = collectConnected(level, pos);
            for (BlockPos p : line) {
                level.removeBlock(p, false); // pull it all down with no drops
            }
            giveOrDrop(player, line.size());
            return false; // we handled the break
        });
    }

    /** Flood-fill every rope block reachable (6-connected) from the broken one — the whole line. */
    private static Set<BlockPos> collectConnected(Level level, BlockPos start) {
        Set<BlockPos> found = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < MAX_COLLECT) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!found.contains(next) && level.getBlockState(next).is(AloneBlocks.ROPE)) {
                    found.add(next);
                    queue.add(next);
                }
            }
        }
        return found;
    }

    private static void giveOrDrop(Player player, int count) {
        ItemStack coil = new ItemStack(AloneItems.ROPE, count);
        if (!player.getInventory().add(coil) && !coil.isEmpty()) {
            player.drop(coil, false); // pack full — the remainder falls at your feet
        }
    }
}
