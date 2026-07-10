package dev.alone.core;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * World generation hooks (proposal §8.1). Scatters {@link AloneBlocks#LOOSE_ROCK loose rocks} across the
 * overworld surface like grass tufts, so stone is something you can spot and pick up from your first
 * minute — no tool, no digging. The feature itself is datapack-defined (worldgen/placed_feature/
 * loose_rocks.json); this just adds it to every overworld biome's vegetation pass.
 */
public final class WorldGen {
    private WorldGen() {
    }

    private static final ResourceKey<PlacedFeature> LOOSE_ROCKS = ResourceKey.create(
        Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("alone", "loose_rocks"));
    private static final ResourceKey<PlacedFeature> LOOSE_ROCKS_DENSE = ResourceKey.create(
        Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("alone", "loose_rocks_dense"));

    public static void init() {
        // A light scatter everywhere — you can find a stone in any biome.
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            LOOSE_ROCKS);
        // And a much denser scatter where the ground is bare rock and scree — peaks, windswept hills,
        // stony shores are strewn with loose stone, as they are in life.
        BiomeModifications.addFeature(
            BiomeSelectors.includeByKey(
                Biomes.STONY_PEAKS, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS,
                Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.STONY_SHORE),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            LOOSE_ROCKS_DENSE);
    }
}
