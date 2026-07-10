package dev.alone.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The live container behind a held (open) backpack (§6). Loads from and saves to the backpack item's
 * {@code minecraft:container} component. It's its own class so the carry system can recognise it and
 * apply the backpack's <b>own volume cap</b> — the pack's volume decides how much more fits in it,
 * separate from what you carry on your body ({@link Carry#containerVolumeLimit}).
 */
public class BackpackContainer extends SimpleContainer {
    private final ItemStack backpack;
    private boolean loading;

    public BackpackContainer(ItemStack backpack) {
        super(BackpackBlockEntity.SLOTS);
        this.backpack = backpack;
        this.loading = true;
        NonNullList<ItemStack> items = NonNullList.withSize(BackpackBlockEntity.SLOTS, ItemStack.EMPTY);
        backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        for (int i = 0; i < items.size(); i++) {
            setItem(i, items.get(i));
        }
        this.loading = false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !stack.is(AloneItems.BACKPACK); // no packing a backpack inside a backpack
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (loading) {
            return;
        }
        List<ItemStack> out = new ArrayList<>(getContainerSize());
        for (int i = 0; i < getContainerSize(); i++) {
            out.add(getItem(i));
        }
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(out));
    }
}
