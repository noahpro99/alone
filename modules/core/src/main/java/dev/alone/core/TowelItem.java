package dev.alone.core;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A towel (§5.6 / §1.3) — right-click to <b>rub yourself dry at once</b>, instead of waiting out the ~30 s
 * of natural drying or hugging a fire. It earns its place because <b>wet-cold kills</b> (§1.3): step out of
 * a river or a downpour in cold country and a quick towel-off can be the difference between a passing chill
 * and hypothermia. Drying yourself soaks the towel, so it goes <b>damp</b> and can't dry you again until it
 * has had time to dry out (a cooldown) — and while you're still standing in the rain or the water it just
 * re-wets, so a towel is what you reach for once you're <b>out</b> of the wet.
 */
public class TowelItem extends Item {
    /** How long a used (soaked) towel takes to dry before it can dry you again. */
    private static final int DAMP_TICKS = 1200; // ~60 s

    public TowelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS; // still damp from the last dry-off
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // wetness isn't client-synced — let the server judge it
        }
        if (player.getAttachedOrElse(SurvivalMeters.WETNESS, 0) <= 0) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("You're already dry."), true);
            }
            return InteractionResult.PASS;
        }
        player.setAttached(SurvivalMeters.WETNESS, 0); // rubbed dry
        player.getCooldowns().addCooldown(stack, DAMP_TICKS); // the towel's soaked now — let it dry
        level.playSound(null, player.blockPosition(), SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.5f, 1.1f);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("You rub yourself dry with the towel."), true);
        }
        return InteractionResult.SUCCESS;
    }
}
