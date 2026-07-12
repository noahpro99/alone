package dev.alone.core;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A shield doesn't save you from an animal that hits harder than a person (proposal §1.5 / roadmap combat
 * depth). This repurposes vanilla's own <b>temporary shield-disable</b> (the greyed-out, can't-block cooldown
 * an axe hit inflicts) for beasts, in two tiers by how the real fight goes:
 * <ul>
 *   <li><b>Smashers</b> ({@link #SHIELD_BREAKERS} — bear, bison, ravager, golem, warden): sheer mass bashes a
 *       braced guard <em>aside</em>. A long disable ({@value #DISABLE_TICKS}t ≈ 6s) — you can brace one blow,
 *       but you can't turtle; the follow-up lands on flesh.</li>
 *   <li><b>Staggerers</b> ({@link #SHIELD_STAGGERERS} — the wild boar): a fast, low charge <em>bowls your
 *       guard aside</em> and drives under it at your legs. A brief disable ({@value #STAGGER_TICKS}t ≈ 2s) —
 *       a stagger you recover from, not a shattered guard. So you don't hold block against a boar; you take
 *       the hit off your guard, then strike on its overshoot (the way a boar spear's crossguard, not a shield,
 *       was the real tool). Fast and recoverable, unlike a bear's overwhelming bulk.</li>
 * </ul>
 * The block still spends the shield's durability as vanilla does — so it's drained as well as beaten.
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

    private static final int DISABLE_TICKS = 120; // ~6s — a smasher knocks your guard down; follow-ups land
    private static final int STAGGER_TICKS = 40;  // ~2s — a boar's charge staggers your guard; you recover

    public static void init() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, takenDamage, blocked) -> {
            if (!blocked || !(entity instanceof ServerPlayer player)) {
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
            ItemStack shield = shieldInHand(player);
            if (shield.isEmpty()) {
                return;
            }
            player.getCooldowns().addCooldown(shield, smasher ? DISABLE_TICKS : STAGGER_TICKS);
            player.stopUsingItem(); // drop the block that just soaked the blow
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, smasher ? 0.8f : 1.1f);
            player.sendSystemMessage(Component.literal(smasher
                ? "The blow smashes your guard aside — no shield will hold against that."
                : "The boar's charge bowls your guard aside — it drives in low, under the shield."), true);
        });
    }

    /** The shield the player is guarding with — off hand first (where it's normally held), then main. */
    private static ItemStack shieldInHand(ServerPlayer player) {
        if (player.getOffhandItem().is(Items.SHIELD)) {
            return player.getOffhandItem();
        }
        if (player.getMainHandItem().is(Items.SHIELD)) {
            return player.getMainHandItem();
        }
        return ItemStack.EMPTY;
    }
}
