package dev.alone.core;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The slingshot (roadmap: the loadout — a low-power ranged weapon <b>below the bow</b>, for small game). A
 * forked stick, two bands, and a leather pouch that flings a foraged <b>loose rock</b> ({@link
 * AloneItems#ROCK}) — you <b>draw</b> (hold right-click) to build power and <b>loose</b> (release) to sling
 * a stone at whatever you're aiming at. It's a real hunting tool for <b>birds and rabbits</b>: a stinging
 * pebble that drops small game but barely troubles anything big, so it's the first ranged food-getter, not a
 * war weapon. A stone flies true and fast, so it's a hitscan — but the pebble is stopped by a wall, and a
 * weak draw carries only a short way.
 */
public class SlingshotItem extends Item {
    private static final int FULL_DRAW = 16;        // ~0.8s to wind up a full-power sling
    private static final int MIN_DRAW = 4;          // a flick doesn't loose a stone
    private static final float MAX_DAMAGE = 3.5f;   // enough to drop a rabbit/bird at a full draw; little else
    private static final double MAX_RANGE = 20.0;   // a well-slung stone carries a fair way
    private static final int COOLDOWN = 12;         // a beat to fit the next stone in the pouch

    public SlingshotItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isCreative() && !hasAmmo(player)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("No stones to sling — forage a loose rock."), true);
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Held like a bow — you draw as long as you hold, and {@link #releaseUsing} looses on release. */
    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int drawTicks = getUseDuration(stack, entity) - timeLeft;
        if (drawTicks < MIN_DRAW) {
            return false; // barely drawn — no shot
        }
        float power = Math.min(1f, drawTicks / (float) FULL_DRAW);
        if (!level.isClientSide()) {
            if (!player.isCreative() && !consumeAmmo(player)) {
                return false; // out of stones between the draw and the loose
            }
            loose(level, player, stack, power);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return true;
    }

    /** Sling the stone: a hitscan along the aim, stopped by blocks, damage &amp; reach scaled by the draw. */
    private void loose(Level level, Player player, ItemStack stack, float power) {
        level.playSound(null, player.blockPosition(), SoundEvents.EGG_THROW, SoundSource.PLAYERS,
            0.8f, 1.2f + level.getRandom().nextFloat() * 0.3f);
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player,
            e -> e != player && !e.isSpectator() && e.isPickable(), MAX_RANGE * power);
        if (hit.getType() == HitResult.Type.ENTITY && level instanceof ServerLevel serverLevel) {
            Entity target = ((EntityHitResult) hit).getEntity();
            // Attribute the hit to the player so a kill butchers, tracks, and drops as a player kill would.
            target.hurtServer(serverLevel, level.damageSources().playerAttack(player), MAX_DAMAGE * power);
        }
        if (!player.isCreative()) {
            stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND); // the bands and pouch wear with use
        }
    }

    private static boolean hasAmmo(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(AloneItems.ROCK)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeAmmo(Player player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.is(AloneItems.ROCK)) {
                slot.shrink(1);
                return true;
            }
        }
        return false;
    }
}
