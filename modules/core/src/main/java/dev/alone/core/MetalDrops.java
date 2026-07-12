package dev.alone.core;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Metal comes from the forge, not the corpse (progression — keep the age-gates real; roadmap: "the good
 * gear is in the chests, not the corpses"). The flint → copper → iron ladder is only honest if you can't
 * short-circuit it: iron ore is gated behind a copper pick and iron only smelts in a bloomery — but a
 * village <b>iron golem</b> drops iron ingots outright, and a few undead drop them rarely, which would let
 * you skip the whole bloomery chain by killing rather than smelting.
 *
 * <p>So we strip <b>iron ingots and nuggets from mob drops</b> — anything with a dying entity in its loot
 * context (a corpse), which leaves <b>chest and structure loot untouched</b> (those have no {@code
 * THIS_ENTITY}), since found iron is the deliberate "old-world leavings" exception. Butchered game (leather,
 * bone) is unaffected — it drops no metal to begin with.
 */
public final class MetalDrops {
    private MetalDrops() {
    }

    public static void init() {
        LootTableEvents.MODIFY_DROPS.register((key, context, drops) -> {
            if (!context.hasParameter(LootContextParams.THIS_ENTITY)) {
                return; // not a corpse — chest/structure loot keeps its found iron
            }
            drops.removeIf(stack -> stack.is(Items.IRON_INGOT) || stack.is(Items.IRON_NUGGET));
        });
    }
}
