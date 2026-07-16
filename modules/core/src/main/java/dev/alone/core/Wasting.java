package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

/**
 * <b>Wasting</b> — the long-horizon body-condition arc that makes this feel like <em>Alone</em> (the real
 * reason people leave that show is cumulative weight loss over weeks, not any single event). The short-term
 * hunger bar ({@link SurvivalMeters}/vanilla {@link FoodData}) is your day-to-day energy; this is the reserve
 * <em>behind</em> it — your condition, i.e. body fat and muscle. It moves slowly, over in-game <b>days</b>,
 * according to your running energy balance:
 *
 * <ul>
 *   <li><b>Well-fed</b> (a genuinely full stomach) → you're in surplus and slowly <b>rebuild</b> condition.</li>
 *   <li><b>Just getting by</b> (not hungry, but not full) → a survival diet is rarely true surplus, so you
 *       still <b>slowly waste</b> — the quiet, always-losing math that defines a long stint out there.</li>
 *   <li><b>Hungry</b> → real deficit; condition <b>falls</b>. <b>Starving</b> (empty) → it <b>plummets</b>.</li>
 * </ul>
 *
 * <p><b>You can see it happen:</b> condition drives your <b>maximum health</b> — as you waste, your heart bar
 * <em>shrinks</em> (a gaunt, frail body), and eating well over days brings the hearts back. So the vanilla
 * heart display doubles as your weight gauge, no extra UI. Bottom out and you're in medical-pull territory:
 * only a few hearts, too weak to work ({@link MobEffects#WEAKNESS}/{@link MobEffects#SLOWNESS}), and unable to
 * mend — the slow, hard-to-reverse failure. Recovering from deep wasting takes far longer than falling into it
 * (a one-way ratchet you have to stay ahead of), exactly as a real caloric hole does.
 */
public final class Wasting {
    private Wasting() {
    }

    /** Body condition, 0 (wasted away) .. 100 (well-nourished). Persistent; a fresh/respawned body starts full. */
    public static final AttachmentType<Float> CONDITION =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "body_condition"), Codec.FLOAT);

    public static final float FULL = 100f;

    // Energy-balance thresholds, read off the vanilla food bar (0..20).
    private static final int WELL_FED = 17;   // a genuinely full stomach — the only state that rebuilds condition
    private static final int HUNGRY = 6;       // below this you're in real deficit (matches SurvivalMeters' hunger floor)

    // Per-tick drift (tick runs every server tick). Losing outpaces gaining — the ratchet.
    private static final float GAIN = 0.0003f;    // well-fed: full recovery over ~14 in-game days of eating well
    private static final float DRIFT = 0.0002f;   // getting-by: still wastes, ~20 days full→gone on maintenance rations
    private static final float LOSS = 0.0005f;    // hungry: ~8 in-game days of deficit full→wasted
    private static final float STARVE = 0.0013f;  // empty stomach: the body eats itself — ~3 days full→wasted

    private static final float GAUNT = 40f;       // visibly thin and flagging
    private static final float CRITICAL = 15f;     // medical-pull territory: frail, weak, can't mend
    private static final float MAX_HP_PENALTY = 10f; // fully wasted = 10 max health (5 hearts); linear with condition

    /** Touching this class registers its attachment. */
    public static void init() {
    }

    public static float getCondition(Player player) {
        return player.getAttachedOrElse(CONDITION, FULL);
    }

    /** Wasted to the point of frailty — used to gate healing (a starving body can't mend) and for the HUD. */
    public static boolean isCritical(Player player) {
        return getCondition(player) <= CRITICAL;
    }

    /** Restore condition to full — for {@code /alone reset} and any "carried home, recovered" hooks. */
    public static void restore(Player player) {
        player.setAttached(CONDITION, FULL);
    }

    /** Per-tick: drift condition by energy balance, then apply its frailty (shrunken hearts, weakness). Server-side. */
    public static void tick(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            clearFrailty(player); // no wasting in creative — make sure hearts aren't left shrunk
            return;
        }
        float condition = getCondition(player);
        int food = player.getFoodData().getFoodLevel();

        // Running energy balance off the food bar: full → surplus (gain), hungry → deficit (loss), empty →
        // wasting, and merely-not-hungry still bleeds a little (a survival diet is seldom true surplus).
        float delta;
        if (food <= 0) {
            delta = -STARVE;
        } else if (food < HUNGRY) {
            delta = -LOSS;
        } else if (food >= WELL_FED) {
            delta = GAIN;
        } else {
            delta = -DRIFT;
        }
        condition = Math.max(0f, Math.min(FULL, condition + delta));
        player.setAttached(CONDITION, condition);

        applyFrailty(player, condition);
    }

    /** Condition → maximum health: a wasted body carries fewer hearts. Linear, so the heart bar visibly tracks
     *  your weight, and eating back to full brings the hearts back. */
    private static void applyFrailty(ServerPlayer player, float condition) {
        float penalty = (FULL - condition) / FULL * MAX_HP_PENALTY;
        float target = 20f - penalty;
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null && maxHp.getBaseValue() != target) {
            maxHp.setBaseValue(target);
        }
        // Don't let current health hover above the new, lower ceiling — the frame shrinks with you.
        if (player.getHealth() > target) {
            player.setHealth(target);
        }

        if (condition <= CRITICAL) {
            // Badly wasted — frail and feeble, hard work is beyond you (the "medical pull" state).
            frail(player, MobEffects.WEAKNESS);
            frail(player, MobEffects.SLOWNESS);
            frail(player, MobEffects.MINING_FATIGUE);
            if (player.tickCount % 1200 == 0) {
                player.sendSystemMessage(Component.literal(
                    "You're wasting away — gaunt, frail, barely any strength left. You must eat well, and soon."));
            }
        } else if (condition <= GAUNT && player.tickCount % 1800 == 0) {
            player.sendSystemMessage(Component.literal(
                "You've grown gaunt — you're slowly wasting from too little food. Eat well to rebuild your strength."));
        }
    }

    /** Restore the max-health frame to full (creative, or a full recovery) so hearts aren't left shrunk. */
    private static void clearFrailty(ServerPlayer player) {
        AttributeInstance maxHp = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null && maxHp.getBaseValue() != 20f) {
            maxHp.setBaseValue(20f);
        }
    }

    private static void frail(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        player.addEffect(new MobEffectInstance(effect, 40, 0, false, false, true));
    }
}
