package dev.alone.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EntityBlock;

/**
 * Timed, stationary crafting (proposal §8.2). A craft isn't instant — once a valid recipe sits in the
 * grid you must <b>work at it</b> for a spell before the result can be taken, roughly in proportion to
 * how long the real thing takes, compressed to the in-game day. You can't take the result early, and
 * because the crafting screen roots you, workshop time becomes a real phase of the day — no panic
 * crafting mid-fight.
 *
 * <p>Progress accrues while the recipe is in the result slot (the gate is {@code SlotCraftTimeMixin}
 * on {@code mayPickup}), shown as a bar on the result slot. It is <b>resumable</b>: progress is kept
 * <em>per result item</em> and survives closing the screen or switching to another recipe and back —
 * you pick up where you left off. Taking a craft resets that item's clock, so each item costs its own
 * time. Run on both sides (side-separated maps) so client and server agree on when it's takeable.
 */
public final class CraftingTime {
    private CraftingTime() {
    }

    // Craft durations in ticks (20/s) — compressed real-world effort. Tunable; a datapack could refine.
    private static final int SIMPLE = 40;    // ~2s: sticks, a torch
    private static final int PLANK = 240;    // ~12s: riving/sawing a log into boards is slow, real work
    // Reverse-wrap twisting fibre into a usable length of cordage is ~40 real minutes by hand (a slow,
    // notorious bushcraft job). At the pack's 72x clock (1 real hour = 1000 ticks) that's ~700 ticks —
    // ~35 wall-clock seconds. Set by real duration, not feel; a snare's two strings is ~70 real minutes.
    private static final int CORDAGE = 700;
                                             // (~20 min/yard, ~6 m/hr real → ~8–17s at the 72x day scale)
    private static final int FOOD = 100;     // ~5s: prep a meal
    private static final int STATION = 1200; // ~60s: a chest, furnace — real joinery/masonry
    private static final int TOOL = 300;     // ~15s: shape and haft a tool or weapon (metal ones then forge)
    private static final int ARMOR = 2400;   // ~2 min assembling the piece — metal is then forged on top
    private static final int BLOOM_WORK = 600; // ~30s: hammering a spongy bloom to consolidate iron and drive out slag
    // Shelter craft, set by real duration × the 72x day (1 real hour = 1000 ticks), not by feel (§5.5):
    private static final int THATCH = 300;      // ~18 real min weaving/lashing a rough grass panel — bulk grass is the material cost, the weave is the timed work
    private static final int SEWN_SHEET = 1200; // ~72 real min hand-stitching tanned hides into an oiled tarp — slow, patient sewing
    private static final int SEWN_BAG = 1800;   // ~108 real min sewing a lofted, shelled sleeping bag — a real half-day project
    private static final int GRIND = 200;       // ~12 real min grinding grain to flour on a hand quern

    // Per player, the accumulated ticks worked on each result item (by item id) — kept so a craft you
    // stepped away from resumes. Side-separated so the integrated server and client don't collide.
    private static final Map<UUID, Map<Integer, Integer>> CLIENT = new HashMap<>();
    private static final Map<UUID, Map<Integer, Integer>> SERVER = new HashMap<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player);
            }
        });
    }

    /** Advance the craft timer for whatever recipe is in the player's grid. Runs each tick, both sides. */
    public static void tick(Player player) {
        ItemStack result = currentResult(player);
        if (result.isEmpty()) {
            return; // nothing on the bench — leave saved progress alone (resumable)
        }
        Map<Integer, Integer> worked = mapFor(player).computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        int itemId = BuiltInRegistries.ITEM.getId(result.getItem());
        int required = craftTicks(result);
        int ticks = worked.getOrDefault(itemId, 0);
        if (ticks >= required) {
            return; // already done; waiting to be taken
        }
        ticks++;
        worked.put(itemId, ticks);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(ticks >= required
                ? "Ready to take."
                : "Crafting… " + (ticks * 100 / required) + "%"), true);
        }
    }

    /** Whether the crafted result may be taken yet — false until its craft time has been worked. */
    public static boolean isReady(Player player, ItemStack result) {
        return workedTicks(player, result) >= craftTicks(result);
    }

    /** 0..1 progress toward completing this result — drives the on-slot bar. */
    public static float progressFraction(Player player, ItemStack result) {
        return Math.min(1f, workedTicks(player, result) / (float) craftTicks(result));
    }

    private static int workedTicks(Player player, ItemStack result) {
        Map<Integer, Integer> worked = mapFor(player).get(player.getUUID());
        return worked == null ? 0 : worked.getOrDefault(BuiltInRegistries.ITEM.getId(result.getItem()), 0);
    }

    /** Taking a result restarts that item's clock, so each item costs its own crafting time. */
    public static void reset(Player player, ItemStack taken) {
        Map<Integer, Integer> worked = mapFor(player).get(player.getUUID());
        if (worked != null) {
            worked.remove(BuiltInRegistries.ITEM.getId(taken.getItem()));
        }
    }

    private static ItemStack currentResult(Player player) {
        if (player.containerMenu == null) {
            return ItemStack.EMPTY;
        }
        for (Slot slot : player.containerMenu.slots) {
            if (slot instanceof ResultSlot && slot.hasItem()) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /** How long this result takes to craft, in ticks — compressed real-world effort by category. */
    public static int craftTicks(ItemStack result) {
        var equippable = result.get(DataComponents.EQUIPPABLE);
        if (equippable != null && isArmorSlot(equippable.slot())) {
            return ARMOR;
        }
        if (result.is(ItemTags.SWORDS) || result.is(ItemTags.AXES) || result.is(ItemTags.PICKAXES)
            || result.is(ItemTags.SHOVELS) || result.is(ItemTags.HOES) || result.isDamageableItem()) {
            return TOOL;
        }
        if (result.is(Items.IRON_INGOT)) {
            // Working a bloomery bloom into a usable ingot is real hammering — consolidating the spongy,
            // slag-riddled lump — not an instant craft. (This is the only way to grid-craft an iron ingot.)
            return BLOOM_WORK;
        }
        if (result.is(AloneItems.THATCH)) {
            return THATCH; // bulk grass is the material cost; weaving and lashing the panel is the timed work (§5.5)
        }
        if (result.is(AloneItems.TARP)) {
            return SEWN_SHEET; // tanned hides hand-sewn into an oiled sheet — slow, patient stitching (§5.5)
        }
        if (result.is(AloneItems.SLEEPING_BAG)) {
            return SEWN_BAG; // a warm bag is a real sewing project — loft and a shell, all by hand (§5.5)
        }
        if (result.is(AloneItems.FLOUR)) {
            return GRIND; // grinding grain to flour by hand (a quern) is slow, tedious work
        }
        if (result.is(ItemTags.PLANKS)) {
            return PLANK; // riving a log into boards by hand is hard, laborious work (§5.4)
        }
        if (result.is(Items.STRING)) {
            return CORDAGE; // twisting plant fibre into a length of string — slow reverse-wrap by hand (§8.1)
        }
        if (result.is(Items.CAMPFIRE) || result.is(Items.SOUL_CAMPFIRE)) {
            return SIMPLE; // just kindling laid in one spot — quick, not workshop joinery (despite its BE)
        }
        if (result.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EntityBlock) {
            return STATION; // chests, furnaces, barrels — anything with a block entity is involved work
        }
        if (result.has(DataComponents.FOOD)) {
            return FOOD;
        }
        return SIMPLE;
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
            || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private static Map<UUID, Map<Integer, Integer>> mapFor(Player player) {
        return player.level().isClientSide() ? CLIENT : SERVER;
    }
}
