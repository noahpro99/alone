package dev.alone.core;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug/admin conveniences (op-only) — not survival mechanics, just tools to set up test conditions fast:
 * <ul>
 *   <li><code>/alone reset</code> — refill every meter, clear every condition, top off vanilla vitals.</li>
 *   <li><code>/alone wet</code> — soak you (test the towel and wet-cold).</li>
 *   <li><code>/alone cold</code> / <code>/alone hot</code> — set body temp to the freeze-/heatstroke-damage range.</li>
 *   <li><code>/alone dirty</code> — dirty your hands (bleed while dirty to test wound sepsis).</li>
 * </ul>
 */
public final class AdminCommand {
    private AdminCommand() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // The op-gate lives on each debug subcommand (not the root), so the root "alone" node stays open
            // for the player-facing "/alone loadout" registered in Loadout — Brigadier merges the two.
            dispatcher.register(Commands.literal("alone")
                .then(Commands.literal("reset").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    resetAll(player);
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("Alone: all meters refilled, conditions cleared."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                // Debug helpers to set up test conditions instantly, so the harsh systems can be checked
                // without hunting for the exact circumstances (a freezing lake, being soaked, dirty hands…).
                .then(Commands.literal("wet").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(SurvivalMeters.WETNESS, 600);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): soaked through — try a towel, or feel the wet-cold."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("cold").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(SurvivalMeters.BODY_TEMP, -90f);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): body temp set to severe cold (freeze-damage range)."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("hot").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(SurvivalMeters.BODY_TEMP, 90f);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): body temp set to severe heat (heatstroke range)."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("dirty").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(Hygiene.HANDS_DIRTY, true);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): hands dirtied — bleed while dirty to test wound sepsis."), false);
                    return Command.SINGLE_SUCCESS;
                })));

            // Player-facing: check your skill levels (they otherwise only surface on a tier-up). No op needed.
            dispatcher.register(Commands.literal("skills").executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayerOrException();
                ctx.getSource().sendSuccess(() -> Skills.summary(player), false);
                return Command.SINGLE_SUCCESS;
            }));
        });
    }

    /** Refill every Alone meter, clear every condition, and top off the vanilla vitals. */
    private static void resetAll(ServerPlayer player) {
        // Alone survival meters (§1) — full stamina, no fatigue, full hydration, comfortable temp, dry.
        player.setAttached(SurvivalMeters.STAMINA, SurvivalMeters.MAX_STAMINA);
        player.setAttached(SurvivalMeters.FATIGUE, 0f);
        player.setAttached(SurvivalMeters.THIRST, SurvivalMeters.MAX_THIRST);
        player.setAttached(SurvivalMeters.BODY_TEMP, 0f);
        player.setAttached(SurvivalMeters.WETNESS, 0);
        player.setAttached(SurvivalMeters.VIGOR_UNTIL, 0L);

        // Conditions & injuries (§1.5) — clear illness, bleeding, sprains, infection, dirty hands.
        player.setAttached(Conditions.SICKNESS, 0);
        player.setAttached(Conditions.BLEEDING, 0);
        player.setAttached(Conditions.SPRAIN, 0);
        player.setAttached(Conditions.INFECTION, 0);
        player.setAttached(Conditions.DYSENTERY, 0);
        player.setAttached(Hygiene.HANDS_DIRTY, Boolean.FALSE);
        // Reset the scurvy clock (no fresh-food deficiency right after a reset).
        player.setAttached(Nutrition.LAST_VITAMIN, player.level().getGameTime());

        // Vanilla vitals — full health, fed, unhurt by fire/drowning, no lingering effects.
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
        player.setRemainingFireTicks(0);
        player.setAirSupply(player.getMaxAirSupply());
        player.removeAllEffects();
    }
}
