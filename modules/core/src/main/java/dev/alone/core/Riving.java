package dev.alone.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.alone.core.net.RiveStrokePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Riving planks (proposal §8.1/§5.4). Before saws, boards were <b>split from a log</b> with a froe and
 * mallet — here, your flint hatchet. Hold a <b>log</b> and a <b>{@link AloneItems#FLINT_HATCHET flint
 * hatchet}</b> (either hand) and <b>sneak + hold right-click</b> to rive. It is <b>brutal, slow work</b>:
 * a full split is dozens of blows that drain most of your wind, so one log yields just <b>2 rough
 * boards</b> and a couple of rivings leave you spent — a stack of planks is a real day's labour, not a
 * crafting-grid afterthought. Splitting along the grain is gentle on the
 * edge, so it barely wears the hatchet.
 *
 * <p>A <b>{@link AloneItems#HAND_SAW hand saw}</b> (iron) changes that: the same sneak + hold-right-click on
 * a log, but sawing is far quicker (~6s), much easier on your wind, and yields <b>more, cleaner boards</b> —
 * the tool that turns plank-making from a gruelling chore into ordinary work.
 */
public final class Riving {
    private Riving() {
    }

    private static final int STROKES_TO_RIVE = 45;     // ~13.5s of steady splitting (client sends ~every 6t)
    private static final float STAMINA_PER_STROKE = 2.0f; // ~90 stamina a log — nearly a full bar; you tire fast
    private static final int PLANKS_PER_LOG = 2;        // rough hand-riven yield

    private static final int SAW_STROKES = 20;          // ~6s — a saw cuts far quicker than splitting
    private static final float SAW_STAMINA = 0.8f;      // ~16 stamina a log — ordinary work, not a wringing-out
    private static final int SAW_PLANKS = 4;            // clean boards, a full log's worth

    private record Rive(int strokes, int atTick) {
    }

    private static final Map<UUID, Rive> ACTIVE = new HashMap<>();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(RiveStrokePayload.TYPE,
            (payload, context) -> strike(context.player()));
    }

    private static void strike(ServerPlayer player) {
        Level level = player.level();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack log = main.is(ItemTags.LOGS) ? main : (off.is(ItemTags.LOGS) ? off : ItemStack.EMPTY);
        ItemStack tool = isRiveTool(main) ? main : (isRiveTool(off) ? off : ItemStack.EMPTY);
        Item planks = log.isEmpty() ? null : planksFor(log.getItem());
        if (!player.isShiftKeyDown() || log.isEmpty() || tool.isEmpty() || planks == null) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        boolean saw = tool.is(AloneItems.HAND_SAW);
        int strokesNeeded = saw ? SAW_STROKES : STROKES_TO_RIVE;
        float staminaPerStroke = saw ? SAW_STAMINA : STAMINA_PER_STROKE;
        int yield = saw ? SAW_PLANKS : PLANKS_PER_LOG;
        if (SurvivalMeters.getStamina(player) <= 0f) {
            player.sendSystemMessage(Component.literal("You're too spent to keep splitting — rest first."), true);
            return; // exhausted; the split waits
        }

        var rng = player.getRandom();
        SurvivalMeters.exert(player, staminaPerStroke);
        level.playSound(null, player.blockPosition(), SoundEvents.AXE_STRIP, SoundSource.PLAYERS,
            0.6f, 0.8f + rng.nextFloat() * 0.2f);

        Rive current = ACTIVE.get(player.getUUID());
        boolean continuing = current != null && player.tickCount - current.atTick <= 12;
        int strokes = continuing ? current.strokes + 1 : 1;

        if (strokes < strokesNeeded) {
            player.sendSystemMessage(Component.literal(
                (saw ? "Sawing… " : "Riving… ") + (strokes * 100 / strokesNeeded) + "%"), true);
            ACTIVE.put(player.getUUID(), new Rive(strokes, player.tickCount));
            return;
        }

        // The log finally splits into a couple of rough boards.
        ACTIVE.remove(player.getUUID());
        log.shrink(1);
        ItemStack boards = new ItemStack(planks, yield);
        if (!player.getInventory().add(boards)) {
            player.drop(boards, false); // add() reduces the stack to the remainder — drop THAT, not a fresh full stack
        }
        if (!player.isCreative()) {
            tool.setDamageValue(tool.getDamageValue() + 1); // riving is gentle on an edge; sawing wears the blade
            if (tool.getDamageValue() >= tool.getMaxDamage()) {
                tool.shrink(1); // worn out
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.7f, 1f);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.8f, 1f);
        player.sendSystemMessage(Component.literal(saw
            ? "You saw the log into clean boards." : "The log splits into rough boards."), true);
    }

    /** Either tool that works a log into boards: the flint hatchet (slow, brutal splitting) or the hand saw
     *  (quick, easy, more boards). */
    private static boolean isRiveTool(ItemStack stack) {
        return stack.is(AloneItems.FLINT_HATCHET) || stack.is(AloneItems.HAND_SAW);
    }

    /** The planks a given log rives into — derived from its name (oak_log → oak_planks, warped_stem →
     *  warped_planks, stripped_birch_wood → birch_planks). Null if it isn't a rivable log. */
    private static Item planksFor(Item log) {
        String path = BuiltInRegistries.ITEM.getKey(log).getPath();
        String family = path.replace("stripped_", "").replaceAll("_(log|wood|stem|hyphae)$", "");
        return BuiltInRegistries.ITEM
            .getOptional(Identifier.fromNamespaceAndPath("minecraft", family + "_planks"))
            .orElse(null);
    }
}
