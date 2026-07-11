package dev.alone.core;

import dev.alone.core.net.DrinkRequestPayload;
import dev.alone.core.net.FireDrillPayload;
import dev.alone.core.net.SurvivalSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alone: Core — the foundation module.
 *
 * <p>For now it also carries the very first grounding rule so the whole toolchain has
 * something visible to prove itself against (proposal §8.1: you fell a tree with a tool,
 * not your fist). That rule will move to Alone: Craft once that module exists.
 */
public class AloneCore implements ModInitializer {
    public static final String MOD_ID = "alone-core";
    /** Shared content namespace for tags, attachments, packets across all Alone modules. */
    public static final String NAMESPACE = "alone";
    public static final Logger LOGGER = LoggerFactory.getLogger("Alone: Core");

    /** Proper felling tool — an axe (proposal §8.1). Uses the vanilla item tag. */
    public static final TagKey<Item> AXES =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("minecraft", "axes"));
    /** Crude choppers (a bladed/hard edge) that can *barely* fell a log. Datapack-defined. */
    public static final TagKey<Item> CRUDE_CHOPPERS =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", "crude_choppers"));

    /** How well the held item can fell a log. */
    public enum LogTool { AXE, CRUDE, NONE }

    public static LogTool classifyForLog(ItemStack held) {
        if (held.is(AXES)) {
            return LogTool.AXE;
        }
        if (held.is(CRUDE_CHOPPERS)) {
            return LogTool.CRUDE;
        }
        return LogTool.NONE;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Alone: Core initializing — modelling the decisions real survival forces.");

        AloneBlocks.init();     // §5.2 — the bedroll block (a real bed), registered before its item
        AloneEntities.init();   // §6 — custom entities (the travois), registered before their items
        AloneItems.init();      // §2/§5/§6 — custom items (waterskin, bedroll, backpack)

        // Sync channel so the client HUD can see the player's survival state; drink request goes back.
        PayloadTypeRegistry.clientboundPlay().register(SurvivalSyncPayload.TYPE, SurvivalSyncPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
            dev.alone.core.net.ClimbSlipPayload.TYPE, dev.alone.core.net.ClimbSlipPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DrinkRequestPayload.TYPE, DrinkRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FireDrillPayload.TYPE, FireDrillPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
            dev.alone.core.net.KnapStrikePayload.TYPE, dev.alone.core.net.KnapStrikePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
            dev.alone.core.net.RiveStrokePayload.TYPE, dev.alone.core.net.RiveStrokePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
            dev.alone.core.net.BackpackOpenPayload.TYPE, dev.alone.core.net.BackpackOpenPayload.CODEC);
        // Quick-open keybind: toggle the backpack — open the first one in your pack, or close it if
        // that's what you're already looking at (§6).
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            dev.alone.core.net.BackpackOpenPayload.TYPE, (payload, context) -> {
                var player = context.player();
                if (player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu chest
                    && chest.getContainer() instanceof BackpackContainer) {
                    player.closeContainer(); // already in it — the key toggles it shut
                    return;
                }
                var backpack = BackpackItem.findInInventory(player);
                if (!backpack.isEmpty()) {
                    BackpackItem.open(player, backpack);
                }
            });

        // The core survival systems everything else hooks into (proposal §1).
        Conditions.init();      // §1.5 — conditions/injuries (persistent, debilitating)
        SurvivalMeters.init();  // §1 — stamina, thirst, temperature, realistic reach
        Drinking.init();        // §1.2 — drink from water sources
        FireStarting.init();    // §3.1 — friction fire (drill with a stick)
        Campfires.init();       // §3 — campfire fuel (feed sticks/logs; burns down)
        Sleeping.init();        // §1.4/§5.2 — rest on the ground (worst sleep; night + dry only)
        Eating.init();          // §1.1 — no eating while sprinting
        Hygiene.init();         // §5.6 — dirty hands from butchering contaminate food
        Torches.init();         // §5.6 — lit torches burn their fuel down to a spent torch
        Nutrition.init();       // §1.1 — diet variety; a monotonous diet shrinks usable hunger
        Leaves.init();          // §5.4 — leaves: hand → twigs + litter, axe/hoe → leaf block
        Climbing.init();        // §5.4 — climb up through leaves; free-climb short flat walls (strenuous)
        Forging.init();         // §8.2 — forge & temper: heat + hammer metal gear to quality at an anvil
        Sharpening.init();       // §8.5 — hone a worn edge back up with a whetstone (off-hand, sneak+use)
        Embers.init();          // §3.1 — scoop a glowing ember from a fire and carry it to light the next
        Ropes.init();           // §5.7 — breaking a rope rolls the whole connected line back into your pack
        Falls.init();           // §1.5 — water only breaks a fall if it's deep enough (else you hit bottom)
        Backpacks.init();       // §6 — breaking a set-down backpack returns the pack with its contents
        Pickup.init();          // §5.1 — no vacuum pickup; right-click a dropped item to pick it up
        Wildlife.init();        // §7.2 — wild animals are skittish; sneak to approach, else they bolt
        Hunting.init();         // §7.3 — a blade kill salvages the hide (leather) from a hide-bearing animal
        Fibers.init();          // §8.1 — strip plant fiber from grass/ferns; twist into string (no spiders)
        PlacedBlocks.init();    // §5.1 — remember player-placed blocks so they break loose, not rooted
        WorldGen.init();        // §8.1 — scatter loose rocks across the surface like grass, to pick up
        Foraging.init();        // §8.1 — dig gravel / rummage grass for flint, rocks, sticks (day-one)
        Knapping.init();        // §8.1 — knap flint with a rock into sharp shards (flint tools start here)
        Riving.init();          // §8.1 — rive a log into rough boards by hand (slow, exhausting; no saw)
        AdminCommand.init();    // debug — /alone reset refills meters, clears conditions, tops off vitals
        CraftingTime.init();    // §8.2 — timed, stationary crafting: a recipe must be worked before you take it

        // Proposal §8.1 — you fell a tree with the right tool, not your fist or a chicken.
        // An axe works; a crude blade barely works (the destroy-speed mixin makes it crawl);
        // anything else can't chop at all. This is the server-side authority; the mixin
        // handles the felt speed / lack of cracking.
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player.isCreative()) {
                return true;
            }
            var held = player.getMainHandItem();
            // You fell a tree with an axe (or barely, a crude blade) — never a bare fist.
            if (state.is(BlockTags.LOGS) && classifyForLog(held) == LogTool.NONE) {
                return false;
            }
            // And you can't claw stone apart bare-handed — quarrying needs a pickaxe (start with a
            // knapped flint one). Loose rock is foraged; worked stone is mined (§8.1).
            if (state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !isPickaxe(held)) {
                return false;
            }
            return true;
        });

    }

    /** A pickaxe of any tier — vanilla, flint, or steel — enough to break stone at all. */
    private static boolean isPickaxe(net.minecraft.world.item.ItemStack held) {
        return held.is(net.minecraft.tags.ItemTags.PICKAXES)
            || held.is(AloneItems.FLINT_PICK) || held.is(AloneItems.STEEL_PICKAXE);
    }
}
