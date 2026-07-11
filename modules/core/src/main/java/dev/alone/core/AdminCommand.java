package dev.alone.core;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A debug/admin convenience (op-only): <code>/alone reset</code> puts you fully back to normal —
 * every survival meter refilled, every condition cleared, vanilla vitals topped off. Not a survival
 * mechanic; it's here so you can test the harsh systems without dying to set up the next test.
 */
public final class AdminCommand {
    private AdminCommand() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("alone")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("reset").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    resetAll(player);
                    ctx.getSource().sendSuccess(
                        () -> Component.literal("Alone: all meters refilled, conditions cleared."), false);
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
