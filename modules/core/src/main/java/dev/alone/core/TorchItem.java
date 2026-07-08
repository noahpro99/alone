package dev.alone.core;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * An unlit torch (proposal §5.6). It's a fuel item with durability — crafted full and dark, it gives
 * no light until you <b>light it</b>: right-click a fire source (campfire/fire/lava/another lit torch)
 * with it, or right-click with flint &amp; steel in your other hand. Lighting turns it into
 * {@code alone:torch_lit}, which burns its fuel down and, when spent, leaves a burnt-out (unlit) torch.
 * A shader handles the actual glow of a held lit torch.
 */
public class TorchItem extends Item {
    public TorchItem(Properties properties) {
        super(properties);
    }

    /** Light from a fire source you click on. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player != null && Torches.isFireSource(level.getBlockState(context.getClickedPos()))) {
            if (!level.isClientSide()) {
                Torches.light(player, context.getHand());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Light with flint &amp; steel held in the other hand. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack striker = player.getItemInHand(other);
        if (striker.is(Items.FLINT_AND_STEEL)) {
            if (!level.isClientSide()) {
                Torches.light(player, hand);
                striker.setDamageValue(striker.getDamageValue() + 1); // wear the flint & steel
                if (striker.getDamageValue() >= striker.getMaxDamage()) {
                    striker.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
