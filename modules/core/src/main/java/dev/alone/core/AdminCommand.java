package dev.alone.core;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Debug/admin conveniences (op-only) — not survival mechanics, just tools to set up test conditions fast:
 * <ul>
 *   <li><code>/alone reset</code> — refill every meter, clear every condition, top off vanilla vitals.</li>
 *   <li><code>/alone wet</code> — soak you (test the towel and wet-cold).</li>
 *   <li><code>/alone cold</code> / <code>/alone hot</code> — set body temp to the freeze-/heatstroke-damage range.</li>
 *   <li><code>/alone dirty</code> — dirty your hands (bleed while dirty to test wound sepsis).</li>
 *   <li><code>/alone kit</code> — give one of each of this build's new items plus materials for the chains.</li>
 * </ul>
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
                }))
                // Debug helpers (op-only) to set up test conditions instantly, so the harsh systems can be
                // checked without hunting for the exact circumstances (a freezing lake, a near-full pack…).
                .then(Commands.literal("wet").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(SurvivalMeters.WETNESS, 600);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): soaked through — try a towel, or feel the wet-cold."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("cold").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(SurvivalMeters.BODY_TEMP, -90f);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): body temp set to severe cold (freeze-damage range)."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("hot").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(SurvivalMeters.BODY_TEMP, 90f);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): body temp set to severe heat (heatstroke range)."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("dirty").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.setAttached(Hygiene.HANDS_DIRTY, true);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): hands dirtied — bleed while dirty to test wound sepsis."), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("kit").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    giveTestKit(player);
                    ctx.getSource().sendSuccess(() -> Component.literal("Alone(debug): this build's new items + materials to craft the chains."), false);
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

    /** Give one of each of this build's new items, plus the raw materials to craft the new chains, so the
     *  whole batch can be exercised at once (saw, sleeping bag, towel, tarp, gill net, ferro rod, bread). */
    private static void giveTestKit(ServerPlayer player) {
        ItemStack[] kit = {
            new ItemStack(AloneItems.FERRO_ROD),
            new ItemStack(AloneItems.SLEEPING_BAG),
            new ItemStack(AloneItems.THATCH, 8),
            new ItemStack(AloneItems.TARP, 3),
            new ItemStack(AloneItems.SEWING_KIT),
            new ItemStack(AloneItems.TOWEL),
            new ItemStack(AloneItems.HAND_SAW),
            new ItemStack(AloneItems.GILL_NET, 2),
            new ItemStack(AloneItems.FISH_TRAP),
            new ItemStack(AloneItems.FLOUR, 4),
            new ItemStack(AloneItems.DOUGH, 2),
            // materials for the new crafting chains (wool omitted — the bag/towel are given finished)
            new ItemStack(Items.IRON_INGOT, 6),
            new ItemStack(Items.LEATHER, 6),
            new ItemStack(Items.STRING, 16),
            new ItemStack(Items.WHEAT, 8),
            new ItemStack(Items.WATER_BUCKET),
            new ItemStack(Items.BONE, 2),
            new ItemStack(Items.OAK_LOG, 6),
        };
        for (ItemStack stack : kit) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
    }
}
