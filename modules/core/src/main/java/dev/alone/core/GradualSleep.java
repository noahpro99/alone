package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sleep as a gradual time-skip (proposal §5.2, roadmap "gradual sleep"). Vanilla teleports the night to
 * dawn; that's not how a night passes. Instead — with the instant-skip suppressed in {@link
 * dev.alone.core.mixin.SleepStatusMixin} — bedding down runs the world's <b>tick rate fast</b> until
 * morning, then hands it back. The night genuinely happens, only compressed: mobs still roam, weather
 * still moves, your body still burns the night's food and water (see the sleep metabolism in
 * {@link SurvivalMeters}). And because it's real time passing rather than a blink, it can be
 * <b>interrupted</b> — a mob wakes you, a scent event stirs you, and you're back to real time the very
 * next tick, "a second later, but hours later in game time." The tick-rate lever is global (single-player
 * is one world), so it's driven once from the server tick, not per player.
 */
public final class GradualSleep {
    private GradualSleep() {
    }

    /** ~15x real time — a full night fast-forwards in roughly half a minute, fast but still perceptibly time. */
    private static final float SLEEP_TICK_RATE = 300f;
    private static final float NORMAL_TICK_RATE = 20f;
    /** First light (just after sunrise) — whoever's lying down rises here, so a rest always ends at dawn. */
    private static final long MORNING_RISE = 1000L;

    private static boolean fastForwarding = false;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GradualSleep::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        boolean anyResting = anyPlayerSleeping(server);
        long timeOfDay = server.overworld().getOverworldClockTime() % 24000L;
        boolean morning = timeOfDay < MORNING_RISE; // first light — time to get up
        var tickRate = server.tickRateManager();

        // Anyone lying down — a night's sleep OR a daytime rest to pass the hours (convalescing an injury,
        // waiting out weather, letting a crop grow) — fast-forwards the world until first light. It's the
        // one way to skip the long real durations the pack models: you rest, and the clock runs.
        if (anyResting && !morning) {
            if (!fastForwarding) {
                tickRate.setTickRate(SLEEP_TICK_RATE);
                fastForwarding = true;
            }
            return;
        }

        // First light (or everyone got up on their own): hand the tick rate back to real time...
        if (fastForwarding) {
            tickRate.setTickRate(NORMAL_TICK_RATE);
            fastForwarding = false;
        }
        // ...and rouse anyone still lying down when morning comes.
        if (anyResting) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSleeping()) {
                    player.stopSleeping();
                }
            }
        }
    }

    private static boolean anyPlayerSleeping(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSleeping()) {
                return true;
            }
        }
        return false;
    }
}
