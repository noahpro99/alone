package dev.alone.core;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Leaves behave like real foliage (proposal §5.4 / realism). Tearing a canopy apart with your bare
 * hands leaves you holding broken twigs and a scatter of leaf litter — not a tidy hedge cube; a clean
 * cut with an <b>axe or hoe</b> salvages the whole leaf block. Either way it's slow, tugging work
 * (see {@code PlayerDestroySpeedMixin}). Vanilla saplings/apples still fall on top, so tree farming
 * survives. Leaves are also soft enough to climb up through — see {@link Climbing}.
 */
public final class Leaves {
    private Leaves() {
    }

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || player.isCreative() || !(level instanceof ServerLevel serverLevel)) {
                return;
            }
            if (!state.is(BlockTags.LEAVES)) {
                return;
            }
            ItemStack tool = player.getMainHandItem();
            if (tool.is(ItemTags.AXES) || tool.is(ItemTags.HOES)) {
                Block.popResource(serverLevel, pos, new ItemStack(state.getBlock())); // clean cut → the leaf block
            } else {
                // bare hands: you come away with snapped twigs and a handful of leaf litter
                Block.popResource(serverLevel, pos, new ItemStack(Items.STICK, 1 + player.getRandom().nextInt(2)));
                Block.popResource(serverLevel, pos, new ItemStack(Items.LEAF_LITTER, 1 + player.getRandom().nextInt(2)));
            }
        });
    }
}
