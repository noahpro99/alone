package dev.alone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The armed village guard (proposal §7.2 — a settlement you wrong doesn't just sulk; it musters men and
 * comes for you). Mechanically it is a {@link Vindicator} — a humanoid illager already built to hunt a
 * target with the classic goal-based AI (no brain rewrite), and it reuses the vindicator model, renderer and
 * texture, so it reads as an angry villager-in-arms with <b>no new art</b>. It carries a <b>bow</b> in the
 * main hand and a <b>sword</b> in the off hand: it looses arrows at range and cuts you up close.
 *
 * <p>The tactical brain is <em>not</em> reinvented here — it is the exact same {@link Golems.SeekCoverGoal
 * advance-under-cover} and {@link Golems.ChargeOrFleeGoal charge-or-flee} goals the iron golem uses, now
 * generic over any {@link PathfinderMob}. So a guard under fire breaks toward cover instead of eating arrows
 * in the open, and if you snipe it from a spot it can't path to, it flees rather than stand and be farmed —
 * the same anti-cheese logic, shared, not duplicated. What it does <b>not</b> get is the golem's
 * super-strength wall-smashing: a mortal guard can't pulverise a dirt wall.
 *
 * <p>It targets only a player the village has been turned hostile against (see {@link VillageDefense}), plus
 * anyone who hits it ({@link HurtByTargetGoal}). Guards are spawned in a wave when a crime flags the village
 * and are marked persistent so they see the reprisal through.
 *
 * <p><b>Simplifications in this first cut</b> (documented for the next pass): the bow lives in the main hand
 * so the vanilla {@link RangedBowAttackGoal} and its draw animation work unchanged; the sword is carried
 * off-hand for the look, and melee damage rides the {@code ATTACK_DAMAGE} attribute rather than a
 * hand-to-hand weapon swap by range. The illager model has no bow-pull pose, so the draw reads as a hold.
 */
public class VillageGuard extends Vindicator implements RangedAttackMob {
    /** Inside this range the guard draws its sword and fights hand-to-hand; beyond it, it uses the bow. */
    private static final double MELEE_RANGE_SQ = 9.0; // ~3 blocks

    public VillageGuard(EntityType<? extends Vindicator> type, Level level) {
        super(type, level);
        // Armed the moment it exists (egg-spawned or mustered): bow in hand, sword on the hip. Neither is
        // dropped on death — this is a defender's kit, not loot to farm.
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0f);
    }

    @Override
    protected void registerGoals() {
        // Deliberately NOT super.registerGoals(): the vindicator's raid/patrol kit would have it hunting
        // villagers and iron golems. A guard defends the village, so we build a clean goal set from scratch.
        this.goalSelector.addGoal(0, new FloatGoal(this)); // don't drown chasing you across water
        // The shared golem tactical brain — top priority, so a guard under fire reacts before anything else.
        this.goalSelector.addGoal(0, new Golems.ChargeOrFleeGoal(this)); // charge if it can reach you, else flee
        this.goalSelector.addGoal(1, new Golems.SeekCoverGoal(this));    // advance under cover through arrows
        this.goalSelector.addGoal(2, new GuardMeleeGoal(this, 1.15, true)); // sword up close
        this.goalSelector.addGoal(3, new RangedBowAttackGoal<>(this, 0.9, 20, 15.0f)); // bow at range
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6)); // idle patrol
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this)); // retaliate against whoever strikes it
        this.targetSelector.addGoal(2, new VillageDefense.FlaggedPlayerTargetGoal(this)); // hunt the flagged wrongdoer
    }

    /** Fires an arrow at the target — a trimmed copy of the skeleton's bow logic (a vindicator has none of
     *  its own). No ammo is required: a mustered guard is issued arrows. */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        ItemStack bow = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), velocity, bow);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        // Lob it slightly to cover the drop; inaccuracy eases as the world gets harder (like a skeleton).
        arrow.shoot(dx, dy + horiz * 0.2, dz, 1.6f, 14 - server.getDifficulty().getId() * 4);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
        server.addFreshEntity(arrow);
    }

    /** Melee only when the target has closed inside sword reach — otherwise the guard stays at range and
     *  shoots (the bow goal, one priority lower, then wins). Everything else is vanilla melee behaviour. */
    private static class GuardMeleeGoal extends MeleeAttackGoal {
        GuardMeleeGoal(PathfinderMob mob, double speed, boolean followWhenOutOfSight) {
            super(mob, speed, followWhenOutOfSight);
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && this.mob.distanceToSqr(target) <= MELEE_RANGE_SQ && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.mob.getTarget();
            return target != null && this.mob.distanceToSqr(target) <= MELEE_RANGE_SQ * 2.25 && super.canContinueToUse();
        }
    }
}
