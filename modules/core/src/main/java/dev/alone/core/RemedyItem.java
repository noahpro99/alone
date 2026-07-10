package dev.alone.core;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * A steeped herbal remedy (proposal §1.5 medicine) — the treatment the <b>sickness</b> condition was
 * missing. Drink it (a moment, like any brew) to settle a foodborne illness and take the edge off a
 * festering wound: it knocks the sickness timer well down and eases an infection, and clears the acute
 * nausea/poison of a bad meal. It won't fully cure a deep infection — dress that with cloth — but it
 * turns a lost afternoon of illness into a manageable one.
 */
public class RemedyItem extends Item {
    private static final int SICKNESS_RELIEF = 3000;   // ~2.5 min off the illness
    private static final int INFECTION_RELIEF = 2000;  // eases a fever, doesn't cure a deep one

    public RemedyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand); // steeped and sipped — takes a moment
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40; // ~2s to drink it down
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!level.isClientSide()) {
                Conditions.relieveSickness(player, SICKNESS_RELIEF);
                Conditions.relieveInfection(player, INFECTION_RELIEF);
                player.removeEffect(MobEffects.NAUSEA);
                player.removeEffect(MobEffects.POISON);
                player.sendSystemMessage(Component.literal("The bitter brew settles your stomach."));
            }
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return stack;
    }
}
