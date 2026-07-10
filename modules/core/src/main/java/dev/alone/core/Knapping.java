package dev.alone.core;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Knapping (proposal §8.1/§8.4). Your first sharp edge: hold <b>flint</b>, a <b>{@link AloneItems#ROCK
 * rock}</b> as a hammerstone in your off hand, and <b>sneak + right-click</b> to strike. It often flakes
 * off a sharp {@link AloneItems#FLINT_SHARD}, but sometimes the flint just <b>shatters</b> — you learn
 * the technique the hard way (skill-by-doing, §8.4). Those shards are what flint tools are built from,
 * so a knife/axe/pick begins with knapping, not a crafting grid full of raw flint.
 */
public final class Knapping {
    private Knapping() {
    }

    private static final float SUCCESS_CHANCE = 0.65f; // the rest of the time the flint shatters
    private static final float ROCK_WEAR_CHANCE = 0.15f; // the hammerstone chips and eventually breaks

    public static void init() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack flint = player.getMainHandItem();
            ItemStack hammerstone = player.getOffhandItem();
            if (!flint.is(Items.FLINT) || !hammerstone.is(AloneItems.ROCK)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                var rng = player.getRandom();
                flint.shrink(1); // struck — spent whether it flakes clean or shatters
                boolean success = rng.nextFloat() < SUCCESS_CHANCE;
                if (success && !player.getInventory().add(new ItemStack(AloneItems.FLINT_SHARD))) {
                    player.drop(new ItemStack(AloneItems.FLINT_SHARD), false);
                }
                if (!player.isCreative() && rng.nextFloat() < ROCK_WEAR_CHANCE) {
                    hammerstone.shrink(1); // the hammerstone chips away
                }
                level.playSound(null, player.blockPosition(), SoundEvents.STONE_HIT, SoundSource.PLAYERS,
                    0.6f, 0.8f + rng.nextFloat() * 0.3f);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.literal(success
                        ? "You knap a sharp flake of flint." : "The flint shatters uselessly."), true);
                }
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        });
    }
}
