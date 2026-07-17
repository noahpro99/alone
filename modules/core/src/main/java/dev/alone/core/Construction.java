package dev.alone.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Building is real labour (proposal §5.5). In the show <em>Alone</em>, felling the wood is the small part —
 * the days go into the <b>construction</b>: hauling, fitting and stacking heavy timber. The mod times the
 * woodcutting but placing blocks was instant and free, so a steel axe could throw up a log shelter in a few
 * minutes. This class marks the <b>heavy structural blocks</b> whose placement costs real effort: heaving one
 * into a wall burns stamina, and when you're spent you can't lift another until you rest (see
 * {@code BlockItemStaminaMixin}). Small blocks — torches, tools, workstations, thatch — place freely.
 */
public final class Construction {
    private Construction() {
    }

    /** Heavy structural blocks (logs, timber, stone) whose placement is tiring — datapack-defined,
     *  {@code data/alone/tags/block/tiring_to_place.json}. */
    public static final TagKey<Block> TIRING_TO_PLACE =
        TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("alone", "tiring_to_place"));

    /** Stamina burned heaving one heavy block into place (MAX_STAMINA is 100, so ~8 in a row wind you, then
     *  you rest and carry on — which is what paces a shelter into a real project instead of instant clicking). */
    public static final float PLACE_STAMINA = 12f;
}
