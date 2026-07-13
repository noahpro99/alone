package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Overhead leaf cover as <b>partial</b> shelter (proposal §2). A leaf canopy isn't a roof: rain drips
 * through it and sun dapples under it. Real foliage attenuates like Beer&ndash;Lambert &mdash; each layer
 * passes a fixed fraction of what reached it, so cover builds up <em>exponentially</em> with depth rather
 * than all-or-nothing. We tune it so <b>three layers of leaves ≈ half shelter</b> (half the rain/sun gets
 * through), and it keeps halving every three layers beyond that (6 → quarter, 9 → eighth…).
 *
 * <p>A solid block overhead (planks, stone, a tarp) is a real roof &mdash; full shelter, zero exposure.
 * Only leaves are treated as porous.
 */
public final class Canopy {
    private Canopy() {
    }

    /** How far up we look for overhead cover. Anything higher than this doesn't meaningfully shade a pot
     *  or a person standing here. */
    private static final int SCAN_UP = 40;

    /** Leaf layers to cut exposure in half. Beyond this it keeps halving every this-many layers. */
    private static final double LAYERS_PER_HALVING = 3.0;

    /**
     * Overhead leaf layers between {@code pos} and open sky, or {@code -1} if a <b>solid</b> (non-leaf)
     * block roofs the column &mdash; that's a real roof, not a canopy. Passable things (air, grass, flowers,
     * torches) don't count; only leaves attenuate and only solid blocks fully block.
     */
    public static int overheadLeaves(Level level, BlockPos pos) {
        int leaves = 0;
        for (int dy = 1; dy <= SCAN_UP; dy++) {
            BlockPos p = pos.above(dy);
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.LEAVES)) {
                leaves++;
                continue;
            }
            if (s.getCollisionShape(level, p).isEmpty()) {
                continue; // passable — rain and sun get straight through
            }
            return -1; // a solid roof overhead — fully sheltered
        }
        return leaves;
    }

    /** Beer&ndash;Lambert transmission for {@code n} leaf layers: {@code 0.5^(n/3)}. 0 → 1.0 (open),
     *  3 → 0.5, 6 → 0.25, and so on. */
    private static float transmission(int leaves) {
        return (float) Math.pow(0.5, leaves / LAYERS_PER_HALVING);
    }

    /**
     * Fraction of open-sky <b>sun</b> reaching {@code pos}: 1.0 under open sky, 0.0 under a solid roof,
     * and a partial value under a leaf canopy (half at three layers, exponential falloff). Use this to
     * scale how hard the sun bakes you &mdash; dappled shade under trees is real, cooler-than-open relief.
     */
    public static float skyExposure(Level level, BlockPos pos) {
        if (level.canSeeSky(pos)) {
            return 1f; // truly open above
        }
        int n = overheadLeaves(level, pos); // covered — is it just leaves, or a hard roof?
        return n <= 0 ? 0f : transmission(n);
    }

    /**
     * Fraction of falling <b>rain</b> reaching {@code pos}, 0..1 &mdash; like {@link #skyExposure} but zero
     * unless it's actually raining into this column. A leaf canopy lets a fraction of the rain drip through
     * (half at three layers), so a pot set under light foliage still slowly catches, while dense cover or a
     * solid roof catches nothing.
     */
    public static float rainExposure(Level level, BlockPos pos) {
        int leaves = 0;
        int topLeafDy = 0;
        for (int dy = 1; dy <= SCAN_UP; dy++) {
            BlockPos p = pos.above(dy);
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.LEAVES)) {
                leaves++;
                topLeafDy = dy;
                continue;
            }
            if (s.getCollisionShape(level, p).isEmpty()) {
                continue;
            }
            return 0f; // solid roof — no rain reaches the pot
        }
        // Under leaves, canSeeSky (and so isRainingAt) is false at the pot itself — the leaves are in the
        // heightmap. Probe the rain just above the highest leaf, where the sky check is honest; with no
        // leaves this is simply the block above the pot.
        if (!level.isRainingAt(pos.above(topLeafDy + 1))) {
            return 0f;
        }
        return leaves == 0 ? 1f : transmission(leaves);
    }
}
