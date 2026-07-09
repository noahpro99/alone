package dev.alone.food;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alone: Table — food realism (proposal §4).
 *
 * <p>First feature: raw-food sickness risk (§4.2). Which foods are risky is declared in a
 * bundled datapack tag ({@code data/alone/tags/item/dangerous_raw.json}) so the list is
 * tunable/hot-reloadable without touching code — the datapack half of the pack.
 */
public class AloneFood implements ModInitializer {
    public static final String MOD_ID = "alone-food";
    public static final Logger LOGGER = LoggerFactory.getLogger("Alone: Table");

    // Raw foods by how dangerous they are undercooked (§4.2), each a bundled, tunable datapack tag.
    public static final TagKey<Item> RAW_HIGH_RISK = tag("raw_high_risk");     // chicken, rotten flesh
    public static final TagKey<Item> RAW_MEDIUM_RISK = tag("raw_medium_risk"); // beef, pork, mutton, rabbit
    public static final TagKey<Item> RAW_LOW_RISK = tag("raw_low_risk");       // fresh fish

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("alone", path));
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Alone: Table initializing — raw food is a gamble.");
        Spoilage.init();   // §4.2 — perishable food rots over time
        Preserving.init(); // §4.2 — salt (from boiled seawater) preserves food for winter
    }
}
