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

    public static final int SEASON_LENGTH_DAYS = 7; // a 28-day year; tunable

    /** 0 = spring, 1 = summer, 2 = autumn, 3 = winter. */
    public static int index(Level level) {
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
