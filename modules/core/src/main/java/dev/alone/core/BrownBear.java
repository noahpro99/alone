package dev.alone.core;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.level.Level;

/**
 * The brown bear (proposal §7.2) — the forest's big predator, and the one that actually belongs here. The
 * <em>Alone</em> wilderness is temperate and boreal forest, where the bear you fear is a brown/black bear,
 * not the polar bear that only makes sense on the ice. Mechanically it <b>is</b> a polar bear — same build,
 * same "leave me and my cubs alone or I'll maul you" temperament, same hide-bearing carcass — just brown,
 * and spawned in the woods instead of the snow. Reusing the polar bear wholesale keeps the behaviour real
 * and battle-tested; only the coat and the habitat change (see {@code BrownBearRenderer} and its forest
 * spawns). It's wired into the scent/predator systems through {@link Scent#PREDATORS}, so carried meat
 * draws it exactly as it draws a wolf.
 */
public class BrownBear extends PolarBear {
    public BrownBear(EntityType<? extends PolarBear> type, Level level) {
        super(type, level);
    }
}
