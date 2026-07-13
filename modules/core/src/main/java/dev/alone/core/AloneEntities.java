package dev.alone.core;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
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
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.rabbit.Rabbit;

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
        .sized(0.6f, 1.2f) // a slender, wild grazer (scaled placeholder model)
        .build(DEER_KEY);

    public static final ResourceKey<EntityType<?>> SQUIRREL_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "squirrel"));

    public static final EntityType<Squirrel> SQUIRREL = EntityType.Builder
        .of(Squirrel::new, MobCategory.CREATURE)
        .sized(0.4f, 0.5f) // small game — rabbit-sized (placeholder rabbit model for now)
        .build(SQUIRREL_KEY);

    public static final ResourceKey<EntityType<?>> WILD_BOAR_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "wild_boar"));

    public static final EntityType<WildBoar> WILD_BOAR = EntityType.Builder
        .of(WildBoar::new, MobCategory.CREATURE)
        .sized(0.9f, 0.9f) // a shade bigger and squarer than a pig (0.9×0.9 vs the pig's 0.9×0.9 build)
        .build(WILD_BOAR_KEY);

    public static final ResourceKey<EntityType<?>> BISON_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "bison"));

    public static final EntityType<Bison> BISON = EntityType.Builder
        .of(Bison::new, MobCategory.CREATURE)
        .sized(1.4f, 1.9f) // a ton of wild bovine — much bigger than the cow's 0.9×1.4 (model scaled to match)
        .build(BISON_KEY);

    public static final ResourceKey<EntityType<?>> THROWN_ROCK_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "thrown_rock"));

    /** A hand-thrown {@link AloneItems#ROCK loose rock} (§8.1) — the ranged tier below the slingshot; see
     *  {@link ThrownRock}. A small, short-lived projectile: MISC (no AI/attributes), sized like the pebble
     *  it is. Tracked often (updateInterval 10) with velocity so its arc renders smoothly. */
    public static final EntityType<ThrownRock> THROWN_ROCK = EntityType.Builder
        .<ThrownRock>of(ThrownRock::new, MobCategory.MISC)
        .noLootTable()
        .sized(0.25f, 0.25f)
        .clientTrackingRange(4)
        .updateInterval(10)
        .build(THROWN_ROCK_KEY);

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
        // A deer is FAST — you can't run it down in a sprint; you wear it out (persistence hunting). Base
        // speed is the reliable lever (the AI flee-modifier alone wasn't translating to real pace), so it's
        // set well above a player's sprint (rabbit is 0.3). Tracking's wide radius keeps it huntable despite
        // the speed: keep the pressure on and it tires, slows, and can be finished. Tune in playtest.
        // STEP_HEIGHT 1.1: a deer flows up 1-block ledges and rough forest floor at speed instead of stalling
        // on a slow hop at every step — without it, it bogs on terrain and you catch it in seconds, tired or
        // not. This is what lets the chase actually run its ~2-3 minute course over real ground.
        FabricDefaultAttributeRegistry.register(DEER, Cow.createAttributes()
            .add(Attributes.MOVEMENT_SPEED, 0.34)
            .add(Attributes.STEP_HEIGHT, 1.1));
        // The woods and their edges — forest, taiga, and grassland. Common wild game, in small herds.
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(BiomeTags.IS_FOREST).or(BiomeSelectors.tag(BiomeTags.IS_TAIGA))
                .or(BiomeSelectors.tag(BiomeTags.IS_HILL)),
            MobCategory.CREATURE, DEER, 12, 2, 4);

        Registry.register(BuiltInRegistries.ENTITY_TYPE, SQUIRREL_KEY, SQUIRREL);
        // A rabbit under the fur — its attributes suit small game: little health (so it winds fast under a
        // chase) and a quick, darting flight. Common in wooded country, in ones and twos.
        FabricDefaultAttributeRegistry.register(SQUIRREL, Rabbit.createAttributes());
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(BiomeTags.IS_FOREST).or(BiomeSelectors.tag(BiomeTags.IS_TAIGA)),
            MobCategory.CREATURE, SQUIRREL, 10, 1, 3);

        Registry.register(BuiltInRegistries.ENTITY_TYPE, WILD_BOAR_KEY, WILD_BOAR);
        // A pig under the bristles, but a wild one: tougher and genuinely dangerous. Real boar (60–90 kg)
        // outweigh the barnyard pig-in-the-woods and carry tusks — so more health (~16 vs the pig's 10), a
        // real gore (ATTACK_DAMAGE 4, which a bare pig has none of) and a touch more pace to press a charge.
        // STEP_HEIGHT 1.0 lets it rush over the forest floor's ledges instead of stalling on every root.
        FabricDefaultAttributeRegistry.register(WILD_BOAR, Pig.createAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.ATTACK_KNOCKBACK, 0.6) // a charge shoves you back — you can't just stand and trade
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.STEP_HEIGHT, 1.0));
        // Home is thick cover: forest, taiga, and the swamp. Rooting about in ones and twos, not herds.
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(BiomeTags.IS_FOREST).or(BiomeSelectors.tag(BiomeTags.IS_TAIGA))
                .or(BiomeSelectors.tag(ConventionalBiomeTags.IS_SWAMP)),
            MobCategory.CREATURE, WILD_BOAR, 8, 1, 2);

        Registry.register(BuiltInRegistries.ENTITY_TYPE, BISON_KEY, BISON);
        // A cow under the shag, but wild megafauna: a bull bison tops a tonne. So a big health pool (~40,
        // four times a cow's 10, the sort of animal you don't drop in one blow), a heavy gore
        // (ATTACK_DAMAGE 6) with real KNOCKBACK to bowl a hunter over, and enough pace to run a charge home.
        // STEP_HEIGHT 1.1 keeps a stampede flowing over broken ground instead of bogging on 1-block rises.
        FabricDefaultAttributeRegistry.register(BISON, Cow.createAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.5)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.STEP_HEIGHT, 1.1));
        // The open grazing country — plains/grassland and savanna — where great herds of wild bovine ranged.
        // Spawns in herds (2–4): a bison is a herd animal, and a herd that all turns on you is the danger.
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(ConventionalBiomeTags.IS_PLAINS).or(BiomeSelectors.tag(BiomeTags.IS_SAVANNA)),
            MobCategory.CREATURE, BISON, 10, 2, 4);

        // A hand-thrown rock (§8.1) — a plain projectile, no attributes or spawns. It rides the standard
        // ThrowableItemProjectile spawn (the unified add-entity packet) and syncs its rock item via entity
        // data, so no custom spawn packet is needed; it just needs its type registered and a renderer.
        Registry.register(BuiltInRegistries.ENTITY_TYPE, THROWN_ROCK_KEY, THROWN_ROCK);
    }
}
