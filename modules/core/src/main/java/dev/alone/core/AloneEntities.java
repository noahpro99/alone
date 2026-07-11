package dev.alone.core;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Custom entities (proposal §6 — transport). The mod's first entity is the {@link TravoisEntity travois}:
 * a dragged cargo sled for hauling the heavy loads the carry limits (§5.1) won't let you pocket.
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

    /** Touching this class registers the entity types above. Called from {@link AloneCore}. */
    public static void init() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, TRAVOIS_KEY, TRAVOIS);
    }
}
