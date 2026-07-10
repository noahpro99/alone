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
 * Butchering (proposal §7.3). A clean kill with a <b>blade</b> lets you salvage a hide-bearing animal's
 * skin — killing it with a knife, sword or axe drops <b>leather (hide)</b> on top of the usual meat, so
 * the flint knife and forged blades pay off in the hunt. Beat an animal to death with your fists (or a
 * pickaxe) and you waste the hide. Which animals have a hide, and what counts as a blade, are datapack
 * tags ({@code alone:hide_bearing} / {@code alone:blades}).
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
        });
    }
}
