package dev.alone.core;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The brown bear (proposal §7.2) — the forest's big predator, and the one that actually belongs here. The
 * <em>Alone</em> wilderness is temperate and boreal forest, where the bear you fear is a brown/black bear,
 * not the polar bear that only makes sense on the ice. Mechanically it <b>is</b> a polar bear — same build,
 * same bulk, same hide-bearing carcass — just brown, spawned in the woods, and <b>more willing to come at
 * you</b>: where a polar bear mostly minds its own business until you crowd its cubs, the brown bear will
 * hunt a person it notices. Reusing the polar bear keeps the behaviour real and battle-tested; only the
 * coat, the habitat, and the temper change (see {@code BrownBearRenderer} and its forest spawns). It's
 * wired into the scent/predator systems through {@link Scent#PREDATORS}, so carried meat draws it too.
 */
public class BrownBear extends PolarBear {
    public BrownBear(EntityType<? extends PolarBear> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals(); // the polar bear's kit: melee, cub defence, panic, wander, hurt-by-target
        // A brown bear is bolder than its polar cousin — it actively hunts a person it notices, not only
        // when provoked or guarding cubs.
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}
