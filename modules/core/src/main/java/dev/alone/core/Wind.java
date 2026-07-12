package dev.alone.core;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A prevailing wind (proposal §7.3 — tracking sign). There's no wind in vanilla, but wind is the single
 * most important thing in real hunting and predator-avoidance: scent travels <b>downwind</b>, so an animal
 * downwind of you smells you from far off while one upwind barely catches you. This gives the world a
 * <b>steady wind for the day that shifts day to day</b> — a direction the whole map shares, derived
 * deterministically from the day count (no RNG, so it's identical on every client and across a reload).
 * {@link Scent} reads it so carried meat carries on the wind, not in a tidy circle.
 */
public final class Wind {
    private Wind() {
    }

    /** Horizontal unit vector the wind blows <b>toward</b> — fixed for the in-game day, shifting each day. */
    public static Vec3 direction(Level level) {
        long day = level.getOverworldClockTime() / 24000L;
        // Deterministic per-day angle, no RNG — a SplitMix-style mix of the day number spread over a full turn.
        long h = day * 0x9E3779B97F4A7C15L;
        h ^= (h >>> 30);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        double angle = ((h >>> 11) & 0x1FFFFFFFFFFFFFL) / (double) (1L << 53) * (Math.PI * 2.0);
        return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
    }

    /**
     * How hard the wind blows today, <b>0 (dead calm) .. 1 (strong)</b> — steady for the day, shifting day
     * to day, deterministic like the direction. On a calm day scent barely rides the wind (you can approach
     * from any quarter, and predators smell your meat only close by); on a strong day it carries far and
     * reading the wind is everything. Drives the HUD wind gauge's needle length.
     */
    public static float strength(Level level) {
        long day = level.getOverworldClockTime() / 24000L;
        long h = (day * 0x9E3779B97F4A7C15L) ^ 0xD1B54A32D192ED03L; // different mix than the direction — uncorrelated
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        return (float) (((h >>> 11) & 0x1FFFFFFFFFFFFFL) / (double) (1L << 53));
    }

    /** The compass point the wind comes <b>from</b> (how a person names a wind) — "the west", etc. */
    public static String comingFrom(Level level) {
        Vec3 toward = direction(level);
        double fromX = -toward.x;
        double fromZ = -toward.z; // +x is east, +z is south in Minecraft
        double deg = Math.toDegrees(Math.atan2(fromZ, fromX));
        if (deg < 0.0) {
            deg += 360.0;
        }
        String[] points = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        return points[(int) Math.round(deg / 45.0) % 8];
    }
}
