package dev.alone.core;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Butchering (proposal §7.3). A clean kill with a <b>blade</b> lets you work the carcass properly —
 * killing a hide-bearing animal with a knife, sword, axe or spear salvages its <b>leather (hide)</b> and
 * its <b>bone</b> on top of the usual meat, so the whole animal is used and the flint knife and forged
 * blades pay off in the hunt. Beat one to death with your fists (or a pickaxe) and you waste the hide and
 * bone both. Which animals are butcherable, and what counts as a blade, are datapack tags
 * ({@code alone:hide_bearing} / {@code alone:blades}). Bone feeds bonemeal — and so the farming that keeps
 * scurvy at bay.
 */
public final class Hunting {
    private Hunting() {
    }

    public static final TagKey<EntityType<?>> HIDE_BEARING =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "hide_bearing"));
    public static final TagKey<Item> BLADES =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "blades"));

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) {
                return;
            }
            if (!entity.getType().builtInRegistryHolder().is(HIDE_BEARING)) {
                return;
            }
            if (!(source.getEntity() instanceof Player player) || !player.getMainHandItem().is(BLADES)) {
                return;
            }
            int hide = 1 + player.getRandom().nextInt(2); // 1–2 leather salvaged from the skin
            entity.spawnAtLocation(level, new ItemStack(Items.LEATHER, hide));
            int bone = 1 + player.getRandom().nextInt(2); // 1–2 bone worked out of the carcass
            entity.spawnAtLocation(level, new ItemStack(Items.BONE, bone));
        });
    }
}
