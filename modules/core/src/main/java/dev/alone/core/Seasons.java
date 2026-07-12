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

    /** 0 = spring, 1 = summer, 2 = autumn, 3 = winter. */
    public static int index(Level level) {
        if (overrideIndex != null) {
            return overrideIndex;
        }
        long day = level.getGameTime() / 24000L;
        return (int) ((day / SEASON_LENGTH_DAYS) % 4L);
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

    /** Added to ambient temperature (biome scale): winter cold, summer hot, shoulder seasons mild. */
    public static float temperatureOffset(Level level) {
        return switch (index(level)) {
            case 1 -> 0.30f;
            case 3 -> -0.45f;
            default -> 0f;
        };
    }
}
