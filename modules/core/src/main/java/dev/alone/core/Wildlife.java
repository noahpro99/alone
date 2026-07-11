package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

/**
 * Wary wildlife (proposal §7.2). With hostile mobs cleared out of the open wilderness (§7.1), the
 * wilds are <b>animals</b> — and wild game doesn't stand around to be clubbed. <b>Wildlife</b> keeps
 * its distance and bolts when you get close, unless you approach <b>crouched</b> (stalking) — so
 * hunting means stealth, a bow, or cornering, not a foot-race.
 *
 * <p>What counts as wild vs. tame is a datapack classification: <b>{@code alone:domestic}</b> (cows,
 * pigs, sheep, chickens, and the ride-able livestock, §9.1) stay calm so farming/companions work,
 * while everything else passive is treated as skittish wildlife. (There's a companion
 * {@code alone:wildlife} tag for the wild things, incl. spiders, for hunting/spawn rules to build on.)
 */
public final class Wildlife {
    private Wildlife() {
    }

    /** Barterable livestock — calm around people; the rest of the passive animals are wild game. */
    public static final TagKey<EntityType<?>> DOMESTIC =
        TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("alone", "domestic"));

    private static final float FLEE_RANGE = 12f;   // how close a standing player they'll tolerate
    private static final double WALK_AWAY = 1.0;   // amble off when you're at the edge of range
    private static final double BOLT = 1.35;        // sprint clear when you're right on them

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof Animal animal && shouldBeWary(animal)) {
                animal.getGoalSelector().addGoal(1, new AvoidEntityGoal<>(
                    animal, Player.class, FLEE_RANGE, WALK_AWAY, BOLT, Wildlife::spooks));
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

    /** A player worth fleeing — one who isn't sneaking up on you (and isn't in creative/spectator). */
    private static boolean spooks(LivingEntity entity) {
        return entity instanceof Player player
            && !player.isShiftKeyDown() && !player.isCreative() && !player.isSpectator();
    }
}
