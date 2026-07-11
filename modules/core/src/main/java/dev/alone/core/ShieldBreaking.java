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
 * Shields don't stop a big animal (proposal §1.5 / roadmap combat depth). A raised shield can turn a
 * person's blade, but bracing it against a <b>charging bear or a ravager</b> is folly — the mass and the
 * power bash your guard aside. So when a creature in {@link #SHIELD_BREAKERS} lands a blow you <em>block</em>,
 * the shield is <b>knocked down</b>: put on cooldown for a few seconds, exactly as an axe-hit disables it,
 * so the follow-up lands on flesh. You can brace one blow; you can't turtle behind a shield against a bear.
 * (The block still spends the shield's durability as vanilla does — so it's drained as well as defeated.)
 */
public final class ShieldBreaking {
    private ShieldBreaking() {
    }

    /** Creatures big and strong enough to smash a braced shield aside — bears, ravagers, golems, the warden. */
    public static final TagKey<EntityType<?>> SHIELD_BREAKERS =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "shield_breakers"));

    private static final int DISABLE_TICKS = 120; // ~6s — your guard is knocked down and follow-ups land

    public static void init() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, takenDamage, blocked) -> {
            if (!blocked || !(entity instanceof ServerPlayer player)) {
                return;
            }
            Entity attacker = source.getDirectEntity();
            if (attacker == null || !attacker.getType().builtInRegistryHolder().is(SHIELD_BREAKERS)) {
                return;
            }
            ItemStack shield = shieldInHand(player);
            if (shield.isEmpty()) {
                return;
            }
            player.getCooldowns().addCooldown(shield, DISABLE_TICKS);
            player.stopUsingItem(); // drop the block that just soaked the blow
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
            player.sendSystemMessage(Component.literal(
                "The blow smashes your guard aside — no shield will hold against that."), true);
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
