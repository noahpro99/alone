package dev.alone.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.alone.core.net.KnapStrikePayload;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Knapping (proposal §8.1/§8.4). Your first sharp edge: hold <b>flint</b> in one hand and a
 * <b>{@link AloneItems#ROCK rock}</b> (a hammerstone) in the other, then <b>sneak and hold right-click</b>
 * to strike it, again and again. It isn't one tap — it takes a couple of seconds of steady striking
 * before a flake finally comes off, and even then the flint sometimes just <b>shatters</b> (skill-by-
 * doing, §8.4). Each held click is one strike (the client sends them, like the fire drill); enough
 * strikes on the same core flake off a sharp {@link AloneItems#FLINT_SHARD}. If you're holding only one
 * of the two, it tells you what's missing.
 */
public final class Knapping {
    private Knapping() {
    }

    private static final int STRIKES_TO_FLAKE = 10;     // ~2.5 s of steady striking (client sends ~every 5t)
    private static final float SUCCESS_CHANCE = 0.65f;  // the rest of the time the flint shatters
    private static final float ROCK_WEAR_CHANCE = 0.15f; // the hammerstone chips and eventually breaks
    private static final float STAMINA_PER_STRIKE = 0.8f;

    private record Knap(int strikes, int atTick) {
    }

    private static final Map<UUID, Knap> ACTIVE = new HashMap<>();

    public static void init() {
        // Each held right-click is one strike of the hammerstone (sent by the client use mixin).
        ServerPlayNetworking.registerGlobalReceiver(KnapStrikePayload.TYPE,
            (payload, context) -> strike(context.player()));

        // Holding just one of the pair while trying to strike — point them at the missing half. (The
        // client only intercepts the click when you hold BOTH, so this fires only on a near-miss.)
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            boolean haveFlint = main.is(Items.FLINT) || off.is(Items.FLINT);
            boolean haveRock = main.is(AloneItems.ROCK) || off.is(AloneItems.ROCK);
            if ((haveFlint ^ haveRock) && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal(haveFlint
                    ? "You need a rock in your other hand to knap the flint."
                    : "You need flint in your other hand to knap against the rock."), true);
            }
            return InteractionResult.PASS;
        });
    }

    private static void strike(ServerPlayer player) {
        Level level = player.level();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean haveFlint = main.is(Items.FLINT) || off.is(Items.FLINT);
        boolean haveRock = main.is(AloneItems.ROCK) || off.is(AloneItems.ROCK);
        if (!player.isShiftKeyDown() || !haveFlint || !haveRock) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        var rng = player.getRandom();
        level.playSound(null, player.blockPosition(), SoundEvents.STONE_HIT, SoundSource.PLAYERS,
            0.5f, 0.8f + rng.nextFloat() * 0.3f);
        SurvivalMeters.exert(player, STAMINA_PER_STRIKE);

        Knap current = ACTIVE.get(player.getUUID());
        boolean continuing = current != null && player.tickCount - current.atTick <= 12;
        int strikes = continuing ? current.strikes + 1 : 1;

        if (strikes < STRIKES_TO_FLAKE) {
            player.sendSystemMessage(Component.literal("Knapping…"), true);
            ACTIVE.put(player.getUUID(), new Knap(strikes, player.tickCount));
            return;
        }

        // A flake finally comes off — or the core shatters. Either way this flint is spent.
        ACTIVE.remove(player.getUUID());
        ItemStack flint = main.is(Items.FLINT) ? main : off;
        ItemStack hammerstone = main.is(AloneItems.ROCK) ? main : off;
        flint.shrink(1);
        boolean success = rng.nextFloat() < SUCCESS_CHANCE;
        if (success && !player.getInventory().add(new ItemStack(AloneItems.FLINT_SHARD))) {
            player.drop(new ItemStack(AloneItems.FLINT_SHARD), false);
        }
        if (!player.isCreative() && rng.nextFloat() < ROCK_WEAR_CHANCE) {
            hammerstone.shrink(1); // the hammerstone chips away
        }
        level.playSound(null, player.blockPosition(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 0.7f, 1.1f);
        player.sendSystemMessage(Component.literal(success
            ? "You knap a sharp flake of flint." : "The flint shatters uselessly."), true);
    }
}
