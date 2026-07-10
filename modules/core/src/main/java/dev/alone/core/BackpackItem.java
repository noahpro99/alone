package dev.alone.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * A backpack (proposal §6) — real carried storage, not just a capacity number. <b>Right-click to open
 * it</b> and a 27-slot pack (a vanilla chest screen) appears; its contents live on the item itself
 * ({@code minecraft:container} component), so they travel with the pack and even show in its tooltip.
 * It still raises your volume limit (see {@link Carry#volumeLimit}). A body slot and a placeable
 * "set it down like a chest" mode are planned on top of this.
 */
public class BackpackItem extends Item {
    private static final int SLOTS = 27;

    public BackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            open(player, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    /** Open a chest menu backed by the pack's stored contents, saving back to the item on every change. */
    public static void open(Player player, ItemStack backpack) {
        boolean[] loading = {true};
        SimpleContainer container = new SimpleContainer(SLOTS) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return !stack.is(AloneItems.BACKPACK); // no packing a backpack inside a backpack
            }

            @Override
            public void setChanged() {
                super.setChanged();
                if (loading[0]) {
                    return; // don't write mid-load
                }
                List<ItemStack> out = new ArrayList<>(SLOTS);
                for (int i = 0; i < SLOTS; i++) {
                    out.add(this.getItem(i));
                }
                backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(out));
            }
        };
        NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        for (int i = 0; i < SLOTS; i++) {
            container.setItem(i, items.get(i));
        }
        loading[0] = false;
        player.openMenu(new SimpleMenuProvider(
            (id, inventory, opener) -> ChestMenu.threeRows(id, inventory, container), backpack.getHoverName()));
    }
}
