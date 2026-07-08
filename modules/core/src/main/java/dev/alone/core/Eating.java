package dev.alone.core;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Eating discipline (proposal §1.1) — you can't eat while sprinting; food is something you stop for.
 * (Snack-vs-meal rooting is a later refinement; this is the core rule.)
 */
public final class Eating {
    private Eating() {
    }

    public static void init() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isSprinting() && stack.has(DataComponents.FOOD)) {
                return InteractionResult.FAIL; // no eating on the run
            }
            return InteractionResult.PASS;
        });
    }
}
