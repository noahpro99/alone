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
        AloneItems.init();      // §2/§5/§6 — custom items (waterskin, bedroll, backpack)

        // Sync channel so the client HUD can see the player's survival state; drink request goes back.
        PayloadTypeRegistry.clientboundPlay().register(SurvivalSyncPayload.TYPE, SurvivalSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DrinkRequestPayload.TYPE, DrinkRequestPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FireDrillPayload.TYPE, FireDrillPayload.CODEC);

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
        Fibers.init();          // §8.1 — strip plant fiber from grass/ferns; twist into string (no spiders)
        CraftingTime.init();    // §8.2 — timed, stationary crafting: a recipe must be worked before you take it

        // Proposal §8.1 — you fell a tree with the right tool, not your fist or a chicken.
        // An axe works; a crude blade barely works (the destroy-speed mixin makes it crawl);
        // anything else can't chop at all. This is the server-side authority; the mixin
        // handles the felt speed / lack of cracking.
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (player.isCreative()) {
                return true;
            }
            return !(state.is(BlockTags.LOGS)
                && classifyForLog(player.getMainHandItem()) == LogTool.NONE);
        });

    }
}
