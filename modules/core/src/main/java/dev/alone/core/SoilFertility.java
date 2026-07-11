package dev.alone.core;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Soil fertility &amp; crop rotation (proposal §4.1). Real ground is not an infinite larder: a crop draws
 * nutrients out of the soil, and taking the <b>same crop off the same plot over and over</b> exhausts it —
 * growth slows, then the crop fails outright on the worn-out ground. You restore fertility the way farmers
 * did for millennia, before any bagged fertiliser:
 * <ul>
 *   <li><b>Let it lie fallow</b> — an empty tilled plot slowly heals as nutrients return (see the farmland
 *       tick).</li>
 *   <li><b>Rotate your crops</b> — monoculture is what really drains a field, so following one crop with a
 *       <em>different</em> one is far gentler than repeating it.</li>
 * </ul>
 * Fertility (0..100) and the last crop taken are stored per farmland tile as a <b>chunk attachment</b>, so
 * they persist and travel with the land. Depletion happens on harvest; the {@code CropGrowthMixin} reads
 * fertility to slow growth and raise the death chance on tired soil.
 */
public final class SoilFertility {
    private SoilFertility() {
    }

    public static final int FRESH = 100;         // virgin, well-rested soil
    public static final int LOW = 30;            // below this, crops start to struggle
    private static final int HARVEST_DRAW = 18;  // fertility a harvest pulls out — a crop is a heavy feeder
    private static final int MONOCULTURE_EXTRA = 10; // repeating the same crop drains extra

    private static final Codec<Map<Long, Integer>> POS_MAP = Codec.unboundedMap(
        Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.INT);

    /** Fertility 0..100 per farmland tile (a tile absent from the map is {@link #FRESH}). */
    public static final AttachmentType<Map<Long, Integer>> FERTILITY = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "soil_fertility"), POS_MAP);
    /** The last crop harvested off each tile (block registry id), for rotation checks (absent = none). */
    public static final AttachmentType<Map<Long, Integer>> LAST_CROP = AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("alone", "soil_last_crop"), POS_MAP);

    public static void init() {
        // Harvesting a mature crop draws nutrients out of the soil below it, and records what grew there.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide()) {
                return;
            }
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                onHarvest(level, pos.below(), state.getBlock());
            }
        });
    }

    public static int fertility(Level level, BlockPos farmland) {
        Map<Long, Integer> map = level.getChunkAt(farmland).getAttachedOrElse(FERTILITY, Map.of());
        return map.getOrDefault(farmland.asLong(), FRESH);
    }

    /** How exhausted the soil is, 0 (fine) .. 1 (dead), for scaling growth/death penalties. */
    public static float exhaustion(Level level, BlockPos farmland) {
        int f = fertility(level, farmland);
        return f >= LOW ? 0f : (LOW - f) / (float) LOW;
    }

    /** Same crop as the last one taken off this tile? Monoculture — what really wears a field out. */
    public static boolean isMonoculture(Level level, BlockPos farmland, Block crop) {
        Map<Long, Integer> map = level.getChunkAt(farmland).getAttachedOrElse(LAST_CROP, Map.of());
        return map.getOrDefault(farmland.asLong(), -1) == BuiltInRegistries.BLOCK.getId(crop);
    }

    /** A harvest draws fertility out — more if it repeats the last crop — and records the crop for rotation. */
    public static void onHarvest(Level level, BlockPos farmland, Block crop) {
        if (!level.getBlockState(farmland).is(Blocks.FARMLAND)) {
            return; // only tilled ground keeps a fertility ledger
        }
        int draw = HARVEST_DRAW + (isMonoculture(level, farmland, crop) ? MONOCULTURE_EXTRA : 0);
        LevelChunk chunk = level.getChunkAt(farmland);
        put(chunk, FERTILITY, farmland, Math.max(0, fertility(level, farmland) - draw));
        put(chunk, LAST_CROP, farmland, BuiltInRegistries.BLOCK.getId(crop));
    }

    /** Fallow ground heals — nutrients return slowly when nothing is drawing them out. */
    public static void recover(Level level, BlockPos farmland, int amount) {
        int f = fertility(level, farmland);
        if (f < FRESH) {
            put(level.getChunkAt(farmland), FERTILITY, farmland, Math.min(FRESH, f + amount));
        }
    }

    private static void put(LevelChunk chunk, AttachmentType<Map<Long, Integer>> which, BlockPos pos, int value) {
        Map<Long, Integer> map = new HashMap<>(chunk.getAttachedOrElse(which, Map.of()));
        map.put(pos.asLong(), value);
        chunk.setAttached(which, map);
    }
}
