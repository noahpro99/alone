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

    /** ~15x real time — a full night fast-forwards in roughly half a minute, fast but still perceptibly a night. */
    private static final float SLEEP_TICK_RATE = 300f;
    private static final float NORMAL_TICK_RATE = 20f;
    /** Night window (matches the ground-rest rules) — sleeping fast-forwards only across the dark hours. */
    private static final long NIGHT_START = 13000L;
    private static final long NIGHT_END = 23000L;

    private static boolean fastForwarding = false;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GradualSleep::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        boolean anySleeping = anyPlayerSleeping(server);
        long timeOfDay = server.overworld().getOverworldClockTime() % 24000L;
        boolean night = timeOfDay >= NIGHT_START && timeOfDay < NIGHT_END;
        var tickRate = server.tickRateManager();

        // Someone's bedded down during the night: run the clock fast until dawn.
        if (anySleeping && night) {
            if (!fastForwarding) {
                tickRate.setTickRate(SLEEP_TICK_RATE);
                fastForwarding = true;
            }
            return;
        }

        // Otherwise we're not fast-forwarding a night any more — either dawn arrived, or the sleeper woke
        // (or was woken). Hand the tick rate back to real time...
        if (fastForwarding) {
            tickRate.setTickRate(NORMAL_TICK_RATE);
            fastForwarding = false;
        }
        // ...and if morning has come while someone's still lying down, get them up.
        if (anySleeping && !night) {
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
