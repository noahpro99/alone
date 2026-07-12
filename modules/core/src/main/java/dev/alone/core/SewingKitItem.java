package dev.alone.core;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A sewing kit (§5.5) — a bone needle and a length of cordage thread, the way you keep hide clothing
 * alive. There's no anvil in the wild: worn <b>leather/hide armour</b> is mended by hand, and this is the
 * tool. Aim it at nothing in particular and <b>hold right-click</b> to sew: it patches the <b>most worn</b>
 * leather piece you're wearing, spending one <b>plant fibre</b> as thread each time and fraying the kit a
 * little with use. Slow, patient work — a stitch at a time — but it's the only way to claw a favourite set
 * of hides back from ruin. (Metal plates are forged, not sewn, so the kit won't touch them.)
 */
public class SewingKitItem extends Item {
    /** A mend restores this fraction of the garment's life — a good patch, not a full re-make. */
    private static final int REPAIR_PERCENT = 35;
    /** Sewing a patch takes a moment of steady work — a hold, not a tap. */
    private static final int SEW_TICKS = 50;

    public SewingKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (mostWornMendable(player) == null) {
            return InteractionResult.PASS; // nothing worn that's leather and actually damaged
        }
        if (!hasThread(player)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                    Component.literal("You need plant fibre for thread to sew with."), true);
            }
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return SEW_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE; // no vanilla pose fits stitching; the sound + delay carry it
    }

    /** The needle working through hide — a soft, repeated stitch while you sew. */
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (!level.isClientSide() && remainingUseTicks % 10 == 0) {
            level.playSound(null, entity.blockPosition(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS,
                0.4f, 0.9f + entity.getRandom().nextFloat() * 0.2f);
        }
    }

    /** The patch lands when the stitching finishes: mend the worst piece, spend a thread, wear the kit. */
    @Override
    public ItemStack finishUsingItem(ItemStack kit, Level level, LivingEntity entity) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return kit;
        }
        ItemStack target = mostWornMendable(player);
        if (target == null || !useThread(player)) {
            return kit; // gear got swapped, or the thread's gone — nothing to do
        }
        int max = target.getMaxDamage();
        int repair = Math.max(1, max * REPAIR_PERCENT / 100);
        target.setDamageValue(Math.max(0, target.getDamageValue() - repair));
        EquipmentSlot slot = player.getUsedItemHand() == InteractionHand.MAIN_HAND
            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        kit.hurtAndBreak(1, player, slot); // needles bend and thread runs out — the kit wears down
        level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.5f, 1.1f);
        return kit;
    }

    /** The most-damaged leather/hide piece you're wearing, or null if none is worn and worn-out. */
    private static ItemStack mostWornMendable(Player player) {
        ItemStack best = null;
        int worst = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (isMendable(worn) && worn.isDamaged() && worn.getDamageValue() > worst) {
                worst = worn.getDamageValue();
                best = worn;
            }
        }
        return best;
    }

    /** Hide/leather clothing — what a needle and thread can mend (metal plates are forged, not sewn). */
    private static boolean isMendable(ItemStack stack) {
        return stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
            || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)
            || stack.is(Items.TURTLE_HELMET);
    }

    private static boolean hasThread(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(AloneItems.PLANT_FIBER)) {
                return true;
            }
        }
        return false;
    }

    /** Spend one plant fibre as thread; true if one was found and consumed. */
    private static boolean useThread(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(AloneItems.PLANT_FIBER)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }
}
