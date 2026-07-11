package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Finite, slow-recovering fish stocks (proposal §4.1 / roadmap: fishing as a food source). A body of water
 * isn't a bottomless larder: pull fish out of one spot fast enough and you <b>fish it out</b> — the catches
 * dry up until the stock recovers over the following days, or you move to fresh water. A small pond empties
 * quickly; a lake or the sea is many spots, so it lasts. Tracked as a per-chunk stock (0..{@link #FULL}) on
 * a chunk attachment, recovered lazily from the last time it was fished, so there's no periodic bookkeeping.
 * The gate hangs off the vanilla fishing loot via {@link LootTableEvents#MODIFY_DROPS} — no rod internals.
 */
public final class FishStock {
    private FishStock() {
    }

    public static final int FULL = 100;
    private static final int CATCH_COST = 8;              // each fish taken — ~12 from a fresh spot before it thins
    private static final int RECOVERY_TICKS_PER_POINT = 600; // ~2.5 in-game days for a fished-out spot to fully return

    public static final AttachmentType<Integer> STOCK = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "fish_stock"), Codec.INT);
    public static final AttachmentType<Long> LAST_FISHED = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "fish_last_fished"), Codec.LONG);

    public static void init() {
        LootTableEvents.MODIFY_DROPS.register((key, context, drops) -> {
            if (!key.is(BuiltInLootTables.FISHING) || !context.hasParameter(LootContextParams.ORIGIN)) {
                return;
            }
            if (drops.stream().noneMatch(FishStock::isFish)) {
                return; // a junk or treasure catch — no fish to gate
            }
            Vec3 origin = context.getParameter(LootContextParams.ORIGIN);
            if (!tryCatch(context.getLevel(), BlockPos.containing(origin))) {
                drops.removeIf(FishStock::isFish); // this spot's fished out — the fish just aren't biting
            }
        });
    }

    private static boolean isFish(ItemStack stack) {
        return stack.is(ItemTags.FISHES);
    }

    /** Draw a fish from the spot if the stock allows; otherwise deny. Recovers the stock lazily first. */
    private static boolean tryCatch(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        long now = level.getGameTime();
        long last = chunk.getAttachedOrElse(LAST_FISHED, now);
        int stock = chunk.getAttachedOrElse(STOCK, FULL);
        // Fish return while you're away: recover a point for every so many ticks since it was last fished.
        long elapsed = Math.max(0L, now - last);
        stock = Math.min(FULL, stock + (int) (elapsed / RECOVERY_TICKS_PER_POINT));
        chunk.setAttached(LAST_FISHED, now);
        if (stock < CATCH_COST) {
            chunk.setAttached(STOCK, stock);
            return false; // fished out
        }
        chunk.setAttached(STOCK, stock - CATCH_COST);
        return true;
    }
}
