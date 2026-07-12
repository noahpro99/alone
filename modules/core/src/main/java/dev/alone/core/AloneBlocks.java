package dev.alone.core;

import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Alone's custom blocks. The bedroll (proposal §5.2) is, for now, a real {@link BedBlock}: it places,
 * sleeps, sets spawn and skips the night exactly like a vanilla bed. Its model just reuses the bed's
 * foot half twice (see the blockstate) so it reads as a flat mat instead of a pillowed bed.
 */
public final class AloneBlocks {
    private AloneBlocks() {
    }

    public static final Block BEDROLL = register("bedroll",
        key -> new BedBlock(DyeColor.WHITE, BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL)
            .sound(SoundType.WOOL)
            .strength(0.2F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)) {
            @Override
            public net.minecraft.world.level.block.RenderShape getRenderShape(net.minecraft.world.level.block.state.BlockState state) {
                return net.minecraft.world.level.block.RenderShape.MODEL;
            }
        });

    /**
     * A warmth-rated sleeping bag (§5.5) — a bedroll that sleeps like a real cold-weather bag. Mechanically
     * it places, sleeps, sets spawn and skips the night like the {@link #BEDROLL}, but {@link SurvivalMeters}
     * gives it strong insulation <b>while you sleep in it</b>, so a cold or even freezing night still rests
     * you fully (a bedroll leaves you shivering). The trade: it costs more loft (wool) and a shell (leather)
     * to make, and — like all insulation — a soaked bag holds almost no heat, so keep it dry. Reuses the
     * bedroll model/textures as <b>placeholder art</b> for now.
     */
    public static final Block SLEEPING_BAG = register("sleeping_bag",
        key -> new BedBlock(DyeColor.RED, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED)
            .sound(SoundType.WOOL)
            .strength(0.2F)
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)) {
            @Override
            public net.minecraft.world.level.block.RenderShape getRenderShape(net.minecraft.world.level.block.state.BlockState state) {
                return net.minecraft.world.level.block.RenderShape.MODEL;
            }
        });

    /**
     * Rope (proposal §5.7) — a hanging climb line. No collision (you move through it) and it's in the
     * {@code minecraft:climbable} tag, so vanilla treats it exactly like a ladder: full-speed, no-cost,
     * safe up-and-down climbing — the civilized alternative to brutal free-climbing. Deployed as a run
     * down a cliff face by {@link RopeItem}. Dry plant fibre, so it's <b>very flammable</b>: lava lights
     * it and fire climbs a line fast (see {@link #init()}) — keep your ropes away from flame.
     */
    public static final Block ROPE = register("rope",
        key -> new RopeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOL)
            .strength(0.4F)
            .noCollision()
            .noOcclusion()
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)));

    /** Loose rocks scattered on the ground (§8.1) — generated like grass tufts, break instantly by hand
     *  for a {@link AloneItems#ROCK}. The day-one stone source you can see and grab. */
    public static final Block LOOSE_ROCK = register("loose_rock",
        key -> new LooseRockBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .instabreak()
            .noCollision()
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)));

    /** A backpack set down as a block (§6) — right-click opens its 27 slots; break it to pick it up. */
    public static final Block BACKPACK_BLOCK = register("backpack",
        key -> new BackpackBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BROWN)
            .sound(SoundType.WOOL)
            .strength(0.6F)
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)));

    /** A kiln (§3.2) — a brick heat structure that fires unfired pottery/clay into hardened ware over a
     *  couple of fuel-hungry minutes. Load ware + fuel by hand; take the finished piece out (see KilnBlock). */
    public static final Block KILN = register("kiln",
        key -> new KilnBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_RED)
            .sound(SoundType.STONE)
            .strength(2.0F)
            .requiresCorrectToolForDrops()
            .setId(key)));

    /** A bloomery (§3.2/§8.2) — the primitive iron furnace, built of heat-resistant refractory clay.
     *  Load iron + charcoal and it smelts a bloom of iron over a long, fuel-hungry burn (see BloomeryBlock). */
    public static final Block BLOOMERY = register("bloomery",
        key -> new BloomeryBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY)
            .sound(SoundType.STONE)
            .strength(2.5F)
            .requiresCorrectToolForDrops()
            .setId(key)));

    /** A clay pot set down in the open to catch rain (§2). Fills clean while it rains; place empty. */
    public static final Block CLAY_POT = register("clay_pot",
        key -> new ClayPotBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN)
            .sound(SoundType.DECORATED_POT)
            .strength(0.4F)
            .noOcclusion()
            .setId(key)));

    /** The block entity that stores the set-down backpack's contents. Assigned in {@link #init()}. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<BackpackBlockEntity> BACKPACK_BLOCK_ENTITY;
    /** A drying rack (§4.2) — hang perishable food to dry into non-spoiling jerky (smoked faster by fire). */
    public static final Block DRYING_RACK = register("drying_rack",
        key -> new DryingRackBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOD)
            .strength(0.6F)
            .noOcclusion()
            .setId(key)));

    /** A snare (§7.2) — a cordage noose set on the ground that passively catches small game over time,
     *  drawing on the local {@link GameStock}. No collision (you step over it); random-ticks to catch. */
    public static final Block SNARE = register("snare",
        key -> new SnareBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .sound(SoundType.GRASS)
            .strength(0.2F)
            .noCollision()
            .noOcclusion()
            .randomTicks()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)));

    /** A deadfall (§7.2) — a baited stone propped over a trigger that drops on small game. Second rung of
     *  the trapping ladder above the {@link #SNARE}: costs bait, one-shot, but better odds. Random-ticks. */
    public static final Block DEADFALL = register("deadfall",
        key -> new DeadfallBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE)
            .sound(SoundType.STONE)
            .strength(0.5F)
            .noCollision()
            .noOcclusion()
            .randomTicks()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)));

    /** A fish trap / weir (§7.2) — a woven basket set on the water that passively catches fish, drawing on
     *  the local {@link FishStock}. Top rung of the trapping ladder; placed on water like a lily pad. */
    public static final Block FISH_TRAP = register("fish_trap",
        key -> new FishTrapBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WATER)
            .sound(SoundType.WET_GRASS)
            .strength(0.3F)
            .noCollision()
            .noOcclusion()
            .randomTicks()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)));

    /** The set-down clay pot's caught rainwater (how full, fill progress). Assigned in {@link #init()}. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<ClayPotBlockEntity> CLAY_POT_BLOCK_ENTITY;
    /** The drying rack's hung food + dry progress. Assigned in {@link #init()}. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<DryingRackBlockEntity> DRYING_RACK_BLOCK_ENTITY;
    /** The kiln's firing state (loaded ware, fuel, progress). Assigned in {@link #init()}. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<KilnBlockEntity> KILN_BLOCK_ENTITY;
    /** The bloomery's smelting state (loaded ore, fuel, progress). Assigned in {@link #init()}. */
    public static net.minecraft.world.level.block.entity.BlockEntityType<BloomeryBlockEntity> BLOOMERY_BLOCK_ENTITY;

    /** Touching this class registers the blocks above. Call before {@link AloneItems#init()}. */
    public static void init() {
        KILN_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("alone", "kiln"),
            new net.minecraft.world.level.block.entity.BlockEntityType<>(
                KilnBlockEntity::new, java.util.Set.of(KILN)));
        BLOOMERY_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("alone", "bloomery"),
            new net.minecraft.world.level.block.entity.BlockEntityType<>(
                BloomeryBlockEntity::new, java.util.Set.of(BLOOMERY)));
        BACKPACK_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("alone", "backpack"),
            new net.minecraft.world.level.block.entity.BlockEntityType<>(
                BackpackBlockEntity::new, java.util.Set.of(BACKPACK_BLOCK)));
        CLAY_POT_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("alone", "clay_pot"),
            new net.minecraft.world.level.block.entity.BlockEntityType<>(
                ClayPotBlockEntity::new, java.util.Set.of(CLAY_POT)));
        DRYING_RACK_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("alone", "drying_rack"),
            new net.minecraft.world.level.block.entity.BlockEntityType<>(
                DryingRackBlockEntity::new, java.util.Set.of(DRYING_RACK)));

        // Rope is dry plant fibre — it catches readily and burns up fast, so fire runs up a hung line.
        net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance().add(ROPE, 30, 60);
    }

    private static Block register(String path, Function<ResourceKey<Block>, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("alone", path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(key));
    }
}
