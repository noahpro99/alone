package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;

/**
 * Spiders, made real (§7.2). Two tweaks so a spider reads like a spider, not a bear-sized cartoon:
 * <ul>
 *   <li><b>Smaller.</b> A vanilla spider is a metre-plus across — far bigger than any real one. We scale it
 *       down so it's a low, scuttling thing, not a pony with legs.</li>
 *   <li><b>No silk drop.</b> String comes from <b>plant fibre and sinew</b> in this pack, not from butchering
 *       bugs (see {@link Fibers}) — so spiders no longer drop it (handled in the spider loot table).</li>
 * </ul>
 */
public final class Spiders {
    private Spiders() {
    }

    /** Scale a common spider down to — a real one is far smaller than vanilla's. Cave spiders are already
     *  small, so they're left alone. */
    private static final double SPIDER_SCALE = 0.65;

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Spider && !(entity instanceof CaveSpider)) {
                AttributeInstance scale = ((Spider) entity).getAttribute(Attributes.SCALE);
                // Only set it once — re-applying every load is harmless but pointless; the guard also leaves
                // any deliberately-resized spider (a future boss?) untouched.
                if (scale != null && scale.getBaseValue() > SPIDER_SCALE + 0.01) {
                    scale.setBaseValue(SPIDER_SCALE);
                }
            }
        });
    }
}
