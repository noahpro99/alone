package dev.alone.core;

import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Blood trails (roadmap: Wildlife &amp; hunting — tracking sign). Wound an animal and it <b>bleeds</b>: as
 * it flees it drips a trail of blood you can follow — the tracker's craft, and the companion to running
 * game down (see {@link Tracking}). The bleeding is heavier the harder you hit it and <b>clots over time</b>
 * (the wound closes), so the trail is only good while it's fresh — <b>track it promptly or lose it</b>.
 *
 * <p>The drops fade within a second or two of hitting the ground (as real blood sign goes cold), so what
 * you see is a short, fresh dotted line trailing the animal — enough to follow it through brush and over a
 * rise.
 *
 * <p>And a deep wound is mortal: while an animal is <b>badly</b> bleeding it <b>loses blood and weakens</b>,
 * so a solid hit will <b>bleed it out</b> and drop small game outright, while a graze only clots. A good
 * shot is worth more than a lucky one — land it, follow the blood, and the wound finishes the work.
 */
public final class BloodTrail {
    private BloodTrail() {
    }

    private static final int DRIP_INTERVAL = 5;             // lay a drop every quarter-second
    private static final int MORTAL_INTERVAL = 20;          // bleed-out damage once a second (a multiple of DRIP_INTERVAL)
    private static final int BLEED_TICKS_PER_DAMAGE = 40;    // each half-heart of damage → ~2s of bleeding
    private static final int MAX_BLEED = 600;               // a bad wound bleeds up to ~30s
    private static final double TRACK_RADIUS = 26.0;         // drops render for players within this range
    private static final int MORTAL_BLEED = 60;             // only a real wound (≥~1.5 dmg) is deep enough to bleed out
    private static final float BLEED_OUT_DAMAGE = 1.0f;     // blood loss, per second, while the wound runs deep

    /** Remaining bleed time in ticks on a wounded animal. Transient — a session state, not saved. */
    public static final AttachmentType<Integer> BLEED = AttachmentRegistry.createDefaulted(
        Identifier.fromNamespaceAndPath("alone", "bleeding"), () -> 0);

    public static void init() {
        // A hit from a player opens a wound that bleeds (the bigger the hit, the more).
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, takenDamage, blocked) -> {
            if (blocked || takenDamage <= 0f || !(entity instanceof Animal animal) || !animal.isAlive()) {
                return;
            }
            if (source.getEntity() instanceof Player hunter) {
                // A practised tracker's quarry leaves a longer, more followable trail (§8.4) — skill reads
                // the blood better and presses the wounded animal harder.
                float trackBonus = 1f + 0.5f * Skills.proficiency(hunter, Skills.TRACKING);
                int added = Math.round(takenDamage * BLEED_TICKS_PER_DAMAGE * trackBonus);
                int bleed = Math.min(MAX_BLEED, animal.getAttachedOrElse(BLEED, 0) + added);
                animal.setAttached(BLEED, bleed);
            }
        });

        // Bleeding animals drip a fading trail; the wound clots as the bleed timer runs down.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % DRIP_INTERVAL != 0) {
                return;
            }
            // Whether this drip tick is also a bleed-out-damage tick — derived from the SAME server clock as
            // the drip gate. (Previously the damage keyed off each animal's own tickCount, whose phase is set
            // by its spawn tick, so it almost never lined up with the drip gate — bleed-out fired for only
            // ~1 in 5 animals.)
            boolean bleedOut = server.getTickCount() % MORTAL_INTERVAL == 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.level() instanceof ServerLevel level) {
                    dripNearby(level, player, bleedOut);
                }
            }
        });
    }

    private static void dripNearby(ServerLevel level, ServerPlayer player, boolean bleedOut) {
        AABB box = player.getBoundingBox().inflate(TRACK_RADIUS);
        List<Animal> wounded = level.getEntitiesOfClass(Animal.class, box,
            a -> a.getAttachedOrElse(BLEED, 0) > 0);
        for (Animal animal : wounded) {
            int bleed = animal.getAttachedOrElse(BLEED, 0);
            // A drop or two at its feet — where it stands now, so a moving animal lays a dotted trail.
            level.sendParticles(DustParticleOptions.REDSTONE,
                animal.getX(), animal.getY() + 0.1, animal.getZ(),
                2, 0.12, 0.02, 0.12, 0.0);
            // A deep, fresh wound bleeds it out — once a second (clear of hurt-immunity frames), tapering
            // as the wound clots. Small game drops from one good hit; big game needs more.
            if (bleed >= MORTAL_BLEED && bleedOut) {
                animal.hurtServer(level, level.damageSources().generic(), BLEED_OUT_DAMAGE);
            }
            animal.setAttached(BLEED, Math.max(0, bleed - DRIP_INTERVAL));
        }
    }
}
