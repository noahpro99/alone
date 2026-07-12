package dev.alone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;

/**
 * The bison / aurochs (roadmap: "a wilderness that's actually wild") — the wild bovine, the "wild cow" that
 * the domestic cow was bred down from. It's the realism payoff for leather and beef: a big grazing animal
 * you can't just walk up and slaughter. Mechanically it <b>is</b> a cow (grazes, herds, breeds), scaled up
 * to a ton of muscle in the {@link dev.alone.core.client.BisonRenderer renderer}, and turned into a
 * <b>defensive charger</b>: it feeds calmly in the herd until something provokes it, then it wheels and
 * <b>charges hard</b>, hitting for real damage with heavy knockback.
 *
 * <p>So the domestic cow's panic-and-run reflex is stripped and a melee/retaliation kit added: hit one of
 * the herd and it comes at you rather than trotting off. That's the wager the wild bovine forces — several
 * hides and a lot of beef are out there on the plains, but you have to <em>risk</em> a charging animal to
 * take them. It keeps its own {@link AloneEntities#BISON type}, so the sibling "domestic livestock only near
 * villages" veto (which matches the vanilla {@code EntityType.COW} exactly) leaves it alone in the wild.</p>
 */
public class Bison extends Cow {
    public Bison(EntityType<? extends Cow> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals(); // the cow's kit: float, breed, tempt, follow-parent, stroll, look
        // A bison is not spooked by a blow the way a milk cow is — it stands its ground and charges. Drop
        // the cow's panic-and-run reflex so the retaliation kit below can drive it instead.
        this.goalSelector.removeAllGoals(goal -> goal instanceof PanicGoal);
        // Provoked or hit → charge. MeleeAttackGoal runs the charge (fast, since a bison closes ground in a
        // hurry); HurtByTargetGoal makes being struck the trigger, so the herd is safe to watch but deadly to
        // pick a fight with. Big-animal knockback/damage comes from the attributes in AloneEntities.
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.4, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers()); // one hit and the herd turns
    }

    @Override
    public Cow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return AloneEntities.BISON.create(level, EntitySpawnReason.BREEDING); // bison breed bison, not cows
    }
}
