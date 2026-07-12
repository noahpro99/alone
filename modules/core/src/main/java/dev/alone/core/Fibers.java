package dev.alone.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import dev.alone.core.net.StripFiberPayload;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Plant fiber &amp; cordage (proposal §8.1) — where string comes from without a spider. Real cordage is
 * <b>stripped</b> from grasses, ferns and vines: a patient, hands-on job, not a by-product of ripping the
 * plant up. So you <b>hold right-click on a fibrous plant</b> (bare-handed, or with a cutting blade for a
 * cleaner, better strip) and tug at it for a couple of seconds until the fibre comes free and the plant is
 * torn out. Then you twist the fibre into string (see {@link CraftingTime} — that's timed too). Each held
 * click is one tug (the client sends them, like knapping/drilling).
 */
public final class Fibers {
    private Fibers() {
    }

    private static final int TUGS_TO_STRIP = 10;       // ~2.5s of steady stripping (client sends ~every 5t)
    private static final float STAMINA_PER_TUG = 0.6f;

    private record Strip(BlockPos pos, int tugs, int atTick) {
    }

    private static final Map<UUID, Strip> ACTIVE = new HashMap<>();
    /** Players already told (this session) how to get fibre — so the discovery hint shows once, not on repeat. */
    private static final Set<UUID> HINTED = new HashSet<>();

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(StripFiberPayload.TYPE,
            (payload, context) -> strip(context.player()));

        // Plant fibre is non-obvious: you don't get it by BREAKING grass (that just scatters seeds), you
        // STRIP it by holding right-click. So the first time a player mines a fibrous plant the old way,
        // nudge them once toward the real method — then never again (they've either learned it or don't care).
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (level.isClientSide() || !isFibrousPlant(state)) {
                return;
            }
            if (player instanceof ServerPlayer serverPlayer && HINTED.add(serverPlayer.getUUID())) {
                serverPlayer.sendSystemMessage(Component.literal(
                    "To get plant fibre, don't break grass — look at grass, a fern or a vine and "
                        + "hold right-click (bare-handed or with a blade) to strip it."));
            }
        });
    }

    /** The fibrous plant you're looking at (the thing you strip), or null. */
    public static BlockPos findFibrousPlant(Player player, Level level) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.calculateViewVector(player.getXRot(), player.getYRot())
            .scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        return isFibrousPlant(level.getBlockState(pos)) ? pos : null;
    }

    private static void strip(ServerPlayer player) {
        Level level = player.level();
        BlockPos plant = findFibrousPlant(player, level);
        if (plant == null || !(level instanceof ServerLevel serverLevel)) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        HINTED.add(player.getUUID()); // already stripping — they know how; don't nudge them later
        var rng = player.getRandom();
        level.playSound(null, plant, SoundEvents.GRASS_HIT, SoundSource.PLAYERS,
            0.4f, 0.9f + rng.nextFloat() * 0.2f);
        SurvivalMeters.exert(player, STAMINA_PER_TUG);

        Strip current = ACTIVE.get(player.getUUID());
        boolean continuing = current != null && current.pos.equals(plant) && player.tickCount - current.atTick <= 12;
        int tugs = continuing ? current.tugs + 1 : 1;
        if (tugs < TUGS_TO_STRIP) {
            player.sendSystemMessage(Component.literal("Stripping fibre…"), true);
            ACTIVE.put(player.getUUID(), new Strip(plant.immutable(), tugs, player.tickCount));
            return;
        }

        // The fibre comes free and the plant is torn out.
        ACTIVE.remove(player.getUUID());
        ItemStack tool = player.getMainHandItem();
        boolean blade = tool.is(ItemTags.SWORDS) || tool.is(ItemTags.AXES) || tool.is(ItemTags.HOES)
            || tool.is(AloneItems.FLINT_KNIFE);
        int fiber = blade
            ? 2 + rng.nextInt(2)                      // a blade strips 2–3 clean lengths
            : 1 + (rng.nextFloat() < 0.5f ? 1 : 0);   // bare hands: 1–2, rougher
        Block.popResource(serverLevel, plant, new ItemStack(AloneItems.PLANT_FIBER, fiber));
        serverLevel.destroyBlock(plant, false); // the plant is stripped out, no seed
        level.playSound(null, plant, SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 0.7f, 1.0f);
        player.sendSystemMessage(Component.literal("You strip " + fiber + " plant fibre."), true);
    }

    /** Grasses, ferns, vines, and other fibrous ground plants you can strip cordage from. */
    private static boolean isFibrousPlant(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
            || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
            || state.is(Blocks.VINE) || state.is(Blocks.DEAD_BUSH)
            || state.is(Blocks.HANGING_ROOTS) || state.is(Blocks.CAVE_VINES)
            || state.is(Blocks.CAVE_VINES_PLANT) || state.is(Blocks.WEEPING_VINES)
            || state.is(Blocks.TWISTING_VINES);
    }
}
