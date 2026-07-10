package dev.alone.core;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Sharpening stone (proposal §8.5) — the maintenance half of the tool loop. Hold a worn tool or weapon,
 * keep a {@link AloneItems#WHETSTONE} in your <b>off hand</b>, and <b>sneak + right-click</b> to hone
 * the edge back up: it restores a chunk of durability, wears the whetstone a little, and takes a moment
 * (a short cooldown = the honing). Repairing an edge is far cheaper than re-forging it — but only a
 * forge (re-temper) can change a blade's <em>quality</em>. Armour can't be sharpened (no edge).
 */
public final class Sharpening {
    private Sharpening() {
    }

    private static final int SHARPEN_PERCENT = 25; // a good hone restores ~a quarter of the edge's life
    private static final int HONE_COOLDOWN = 40;    // ~2s of honing between passes

    public static void init() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack tool = player.getMainHandItem();
            ItemStack whetstone = player.getOffhandItem();
            if (!whetstone.is(AloneItems.WHETSTONE) || !isSharpenable(tool) || tool.getDamageValue() <= 0) {
                return InteractionResult.PASS;
            }
            if (player.getCooldowns().isOnCooldown(whetstone)) {
                return InteractionResult.FAIL; // still honing the last pass
            }
            if (!level.isClientSide()) {
                int restore = Math.max(1, tool.getMaxDamage() * SHARPEN_PERCENT / 100);
                tool.setDamageValue(Math.max(0, tool.getDamageValue() - restore));
                wearWhetstone(player, whetstone);
                player.getCooldowns().addCooldown(whetstone, HONE_COOLDOWN);
                level.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.5f, 1.1f);
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(Component.literal("You hone the edge sharp again."), true);
                }
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        });
    }

    /** A damageable edge — tools and weapons, but not armour (nothing to sharpen). */
    private static boolean isSharpenable(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return false;
        }
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return true;
        }
        return switch (equippable.slot()) {
            case HEAD, CHEST, LEGS, FEET, BODY -> false; // worn armour has no edge
            default -> true;
        };
    }

    private static void wearWhetstone(net.minecraft.world.entity.player.Player player, ItemStack whetstone) {
        if (player.isCreative()) {
            return;
        }
        int damage = whetstone.getDamageValue() + 1;
        if (damage >= whetstone.getMaxDamage()) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY); // worn smooth — spent
        } else {
            whetstone.setDamageValue(damage);
        }
    }
}
