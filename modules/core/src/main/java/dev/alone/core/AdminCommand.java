package dev.alone.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Debug/admin conveniences (op-only) — not survival mechanics, just tools to set up test conditions fast:
 * <ul>
 *   <li><code>/alone reset</code> — refill every meter, clear every condition, top off vanilla vitals.</li>
 *   <li><code>/alone wet</code> — soak you (test the towel and wet-cold).</li>
 *   <li><code>/alone cold</code> / <code>/alone hot</code> — set body temp to the freeze-/heatstroke-damage range.</li>
 *   <li><code>/alone dirty</code> — dirty your hands (bleed while dirty to test wound sepsis).</li>
 *   <li><code>/alone wind &lt;from&gt; &lt;0..1&gt;</code> / <code>/alone wind clear</code> — force the prevailing wind.</li>
 *   <li><code>/alone season &lt;spring|summer|autumn|winter&gt;</code> / <code>/alone season clear</code> — force the season.</li>
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
                }))
                // Force the prevailing wind: "/alone wind <from-direction> <strength 0..1>", or "clear".
                .then(Commands.literal("wind").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("clear").executes(ctx -> {
                        Wind.clearOverride();
                        ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): wind back to natural."), false);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.argument("from", StringArgumentType.word())
                        .suggests((c, b) -> {
                            for (String d : COMPASS) {
                                b.suggest(d);
                            }
                            return b.buildFuture();
                        })
                        .then(Commands.argument("strength", DoubleArgumentType.doubleArg(0.0, 1.0))
                            .executes(ctx -> {
                                String from = StringArgumentType.getString(ctx, "from").toLowerCase();
                                Vec3 toward = towardFrom(from);
                                if (toward == null) {
                                    ctx.getSource().sendFailure(Component.literal(
                                        "Unknown direction '" + from + "' — use north/south/east/west or a diagonal."));
                                    return 0;
                                }
                                float s = (float) DoubleArgumentType.getDouble(ctx, "strength");
                                Wind.setOverride(toward, s);
                                int pct = Math.round(s * 100);
                                ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Alone(debug): wind from the " + from + " at " + pct + "% strength."), false);
                                return Command.SINGLE_SUCCESS;
                            }))))
                // Force the season: "/alone season <spring|summer|autumn|winter>", or "clear".
                .then(Commands.literal("season").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("clear").executes(ctx -> {
                        Seasons.clearOverride();
                        ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): season back to natural."), false);
                        return Command.SINGLE_SUCCESS;
                    }))
                    .then(Commands.argument("season", StringArgumentType.word())
                        .suggests((c, b) -> {
                            b.suggest("spring");
                            b.suggest("summer");
                            b.suggest("autumn");
                            b.suggest("winter");
                            return b.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "season").toLowerCase();
                            int idx = switch (name) {
                                case "spring" -> 0;
                                case "summer" -> 1;
                                case "autumn", "fall" -> 2;
                                case "winter" -> 3;
                                default -> -1;
                            };
                            if (idx < 0) {
                                ctx.getSource().sendFailure(Component.literal(
                                    "Unknown season '" + name + "' — spring/summer/autumn/winter."));
                                return 0;
                            }
                            Seasons.setOverride(idx);
                            ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): season set to " + name + "."), false);
                            return Command.SINGLE_SUCCESS;
                        }))));

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

    private static final String[] COMPASS = {
        "north", "south", "east", "west", "northeast", "northwest", "southeast", "southwest"
    };

    /** The unit vector a wind named by where it comes FROM blows TOWARD (the opposite direction). Null if the
     *  name isn't a compass point. (MC axes: +x east, +z south.) */
    private static Vec3 towardFrom(String from) {
        Vec3 comesFrom = switch (from) {
            case "north" -> new Vec3(0, 0, -1);
            case "south" -> new Vec3(0, 0, 1);
            case "east" -> new Vec3(1, 0, 0);
            case "west" -> new Vec3(-1, 0, 0);
            case "northeast" -> new Vec3(0.7071, 0, -0.7071);
            case "northwest" -> new Vec3(-0.7071, 0, -0.7071);
            case "southeast" -> new Vec3(0.7071, 0, 0.7071);
            case "southwest" -> new Vec3(-0.7071, 0, 0.7071);
            default -> null;
        };
        return comesFrom == null ? null : comesFrom.scale(-1.0);
    }
}
