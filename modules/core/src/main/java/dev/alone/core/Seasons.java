package dev.alone.core;

import net.minecraft.world.level.Level;

/**
 * Seasons (proposal §10) — a slow spring → summer → autumn → winter cycle over the world's days that
 * shifts ambient temperature (winter is the recurring boss). No new blocks; it feeds the temperature
 * model via {@link SurvivalMeters#ambientTemperature}.
 */
public final class Seasons {
    private Seasons() {
    }

    // One season is a "month" of 28 in-game days; four of them make a 112-day year — spring, summer,
    // autumn, winter. That's a real stretch per season (~a month, not a week), so winter is a month-long
    // survival test you stockpile for, and a crop has a whole season to grow. (Was 7 — over-compressed.)
    public static final int SEASON_LENGTH_DAYS = 28;

    // Debug override (set via /alone season). Volatile shared static — reflects on both sides of a
    // single-player integrated server. Null = natural, day-derived season.
    private static volatile Integer overrideIndex = null;

    /** Force the season (debug): 0 spring, 1 summer, 2 autumn, 3 winter. */
    public static void setOverride(int index) {
        overrideIndex = ((index % 4) + 4) % 4;
    }

    /** Back to the natural, day-derived season. */
    public static void clearOverride() {
        overrideIndex = null;
    }

    /** The world starts in <b>autumn</b> (index 2), like an <em>Alone</em> drop: you land as the cold is
     *  coming on, and <b>winter looms one season out</b> (~{@link #SEASON_LENGTH_DAYS} days) — so shelter and a
     *  fire are an early necessity, not a someday luxury. From there: autumn → winter → spring → summer. */
    private static final int START_SEASON = 2; // autumn

    /** 0 = spring, 1 = summer, 2 = autumn, 3 = winter. */
    public static int index(Level level) {
        if (overrideIndex != null) {
            return overrideIndex;
        }
        long day = level.getGameTime() / 24000L;
        return (int) ((day / SEASON_LENGTH_DAYS + START_SEASON) % 4L);
    }

    /** Deep cold — frozen ground, nothing grows (see the crop-growth mixin). */
    public static boolean isWinter(Level level) {
        return index(level) == 3;
    }

    public static String name(Level level) {
        return switch (index(level)) {
            case 1 -> "Summer";
            case 2 -> "Autumn";
            case 3 -> "Winter";
            default -> "Spring";
        };
    }

    /** Added to ambient temperature (biome scale): winter deep cold, summer hot, autumn a real cooling toward
     *  winter (nights bite), spring mild. Autumn isn't neutral — it's the shoulder that warns winter is close. */
    public static float temperatureOffset(Level level) {
        return switch (index(level)) {
            case 1 -> 0.30f;   // summer — hot
            case 2 -> -0.12f;  // autumn — cooling; nights get cold, a nudge to build before winter
            case 3 -> -0.45f;  // winter — deadly cold, lethal at night without shelter + fire
            default -> 0f;     // spring — mild
        };
    }
}
