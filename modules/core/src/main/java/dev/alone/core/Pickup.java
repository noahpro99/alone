package dev.alone.core;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Pick things up by hand (proposal §5.1 / realism). Auto-pickup is off ({@code ItemEntityPickupMixin});
 * instead you <b>right-click a dropped item to pick it up</b>, and a stray <b>left-click won't destroy
 * it</b>. Items you can't hold (pack full) stay on the ground.
 */
public final class Pickup {
    private Pickup() {
    }

    public static void init() {
        // A swing near dropped items shouldn't smash them — cancel attacks on item entities.
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
            entity instanceof ItemEntity ? InteractionResult.FAIL : InteractionResult.PASS);

        // Right-click a dropped item to pick it up.
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(entity instanceof ItemEntity item)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS; // the server does the actual pickup
            }
            if (item.hasPickUpDelay()) {
                return InteractionResult.PASS; // just thrown / not settled — let it be for a moment
            }
            // One backpack per person (§6) — say so instead of silently refusing.
            if (item.getItem().is(AloneItems.BACKPACK) && !BackpackItem.findInInventory(player).isEmpty()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("You can only carry one backpack."), true);
                }
                return InteractionResult.SUCCESS;
            }
            ItemStack stack = item.getItem();
            int before = stack.getCount();
            player.getInventory().add(stack); // takes what fits
            if (stack.getCount() < before) {
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                    0.2f, 1.4f);
            }
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack); // some left on the ground (pack full) — sync the reduced stack
            }
            return InteractionResult.SUCCESS;
        });
    }
}
