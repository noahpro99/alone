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

    private static final double FLEE_RANGE = 12.0;  // sight/sound: how close a STANDING player they spot before bolting
    private static final double SNEAK_SIGHT = 4.0;  // crouched (stalking), they only see/hear you up close
    private static final double SCENT_RANGE = 20.0; // and they SMELL you from here when you're UPWIND (your scent
                                                     // blows to them) — no crouch hides that. Stalk from downwind.
    private static final double PURSUIT_RANGE = 26.0; // once spooked they keep running until you fall this far back —
                                                       // so you can PACE a deer from a distance and wear it down,
                                                       // rather than having to sprint-glue to within 12 blocks
    private static final double BOLT_RANGE = 9.0;   // inside this they put on a burst, not just amble off
    private static final double[] FLEE_LEGS = {18.0, 11.0, 6.0}; // straight-away distances to try (long first)
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

    /** A player the animal will react to at all — alive, and not in creative/spectator. */
    private static boolean valid(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    /**
     * How far off an animal notices a player right now. Two senses: <b>sight/sound</b> (shorter when the
     * player is crouched — the stalk), and <b>smell</b> — which reaches when the player is <b>upwind</b> so
     * their scent blows to the animal, and <em>no crouch hides it</em>. So the only way to get close is to
     * stalk crouched <b>and</b> from downwind (wind in your face). The wider of the two senses wins.
     */
    private static double noticeRange(PathfinderMob mob, Player player, Vec3 wind, float windStrength) {
        double sight = player.isShiftKeyDown() ? SNEAK_SIGHT : FLEE_RANGE;
        // Scent reaches when the animal lies downwind of you: the player→animal direction runs with the wind.
        // Directly downwind = full reach, crosswind less, upwind (into your face) = nothing. And it only
        // carries as hard as the wind blows — on a calm day there's next to no scent to give you away.
        Vec3 toAnimal = mob.position().subtract(player.position());
        double horiz = Math.sqrt(toAnimal.x * toAnimal.x + toAnimal.z * toAnimal.z);
        double align = horiz < 1.0e-3 ? 0.0 : (toAnimal.x * wind.x + toAnimal.z * wind.z) / horiz;
        double scent = SCENT_RANGE * windStrength * Math.max(0.0, align);
        return Math.max(sight, scent);
    }

    /**
     * Bolt from a player the animal notices (by {@link #noticeRange sight or smell}) — sprinting when they
     * get close, ambling at the edge, re-pathing as it goes so it keeps its distance. To close on one you
     * must beat <b>both</b> senses: <b>crouch</b> (so it doesn't see you) and stay <b>downwind</b> (so it
     * doesn't smell you). Approach standing, or from upwind, and it's gone before you're in reach.
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
            Vec3 wind = Wind.direction(this.mob.level());
            float windStrength = Wind.strength(this.mob.level());
            Player nearest = null;
            double best = Double.MAX_VALUE;
            for (Player p : this.mob.level().players()) {
                if (!valid(p)) {
                    continue;
                }
                double range = noticeRange(this.mob, p, wind, windStrength);
                double d = p.distanceToSqr(this.mob);
                if (d <= range * range && d < best) {
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
            return this.threat != null && valid(this.threat)
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
            boolean close = this.mob.distanceToSqr(p) < BOLT_RANGE * BOLT_RANGE;
            double speed = close ? BOLT : WALK_AWAY;
            // Bolt in a STRAIGHT LINE directly away from the player — the way real game flees, putting
            // distance between you. Vanilla's getPosAway picks a random spot in a wide arc, which up close
            // reads as "running around crazily" and lets you cut it off. Aim straight away; try a long leg,
            // then shorter ones if a wall's right behind it, so it commits to flight instead of circling.
            Vec3 flat = this.mob.position().subtract(p.position());
            flat = new Vec3(flat.x, 0.0, flat.z);
            if (flat.lengthSqr() < 1.0e-4) {
                flat = new Vec3(1.0, 0.0, 0.0); // player exactly on it — any heading will do
            }
            Vec3 dir = flat.normalize();
            for (double leg : FLEE_LEGS) {
                Vec3 target = this.mob.position().add(dir.scale(leg));
                if (this.mob.getNavigation().moveTo(target.x, target.y, target.z, speed)) {
                    return true; // got a clear straight bolt away
                }
            }
            // Boxed in directly behind — let it veer to a nearby opening rather than stall against the wall.
            Vec3 away = DefaultRandomPos.getPosAway(this.mob, 12, 7, p.position());
            return away != null && this.mob.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
    }
}
