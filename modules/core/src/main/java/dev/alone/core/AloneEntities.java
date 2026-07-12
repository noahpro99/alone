package dev.alone.core;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.polarbear.PolarBear;

/**
 * Custom entities. The {@link TravoisEntity travois} (proposal §6 — transport) is a dragged cargo sled;
 * the {@link BrownBear brown bear} (§7.2) is the forest's big predator, a re-coated polar bear that
 * belongs in temperate/boreal woods where this game is set.
 */
public final class AloneEntities {
    private AloneEntities() {
    }

    public static final ResourceKey<EntityType<?>> TRAVOIS_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "travois"));

    public static final EntityType<TravoisEntity> TRAVOIS = EntityType.Builder
        .of(TravoisEntity::new, MobCategory.MISC)
        .sized(1.1f, 0.5f) // a low, wide sled
        .build(TRAVOIS_KEY);

    public static final ResourceKey<EntityType<?>> BROWN_BEAR_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "brown_bear"));

    public static final EntityType<BrownBear> BROWN_BEAR = EntityType.Builder
        .of(BrownBear::new, MobCategory.CREATURE)
        .sized(1.4f, 1.4f) // same bulk as the polar bear it reuses
        .build(BROWN_BEAR_KEY);

    public static final ResourceKey<EntityType<?>> DEER_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "deer"));

    public static final EntityType<Deer> DEER = EntityType.Builder
        .of(Deer::new, MobCategory.CREATURE)
        .sized(0.9f, 1.5f) // a lean, tall grazer (placeholder cow model for now)
        .build(DEER_KEY);

    /** Touching this class registers the entity types above. Called from {@link AloneCore}. */
    public static void init() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, TRAVOIS_KEY, TRAVOIS);

        Registry.register(BuiltInRegistries.ENTITY_TYPE, BROWN_BEAR_KEY, BROWN_BEAR);
        // It's a polar bear under the coat, so it wants the polar bear's attributes (health, damage, speed).
        FabricDefaultAttributeRegistry.register(BROWN_BEAR, PolarBear.createAttributes());
        // Home is the woods: temperate and boreal forest, and the taiga. Rare and near-solitary — a bear is
        // a landmark encounter, not a herd. (Spawn placement stays vanilla-default for now; tune in playtest.)
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(BiomeTags.IS_FOREST).or(BiomeSelectors.tag(BiomeTags.IS_TAIGA)),
            MobCategory.CREATURE, BROWN_BEAR, 6, 1, 1);

        Registry.register(BuiltInRegistries.ENTITY_TYPE, DEER_KEY, DEER);
        // Cow's grazing brain, but tuned to run FAST — a deer bolts and must be run down, not walked up to.
        FabricDefaultAttributeRegistry.register(DEER, Cow.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3));
        // The woods and their edges — forest, taiga, and grassland. Common wild game, in small herds.
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(BiomeTags.IS_FOREST).or(BiomeSelectors.tag(BiomeTags.IS_TAIGA))
                .or(BiomeSelectors.tag(BiomeTags.IS_HILL)),
            MobCategory.CREATURE, DEER, 12, 2, 4);
    }
}
