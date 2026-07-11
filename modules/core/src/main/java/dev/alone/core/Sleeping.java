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
 * hand to catch a fitful rest — the <b>worst</b> sleep, only <b>at night</b> with <b>no rain on you</b>.
 * It sheds some fatigue and restores some stamina, then <b>lays you down</b> on the spot the way a bed
 * does; {@link GradualSleep} runs the clock fast to dawn from there, just at the worst comfort. The
 * bedroll (a real bed block) does it warmer, and sets your spawn.
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

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            // Bare-handed right-click on the top of bare ground (grass/dirt/sand). No sneak needed —
            // but we only react when the hand is empty and you're aiming at the top face, so normal
            // play doesn't trip it. grass_block is in BlockTags.DIRT, so grass counts.
            if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
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
     * Rest, if it's night and no rain is on you (shared by ground-sleep and the bedroll). Tells you
     * <em>why</em> it won't work so it isn't a silent no-op; a cooldown gates the actual recovery.
     */
    public static boolean tryRest(Player player, Level level, float fatigueShed, float staminaRestore, String message) {
        long timeOfDay = level.getOverworldClockTime() % 24000L;
        boolean night = timeOfDay >= 13000L && timeOfDay < 23000L;
        if (!night) {
            say(player, level, "It's not dark enough to bed down — wait for night.");
            return true; // consume the click; we explained why
        }
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
