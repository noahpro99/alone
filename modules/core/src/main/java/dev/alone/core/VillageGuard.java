package dev.alone.core;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * The armed village guard (proposal §7.2 — a settlement isn't a free pantry; it keeps armed men who live in
 * it, patrol it, and turn on anyone who wrongs it). This is the <b>redesigned</b> guard:
 *
 * <ul>
 *   <li><b>An armoured human, not an illager.</b> The class extends {@link PathfinderMob} directly — <em>not</em>
 *       a {@link net.minecraft.world.entity.monster.Monster} and <em>not</em> a raider — so it does <b>not</b>
 *       implement {@link net.minecraft.world.entity.monster.Enemy}. That's the whole trick: an
 *       {@link net.minecraft.world.entity.animal.golem.IronGolem iron golem} only ever targets mobs that are
 *       {@code Enemy} (its {@code NearestAttackableTargetGoal} filters on {@code instanceof Enemy}), so a guard
 *       that isn't an {@code Enemy} is invisible to the golems — they patrol side by side instead of the golem
 *       cutting the guards down. It's rendered as an armoured biped (see {@code VillageGuardRenderer}, reusing
 *       the vanilla player model + a plain human skin), so it reads as a villager-in-arms with no new art, but
 *       the <em>entity type</em> is a neutral pathfinder, which is what the golems key off.</li>
 *   <li><b>Bow and sword.</b> A bow in the main hand and an iron sword in the off hand: it looses arrows at
 *       range ({@link GuardBowAttackGoal}) and cuts you up close ({@link GuardMeleeGoal}). It wears a full set
 *       of chainmail.</li>
 *   <li><b>Moves at your pace.</b> Its {@code MOVEMENT_SPEED} base is {@code 0.09} — the player's own walk
 *       ({@link SurvivalMeters} {@code WALK_SPEED}). An armoured man on foot doesn't outrun you; stand and
 *       fight and he keeps pace, sprint away and you can break contact. (Deliberate — a tuning knob.)</li>
 *   <li><b>Drops its whole kit.</b> Unlike the old cut, the gear is <b>not</b> kept back: a guard has a loot
 *       table ({@code data/alone/loot_table/entities/village_guard.json}) that drops its chainmail, bow,
 *       arrows, sword, and the rations it was carrying. Equipment drop-chances are zeroed so the loot table is
 *       the single, deterministic source (no random double-drop). Killing a guard is a real, dangerous fight
 *       — not a safe farm — but the reward is honest (realistic-drops principle).</li>
 * </ul>
 *
 * <p><b>Behaviour.</b> Peaceful by default: it patrols and wanders its village (kept near its home post by a
 * {@link MoveTowardsRestrictionGoal}, set when {@link VillageDefense} spawns it). It only turns hostile on a
 * player the village has been {@link VillageDefense#isHostile flagged against} (a decaying, per-player aggro
 * timer set by a village crime) — {@link VillageDefense.FlaggedPlayerTargetGoal} — or on anyone who strikes it
 * ({@link HurtByTargetGoal}). So the guards and the iron golems both come for the wrongdoer, together, while
 * leaving each other alone.
 *
 * <p>The tactical brain is shared, not reinvented: the same {@link Golems.SeekCoverGoal advance-under-cover}
 * and {@link Golems.ChargeOrFleeGoal charge-or-flee} goals the iron golem uses, generic over any
 * {@link PathfinderMob}. A guard under fire breaks toward cover instead of eating arrows in the open, and if
 * you snipe it from a spot it can't path to, it flees rather than stand and be farmed. What it does <b>not</b>
 * get is the golem's super-strength wall-smashing: a mortal guard can't pulverise a dirt wall.
 */
public class VillageGuard extends PathfinderMob implements RangedAttackMob {
    /** Inside this range the guard fights hand-to-hand with the sword; beyond it, it uses the bow. */
    private static final double MELEE_RANGE_SQ = 9.0; // ~3 blocks

    public VillageGuard(EntityType<? extends VillageGuard> type, Level level) {
        super(type, level);
        // Kitted the moment it exists (spawned into a village or from the egg): full chainmail, bow in hand,
        // sword on the hip. Drop-chances are zeroed — the loot table is the one source of its dropped kit, so
        // the gear drops cleanly and in full rather than at vanilla's random equipment-drop odds.
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.CHAINMAIL_BOOTS));
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0f);
        }
    }

    /** Attributes for a mortal man-at-arms: a modest health pool, a real sword arm, a wide follow range so it
     *  keeps after a flagged wrongdoer, and — the point — a walk speed matched to the player's own. */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 24.0)
            .add(Attributes.MOVEMENT_SPEED, 0.09) // == SurvivalMeters.WALK_SPEED — the player's walk
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this)); // don't drown chasing you across water
        // The shared golem tactical brain — top priority, so a guard under fire reacts before anything else.
        this.goalSelector.addGoal(0, new Golems.ChargeOrFleeGoal(this)); // charge if it can reach you, else flee
        this.goalSelector.addGoal(1, new Golems.SeekCoverGoal(this));    // advance under cover through arrows
        this.goalSelector.addGoal(2, new GuardMeleeGoal(this, 1.0, true)); // sword up close, at the player's pace
        this.goalSelector.addGoal(3, new GuardBowAttackGoal(this, 1.0, 20, 15.0f)); // bow at range
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.7)); // don't stray far from its post
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6)); // peaceful patrol/wander
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this)); // retaliate against whoever strikes it
        this.targetSelector.addGoal(2, new VillageDefense.FlaggedPlayerTargetGoal(this)); // hunt the flagged wrongdoer
    }

    /** Fires an arrow at the target — a trimmed copy of the skeleton's bow logic. No ammo is consumed: a
     *  posted guard is issued arrows. */
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

    /**
     * The bow goal. Vanilla's {@link net.minecraft.world.entity.ai.goal.RangedBowAttackGoal} is generic over
     * {@code T extends Monster & RangedAttackMob} — and a village guard is deliberately <b>not</b> a
     * {@code Monster} (so the golems ignore it), so that goal won't type-check for us. This is a faithful copy
     * of its strafe-and-loose logic, retyped to the guard (dropping the vanilla vehicle branch a guard never
     * uses).
     */
    private static class GuardBowAttackGoal extends Goal {
        private final VillageGuard mob;
        private final double speedModifier;
        private final int attackIntervalMin;
        private final float attackRadiusSqr;
        private int attackTime = -1;
        private int seeTime;
        private boolean strafingClockwise;
        private boolean strafingBackwards;
        private int strafingTime = -1;

        GuardBowAttackGoal(VillageGuard mob, double speedModifier, int attackIntervalMin, float attackRadius) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.attackIntervalMin = attackIntervalMin;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean isHoldingBow() {
            return this.mob.isHolding(Items.BOW);
        }

        @Override
        public boolean canUse() {
            return this.mob.getTarget() != null && this.isHoldingBow();
        }

        @Override
        public boolean canContinueToUse() {
            return (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingBow();
        }

        @Override
        public void start() {
            super.start();
            this.mob.setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.mob.setAggressive(false);
            this.seeTime = 0;
            this.attackTime = -1;
            this.mob.stopUsingItem();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.mob.getTarget();
            if (target == null) {
                return;
            }
            double targetDistSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
            boolean hadLineOfSight = this.seeTime > 0;
            if (hasLineOfSight != hadLineOfSight) {
                this.seeTime = 0;
            }
            if (hasLineOfSight) {
                this.seeTime++;
            } else {
                this.seeTime--;
            }

            if (!(targetDistSqr > this.attackRadiusSqr) && this.seeTime >= 20) {
                this.mob.getNavigation().stop();
                this.strafingTime++;
            } else {
                this.mob.getNavigation().moveTo(target, this.speedModifier);
                this.strafingTime = -1;
            }

            if (this.strafingTime >= 20) {
                if (this.mob.getRandom().nextFloat() < 0.3) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                if (this.mob.getRandom().nextFloat() < 0.3) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }

            if (this.strafingTime > -1) {
                if (targetDistSqr > this.attackRadiusSqr * 0.75F) {
                    this.strafingBackwards = false;
                } else if (targetDistSqr < this.attackRadiusSqr * 0.25F) {
                    this.strafingBackwards = true;
                }
                this.mob.getMoveControl().strafe(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
                this.mob.lookAt(target, 30.0F, 30.0F);
            } else {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            if (this.mob.isUsingItem()) {
                if (!hasLineOfSight && this.seeTime < -60) {
                    this.mob.stopUsingItem();
                } else if (hasLineOfSight) {
                    int pullTime = this.mob.getTicksUsingItem();
                    if (pullTime >= 20) {
                        this.mob.stopUsingItem();
                        this.mob.performRangedAttack(target, BowItem.getPowerForTime(pullTime));
                        this.attackTime = this.attackIntervalMin;
                    }
                }
            } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
                this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.BOW));
            }
        }
    }
}
