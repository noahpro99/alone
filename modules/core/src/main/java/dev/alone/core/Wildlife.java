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

    // Detection ranges tuned to real deer, compressed only where the game must. A deer's nose is the giant
    // sense — it winds a human a couple hundred metres downwind — while sight needs line of sight and a
    // crouched stalk gets you to bow range. So the only approach is crouched, from downwind.
    private static final double FLEE_RANGE = 45.0;  // sight of a STANDING/moving player, if it can SEE you (LOS)
    private static final double SNEAK_SIGHT = 9.0;  // crouched, it only makes you out up close — bow range
    private static final double SCENT_RANGE = 200.0; // it SMELLS you from here directly downwind in a strong wind
                                                      // (scaled by wind strength × how downwind you are); no crouch
                                                      // hides scent. (Beyond loaded range a far deer just isn't ticking.)
    private static final double PURSUIT_RANGE = 30.0; // once spooked they keep running until you fall this far back —
                                                       // so you can PACE a deer from a distance and wear it down,
                                                       // rather than having to sprint-glue to within 12 blocks
    private static final double BOLT_RANGE = 9.0;   // inside this they put on a burst, not just amble off
    private static final double[] FLEE_LEGS = {18.0, 11.0, 6.0}; // straight-away distances to try (long first)
    private static final float[] FLEE_FAN = {0f, 25f, -25f, 50f, -50f, 78f, -78f}; // veer angles if straight is blocked
    private static final double WALK_AWAY = 1.05;   // the animal's own (already fast) pace carries the flee
    private static final double BOLT = 1.25;        // a modest burst when you're on them (base speed does the work)

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Animal animal && shouldBeWary(animal)) {
                animal.getGoalSelector().addGoal(2, new SkittishFleeGoal(animal));
            }
        });
    }

    /** Wild enough to be skittish — not domestic livestock, tamed pets, babies, led animals, bold
     *  predators, or dangerous megafauna. Wolves and polar bears aren't skittish game that bolts from you;
     *  they hold their ground (and are drawn to carried meat — see {@link Scent}), so they're left out of
     *  the flee behaviour. The wild boar and bison likewise don't bolt at the sight of a person: they graze
     *  calmly and turn dangerous only when provoked (they retaliate — see {@link WildBoar}/{@link Bison}),
     *  so making them skittish would be both wrong and at odds with their charge. */
    private static boolean shouldBeWary(Animal animal) {
        if (animal.isBaby() || animal.isLeashed() || animal.getType().builtInRegistryHolder().is(DOMESTIC)
            || Scent.PREDATORS.contains(animal.getType())
            || animal instanceof WildBoar || animal instanceof Bison) {
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
        // Scent reaches when the animal lies downwind of you: the player→animal direction runs with the wind.
        // Directly downwind = full reach, crosswind less, upwind (into your face) = nothing. And it only
        // carries as hard as the wind blows — on a calm day there's next to no scent to give you away. Smell
        // travels around terrain (no line of sight needed).
        Vec3 toAnimal = mob.position().subtract(player.position());
        double horiz = Math.sqrt(toAnimal.x * toAnimal.x + toAnimal.z * toAnimal.z);
        double align = horiz < 1.0e-3 ? 0.0 : (toAnimal.x * wind.x + toAnimal.z * wind.z) / horiz;
        double scent = SCENT_RANGE * windStrength * Math.max(0.0, align);
        // Sight/sound: farther standing/moving than crouched, but only if the animal can actually SEE you —
        // it won't spook at a human behind a hill, and forest cover lets you close in. Only raytrace when
        // you're within the sight range at all (cheap distance gate first).
        double sightRange = player.isShiftKeyDown() ? SNEAK_SIGHT : FLEE_RANGE;
        double sight = 0.0;
        if (player.distanceToSqr(mob) <= sightRange * sightRange && mob.hasLineOfSight(player)) {
            sight = sightRange;
        }
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
            // Bolt in a straight line directly away from the player — the way real game flees, putting
            // distance between you. But if terrain blocks that direct line (a tree, a rise), FAN OUT to
            // either side and take an angled escape instead of STALLING — that stall is why it "ran, then
            // stopped and waited for you". It only truly stops if it's hemmed in on every side.
            Vec3 base = this.mob.position().subtract(p.position());
            base = new Vec3(base.x, 0.0, base.z);
            if (base.lengthSqr() < 1.0e-4) {
                base = new Vec3(1.0, 0.0, 0.0); // player exactly on it — any heading will do
            }
            base = base.normalize();
            for (float deg : FLEE_FAN) {                 // 0° (straight away) first, then wider veers
                Vec3 dir = rotateY(base, (float) Math.toRadians(deg));
                for (double leg : FLEE_LEGS) {           // a long bolt, shorter if that's blocked too
                    Vec3 target = this.mob.position().add(dir.scale(leg));
                    if (this.mob.getNavigation().moveTo(target.x, target.y, target.z, speed)) {
                        return true;
                    }
                }
            }
            return false; // nowhere to run — hemmed in (a cornered animal stands, and will fight)
        }

        private static Vec3 rotateY(Vec3 v, float rad) {
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            return new Vec3(v.x * cos - v.z * sin, 0.0, v.x * sin + v.z * cos);
        }
    }
}
