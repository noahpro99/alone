package dev.alone.core;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;

/**
 * A shield doesn't save you from an animal that hits harder than a person (proposal §1.5 / roadmap combat
 * depth). This drives beasts through vanilla's <b>own</b> shield-disable — the exact code path a vindicator's
 * axe uses: {@link BlocksAttacks#disable} on the blocking item, given a number of seconds. That's the greyed-
 * out, can't-block cooldown (with the shield's own disable sound, its {@code disable_cooldown_scale}, and the
 * client HUD sync) — not a hand-rolled cooldown. We just supply the seconds, in two tiers by how the real
 * fight goes:
 * <ul>
 *   <li><b>Smashers</b> ({@link #SHIELD_BREAKERS} — bear, bison, ravager, golem, warden): sheer mass bashes a
 *       braced guard <em>aside</em>. A long disable ({@value #SMASH_SECONDS}s) — you can brace one blow, but you
 *       can't turtle; the follow-up lands on flesh.</li>
 *   <li><b>Staggerers</b> ({@link #SHIELD_STAGGERERS} — the wild boar): a fast, low charge <em>bowls your
 *       guard aside</em> and drives under it at your legs. A brief disable ({@value #STAGGER_SECONDS}s) — a
 *       stagger you recover from, not a shattered guard. So you don't hold block against a boar; you take the
 *       hit off your guard, then strike on its overshoot (the way a boar spear's crossguard, not a shield, was
 *       the real tool). Fast and recoverable, unlike a bear's overwhelming bulk.</li>
 * </ul>
 * The block still spends the item's durability as vanilla does — so it's drained as well as beaten.
 */
public final class ShieldBreaking {
    private ShieldBreaking() {
    }

    /** Creatures big and strong enough to smash a braced shield aside — bears, bison, ravagers, golems, warden. */
    public static final TagKey<EntityType<?>> SHIELD_BREAKERS =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "shield_breakers"));

    /** Fast, low chargers that bowl a guard aside for a moment rather than shattering it — the wild boar. */
    public static final TagKey<EntityType<?>> SHIELD_STAGGERERS =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "shield_staggerers"));

    private static final float SMASH_SECONDS = 6.0f;   // a smasher knocks your guard down; follow-ups land
    private static final float STAGGER_SECONDS = 2.0f; // a boar's charge staggers your guard; you recover

    public static void init() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, takenDamage, blocked) -> {
            if (!blocked || !(entity instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            Entity attacker = source.getDirectEntity();
            if (attacker == null) {
                return;
            }
            boolean smasher = attacker.getType().builtInRegistryHolder().is(SHIELD_BREAKERS);
            boolean staggerer = attacker.getType().builtInRegistryHolder().is(SHIELD_STAGGERERS);
            if (!smasher && !staggerer) {
                return;
            }
            // The item that just soaked the blow — normally the shield being blocked with (fall back to a
            // shield in hand). It must actually be a blocking item (carry BLOCKS_ATTACKS) to be disabled.
            ItemStack blockingWith = player.getItemBlockingWith();
            if (blockingWith == null || blockingWith.isEmpty()) {
                blockingWith = shieldInHand(player);
            }
            BlocksAttacks blocksAttacks = blockingWith.isEmpty() ? null : blockingWith.get(DataComponents.BLOCKS_ATTACKS);
            if (blocksAttacks == null) {
                return;
            }
            // Vanilla's exact disable (the vindicator-axe path): cooldown + stop blocking + the shield's own
            // disable sound + client sync — but for LONGER the flimsier the shield: a wicker guard is knocked
            // aside far longer than a metal-faced one (see shieldToughness).
            float seconds = (smasher ? SMASH_SECONDS : STAGGER_SECONDS) / shieldToughness(blockingWith);
            blocksAttacks.disable(level, player, seconds, blockingWith);
            player.sendSystemMessage(Component.literal(smasher
                ? "The blow smashes your guard aside — no shield will hold against that."
                : "The boar's charge bowls your guard aside — it drives in low, under the shield."), true);
        });
    }

    /** How well a shield resists being bashed aside — the divisor on the disable time, so a tougher shield is
     *  knocked down for less. The metal-faced vanilla shield is the baseline (1.0); the plank shield gives a
     *  little, and a woven wicker guard is knocked flying (bashed aside far longer). */
    private static float shieldToughness(ItemStack shield) {
        if (shield.is(AloneItems.WICKER_SHIELD)) {
            return 0.6f;  // flimsy — disabled ~1.6x as long
        }
        if (shield.is(AloneItems.WOODEN_SHIELD)) {
            return 0.8f;  // solid boards — disabled ~1.25x as long
        }
        return 1.0f;      // metal-reinforced (the vanilla shield) — the baseline
    }

    /** The shield the player is guarding with — any blocking item (carries BLOCKS_ATTACKS), off hand first
     *  (where a shield is normally held), then main, so every shield tier is caught, not just the vanilla one. */
    private static ItemStack shieldInHand(ServerPlayer player) {
        if (player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)) {
            return player.getOffhandItem();
        }
        if (player.getMainHandItem().has(DataComponents.BLOCKS_ATTACKS)) {
            return player.getMainHandItem();
        }
        return ItemStack.EMPTY;
    }
}
