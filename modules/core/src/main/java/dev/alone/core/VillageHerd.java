package dev.alone.core;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;

/**
 * The village livestock roster and its <b>Mesopotamian herd proportions</b>. Domestic animals are
 * vetoed away from villages ({@link Domestic}/{@code MobSpawnMixin}), so the biome spawn list only ever
 * fields them <em>at</em> a settlement — which means the numbers below are exactly what you meet walking
 * up to a village: mostly <b>sheep and goats</b> (the staple flocks), fewer cattle and pigs, a
 * <b>donkey</b> or two at the yard, and some chickens underfoot.
 *
 * <p>Why add spawns at all when the veto already confines them? Two reasons. First, weight: vanilla
 * plains list sheep, cow, pig and chicken at flat, roughly-equal weights and has <b>no goat or donkey</b>
 * — so an untouched village herd would be an even barnyard mix, not a flock. We stack heavy sheep/goat
 * entries on top so the staple flocks dominate the weighted draw. Second, coverage: <b>desert has no
 * passive-animal spawns at all</b> in vanilla, yet desert villages exist — without an explicit roster a
 * desert (Mesopotamian!) village would stand empty of livestock. Adding the roster to the warm/dry
 * village biomes fixes both.
 *
 * <p>The <b>horse</b> is pointedly absent: it is NOT domestic, NOT vetoed, and keeps its untouched
 * vanilla plains spawn — wild horses roaming the fields represent a different region, not village stock.
 *
 * <p>Group sizes and weights (per {@code addSpawn}, which <em>adds</em> to — never replaces — the
 * vanilla list, so these stack on top of any base entry):
 * <ul>
 *   <li><b>Sheep</b> — COMMON: weight 30, flocks of 4-6</li>
 *   <li><b>Goat</b> — COMMON: weight 26, flocks of 4-6</li>
 *   <li><b>Chicken</b> — SOME: weight 8, 3-5</li>
 *   <li><b>Pig</b> — SOME: weight 7, 1-3</li>
 *   <li><b>Cow</b> — UNCOMMON: weight 5, 1-3</li>
 *   <li><b>Donkey</b> — FEW: weight 3, 1-2</li>
 * </ul>
 */
public final class VillageHerd {
    private VillageHerd() {
    }

    private static final MobCategory HERD = MobCategory.CREATURE;

    public static void init() {
        // The warm/dry village biomes — the pasture, savanna and desert-edge country a Mesopotamian
        // settlement sits in. Snowy/taiga villages keep their vanilla (non-flock) barnyard: a different
        // region. Domestics are vetoed away from villages, so offering the roster across the whole biome
        // costs nothing in the wilds — it only ever lands at a settlement.
        var villageCountry = BiomeSelectors.includeByKey(
            Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
            Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA,
            Biomes.DESERT);

        // The staple flocks — sheep and goats dominate the weighted draw and travel in numbers.
        addHerd(villageCountry, EntityTypes.SHEEP, 30, 4, 6);
        addHerd(villageCountry, EntityTypes.GOAT, 26, 4, 6);
        // Yard birds and the odd pig — present, not dominant.
        addHerd(villageCountry, EntityTypes.CHICKEN, 8, 3, 5);
        addHerd(villageCountry, EntityTypes.PIG, 7, 1, 3);
        // A little cattle, and a donkey or two at the edge of the pens.
        addHerd(villageCountry, EntityTypes.COW, 5, 1, 3);
        addHerd(villageCountry, EntityTypes.DONKEY, 3, 1, 2);
        // NOTE: horse is intentionally untouched — it stays wild on the plains (see class doc).
    }

    private static void addHerd(java.util.function.Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> where,
                                EntityType<?> type, int weight, int min, int max) {
        BiomeModifications.addSpawn(where, HERD, type, weight, min, max);
    }
}
