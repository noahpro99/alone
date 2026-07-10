package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Carrying fire (proposal §3.1) — the ember-carry technique, so you don't have to friction-drill a new
 * fire at every camp. <b>Sneak + right-click a lit campfire, empty-handed,</b> to scoop a glowing
 * {@link AloneItems#EMBER} (it costs the fire a little fuel). The ember <b>cools as you carry it</b>
 * (its durability bar is the remaining glow); <b>right-click the top of the ground</b> with it to coax
 * a fresh lit campfire — no drilling, no tinder. Let it run out and it dies to cold charcoal.
 */
public final class Embers {
    private Embers() {
    }

    private static final int EMBER_FUEL_COST = 400; // scooping an ember costs the fire a bit of life

    public static void init() {
        // Embers cool wherever they sit in your pack — the durability bar counts down to a dead coal.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                coolEmbers(player);
            }
        });

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            ItemStack held = player.getMainHandItem();

            // Take an ember from a lit campfire (sneak + empty hand).
            if (held.isEmpty() && player.isShiftKeyDown()
                && state.is(Blocks.CAMPFIRE) && state.getValue(BlockStateProperties.LIT)) {
                if (!level.isClientSide()) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        Campfires.setFuel(be, Math.max(0, Campfires.getFuel(be) - EMBER_FUEL_COST));
                    }
                    player.setItemInHand(hand, new ItemStack(AloneItems.EMBER));
                    level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.3f);
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("You scoop a glowing ember from the fire."), true);
                    }
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            // Nestle a carried ember into an UNLIT campfire — it catches at once. The instant, sure-thing
            // alternative to gambling with the friction drill, which is exactly why carrying one is worth it.
            if (held.is(AloneItems.EMBER)
                && (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                && !state.getValue(BlockStateProperties.LIT)) {
                if (!level.isClientSide()) {
                    level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.LIT, true));
                    held.shrink(1); // the ember becomes the fire
                    level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.7f, 1.0f);
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("You nestle the ember in — the campfire catches at once."), true);
                    }
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private static void coolEmbers(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(AloneItems.EMBER)) {
                continue;
            }
            int used = stack.getDamageValue() + 1;
            if (used >= stack.getMaxDamage()) {
                inventory.setItem(i, new ItemStack(Items.CHARCOAL)); // gone cold — just a spent coal now
            } else {
                stack.setDamageValue(used);
            }
        }
    }
}
