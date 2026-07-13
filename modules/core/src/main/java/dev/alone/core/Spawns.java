package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The core spawn rule (proposal §7.1): <b>monsters only near loot structures.</b> The open wilderness
 * is lonely and (relatively) safe — danger clusters around ruins, dungeons, mineshafts, strongholds,
 * villages and the like. Natural hostile spawns are cancelled unless they're within a structure's
 * footprint (see {@code MobSpawnMixin}); spawners, structure-placed mobs, breeding, and passive
 * wildlife are untouched.
 */
public final class Spawns {
    private Spawns() {
    }

    /** Horizontal samples around a spawn: centre + a ring, so a structure nearby still fields guards. */
    private static final int[][] SAMPLES = {
        {0, 0}, {40, 0}, {-40, 0}, {0, 40}, {0, -40}, {28, 28}, {-28, 28}, {28, -28}, {-28, -28}
    };

    /** The substantial loot/danger structures where hostiles are allowed to spawn — mineshafts, strongholds,
     *  ancient cities, outposts, monuments, mansions, fortresses, bastions, trial chambers, witch huts. We
     *  deliberately do NOT include the many small, common structures (above all <b>ruined portals</b>, which
     *  blanket the surface) or peaceful <b>villages</b>: matching "any structure at all" let monsters spawn
     *  around every little ruin, which is why the night still felt busy. Datapack-defined
     *  ({@code data/alone/tags/worldgen/structure/monster_haunts.json}). */
    private static final TagKey<Structure> MONSTER_HAUNTS = TagKey.create(Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath("alone", "monster_haunts"));

    /** True if this position is on or near a generated <b>danger</b> structure PIECE (see {@link #MONSTER_HAUNTS})
     *  — the only place hostiles may spawn naturally. We check for an actual structure PIECE (a real
     *  ruin/dungeon/mineshaft corridor), NOT {@code hasAnyStructureAt}: that only asks whether the chunk holds
     *  a structure <em>reference</em> (overlaps some structure's bounding box), and a single mineshaft's box
     *  blankets a huge area — so it was true almost everywhere. Piece-level, and restricted to the curated
     *  danger tag, keeps monsters at the actual ruins and out of the open (and quiet) wilderness. */
    public static boolean nearStructure(ServerLevel level, BlockPos pos) {
        StructureManager structures = level.structureManager();
        for (int[] offset : SAMPLES) {
            if (structures.getStructureWithPieceAt(pos.offset(offset[0], 0, offset[1]), MONSTER_HAUNTS).isValid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if this position is on or near a <b>village</b> specifically — livestock are farmed animals
     * that belong to settlements, so their natural spawns are only allowed here (see {@code Domestic}).
     * Samples centre + the same ring as {@link #nearStructure} so a village nearby still fields its herd.
     */
    public static boolean nearVillage(ServerLevel level, BlockPos pos) {
        StructureManager structures = level.structureManager();
        for (int[] offset : SAMPLES) {
            if (structures.getStructureWithPieceAt(pos.offset(offset[0], 0, offset[1]), StructureTags.VILLAGE).isValid()) {
                return true;
            }
        }
        return false;
    }
}
