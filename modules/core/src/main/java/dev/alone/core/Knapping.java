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
 * Knapping (proposal §8.1/§8.4). Your first sharp edge: hold <b>flint</b> in one hand and a
 * <b>{@link AloneItems#ROCK rock}</b> (a hammerstone) in the other, then <b>sneak + right-click</b> to
 * strike. It often flakes off a sharp {@link AloneItems#FLINT_SHARD}, but sometimes the flint just
 * <b>shatters</b> — you learn the technique the hard way (skill-by-doing, §8.4). Those shards are what
 * flint tools are built from, so a knife/axe/pick begins with knapping, not a grid full of raw flint.
 * If you're holding only one of the two, it tells you what's missing.
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
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            boolean haveFlint = main.is(Items.FLINT) || off.is(Items.FLINT);
            boolean haveRock = main.is(AloneItems.ROCK) || off.is(AloneItems.ROCK);

            // Holding just one of the pair while trying to strike — point them at the missing half.
            if (haveFlint ^ haveRock) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.literal(haveFlint
                        ? "You need a rock in your other hand to knap the flint."
                        : "You need flint in your other hand to knap against the rock."), true);
                }
                return InteractionResult.PASS;
            }
            if (!haveFlint || !haveRock) {
                return InteractionResult.PASS; // neither piece — not a knapping attempt
            }

            // Either arrangement works — flint in whichever hand, rock in the other.
            ItemStack flint = main.is(Items.FLINT) ? main : off;
            ItemStack hammerstone = main.is(AloneItems.ROCK) ? main : off;
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
