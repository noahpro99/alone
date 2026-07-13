package dev.alone.core.client.jei;

import dev.alone.core.AloneItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;

/**
 * JEI plugin that attaches a wiki-like "Information" tab to each Alone item, so pressing the info
 * key in JEI shows a short blurb explaining what the item is and how it's used.
 *
 * <p>Discovery on Fabric is via the {@code jei_mod_plugin} entrypoint in {@code fabric.mod.json}
 * (NOT the {@link JeiPlugin @JeiPlugin} annotation, which is the Forge/NeoForge path) — verified
 * against JEI's own {@code fabric.mod.json} in jei-26.2-fabric-30.10.0.60. The annotation is kept
 * for documentation and cross-loader parity. JEI is bundled at runtime by the mrpack; the API is a
 * compile-only dependency, so this class only ever loads on a client that has JEI installed.
 *
 * <p>Blurbs are sourced from {@code jei.alone.<item>.desc} lang keys so they stay translatable.
 * This is a representative starter set (§ items from across the tech tree); expand it by adding an
 * {@link #info} line plus the matching lang key.
 */
@JeiPlugin
public class AloneJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("alone", "jei_info");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Water & vessels
        info(registration, AloneItems.WATERSKIN, "waterskin");
        info(registration, AloneItems.CLAY_POT, "clay_pot");
        info(registration, AloneItems.IRON_POT, "iron_pot");
        info(registration, AloneItems.HOT_ROCK, "hot_rock");

        // Fire
        info(registration, AloneItems.BOW_DRILL, "bow_drill");
        info(registration, AloneItems.FERRO_ROD, "ferro_rod");
        info(registration, AloneItems.FLINT_AND_PYRITE, "flint_and_pyrite");
        info(registration, AloneItems.EMBER, "ember");
        info(registration, AloneItems.TORCH, "torch");

        // Stone & woodworking
        info(registration, AloneItems.ROCK, "rock");
        info(registration, AloneItems.FLINT_KNIFE, "flint_knife");
        info(registration, AloneItems.FLINT_HATCHET, "flint_hatchet");
        info(registration, AloneItems.HAND_SAW, "hand_saw");
        info(registration, AloneItems.WHETSTONE, "whetstone");

        // Metalworking (no GUI — custom block/tool interactions)
        info(registration, AloneItems.KILN, "kiln");
        info(registration, AloneItems.BLOOMERY, "bloomery");
        info(registration, AloneItems.SMITHING_HAMMER, "smithing_hammer");

        // Medicine & hygiene
        info(registration, AloneItems.SPLINT, "splint");
        info(registration, AloneItems.HERBAL_REMEDY, "herbal_remedy");
        info(registration, AloneItems.SEWING_KIT, "sewing_kit");
        info(registration, AloneItems.TOWEL, "towel");

        // Carry & travel
        info(registration, AloneItems.BACKPACK, "backpack");
        info(registration, AloneItems.WOVEN_BASKET, "woven_basket");
        info(registration, AloneItems.TRAVOIS, "travois");
        info(registration, AloneItems.ROPE, "rope");

        // Combat & hunting
        info(registration, AloneItems.SLINGSHOT, "slingshot");
        info(registration, AloneItems.WICKER_SHIELD, "wicker_shield");
        info(registration, AloneItems.WOODEN_SHIELD, "wooden_shield");
        info(registration, AloneItems.BUG_NET, "bug_net");

        // Traps & fishing
        info(registration, AloneItems.SNARE, "snare");
        info(registration, AloneItems.DEADFALL, "deadfall");
        info(registration, AloneItems.FISH_TRAP, "fish_trap");
        info(registration, AloneItems.GILL_NET, "gill_net");
        info(registration, AloneItems.WORMS, "worms");

        // Hide-working & food preservation
        info(registration, AloneItems.DRYING_RACK, "drying_rack");
        info(registration, AloneItems.RAW_HIDE, "raw_hide");
        info(registration, AloneItems.ANIMAL_BRAINS, "animal_brains");
        info(registration, AloneItems.BONE_SCRAPER, "bone_scraper");

        // Shelter & sleep
        info(registration, AloneItems.THATCH, "thatch");
        info(registration, AloneItems.TARP, "tarp");
        info(registration, AloneItems.WATTLE, "wattle");
        info(registration, AloneItems.BEDROLL, "bedroll");
        info(registration, AloneItems.SLEEPING_BAG, "sleeping_bag");
    }

    /** Attach the {@code jei.alone.<key>.desc} blurb to {@code item}'s JEI Information tab. */
    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable("jei.alone." + key + ".desc"));
    }
}
