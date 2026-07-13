package dev.alone.core;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Rooted vs loose (proposal §5.1/§5.4). A naturally-generated block is <b>rooted</b> — dirt tamped into
 * the ground, a tree fixed to the earth, stone packed in the crust — and slow to break. A block a player
 * <b>set down</b> is just resting there, <b>loose</b>, and quick to pick back up. We remember which
 * blocks were placed (per chunk, saved), and {@link dev.alone.core.mixin.BlockBehaviourLooseBreakMixin}
 * lets those loose ones break fast. Dig a log out of the ground (slow), set it down, and pulling it back
 * up is quick — but a rooted block stays stubborn.
 */
public final class PlacedBlocks {
    private PlacedBlocks() {
    }

    /** Packed {@link BlockPos#asLong() positions} within a chunk that a player set down. Synced to clients:
     *  break SPEED is computed client-side ({@code getDestroyProgress}), so every client — not just the one
     *  who placed the block — needs to know a block is loose, or a teammate would mine your set-down blocks
     *  at the slow rooted rate. The server stays authoritative; this just keeps the client's prediction (and
     *  the crack animation) honest for everyone. */
    public static final AttachmentType<Set<Long>> PLACED = AttachmentRegistry.<Set<Long>>builder()
        .persistent(Codec.LONG.listOf().xmap(HashSet::new, ArrayList::new))
        .syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG
                .apply(net.minecraft.network.codec.ByteBufCodecs.list())
                .map(HashSet::new, ArrayList::new),
            net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
        .buildAndRegister(Identifier.fromNamespaceAndPath("alone", "placed_blocks"));

    public static void init() {
        // A placed block that's broken is no longer placed — forget it, so a natural block later at the
        // same spot (or a re-placed one) starts from the right state.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> unmark(level, pos));
    }

    /** Remember that a player set this block down (it's loose now). */
    public static void markPlaced(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        Set<Long> existing = chunk.getAttached(PLACED);
        // Store a FRESH set instance (not the mutated old reference) so the attachment change is actually
        // detected, and mark the chunk unsaved so it's re-serialised — otherwise the "placed" flags were
        // lost across a world close/reopen (a set placed by the player looked rooted again on reload).
        Set<Long> set = existing == null ? new HashSet<>() : new HashSet<>(existing);
        if (set.add(pos.asLong())) {
            chunk.setAttached(PLACED, set);
            chunk.markUnsaved();
        }
    }

    /** Was this block set down by a player (loose), as opposed to naturally generated (rooted)? */
    public static boolean isPlaced(BlockGetter level, BlockPos pos) {
        if (!(level instanceof Level real)) {
            return false;
        }
        LevelChunk chunk = real.getChunkAt(pos);
        Set<Long> set = chunk.getAttached(PLACED);
        return set != null && set.contains(pos.asLong());
    }

    /** Commit a loose placed block back into rooted structure (it breaks slow again) — e.g. after tamping. */
    public static void reRoot(Level level, BlockPos pos) {
        unmark(level, pos);
    }

    private static void unmark(Level level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        Set<Long> existing = chunk.getAttached(PLACED);
        if (existing != null && existing.contains(pos.asLong())) {
            Set<Long> set = new HashSet<>(existing); // fresh instance + markUnsaved, so the removal persists
            set.remove(pos.asLong());
            chunk.setAttached(PLACED, set);
            chunk.markUnsaved();
        }
    }
}
