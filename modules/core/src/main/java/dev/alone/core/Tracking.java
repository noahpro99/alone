package dev.alone.core;

import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Persistence hunting (roadmap: Wildlife &amp; hunting — tracking) — humanity's oldest hunting method,
 * made real. Wild game is faster than you in a sprint (it bolts when you close in, see {@link Wildlife}),
 * but it can't sprint <b>forever</b>: keep the pressure on and a fleeing animal <b>tires and slows</b>,
 * until an exhausted quarry is slow enough to run down and finish by hand — no bow required.
 *
 * <p>Fatigue builds only while it's actually <b>running from you</b> and <b>bleeds back off when it gets
 * to rest</b>, so a half-hearted chase lets it recover — you have to stay on it. Domestic livestock (which
 * don't flee) and bold predators (wolves/bears) are left out; this is for the wild game that runs.
 */
public final class Tracking {
    private Tracking() {
    }

    private static final int SCAN = 20;               // press the chase ~every 1s
    private static final double RADIUS = 18.0;         // game this close feels the pursuit
    private static final double FLEE_SPEED_SQ = 0.014; // horizontal speed² above which it's genuinely running
    private static final float GAIN = 5f;              // fatigue gained per second of hard running
    private static final float RECOVER = 2f;           // and shed per second at rest (slower than it builds)
    private static final float MAX = 100f;
    private static final float SLOW_THRESHOLD = 45f;   // winded — it starts to flag (~9s of sustained chase)

    /** Chase fatigue on a hunted animal, 0..{@link #MAX}. Transient — a session state, not saved. */
    public static final AttachmentType<Float> FATIGUE = AttachmentRegistry.createDefaulted(
        Identifier.fromNamespaceAndPath("alone", "chase_fatigue"), () -> 0f);

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.tickCount % SCAN != 0 || player.isSpectator()) {
                    continue;
                }
                if (player.level() instanceof ServerLevel level) {
                    pursue(level, player);
                }
            }
        });
    }

    private static void pursue(ServerLevel level, ServerPlayer player) {
        double radiusSq = RADIUS * RADIUS;
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<Animal> game = level.getEntitiesOfClass(Animal.class, box, Tracking::isWildGame);
        for (Animal animal : game) {
            float fatigue = animal.getAttachedOrElse(FATIGUE, 0f);
            Vec3 v = animal.getDeltaMovement();
            boolean running = (v.x * v.x + v.z * v.z) > FLEE_SPEED_SQ
                && animal.distanceToSqr(player) <= radiusSq;
            if (running) {
                fatigue = Math.min(MAX, fatigue + GAIN);
                if (fatigue >= SLOW_THRESHOLD) {
                    // The more spent it is, the harder it flags — Slowness I..III as it approaches collapse.
                    int amplifier = (int) ((fatigue - SLOW_THRESHOLD) / (MAX - SLOW_THRESHOLD) * 2f);
                    animal.addEffect(new MobEffectInstance(
                        MobEffects.SLOWNESS, SCAN + 20, amplifier, false, false, false));
                }
            } else {
                fatigue = Math.max(0f, fatigue - RECOVER); // catching its breath
            }
            animal.setAttached(FATIGUE, fatigue);
        }
    }

    /** Wild game that actually runs — not livestock, tamed pets, babies, or bold predators. */
    private static boolean isWildGame(Animal animal) {
        if (animal.isBaby() || animal.getType().builtInRegistryHolder().is(Wildlife.DOMESTIC)) {
            return false;
        }
        if (animal instanceof TamableAnimal tame && tame.isTame()) {
            return false;
        }
        return !Scent.PREDATORS.contains(animal.getType());
    }
}
