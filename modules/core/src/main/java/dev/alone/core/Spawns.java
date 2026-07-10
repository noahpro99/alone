package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.StructureManager;

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

    /** True if this position is on or near a generated structure — the only place hostiles may spawn naturally. */
    public static boolean nearStructure(ServerLevel level, BlockPos pos) {
        StructureManager structures = level.structureManager();
        for (int[] offset : SAMPLES) {
            if (structures.hasAnyStructureAt(pos.offset(offset[0], 0, offset[1]))) {
                return true;
            }
        }
        return false;
    }
}
