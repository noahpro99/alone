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
            .setId(key)));

    /**
     * Rope (proposal §5.7) — a hanging climb line. No collision (you move through it) and it's in the
     * {@code minecraft:climbable} tag, so vanilla treats it exactly like a ladder: full-speed, no-cost,
     * safe up-and-down climbing — the civilized alternative to brutal free-climbing. Deployed as a run
     * down a cliff face by {@link RopeItem}.
     */
    public static final Block ROPE = register("rope",
        key -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD)
            .sound(SoundType.WOOL)
            .strength(0.4F)
            .noCollision()
            .noOcclusion()
            .pushReaction(PushReaction.DESTROY)
            .setId(key)));

    /** Touching this class registers the blocks above. Call before {@link AloneItems#init()}. */
    public static void init() {
    }

    private static Block register(String path, Function<ResourceKey<Block>, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("alone", path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(key));
    }
}
