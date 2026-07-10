package dev.alone.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    /** Sneak + right-click a surface to <b>set the backpack down as a block</b>, contents and all. */
    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS; // not sneaking → fall through to use() (open it in hand)
        }
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(placePos, AloneBlocks.BACKPACK_BLOCK.defaultBlockState());
            PlacedBlocks.markPlaced(level, placePos); // set down by the player — loose, quick to pick up
            ItemStack backpack = context.getItemInHand();
            if (level.getBlockEntity(placePos) instanceof BackpackBlockEntity be) {
                NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
                backpack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
                for (int i = 0; i < SLOTS; i++) {
                    be.setItem(i, items.get(i));
                }
                be.setChanged();
            }
            if (!player.isCreative()) {
                backpack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The first backpack anywhere in the player's inventory, or empty — used by the quick-open keybind. */
    public static ItemStack findInInventory(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(AloneItems.BACKPACK)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Open a chest menu backed by the pack's stored contents (a {@link BackpackContainer}). */
    public static void open(Player player, ItemStack backpack) {
        BackpackContainer container = new BackpackContainer(backpack);
        player.openMenu(new SimpleMenuProvider(
            (id, inventory, opener) -> ChestMenu.threeRows(id, inventory, container), backpack.getHoverName()));
    }
}
