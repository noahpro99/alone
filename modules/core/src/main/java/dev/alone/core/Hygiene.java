package dev.alone.core;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Surface / hand contamination (proposal §5.6) — butchering an animal leaves your hands dirty, and
 * eating with dirty hands raises the odds of sickness. Wash them in water (any drink from a source)
 * to clean up. The system quietly teaches the real habit: wash before you eat.
 */
public final class Hygiene {
    private Hygiene() {
    }

    public static final AttachmentType<Boolean> HANDS_DIRTY =
        AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("alone", "hands_dirty"), Codec.BOOL);

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && entity instanceof LivingEntity) {
                player.setAttached(HANDS_DIRTY, true); // butchering / handling a kill
            }
            return InteractionResult.PASS;
        });
    }

    public static boolean handsDirty(Player player) {
        return player.getAttachedOrElse(HANDS_DIRTY, false);
    }

    public static void wash(Player player) {
        player.setAttached(HANDS_DIRTY, false);
    }
}
