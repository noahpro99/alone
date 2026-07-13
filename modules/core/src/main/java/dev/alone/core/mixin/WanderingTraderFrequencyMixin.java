package dev.alone.core.mixin;

import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Bring the caravan by far more often (proposal §9 / progression — the wandering trader is Alone's whole
 * emerald economy, so a once-every-few-hours visit makes trade a novelty instead of a livelihood).
 *
 * <p>In 26.2 the visit cadence is baked into two constants on {@link WanderingTraderSpawner} rather than the old
 * {@code WANDERING_TRADER_SPAWN_DELAY / _CHANCE} gamerules (those are gone; only the
 * {@code SPAWN_WANDERING_TRADERS} boolean remains, and the delay/chance now live in {@code WanderingTraderData}):
 *
 * <ul>
 *   <li><b>The spawn-attempt interval</b> — {@code tick(...)} resets the saved {@code spawnDelay} to {@code 24000}
 *       ticks (20 minutes) between attempts. We cut it to {@code 6000} (5 minutes), so the game rolls for a trader
 *       four times as often.</li>
 *   <li><b>The final spawn gate</b> — even once the timer and the ramping spawn-chance pass, {@code spawn(...)}
 *       still rejects the attempt {@code 9} times in {@code 10} ({@code random.nextInt(10) != 0}). We change that
 *       {@code 10} to {@code 2}, turning a 1-in-10 long shot into a coin-flip.</li>
 * </ul>
 *
 * <p>Together these take the effective time-between-visits from the vanilla ballpark of several in-game hours down
 * to roughly 15–20 minutes — "comes by notably more often" without becoming a constant parade (the trader still
 * lives its normal 48000-tick despawn window, so at most a couple overlap). Both are {@link ModifyConstant} hits
 * with the default {@code require = 1}, so each targets a single unambiguous literal in its method or the build
 * fails loudly.
 */
@Mixin(WanderingTraderSpawner.class)
public class WanderingTraderFrequencyMixin {
    /** Interval between spawn attempts: 24000 ticks (20 min) → 6000 ticks (5 min). */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 24000))
    private int alone$shorterSpawnDelay(final int original) {
        return 6000;
    }

    /** Final per-attempt odds gate: 1-in-10 → 1-in-2. */
    @ModifyConstant(method = "spawn", constant = @Constant(intValue = 10))
    private int alone$betterSpawnOdds(final int original) {
        return 2;
    }
}
