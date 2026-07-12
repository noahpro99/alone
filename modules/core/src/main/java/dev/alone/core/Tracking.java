package dev.alone.core;

import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
    private static final double RADIUS = 28.0;         // game this close feels the pursuit (wide enough that a
                                                        // genuinely fast animal stays "in the chase" and tires)
    private static final double FLEE_SPEED_SQ = 0.014; // horizontal speed² above which it's genuinely running
    // A persistence hunt is a long endurance grind, not a sprint. Real ones run hours; compressed to the
    // game's clock that's a few real minutes of sustained pressure. So fatigue builds SLOWLY (mid-size game
    // takes ~2–3 min of keeping it on the run to exhaust) and bleeds off slowly too — an animal you've
    // worked hard stays hot, it can't cool off the moment you slacken. Per-animal endurance and tracking
    // skill scale the rate: small game winds in ~1 min, a horse takes ~5; a skilled tracker closes faster.
    private static final float GAIN = 0.6f;            // fatigue/sec of hard running (mid-size game → ~2.8 min to spend)
    private static final float RECOVER = 0.2f;         // shed per second out of the chase — 3x slower than it builds
    private static final float MAX = 100f;
    private static final float SLOW_THRESHOLD = 30f;   // winded — it starts to visibly flag (~50s of sustained chase)

    // Per-animal endurance: little animals wind fast, big-bodied ones have staying power. We scale the
    // fatigue gain by body mass, using max health as the stand-in (rabbit 3, cow/deer 10, horse 15–30):
    // a rabbit tires several times faster than the reference grazer, a horse markedly slower. Clamped so
    // nothing tires instantly or is impossible to run down.
    private static final float REFERENCE_HEALTH = 10f; // the mid-size grazer GAIN is tuned for (cow, deer)
    private static final float MIN_TIRE = 0.5f;        // biggest game — half-speed fatigue (~18s to flag)
    private static final float MAX_TIRE = 3f;          // smallest game — triple-speed fatigue (~3s to flag)

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
        // Skill by doing (§8.4): a practised tracker reads the quarry and presses it harder — it tires faster.
        float gain = GAIN * (1f + 0.6f * Skills.proficiency(player, Skills.TRACKING));
        AABB box = player.getBoundingBox().inflate(RADIUS);
        List<Animal> game = level.getEntitiesOfClass(Animal.class, box, Tracking::isWildGame);
        for (Animal animal : game) {
            float fatigue = animal.getAttachedOrElse(FATIGUE, 0f);
            Vec3 v = animal.getDeltaMovement();
            boolean running = (v.x * v.x + v.z * v.z) > FLEE_SPEED_SQ
                && animal.distanceToSqr(player) <= radiusSq;
            if (running) {
                // Lighter game winds faster than heavy game (rabbit vs. horse); scale by body mass.
                float tireRate = Mth.clamp(REFERENCE_HEALTH / animal.getMaxHealth(), MIN_TIRE, MAX_TIRE);
                fatigue = Math.min(MAX, fatigue + gain * tireRate);
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
