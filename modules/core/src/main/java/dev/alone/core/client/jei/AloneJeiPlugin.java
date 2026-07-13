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
        info(registration, AloneItems.WATERSKIN, "waterskin");
        info(registration, AloneItems.FLINT_KNIFE, "flint_knife");
        info(registration, AloneItems.TALLOW, "tallow");
        info(registration, AloneItems.PYRITE, "pyrite");
        info(registration, AloneItems.FLINT_AND_PYRITE, "flint_and_pyrite");
        info(registration, AloneItems.SLINGSHOT, "slingshot");
        info(registration, AloneItems.BUG_NET, "bug_net");
        info(registration, AloneItems.WICKER_SHIELD, "wicker_shield");
    }

    /** Attach the {@code jei.alone.<key>.desc} blurb to {@code item}'s JEI Information tab. */
    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable("jei.alone." + key + ".desc"));
    }
}
