package dev.alone.food;

import com.mojang.serialization.Codec;
import dev.alone.core.SurvivalMeters;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Food spoilage (proposal §4.2), driven by <b>temperature</b> — because where and when you keep food is
 * the whole game before refrigeration. Each perishable carries a freshness budget that drains faster in
 * the heat and slower in the cold: a warm summer hut rots meat in a day, but a <b>root cellar</b> (dug in,
 * lined against the caving earth, and cool from the ground) — or a cold biome, or winter — keeps it for
 * weeks. Preserving (salting/drying) just buys a much bigger budget, not immortality.
 *
 * <p>The budget drains by the <b>in-game time that actually elapses</b> — measured against the world
 * clock ({@link Level#getGameTime()}), not real seconds — so food <b>keeps rotting while you're away</b>.
 * Leave meat in a warm base and go exploring, and you come home to spoilage; leave it in a cold cellar
 * and it barely ticks. We only re-check a stack when it's near you again (carried, or in a loaded
 * container nearby), but the drain then covers the <b>whole</b> stretch of world-time since it was last
 * seen — nothing is paused or forgiven by leaving. When the budget runs out the food turns to rotten flesh.
 */
public final class Spoilage {
    private Spoilage() {
    }

    public static final long SPOIL_TICKS = 24000L;            // fresh perishable: ~1 in-game day at comfortable temp
    public static final long PRESERVED_SHELF_TICKS = 720000L; // salted/dried: ~30 in-game days — long, not forever
    private static final int SCAN_INTERVAL = 40;              // check food every ~2s

    /** Remaining freshness, in comfortable-temperature ticks; when it hits 0 the food spoils. */
    public static final DataComponentType<Long> FRESHNESS = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "freshness"),
        DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    /** World time the freshness was last stamped (drain since then is computed on the fly, not written
     *  every tick — that lazy read is what stops carried food flickering in your hand). */
    public static final DataComponentType<Long> FRESHNESS_SEEN = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "freshness_seen"),
        DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    /** The spoil-rate band it was stamped at, so we drain the elapsed time at the right rate and only
     *  re-stamp when the temperature band actually changes — not every scan. */
    public static final DataComponentType<Integer> RATE_BAND = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "freshness_band"),
        DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    /** Salted/dried food starts with the long shelf life (§4.2). */
    public static final DataComponentType<Boolean> PRESERVED = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "preserved"),
        DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** Dried (jerked) food specifically — the water's gone, so it's lighter to carry (read by Carry). */
    public static final DataComponentType<Boolean> DRIED = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "dried"),
        DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final TagKey<Item> PERISHABLE =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % SCAN_INTERVAL != 0) {
                    continue;
                }
                if (!(player.level() instanceof ServerLevel level)) {
                    continue;
                }
                long now = level.getGameTime();
                // On your body: food spoils at the temperature around YOU.
                float bodyTemp = SurvivalMeters.environmentTempAt(level, player.blockPosition());
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    tickStack(inventory, i, now, bodyTemp);
                }
                // In chests/barrels near you: food spoils at that container's temperature (cellar = cold).
                scanContainers(level, player.blockPosition(), now);
            }
        });
    }

    /** Sweep the loaded containers in the 3x3 chunks around the player — a base cellar keeps its food cool. */
    private static void scanContainers(ServerLevel level, BlockPos center, long now) {
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue() instanceof Container container) {
                        float temp = storageTemp(level, entry.getKey());
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            tickStack(container, i, now, temp);
                        }
                    }
                }
            }
        }
    }

    private static void tickStack(Container container, int slot, long now, float temp) {
        ItemStack stack = container.getItem(slot);
        if (stack.isEmpty() || !stack.is(PERISHABLE)) {
            return;
        }
        int band = tempBand(temp);
        Long freshness = stack.get(FRESHNESS);
        if (freshness == null) {
            stack.set(FRESHNESS, stack.getOrDefault(PRESERVED, false) ? PRESERVED_SHELF_TICKS : SPOIL_TICKS);
            stack.set(FRESHNESS_SEEN, now);
            stack.set(RATE_BAND, band);
            return;
        }
        // Drain is computed lazily: full world-time since it was stamped, at the rate of the band it was
        // stamped in — uncapped, so away-time still counts and food rots while you're gone.
        long elapsed = Math.max(0L, now - stack.getOrDefault(FRESHNESS_SEEN, now));
        long left = freshness - Math.round(elapsed * rateForBand(stack.getOrDefault(RATE_BAND, band)));
        if (left <= 0) {
            container.setItem(slot, new ItemStack(Items.ROTTEN_FLESH, stack.getCount())); // spoiled — swapped once
            return;
        }
        // Only WRITE the stack (which re-syncs it and would bob a held item) when the temperature band
        // actually changes: commit the drain so far and re-stamp at the new rate. On a stable temperature
        // nothing is written every scan, so carried meat no longer flickers.
        if (band != stack.getOrDefault(RATE_BAND, band)) {
            stack.set(FRESHNESS, left);
            stack.set(FRESHNESS_SEEN, now);
            stack.set(RATE_BAND, band);
        }
    }

    /** Temperature quantised to a spoil-rate band (25° per band = a doubling/halving), so a stack is only
     *  re-stamped when conditions meaningfully change — cold cellar, warm hut, winter — not every scan. */
    public static int tempBand(float temp) {
        return Math.max(-6, Math.min(6, Math.round(temp / 25.0f)));
    }

    /** Spoil rate for a band: doubles per +band, halves per -band, clamped like the old continuous rate. */
    public static float rateForBand(int band) {
        return Math.max(0.1f, Math.min(4.0f, (float) Math.pow(2.0, band)));
    }

    /** Storage temperature: a covered spot below sea level is an earth-cooled root cellar (colder the
     *  deeper); ice/snow packed around the store (an ice house) chills it too, even above ground; otherwise
     *  it's just the ambient temperature there. The coldest applicable source wins. */
    private static float storageTemp(Level level, BlockPos pos) {
        float base;
        if (!level.canSeeSky(pos) && pos.getY() < level.getSeaLevel()) {
            int depth = level.getSeaLevel() - pos.getY();
            base = -20f - Math.min(30f, depth * 0.5f); // ~-20 just under, down to ~-50 deep
        } else {
            base = SurvivalMeters.environmentTempAt(level, pos);
        }
        // Ice house: ice/snow packed against the container keeps it cold anywhere — a block against it is
        // ~-12°, packed in on every side approaches ~-40°. Haul lake ice into a dark store to hold a harvest.
        int ice = dev.alone.core.IceHouse.coldPacking(level, pos);
        if (ice > 0) {
            return Math.min(base, -8f - Math.min(32f, ice * 4f));
        }
        return base;
    }
}
