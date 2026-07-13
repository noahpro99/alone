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
 *
 * <p>A rest skips only the <b>phase you're in</b>: everyone <b>sleeping at night</b> tick-sprints to
 * <b>dawn</b>, and everyone <b>resting by day</b> tick-sprints to <b>nightfall</b> — the clock hands back
 * and everyone rises the moment the phase flips, so "skip the night" and "skip the day" are two distinct
 * things, not a blanket jump to morning. Either way it needs <b>every</b> player lying down: one person
 * can't drag the whole server's clock forward.
 */
public final class GradualSleep {
    private GradualSleep() {
    }

    /** ~15x real time — a phase fast-forwards in roughly half a minute, fast but still perceptibly time. */
    private static final float SLEEP_TICK_RATE = 300f;
    private static final float NORMAL_TICK_RATE = 20f;

    private static boolean fastForwarding = false;
    /** Which phase the current sprint is skipping: {@code true} = a daytime rest (skip on to nightfall),
     *  {@code false} = a night's sleep (skip on to dawn). We stop the moment daylight flips away from this. */
    private static boolean skippingDaylight = false;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GradualSleep::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        var tickRate = server.tickRateManager();
        boolean daylight = server.overworld().isBrightOutside();

        // The world only fast-forwards when EVERY (non-spectator) player is lying down — a night's sleep to
        // skip to dawn, or a daytime rest to skip to nightfall. Requiring all of them means one player can't
        // drag everyone else's clock forward in multiplayer.
        if (allPlayersResting(server)) {
            if (!fastForwarding) {
                // Begin the sprint, and remember which phase we're skipping so we know where to stop.
                tickRate.setTickRate(SLEEP_TICK_RATE);
                fastForwarding = true;
                skippingDaylight = daylight;
            } else if (daylight != skippingDaylight) {
                // The phase we were skipping just ended — daybreak after a night's sleep, or nightfall after
                // a day's rest. Hand the clock back and rouse everyone so the rest ends right at the boundary.
                stopSprint(server);
            }
            return;
        }

        // Someone got up (no longer unanimous) — back to real time; leave anyone still lying down as they are.
        if (fastForwarding) {
            tickRate.setTickRate(NORMAL_TICK_RATE);
            fastForwarding = false;
        }
    }

    private static void stopSprint(MinecraftServer server) {
        server.tickRateManager().setTickRate(NORMAL_TICK_RATE);
        fastForwarding = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSleeping()) {
                player.stopSleeping();
            }
        }
    }

    /** True only when there's at least one non-spectator player and every one of them is lying down. So a
     *  lone sleeper in multiplayer just waits in bed (no time skip) until the rest join them. */
    private static boolean allPlayersResting(MinecraftServer server) {
        boolean anyReal = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }
            anyReal = true;
            if (!player.isSleeping()) {
                return false;
            }
        }
        return anyReal;
    }
}
