package dev.alone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.Level;

/**
 * The wild boar (roadmap: "a wilderness that's actually wild") — the wild ancestor of the barnyard pig, and
 * the reason a survivor treats the woods with respect. Mechanically it <b>is</b> a pig (same forage-and-breed
 * brain, same carcass) — but a real boar is <b>not</b> the placid livestock its descendant is. It roots
 * about calmly until it feels threatened, then it <b>charges and gores</b>: a neutral-aggressive animal that
 * fights back rather than fleeing, exactly like the {@link BrownBear brown bear} or a wolf that's been hit.
 *
 * <p>So where the domestic pig <em>panics and runs</em> when struck, the boar has that flight reflex
 * stripped and a melee/retaliation kit bolted on: hit it (or corner it) and it wheels around and comes at
 * you with real tusks. It keeps its own {@link AloneEntities#WILD_BOAR type}, so the sibling "domestic
 * livestock only near villages" veto (which matches the vanilla {@code EntityType.PIG} exactly) never touches
 * it — the boar stays out in the forest, taiga and swamp where it belongs.</p>
 */
public class WildBoar extends Pig {
    public WildBoar(EntityType<? extends Pig> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals(); // the pig's kit: float, breed, tempt, follow-parent, stroll, look
        // A boar doesn't flee a blow — it turns and fights. Drop the pig's panic-and-run reflex so the
        // retaliation kit below actually gets to run when the boar is struck.
        this.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal);
        // Threatened or hit → charge and gore. MeleeAttackGoal drives the charge; HurtByTargetGoal makes
        // "you hit me" the trigger, so it's neutral-aggressive (retaliates) rather than a passive prey animal.
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public Pig getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return AloneEntities.WILD_BOAR.create(level, EntitySpawnReason.BREEDING); // boars breed boars, not pigs
    }
}
