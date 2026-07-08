package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Lite seed of the condition system (proposal §1.5). A <em>persistent</em> "sickness" timer on
 * the player that debilitates — weak, unproductive, feverish — for a real stretch, not the few
 * seconds a raw potion effect lasts. It survives relog and keeps ticking down, and it feeds the
 * hunger system (a fever burns calories).
 *
 * <p>This is the first use of the player-data-attachment framework that Core owns. The full
 * condition panel + HUD move to Alone: Body in Phase 2; the attachment defined here stays the
 * source of truth so every future contamination source (bad water, dirty wounds) writes to it.
 */
public final class Conditions {
    private Conditions() {
    }

    /** How long foodborne illness lays you low, in ticks (~4 real minutes). Tunable; becomes config. */
    public static final int FOODBORNE_ILLNESS_TICKS = 4800;
    private static final int MAX_SICKNESS_TICKS = 24000; // cap so it can't stack into oblivion

    /** Remaining sickness in ticks. Persists across save/load. */
    public static final AttachmentType<Integer> SICKNESS =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "sickness"), Codec.INT);
    /** Remaining bleeding in ticks — a wound that drains you until it clots (or is bandaged, later). */
    public static final AttachmentType<Integer> BLEEDING =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "bleeding"), Codec.INT);
    /** Remaining sprain in ticks — a bad fall leaves you limping for a while (§1.5). */
    public static final AttachmentType<Integer> SPRAIN =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "sprain"), Codec.INT);

    private static final int BLEED_TICKS = 200;   // ~10s per wound (stacks with more hits)
    private static final int SPRAIN_TICKS = 600;  // ~30s limping per bad fall

    // Condition flags synced to the HUD.
    public static final int FLAG_SICK = 1;
    public static final int FLAG_BLEEDING = 2;
    public static final int FLAG_SPRAINED = 4;
    public static final int FLAG_DIRTY_HANDS = 8;

    public static int flags(Player player) {
        int f = 0;
        if (player.getAttachedOrElse(SICKNESS, 0) > 0) {
            f |= FLAG_SICK;
        }
        if (player.getAttachedOrElse(BLEEDING, 0) > 0) {
            f |= FLAG_BLEEDING;
        }
        if (player.getAttachedOrElse(SPRAIN, 0) > 0) {
            f |= FLAG_SPRAINED;
        }
        if (Hygiene.handsDirty(player)) {
            f |= FLAG_DIRTY_HANDS;
        }
        return f;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickSickness(player);
                tickBleeding(player);
                tickSprain(player);
            }
        });
        // Damage lands as conditions (§1.5/§8.6): a claw/bite/arrow bleeds you; a bad fall sprains you.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            if (damageTaken >= 2.0f && source.getEntity() instanceof LivingEntity) {
                addBleeding(player, BLEED_TICKS);
            }
            if (damageTaken >= 4.0f && source.is(DamageTypes.FALL)) {
                addSprain(player, SPRAIN_TICKS);
            }
        });
        // Treatment (§1.5): sneak + right-click with cloth (string/wool/paper) to bind a bleeding wound.
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && isBandage(stack) && isBleeding(player)) {
                if (!level.isClientSide()) {
                    player.removeAttached(BLEEDING);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("You bind the wound."));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    public static boolean isBleeding(Player player) {
        return player.getAttachedOrElse(BLEEDING, 0) > 0;
    }

    private static boolean isBandage(ItemStack stack) {
        return stack.is(Items.STRING) || stack.is(Items.PAPER) || stack.is(ItemTags.WOOL);
    }

    /** A bad fall — limp for a while (§1.5). */
    public static void addSprain(Player player, int ticks) {
        player.setAttached(SPRAIN, Math.min(player.getAttachedOrElse(SPRAIN, 0) + ticks, MAX_SICKNESS_TICKS));
    }

    private static void tickSprain(ServerPlayer player) {
        int remaining = player.getAttachedOrElse(SPRAIN, 0);
        if (remaining <= 0) {
            return;
        }
        if (remaining % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1, false, false, true)); // limping
        }
        player.setAttached(SPRAIN, remaining - 1);
    }

    /** Open (or worsen) a bleeding wound. */
    public static void addBleeding(Player player, int ticks) {
        player.setAttached(BLEEDING, Math.min(player.getAttachedOrElse(BLEEDING, 0) + ticks, MAX_SICKNESS_TICKS));
    }

    private static void tickBleeding(ServerPlayer player) {
        int remaining = player.getAttachedOrElse(BLEEDING, 0);
        if (remaining <= 0) {
            return;
        }
        if (remaining % 40 == 0) { // lose a little blood every couple of seconds
            ServerLevel level = (ServerLevel) player.level();
            player.hurtServer(level, level.damageSources().generic(), 1.0f);
        }
        player.setAttached(BLEEDING, remaining - 1);
    }

    /** Contract (or worsen) sickness. */
    public static void addSickness(Player player, int ticks) {
        int current = player.getAttachedOrElse(SICKNESS, 0);
        player.setAttached(SICKNESS, Math.min(current + ticks, MAX_SICKNESS_TICKS));
    }

    public static boolean isSick(Player player) {
        return player.getAttachedOrElse(SICKNESS, 0) > 0;
    }

    private static void tickSickness(ServerPlayer player) {
        Integer remaining = player.getAttached(SICKNESS);
        if (remaining == null || remaining <= 0) {
            if (remaining != null) {
                player.removeAttached(SICKNESS); // recovered
            }
            return;
        }
        // Reapply the debilitation once a second so it persists as the illness runs its course:
        // weak (poor attacks), sluggish work (mining fatigue), and a fever appetite (hunger).
        if (remaining % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 30, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 30, 0, false, false, true));
        }
        player.setAttached(SICKNESS, remaining - 1);
    }
}
