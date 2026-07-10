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
 * crafting-grid afterthought. (A saw, later, changes that.) Splitting along the grain is gentle on the
 * edge, so it barely wears the hatchet.
 */
public final class Riving {
    private Riving() {
    }

    private static final int STROKES_TO_RIVE = 45;     // ~13.5s of steady splitting (client sends ~every 6t)
    private static final float STAMINA_PER_STROKE = 2.0f; // ~90 stamina a log — nearly a full bar; you tire fast
    private static final int PLANKS_PER_LOG = 2;        // rough hand-riven yield (a saw does better, later)

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
        ItemStack hatchet = main.is(AloneItems.FLINT_HATCHET) ? main
            : (off.is(AloneItems.FLINT_HATCHET) ? off : ItemStack.EMPTY);
        Item planks = log.isEmpty() ? null : planksFor(log.getItem());
        if (!player.isShiftKeyDown() || log.isEmpty() || hatchet.isEmpty() || planks == null) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        if (SurvivalMeters.getStamina(player) <= 0f) {
            player.sendSystemMessage(Component.literal("You're too spent to keep splitting — rest first."), true);
            return; // exhausted; the split waits
        }

        var rng = player.getRandom();
        SurvivalMeters.exert(player, STAMINA_PER_STROKE);
        level.playSound(null, player.blockPosition(), SoundEvents.AXE_STRIP, SoundSource.PLAYERS,
            0.6f, 0.8f + rng.nextFloat() * 0.2f);

        Rive current = ACTIVE.get(player.getUUID());
        boolean continuing = current != null && player.tickCount - current.atTick <= 12;
        int strokes = continuing ? current.strokes + 1 : 1;

        if (strokes < STROKES_TO_RIVE) {
            player.sendSystemMessage(Component.literal(
                "Riving… " + (strokes * 100 / STROKES_TO_RIVE) + "%"), true);
            ACTIVE.put(player.getUUID(), new Rive(strokes, player.tickCount));
            return;
        }

        // The log finally splits into a couple of rough boards.
        ACTIVE.remove(player.getUUID());
        log.shrink(1);
        if (!player.getInventory().add(new ItemStack(planks, PLANKS_PER_LOG))) {
            player.drop(new ItemStack(planks, PLANKS_PER_LOG), false);
        }
        if (!player.isCreative()) {
            hatchet.setDamageValue(hatchet.getDamageValue() + 1); // splitting is gentle on the edge
            if (hatchet.getDamageValue() >= hatchet.getMaxDamage()) {
                hatchet.shrink(1); // worn out
                level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.7f, 1f);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.8f, 1f);
        player.sendSystemMessage(Component.literal("The log splits into rough boards."), true);
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
