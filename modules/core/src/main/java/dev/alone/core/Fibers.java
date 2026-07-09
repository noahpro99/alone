package dev.alone.core;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plant fiber &amp; cordage (proposal §8.1) — where string actually comes from without a spider. Real
 * cordage is stripped from grasses, ferns, vines and other fibrous plants, then twisted into string
 * (see the {@code string} recipe). Tearing these plants up gives you <b>plant fiber</b>; doing it with
 * a <b>cutting blade</b> (sword/axe/hoe) strips more, cleaner fiber than bare hands. Spiders still drop
 * string too — this just frees you from needing them.
 */
public final class Fibers {
    private Fibers() {
    }

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || player.isCreative() || !(level instanceof ServerLevel serverLevel)) {
                return;
            }
            if (!isFibrousPlant(state)) {
                return;
            }
            ItemStack tool = player.getMainHandItem();
            boolean blade = tool.is(ItemTags.SWORDS) || tool.is(ItemTags.AXES) || tool.is(ItemTags.HOES)
                || tool.is(AloneItems.FLINT_KNIFE);
            int fiber = blade
                ? 1 + player.getRandom().nextInt(2)              // a blade strips 1–2 clean lengths
                : (player.getRandom().nextFloat() < 0.5f ? 1 : 0); // bare hands: sometimes a strand
            if (fiber > 0) {
                Block.popResource(serverLevel, pos, new ItemStack(AloneItems.PLANT_FIBER, fiber));
            }
        });
    }

    /** Grasses, ferns, vines, and other fibrous ground plants you can strip cordage from. */
    private static boolean isFibrousPlant(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
            || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
            || state.is(Blocks.VINE) || state.is(Blocks.DEAD_BUSH)
            || state.is(Blocks.HANGING_ROOTS) || state.is(Blocks.CAVE_VINES)
            || state.is(Blocks.CAVE_VINES_PLANT) || state.is(Blocks.WEEPING_VINES)
            || state.is(Blocks.TWISTING_VINES);
    }
}
