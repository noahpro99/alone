package dev.alone.core;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class AloneSounds {
    private AloneSounds() {
    }

    public static final SoundEvent DEER_AMBIENT = register("entity.deer.ambient");
    public static final SoundEvent DEER_HURT = register("entity.deer.hurt");
    public static final SoundEvent DEER_DEATH = register("entity.deer.death");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath("alone", name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
        // Touches the class to trigger static field initialization
    }
}
