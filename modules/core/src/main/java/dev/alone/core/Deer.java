package dev.alone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;

/**
 * The deer (proposal §7.2) — the forest's proper wild game, the animal a survivor really lives off. It's
 * built for now on the cow's grazing-and-breeding brain (and made <b>skittish</b> by {@link Wildlife},
 * since it's wild, not livestock, so it bolts when you get near), but tuned to run <b>fast</b> — you can't
 * just walk up to a deer, you stalk it or run it down over distance by
 * [persistence hunting]. Like the {@link BrownBear brown bear} it reuses an existing model as placeholder
 * art (the cow) until a real deer model lands, and it slots into every wildlife system: skittish flight,
 * hide/bone/sinew on a blade kill, {@link GameStock overhunting}, and {@link Seasons seasonal} breeding.
 */
public class Deer extends Cow {
    public Deer(EntityType<? extends Cow> type, Level level) {
        super(type, level);
        // Deer are strong swimmers and take to water to escape — so run one to a coast and it swims off,
        // it doesn't freeze at the shoreline (a cow's land pathfinding treats water as a wall). Let it float
        // and treat water as passable so the flight can carry on across a river or into the sea.
        this.getNavigation().setCanFloat(true);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    /** Deer are powerful swimmers (~13–15 mph) — far faster in water than a person (~5). So a deer that
     *  takes to water to escape <b>outswims you</b>: the coast is its refuge, not a place you corner it.
     *  Top its swim up to a strong pace in whatever direction it's already headed (its flee heading). */
    private static final double SWIM_SPEED = 0.25;

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isInWater()) {
            Vec3 v = this.getDeltaMovement();
            double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
            if (horiz > 0.02 && horiz < SWIM_SPEED) {
                double f = SWIM_SPEED / horiz;
                this.setDeltaMovement(v.x * f, v.y, v.z * f);
            }
        }
    }

    @Override
    public Cow getBreedOffspring(ServerLevel level, AgeableMob mate) {
        return AloneEntities.DEER.create(level, EntitySpawnReason.BREEDING); // deer breed deer, not cows
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return AloneSounds.DEER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return AloneSounds.DEER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return AloneSounds.DEER_DEATH;
    }
}

