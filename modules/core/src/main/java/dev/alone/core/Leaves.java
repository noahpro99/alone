package dev.alone.core;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Leaves behave like real foliage (proposal §5.4 / realism). However you tear a canopy down — bare
 * hands, an axe, a hoe — you come away with the same thing a fistful of branches leaves in the real
 * world: a good scatter of <b>leaf litter</b> and, often, some snapped <b>twigs</b>. A tool only makes
 * the work faster, not richer (the yield is tool-agnostic); either way it's slow, tugging work (see
 * {@code PlayerDestroySpeedMixin}). Vanilla saplings/apples still fall on top, so tree farming survives.
 * Leaves are also soft enough to climb up through — see {@link Climbing}.
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
            // Tool-agnostic: a torn-down canopy always sheds a decent pile of litter, plus a chance of
            // twigs — an axe or hoe just breaks it faster, it doesn't salvage more.
            int litter = 2 + player.getRandom().nextInt(3); // 2–4 leaf litter, every break, whatever the tool
            Block.popResource(serverLevel, pos, new ItemStack(Items.LEAF_LITTER, litter));
            int sticks = player.getRandom().nextInt(3);      // 0–2 snapped twigs — a chance, not a guarantee
            if (sticks > 0) {
                Block.popResource(serverLevel, pos, new ItemStack(Items.STICK, sticks));
            }
        });
    }
}
