package dev.alone.food;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Food spoilage (proposal §4.2). Perishable foods (a bundled datapack tag) rot over time — once
 * spoiled they turn to rotten flesh (which is itself high-risk to eat). Bread, golden apples, and
 * other keepers are left out of the tag. Preservation (smoking/salting/drying) that slows this is a
 * later refinement; for now it's a fixed shelf life.
 */
public final class Spoilage {
    private Spoilage() {
    }

    public static final long SPOIL_TICKS = 24000L;            // fresh perishable: ~1 in-game day; tunable
    public static final long PRESERVED_SHELF_TICKS = 720000L; // salted/dried: ~30 in-game days (~a month) —
                                                              // long, but not forever: even jerky goes rancid
    private static final int SCAN_INTERVAL = 40;   // check each player's food every ~2s

    /** Game time at which this food spoils. */
    public static final DataComponentType<Long> SPOILS_AT = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "spoils_at"),
        DataComponentType.<Long>builder().persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG).build());

    /** Salted/dried food keeps far longer (a long finite shelf life, not forever) — see PRESERVED_SHELF (§4.2). */
    public static final DataComponentType<Boolean> PRESERVED = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "preserved"),
        DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    /** Dried (jerked) food specifically — the water's gone, so it's lighter to carry (read by Carry). */
    public static final DataComponentType<Boolean> DRIED = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alone", "dried"),
        DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final TagKey<Item> PERISHABLE =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "perishable_foods"));

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = server.overworld().getGameTime();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % SCAN_INTERVAL != 0) {
                    continue;
                }
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    tickStack(inventory, i, now);
                }
            }
        });
    }

    private static void tickStack(Inventory inventory, int slot, long now) {
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty() || !stack.is(PERISHABLE)) {
            return;
        }
        Long spoilsAt = stack.get(SPOILS_AT);
        if (spoilsAt == null) {
            // Preserved (salted/dried) food gets a long shelf, fresh food a short one — but both DO expire.
            long shelf = stack.getOrDefault(PRESERVED, false) ? PRESERVED_SHELF_TICKS : SPOIL_TICKS;
            stack.set(SPOILS_AT, now + shelf);
        } else if (now >= spoilsAt) {
            inventory.setItem(slot, new ItemStack(Items.ROTTEN_FLESH, stack.getCount())); // spoiled
        }
    }
}
