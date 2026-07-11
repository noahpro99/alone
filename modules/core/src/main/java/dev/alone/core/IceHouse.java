package dev.alone.core;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ice houses (roadmap: Food, farming &amp; preservation) — the pre-refrigeration way to hold cold through
 * the warm season, exactly as it was done historically: <b>cut blocks of lake ice in the cold</b>, haul
 * them home, and <b>pack them around your food store</b> in an enclosed space so the store stays cold for
 * weeks — an ice house.
 *
 * <p>Two honest halves:
 * <ul>
 *   <li><b>Harvesting:</b> plain ice normally shatters to a meltwater puddle when you break it. Cut it with
 *       a <b>pick or axe</b> instead and you lift out a clean <b>block of ice</b> to carry (heavy — see
 *       {@link Carry}). Only natural {@link Blocks#ICE} works; packed/blue ice keep their vanilla rules.</li>
 *   <li><b>Cold store:</b> ice or snow packed against a chest/barrel chills it like a cellar (see
 *       {@link #coldPacking}). It works above ground and in warm climates — but the ice must be kept out of
 *       the light or it melts (vanilla melts ice near torches/lava), so a real ice house is <b>dark and
 *       enclosed</b>, and in a warm biome you must <b>haul the ice in</b> because none forms there.</li>
 * </ul>
 */
public final class IceHouse {
    private IceHouse() {
    }

    public static void init() {
        // Cut a clean block of ice out (instead of a puddle) when a pick/axe breaks natural lake ice.
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || player.isCreative()) {
                return;
            }
            if (!state.is(Blocks.ICE) || !alone$canCutIce(player.getMainHandItem())) {
                return;
            }
            BlockState now = level.getBlockState(pos);
            if (now.is(Blocks.WATER) || now.isAir()) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()); // no meltwater — we lifted it out
            }
            Block.popResource(level, pos, new ItemStack(Blocks.ICE));
        });
    }

    /** A pick or axe chips/saws ice out in one piece; anything else just smashes it to slush. */
    private static boolean alone$canCutIce(ItemStack tool) {
        if (tool.isEmpty()) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath();
        return path.contains("pick") || path.contains("axe") || path.contains("hatchet");
    }

    /**
     * How many ice/snow blocks are packed in the 26 cells surrounding a store — the more the earth around a
     * chest is lined with ice, the colder it keeps. Used by food spoilage to make an ice house a cold store.
     */
    public static int coldPacking(Level level, BlockPos pos) {
        int count = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (alone$isCold(level.getBlockState(p))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean alone$isCold(BlockState s) {
        return s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE)
            || s.is(Blocks.FROSTED_ICE) || s.is(Blocks.SNOW_BLOCK);
    }
}
