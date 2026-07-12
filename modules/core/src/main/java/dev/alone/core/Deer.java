package dev.alone.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;

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

