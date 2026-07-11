package dev.alone.core;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Day-one foraging (proposal §8.1). Before you can knap a tool you gather from the ground: <b>digging
 * gravel</b> by hand turns up <b>flint and loose rocks</b>, and rummaging through <b>grass and ferns</b>
 * turns up the odd <b>stick or stone</b>. Those loose {@link AloneItems#ROCK rocks} + flint + sticks are
 * everything you need to knap your first flint tools ({@link Knapping}) — no punching trees or stone.
 */
public final class Foraging {
    private Foraging() {
    }

    public static void init() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || player.isCreative() || !(level instanceof ServerLevel serverLevel)) {
                return;
            }
            RandomSource rng = player.getRandom();
            if (state.is(Blocks.GRAVEL)) {
                // Flint isn't a lump you get when the block finally pops — it shakes loose bit by bit AS you
                // sift the gravel (see ServerPlayerGameModeMixin). The break itself just turns up a loose rock.
                if (rng.nextFloat() < 0.5f) {
                    Block.popResource(serverLevel, pos, new ItemStack(AloneItems.ROCK));
                }
            } else if (isUnderbrush(state)) {
                if (rng.nextFloat() < 0.2f) {
                    Block.popResource(serverLevel, pos, new ItemStack(Items.STICK));
                }
                if (rng.nextFloat() < 0.12f) {
                    Block.popResource(serverLevel, pos, new ItemStack(AloneItems.ROCK));
                }
            }
        });
    }

    private static boolean isUnderbrush(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
            || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.DEAD_BUSH);
    }
}
