package dev.alone.food;

import dev.alone.core.AloneItems;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Salting food to preserve it (proposal §4.2). Hold a perishable food, keep <b>salt in your other
 * hand</b>, and <b>sneak + right-click</b>: the food is salted — marked {@code PRESERVED} so spoilage
 * skips it and it keeps through winter — at the cost of one salt. Salt comes from boiling seawater (§2).
 */
public final class Preserving {
    private Preserving() {
    }

    public static void init() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack food = player.getMainHandItem();
            ItemStack salt = player.getOffhandItem();
            if (food.is(Spoilage.PERISHABLE) && !food.getOrDefault(Spoilage.PRESERVED, false)
                && salt.is(AloneItems.SALT)) {
                if (!level.isClientSide()) {
                    food.set(Spoilage.PRESERVED, true);
                    food.remove(Spoilage.SPOILS_AT); // cancel any running shelf-life timer
                    if (!player.isCreative()) {
                        salt.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("Salted — it will keep."));
                }
                return InteractionResult.SUCCESS; // consume the click so it doesn't start eating
            }
            return InteractionResult.PASS;
        });
    }
}
