package dev.alone.core;

import java.util.EnumSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
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
 *
 * <p><b>Provoked escalation:</b> the first blow makes it break for cover ({@link SeekCoverGoal}); keep
 * hitting a golem that's already backing off and it stops absorbing punishment. If it <b>can path to
 * you</b> it turns and <b>charges</b> home (its normal melee takes the wheel); if it <b>can't</b> reach you
 * at all — you're across a gap, behind a wall it can't route around — it <b>turns tail and flees</b> away
 * from you until the hits stop. A cornered construct that can't fight back doesn't stand there being farmed.
 *
 * <p><b>The two range-fight goals — {@link SeekCoverGoal} (advance under cover) and {@link ChargeOrFleeGoal}
 * (charge if reachable, else flee) — are generic over any {@link PathfinderMob}</b> so the {@link VillageGuard
 * village guards} reuse the exact same tactical brain (proposal §7.2 / village defence). Only the
 * golem-specific super-strength goals ({@link SmashThroughGoal}/{@link ReachUpGoal}, which pulverise blocks)
 * stay bound to the golem — a mortal guard doesn't punch through walls. The golems also hunt any player the
 * village has been turned hostile against (see {@link VillageDefense}), so wronging a settlement sets its
 * iron sentinels on you as well as its armed guards.
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

    // Provoked escalation: 1st hit → retreat (SeekCoverGoal); a further hit while it's still smarting →
    // charge if it can reach you, else flee. Tracked as a hit counter that decays when the blows stop.
    private static final int PROVOKE_THRESHOLD = 2;      // hits within the window before it stops absorbing and reacts
    private static final int PROVOKE_RESET_TICKS = 100;  // ~5s with no fresh blow and it stands down (matches vanilla hurt-by expiry)
    private static final int FLEE_RADIUS = 16;           // how far it scans for a standing spot away from you
    private static final int FLEE_Y = 7;                 // vertical slack allowed on that flee spot
    private static final double FLEE_SPEED = 1.4;        // a multi-ton construct breaking away at a dead run

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof IronGolem golem) {
                // It hunts a ranged attacker from far off — charge across the field, don't lose interest.
                AttributeInstance follow = golem.getAttribute(Attributes.FOLLOW_RANGE);
                if (follow != null) {
                    follow.setBaseValue(Math.max(follow.getBaseValue(), 40.0));
                }
                // Highest priority: once repeatedly provoked it charges (if it can reach you) or flees (if it
                // can't) — this outranks cover-seeking so a fed-up golem stops hiding and commits.
                golem.getGoalSelector().addGoal(0, new ChargeOrFleeGoal(golem));
                golem.getGoalSelector().addGoal(1, new SeekCoverGoal(golem)); // break for cover under ranged fire first
                golem.getGoalSelector().addGoal(2, new SmashThroughGoal(golem));
                golem.getGoalSelector().addGoal(2, new ReachUpGoal(golem));
                // Wrong the village and its iron sentinels come for you too, alongside the armed guards. This
                // TARGET-flag goal rides the golem's GOAL selector because the target selector has no public
                // accessor in 26.2; the selectors track their flag sets independently, so it targets cleanly.
                golem.getGoalSelector().addGoal(4, new VillageDefense.FlaggedPlayerTargetGoal(golem));
            }
        });
    }

    /** Breaks weak blocks between the golem and a target it can't reach — the anti-cheese. Non-exclusive
     *  (no flags), so it runs alongside the golem's normal move/attack goals rather than replacing them. */
    private static class SmashThroughGoal extends Goal {
        private final IronGolem golem;
        private int cooldown;
        private int nextReachCheck;
        private boolean blocked; // cached: can't currently path to the target (re-tested on a throttle)

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
            if (dx * dx + dz * dz > 256.0) {
                return false; // too far off horizontally (>16 blocks) to be worth digging at
            }
            // The gate is REACHABILITY, not line of sight. Keying off sight was the cheese: the moment the golem
            // broke a peephole it could see you through, it stopped digging — and simply moving re-blocked that
            // sightline, so standing still behind a wall froze it. Dig until it can actually WALK to you.
            return isBlockedFromTarget(target);
        }

        /** Can the golem currently NOT path to its target? Throttled — pathfinding every tick per golem is dear. */
        private boolean isBlockedFromTarget(LivingEntity target) {
            if (golem.tickCount >= nextReachCheck) {
                nextReachCheck = golem.tickCount + 10;
                Path path = golem.getNavigation().createPath(target, 0);
                blocked = path == null || !path.canReach();
            }
            return blocked;
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
            // Perched up a tower out of reach (towered up to snipe with a bow) — pound the pillar out from under
            // it. Loose soil then collapses (see DirtFalling), dropping it to you; a flung-up dirt tower is no
            // refuge from a construct that can knock it down.
            if (target.getY() - golem.getY() > UNDERMINE_MIN_DY) {
                undermine(level, target);
                return;
            }
            // Walled off at ground level: carve a walkable, golem-tall DOORWAY through the wall right in front of
            // it, toward the target — not the single block on the eye-line (which turns to air the instant a
            // peephole opens, leaving a gap too small to step through). Breaking the wall in front, feet-to-head,
            // opens a passage it can actually walk, so a player holed up in a house can't just wait it out.
            BlockPos wall = wallToward(level, target);
            if (wall != null) {
                boolean approachAlongX = Math.abs(target.getX() - golem.getX()) >= Math.abs(target.getZ() - golem.getZ());
                breachColumn(level, wall, approachAlongX);
            }
        }

        /** The nearest solid block directly between the golem and its target at body height — the wall to breach.
         *  Marches horizontally toward the target at foot and head level so it finds the wall even when an
         *  eye-level peephole is already open. Returns that column's base (golem foot Y), or null if the way is
         *  clear in front (nothing to dig — the block is elsewhere and navigation will bring it round). */
        private BlockPos wallToward(Level level, LivingEntity target) {
            double dx = target.getX() - golem.getX();
            double dz = target.getZ() - golem.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0e-3) {
                return null;
            }
            double ux = dx / len;
            double uz = dz / len;
            int fy = golem.blockPosition().getY();
            for (double step = 0.5; step <= 3.5; step += 0.5) { // out to just past smash reach, to the wall it faces
                int bx = (int) Math.floor(golem.getX() + ux * step);
                int bz = (int) Math.floor(golem.getZ() + uz * step);
                for (int y = fy; y <= fy + 1; y++) { // foot and body height — a wall blocks one of these
                    BlockPos p = new BlockPos(bx, y, bz);
                    if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                        return new BlockPos(bx, fy, bz); // breach this column from the feet up
                    }
                }
            }
            return null;
        }

        /** Carve a walkable, golem-tall AND golem-WIDE doorway through the wall at the given column. The golem is
         *  ~1.4 blocks wide, so a single one-block gap won't fit it — the pathfinder needs a two-wide clearance,
         *  and when the golem straddles a block boundary neither of the columns it overlaps is fully open. So we
         *  break the found column PLUS its two neighbours across the wall's face (perpendicular to the approach),
         *  feet-to-head — a passage wide and tall enough to actually walk through. Lowest row first so the base
         *  clears before the top; one block per interval. */
        private void breachColumn(Level level, BlockPos wall, boolean approachAlongX) {
            int gy = wall.getY();
            for (int y = gy; y <= gy + 2; y++) {         // feet → head (3 tall for the ~2.7-block golem)
                for (int side = -1; side <= 1; side++) { // centre and both sides — a passage the wide golem fits
                    int bx = wall.getX() + (approachAlongX ? 0 : side);
                    int bz = wall.getZ() + (approachAlongX ? side : 0);
                    if (smash(level, new BlockPos(bx, y, bz))) {
                        return; // one block broken this pass
                    }
                }
            }
        }

        /** Break a single block near the golem if it's within reach and not too tough. Returns whether a block
         *  was actually pulverised this call (so callers can stop after one per interval). */
        private boolean smash(Level level, BlockPos pos) {
            if (golem.distanceToSqr(Vec3.atCenterOf(pos)) > SMASH_RANGE_SQ) {
                return false; // too far to be the cheese wall — leave the world alone
            }
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || hardness < 0f || hardness > MAX_HARDNESS) {
                return false; // air, unbreakable (bedrock), or too tough for even a golem
            }
            golem.swing(InteractionHand.MAIN_HAND);
            level.destroyBlock(pos, false, golem, 512); // pulverised — plays the break effect itself
            cooldown = SMASH_INTERVAL;
            return true;
        }

        /** Dig out the pillar under a perched target: break a reachable block of its supporting column near
         *  the golem's own height. With the base gone, loose soil above it falls and the tower comes down. */
        private void undermine(Level level, LivingEntity target) {
            int cx = (int) Math.floor(target.getX());
            int cz = (int) Math.floor(target.getZ());
            int gy = golem.blockPosition().getY();
            for (int y = gy + 2; y >= gy - 1; y--) {
                if (smash(level, new BlockPos(cx, y, cz))) {
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
     * cover. This pulls the mob to the nearest standing spot that puts something solid between it and the
     * archer, favouring cover that's <b>toward</b> the shooter so it <b>advances under cover</b> rather than
     * cowering. It only triggers on ranged fire from a distance (up close it still fights head-on), and in
     * open ground with no cover to reach it simply charges — so it stays the relentless closer, just not a
     * stationary target. Highest priority, so a defender under fire ducks before it does anything else.
     *
     * <p>Generic over any {@link PathfinderMob} so both the iron golem and the {@link VillageGuard armed
     * village guards} share it — the tactical "advance under fire" brain lives here, once.
     */
    static class SeekCoverGoal extends Goal {
        private final PathfinderMob mob;
        private Player shooter;
        private Vec3 cover;
        private int nextSearch;

        SeekCoverGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(mob.getLastHurtByMob() instanceof Player p) || !p.isAlive() || p.isCreative()) {
                return false;
            }
            if (mob.tickCount - mob.getLastHurtByMobTimestamp() > RECENT_HIT_TICKS) {
                return false; // not shot recently
            }
            if (mob.distanceToSqr(p) < COVER_TRIGGER_SQ) {
                return false; // it's near — close in and fight, don't hide
            }
            if (!mob.getSensing().hasLineOfSight(p)) {
                return false; // already out of its line — no need to move
            }
            if (mob.tickCount < nextSearch) {
                return false; // throttle the (costly) cover scan
            }
            nextSearch = mob.tickCount + 10;
            // If it can just walk to you, CHARGE — don't detour to cover. Cover is only for when the direct
            // approach is blocked (a gap, or a wall it has to go around).
            Path path = mob.getNavigation().createPath(p, 0);
            if (path != null && path.canReach()) {
                return false;
            }
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
                && !this.mob.getNavigation().isDone()
                && this.mob.getSensing().hasLineOfSight(this.shooter); // stop once it's safely behind cover
        }

        @Override
        public void start() {
            this.mob.getNavigation().moveTo(this.cover.x, this.cover.y, this.cover.z, 1.3);
        }

        @Override
        public void stop() {
            this.shooter = null;
            this.cover = null;
        }

        /** Nearest reachable standing spot (favouring ones toward the shooter, to advance) whose line to the
         *  archer is blocked by a solid block. Null if nothing within range gives cover (open ground). */
        private BlockPos findCover(Player p) {
            Level level = this.mob.level();
            Vec3 shooterEye = p.getEyePosition();
            BlockPos origin = this.mob.blockPosition();
            BlockPos best = null;
            double bestToShooter = Double.MAX_VALUE;
            for (int dx = -COVER_SEARCH; dx <= COVER_SEARCH; dx++) {
                for (int dz = -COVER_SEARCH; dz <= COVER_SEARCH; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos spot = origin.offset(dx, dy, dz);
                        if (!standable(level, spot)) {
                            continue;
                        }
                        if (!hiddenFrom(level, spot, shooterEye)) {
                            continue; // not fully screened — the mob needs feet, body AND head covered
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

        /** A spot only counts as cover if something screens the mob's feet, middle AND head from the archer —
         *  a one-block wall doesn't hide a tall body. Sample heights scale with the mob's own height (a 2.7 m
         *  golem needs taller cover than a 1.95 m guard). Head checked first (bails cheapest). */
        private boolean hiddenFrom(Level level, BlockPos spot, Vec3 shooterEye) {
            double h = this.mob.getBbHeight();
            double[] heights = {h * 0.95, 0.3, h * 0.5};
            for (double sample : heights) {
                Vec3 from = new Vec3(spot.getX() + 0.5, spot.getY() + sample, spot.getZ() + 0.5);
                BlockHitResult hit = level.clip(new ClipContext(from, shooterEye,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob));
                if (hit.getType() != HitResult.Type.BLOCK) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * The provoked-escalation goal. A defender's first response to being hurt is to break for cover
     * ({@link SeekCoverGoal}); this goal is what happens when you <b>keep hitting it anyway</b>. It counts
     * recent blows from a player (via the vanilla hurt-by timestamp) and only fires once you're past the
     * first, so it never overrides the initial retreat — it's the second-stage reaction.
     *
     * <p>When provoked it makes one decision: <b>can it path to you?</b>
     * <ul>
     *   <li><b>Yes</b> — it stands down from this goal ({@code canUse} returns {@code false}) and lets its
     *       normal {@code MeleeAttackGoal} charge home and attack. No point steering a fight the vanilla AI
     *       already wins; we only exist to change what happens when it <i>can't</i> fight.</li>
     *   <li><b>No</b> — there's no route to you (a gap, a wall it can't route around, you're up on terrain
     *       it can't climb): it <b>flees</b> to a spot away from you and keeps running until the blows stop
     *       for {@link #PROVOKE_RESET_TICKS}. A defender being poked from an unreachable perch doesn't
     *       stand still and get farmed — it leaves.</li>
     * </ul>
     *
     * <p>Holds only the {@link Flag#MOVE} flag and sits at the top priority, so a fleeing defender wins the
     * navigation over both cover-seeking and melee; the moment a path to the player opens up it yields so
     * the charge can take over instead. Generic over {@link PathfinderMob} — shared by the golem and the
     * {@link VillageGuard village guards}.
     */
    static class ChargeOrFleeGoal extends Goal {
        private final PathfinderMob mob;
        private int lastHurtStamp;   // the last hurt-by timestamp we've already counted
        private int hitCount;        // blows taken within the current provoke window
        private int lastHitTick;     // mob.tickCount of the most recent counted blow
        private Player fleeFrom;
        private Vec3 fleeTarget;

        ChargeOrFleeGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        /**
         * Fold any new blow into the provoke counter and let it decay. Runs every tick — from {@code canUse}
         * while idle (this is the top-priority goal, so the selector polls it each tick) and from
         * {@code tick} while fleeing — so no hit is ever missed regardless of which state we're in. Detection
         * keys off the vanilla hurt-by <i>timestamp</i> changing, so it's independent of poll timing.
         */
        private void trackHits() {
            int stamp = mob.getLastHurtByMobTimestamp();
            if (stamp != lastHurtStamp && mob.getLastHurtByMob() instanceof Player) {
                if (mob.tickCount - lastHitTick > PROVOKE_RESET_TICKS) {
                    hitCount = 0; // the previous flurry had already lapsed — start a fresh window
                }
                hitCount++;
                lastHitTick = mob.tickCount;
            }
            lastHurtStamp = stamp;
            if (hitCount > 0 && mob.tickCount - lastHitTick > PROVOKE_RESET_TICKS) {
                hitCount = 0; // the hits stopped for a few seconds — stand down
            }
        }

        @Override
        public boolean canUse() {
            trackHits();
            if (hitCount < PROVOKE_THRESHOLD) {
                return false; // still on the first blow (or calm) — leave the retreat to SeekCoverGoal
            }
            if (!(mob.getLastHurtByMob() instanceof Player p) || !p.isAlive() || p.isCreative()) {
                return false;
            }
            // Can it get at the attacker? Then don't take the wheel — its melee charges in the vanilla way.
            Path path = mob.getNavigation().createPath(p, 0);
            if (path != null && path.canReach()) {
                return false;
            }
            // No route to you and you're still hurting it: turn tail and run.
            Vec3 away = DefaultRandomPos.getPosAway(mob, FLEE_RADIUS, FLEE_Y, p.position());
            if (away == null) {
                return false; // boxed in with nowhere to flee — nothing useful to do here
            }
            this.fleeFrom = p;
            this.fleeTarget = away;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (hitCount < PROVOKE_THRESHOLD) {
                return false; // blows have lapsed (see trackHits decay) — stop fleeing
            }
            if (this.fleeFrom == null || !this.fleeFrom.isAlive()) {
                return false;
            }
            // If a path to the player opens up mid-flight, bail so its melee can turn and charge instead.
            Path path = mob.getNavigation().createPath(this.fleeFrom, 0);
            if (path != null && path.canReach()) {
                return false;
            }
            return !mob.getNavigation().isDone() || fleeTarget != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            mob.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, FLEE_SPEED);
        }

        @Override
        public void tick() {
            trackHits(); // keep counting blows landed while we're running, so the flee ends when they stop
            if (fleeFrom == null) {
                return;
            }
            if (mob.getNavigation().isDone()) {
                // Reached the last spot but still being pursued/hit — pick a fresh spot further from the player.
                Vec3 away = DefaultRandomPos.getPosAway(mob, FLEE_RADIUS, FLEE_Y, fleeFrom.position());
                if (away != null) {
                    fleeTarget = away;
                    mob.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
                }
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            fleeFrom = null;
            fleeTarget = null;
        }
    }
}
