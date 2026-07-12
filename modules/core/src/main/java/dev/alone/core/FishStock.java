package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Finite, slow-recovering fish stocks (proposal §4.1 / roadmap: fishing as a food source). A body of water
 * isn't a bottomless larder: pull fish out of one spot fast enough and you <b>fish it out</b> — the catches
 * dry up until the stock recovers over the following days, or you move to fresh water. How much water
 * actually surrounds the spot sets how rich it is: a shallow puddle thins out in a few casts and barely
 * comes back, while open water is deep, plentiful, and restocks fast. Tracked as a per-chunk stock (0..{@link #FULL}) on
 * a chunk attachment, recovered lazily from the last time it was fished, so there's no periodic bookkeeping.
 * The gate hangs off the vanilla fishing loot via {@link LootTableEvents#MODIFY_DROPS} — no rod internals.
 */
public final class FishStock {
    private FishStock() {
    }

    public static final int FULL = 100;
    private static final int CATCH_COST = 8;              // each fish taken — ~12 from a fresh spot before it thins
    private static final int RECOVERY_TICKS_PER_POINT = 600; // ~2.5 in-game days for a fished-out spot to fully return

    // How much water actually surrounds the spot decides how rich it is. A tiny puddle holds few fish and
    // has no connected body to restock it; open water is deep and fed by the whole lake or sea. We survey a
    // box around the bobber, count water, and scale both the draw per catch (poor water thins fast) and the
    // recovery rate (poor water barely comes back) by it.
    private static final int SURVEY_RADIUS = 4;          // horizontal reach of the water survey around the bobber
    private static final int SURVEY_DEPTH = 3;           // and how far down — fish need water beneath them, not a film
    private static final float OPEN_WATER = 150f;        // water blocks in the survey that count as fully "open water"
    private static final float MIN_RICHNESS = 0.25f;     // a puddle still fishes, just poorly (draws 4x, recovers 4x slower)

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

    /** A set fish trap drew on the water here — the same finite, richness-scaled stock the rod draws on.
     *  Returns true (and depletes the spot) if it caught; false if this water is fished out. */
    public static boolean drawFromTrap(ServerLevel level, BlockPos pos) {
        return tryCatch(level, pos);
    }

    /** How much open, deep water a spot needs before a gill net will fish it — below this it's a shore or a
     *  puddle where a net can't span (a weir still works there). */
    private static final float OPEN_WATER_MIN = 0.5f;

    /** Whether this spot is open, deep-enough water to string a gill net across, as opposed to a shore or
     *  narrow creek (where the fixed weir is the tool instead). */
    public static boolean isOpenWater(ServerLevel level, BlockPos pos) {
        return richness(level, pos) >= OPEN_WATER_MIN;
    }

    /** Draw a fish from the spot if the stock allows; otherwise deny. Fish return over calendar time, so a
     *  fished-out spot recovers whether or not you keep casting — we only touch state on an actual catch,
     *  and recovery accrues from the last CATCH (not every futile cast, which must not reset the clock). */
    private static boolean tryCatch(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        float richness = richness(level, pos);
        int cost = Math.max(1, Math.round(CATCH_COST / richness));           // poorer water — each fish costs more
        long recoveryPerPoint = (long) (RECOVERY_TICKS_PER_POINT / richness); // and comes back slower
        long now = level.getGameTime();
        long last = chunk.getAttachedOrElse(LAST_FISHED, now);
        int stock = Math.min(FULL, chunk.getAttachedOrElse(STOCK, FULL)
            + (int) (Math.max(0L, now - last) / recoveryPerPoint));
        if (stock < cost) {
            return false; // fished out — leave the clock alone so it keeps recovering while you cast in vain
        }
        chunk.setAttached(STOCK, stock - cost);
        chunk.setAttached(LAST_FISHED, now);
        return true;
    }

    /** How rich the spot is (0.25..1): the share of a survey box around the bobber that's water. A puddle
     *  scores near the floor and fishes out fast with little recovery; open sea scores 1 and lasts. */
    private static float richness(ServerLevel level, BlockPos pos) {
        int water = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -SURVEY_RADIUS; dx <= SURVEY_RADIUS; dx++) {
            for (int dz = -SURVEY_RADIUS; dz <= SURVEY_RADIUS; dz++) {
                for (int dy = -SURVEY_DEPTH; dy <= 0; dy++) {
                    m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getFluidState(m).is(FluidTags.WATER)) {
                        water++;
                    }
                }
            }
        }
        return Mth.clamp(water / OPEN_WATER, MIN_RICHNESS, 1f);
    }
}
