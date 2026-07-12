package dev.alone.core;

import java.util.EnumSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Wary wildlife (proposal §7.2). With hostile mobs cleared out of the open wilderness (§7.1), the
 * wilds are <b>animals</b> — and wild game doesn't stand around to be clubbed. <b>Wildlife</b> keeps
 * its distance and bolts when you get close, unless you approach <b>crouched</b> (stalking) — so
 * hunting means stealth, a bow, or cornering, not a foot-race.
 *
 * <p>What counts as wild vs. tame is a datapack classification: <b>{@code alone:domestic}</b> (cows,
 * pigs, sheep, chickens, and the ride-able livestock, §9.1) stay calm so farming/companions work,
 * while everything else passive is treated as skittish wildlife.
 *
 * <p>The flee is a purpose-built goal ({@link SkittishFleeGoal}) rather than vanilla's
 * {@code AvoidEntityGoal}: it triggers on <b>proximity</b> (a wary animal reads scent and sound, not just
 * line of sight), sits at its own priority so it can't be shadowed by another goal, and is fully tunable.
 */
public final class Wildlife {
    private Wildlife() {
    }

    /** Barterable livestock — calm around people; the rest of the passive animals are wild game. */
    public static final TagKey<EntityType<?>> DOMESTIC =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "domestic"));

    private static final double FLEE_RANGE = 12.0;  // how close a standing player they'll tolerate before bolting
    private static final double PURSUIT_RANGE = 26.0; // once spooked they keep running until you fall this far back —
                                                       // so you can PACE a deer from a distance and wear it down,
                                                       // rather than having to sprint-glue to within 12 blocks
    private static final double BOLT_RANGE = 9.0;   // inside this they put on a burst, not just amble off
    private static final double WALK_AWAY = 1.05;   // the animal's own (already fast) pace carries the flee
    private static final double BOLT = 1.25;        // a modest burst when you're on them (base speed does the work)

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Animal animal && shouldBeWary(animal)) {
                animal.getGoalSelector().addGoal(2, new SkittishFleeGoal(animal));
            }
        });
    }

    /** Wild enough to be skittish — not domestic livestock, tamed pets, babies, led animals, or bold
     *  predators. Wolves and polar bears aren't skittish game that bolts from you; they hold their ground
     *  (and are drawn to carried meat — see {@link Scent}), so they're left out of the flee behaviour. */
    private static boolean shouldBeWary(Animal animal) {
        if (animal.isBaby() || animal.isLeashed() || animal.getType().builtInRegistryHolder().is(DOMESTIC)
            || Scent.PREDATORS.contains(animal.getType())) {
            return false;
        }
        return !(animal instanceof TamableAnimal tame && tame.isTame());
    }

    /** A player worth fleeing — one who isn't sneaking up on you (stalking), and isn't in creative/spectator. */
    private static boolean spooks(Player player) {
        return player.isAlive() && !player.isShiftKeyDown() && !player.isCreative() && !player.isSpectator();
    }

    /**
     * Bolt from a standing player who comes within range — sprinting when they get close, ambling when
     * they're at the edge. Proximity-triggered (no line-of-sight requirement), re-pathing as it goes so it
     * keeps its distance rather than fleeing once and stopping. Sneak to stalk within range without spooking it.
     */
    private static class SkittishFleeGoal extends Goal {
        private final PathfinderMob mob;
        private Player threat;
        private int repath;

        SkittishFleeGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private Player nearestThreat() {
            Player nearest = null;
            double best = FLEE_RANGE * FLEE_RANGE;
            for (Player p : this.mob.level().players()) {
                if (!spooks(p)) {
                    continue;
                }
                double d = p.distanceToSqr(this.mob);
                if (d < best) {
                    best = d;
                    nearest = p;
                }
            }
            return nearest;
        }

        @Override
        public boolean canUse() {
            this.threat = nearestThreat();
            return this.threat != null && fleeFrom(this.threat);
        }

        @Override
        public boolean canContinueToUse() {
            // Keep running until the pursuer falls well back — it's committed to flight now, so you can
            // pace it (jog to keep it inside this range) instead of having to stay right on top of it.
            return this.threat != null && spooks(this.threat)
                && this.threat.distanceToSqr(this.mob) < PURSUIT_RANGE * PURSUIT_RANGE;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.threat == null) {
                return;
            }
            this.mob.getLookControl().setLookAt(this.threat); // keep an eye on the threat
            boolean close = this.mob.distanceToSqr(this.threat) < BOLT_RANGE * BOLT_RANGE;
            this.mob.getNavigation().setSpeedModifier(close ? BOLT : WALK_AWAY);
            // Re-pick an escape often (not only when the path ends) so it flees decisively away from you
            // rather than dawdling toward a stale point and letting you close the gap.
            if (--this.repath <= 0 || this.mob.getNavigation().isDone()) {
                fleeFrom(this.threat);
                this.repath = 20;
            }
        }

        @Override
        public void stop() {
            this.threat = null;
            this.mob.getNavigation().stop();
        }

        private boolean fleeFrom(Player p) {
            Vec3 away = DefaultRandomPos.getPosAway(this.mob, 24, 7, p.position()); // a long, committed flee leg
            if (away == null) {
                return false;
            }
            boolean close = this.mob.distanceToSqr(p) < BOLT_RANGE * BOLT_RANGE;
            return this.mob.getNavigation().moveTo(away.x, away.y, away.z, close ? BOLT : WALK_AWAY);
        }
    }
}
