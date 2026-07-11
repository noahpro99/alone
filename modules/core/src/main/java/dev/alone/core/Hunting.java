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
 * killing a hide-bearing animal with a knife, sword, axe or spear salvages its <b>leather (hide)</b>, its
 * <b>bone</b>, and often a strand of <b>sinew</b> (cordage — string) on top of the usual meat, so the whole
 * animal is used and the flint knife and forged blades pay off in the hunt. Beat one to death with your
 * fists (or a pickaxe) and you waste it all. Which animals are butcherable, and what counts as a blade, are
 * datapack tags ({@code alone:hide_bearing} / {@code alone:blades}). Bone feeds bonemeal — and so the
 * farming that keeps scurvy at bay; sinew is animal cordage alongside plant fibre.
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
            if (!(source.getEntity() instanceof Player player)) {
                return;
            }
            Skills.gain(player, Skills.TRACKING, 1); // you learn the quarry by taking it — blade or not
            if (!player.getMainHandItem().is(BLADES)) {
                return; // no blade → no clean butchering (but the hunt still taught you)
            }
            int hide = 1 + player.getRandom().nextInt(2); // 1–2 leather salvaged from the skin
            entity.spawnAtLocation(level, new ItemStack(Items.LEATHER, hide));
            int bone = 1 + player.getRandom().nextInt(2); // 1–2 bone worked out of the carcass
            entity.spawnAtLocation(level, new ItemStack(Items.BONE, bone));
            if (player.getRandom().nextFloat() < 0.6f) {
                // Sinew from the tendons — a bit of animal cordage (string), when the carcass yields it.
                entity.spawnAtLocation(level, new ItemStack(Items.STRING, 1));
            }
        });
    }
}
