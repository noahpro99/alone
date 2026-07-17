package dev.alone.core.client;

import dev.alone.core.net.SurvivalSyncPayload;

/** Client-side cache of the last survival state the server sent, read by the HUD each frame. */
public final class ClientSurvivalState {
    public static volatile float stamina = 100f;
    public static volatile float thirst = 100f;
    public static volatile float temperature = 0f;
    /** +1 warming, -1 cooling, 0 steady — derived from consecutive syncs. */
    public static volatile int temperatureTrend = 0;
    /** Bitmask of active conditions (see Conditions.FLAG_*). */
    public static volatile int conditions = 0;
    /** Medium-term fatigue/soreness, 0..100. */
    public static volatile float fatigue = 0f;
    /** Water in the gut, absorbing into hydration, 0..MAX_GUT. */
    public static volatile float gut = 0f;
    /** Long-term body condition, 0 (wasted away) .. 100 (well-nourished) — the wasting/weight arc. */
    public static volatile float condition = 100f;

    private ClientSurvivalState() {
    }

    public static void update(SurvivalSyncPayload payload) {
        stamina = payload.stamina();
        thirst = payload.thirst();
        float newTemperature = payload.temperature();
        float delta = newTemperature - temperature;
        temperatureTrend = delta > 0.1f ? 1 : (delta < -0.1f ? -1 : 0);
        temperature = newTemperature;
        conditions = payload.conditions();
        fatigue = payload.fatigue();
        gut = payload.gut();
        condition = payload.condition();
    }
}
