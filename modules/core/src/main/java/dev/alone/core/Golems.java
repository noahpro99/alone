package dev.alone.core;

import java.util.EnumSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Iron golems fight like the multi-ton iron constructs they are, so they can't be cheesed (progression /
 * realism — a golem should still <b>drop its iron</b> when killed, but taking it must be a real, dangerous
 * fight, not a safe farm; see the realistic-drops principle). The classic exploit is to hurt a golem from
 * behind a one-block wall or through a small hole its pathfinder can't cross. Here a golem <b>smashes
 * through weak cover</b> to reach a target it can't otherwise get at: the dirt, wood, or stone in the way
 * is pulverised, opening a path to you — you can't poke a giant iron construct through a gap it can't reach
 * through. Metal, obsidian, and deepslate are too tough for even a golem to shrug aside, so a real fortified
 * bunker still holds; a thrown-up dirt wall does not.
 *
 * <p>It also closes the other cheeses: it can <b>reach up</b> to strike a target perched a few blocks above
 * it, and if you <b>tower up higher</b> to snipe with a bow it <b>digs the pillar out from under you</b> —
 * loose soil then collapses and drops you back into reach. Only touches blocks right next to it, so it opens
 * a path (or knocks a tower down) rather than levelling the countryside.
 */
public final class Golems {
    private Golems() {
    }

    private static final float MAX_HARDNESS = 3.0f;   // earth/wood/stone tier — not metal, obsidian, deepslate
    private static final double SMASH_RANGE_SQ = 9.0;  // only cover within ~3 blocks of the golem
    private static final int SMASH_INTERVAL = 15;      // pulverise ~one block every 0.75s (tune in playtest)
    private static final double UNDERMINE_MIN_DY = 2.0; // a target perched this far above (a tower) gets its pillar dug out

    // Reaching a target perched above: vanilla melee already covers ~2 blocks up (the golem is wide, so its
    // reach is generous), so we only need to cover the taller perch a survivor climbs to feel safe.
    private static final double REACH_UP_MIN = 2.5;    // above the ~2 blocks vanilla melee already reaches
    private static final double REACH_UP_MAX = 4.0;    // a 3–4 block pillar no longer keeps you out of reach
    private static final int REACH_UP_INTERVAL = 20;   // an upward swing about once a second

    private static final double COVER_TRIGGER_SQ = 36.0; // shot from >6 blocks away counts as "at range"
    private static final int RECENT_HIT_TICKS = 60;      // reacts within ~3s of being shot
    private static final int COVER_SEARCH = 5;           // how far to look for a spot that breaks line of sight

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof IronGolem golem) {
                golem.getGoalSelector().addGoal(1, new SeekCoverGoal(golem)); // break for cover under ranged fire first
                golem.getGoalSelector().addGoal(2, new SmashThroughGoal(golem));
                golem.getGoalSelector().addGoal(2, new ReachUpGoal(golem));
            }
        });
    }

    /** Breaks weak blocks between the golem and a target it can't reach — the anti-cheese. Non-exclusive
     *  (no flags), so it runs alongside the golem's normal move/attack goals rather than replacing them. */
    private static class SmashThroughGoal extends Goal {
        private final IronGolem golem;
        private int cooldown;

        SmashThroughGoal(IronGolem golem) {
            this.golem = golem;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = golem.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double dx = target.getX() - golem.getX();
            double dz = target.getZ() - golem.getZ();
            if (dx * dx + dz * dz > 64.0) {
                return false; // too far off horizontally to be worth digging at
            }
            // A wall between us (can't see it), OR it's perched up a tower out of reach (see it, can't get it).
            return !golem.getSensing().hasLineOfSight(target)
                || target.getY() - golem.getY() > UNDERMINE_MIN_DY;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            LivingEntity target = golem.getTarget();
            if (target == null) {
                return;
            }
            Level level = golem.level();
            // A wall between us: smash straight through it toward the target.
            if (!golem.getSensing().hasLineOfSight(target)) {
                BlockHitResult hit = level.clip(new ClipContext(golem.getEyePosition(), target.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, golem));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    smash(level, hit.getBlockPos());
                }
                return;
            }
            // Otherwise it's perched above (towered up to snipe with a bow) — pound the pillar out from under
            // it. Loose soil then collapses (see DirtFalling), dropping it to you; a flung-up dirt tower is
            // no refuge from a construct that can knock it down.
            if (target.getY() - golem.getY() > UNDERMINE_MIN_DY) {
                undermine(level, target);
            }
        }

        /** Break a single block near the golem if it's within reach and not too tough. */
        private void smash(Level level, BlockPos pos) {
            if (golem.distanceToSqr(Vec3.atCenterOf(pos)) > SMASH_RANGE_SQ) {
                return; // too far to be the cheese wall — leave the world alone
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0f || hardness > MAX_HARDNESS) {
                return; // air, unbreakable (bedrock), or too tough for even a golem
            }
            golem.swing(InteractionHand.MAIN_HAND);
            level.destroyBlock(pos, false, golem, 512); // pulverised — plays the break effect itself
            cooldown = SMASH_INTERVAL;
        }

        /** Dig out the pillar under a perched target: break a reachable block of its supporting column near
         *  the golem's own height. With the base gone, loose soil above it falls and the tower comes down. */
        private void undermine(Level level, LivingEntity target) {
            int cx = (int) Math.floor(target.getX());
            int cz = (int) Math.floor(target.getZ());
            int gy = golem.blockPosition().getY();
            for (int y = gy + 2; y >= gy - 1; y--) {
                smash(level, new BlockPos(cx, y, cz));
                if (cooldown > 0) {
                    return; // smashed one this pass
                }
            }
        }
    }

    /** Lets the golem strike a target perched on a tall pillar or ledge above it — the other classic cheese
     *  (stand just out of its reach and hit down). Only for perches beyond vanilla's already-generous melee
     *  reach; runs its normal attack (damage, knockback, arm-raise) via {@code doHurtTarget}. Non-exclusive. */
    private static class ReachUpGoal extends Goal {
        private final IronGolem golem;
        private int cooldown;

        ReachUpGoal(IronGolem golem) {
            this.golem = golem;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = golem.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            double dy = target.getY() - golem.getY();
            if (dy < REACH_UP_MIN || dy > REACH_UP_MAX) {
                return false; // at or below vanilla reach, or too high to plausibly reach
            }
            double dx = target.getX() - golem.getX();
            double dz = target.getZ() - golem.getZ();
            double horizReach = golem.getBbWidth() + 1.2; // roughly under it, allowing for a ledge overhang
            return dx * dx + dz * dz <= horizReach * horizReach
                && golem.getSensing().hasLineOfSight(target); // don't punch through a solid floor
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = golem.getTarget();
            if (target == null) {
                return;
            }
            golem.getLookControl().setLookAt(target);
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (golem.level() instanceof ServerLevel level) {
                golem.doHurtTarget(level, target); // its real attack: damage, knockback, and the arm-raise
            }
            cooldown = REACH_UP_INTERVAL;
        }
    }

    /**
     * Shot from range and it survived? A defender doesn't stand in the open taking arrows — it breaks for
     * cover. This pulls the golem to the nearest standing spot that puts something solid between it and the
     * archer, favouring cover that's <b>toward</b> the shooter so it <b>advances under cover</b> rather than
     * cowering. It only triggers on ranged fire from a distance (up close it still fights head-on), and in
     * open ground with no cover to reach it simply charges — so it stays the relentless closer, just not a
     * stationary target. Highest priority, so a golem under fire ducks before it does anything else.
     */
    private static class SeekCoverGoal extends Goal {
        private final IronGolem golem;
        private Player shooter;
        private Vec3 cover;
        private int nextSearch;

        SeekCoverGoal(IronGolem golem) {
            this.golem = golem;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(golem.getLastHurtByMob() instanceof Player p) || !p.isAlive() || p.isCreative()) {
                return false;
            }
            if (golem.tickCount - golem.getLastHurtByMobTimestamp() > RECENT_HIT_TICKS) {
                return false; // not shot recently
            }
            if (golem.distanceToSqr(p) < COVER_TRIGGER_SQ) {
                return false; // it's near — close in and fight, don't hide
            }
            if (!golem.getSensing().hasLineOfSight(p)) {
                return false; // already out of its line — no need to move
            }
            if (golem.tickCount < nextSearch) {
                return false; // throttle the (costly) cover scan
            }
            nextSearch = golem.tickCount + 10;
            BlockPos spot = findCover(p);
            if (spot == null) {
                return false; // no cover within reach — it'll just advance in the open
            }
            this.shooter = p;
            this.cover = Vec3.atBottomCenterOf(spot);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.shooter != null && this.shooter.isAlive()
                && !this.golem.getNavigation().isDone()
                && this.golem.getSensing().hasLineOfSight(this.shooter); // stop once it's safely behind cover
        }

        @Override
        public void start() {
            this.golem.getNavigation().moveTo(this.cover.x, this.cover.y, this.cover.z, 1.3);
        }

        @Override
        public void stop() {
            this.shooter = null;
            this.cover = null;
        }

        /** Nearest reachable standing spot (favouring ones toward the shooter, to advance) whose line to the
         *  archer is blocked by a solid block. Null if nothing within range gives cover (open ground). */
        private BlockPos findCover(Player p) {
            Level level = this.golem.level();
            Vec3 shooterEye = p.getEyePosition();
            BlockPos origin = this.golem.blockPosition();
            BlockPos best = null;
            double bestToShooter = Double.MAX_VALUE;
            for (int dx = -COVER_SEARCH; dx <= COVER_SEARCH; dx++) {
                for (int dz = -COVER_SEARCH; dz <= COVER_SEARCH; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos spot = origin.offset(dx, dy, dz);
                        if (!standable(level, spot)) {
                            continue;
                        }
                        Vec3 spotEye = new Vec3(spot.getX() + 0.5, spot.getY() + 1.6, spot.getZ() + 0.5);
                        BlockHitResult hit = level.clip(new ClipContext(spotEye, shooterEye,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.golem));
                        if (hit.getType() != HitResult.Type.BLOCK) {
                            continue; // still exposed to the shooter from here
                        }
                        double toShooter = spot.distSqr(p.blockPosition());
                        if (toShooter < bestToShooter) {
                            bestToShooter = toShooter;
                            best = spot;
                        }
                    }
                }
            }
            return best;
        }

        private static boolean standable(Level level, BlockPos pos) {
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        }
    }
}
