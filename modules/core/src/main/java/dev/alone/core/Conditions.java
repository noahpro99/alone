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
import net.minecraft.world.entity.monster.zombie.Zombie;
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
    /** Remaining infection in ticks — a dirty (zombie) bite festers: fever, and if it deepens, it kills. */
    public static final AttachmentType<Integer> INFECTION =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "infection"), Codec.INT);

    private static final int BLEED_TICKS = 200;   // ~10s per wound (stacks with more hits)
    private static final int SPRAIN_TICKS = 2400;  // ~2 min limping per bad fall (splint it to mend in ~30s)
    private static final int INFECTION_PER_BITE = 4000; // one bite ≈ a fever that clears; bites compound
    private static final int SEVERE_INFECTION = 6000;   // past this it's winning — the fever drains you
    private static final float INFECT_CHANCE = 0.30f;   // odds a zombie-type bite gets infected

    // Condition flags synced to the HUD.
    public static final int FLAG_SICK = 1;
    public static final int FLAG_BLEEDING = 2;
    public static final int FLAG_SPRAINED = 4;
    public static final int FLAG_DIRTY_HANDS = 8;
    public static final int FLAG_INFECTED = 16;

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
        if (player.getAttachedOrElse(INFECTION, 0) > 0) {
            f |= FLAG_INFECTED;
        }
        return f;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickSickness(player);
                tickBleeding(player);
                tickSprain(player);
                tickInfection(player);
            }
        });
        // Damage lands as conditions (§1.5/§8.6), and armour blunts the chance and severity by type.
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            var rng = player.getRandom();
            float armorGuard = 1.0f - Math.min(0.8f, player.getArmorValue() / 25.0f); // plate turns a slash into a bruise
            // Cuts/claws/arrows open a bleeding wound — likelier the harder the hit, far rarer with armour.
            if (damageTaken >= 2.0f && source.getEntity() instanceof LivingEntity
                && rng.nextFloat() < Math.min(0.9f, damageTaken / 8.0f) * armorGuard) {
                addBleeding(player, BLEED_TICKS);
            }
            // A bad fall sprains; heavy blunt trauma can fracture too (both splinted, §1.5).
            if (source.is(DamageTypes.FALL) && damageTaken >= 4.0f) {
                addSprain(player, SPRAIN_TICKS);
            } else if (damageTaken >= 8.0f && rng.nextFloat() < 0.5f * armorGuard) {
                addSprain(player, SPRAIN_TICKS);
            }
            // A zombie-type bite can fester into infection.
            if (source.getEntity() instanceof Zombie && rng.nextFloat() < INFECT_CHANCE) {
                addInfection(player, INFECTION_PER_BITE);
            }
        });
        // Treatment (§1.5): sneak + right-click with cloth (string/wool/paper) to dress a wound —
        // stops bleeding and cleans an infection back down.
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && isBandage(stack) && (isBleeding(player) || isInfected(player))) {
                if (!level.isClientSide()) {
                    player.removeAttached(BLEEDING);
                    int infection = player.getAttachedOrElse(INFECTION, 0);
                    if (infection > 0) {
                        player.setAttached(INFECTION, Math.max(0, infection - INFECTION_PER_BITE)); // dressing knocks it back
                    }
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("You clean and bind the wound."));
                }
                return InteractionResult.SUCCESS;
            }
            // Splint a sprain (§1.5): sneak + right-click a splint to bind the joint and speed recovery.
            if (player.isShiftKeyDown() && stack.is(AloneItems.SPLINT) && isSprained(player)) {
                if (!level.isClientSide()) {
                    player.setAttached(SPRAIN, player.getAttachedOrElse(SPRAIN, 0) / 4); // bound → mends faster
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("You splint the sprain."));
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    public static boolean isSprained(Player player) {
        return player.getAttachedOrElse(SPRAIN, 0) > 0;
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

    /**
     * Realistic fall consequences through the injury system, not a raw health bar (§1.5). Real fall
     * outcomes for a fit adult on hard ground: safe under ~3.5 m, injury from ~4–6 m, and a rising
     * <b>chance of death</b> — ~50% around 15 m (the classic LD50), near-certain past ~22 m. So a fall
     * <em>rolls</em> for death like real life; survive it and the cost is <b>injuries</b> (a sprain,
     * often a fracture, sometimes internal bleeding) plus a modest hit — the limp and the blood, not
     * just a number. {@code damageMultiplier} (&lt;1 on hay/honey) softens everything. Returns whether
     * any harm landed. Called from {@code PlayerFallMixin}; vanilla fall damage is skipped.
     */
    public static boolean applyFall(ServerPlayer player, double fallDistance, float damageMultiplier,
                                    net.minecraft.world.damagesource.DamageSource source) {
        if (fallDistance <= 3.5 || damageMultiplier <= 0f) {
            return false; // an athlete walks off a small drop
        }
        ServerLevel level = (ServerLevel) player.level();
        var rng = player.getRandom();

        // Death roll: 0 below ~8 m, ~50% at ~15 m, ~certain by ~22 m. Softened by hay/honey.
        float deathChance = net.minecraft.util.Mth.clamp((float) ((fallDistance - 8.0) / 14.0), 0f, 1f)
            * damageMultiplier;
        if (rng.nextFloat() < deathChance) {
            player.hurtServer(level, source, 1000f); // the fall was fatal
            return true;
        }

        // Survived — the injuries are the real cost. A twist/sprain always; a fracture the harder you land.
        addSprain(player, SPRAIN_TICKS);
        float fractureChance = net.minecraft.util.Mth.clamp((float) ((fallDistance - 6.0) / 12.0), 0f, 0.9f)
            * damageMultiplier;
        if (rng.nextFloat() < fractureChance) {
            addSprain(player, SPRAIN_TICKS); // a fracture stacks into a longer, worse limp
        }
        float bleedChance = net.minecraft.util.Mth.clamp((float) ((fallDistance - 8.0) / 16.0), 0f, 0.85f)
            * damageMultiplier;
        if (rng.nextFloat() < bleedChance) {
            addBleeding(player, BLEED_TICKS * 2); // internal/open injury
        }

        // A modest hit on top — you're hurt, but you won't simply die from the number.
        float hurt = (float) ((fallDistance - 3.5) * 0.6) * damageMultiplier;
        if (hurt > 0f) {
            player.hurtServer(level, source, hurt);
        }
        return true;
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

    public static boolean isInfected(Player player) {
        return player.getAttachedOrElse(INFECTION, 0) > 0;
    }

    /** Contract (or worsen) an infection — a dirty wound that festers. */
    public static void addInfection(Player player, int ticks) {
        player.setAttached(INFECTION, Math.min(player.getAttachedOrElse(INFECTION, 0) + ticks, MAX_SICKNESS_TICKS));
    }

    private static void tickInfection(ServerPlayer player) {
        int remaining = player.getAttachedOrElse(INFECTION, 0);
        if (remaining <= 0) {
            return;
        }
        if (remaining % 20 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0, false, false, true)); // feverish, weak
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 40, 0, false, false, true));    // malaise
        }
        // A deep (compounding) infection turns septic — the fever drains vitality until you dress it.
        if (remaining > SEVERE_INFECTION && remaining % 60 == 0) {
            ServerLevel level = (ServerLevel) player.level();
            player.hurtServer(level, level.damageSources().generic(), 1.0f);
        }
        player.setAttached(INFECTION, remaining - 1);
    }

    /** Contract (or worsen) sickness. */
    public static void addSickness(Player player, int ticks) {
        int current = player.getAttachedOrElse(SICKNESS, 0);
        player.setAttached(SICKNESS, Math.min(current + ticks, MAX_SICKNESS_TICKS));
    }

    /** Ease a foodborne illness (herbal remedy) — knock the sickness timer down. */
    public static void relieveSickness(Player player, int ticks) {
        player.setAttached(SICKNESS, Math.max(0, player.getAttachedOrElse(SICKNESS, 0) - ticks));
    }

    /** Ease a festering wound (herbal remedy) — knock the infection back. */
    public static void relieveInfection(Player player, int ticks) {
        player.setAttached(INFECTION, Math.max(0, player.getAttachedOrElse(INFECTION, 0) - ticks));
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
