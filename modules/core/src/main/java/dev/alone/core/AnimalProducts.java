package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/**
 * Realism for the things animals <em>give</em> you — milk and eggs (proposal §7.2).
 *
 * <p>Vanilla treats these as free, infinite taps: a cow can be milked every tick for as many buckets
 * as you have, and an egg is a throwable toy, not food. Neither is how it works. A cow's udder holds
 * roughly a day's yield and then it must refill; an egg is a meal once you cook it. This class owns the
 * shared state and tuning for both; the behaviour lives in the mixins/recipes that read it:
 *
 * <ul>
 *   <li><b>Milk on a schedule</b> — {@link dev.alone.core.mixin.CowMilkCooldownMixin} stamps
 *       {@link #LAST_MILKED} on the cow when you draw a bucket and refuses another until the udder has
 *       refilled ({@link #MILK_COOLDOWN_TICKS}).</li>
 *   <li><b>Drinkable milk for nutrition</b> — {@link dev.alone.core.mixin.ItemStackDrinkMixin} feeds
 *       {@link #MILK_NUTRITION}/{@link #MILK_SATURATION} on top of the thirst it already restores, so a
 *       bucket of milk is a modest meal (it does <b>not</b> cheese the pack's conditions — those are
 *       persistent attachments, not the mob effects vanilla milk clears; see that mixin).</li>
 *   <li><b>Cookable eggs</b> — {@link AloneItems#COOKED_EGG} plus the {@code cooked_egg_from_*} recipes
 *       turn a raw egg (throwable, inedible) into a small cooked food.</li>
 * </ul>
 */
public final class AnimalProducts {
    private AnimalProducts() {
    }

    /**
     * When this cow was last milked, as a total-world-game-time tick stamp. Persistent (survives relog)
     * and <b>synced to clients</b> — {@code mobInteract} runs client-side for prediction, so the client
     * must know the cooldown too or it would flash a filled bucket the server then rejects.
     */
    public static final AttachmentType<Long> LAST_MILKED = AttachmentRegistry.<Long>builder()
        .persistent(Codec.LONG)
        .syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.all())
        .buildAndRegister(Identifier.fromNamespaceAndPath("alone", "last_milked"));

    /** How long a cow's udder takes to refill after milking — ~one in-game day (24000 ticks). */
    public static final int MILK_COOLDOWN_TICKS = 24000;

    /** Hunger a bucket of milk restores when drunk — modest; it's a drink, not a roast (§1.1). */
    public static final int MILK_NUTRITION = 3;
    /** Saturation modifier for that bucket of milk — a little staying power from the fat. */
    public static final float MILK_SATURATION = 0.3f;

    /**
     * True if this cow may be milked now — either it has never been milked or the udder has had time to
     * refill since the last draw. {@code now} is {@code level.getGameTime()}.
     */
    public static boolean udderRefilled(Long lastMilked, long now) {
        return lastMilked == null || now - lastMilked >= MILK_COOLDOWN_TICKS;
    }

    /** Touching this class registers its attachment (and so wires the milk schedule). */
    public static void init() {
    }
}
