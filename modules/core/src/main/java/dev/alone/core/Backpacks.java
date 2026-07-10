package dev.alone.core;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

/**
 * Breaking a set-down backpack (proposal §6) hands the <b>pack itself back, contents and all</b> — the
 * block-entity's items become the item's {@code minecraft:container} component. (Placement lives in
 * {@link BackpackItem#useOn}, opening in {@link BackpackBlock}.)
 */
public final class Backpacks {
    private Backpacks() {
    }

    public static void init() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || !state.is(AloneBlocks.BACKPACK_BLOCK)
                || !(level instanceof ServerLevel serverLevel)) {
                return true;
            }
            if (be instanceof BackpackBlockEntity backpack) {
                List<ItemStack> contents = new ArrayList<>(backpack.getContainerSize());
                for (int i = 0; i < backpack.getContainerSize(); i++) {
                    contents.add(backpack.getItem(i));
                }
                backpack.clearContent(); // don't let the block-entity also scatter its items
                ItemStack item = new ItemStack(AloneItems.BACKPACK);
                item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
                serverLevel.removeBlock(pos, false);
                if (!player.isCreative()) {
                    Block.popResource(serverLevel, pos, item);
                }
                return false; // handled — no vanilla break/drops
            }
            return true;
        });
    }
}
