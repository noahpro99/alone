package dev.alone.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.WeatherData;

/**
 * Telegraphed weather (proposal §10 / roadmap: weather with teeth). A storm doesn't arrive out of nowhere —
 * the sky tells you it's coming, and reading that was survival-critical: you get your fire covered, your
 * drying meat in, and yourself under a roof before the rain (or a {@link SurvivalMeters#isBlizzard blizzard})
 * hits. This reads the world's own weather schedule ahead of time and gives a <b>lead warning</b> to anyone
 * out under the open sky (you can't read the weather from the bottom of a cave), once per approaching front.
 */
public final class Weather {
    private Weather() {
    }

    private static final int SCAN = 100;       // check the sky ~every 5s
    private static final int WARN_LEAD = 2400;  // warn when a front is within ~2.4h (rain-time counts in ticks)

    private static boolean warnedThisFront = false;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel overworld = server.overworld();
            if (overworld.getGameTime() % SCAN != 0L) {
                return;
            }
            WeatherData weather = overworld.getWeatherData();
            if (weather.isRaining()) {
                warnedThisFront = false; // the front's here — arm the warning for the next clear spell's storm
                return;
            }
            int rainTime = weather.getRainTime();
            if (warnedThisFront || rainTime <= 0 || rainTime > WARN_LEAD) {
                return;
            }
            warnedThisFront = true;
            boolean storm = weather.getThunderTime() <= WARN_LEAD; // rain that'll come in as a thunderstorm
            boolean blizzard = storm && Seasons.isWinter(overworld);
            String sign = blizzard
                ? "The light goes flat and the wind bites harder — a blizzard is building. Get to a roof and a fire."
                : storm
                ? "The air turns heavy and still, the sky bruising dark — a storm is building. Get under cover."
                : "The wind shifts and the sky hazes over — rain is coming on before long.";
            Component message = Component.literal(sign);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // You read the weather from the open sky, not from underground or under a solid roof.
                if (player.level() == overworld && player.level().canSeeSky(player.blockPosition())) {
                    player.sendSystemMessage(message);
                }
            }
        });
    }
}
