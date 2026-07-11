package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Hot-rock boiling (proposal §2 — the water half you can't do over a flame). A hide waterskin or bark cup
 * can't sit in a fire, so the real way to boil water in one is to <b>heat stones in the fire and drop them
 * in</b>: <b>sneak + right-click a lit campfire holding a rock</b> to pull out a glowing {@link
 * AloneItems#HOT_ROCK} (it costs the fire a little fuel), then hold the <b>vessel in your off hand, the hot
 * rock in your main hand, and sneak + right-click</b> — the stone hisses into the water and brings it to a
 * boil, turning it clean. The rock gives up its heat and cools back to a plain rock; carry one too long and
 * it goes cold on its own. Boiling doesn't desalinate seawater — that still needs a still (§2).
 *
 * <p>Simplified honestly: one good hot stone boils a small skin's worth in a go (real hot-rock boiling
 * cycles several cooling stones), and it only works on non-fire-safe vessels — a metal or clay pot just
 * goes straight over the fire ({@link WaterskinItem#isFireSafe()}).
 */
public final class HotRocks {
    private HotRocks() {
    }

    private static final int HEAT_FUEL_COST = 200;   // heating a stone costs the fire a little life
    /** A stone must still hold this fraction of its heat to bring water to a boil — a near-cold one won't. */
    private static final float BOIL_HEAT_FRACTION = 0.4f;

    public static void init() {
        // A carried hot rock loses its heat, the durability bar counting down to a cold, plain rock.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                coolRocks(player);
            }
        });

        // Heat a rock in a lit campfire: sneak + right-click with a rock in hand.
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack held = player.getMainHandItem();
            if (!held.is(AloneItems.ROCK) || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.CAMPFIRE) || !state.getValue(BlockStateProperties.LIT)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    Campfires.setFuel(be, Math.max(0, Campfires.getFuel(be) - HEAT_FUEL_COST));
                }
                held.shrink(1);
                giveOrDrop(player, new ItemStack(AloneItems.HOT_ROCK)); // fresh from the fire — full heat
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.1f);
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("You nestle a stone in the coals until it glows."), true);
                }
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        });

        // Boil an off-hand hide/bark vessel by dropping the main-hand hot rock in: sneak + right-click.
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack rock = player.getMainHandItem();
            ItemStack vessel = player.getOffhandItem();
            if (!rock.is(AloneItems.HOT_ROCK)
                || !(vessel.getItem() instanceof WaterskinItem w) || w.isFireSafe()) {
                return InteractionResult.PASS; // a fire-safe pot just goes over the fire; nothing to do here
            }
            int charges = vessel.getOrDefault(AloneItems.WATER_CHARGES, 0);
            int quality = vessel.getOrDefault(AloneItems.WATER_QUALITY, WaterskinItem.RAW);
            if (charges <= 0 || quality == WaterskinItem.CLEAN) {
                return InteractionResult.PASS; // nothing to boil, or already clean
            }
            if (!level.isClientSide()) {
                float heatLeft = 1f - (float) rock.getDamageValue() / rock.getMaxDamage();
                if (quality == WaterskinItem.SALT) {
                    player.sendSystemMessage(Component.literal(
                        "Boiling won't sweeten seawater — you'd have to distil it.")); // §2
                } else if (heatLeft < BOIL_HEAT_FRACTION) {
                    player.sendSystemMessage(Component.literal("The stone's gone too cool to boil it — reheat it."));
                } else {
                    vessel.set(AloneItems.WATER_QUALITY, WaterskinItem.CLEAN);
                    vessel.set(AloneItems.VESSEL_DIRTY, false); // a rolling boil sterilises the skin too
                    player.setItemInHand(hand, new ItemStack(AloneItems.ROCK)); // spent its heat into the water
                    level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.6f, 1.4f);
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal(
                            "The stone hisses into the water and brings it to a boil — clean."), true);
                    }
                }
            }
            player.swing(hand);
            return InteractionResult.SUCCESS; // consume the click either way (don't also start some other use)
        });
    }

    private static void coolRocks(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(AloneItems.HOT_ROCK)) {
                continue;
            }
            int used = stack.getDamageValue() + 1;
            if (used >= stack.getMaxDamage()) {
                inventory.setItem(i, new ItemStack(AloneItems.ROCK)); // gone cold — just a stone again
            } else {
                stack.setDamageValue(used);
            }
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
