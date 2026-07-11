package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The bottom rung of the shelter ladder (proposal §1.4/§5.2): right-click bare ground with an empty
 * hand to catch a fitful rest — the <b>worst</b> sleep, needing only that <b>no rain is on you</b>. It
 * works <b>any time of day</b>: a night's sleep, or by day a way to <b>lie down and pass the hours</b>
 * (mending an injury, waiting out weather, letting crops grow). It sheds some fatigue and stamina, then
 * <b>lays you down</b> on the spot the way a bed does, and {@link GradualSleep} runs the clock fast to
 * first light from there — just at the worst comfort. The bedroll (a real bed block) does it warmer, and
 * sets your spawn.
 */
public final class Sleeping {
    private Sleeping() {
    }

    private static final long COOLDOWN = 1200L; // ~60s between ground-rests
    private static final float FATIGUE_SHED = 40f;
    private static final float STAMINA_RESTORE = 50f;

    /** Game time of the player's last ground-rest, for the cooldown. */
    public static final AttachmentType<Long> LAST_REST =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "last_rest"), Codec.LONG);

    /** Set while you're bedded down on bare ground (no bed block). {@code LivingEntitySleepMixin} reads it so
     *  the engine doesn't wake you for want of a bed. Transient — a fresh login isn't mid-rest. */
    public static final AttachmentType<Boolean> GROUND_RESTING = AttachmentRegistry.createDefaulted(
        Identifier.fromNamespaceAndPath("alone", "ground_resting"), () -> false);

    public static void init() {
        // Once you're up (at dawn, or woken), you're no longer ground-resting — so a later real-bed sleep
        // isn't wrongly treated as bedless.
        net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
            if (entity instanceof Player p) {
                p.setAttached(GROUND_RESTING, false);
            }
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            // Deliberate: SNEAK + bare hand + right-click the top of bare ground (grass/dirt/sand). Since
            // resting works any time of day now, the sneak means a stray ground-click during normal play
            // won't lie you down and fast-forward the clock by accident. grass_block is in BlockTags.DIRT.
            if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()
                || !player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            var groundPos = hit.getBlockPos();
            var ground = level.getBlockState(groundPos);
            // You may have clicked the grass/fern/flower growing on the soil — look through it to the
            // ground beneath (so grassy terrain works, not just bare dirt).
            if (!isSoftGround(ground) && ground.canBeReplaced()
                && isSoftGround(level.getBlockState(groundPos.below()))) {
                groundPos = groundPos.below();
                ground = level.getBlockState(groundPos);
            }
            if (!isSoftGround(ground)) {
                // Sneaking on a hard floor clearly means "I want to sleep here" — so say why you can't,
                // instead of a silent no-op.
                if (player.isShiftKeyDown() && hit.getDirection() == Direction.UP
                    && ground.isFaceSturdy(level, groundPos, Direction.UP)) {
                    say(player, level, "You can't bed down on this — you need soft ground (dirt, grass, sand) or a bedroll.");
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
            return tryRest(player, level, FATIGUE_SHED, STAMINA_RESTORE, "You rest fitfully on the cold ground.")
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    /**
     * Rest on the ground, so long as no rain is on you — <b>any time of day</b>. At night it's sleep; by
     * day it's lying down to <b>pass the hours</b> (convalescing an injury, waiting out weather, letting a
     * crop grow), which {@link GradualSleep} fast-forwards to first light. Tells you why it won't work so
     * it isn't a silent no-op; a cooldown gates the actual recovery.
     */
    public static boolean tryRest(Player player, Level level, float fatigueShed, float staminaRestore, String message) {
        if (player.isInWaterOrRain()) {
            say(player, level, "You're too exposed to the rain to rest — find cover.");
            return true;
        }
        if (!level.isClientSide()) {
            long now = level.getGameTime();
            long since = now - player.getAttachedOrElse(LAST_REST, -COOLDOWN);
            if (since >= COOLDOWN) {
                player.setAttached(LAST_REST, now);
                SurvivalMeters.rest(player, fatigueShed, staminaRestore);
                player.sendSystemMessage(Component.literal(message));
                // Lie down like a bed does — bed down on the spot. With the ground-rest happening at
                // night, GradualSleep then runs the clock fast to dawn (and wakes you), just at the
                // worst comfort. (If the engine won't hold a bedless sleeper down, the recovery above
                // still stands, so resting never breaks.)
                if (!player.isSleeping()) {
                    player.startSleeping(player.blockPosition());
                    player.setAttached(GROUND_RESTING, true); // no bed here — LivingEntitySleepMixin keeps you down
                }
            } else {
                player.sendSystemMessage(Component.literal(
                    "You've only just rested — give it " + ((COOLDOWN - since) / 20L) + "s."));
            }
        }
        return true;
    }

    /** Soft enough to bed down on — dirt/grass or sand. */
    private static boolean isSoftGround(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND);
    }

    private static void say(Player player, Level level, String text) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal(text));
        }
    }
}
